package net.ib.mn.data.remote.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 파일 업로드 관련 API
 * Old 프로젝트의 FilesApi 참고
 */
interface FilesApi {

    /**
     * Presigned URL 요청
     * AWS S3 업로드를 위한 presigned URL 및 credentials 받기
     */
    @GET("files/presigned_url/")
    suspend fun getPresignedUrl(
        @Query("bucket") bucket: String? = null,
        @Query("filename") filename: String? = null,
        @Query("width") width: Int? = null,
        @Query("height") height: Int? = null,
        @Query("image_hash") imageHash: String? = null,
        @Query("file_type") fileType: String? = null,
    ): Response<ResponseBody>

    /**
     * CDN에 파일 업로드
     * Presigned URL로 받은 credentials를 사용해 S3에 직접 업로드
     */
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
