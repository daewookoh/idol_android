package com.example.idol_android.data.remote

import com.example.idol_android.data.model.UserDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API service interface.
 * Defines endpoints for JSONPlaceholder API.
 */
interface ApiService {

    @GET("users")
    suspend fun getUsers(): List<UserDto>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: Int): UserDto

    companion object {
        const val BASE_URL = "https://www.myloveidol.com/api/v1"
    }
}
