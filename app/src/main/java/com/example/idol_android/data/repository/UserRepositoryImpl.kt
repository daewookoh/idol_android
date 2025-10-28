package com.example.idol_android.data.repository

import com.example.idol_android.data.model.toDomain
import com.example.idol_android.data.remote.ApiService
import com.example.idol_android.domain.model.Result
import com.example.idol_android.domain.model.User
import com.example.idol_android.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementation of UserRepository.
 * Handles data operations and error handling.
 */
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {

    override fun getUsers(): Flow<Result<List<User>>> = flow {
        try {
            emit(Result.Loading)
            val users = apiService.getUsers().map { it.toDomain() }
            emit(Result.Success(users))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun getUserById(userId: Int): Result<User> {
        return try {
            val user = apiService.getUserById(userId).toDomain()
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
