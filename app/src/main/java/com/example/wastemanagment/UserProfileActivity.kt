package com.example.wastemanagment

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wastemanagment.adapter.WasteHistoryAdapter
import com.example.wastemanagment.repository.UserRepository

class UserProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: WasteClassificationViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var wasteHistoryAdapter: WasteHistoryAdapter
    
    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var buttonSave: Button
    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var textViewNoHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        
        // Initialize ViewModel and repositories
        viewModel = ViewModelProvider(this)[WasteClassificationViewModel::class.java]
        userRepository = UserRepository(applicationContext)
        
        // Initialize views
        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        buttonSave = findViewById(R.id.buttonSave)
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory)
        textViewNoHistory = findViewById(R.id.textViewNoHistory)
        
        // Initialize ViewModel
        viewModel.loadWasteHistory()
        
        // Setup RecyclerView
        wasteHistoryAdapter = WasteHistoryAdapter()
        recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@UserProfileActivity)
            adapter = wasteHistoryAdapter
        }
        
        // Load user profile
        loadUserProfile()
        
        // Load waste history
        viewModel.loadWasteHistory()
        
        // Setup observers
        setupObservers()
        
        // Setup save button
        buttonSave.setOnClickListener {
            saveUserProfile()
        }
    }
    
    private fun loadUserProfile() {
        val user = userRepository.getCurrentUser()
        editTextName.setText(user.name)
        editTextEmail.setText(user.email)
    }
    
    private fun saveUserProfile() {
        val name = editTextName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        
        if (name.isEmpty()) {
            editTextName.error = "Name cannot be empty"
            return
        }
        
        if (email.isEmpty()) {
            editTextEmail.error = "Email cannot be empty"
            return
        }
        
        userRepository.updateProfile(name, email)
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
    }
    
    private fun setupObservers() {
        viewModel.wasteHistory.observe(this) { records ->
            if (records.isEmpty()) {
                textViewNoHistory.visibility = android.view.View.VISIBLE
                recyclerViewHistory.visibility = android.view.View.GONE
            } else {
                textViewNoHistory.visibility = android.view.View.GONE
                recyclerViewHistory.visibility = android.view.View.VISIBLE
                wasteHistoryAdapter.submitList(records)
            }
        }
        
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
}