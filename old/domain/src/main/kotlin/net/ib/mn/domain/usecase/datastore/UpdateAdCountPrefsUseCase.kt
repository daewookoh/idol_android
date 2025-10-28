package net.ib.mn.domain.usecase.datastore

import net.ib.mn.domain.repository.AppRepository
import javax.inject.Inject

class UpdateAdCountPrefsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {

    operator fun invoke(currentCount: Int, maxCount: Int) =
        appRepository.updateAdCount(currentCount, maxCount)
}