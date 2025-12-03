package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Award Model
 * 이벤트/어워즈 정보
 *
 * old 프로젝트의 AwardModel과 동일한 구조
 * - mainFloatingImgUrl: 플로팅 버튼에 표시할 이벤트 이미지 URL
 */
data class AwardModel(
    @SerializedName("aggregated_title")
    val aggTitle: String? = null,

    @SerializedName("aggregated_desc")
    val aggDesc: String? = null,

    @SerializedName("realtime_title")
    val realtimeTitle: String? = null,

    @SerializedName("realtime_desc")
    val realtimeDesc: String? = null,

    @SerializedName("main_floating_image_url")
    val mainFloatingImgUrl: String? = null,

    @SerializedName("aggregated_image_url")
    val aggImgUrl: String? = null,

    @SerializedName("logo_dark_image_url")
    val logoDarkImgUrl: String? = null,

    @SerializedName("logo_light_image_url")
    val logoLightImgUrl: String? = null,

    @SerializedName("realtime_image_url")
    val realtimeImgUrl: String? = null,

    @SerializedName("desc")
    val desc: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("show_banner")
    val showBanner: String? = null,

    @SerializedName("banner_dark_image_url")
    val bannerDarkImgUrl: String? = null,

    @SerializedName("banner_light_image_url")
    val bannerLightImgUrl: String? = null,

    @SerializedName("banner_url")
    val bannerUrl: String? = null,

    @SerializedName("title")
    val awardTitle: String? = null,

    @SerializedName("prize")
    val prize: String? = null,

    @SerializedName("keyword")
    val keyword: String? = null,

    @SerializedName("result_title")
    val resultTitle: String? = null,

    @SerializedName("charts")
    val charts: List<AwardChartsModel>? = null
)

/**
 * Award Charts Model
 * 어워즈 카테고리 (남자 솔로, 여자 솔로, 남자 그룹, 여자 그룹 등)
 */
data class AwardChartsModel(
    @SerializedName("code")
    val code: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("subname")
    val subname: String? = null,

    @SerializedName("desc")
    val desc: String? = null,

    @SerializedName("example_title")
    val exampleTitle: String? = null,

    @SerializedName("example_desc")
    val exampleDesc: String? = null
)

/**
 * Awards API Response
 */
data class AwardsCurrentResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("award")
    val award: AwardModel? = null
)
