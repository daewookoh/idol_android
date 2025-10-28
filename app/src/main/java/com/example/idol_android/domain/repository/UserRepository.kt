package com.example.idol_android.domain.repository

import com.example.idol_android.domain.model.Result
import com.example.idol_android.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for User data operations.
 * Following the Dependency Inversion Principle, domain layer defines the interface,
 * and data layer provides the implementation.
 */
interface UserRepository {
    /**
     * Get all users as a Flow.
     */
    fun getUsers(): Flow<Result<List<User>>>

    /**
     * Get a single user by ID.
     */
    suspend fun getUserById(userId: Int): Result<User>
}
