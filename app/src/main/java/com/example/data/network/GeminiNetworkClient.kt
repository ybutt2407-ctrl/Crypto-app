package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiNetworkClient {
    private const val TAG = "GeminiNetworkClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Data classes matching Gemini REST JSON structure
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
    
    data class ResponseFormat(val mimeType: String)
    data class GenerationConfig(val responseFormat: ResponseFormat? = null, val temperature: Float? = null)
    
    data class GenerateContentRequest(
        val contents: List<Content>,
        val generationConfig: GenerationConfig? = null
    )

    data class GenerateContentResponse(
        val candidates: List<Candidate>? = null,
        val error: GeminiError? = null
    )
    
    data class Candidate(val content: Content)
    data class GeminiError(val code: Int, val message: String, val status: String)

    suspend fun getPrediction(prompt: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder.")
            return@withContext null
        }

        // Build request payload using direct strings to avoid Moshi reflection serialization limits
        // or configure serializable adapters manually. Sending raw structured JSON is extremely lightweight and safe.
        val requestBodyJson = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": ${escapeJsonString(prompt)}
                    }
                  ]
                }
              ],
              "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.2
              }
            }
        """.trimIndent()

        val requestBody = requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response from Gemini: Code ${response.code}, Body: $bodyString")
                    return@withContext null
                }
                
                // Parse response using Moshi
                val adapter = moshi.adapter(GenerateContentResponse::class.java)
                val apiResponse = adapter.fromJson(bodyString)
                
                val textResponse = apiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                Log.d(TAG, "Successfully retrieved response: $textResponse")
                return@withContext textResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            return@withContext null
        }
    }

    private fun escapeJsonString(input: String): String {
        return moshi.adapter(String::class.java).toJson(input)
    }
}
