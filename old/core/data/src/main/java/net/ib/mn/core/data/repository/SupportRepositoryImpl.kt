/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.future
import net.ib.mn.core.data.BuildConfig
import net.ib.mn.core.data.api.SupportApi
import net.ib.mn.core.data.dto.SupportGiveDiamondDTO
import net.ib.mn.core.data.dto.SupportLikeDTO
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import javax.inject.Inject


class SupportRepositoryImpl @Inject constructor(
    private val supportApi: SupportApi
) : SupportRepository, BaseRepository() {
    override suspend fun getSupports(
        limit: Int,
        offset: Int,
        groupId: String?,
        orderBy: String?,
        status: String?,
        yearMonth: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var _groupId = groupId
            var _idolId: String? = null
            if( BuildConfig.CELEB ) {
                _idolId = groupId
                _groupId = null
            }
            val response = supportApi.getSupports(limit, offset, _idolId, _groupId, orderBy, status, yearMonth)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    /**
     * java에서 suspend fun 호출할 수 있게 해주는 헬퍼 함수
     */
    fun callGetSupports(
        limit: Int,
        offset: Int,
        groupId: String?,
        orderBy: String?,
        status: String?,
        yearMonth: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ): CompletableFuture<Unit> = GlobalScope.future { getSupports(limit, offset, groupId, orderBy, status, yearMonth, listener, errorListener) }

    override suspend fun getSupportDetail(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = supportApi.getSupportDetail(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun createSupport(
        idolId: Int,
        title: String,
        adId: String,
        utcDate: String,
        image: ByteArray?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var imagePart: MultipartBody.Part? = null
            image?.let {
                val requestFile = image.toRequestBody("image/jpeg".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("imagebin", "image.jpg", requestFile)
            }

            val response = supportApi.createSupport(
                idolId = MultipartBody.Part.createFormData("idol_id", idolId.toString()),
                title = MultipartBody.Part.createFormData("title", title),
                adId = MultipartBody.Part.createFormData("type_id", adId),
                utcDate = MultipartBody.Part.createFormData("d_day", utcDate),
                image = imagePart
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun giveDiamond(
        supportId: Int,
        diamond: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = supportApi.giveDiamond(SupportGiveDiamondDTO(supportId, diamond))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun like(
        supportId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = supportApi.like(SupportLikeDTO(supportId))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getTop5(
        supportId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = supportApi.getTop5(supportId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getInAppBanner(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = supportApi.getInAppBanner()
            processResponse(response, listener, errorListener)
        } catch (_: CancellationException) { // Job was cancelled 무시하기
        }
        catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}
