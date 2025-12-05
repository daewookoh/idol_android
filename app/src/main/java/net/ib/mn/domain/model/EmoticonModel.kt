package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 이모티콘 세트 모델 (탭 표시용)
 */
data class EmoticonModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("emoji_url") val emojiUrl: String = "",
    @SerializedName("is_viewable") val isViewable: String = ""
)

/**
 * 이모티콘 상세 모델 (개별 이모티콘)
 */
data class EmoticonDetailModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("description") val description: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("order") val order: Int = 0,
    var emoticonSetId: Int = -1
)

/**
 * 이모티콘 세트 버전 모델
 */
data class EmoticonSetModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("version") val version: Int = 0,
    var isChanged: Boolean = true
)

/**
 * 이모티콘 세트 응답 모델
 */
data class EmoticonSetResponse(
    @SerializedName("emoticon_set") val emoticonSets: List<EmoticonModel> = emptyList(),
    val success: Boolean = false
)

/**
 * 이모티콘 상세 응답 모델
 */
data class EmoticonDetailResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("emoji_url") val emojiUrl: String = "",
    @SerializedName("is_viewable") val isViewable: String = "",
    @SerializedName("emoticons") val emoticons: List<EmoticonDetailModel> = emptyList(),
    val success: Boolean = false
)
