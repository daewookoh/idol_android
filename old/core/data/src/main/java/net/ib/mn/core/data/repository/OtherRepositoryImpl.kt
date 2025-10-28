/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.AuthApi
import net.ib.mn.core.data.api.BlocksApi
import net.ib.mn.core.data.api.CouponApi
import net.ib.mn.core.data.api.EmoticonApi
import net.ib.mn.core.data.api.ImagesApi
import net.ib.mn.core.data.api.InquiryApi
import net.ib.mn.core.data.api.MarketApi
import net.ib.mn.core.data.api.MiscApi
import net.ib.mn.core.data.api.MissionsApi
import net.ib.mn.core.data.api.FilesApi
import net.ib.mn.core.data.api.RedirectApi
import net.ib.mn.core.data.api.StampsApi
import net.ib.mn.core.data.api.TimestampApi
import net.ib.mn.core.data.di.WithoutPrefix
import net.ib.mn.core.data.dto.BlockUserDTO
import net.ib.mn.core.data.dto.ClaimMissionRewardDTO
import net.ib.mn.core.data.dto.DeleteFileDTO
import net.ib.mn.core.data.dto.InquiryDTO
import net.ib.mn.core.data.dto.PurchaseItemBurningDayDTO
import net.ib.mn.core.data.dto.ReportLogDTO
import net.ib.mn.core.data.dto.TakeCouponDTO
import net.ib.mn.core.data.dto.UploadFileDTO
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject


/**
 * @see
 * */

