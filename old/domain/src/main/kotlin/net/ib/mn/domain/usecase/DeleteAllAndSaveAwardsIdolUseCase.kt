package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.repository.AwardsRepository
import javax.inject.Inject

class DeleteAllAndSaveAwardsIdolUseCase @Inject constructor(
    private val awardsRepository: AwardsRepository
) {
    operator fun invoke(idolList: List<Idol>) = awardsRepository.deleteAllAndInsert(idolList)
}