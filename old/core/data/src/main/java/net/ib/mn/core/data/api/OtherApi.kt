/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.di.WithoutPrefix
import net.ib.mn.core.data.dto.BlockUserDTO
import net.ib.mn.core.data.dto.ClaimMissionRewardDTO
import net.ib.mn.core.data.dto.DeleteFileDTO
import net.ib.mn.core.data.dto.InquiryDTO
import net.ib.mn.core.data.dto.PurchaseItemBurningDayDTO
import net.ib.mn.core.data.dto.ReportLogDTO
import net.ib.mn.core.data.dto.ScheduleVoteDTO
import net.ib.mn.core.data.dto.ScheduleWriteDTO
import net.ib.mn.core.data.dto.TakeCouponDTO
import net.ib.mn.core.data.model.ObjectsBaseDataModel
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url


/**
 * Quiz api
 * */

interface InquiryApi {

    /**
     * 현재 진행중인 차트 정보 가져오기
     * */
    @POST("inquiries/")
    suspend fun postInquiry(
        @Body body: InquiryDTO
    ): Response<ResponseBody>
    @GET("inquiries/self/")
    suspend fun getInquiries(
    ): Response<ResponseBody>
}

interface FilesApi {
    @GET("files/presigned_url/")
    suspend fun getPresignedUrl(
        @Query("bucket") bucket: String? = null,
        @Query("filename") filename: String? = null,
        @Query("width") width: Int? = null,
        @Query("height") height: Int? = null,
        @Query("image_hash") imageHash: String? = null,
        @Query("file_type") fileType: String? = null,
    ): Response<ResponseBody>

    @POST("files/delete_uploaded/")
    suspend fun deleteUploaded(
        @Body body: DeleteFileDTO
    ): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun writeCdn(
        @Url url: String,
        @Part AWSAccessKeyId: MultipartBody.Part? = null,
        @Part acl: MultipartBody.Part? = null,
        @Part key: MultipartBody.Part? = null,
        @Part policy: MultipartBody.Part? = null,
        @Part signature: MultipartBody.Part? = null,
        @Part file: MultipartBody.Part? = null,
    ): Response<ResponseBody>
}

interface MarketApi {
    @GET("market_lists/")
    suspend fun getItemShopList(
    ): Response<ResponseBody>

    @POST("market_lists/set_burning_day/")
    suspend fun purchaseItemBurningDay(
        @Body body: PurchaseItemBurningDayDTO
    ): Response<ResponseBody>
}

interface CouponApi {
    @POST("coupons/take/")
    suspend fun take(
        @Body body: TakeCouponDTO
    ): Response<ResponseBody>

    // 어워즈 입장 QR코드
    @GET("coupons/tickets/")
    suspend fun getTickets(
    ): Response<ResponseBody>
}

interface TimestampApi {
    @GET("/timestamp/")
    suspend fun get(
    ): Response<ResponseBody>
}

interface BlocksApi {
    // 차단
    @POST("blocks/add/")
    suspend fun addBlock(
        @Body body: BlockUserDTO
    ): Response<ResponseBody>

    @GET("blocks/")
    suspend fun getBlockList(
        @Query("id_only") idOnly: String,
    ): Response<ResponseBody>
}

interface MissionsApi {
    @GET("missions/welcome/")
    suspend fun getWelcomeMission(
    ): Response<ResponseBody>

    @POST("missions/reward/")
    suspend fun claimMissionReward(
        @Body body: ClaimMissionRewardDTO
    ): Response<ResponseBody>
}

interface RedirectApi {
    @GET("/redirect/")
    suspend fun redirect(
        @Query("url") url: String,
    ): Response<ResponseBody>
}

interface AuthApi {
    @GET("/api/ext/auth/token/")
    suspend fun requestAccessToken(
        @Query("type") type: String?,
        @Query("client_id") clientId: String?,
        @Query("scope") scope: String?,
        @Query("redirect_uri") redirectUri: String?,
        @Query("approve") approve: String?,
    ): Response<ResponseBody>
}

interface StampsApi {
    // 출석도장 찍기
    @POST("stamps/")
    suspend fun postStamp(
    ): Response<ResponseBody>

    @GET("stamps/current/")
    suspend fun getStampsCurrent(
    ): Response<ResponseBody>
}

interface EmoticonApi {
    @GET("emoticonset/{id}/")
    suspend fun getEmoticonSet(
        @Path("id") emoticonSetId: Int,
    ): Response<ResponseBody>

    @GET("emoticonset/")
    suspend fun getEmoticonSet(
    ): Response<ResponseBody>
}

interface ImagesApi {
    @Multipart
    @POST("images/upload/")
    suspend fun uploadImageMultipart(
        @Part imagebin: MultipartBody.Part? = null,
        @Part size: MultipartBody.Part,
    ): Response<ResponseBody>

    @Multipart
    @POST("images/facedetect/")
    suspend fun facedetect(
        @Part imagebin: MultipartBody.Part? = null,
        @Part category: MultipartBody.Part,
    ): Response<ResponseBody>
}

// 기타 잡다한 api
interface MiscApi {
    @POST("reports/log/")
    suspend fun reportLog(
        @Body body: ReportLogDTO
    ): Response<ResponseBody>

    @GET("notices/")
    suspend fun getNotices(
    ): Response<ResponseBody>

    @GET("event_list/")
    suspend fun getEvents(
    ): Response<ResponseBody>

    @GET("faqs/")
    suspend fun getFAQs(
    ): Response<ResponseBody>

    @GET("store/")
    suspend fun getStore(
        @Query("goods") goods: String?,
    ): Response<ResponseBody>

    @GET("{path}")
    suspend fun getResource(
        @Path("path", encoded = true) path: String,
    ): Response<ResponseBody>
}