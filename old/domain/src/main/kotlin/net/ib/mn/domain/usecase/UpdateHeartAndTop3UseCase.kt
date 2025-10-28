package net.ib.mn.domain.usecase

import net.ib.mn.domain.model.IdolFiledData
import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

class UpdateHeartAndTop3UseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {

    operator fun invoke(cdnUrl:String, reqImageSize: String, idolFiledDataList: List<IdolFiledData>) = idolRepository.updateHeartAndTop3(cdnUrl, reqImageSize, idolFiledDataList)
}