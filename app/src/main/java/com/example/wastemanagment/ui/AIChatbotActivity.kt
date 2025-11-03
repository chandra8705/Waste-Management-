package com.example.wastemanagment.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wastemanagment.R
import com.example.wastemanagment.adapter.ChatMessageAdapter
import com.example.wastemanagment.model.ChatMessage
import com.example.wastemanagment.service.AIChatbotService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIChatbotActivity : AppCompatActivity() {

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var backButton: ImageButton

    private val chatAdapter = ChatMessageAdapter(mutableListOf())
    private val chatHistory = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_chatbot)

        initViews()
        setupRecyclerView()
        setupListeners()
        
        // Add welcome message
        val welcomeMessage = AIChatbotService.getWelcomeMessage()
        addMessage(welcomeMessage)
    }

    private fun initViews() {
        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupRecyclerView() {
        recyclerViewChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerViewChat.adapter = chatAdapter
    }

    private fun setupListeners() {
        buttonSend.setOnClickListener {
            sendMessage()
        }

        backButton.setOnClickListener {
            finish()
        }

        // Send on enter key
        editTextMessage.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && 
                event.action == android.view.KeyEvent.ACTION_DOWN) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val messageText = editTextMessage.text.toString().trim()
        
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        // Add user message to chat
        val userMessage = ChatMessage(messageText, isUser = true)
        addMessage(userMessage)
        chatHistory.add(userMessage)

        // Clear input
        editTextMessage.setText("")

        // Show loading indicator
        val loadingMessage = ChatMessage("Thinking...", isUser = false)
        addMessage(loadingMessage)

        // Generate AI response
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val aiResponse = withContext(Dispatchers.IO) {
                    val apiKey = getString(R.string.gemini_api_key)
                    AIChatbotService.generateResponse(messageText, chatHistory, apiKey)
                }

                // Remove loading message
                chatAdapter.removeLast()
                chatAdapter.notifyItemRemoved(chatAdapter.getMessageCount())

                // Add AI response
                val botMessage = ChatMessage(aiResponse, isUser = false)
                addMessage(botMessage)
                chatHistory.add(botMessage)

                // Scroll to bottom
                recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)

            } catch (e: Exception) {
                // Remove loading message on error
                chatAdapter.removeLast()
                chatAdapter.notifyItemRemoved(chatAdapter.getMessageCount())

                // Show error message
                Toast.makeText(this@AIChatbotActivity, 
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        chatAdapter.addMessage(message)
        recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
    }
}

