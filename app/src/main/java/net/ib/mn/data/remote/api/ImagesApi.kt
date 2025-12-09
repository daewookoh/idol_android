package net.ib.mn.data.remote.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * 이미지 업로드 관련 API
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/api/OtherApi.kt - ImagesApi
 *
 * 채팅방 이미지 업로드에 사용
 * - 업로드 성공 시: image_url, thumbnail_url, umjjal_url, thumb_height, thumb_width 반환
 */
interface ImagesApi {

    /**
     * 이미지 업로드
     *
     * @param imagebin 이미지 바이너리 데이터
     * @param size 이미지 크기 (예: "1500")
     * @return 업로드 결과:
     *  - success: Boolean
     *  - image_url: 원본 이미지 URL
     *  - thumbnail_url: 썸네일 URL
     *  - umjjal_url: 움짤 URL
     *  - thumb_height: 썸네일 높이
     *  - thumb_width: 썸네일 너비
     */
    @Multipart
    @POST("images/upload/")
    suspend fun uploadImage(
        @Part imagebin: MultipartBody.Part? = null,
        @Part size: MultipartBody.Part,
    ): Response<ResponseBody>
}
