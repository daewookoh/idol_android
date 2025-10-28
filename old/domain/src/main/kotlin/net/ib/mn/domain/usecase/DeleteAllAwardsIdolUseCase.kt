package net.ib.mn.domain.usecase

import net.ib.mn.domain.repository.AwardsRepository
import javax.inject.Inject

class DeleteAllAwardsIdolUseCase @Inject constructor(
    private val awardsRepository: AwardsRepository
) {
    operator fun invoke() = awardsRepository.deleteAll()
}