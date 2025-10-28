package net.ib.mn.domain.usecase.datastore

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class GetIdolChartCodesPrefsUseCase @Inject constructor(
    private val repository: IdolRepository
) {
    operator fun invoke() = repository.getIdolChartCodes()
}