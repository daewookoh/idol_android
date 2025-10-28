package com.example.idol_android.domain.usecase

import com.example.idol_android.domain.model.Result
import com.example.idol_android.domain.model.User
import com.example.idol_android.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting users list.
 * Encapsulates business logic for fetching users.
 */
class GetUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<Result<List<User>>> {
        return userRepository.getUsers()
    }
}
