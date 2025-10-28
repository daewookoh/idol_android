package net.ib.mn.domain.usecase.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.ib.mn.data_resource.DataResource
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.repository.AppRepository
import javax.inject.Inject

class InitAdDataPrefsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    operator fun invoke(kstMidnight: Long): Flow<DataResource<Unit>> = flow {
        appRepository.getAdData().mapDataResource { it }.collect { result ->
            when (result) {
                is DataResource.Success -> {
                    val data = result.data
                    if (data != null) {
                        val (_, _, adDate) = data
                        if (adDate == 0L || kstMidnight > adDate) {
                            emitAll(appRepository.initAdData(kstMidnight))
                        } else {
                            emit(DataResource.Success(Unit)) // 초기화 필요 없음
                        }
                    } else {
                        emit(DataResource.Error(IllegalStateException("AdData is null")))
                    }
                }
                is DataResource.Error -> emit(DataResource.Error(result.throwable))
                is DataResource.Loading -> emit(DataResource.Loading())
            }
        }
    }
}