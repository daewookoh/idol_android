package net.ib.mn.model

import android.content.Context
import android.util.Log
import net.ib.mn.BuildConfig
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Util
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Created by parkboo on 2017. 10. 19..
 */
class ConfigModel {
    //    public static String coinsplayerMarketUrl;
    //    public static String coplAccountPhrase;
    //    public static String coplPhrase;
    var gcode: Int = 0

    //    public static boolean success;
    var version: Int = 0

    // cdn url
    var cdnUrl: String? = null
    var cdnUrlActor: String? = null // 애돌용
    var cdnUrlIdol: String? = null // 셀럽용
    @JvmField
    var inactiveBegin: Date? = null
    @JvmField
    var inactiveEnd: Date? = null
    var showAwardHistory: Boolean = false
    var showAwardTab: Boolean = false
    @JvmField
    var showLeagueTab: Boolean = false // 애돌용

    //live 스트리밍 탭 보여주기 여부
    var showLiveStreamingTab: Boolean = false

    //이달의 기적 탭 보여주기 여부
    var showMiracleTab: Boolean = false // 애돌용

    @JvmField
    var nasHeart: Int = 0
    var recommendHeart: Int = 0
    var articleMaxSize: Int = Const.MAX_GIF_FILE_SIZE
    @JvmField
    var earnWayPhrase: String? = null
    var expireHeartPhrase: String? = null
    var reportHeart: Int = 0
    var itemLevel: Int = 0 // 아이템 상점 구매가능 레벨
    @JvmField
    var cutLine: Int = 100 // 명예의전당 컷 등수

    // gaon
    @JvmField
    var awardBegin: Date? = null
    @JvmField
    var awardEnd: Date? = null
    @JvmField
    var votable: String = "A"

    //public AwardModel awardData;
    var earnWayNoticeId: Int = 306 // 하트모으는 방법 공지 id. 306=영어 공지 id

    private var context: Context? = null

    //하트박스 180629
    var specialEvent: String? = null

    //LG U+
    var showLg: String? = null

    var video_heart: Int = 0

    var scheduleAddLevel: Int = 0
    @JvmField
    var scheduleVoteLevel: Int = 0

    @JvmField
    var friendApiBlock: String? = null

    @JvmField
    var videoAd: String? = null

    //서버에서 보내주는 앱내에서  차단할  패키지 목록들을 받는다.
    var macroTools: JSONArray? = null

    // udp
    @JvmField
    var udp_broadcast_url: String = ""
    @JvmField
    var udp_stage: Int = 0

    //chatting
    var chat_url: String? = null

    //라이브 스트리밍  socket url
    var live_url: String? = null

    //채팅룸 개설시 필요한  heart
    var chatRoomHeart: Int = 0

    //채팅룸 개설시 필요한  diamond
    @JvmField
    var chatRoomDiamond: Int = 0

    //이모티콘 버전체크.
    @JvmField
    var emoticonUrl: String? = null
    @JvmField
    var emoticonVersion: Int = 0

    var leagueOpenDate: String? = null // 애돌

    // offerwall On/Off list
    var showOfferwallTabs: JSONArray? = null

    // 하트상점 구매가능 여부
    @JvmField
    var showHeartShop: Boolean = true

    // 스토어 이벤트 팝업 마커 플래그
    var showStoreEventMarker: String? = null

    // 무료 충전소 팝업 마커 플래그
    var showFreeChargeMarker: String? = null

    //public int earnWayNoticeId = 306; // 하트모으는 방법 공지 id. 306=영어 공지 id
    // 루키 인포 버튼
    var showRookieInfo: Int = 0

    // 기적 인포 버튼
    var showMiracleInfo: Int = 0

    // 메뉴 화면 공지사항 상단
    var menuNoticeMain: String? = null

    // 메뉴 화면 상정 상단
    var menuStoreMain: String? = null

    // 메뉴 화면 자게 상단
    var menuFreeBoardMain: String? = null

    var showTranslation: Boolean = false
    var translationLocales = ArrayList<String>()

    var videoAdDesc: String? = null

    var videoAdBonusHeart: Int = 0

    var inviteeHeart: Int = 0

