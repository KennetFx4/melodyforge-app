package com.example.musicgenapp

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiConfig {
    const val BASE_URL = "https://tapering-retry-reproach.ngrok-free.dev/"
}

sealed class GenerateResult {
    data class Success(val audioBytes: ByteArray, val trackId: String) : GenerateResult()
    data class Blocked(val reason: String) : GenerateResult()
    data class Error(val message: String) : GenerateResult()
}

class MusicApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun generate(prompt: String, durationSeconds: Int): GenerateResult {
        return try {
            val json = JSONObject().apply {
                put("prompt", prompt)
                put("duration_seconds", durationSeconds)
            }
            val body: RequestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = okhttp3.Request.Builder()
                .url(ApiConfig.BASE_URL.trimEnd('/') + "/generate")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> {
                        val bytes = response.body?.bytes() ?: return GenerateResult.Error("Empty response")
                        val trackId = response.header("X-Track-Id") ?: "unknown"
                        GenerateResult.Success(bytes, trackId)
                    }
                    response.code == 400 -> {
                        val msg = response.body?.string() ?: "Prompt was blocked."
                        GenerateResult.Blocked(msg)
                    }
                    else -> GenerateResult.Error("Server error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            GenerateResult.Error(e.message ?: "Network error")
        }
    }
}
