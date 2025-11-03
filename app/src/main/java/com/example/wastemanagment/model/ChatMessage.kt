package com.example.wastemanagment.model

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_USER = 0
        const val TYPE_BOT = 1
    }
}