    @Synchronized
    fun parse(jsonObject: JSONObject?) {
        if (jsonObject == null) {
            return
        }
        earnWayPhrase = jsonObject.optString("earn_way_phrase")
        expireHeartPhrase = jsonObject.optString("expire_heart_phrase")
        gcode = jsonObject.optInt("gcode")
        cdnUrl = jsonObject.optString("cdn_url")
        cdnUrlActor = jsonObject.optString("cdn_url_actor")
        cdnUrlIdol = jsonObject.optString("cdn_url_idol")
        // cdnUrlActor, cdnUrlIdol에 항상 적절한 값이 들어가 있도록 한다
        if (BuildConfig.CELEB) {
            cdnUrlActor = cdnUrl
        } else {
            cdnUrlIdol = cdnUrl
        }
        itemLevel = jsonObject.optInt("item_level")
        nasHeart = jsonObject.optInt("nas_heart")
        recommendHeart = jsonObject.optInt("recommend_heart")
        reportHeart = jsonObject.optInt("report_heart")
        //        success = jsonObject.optBoolean("success");
        version = jsonObject.optInt("version")
        showLg = jsonObject.optString("show_lguplus")
        video_heart = jsonObject.optInt("video_heart")
        scheduleAddLevel = jsonObject.optInt("sched_add_level")
        scheduleVoteLevel = jsonObject.optInt("sched_vote_level")
        friendApiBlock = jsonObject.optString("friend_api_block")
        videoAd = jsonObject.optString("video_ad")
        emoticonUrl = jsonObject.optString("emoticon_url")
        emoticonVersion = jsonObject.optInt("emoticon_version")

        macroTools = jsonObject.optJSONArray("macro_tools")
        videoAdDesc = jsonObject.optString("videoad_desc")

        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        try {
            inactiveBegin = formatter.parse(jsonObject.optString("inactive_begin"))
            inactiveEnd = formatter.parse(jsonObject.optString("inactive_end"))
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        showAwardHistory = "Y".equals(jsonObject.optString("show_award_history"), ignoreCase = true)
        showAwardTab = "Y".equals(jsonObject.optString("show_award_tab"), ignoreCase = true)
        if (!BuildConfig.CELEB) {
            showLeagueTab = "Y".equals(jsonObject.optString("show_league_tab"), ignoreCase = true)
            showMiracleTab = "Y".equals(jsonObject.optString("show_miracle"), ignoreCase = true)
        }

        //라이브 스트리밍 여부
        showLiveStreamingTab = "Y".equals(jsonObject.optString("show_live_tab"), ignoreCase = true)

        if (jsonObject.optInt("article_maxsize") > 0) {
            articleMaxSize = jsonObject.optInt("article_maxsize")
        }


        try {
            val transFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            transFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            awardBegin = transFormat.parse(jsonObject.optString("award_begin"))
            awardEnd = transFormat.parse(jsonObject.optString("award_end"))
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        v("jasdflajdslf::" + jsonObject.optString("votable"))
        votable = if (BuildConfig.DEBUG) {
            jsonObject.optString("votable")
        } else {
            jsonObject.optString("votable")
        }

        // udp
        udp_broadcast_url = jsonObject.optString("udp_broadcast_url")
        udp_stage = jsonObject.optInt("udp_stage")

        live_url = jsonObject.optString("live_url")

        chat_url = jsonObject.optString("chat_url")
        chatRoomHeart = jsonObject.optInt("chatroom_heart")
        chatRoomDiamond = jsonObject.optInt("chatroom_diamond")

        earnWayNoticeId = jsonObject.optInt("earn_way_notice_id", 306)

        if (!BuildConfig.CELEB) {
            leagueOpenDate = jsonObject.optString("league_open_date")
        }

        showOfferwallTabs = jsonObject.optJSONArray("show_offerwall_tabs")

        showHeartShop = !jsonObject.optString("show_heart_shop").equals("N", ignoreCase = true)

        showStoreEventMarker = jsonObject.optString("show_store_event_marker")
        showFreeChargeMarker = jsonObject.optString("show_free_charge_event_marker")

        showRookieInfo = jsonObject.optInt("rookie_info_event_id")

        showMiracleInfo = jsonObject.optInt("miracle_info_event_id")

        menuNoticeMain = jsonObject.optString("menu_notice_main")

        menuStoreMain = jsonObject.optString("menu_store_main")

        menuFreeBoardMain = jsonObject.optString("menu_board_main")

        showTranslation = jsonObject.optString("show_translation").equals("Y", ignoreCase = true)
        val jsonTranslationLocales = jsonObject.optJSONArray("translation_locales")
        if (jsonTranslationLocales != null) {
            translationLocales.clear()
            for (i in 0 until jsonTranslationLocales.length()) {
                translationLocales.add(jsonTranslationLocales.getString(i))
            }
        }

        inviteeHeart = jsonObject.optInt("invitee_heart", 0)

        Util.setPreference(context, PREF_CONFIG, jsonObject.toString())
    }

    companion object {
        private var instance: ConfigModel? = null

        var PREF_CONFIG: String = "config"

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context?): ConfigModel {
            if (instance == null) {
                instance = ConfigModel()
                instance!!.context = context

                // 기존 정보 불러오기
                val json = Util.getPreference(context, PREF_CONFIG)

                Util.log("ConfigModel getInstance:$json")

                if (json != null && json.isNotEmpty()) {
                    try {
                        val jsonObject = JSONObject(json)
                        instance!!.parse(jsonObject)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            return instance!!
        }
    }
}
