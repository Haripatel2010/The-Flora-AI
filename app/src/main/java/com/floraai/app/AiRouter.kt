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

object AiRouter {

    private val client = OkHttpClient()
    private val jsonType = "application/json".toMediaType()

    suspend fun generate(context: Context, prompt: String): String {
        val providers = listOf(
            "gemini",
            "groq",
            "openrouter"
        )

        for (provider in providers) {
            val key = KeyVault.get(context, provider) ?: continue

            try {
                return when (provider) {
                    "gemini" -> askGemini(key, prompt)
                    "groq" -> askGroq(key, prompt)
                    "openrouter" -> askOpenRouter(key, prompt)
                    else -> continue
                }
            } catch (_: Exception) {
                continue
            }
        }

        return "No working API key found. Please check your provider keys and internet connection."
    }

    private suspend fun askGemini(
        key: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", prompt))
                )
            ))
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
            .post(body.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Gemini error")
            val result = JSONObject(response.body?.string() ?: "")
            result.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private suspend fun askGroq(
        key: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        askOpenAiCompatible(
            "https://api.groq.com/openai/v1/chat/completions",
            key,
            "llama-3.3-70b-versatile",
            prompt
        )
    }

    private suspend fun askOpenRouter(
        key: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        askOpenAiCompatible(
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

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Provider error")
            val result = JSONObject(response.body?.string() ?: "")
            result.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
