package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.model.BaseModel
import org.json.JSONObject
import javax.inject.Inject

class GetEventUseCase @Inject constructor(
    private val repository: UsersRepository
) {
    suspend operator fun invoke(
        version: String,
        gmail: String,
        isVM: Boolean = false,
        isRooted: Boolean = false,
        deviceId: String,
    ): Flow<JSONObject> = repository.getEvent(
        version = version,
        gmail = gmail,
        isVM = isVM,
        isRooted = isRooted,
        deviceId = deviceId
    )
}
