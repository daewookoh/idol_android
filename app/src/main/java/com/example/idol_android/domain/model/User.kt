package com.example.idol_android.domain.model

/**
 * Domain model for User.
 * This is a clean domain entity independent of any framework or library.
 */
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val username: String,
    val phone: String,
    val website: String
)
