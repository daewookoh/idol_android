package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.model.BaseModel
import org.json.JSONObject
import javax.inject.Inject

class GetUserSelfUseCase @Inject constructor(
    private val repository: ConfigRepository
) {
    suspend operator fun invoke(ts: Int): Flow<BaseModel<JSONObject>> = repository.getUserSelf(ts)
}
