/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.dto.UploadFileDTO
import org.json.JSONObject
import java.util.Date


/**
 * @see
 * */

interface InquiryRepository {
    suspend fun postInquiry(
        category: String?,
        title: String,
        content: String,
        files: List<UploadFileDTO>,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getInquiries(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface FilesRepository {
    suspend fun getPresignedUrl(
        bucket: String? = null,
        filename: String? = null,
        width: Int? = null,
        height: Int? = null,
        imageHash: String? = null,
        fileType: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun deleteUploaded(
        bucket: String,
        savedFilename: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun writeCdn(
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
    )
}

interface ItemShopRepository {
    suspend fun getItemShopList(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun purchaseItemBurningDay(
        burningDay: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface CouponRepository {
    suspend fun take(
        value: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getTickets(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface TimestampRepository {
    suspend fun get(
        listener: (Date?) -> Unit,
    )
}

interface BlocksRepository {
    suspend fun addBlock(
        userId: Int,
        reason: Int,
        block: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getBlockList(
        idOnly: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface MissionsRepository {
    suspend fun getWelcomeMission(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun claimMissionReward(
        key: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface RedirectRepository {
    suspend fun redirect(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface AuthRepository {
    suspend fun requestAccessToken(
        type: String?,
        clientId: String?,
        scope: String?,
        redirectUri: String?,
        approve: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface StampsRepository {
    suspend fun postStamp(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getStampsCurrent(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface EmoticonRepository {
    suspend fun getEmoticon(
        id: Int?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface ImagesRepository {
    suspend fun uploadImage(
        image: ByteArray,
        size: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun facedetect(
        image: ByteArray,
        category: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}

interface MiscRepository {
    suspend fun reportLog(
        key: String,
        text: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getNotices(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getEvents(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getFAQs(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getStore(
        goods: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getResource(
        resourceUri: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}