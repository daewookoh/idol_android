package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.Anniversary
import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class UpdateAnniversariesUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {

    operator fun invoke(cdnUrl: String, reqImageSize: String, anniversaryList: List<Anniversary>) =
        idolRepository.updateAnniversaries(cdnUrl, reqImageSize, anniversaryList)
}