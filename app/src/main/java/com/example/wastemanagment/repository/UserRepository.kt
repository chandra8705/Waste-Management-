package com.example.wastemanagment.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.wastemanagment.model.User
import com.google.gson.Gson
import java.util.Date
import java.util.UUID


class UserRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        USER_PREFERENCES, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    

    fun getCurrentUser(): User {
        val userJson = sharedPreferences.getString(KEY_CURRENT_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            val newUser = User(
                id = UUID.randomUUID().toString(),
                name = "Guest User",
                email = "",
                createdAt = Date(),
                lastLoginAt = Date()
            )
            saveUser(newUser)
            newUser
        }
    }
    

    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_CURRENT_USER, userJson).apply()
    }
    

    fun updateLastLogin() {
        val user = getCurrentUser()
        user.lastLoginAt = Date()
        saveUser(user)
    }
    

    fun updateProfile(name: String, email: String, profileImageUrl: String? = null) {
        val user = getCurrentUser()
        user.name = name
        user.email = email
        if (profileImageUrl != null) {
            user.profileImageUrl = profileImageUrl
        }
        saveUser(user)
    }
    

    fun clearUserData() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER).apply()
    }
    
    companion object {
        private const val USER_PREFERENCES = "user_preferences"
        private const val KEY_CURRENT_USER = "current_user"
    }
}