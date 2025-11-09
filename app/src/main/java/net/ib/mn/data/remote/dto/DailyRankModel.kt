package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 일일 랭킹 모델
 *
 * hofs/ API 응답 데이터
 * old 프로젝트의 HallModel과 유사
 */
data class DailyRankModel(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("heart")
    val heart: Long = 0,

    @SerializedName("created_at")
    val createdAt: String = "",

    @SerializedName("resource_uri")
    val resourceUri: String? = null,  // old: "/api/v1/hofs/15013/" 형식

    // idol 중첩 객체 (old 프로젝트와 동일)
    @SerializedName("idol")
    val idol: IdolInfo? = null
) {
    /**
     * resource_uri에서 ID 추출 (old 프로젝트 HallModel.getResourceId()와 동일)
     * 예: "/api/v1/hofs/15013/" -> "15013"
     */
    fun getHofId(): Int {
        if (resourceUri.isNullOrEmpty()) return id
        val splitUri = resourceUri.split("/").filter { it.isNotEmpty() }
        return splitUri.lastOrNull()?.toIntOrNull() ?: id
    }
}

/**
 * Idol 정보 (hofs/ API 응답 내 중첩 객체)
 */
data class IdolInfo(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("name_en")
    val nameEn: String? = null,

    @SerializedName("group_id")
    val groupId: Int? = null,

    @SerializedName("anniversary")
    val anniversary: String? = null,  // "Y" (생일), "E" (데뷔), "C" (컴백), "D" (기념일)

    @SerializedName("anniversary_days")
    val anniversaryDays: Int? = null,

    @SerializedName("type")
    val type: String? = null,  // "S" (Solo) or "G" (Group)

    // rank는 클라이언트에서 계산 (0 = 1위, 1 = 2위, 2 = 3위, -1 = 순위 없음)
    var rank: Int = -1
)
