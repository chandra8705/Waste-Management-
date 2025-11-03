package com.example.wastemanagment.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Helper class for handling language/locale changes in the app
 */
class LocaleHelper {
    companion object {
        private const val LANGUAGE_PREFERENCE = "language_preference"
        private const val SELECTED_LANGUAGE = "selected_language"
        
        /**
         * Set the app's locale to the one specified by the given language code
         */
        fun setLocale(context: Context, languageCode: String): Context {
            // Save selected language to preferences
            saveLanguagePreference(context, languageCode)
            
            // Update the locale for the app
            return updateResources(context, languageCode)
        }
        
        /**
         * Get the current locale from saved preferences
         */
        fun getLanguage(context: Context): String {
            val preferences = context.getSharedPreferences(LANGUAGE_PREFERENCE, Context.MODE_PRIVATE)
            return preferences.getString(SELECTED_LANGUAGE, Locale.getDefault().language) ?: Locale.getDefault().language
        }
        
        /**
         * Save the selected language code to preferences
         */
        private fun saveLanguagePreference(context: Context, languageCode: String) {
            val preferences = context.getSharedPreferences(LANGUAGE_PREFERENCE, Context.MODE_PRIVATE)
            preferences.edit().putString(SELECTED_LANGUAGE, languageCode).apply()
        }
        
        /**
         * Update the app's resources configuration with the new locale
         */
        private fun updateResources(context: Context, languageCode: String): Context {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            val resources = context.resources
            val configuration = Configuration(resources.configuration)
            
            configuration.setLocale(locale)
            
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.createConfigurationContext(configuration)
            } else {
                resources.updateConfiguration(configuration, resources.displayMetrics)
                context
            }
        }
        
        /**
         * Get a list of supported languages in the app
         */
        fun getSupportedLanguages(): List<LanguageItem> {
            return listOf(
                LanguageItem("en", "English"),
                LanguageItem("hi", "हिंदी"),
                LanguageItem("te", "తెలుగు"),
                LanguageItem("ml", "മലയാളം"),
                LanguageItem("pa", "ਪੰਜਾਬੀ"),
                LanguageItem("ne", "नेपाली")
            )
        }
    }
    
    /**
     * Data class to represent a language option
     */
    data class LanguageItem(val code: String, val displayName: String)
}