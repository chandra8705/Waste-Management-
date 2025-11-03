package com.example.wastemanagment.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.wastemanagment.model.WasteRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Repository class for managing waste classification history
 */
class WasteRecordRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        WASTE_RECORD_PREFERENCES, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    /**
     * Save a new waste classification record
     */
    fun saveRecord(record: WasteRecord) {
        val records = getAllRecords().toMutableList()
        records.add(0, record) // Add to the beginning of the list (most recent first)
        
        // Save the updated list
        val recordsJson = gson.toJson(records)
        sharedPreferences.edit().putString(KEY_WASTE_RECORDS, recordsJson).apply()
    }
    
    /**
     * Get all waste classification records for the current user
     */
    fun getAllRecords(): List<WasteRecord> {
        val recordsJson = sharedPreferences.getString(KEY_WASTE_RECORDS, null) ?: return emptyList()
        
        val type = object : TypeToken<List<WasteRecord>>() {}.type
        return gson.fromJson(recordsJson, type) ?: emptyList()
    }
    
    /**
     * Get waste classification records for a specific user
     */
    fun getRecordsForUser(userId: String): List<WasteRecord> {
        return getAllRecords().filter { it.userId == userId }
    }
    
    /**
     * Get waste classification records by waste type
     */
    fun getRecordsByWasteType(wasteType: String): List<WasteRecord> {
        return getAllRecords().filter { it.wasteType == wasteType }
    }
    
    /**
     * Create a new waste record
     */
    fun createRecord(
        userId: String,
        wasteType: String,
        confidence: Float,
        imageUri: String?,
        disposalAdvice: String
    ): WasteRecord {
        val record = WasteRecord(
            id = UUID.randomUUID().toString(),
            userId = userId,
            wasteType = wasteType,
            confidence = confidence,
            imageUri = imageUri,
            disposalAdvice = disposalAdvice
            // timestamp will use the default Date() constructor
        )
        saveRecord(record)
        return record
    }
    
    /**
     * Clear all waste records
     */
    fun clearAllRecords() {
        sharedPreferences.edit().remove(KEY_WASTE_RECORDS).apply()
    }
    
    companion object {
        private const val WASTE_RECORD_PREFERENCES = "waste_record_preferences"
        private const val KEY_WASTE_RECORDS = "waste_records"
    }
}