package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class GetViewableIdolsUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke() = idolRepository.getViewableIdols()
}