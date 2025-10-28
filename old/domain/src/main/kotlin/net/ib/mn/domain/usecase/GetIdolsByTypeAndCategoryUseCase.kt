package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class GetIdolsByTypeAndCategoryUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke(type: String?, category: String?) = idolRepository.getIdolByTypeAndCategory(type, category)
}