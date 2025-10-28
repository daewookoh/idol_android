package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class DeleteAllIdolUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke() = idolRepository.deleteAllIdol()
}