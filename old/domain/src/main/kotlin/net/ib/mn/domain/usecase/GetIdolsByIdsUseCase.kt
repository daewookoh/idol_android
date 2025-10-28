package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class GetIdolsByIdsUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke(idList: List<Int>) = idolRepository.getIdolsByIds(idList)
}