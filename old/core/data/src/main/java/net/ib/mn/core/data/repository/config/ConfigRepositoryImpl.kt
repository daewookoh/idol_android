/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.config

import net.ib.mn.core.data.api.ConfigsApi
import retrofit2.HttpException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.dto.OfferWallRewardRequest
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.model.AwardModel
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.CouponModel
import net.ib.mn.core.model.IdolApiModel
import net.ib.mn.core.model.ObjectsModel
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.core.model.UpdateInfoModel
import org.json.JSONArray
import org.json.JSONObject


/**
 * @see
 * */

class ConfigRepositoryImpl @Inject constructor(
    private val configsApi: ConfigsApi
) : ConfigRepository {

    override suspend fun getConfigStartup(): Flow<BaseModel<ObjectsModel>> = flow {
        try {
            val result = configsApi.getConfigStartup()
            when {
                result.success && result.data != null -> emit(BaseModel(data = result.data!!, success = true))
                result.success -> emit(BaseModel())
                else -> emit(BaseModel(message = result.msg, gcode = result.gcode))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getUpdate(): Flow<BaseModel<UpdateInfoModel>> = flow {
        try {
            val result = configsApi.getUpdateInfo()

            when {
                result.success -> emit(BaseModel(data = result, success = true))
                else -> emit(BaseModel(success = false))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getAdTypeList(): Flow<BaseModel<List<SupportAdTypeListModel>>> = flow {
        try {
            val result = configsApi.getAdTypeList()

            when {
                result.success -> emit(
                    BaseModel(
                        data = result.data,
                        description = result.description,
                        success = true
                    )
                )

                else -> emit(BaseModel(success = false, message = result.msg))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun updateTimeZone(currentTime: Map<String, String>): Flow<BaseModel<String>> = flow {
        try {
            val result = configsApi.updateTimeZone(currentTime)

            when {
                result.success -> emit(
                    BaseModel(
                        data = result.data,
                        success = true
                    )
                )

                else -> emit(BaseModel(success = false, message = result.msg))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getConfigSelf(): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = configsApi.getConfigSelf()
            emit(BaseModel(data = JSONObject(result), success = true))
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getAwardData(): Flow<AwardModel> = flow {

        val result = configsApi.getAwardData()

        try {
            when {
                result.success && result.award != null -> emit(
                    result.award!!
                )
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(AwardModel())
    }

    override suspend fun getAwardIdol(chartCode: String): Flow<BaseModel<List<IdolApiModel>>> = flow {
        try {
            val result = configsApi.getAwardIdol(chartCode)
            emit(BaseModel(data = result.data, success = result.success))
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getMessages(
        type: String,
        after: String?
    ): Flow<BaseModel<List<CouponModel>>> =
        flow {
            try {
                val result = configsApi.getMessages(type, after)
                emit(BaseModel(data = result.data, success = result.success))
            } catch (e: Exception) {
                throw e
            }
        }.catch { e ->
            emit(BaseModel(message = e.message, error = e))
        }

    override suspend fun getUserSelf(ts: Int): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = configsApi.getUserSelf(ts)
            emit(BaseModel(data = JSONObject(result), success = true))
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, code = (e as? HttpException)?.code()))
    }

    override suspend fun getUserStatus(): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = configsApi.getUserStatus()
            emit(BaseModel(data = JSONObject(result), success = true))
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getOfferWall(to: String): Flow<BaseModel<String>> = flow {
        try {
            val result = configsApi.getOfferWallReward(OfferWallRewardRequest(to))
            emit(BaseModel(data = result.data, success = result.success))
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getTypeList(): Flow<BaseModel<List<TypeListModel>>> = flow {
        try {
            val result = configsApi.getTypeList()
            emit(BaseModel(data = result.data, success = result.success))
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getBlocks(idOnly: String): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = configsApi.getBlocks(idOnly)
            emit(BaseModel(data = JSONObject(result ?: ""), success = true))
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun postVideoAdNotification(): Flow<BaseModel<Unit>> = flow {
        try {
            val result = configsApi.postVideoAdNotification()

            when {
                result.success -> emit(
                    BaseModel(
                        data = Unit,
                        success = true
                    )
                )

                else -> emit(BaseModel(success = false, message = result.msg))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }
}