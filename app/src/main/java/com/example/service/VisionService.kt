package com.example.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class VerificationResult(
    val success: Boolean,
    val confidence: Float,
    val funnyMessage: String,
    val detectedLabels: List<String>
)

object VisionService {
    private const val TAG = "VisionService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun verifyDrinkingAction(base64Image: String): VerificationResult = withContext(Dispatchers.IO) {
        // 1. Try Google Vision API if key is set and valid
        val googleApiKey = BuildConfig.GOOGLE_VISION_API_KEY
        if (googleApiKey.isNotEmpty() && !googleApiKey.contains("MY_GOOGLE_VISION_API_KEY")) {
            try {
                return@withContext verifyWithGoogleVision(base64Image, googleApiKey)
            } catch (e: Exception) {
                Log.e(TAG, "Google Vision verification failed, falling back to Gemini", e)
            }
        }

        // 2. Otherwise/Fallback, verify with Gemini API
        try {
            return@withContext verifyWithGemini(base64Image)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini verification failed", e)
            // Hard fallback with warning
            return@withContext VerificationResult(
                success = true, // Fallback to manual override
                confidence = 0.5f,
                funnyMessage = "Network glitch! We'll give you a pass this time, but stay hydrated! ${e.localizedMessage}",
                detectedLabels = listOf("fallback", "offline")
            )
        }
    }

    private fun verifyWithGoogleVision(base64Image: String, apiKey: String): VerificationResult {
        val url = "https://vision.googleapis.com/v1/images:annotate?key=$apiKey"
        val jsonPayload = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("image", JSONObject().apply {
                        put("content", base64Image)
                    })
                    put("features", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "LABEL_DETECTION")
                            put("maxResults", 15)
                        })
                        put(JSONObject().apply {
                            put("type", "OBJECT_LOCALIZATION")
                            put("maxResults", 15)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Google Vision API error: ${response.code} ${response.message}")
            }
            val bodyString = response.body?.string() ?: throw Exception("Empty body response from Google Vision")
            val root = JSONObject(bodyString)
            val responses = root.getJSONArray("responses")
            if (responses.length() == 0) {
                throw Exception("No responses block from Google Vision")
            }
            val firstResponse = responses.getJSONObject(0)

            val detectedLabels = mutableListOf<String>()
            var maxConfidence = 0.0f
            var isDrinkingAction = false

            // Process Label Annotations
            if (firstResponse.has("labelAnnotations")) {
                val labelAnnotations = firstResponse.getJSONArray("labelAnnotations")
                for (i in 0 until labelAnnotations.length()) {
                    val labelObj = labelAnnotations.getJSONObject(i)
                    val description = labelObj.optString("description", "").lowercase()
                    val score = labelObj.optDouble("score", 0.0).toFloat()
                    detectedLabels.add(description)

                    // Keywords indicating drinking activity
                    val matchingKeywords = listOf("drinking", "bottle", "cup", "beverage", "liquid", "water", "juice", "beaker", "fluid", "mug", "sip", "drinkware", "hand", "face")
                    if (matchingKeywords.any { description.contains(it) }) {
                        if (score > 0.70f) {
                            isDrinkingAction = true
                            if (score > maxConfidence) {
                                maxConfidence = score
                            }
                        }
                    }
                }
            }

            // Process Object Localization
            if (firstResponse.has("localizedObjectAnnotations")) {
                val objectAnnotations = firstResponse.getJSONArray("localizedObjectAnnotations")
                for (i in 0 until objectAnnotations.length()) {
                    val obj = objectAnnotations.getJSONObject(i)
                    val name = obj.optString("name", "").lowercase()
                    val score = obj.optDouble("score", 0.0).toFloat()
                    detectedLabels.add(name)

                    if (name.contains("bottle") || name.contains("cup") || name.contains("glass") || name.contains("person")) {
                        if (score > 0.70f) {
                            isDrinkingAction = true
                            if (score > maxConfidence) {
                                maxConfidence = score
                            }
                        }
                    }
                }
            }

            val funnyMsg = if (isDrinkingAction) {
                "HydroForce confirmed! Keep gulping, you majestic dolphin."
            } else {
                "Is that a drink? Google Cloud Vision sees: ${detectedLabels.take(3).joinToString(", ")}. Try showing your glass or bottle clearly!"
            }

            return VerificationResult(
                success = isDrinkingAction,
                confidence = if (maxConfidence > 0f) maxConfidence else 0.5f,
                funnyMessage = funnyMsg,
                detectedLabels = detectedLabels
            )
        }
    }

    private fun verifyWithGemini(base64Image: String): VerificationResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
            Log.w(TAG, "No valid Gemini API key found, using auto-confirm fallback")
            return VerificationResult(
                success = true,
                confidence = 1.0f,
                funnyMessage = "Developer mode: Gemini Key not set. Auto-confirmed! Stay hydrated anyway!",
                detectedLabels = listOf("dev_mode", "auto_confirm")
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val promptText = """
            Analyze this photo of a person taken from their camera.
            Determine if they are drinking water or holding a water bottle, glass, or cup near their mouth/face to drink.
            Verify the action with high accuracy.
            You must reply ONLY with a valid JSON block containing:
            1. "isDrinking" (boolean) - true if a drinkware (bottle, cup, glass) is near their face or they're sipping.
            2. "confidence" (number between 0.0 and 1.0) - your match certainty.
            3. "funnyMessage" (string) - a playful/humorous response. If verified, say something congratulations like "Ocean mode activated!". If verified is false, write a funny, slightly sarcastic reminder to actually drink instead of faking it.
            4. "detectedLabels" (array of strings) - items you recognized like "bottle", "face", "glasses", "cup", etc.
            
            Example response:
            {
              "isDrinking": true,
              "confidence": 0.88,
              "funnyMessage": "Ocean level rising! Verified. Keep it up!",
              "detectedLabels": ["bottle", "mouth", "face"]
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw Exception("Empty body response from Gemini")
            if (!response.isSuccessful) {
                throw Exception("Gemini API error ${response.code}: $responseBody")
            }

            val root = JSONObject(responseBody)
            val candidates = root.getJSONArray("candidates")
            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val jsonResult = JSONObject(text.trim())
            val isDrinking = jsonResult.optBoolean("isDrinking", false)
            val confidence = jsonResult.optDouble("confidence", 0.5).toFloat()
            val funnyMessage = jsonResult.optString("funnyMessage", "Checked by Gemini AI.")
            val detectedLabelsJson = jsonResult.optJSONArray("detectedLabels")
            val detectedLabels = mutableListOf<String>()
            if (detectedLabelsJson != null) {
                for (i in 0 until detectedLabelsJson.length()) {
                    detectedLabels.add(detectedLabelsJson.getString(i))
                }
            }

            return VerificationResult(
                success = isDrinking,
                confidence = confidence,
                funnyMessage = funnyMessage,
                detectedLabels = detectedLabels
            )
        }
    }
}
