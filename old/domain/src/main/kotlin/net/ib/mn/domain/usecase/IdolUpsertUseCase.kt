package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class IdolUpsertUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke(idol: Idol) = idolRepository.upsert(idol)
}