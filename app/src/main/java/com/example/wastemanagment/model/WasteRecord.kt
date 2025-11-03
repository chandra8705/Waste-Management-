package com.example.wastemanagment.model

import java.util.Date

/**
 * Model class for storing waste classification history
 */
data class WasteRecord(
    val id: String,
    val userId: String,
    val wasteType: String,
    val confidence: Float,
    val imageUri: String? = null,
    val disposalAdvice: String,
    val timestamp: Long = System.currentTimeMillis(),
    val location: GeoLocation? = null
)

/**
 * Simple geo-location data class
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)