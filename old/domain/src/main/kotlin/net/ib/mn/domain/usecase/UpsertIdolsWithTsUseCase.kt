package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class UpsertIdolsWithTsUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {

    operator fun invoke(idolList: List<Idol>, ts: Int) = idolRepository.upsertWithTs(idolList.toList(), ts)
}