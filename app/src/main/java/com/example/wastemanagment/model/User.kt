package com.example.wastemanagment.model

import java.util.Date

/**
 * User model class for storing user profile information
 */
data class User(
    val id: String,
    var name: String,
    var email: String,
    var profileImageUrl: String? = null,
    var createdAt: Date = Date(),
    var lastLoginAt: Date = Date()
)