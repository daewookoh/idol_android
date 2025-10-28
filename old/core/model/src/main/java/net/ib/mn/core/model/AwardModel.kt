package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AwardModel(
    @SerialName("aggregated_title") var aggTitle: String? = null, // 누적순위 집계 타이틀
    @SerialName("aggregated_desc") var aggDesc: String? = null, // 누적순위 집계 설명
    @SerialName("realtime_title") var realtimeTitle: String? = null, // 실시간 순위 설명
    @SerialName("main_floating_image_url") var mainFloatingImgUrl: String? = null,
    @SerialName("aggregated_image_url") var aggImgUrl: String? = null,
    @SerialName("logo_dark_image_url") var logoDarkImgUrl: String? = null,
    @SerialName("logo_light_image_url") var logoLightImgUrl: String? = null,
    @SerialName("realtime_image_url") var realtimeImgUrl: String? = null,
    @SerialName("desc") var desc: String? = null,
    @SerialName("name") var name: String? = null,
    @SerialName("show_banner") var showBanner: String? = null,
    @SerialName("banner_dark_image_url") var bannerDarkImgUrl: String? = null,
    @SerialName("banner_light_image_url") var bannerLightImgUrl: String? = null,
    @SerialName("banner_url") var bannerUrl: String? = null,
    @SerialName("title") var awardTitle: String? = null,
    @SerialName("prize") var prize: String? = null,
    @SerialName("charts") var charts: List<AwardChartsModel>? = null,
    @SerialName("keyword") var keyword: String? = null,
    @SerialName("result_title") var resultTitle: String? = null,
    )