class InquiryRepositoryImpl @Inject constructor(
    private val inquiryApi: InquiryApi
) : InquiryRepository, BaseRepository() {
    override suspend fun postInquiry(
        category: String?,
        title: String,
        content: String,
        files: List<UploadFileDTO>,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = InquiryDTO(category, title, content, files)
            val response = inquiryApi.postInquiry(body)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getInquiries(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = inquiryApi.getInquiries()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class FilesRepositoryImpl @Inject constructor(
    private val presignedApi: FilesApi
) : FilesRepository, BaseRepository() {
    override suspend fun getPresignedUrl(
        bucket: String?,
        filename: String?,
        width: Int?,
        height: Int?,
        imageHash: String?,
        fileType: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = presignedApi.getPresignedUrl(bucket, filename, width, height, imageHash, fileType)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun deleteUploaded(
        bucket: String,
        savedFilename: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = presignedApi.deleteUploaded(
                DeleteFileDTO(
                    bucket = bucket,
                    savedFilename = savedFilename
                )
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun writeCdn(
        url: String,
        AWSAccessKeyId: String,
        acl: String,
        key: String,
        policy: String,
        signature: String,
        file: ByteArray,
        filename: String,
        mimeType: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = presignedApi.writeCdn(
                url = url,
                AWSAccessKeyId = MultipartBody.Part.createFormData("AWSAccessKeyId", AWSAccessKeyId),
                acl = MultipartBody.Part.createFormData("acl", acl),
                key = MultipartBody.Part.createFormData("key", key),
                policy = MultipartBody.Part.createFormData("policy", policy),
                signature = MultipartBody.Part.createFormData("signature", signature),
                file = MultipartBody.Part.createFormData(
                    "file",
                    filename,
                    file.toRequestBody(mimeType.toMediaTypeOrNull())
                )
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class ItemShopRepositoryImpl @Inject constructor(
    private val marketApi: MarketApi
) : ItemShopRepository, BaseRepository() {
    override suspend fun getItemShopList(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = marketApi.getItemShopList()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun purchaseItemBurningDay(
        burningDay: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = marketApi.purchaseItemBurningDay(PurchaseItemBurningDayDTO(burningDay))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class CouponRepositoryImpl @Inject constructor(
    private val couponApi: CouponApi
) : CouponRepository, BaseRepository() {
    override suspend fun take(
        value: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = couponApi.take(TakeCouponDTO(value))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getTickets(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = couponApi.getTickets()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class TimestampRepositoryImpl @Inject constructor(
    @WithoutPrefix private val timestampApi: TimestampApi
) : TimestampRepository, BaseRepository() {
    override suspend fun get(
        listener: (Date?) -> Unit,
    ) {
        try {
            val response = timestampApi.get()
            val dateString = response.headers()["Date"]

            val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val date = dateString?.let { formatter.parse(it) }

            listener(date)
        } catch (e: Exception) {
            listener(null)
        }
    }
}

class BlocksRepositoryImpl @Inject constructor(
    private val blocksApi: BlocksApi
) : BlocksRepository, BaseRepository() {

    override suspend fun addBlock(
        userId: Int,
        reason: Int,
        block: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = BlockUserDTO(
                targetId = userId,
                reason = reason,
                block = block
            )
            val response = blocksApi.addBlock(body)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }

    override suspend fun getBlockList(
        idOnly: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = blocksApi.getBlockList(idOnly)
            processResponse(response, listener, errorListener)
        } catch (e: Throwable) {
            errorListener(e)
        }
    }
}

class MissionsRepositoryImpl @Inject constructor(
    private val missionsApi: MissionsApi
) : MissionsRepository, BaseRepository() {
    override suspend fun getWelcomeMission(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = missionsApi.getWelcomeMission()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun claimMissionReward(
        key: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = missionsApi.claimMissionReward(ClaimMissionRewardDTO(key))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class RedirectRepositoryImpl @Inject constructor(
    private val redirectApi: RedirectApi
) : RedirectRepository, BaseRepository() {
    override suspend fun redirect(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = redirectApi.redirect(url)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRepository, BaseRepository() {
    override suspend fun requestAccessToken(
        type: String?,
        clientId: String?,
        scope: String?,
        redirectUri: String?,
        approve: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = authApi.requestAccessToken(
                type = type,
                clientId = clientId,
                scope = scope,
                redirectUri = redirectUri,
                approve = approve
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class StampsRepositoryImpl @Inject constructor(
    private val stampsApi: StampsApi
) : StampsRepository, BaseRepository() {
    override suspend fun postStamp(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = stampsApi.postStamp()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getStampsCurrent(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = stampsApi.getStampsCurrent()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class EmoticonRepositoryImpl @Inject constructor(
    private val emoticonApi: EmoticonApi
) : EmoticonRepository, BaseRepository() {
    override suspend fun getEmoticon(
        id: Int?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = if (id == null) {
                emoticonApi.getEmoticonSet()
            } else {
                emoticonApi.getEmoticonSet(id)
            }
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class ImagesRepositoryImpl @Inject constructor(
    private val imagesApi: ImagesApi
) : ImagesRepository, BaseRepository() {
    override suspend fun uploadImage(
        image: ByteArray,
        size: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var imagePart: MultipartBody.Part? = null
            val requestFile = image.toRequestBody("image/jpeg".toMediaTypeOrNull())
            imagePart = MultipartBody.Part.createFormData("imagebin", "image.jpg", requestFile)

            val response = imagesApi.uploadImageMultipart(
                imagebin = imagePart,
                size = MultipartBody.Part.createFormData("size", size)
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun facedetect(
        image: ByteArray,
        category: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val requestFile = image.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("imagebin", "image.jpg", requestFile)

            val response = imagesApi.facedetect(
                imagebin = imagePart,
                category = MultipartBody.Part.createFormData("category", category),
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}

class MiscRepositoryImpl @Inject constructor(
    private val miscApi: MiscApi
) : MiscRepository, BaseRepository() {
    override suspend fun reportLog(
        key: String,
        text: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = miscApi.reportLog(ReportLogDTO(key, text))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getNotices(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = miscApi.getNotices()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getEvents(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = miscApi.getEvents()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getFAQs(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = miscApi.getFAQs()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getStore(
        goods: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = miscApi.getStore(goods)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getResource(
        resourceUri: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = miscApi.getResource(resourceUri)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}