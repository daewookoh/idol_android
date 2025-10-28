package net.ib.mn.core.data.repository.certificate

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.VoteCertificateModel

interface VoteCertificateRepository {
    suspend fun getVoteCertificate(idolId: Long?): Flow<BaseModel<List<VoteCertificateModel>>>
}