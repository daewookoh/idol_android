package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.repository.certificate.VoteCertificateRepository
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.VoteCertificateModel
import javax.inject.Inject

class GetVoteCertificateUseCase @Inject constructor(
    private val repository: VoteCertificateRepository
) {

    suspend operator fun invoke(idolId: Long?): Flow<BaseModel<List<VoteCertificateModel>>> = repository.getVoteCertificate(idolId)
}