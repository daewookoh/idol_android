package net.ib.mn.domain.usecase.datastore

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class SetIdolChartCodePrefsUseCase @Inject constructor(
    private val repository: IdolRepository
) {
    operator fun invoke(data: Map<String, ArrayList<String>>) = repository.saveIdolChartCodes(data)
}