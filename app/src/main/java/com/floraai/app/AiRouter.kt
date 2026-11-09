package com.floraai.app

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiRouter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    fun generate(context: Context, prompt: String): String {
        val errors = mutableListOf<String>()

        val providers = listOf("gemini", "groq", "openrouter")

        for (provider in providers) {
            val key = KeyVault.get(context, provider)

            if (key.isNullOrBlank()) {
                continue
            }

            try {
                val answer = when (provider) {
                    "gemini" -> askGemini(key, prompt)
                    "groq" -> askGroq(key, prompt)
                    "openrouter" -> askOpenRouter(key, prompt)
                    else -> ""
                }

                if (answer.isNotBlank()) {
                    return answer
                }

                errors.add("$provider: Empty response")

            } catch (e: Exception) {
                errors.add("$provider: ${e.message ?: "Unknown error"}")
            }
        }

        return if (errors.isEmpty()) {
            "No working API key found. Please add an API key in Settings."
        } else {
            "All AI providers failed:\n\n" + errors.joinToString("\n\n")
        }
    }

    private fun askGemini(key: String, prompt: String): String {
        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put("text", prompt)
                        )
                    )
                )
            )

            put(
                "generationConfig",
                JSONObject().put(
                    "thinkingConfig",
                    JSONObject().put("thinkingLevel", "low")
                )
            )
        }

        val request = Request.Builder()
            .url(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
            )
            .addHeader("x-goog-api-key", key)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val result = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $result")
            }

            val json = JSONObject(result)

            return json
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?: throw Exception("No text returned by Gemini")
        }
    }

    private fun askGroq(key: String, prompt: String): String {
        return askOpenAICompatible(
            url = "https://api.groq.com/openai/v1/chat/completions",
            key = key,
            model = "llama-3.3-70b-versatile",
            prompt = prompt
        )
    }

    private fun askOpenRouter(key: String, prompt: String): String {
        return askOpenAICompatible(
            url = "https://openrouter.ai/api/v1/chat/completions",
            key = key,
            model = "meta-llama/llama-3.3-70b-instruct:free",
            prompt = prompt
        )
    }

    private fun askOpenAICompatible(
        url: String,
        key: String,
        model: String,
        prompt: String
    ): String {

        val body = JSONObject().apply {
            put("model", model)

            put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            )

            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val result = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $result")
            }

            val json = JSONObject(result)

            return json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?: throw Exception("No text returned")
        }
    }
}
