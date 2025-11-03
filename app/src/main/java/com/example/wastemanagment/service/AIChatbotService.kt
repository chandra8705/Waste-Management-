package com.example.wastemanagment.service

import com.example.wastemanagment.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI Chatbot Service for waste management assistance
 * 
 * This service provides AI-powered responses for waste management questions.
 * Currently uses a rule-based system that can be easily upgraded to use:
 * - OpenAI API (GPT models)
 * - Google Gemini API
 * - Anthropic Claude API
 * 
 * To use a real AI API, implement the API request logic in the generateResponse method.
 */
object AIChatbotService {

    /**
     * Generate AI response based on user message
     * 
     * This is a fallback implementation using rule-based responses.
     * Replace with actual API calls to OpenAI, Gemini, or Claude for production.
     * 
     * @param userMessage The user's input message
     * @param chatHistory The conversation history for context
     * @return AI generated response
     */
    suspend fun generateResponse(
        userMessage: String,
        chatHistory: List<ChatMessage>,
        geminiApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        // If a Gemini API key is available, try online generation first
        val apiKey = geminiApiKey?.trim().orEmpty()
        if (apiKey.isNotEmpty()) {
            try {
                val response = GeminiService.generateResponse(apiKey, userMessage, chatHistory)
                if (response.isNotBlank()) return@withContext response
                // Do not mask with rule-based when API key is present; surface issue clearly
                return@withContext "Sorry, I couldn't get a response from Gemini. Please try again."
            } catch (e: Exception) {
                return@withContext "Gemini request failed: ${e.message ?: "unknown error"}"
            }
        }
        
        val lowerMessage = userMessage.lowercase()
        
        // Rule-based fallback responses
        when {
            lowerMessage.contains("hello") || lowerMessage.contains("hi") || 
            lowerMessage.contains("hey") -> {
                "Hello! I'm your waste management assistant. I can help you with recycling tips, waste classification, disposal advice, and environmental best practices. How can I assist you today?"
            }
            
            lowerMessage.contains("recycle") || lowerMessage.contains("recycling") -> {
                "Great question! Here are some recycling tips:\n\n" +
                "♻️ Separate materials before recycling\n" +
                "🔹 Clean containers to avoid contamination\n" +
                "🔹 Check local recycling guidelines\n" +
                "🔹 Remove caps and labels when possible\n" +
                "🔹 Flatten cardboard boxes to save space\n\n" +
                "What specific item would you like to recycle?"
            }
            
            lowerMessage.contains("plastic") -> {
                "Plastic recycling tips:\n\n" +
                "• Rinse and clean plastic containers\n" +
                "• Remove labels if possible\n" +
                "• Check recycling symbols (♻️ #1-7)\n" +
                "• Not all plastics can be recycled locally\n" +
                "• Consider reusable alternatives for single-use items\n\n" +
                "Did you know: Some plastic items like bags often require special drop-off locations?"
            }
            
            lowerMessage.contains("glass") -> {
                "Glass recycling guide:\n\n" +
                "🔹 Clean glass thoroughly before recycling\n" +
                "🔹 Remove metal lids and caps\n" +
                "🔹 Glass bottles and jars are typically recyclable\n" +
                "🔹 Windows, mirrors, and ceramics are NOT recyclable\n" +
                "🔹 Broken glass should be wrapped before disposal\n\n" +
                "Tip: Keep different colored glass separate if required by your facility."
            }
            
            lowerMessage.contains("organic") || lowerMessage.contains("compost") ||
            lowerMessage.contains("biodegradable") -> {
                "Organic waste management:\n\n" +
                "🌱 Compost fruits, vegetables, and yard waste\n" +
                "🔹 Avoid meat, dairy, and oily foods in compost\n" +
                "🔹 Keep compost balanced\n" +
                "🔹 Turn compost regularly for better decomposition\n" +
                "🔹 Can create rich fertilizer for your garden\n\n" +
                "Would you like tips on starting your own compost pile?"
            }
            
            lowerMessage.contains("hazardous") || lowerMessage.contains("battery") ||
            lowerMessage.contains("electronic") -> {
                "⚠️ Important: Hazardous waste disposal\n\n" +
                "Batteries, electronics, chemicals, and medications require special handling:\n\n" +
                "• Check for local hazardous waste collection events\n" +
                "• Electronics: Use e-waste recycling centers\n" +
                "• Batteries: Find battery recycling locations\n" +
                "• Medications: Return to pharmacies or approved collection sites\n" +
                "• Never dispose in regular trash or down drains\n\n" +
                "Safety first! Always follow local hazardous waste guidelines."
            }
            
            lowerMessage.contains("thank") || lowerMessage.contains("thanks") -> {
                "You're welcome! I'm here to help you with any waste management questions. Feel free to ask me anything about recycling, disposal, or sustainable practices! 🌍♻️"
            }
            
            lowerMessage.contains("reduce") || lowerMessage.contains("less waste") -> {
                "Reduce waste strategies:\n\n" +
                "✓ Bring reusable bags to stores\n" +
                "✓ Use a reusable water bottle\n" +
                "✓ Avoid single-use plastics\n" +
                "✓ Buy in bulk to reduce packaging\n" +
                "✓ Use digital receipts instead of paper\n" +
                "✓ Repair items instead of replacing\n" +
                "✓ Donate items you no longer need\n\n" +
                "Remember: The best waste is the waste we don't create!"
            }
            
            lowerMessage.contains("paper") || lowerMessage.contains("cardboard") -> {
                "Paper and cardboard recycling:\n\n" +
                "📄 Recycle newspapers, magazines, and office paper\n" +
                "📦 Flatten cardboard boxes for easier processing\n" +
                "🔹 Remove plastic tape and labels\n" +
                "🔹 Ensure paper is clean and dry\n" +
                "⚠️ Greasy pizza boxes may not be recyclable\n" +
                "🔹 Shredded paper may need special handling\n\n" +
                "Tip: One ton of recycled paper saves 17 trees!"
            }
            
            lowerMessage.contains("where") && lowerMessage.contains("recycle") -> {
                "Finding recycling locations:\n\n" +
                "• Check your local government website\n" +
                "• Use recycling center locator apps\n" +
                "• Contact your waste management provider\n" +
                "• Look for community recycling programs\n" +
                "• Some stores offer recycling for specific items\n\n" +
                "Many cities have online directories of recycling facilities!"
            }
            
            else -> {
                // Default response for unhandled queries
                "I understand you're asking about waste management. " +
                "I can help you with:\n\n" +
                "♻️ Recycling guidelines\n" +
                "🗑️ Waste classification\n" +
                "🌱 Composting advice\n" +
                "⚠️ Hazardous waste disposal\n" +
                "🔹 Disposal methods for specific materials\n" +
                "🌍 Environmental best practices\n\n" +
                "What would you like to know more about?"
            }
        }
    }
    
    /**
     * Get a welcome message for the chatbot
     */
    fun getWelcomeMessage(): ChatMessage {
        return ChatMessage(
            message = "👋 Hello! I'm your AI waste management assistant. " +
                    "I can help you with recycling, waste disposal, and environmental best practices. " +
                    "Ask me anything about waste management!",
            isUser = false
        )
    }
    
    /**
     * Extract keywords from message for better context understanding
     */
    private fun extractKeywords(message: String): List<String> {
        val keywords = listOf(
            "recycle", "plastic", "glass", "paper", "metal", "organic", "compost",
            "hazardous", "battery", "electronic", "reduce", "waste", "disposal"
        )
        return keywords.filter { message.lowercase().contains(it) }
    }
}

