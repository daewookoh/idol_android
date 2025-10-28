package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class DeleteAllAndSaveIdolsUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke(idolList: List<Idol>) = idolRepository.deleteAllAndSaveIdols(idolList)
}