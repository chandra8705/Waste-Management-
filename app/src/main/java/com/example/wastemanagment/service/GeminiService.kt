package com.example.wastemanagment.service

import com.example.wastemanagment.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GeminiService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private val MODEL_CANDIDATES = listOf(
        "gemini-1.5-flash",
        "gemini-1.5-flash-latest",
        "gemini-1.0-pro"
    )

    private val api: GeminiApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun generateResponse(
        apiKey: String,
        userMessage: String,
        chatHistory: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(userMessage, chatHistory)
        val request = GeminiContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        var lastError: Exception? = null
        for (model in MODEL_CANDIDATES) {
            try {
                val response = api.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )

                val text = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                    ?: ""

                if (text.isNotEmpty()) return@withContext text
                // If empty, try next model
            } catch (e: HttpException) {
                // 404/403: try next model; others: propagate later
                if (e.code() != 404 && e.code() != 403) {
                    val body = e.response()?.errorBody()?.string()
                    throw RuntimeException("HTTP ${e.code()}: ${body ?: e.message()}")
                }
                lastError = e
            } catch (e: Exception) {
                lastError = e
            }
        }
        // If we get here, all models failed
        lastError?.let {
            if (it is HttpException) {
                val body = it.response()?.errorBody()?.string()
                throw RuntimeException("HTTP ${it.code()}: ${body ?: it.message()}")
            } else {
                throw RuntimeException(it.message ?: "Unknown error")
            }
        }
        ""
    }

    private fun buildPrompt(userMessage: String, chatHistory: List<ChatMessage>): String {
        return buildString {
            append("You are an assistant specialized in waste management.\n")
            chatHistory.forEach { msg ->
                append(if (msg.isUser) "User: " else "Assistant: ")
                append(msg.message)
                append('\n')
            }
            append("User: ")
            append(userMessage)
        }
    }
}


