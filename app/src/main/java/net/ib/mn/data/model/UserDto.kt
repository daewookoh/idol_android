package net.ib.mn.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for User from API.
 * Uses Gson for JSON parsing (consistent with project's Retrofit configuration).
 */
data class UserDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("website")
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
