package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 최애 정보 DTO (favorites/self/ API 응답)
 */
data class FavoriteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("idol") val idol: FavoriteIdolDto,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("resource_uri") val resourceUri: String? = null
)

/**
 * Favorite 내부의 Idol 정보
 */
data class FavoriteIdolDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("name_en") val nameEn: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_url2") val imageUrl2: String? = null,
    @SerializedName("image_url3") val imageUrl3: String? = null,
    @SerializedName("heart") val heart: Long? = null,
    @SerializedName("league") val league: String? = null,
    @SerializedName("rank") val rank: Int? = null,
    @SerializedName("type") val type: String? = null,  // "S" or "G"
    @SerializedName("group_id") val groupId: Int? = null,
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("top3") val top3: String? = null,
    @SerializedName("top3_type") val top3Type: String? = null,
    @SerializedName("top3_image_ver") val top3ImageVer: String? = null,
    @SerializedName("most_count") val mostCount: Int? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("category") val category: String? = null,  // "M" or "F"
    @SerializedName("chart_codes") val chartCodes: List<String>? = null
) {
    /**
     * ChartCode 계산: category와 type 기반
     * 예: category=M, type=S → PR_S_M (남자 개인)
     */
    fun getChartCode(): String? {
        chartCodes?.firstOrNull()?.let { return it }
        val cat = category ?: return null
        val t = type ?: return null
        return "PR_${t}_${cat}"
    }
}

/**
 * 최애 목록 응답 DTO
 */
data class FavoritesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("objects") val objects: List<FavoriteDto>? = null
)
