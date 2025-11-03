# AI Integration Guide

This guide explains how to upgrade the chatbot from rule-based responses to a real LLM API (OpenAI, Google Gemini, or Anthropic Claude).

## Current Implementation

The chatbot currently uses `AIChatbotService.kt` with intelligent rule-based pattern matching. This provides good responses without requiring API keys or internet costs.

## Files Overview

- **`app/src/main/java/com/example/wastemanagment/service/AIChatbotService.kt`**: Main service (currently rule-based)
- **`app/src/main/java/com/example/wastemanagment/ui/AIChatbotActivity.kt`**: Chat UI interface
- **`app/src/main/java/com/example/wastemanagment/model/ChatMessage.kt`**: Message data model
- **`app/src/main/java/com/example/wastemanagment/adapter/ChatMessageAdapter.kt`**: UI adapter for chat messages

## Architecture

```
┌─────────────────────┐
│ AIChatbotActivity   │  (UI Layer)
│  - RecyclerView     │
│  - User Input       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  AIChatbotService   │  (Service Layer)
│  - generateResponse()│  ← Replace this
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    LLM API          │  (Integration Layer)
│  OpenAI/Gemini/etc  │
└─────────────────────┘
```

## Option 1: OpenAI Integration

### Step 1: Add API Key

Create `app/src/main/res/values/api_keys.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="openai_api_key">YOUR_API_KEY_HERE</string>
</resources>
```

**Important**: Add this file to `.gitignore` to protect your API key.

### Step 2: Create API Interface

Create `app/src/main/java/com/example/wastemanagment/service/OpenAIService.kt`:

```kotlin
package com.example.wastemanagment.service

import com.example.wastemanagment.R
import com.example.wastemanagment.model.ChatMessage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Request/Response models
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: OpenAIRequest): OpenAIResponse
}

object OpenAIClient {
    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer YOUR_API_KEY") // TODO: Add from resources
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(createOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: OpenAIApi = retrofit.create(OpenAIApi::class.java)
}
```

### Step 3: Update AIChatbotService

Replace the `generateResponse` function in `AIChatbotService.kt`:

```kotlin
suspend fun generateResponse(
    userMessage: String,
    chatHistory: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    
    try {
        // Convert chat history to OpenAI format
        val messages = chatHistory.map { 
            Message(
                role = if(it.isUser) "user" else "assistant",
                content = it.message
            )
        }
        
        // Add system message for context
        val systemMessage = Message(
            role = "system",
            content = "You are a helpful waste management assistant. " +
                    "Provide advice on recycling, waste disposal, and environmental best practices."
        )
        
        val request = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(systemMessage) + messages
        )
        
        val response = OpenAIClient.api.chat(request)
        
        return@withContext response.choices.firstOrNull()?.message?.content
            ?: "Sorry, I couldn't generate a response."
            
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext "Error: ${e.message}. Please try again."
    }
}
```

## Option 2: Google Gemini Integration

### Step 1: Add Dependencies

Add to `app/build.gradle.kts`:

```kotlin
implementation("com.google.ai.client.generativeai:generativeai:0.1.1")
```

### Step 2: Update AIChatbotService

```kotlin
import com.google.ai.client.generativeai.GenerativeModel

suspend fun generateResponse(
    userMessage: String,
    chatHistory: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    
    try {
        val apiKey = "YOUR_GEMINI_API_KEY" // Get from Google AI Studio
        
        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKey
        )
        
        // Build conversation history
        val conversation = generativeModel.startChat(
            history = chatHistory.map { chat ->
                if (chat.isUser) {
                    com.google.ai.client.generativeai.ChatContent(chat.message)
                } else {
                    com.google.ai.client.generativeai.ChatContent(chat.message)
                }
            }
        )
        
        val response = generativeModel.generateContent(userMessage)
        
        return@withContext response.text ?: "Sorry, I couldn't generate a response."
        
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext "Error: ${e.message}"
    }
}
```

## Option 3: Anthropic Claude Integration

Similar to OpenAI, use Retrofit with Claude's API.

## Testing

1. Test with simple queries first
2. Check API quotas and costs
3. Implement error handling
4. Add rate limiting if needed
5. Cache responses for common queries

## Security Best Practices

1. **Never commit API keys**: Add `api_keys.xml` to `.gitignore`
2. **Use BuildConfig**: Store keys in `build.gradle.kts` using build config
3. **Validate user input**: Prevent prompt injection
4. **Rate limiting**: Protect against abuse
5. **Data privacy**: Don't log sensitive conversations

## Cost Management

- Use caching for common questions
- Implement request debouncing
- Set usage limits
- Monitor API usage
- Consider using smaller models for simple queries

## Hybrid Approach

You can combine rule-based and LLM:

```kotlin
suspend fun generateResponse(
    userMessage: String,
    chatHistory: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    
    // Try rule-based first (free, fast)
    val ruleBasedResponse = tryRuleBasedResponse(userMessage)
    if (ruleBasedResponse != null) return ruleBasedResponse
    
    // Fallback to LLM for complex queries
    val llmResponse = generateLLMResponse(userMessage, chatHistory)
    return llmResponse
}
```

## Need Help?

- OpenAI Docs: https://platform.openai.com/docs
- Gemini Docs: https://ai.google.dev/docs
- Anthropic Docs: https://docs.anthropic.com

## Current Status

✅ Chat UI implemented
✅ Service layer ready for integration
✅ Dependencies added (Retrofit, OkHttp)
✅ Message history support
✅ Multi-language support
✅ Error handling ready

⏳ Choose your LLM provider
⏳ Implement API integration
⏳ Add API key management
⏳ Test and optimize

The foundation is complete - just connect your preferred LLM API!

