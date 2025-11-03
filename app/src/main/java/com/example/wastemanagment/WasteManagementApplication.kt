package com.example.wastemanagment

import android.app.Application
import android.content.Context
import com.example.wastemanagment.utils.LocaleHelper

/**
 * Custom Application class to handle application-wide configurations
 * such as language settings
 */
class WasteManagementApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        // Apply saved language preference
        val languageCode = LocaleHelper.getLanguage(base)
        val context = LocaleHelper.setLocale(base, languageCode)
        super.attachBaseContext(context)
    }
}