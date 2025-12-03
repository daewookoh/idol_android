package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 이붙그램 갤러리 모델
 */
data class TrendsModel(
    @SerializedName("ref_date")
    val refDate: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    val heart: Long = 0,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("image_url2")
    val imageUrl2: String? = null,

    @SerializedName("image_url3")
    val imageUrl3: String? = null,

    @SerializedName("image_ver")
    val imageVer: Int = 0,

    @SerializedName("banner_url")
    val bannerUrl: String? = null,

    val rank: Int = 0,
    val difference: Int = 0,
    val status: String? = null  // increase, same, new, decrease
) {
    /**
     * 비디오 여부 확인
     */
    val isVideo: Boolean
        get() = bannerUrl?.endsWith(".mp4") == true

    /**
     * 원본 GIF URL 추출 (mp4에서 gif로 변환)
     */
    val gifUrl: String?
        get() = if (isVideo) {
            bannerUrl?.replace("_m_mv.mp4", "_o_mv.gif")
        } else null

    /**
     * 표시용 이미지 URL (썸네일 또는 원본)
     */
    val displayUrl: String?
        get() = bannerUrl ?: imageUrl ?: imageUrl2 ?: imageUrl3
}
