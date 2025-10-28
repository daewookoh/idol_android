package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.repository.AwardsRepository
import javax.inject.Inject

class SaveAwardsIdolUseCase @Inject constructor(
    private val awardsRepository: AwardsRepository
) {
    operator fun invoke(idols: List<Idol>) = awardsRepository.insert(idols)
}