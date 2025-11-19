package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * /configs/self/ API Response
 *
 * 사용자별 설정 정보 (old 프로젝트 ConfigModel과 동일)
 * - UDP 브로드캐스트 URL
 * - CDN URL
 * - 각종 앱 설정값
 */
data class ConfigSelfResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("version")
    val version: Int = 0,

    // UDP 설정
    @SerializedName("udp_broadcast_url")
    val udpBroadcastUrl: String? = null,

    @SerializedName("udp_stage")
    val udpStage: Int = 0,

    // CDN 설정
    @SerializedName("cdn_url")
    val cdnUrl: String? = null,

    @SerializedName("cdn_url_actor")
    val cdnUrlActor: String? = null,

    @SerializedName("cdn_url_idol")
    val cdnUrlIdol: String? = null,

    // 채팅/라이브 설정
    @SerializedName("chat_url")
    val chatUrl: String? = null,

    @SerializedName("live_url")
    val liveUrl: String? = null,

    // 아이돌 데이터 업데이트 플래그
    @SerializedName("daily_idol_update")
    val dailyIdolUpdate: Boolean = false,

    @SerializedName("all_idol_update")
    val allIdolUpdate: Boolean = false,

    // 기타 설정들
    @SerializedName("show_award_history")
    val showAwardHistory: Boolean = false,

    @SerializedName("show_award_tab")
    val showAwardTab: Boolean = false,

    @SerializedName("show_league_tab")
    val showLeagueTab: Boolean = false,

    @SerializedName("show_live_streaming_tab")
    val showLiveStreamingTab: Boolean = false,

    @SerializedName("show_miracle_tab")
    val showMiracleTab: Boolean = false,

    @SerializedName("nas_heart")
    val nasHeart: Int = 0,

    @SerializedName("recommend_heart")
    val recommendHeart: Int = 0,

    @SerializedName("report_heart")
    val reportHeart: Int = 0,

    @SerializedName("item_level")
    val itemLevel: Int = 0,

    @SerializedName("cut_line")
    val cutLine: Int = 100,

    @SerializedName("video_heart")
    val videoHeart: Int = 0,

    @SerializedName("schedule_add_level")
    val scheduleAddLevel: Int = 0,

    @SerializedName("schedule_vote_level")
    val scheduleVoteLevel: Int = 0,

    @SerializedName("show_heart_shop")
    val showHeartShop: Boolean = true,

    @SerializedName("show_rookie_info")
    val showRookieInfo: Int = 0,

    @SerializedName("show_miracle_info")
    val showMiracleInfo: Int = 0,

    // 메뉴 설정 (Menu configuration)
    @SerializedName("menu_notice_main")
    val menuNoticeMain: String? = null,  // "Y" = 아이콘 메뉴에 표시, "N" = 텍스트 메뉴에 표시

    @SerializedName("menu_store_main")
    val menuStoreMain: String? = null,  // "Y" = 아이콘 메뉴에 표시, "N" = 텍스트 메뉴에 표시

    @SerializedName("menu_board_main")
    val menuFreeBoardMain: String? = null,  // "Y" = 아이콘 메뉴에 표시, "N" = 텍스트 메뉴에 표시

    // 메뉴 뱃지 마커 (Menu badge markers)
    @SerializedName("show_store_event_marker")
    val showStoreEventMarker: String? = null,  // "Y" = 상점 이벤트 뱃지 표시

    @SerializedName("show_free_charge_event_marker")
    val showFreeChargeMarker: String? = null  // "Y" = 무료충전소 뱃지 표시
)
