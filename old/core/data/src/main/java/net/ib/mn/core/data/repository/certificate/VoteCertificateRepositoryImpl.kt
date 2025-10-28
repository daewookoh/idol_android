package net.ib.mn.core.data.repository.certificate

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.VoteHistoryApi
import net.ib.mn.core.data.mapper.toData
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.VoteCertificateModel
import retrofit2.HttpException
import javax.inject.Inject

class VoteCertificateRepositoryImpl @Inject constructor(
    private val voteHistoryApi: VoteHistoryApi,
) : VoteCertificateRepository {

    override suspend fun getVoteCertificate(idolId: Long?): Flow<BaseModel<List<VoteCertificateModel>>> =
        flow {
            try {
                val result = voteHistoryApi.getTodayCertificate(idolId)
                emit(
                    BaseModel(
                        data = result.data?.map { it.toData() },
                        success = result.success,
                        message = result.msg,
                        code = result.gcode
                    )
                )
            } catch (e: Exception) {
                throw e
            }
        }.catch { e ->
            emit(BaseModel(message = e.message, code = (e as? HttpException)?.code()))
        }
}