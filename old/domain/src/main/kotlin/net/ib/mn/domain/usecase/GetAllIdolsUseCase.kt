package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class GetAllIdolsUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {

    operator fun invoke() = idolRepository.getAllIdols()
}