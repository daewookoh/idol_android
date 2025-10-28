package com.example.idol_android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for User from API.
 * Uses kotlinx.serialization for JSON parsing.
 */
@Serializable
data class UserDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("email")
    val email: String,
    @SerialName("username")
    val username: String,
    @SerialName("phone")
    val phone: String,
    @SerialName("website")
    val website: String
)

/**
 * Extension function to convert DTO to Domain model.
 */
fun UserDto.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        username = username,
        phone = phone,
        website = website
    )
}
