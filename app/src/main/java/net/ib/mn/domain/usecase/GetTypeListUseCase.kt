package net.ib.mn.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * TypeList 조회 UseCase
 * old 프로젝트와 동일
 */
class GetTypeListUseCase @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Flow<List<TypeListModel>> =
        configRepository.getTypeList(forceRefresh)
}
