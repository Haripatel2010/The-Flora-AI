package com.floraai.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AiRouter {

    private val client = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    suspend fun generate(context: Context, prompt: String): String =
        withContext(Dispatchers.IO) {

            val providers = listOf("gemini", "groq", "openrouter")
            val errors = mutableListOf<String>()

            for (provider in providers) {
                val key = KeyVault.get(context, provider)

                if (key.isNullOrBlank()) continue

                try {
                    return@withContext when (provider) {
                        "gemini" -> askGemini(key, prompt)
                        "groq" -> askGroq(key, prompt)
                        "openrouter" -> askOpenRouter(key, prompt)
                        else -> continue
                    }
                } catch (e: Exception) {
                    errors.add("$provider: ${e.message}")
                }
            }

            if (errors.isEmpty()) {
                "No API key found. Open Settings and save a Gemini, Groq or OpenRouter key."
            } else {
                "All AI providers failed:\n\n" + errors.joinToString("\n\n")
            }
        }

    private fun askGemini(key: String, prompt: String): String {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", prompt))
                )
            ))
        }

        val request = Request.Builder()
            .url(
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
)
            .addHeader("x-goog-api-key", key)
            .post(body.toString().toRequestBody(jsonType))
            .build()

        return client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $responseText")
            }

            val result = JSONObject(responseText)
            result.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun askGroq(key: String, prompt: String): String {
        return askOpenAiCompatible(
            "https://api.groq.com/openai/v1/chat/completions",
            key,
            "llama-3.3-70b-versatile",
            prompt
        )
    }

    private fun askOpenRouter(key: String, prompt: String): String {
        return askOpenAiCompatible(
            "https://openrouter.ai/api/v1/chat/completions",
            key,
            "meta-llama/llama-3.3-70b-instruct:free",
            prompt
        )
    }

    private fun askOpenAiCompatible(
        url: String,
        key: String,
        model: String,
        prompt: String
    ): String {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", prompt)
            ))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody(jsonType))
            .build()

        return client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $responseText")
            }

            val result = JSONObject(responseText)
            result.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
