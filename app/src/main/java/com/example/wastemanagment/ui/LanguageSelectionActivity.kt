package com.example.wastemanagment.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wastemanagment.MainActivity
import com.example.wastemanagment.R
import com.example.wastemanagment.utils.LocaleHelper

/**
 * Activity for selecting the app language
 */
class LanguageSelectionActivity : AppCompatActivity() {
    
    private lateinit var languageSpinner: Spinner
    private val languageItems = LocaleHelper.getSupportedLanguages()
    private var currentLanguageCode: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)
        
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.language_settings)
        
        // Get current language
        currentLanguageCode = LocaleHelper.getLanguage(this)
        
        // Set up language spinner
        languageSpinner = findViewById(R.id.language_spinner)
        setupLanguageSpinner()
    }
    
    private fun setupLanguageSpinner() {
        // Create adapter with language display names
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languageItems.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
        
        // Set current language as selected
        val currentPosition = languageItems.indexOfFirst { it.code == currentLanguageCode }
        if (currentPosition != -1) {
            languageSpinner.setSelection(currentPosition)
        }
        
        // Handle language selection
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguageCode = languageItems[position].code
                if (selectedLanguageCode != currentLanguageCode) {
                    // Change language and restart app
                    changeLanguage(selectedLanguageCode)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun changeLanguage(languageCode: String) {
        // Update locale
        LocaleHelper.setLocale(this, languageCode)
        
        // Show confirmation message
        Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
        
        // Restart app to apply language change
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}