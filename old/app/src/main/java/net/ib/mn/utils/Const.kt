package net.ib.mn.utils

import android.os.Build
import android.os.Environment
import net.ib.mn.BuildConfig
import net.ib.mn.R
import java.util.TimeZone

object Const {
    const val TAPJOY_KEY: String = "jHtl2Dx2SfOsAnh5j4iaVgECVbeVQGNGY5bl0OPOjH8hh3B25eLW-TlU4ib4"
    val APPS_FLYER_KEY: String =
        if (BuildConfig.CELEB) "kHxZ9wEN8RxGwsPQYVR9Td" else "kHxZ9wEN8RxGwsPQYVR9Td"
    var ADLIB_API_KEY: String = "56fa9c840cf27038eed05f02"
    var GOOGLE_SERVER_KEY: String = "AIzaSyBr5Bo1k5SNrRTRX0NkYJ_obALray5W9p4"
    const val FACEBOOK_PLACEMENT_ID: String = "513234432489903_513234509156562"
    const val PREF_TV_TALK_ID: String = "exist_user_id"
    const val PREF_TV_TALK_TOKEN: String = "AccessToken"

    // 기념일/몰빵일 등이 설정되는 경우에 변경되는 time stamp 값
    const val PREF_ALL_IDOL_UPDATE: String = "all_idol_update"
    const val PREF_DAILY_IDOL_UPDATE: String = "daily_idol_update"
    const val PREF_OFFICIAL_CHANNEL_UPDATE: String = "sns_channel_update"
    const val PREF_SHOULD_CALL_OFFICIAL_CHANNEL: String = "should_call_official_channel"
    const val PREF_OFFICIAL_CHANNELS: String = "official_channels"
    const val PREF_NEVER_SHOW_EVENT: String = "never_show_event"
    const val PREF_NEVER_SHOW_CHANGE_INFO: String = "never_show_change_info"
    const val PREF_EVENT_READ: String = "event_read"
    const val PREF_NOTICE_READ: String = "notice_read"
    const val PREF_REQUEST_REVIEW: String = "request_review"
    const val PREF_EVENT_LIST: String = "event_list"
    const val PREF_FAMILY_APP_LIST: String = "family_app_list"
    const val PERF_UPLOAD_VIDEO_SPEC: String = "upload_video_spec"
    const val PREF_NOTICE_LIST: String = "notice_list"
    const val PREF_BURNING_TIME: String = "burning_time"
    const val PREF_HEART_BOX_VIEWABLE: String = "heart_box_viewable"
    const val PREF_GCM_PUSH_KEY: String = "push_key" // GCM push key
    const val PREF_DEFAULT_CATEGORY: String = "default_category" // default category key
    const val PREF_DEFAULT_TYPE: String = "default_type" //디폴트 타입 설정.
    const val PREF_LANGUAGE: String = "language" // 언어 설정
    const val PREF_END_POPUP: String = "end_popup" // 종료 팝업관련.
    const val MAIN_BOTTOM_TAB_CURRENT_INDEX: String =
        "main_bottom_tab_current_index" // 메인 하단 탭 현재 선택된 인덱스

    @JvmField
    val LOCAL_CACHE_FILE: String = if (BuildConfig.CELEB) ".exodus_actor" else ".exodus_idol"
    const val PREF_GUIDE: String = "guide_url" // 사용법 url
    const val PREF_APP_INSTALL: String = "app_install" // 앱 설치 시간. 구글 리뷰 팝업용
    const val PREF_SERVER_URL: String = "server_url"

    // 날짜가 변경될 때 기념일이 안바뀌거나 미션이 초기화되지 않거나 하는 현상을 방지
    const val PREF_SERVER_TIME: String = "server_time"

    // 미션 달성 여부
    const val PREF_MISSION_COMPLETED: String = "mission_completed"

    // 최애 설정 유도
    const val PREF_NEVER_SHOW_SET_MOST: String = "never_show_set_most"

    // 플랫폼 변경 시, 구독 상품 관련 알림
    const val PREF_SHOW_WARNING_PLATFORM_CHANGE_SUBSCRIPTION
            : String = "show_warning_playform_change_subscription"

    // 뉴프렌즈 유도
    const val PREF_SHOW_SET_NEW_FRIENDS: String = "showSetNewFriends"

    //친구한테 하트받기
    const val PREF_SHOW_TAKE_HEART_FRIENDS: String = "showTakeHeartFriends"

    // 도움 말풍선
    const val PREF_SHOW_SEARCH_IDOL: String = "show_search_idol"
    const val PREF_SHOW_CHANGE_GENDER: String = "show_change_gender"
    const val PREF_SHOW_SET_MOST_IN_SEARCH: String = "show_set_most_in_search"
    const val PREF_SHOW_PROFILE_ENABLE_UNDER_IMG: String = "show_profile_enable_under_img"
    const val PREF_SHOW_PROFILE_ENABLE_ABOVE_BTN: String = "show_profile_enable_above_btn"

    const val PREF_SHOW_FREE_TRIAL_DAILY_PACK: String = "show_free_trial_daily_pack"
    const val PREF_SHOW_ONEPICK_RESULT_CPATURE: String = "show_onepick_result_capture"
    const val PREF_SHOW_SELECT_MY_IDOL: String = "show_select_my_idol"
    const val PREF_SHOW_SELECT_MY_FAVORITE: String = "show_select_my_favorite"
    const val PREF_SHOW_CREATE_CHATTING_ROOM: String = "show_create_chatting_room"

    //채팅 개설 설명 팝업  다시 보지 않기
    const val PREF_SHOW_CREATE_CHAT_ROOM_INSTRUCTION: String = "show_create_chat_room_instruction"

    // 새로 생긴 feature 표시
    const val PREF_SHOW_ONEPICK_NEW: String = "show_onepick_new"

    // 친구 최대 허용
    const val PREF_FRIENDS_LIMIT: String = "friends_limit"

    // 즐겨찾기 화면에서 날짜 변경 됐는지 확인
    const val PREF_FAVORITE_DATE: String = "favorite_date"
    const val THE_NUMBER_OF_FRIENDS_LIMIT: Int = 300

    // 데이터 절약 모드
    const val PREF_DATA_SAVING: String = "data_saving"

    // 애니메이션 모드 온오프
    const val PREF_ANIMATION_MODE: String = "animation_mode"

    // 타입리스트
    const val PREF_TYPE_LIST: String = "type_list"

    //퀴즈 타입 리스트
    const val PREF_QUIZ_TYPE_LIST: String = "quiz_type_list"

    //아이돌 리스트
    const val PREF_QUIZ_IDOL_LIST: String = "pref_quiz_idol_list"

    // DB upgrade 했는지 여부 (is_viewable Y인 아이돌만 저장하던 것에서 모든 아이돌 저장으로)
    const val PREF_IDOL_DB_UPGRADED: String = "idol_db_upgraded"

    // 응답 받은 서버 시간 기록용
    const val PREF_SERVER_TS: String = "server_ts"

    const val PREF_IS_ABLE_ATTENDANCE: String = "attendance_is_able_attendance"
    const val PREF_IS_AGGREGATING_TIME: String = "is_aggregating_time"

    // 메인 화면 최애 이동 토스트.
    const val PREF_HAS_SHOWN_MY_FAV_TOAST: String = "has_shown_my_fav_toast"

    const val KEY_CATEGORY: String = "category"
    const val KEY_TYPE: String = "type"

    const val KEY_CHART_CODE: String = "chartCode"

    const val KEY_SOURCE_APP: String = "sourceApp"

    const val CACHE_TTL: Long = (1 * 24 * 60 * 60 // 1일
            ).toLong()

    const val SUPERREWARDS_HASHKEY: String = "trmflxpfocm.631319026758"
    const val PAYMENTWALL_KEY: String = "caaece1e64167c99aa5d6eb0c2da395b"
    const val PAYMENTWALL_KEY_TEST: String = "e4707f0818acd6e8d88fd7094f2bf0b1"
    const val PAYMENTWALL_SECRET_TEST: String = "8474e0084058ba96cb27b1f3c9e806d9"

    //    public final static String PAYMENTWALL_SECRET = "81cc5501cd9f6486fec688459f26e862";
    // admob native ad
    const val ADMOB_NATIVE_AD_UNIT_ID: String = "ca-app-pub-4951070488234097/1949582161"
    const val ADMOB_NATIVE_AD_ACTOR_UNIT_ID: String = "ca-app-pub-4951070488234097/4352165051"
    const val ADMOB_NATIVE_AD_TEST_UNIT_ID: String = "ca-app-pub-3940256099942544/2247696110"
    const val AD_MANAGER_ADAPTIVE_IDOL_AD_UNIT_ID: String = "/9176203,22915258703/1829623"
    const val AD_MANAGER_ADAPTIVE_CELEB_AD_UNIT_ID: String = "/9176203,22915258703/1829625"

    // KPOP AOS - Quiz ca-app-pub-4951070488234097/4514768160 퀴즈 기존 광고 단위 사용
    //KPOP AOS - OnePick ca-app-pub-4951070488234097/2107681793 테마/이미지픽
    //KPOP AOS - FreeCharge ca-app-pub-4951070488234097/9020606325 무료충전소
    //KPOP AOS - Profile ca-app-pub-4951070488234097/5920872096 프로필
    //KPOP AOS - HeartBox ca-app-pub-4951070488234097/4607790421 하트박스
    //KPOP AOS - LevelReward ca-app-pub-4951070488234097/5081361311 레벨 보상 후 팝업 3초 종료 후 자동 재생 기획 변경시
    val ADMOB_REWARDED_VIDEO_QUIZ_UNIT_ID: String =
        if (BuildConfig.CELEB) "ca-app-pub-4951070488234097/2619232567" else "ca-app-pub-4951070488234097/4514768160"
    @JvmField
    val ADMOB_REWARDED_VIDEO_ONEPICK_UNIT_ID: String =
        if (BuildConfig.CELEB) "ca-app-pub-4951070488234097/2440792080" else "ca-app-pub-4951070488234097/2107681793"
    val ADMOB_REWARDED_VIDEO_FREECHARGE_UNIT_ID: String =
        if (BuildConfig.CELEB) "ca-app-pub-4951070488234097/5416403233" else "ca-app-pub-4951070488234097/9020606325"
    val ADMOB_REWARDED_VIDEO_PROFILE_UNIT_ID: String =
        if (BuildConfig.CELEB) "ca-app-pub-4951070488234097/5688937806" else "ca-app-pub-4951070488234097/5920872096"
    val ADMOB_REWARDED_VIDEO_HEARTBOX_UNIT_ID: String =
        if (BuildConfig.CELEB) "ca-app-pub-4951070488234097/2682428689" else "ca-app-pub-4951070488234097/4607790421"
    val ADMOB_REWARDED_VIDEO_LEVELREWARD_UNIT_ID: String =
        if (BuildConfig.CELEB) "ca-app-pub-4951070488234097/2249220392" else "ca-app-pub-4951070488234097/5081361311"

    //"ca-app-pub-3940256099942544/2247696110"  -> native ad  테스트 용
    const val ADMOB_REWARDED_VIDEO_TEST_UNIT_ID: String =
        "ca-app-pub-3940256099942544/5224354917" // 비광 테스트용

    // 언어설정값 모아두기
    @JvmField
    val languages: IntArray = intArrayOf(
        R.string.language_default,
        R.string.language_korean,
        R.string.language_english,
        R.string.language_chinese_cn,
        R.string.language_chinese_tw,
        R.string.language_japanese,
        R.string.language_indonesian,
        R.string.language_thai,
        R.string.language_vietnamese,
        R.string.language_spanish,
        R.string.language_portuguese,
        R.string.language_germany,
        R.string.language_french,
        R.string.language_italian,
        R.string.language_arabic,
        R.string.language_persian,
        R.string.language_russian,
        R.string.language_turkish
    )

    val freeBoardLanguages: IntArray = intArrayOf(
        R.string.filter_all_language,
        R.string.language_korean,
        R.string.language_english,
        R.string.language_chinese_cn,
        R.string.language_chinese_tw,
        R.string.language_japanese,
        R.string.language_indonesian,
        R.string.language_thai,
        R.string.language_vietnamese,
        R.string.language_spanish,
        R.string.language_portuguese,
        R.string.language_germany,
        R.string.language_french,
        R.string.language_italian,
        R.string.language_arabic,
        R.string.language_persian,
        R.string.language_russian,
        R.string.language_turkish
    )

    val smallTalkLanguages: IntArray = intArrayOf(
        R.string.filter_all_language,
        R.string.language_korean,
        R.string.language_english,
        R.string.language_chinese_cn,
        R.string.language_chinese_tw,
        R.string.language_japanese,
        R.string.language_indonesian,
        R.string.language_thai,
        R.string.language_vietnamese,
        R.string.language_spanish,
        R.string.language_portuguese,
        R.string.language_germany,
        R.string.language_french,
        R.string.language_italian,
        R.string.language_arabic,
        R.string.language_persian,
        R.string.language_russian,
        R.string.language_turkish
    )

    // 언어 추가되면 추가해주기
    @JvmField
    val locales: Array<String> = arrayOf(
        "", "ko_KR", "en_US", "zh_CN", "zh_TW", "ja_JP", "in_ID", "th_TH", "vi_VN",
        "es_ES", "pt_BR", "de_DE", "fr_FR", "it_IT", "ar_001", "fa_IR", "ru_RU", "tr_TR"
    )

    val smallTalkLocales: Array<String> = arrayOf(
        "", "ko_KR", "en_US", "zh_CN", "zh_TW", "ja_JP", "in_ID", "th_TH", "vi_VN",
        "es_ES", "pt_BR", "de_DE", "fr_FR", "it_IT", "ar_001", "fa_IR", "ru_RU", "tr_TR"
    )

    // 5th menu
    const val FEATURE_5TH_MENU: Boolean = true

    // 인앱 결제 제한 앱내에서 제한 여부
    const val FEATURE_IAB_RESTRICTED: Boolean = false

    // 집계시간에 글 작성/수정 가능 여부
    const val FEATURE_WRITE_RESTRICTION: Boolean = false

    // vungle
    const val VUNGLE_PLACEMENT_ID: String = "KPOPSTA87465"

    // gaon
    const val PREF_PURCHASE_DATE: String = "purchase_date" // 인앱 구매 시간.

    const val RESPONSE_Y: String = "Y"

    const val PARMA_Y: String = "Y"
    const val PARMA_N: String = "N"

    const val ANALYTICS_GA_DEFAULT_ACTION_KEY: String = "ui_action"
    const val ANALYTICS_BUTTON_PRESS_ACTION: String = "button_press"

    const val ANALYTICS_SWITCH_ON: String = "switch_on"
    const val ANALYTICS_SWITCH_OFF: String = "switch_off"

    const val ANALYTICS_APP_LINK: String = "app_link"
    @JvmField
    val FILE_DIR: String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()

    const val IAB_STATE_NORMAL: String = "normal"
    const val IAB_STATE_ABNORMAL: String = "abnormal"

    const val SHOW_PUBLIC: String = "public" // 전체 공개
    const val SHOW_PRIVATE: String = "private" // 최애만 공개

    const val LEVEL_ADMIN: Int = 10
    const val LEVEL_MANAGER: Int = 30

    // 보안관 뱃지 구매자
    const val LEVEL_SHERIFF: Int = 20

    const val SOLO_ID: Int = 110
    const val GROUP_ID: Int = 111
    const val AWARDS_ID: Int = 112

    // 나중에 최고레벨이 변경되면 여기를 수정
    const val MAX_LEVEL: Int = 40

    @JvmField
    val LEVEL_HEARTS: IntArray = intArrayOf(
        0,
        100,
        1000,
        2000,
        3000,
        5000,
        7000,
        10000,
        20000,
        35000,
        50000,
        65000,
        80000,
        100000,
        120000,
        150000,
        190000,
        240000,
        300000,
        400000,
        500000,
        650000,
        800000,
        1000000,
        1500000,
        2000000,
        2500000,
        3000000,
        3500000,
        4000000,
        5000000,
        7000000,
        10000000,
        13000000,
        16000000,
        20000000,
        25000000,
        30000000,
        40000000,
        50000000,
        100000000
    )

    // 명예의전당 개인누적 최저 등수. 기존 50 -> 100
    //    public final static int MAX_HOF_RANK = 100; // -> CUT_LINE 사용으로 변경
    // drawer menu
    const val DRAWER_EVENT: String = "drawer_event"
    const val EXTRA_DRAWER_MESSAGE: String = "drawer"
    const val MESSAGE_CLOSE_DRAWER: Int = 1001

    // image size
    const val IMAGE_SIZE_STANDARD: Int = 1440 // 갤럭시 s10 사이즈를 기준으로 함
    const val IMAGE_SIZE_LOWEST_FOR_SUPPORT: String = "200x200"
    const val IMAGE_SIZE_LOWEST: String = "200x200"
    const val IMAGE_SIZE_LOW: String = "300x300"
    const val IMAGE_SIZE_MEDIUM: String = "600x600"
    const val IMAGE_SIZE_HIGH: String = "1500x1500"

    // 이용 가이드 링크
    val GUIDE_LINK: String =
        if (BuildConfig.CELEB) "https://namu.wiki/w/배우자(애플리케이션)" else "http://blog.naver.com/PostList.nhn?blogId=myloveidolactor&from=postList&categoryNo=8"

    // 배우/배우돌 카테고리 -> 남/여 로 변경 (170810). type은 사용하지 않고 category로 남녀 구분 => 개편전까지는 현상유지해야되서 다시 변경
    const val TYPE_ACTOR: String = "A"
    const val TYPE_ACTORDOL: String = "I"
    const val CATEGORY_MALE: String = "M"
    const val CATEGORY_FEMALE: String = "F"

    // onestore
    const val ONESTORE_APP_ID: String = "OA00731312"

    // 투표 후 갱신 시간 3초로 -> 17/11/28 2초로(투표창에서 이미 2초 대기함)
    const val DELAY_AFTER_VOTE: Int = 2000

    const val MINIMUM_COMMENT_LENGTH: Int = 5

    // 게시글 수정
    const val EXTRA_ARTICLE: String = "extra_article"

    // 게시글 작성 후 완료 노티 클릭시 프리톡으로 돌아오기 및 지정 태그 선택
    const val EXTRA_RETURN_TO: String = "extra_return_to"
    const val EXTRA_TAG_ID: String = "extra_tag_id"


    // gaon
    const val PREF_DEFAULT_CATEGORY_GAON: String = "default_category_gaon" // default category key
    const val GAONCHART_URL: String = "http://www.kpopawards.co.kr/" // default category key

    // MMS vote
    const val MMS_VOTE_ADDRESS: String = "18779815"

    // 게시글 작성시 글자수 제한
    const val MAX_ARTICLE_LENGTH: Int = 1000

    //게시글 제목 글자수 제한
    const val MAX_ARTICLE_TITLE_LENGTH: Int = 30

    //서포트 개설시 제목 글자수 제한
    const val MAX_SUPPORT_LENGTH: Int = 100

    //Faq 글자수 제한
    const val MAX_FAQ_LENGTH: Int = 3000

    // 지식돌
    const val IDOL_ID_KIN: Int = 99999
    const val IDOL_ID_FREEBOARD: Int = 99990

    // 비디오광고 preload
    const val FEATURE_VIDEO_AD_PRELOAD: Boolean = false // 광고 안나온다는 불평이 있어 일단 막음

    // BJ App 관련
    const val FEATURE_FRIENDS: Boolean = true // 친구기능 사용여부
    const val FEATURE_ITEMSHOP: Boolean = true // 아이템샵 사용여부
    const val FEATURE_CATEGORY: Boolean = true // 순위 남/여 카테고리 사용여부
    const val FEATURE_SHARE: Boolean = false //

    // 기부천사/기부요정 아이콘 최대 레벨
    const val MAX_ANGEL_FAIRY_COUNT: Int =
        5 - 1 // 기부천사,기부요정 5회 이상이면 최대레벨로 표시. 리소스 아이디+이 값으로 표시하므로 -1 해줌.

    // 하트 선물하기 기능 사용여부
    const val FEATURE_PRESENT_HEART: Boolean = false

    // 이미지 크롭을 내장 크롭 액티비티를 사용할지
    const val FEATURE_USE_INTERNAL_CROPPER: Boolean = false

    // login 개선
    const val FEATURE_AUTH2: Boolean = true
    const val DOMAIN_EMAIL: String = "email"
    const val DOMAIN_KAKAO: String = "kakao"
    const val DOMAIN_LINE: String = "line"
    const val DOMAIN_GOOGLE: String = "google"
    const val DOMAIN_FACEBOOK: String = "facebook"
    const val DOMAIN_WECHAT: String = "wechat"
    const val DOMAIN_QQ: String = "qq"

    const val POSTFIX_KAKAO: String = "@kakao.com"
    const val POSTFIX_LINE: String = "@line.com"
    const val POSTFIX_WECHAT: String = "@wechat.myloveidol.com"
    const val POSTFIX_QQ: String = "@qq.myloveidol.com"

    const val KEY_DOMAIN: String = "domain"

    // DB에서 저장하는 도메인 구별자
    const val DOMAIN_LETTER_EMAIL: String = "N"
    const val DOMAIN_LETTER_GOOGLE: String = "G"
    const val DOMAIN_LETTER_KAKAO: String = "K"
    const val DOMAIN_LETTER_LINE: String = "L"
    const val DOMAIN_LETTER_FACEBOOK: String = "F"
    const val DOMAIN_LETTER_WECHAT: String = "W"
    const val DOMAIN_LETTER_QQ: String = "Q"

    // api cooldown
    const val COOLDOWN_TIME: Int = 5000
    const val COOLDOWN_TIME_IDOLS: Int = 60 * 1000 * 60

    // idols api refresh time
    const val CACHE_REFRESH_TIME: Long = (60 * 1000 * 60).toLong()

    // api caching
    const val KEY_IDOLS_S: String = "idols_s"
    const val KEY_IDOLS_G: String = "idols_g"
    const val KEY_IDOLS: String = "idols_"
    const val KEY_GAON_NOW: String = "gaon_now" // 가온 현재순위
    const val KEY_GAON_AGG: String = "gaon_agg" // 가온 누적순위
    const val KEY_FAVORITE: String = "favorites/self"
    const val KEY_GAON_NOW_UPDATE: String = "gaon_now_u"

    // device id 변경 막기
    const val PREF_DEVICE_ID: String = "device_id"

    const val KEY_DARKMODE: String = "darkmode"

    // 움짤
    const val FEATURE_VIDEO: Boolean = true
    const val MAX_GIF_FILE_SIZE: Int = 50 // 50MB
    const val MAX_FAQ_FILE_SIZE: Int = 100 //100MB
    const val FEATURE_VIDEO_CACHE: Boolean = true
    const val VIDEO_READY_EVENT: String = "video_ready" // 움짤 재생전 검은화면 방지

    // https://exoplayer.dev/supported-devices.html
    const val EXOPLAYER_MIN_SDK: Int = Build.VERSION_CODES.KITKAT // android 4.4 폰에서 움짤 크래시 안되게

    // 이미지 최대 크기
    const val MAX_IMAGE_WIDTH: Int = 1500

    // 실험실
    const val PREF_USE_INTERNAL_PHOTO_EDITOR: String = "internal_photo_editor"
    const val PREF_USE_HAPTIC: String = "use_haptic"

    const val RECAPTCHA_SITE_KEY: String = "6Le3KTUUAAAAACHNQMapM3akhQaKkmobOcRMgnVP"

    // multipart/form-data
    const val USE_MULTIPART_FORM_DATA: Boolean = true

    const val PACKAGE_NAME_ACTOR: String = "com.exodus.myloveactor"
    const val PACKAGE_NAME_IDOL: String = "net.ib.mn"

    // 결제 후 영수증 검증되어야 다시 구매내역 불러오게
    const val PURCHASE_FINISHED_EVENT: String = "purchase_finished"

    // 2017 gaon award in menu
    const val EVENT_GAON_2016: String = "gaon2016"
    const val EVENT_GAON_2017: String = "gaon2017"
    const val EXTRA_GAON_TYPE: String = "extra_gaon_type" // 누적순위 변화 가온용
    const val EXTRA_GAON_CATEGORY: String = "extra_gaon_category" // 누적순위 변화 가온용
    const val EXTRA_GAON_EVENT: String = "extra_gaon_event" // 누적순위 변화 가온용

    const val TYPE_LOADING: String = "LOADING"


    // 움짤 프사
    const val USE_ANIMATED_PROFILE: Boolean = true
    const val PLAYER_START_RENDERING: String = "start_rendering"
    const val MSG_STOP_EXOPLAYER: String = "stop_exoplayer" // 프사 펼치기하면 재생중이던거 멈추게

    // 카테고리 관련
    const val REFRESH: String = "refresh"
    const val REFRESH_SUMMARY_RANKING: String = "refresh_summary_ranking"

    // 메인 랭킹 하단 탭 관련
    const val SOLO_LEAGUE_CHANGE: String =
        "solo_league_change" //솔로 리그가 바꼈을 경우, 그룹 랭킹도 바뀌도록 하기 위해 사용
    const val GROUP_LEAGUE_CHANGE: String =
        "group_league_change" //그룹 리그가 바꼈을 경우, 솔로 랭킹도 바뀌도록 하기 위해 사용

    const val AWARD_RANKING: String = "award_ranking"

    //라인
    const val LINE_REQUEST_CODE: Int = 9122

    //푸시 채널 아이디
    const val PUSH_CHANNEL_ID_DEFAULT: String = "myloveidol_service"
    const val PUSH_CHANNEL_ID_NOTICE: String = "myloveidol_notice"
    const val PUSH_CHANNEL_ID_HEART: String = "myloveidol_heart"
    const val PUSH_CHANNEL_ID_COMMENT: String = "myloveidol_comment"
    const val PUSH_CHANNEL_ID_FRIEND: String = "myloveidol_friend"
    const val PUSH_CHANNEL_ID_COUPON: String = "myloveidol_coupon"
    const val PUSH_CHANNEL_ID_SCHEDULE: String = "myloveidol_schedule"
    const val PUSH_CHANNEL_ID_SUPPORT: String = "myloveidol_support"
    const val PUSH_CHANNEL_ID_CHATTING_MSG: String = "myloveidol_chat_msg"
    const val PUSH_CHANNEL_ID_CHATTING_MSG_RENEW: String = "myloveidol_chat_msg_renew"

    const val PUSH_CHANNEL_ID_ARTICLE_POSTING: String = "myloveidol_article_posting"

    //quiz max default
    const val TODAY_QUIZ_MAX: Int = 50
    const val TODAY_QUIZ_MIN: Int = 10

    val TIME_ZONE_KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

    //LG App
    const val LGUPLUS_APP_NAME: String = "com.uplus.musicshow"
    const val LG_CODE: String = "LGCode"

    const val BAD_WORDS: String = "badwords"
    const val REPORTED_MESSAGES: String = "reported_messages"

    const val BOARD_TAGS: String = "boardTags"

    const val SELECTED_TAG_IDS: String = "selectedTagIds"
    const val SELECTED_ALL: String = "selected_all"

    // TV Talk
    const val TALK_AUTH: String = "auth"
    const val TALK_AUTH_SUCCESS: String = "auth_success"
    const val TALK_JOIN: String = "join"
    const val TALK_JOIN_OK: String = "join_ok"
    const val TALK_LEAVE: String = "leave"
    const val TALK_LOG: String = "log"
    const val TALK_REQ_LOG: String = "req_log"
    const val TALK_RECEIVE: String = "receive"
    const val TALK_SEND: String = "send"
    const val TALK_DELETE: String = "delete"
    const val TALK_REPORT: String = "report"
    const val TALK_DELETE_SUCCESS: String = "delete_success"
    const val TALK_LAST_MESSAGES: String = "last_messages"
    const val TALK_REQ_LAST_MESSAGES: String = "req_last_messages"
    const val TALK_JOIN_LAST_MESSAGES_SUBSCRIBE: String = "join_last_messages_subscribe"
    const val TALK_LAST_MESSAGES_UPDATE: String = "last_messages_update"
    const val TALK_LEAVE_LAST_MESSAGES_SUBSCRIBE: String = "leave_last_messages_subscribe"

    //Chatting
    const val CHAT_AUTH: String = "auth"
    const val CHAT_AUTH_COMPLETE: String = "authComplete"
    const val CHAT_AUTH_FAILED: String = "authFailed"
    const val CHAT_REQUEST_MESSAGES: String = "requestMessages"
    const val CHAT_RECEIVE_MESSAGES: String = "receiveMessages"
    const val CHAT_SYSTEM_COMMAND: String = "systemCommand"
    const val CHAT_SEND_MESSAGE: String = "sendMessage"
    const val CHAT_SYSTEM_MESSAGE: String = "systemMessage"
    const val CHAT_REQUEST_DELETE: String = "requestDelete"
    const val CHAT_DELETE_MESSAGE: String = "deleteMessage"
    const val CHAT_REPORT_MESSAGE: String = "reportMessage"
    const val CHAT_CHAANGE_STATE: String = "changeState"
    const val CHAT_REQUEST_OG_MESSAGE: String = "requestOGMessage"
    const val BROADCAST_MANAGER_MESSAGE: String = "broadcast_manager_message"
    const val BROADCAST_REFRESH_TOP3: String = "broadcast_refresh_top3"

    const val BROADCAST_VOTE: String = "broadcast_vote"

    // 기념일
    const val ANNIVERSARY_BIRTH: String = "Y" // 생일
    const val ANNIVERSARY_DEBUT: String = "E" // 데뷔일
    const val ANNIVERSARY_COMEBACK: String = "C" // 컴백일
    const val ANNIVERSARY_MEMORIAL_DAY: String = "D" // 기념일
    const val ANNIVERSARY_ALL_IN_DAY: String = "B" // 몰빵일


    // IN_APP_STORE_ITEM
    val STORE_ITEM_DAILY_PACK: String =
        if (BuildConfig.CELEB) "daily_pack_actor_android" else "daily_pack_android"

    // wechat
    const val WECHAT_APP_ID: String = "wx47eda3ee325fe059"
    const val PARAM_WECHAT_ACCESS_TOKEN: String = "WeChatAccessToken"
    const val PARAM_WECHAT_UNIONID: String = "UnionId"

    // QQ
    const val QQ_APP_ID: String = "1110113148"

    // Mobvista
    const val MOBVISTA_APP_ID: String = "103008"
    const val MOBVISTA_APP_KEY: String = "2196c1cae61d2164a79e0a1d905cb92b"
    const val MOBVISTA_REWARDED_VIDEO_UNIT_ID: String = "44884"
    const val MOBVISTA_NATIVE_UNIT_ID: String = "192292"


    // XG Push
    //    public static final String QQ_PUSH_APP_ID="IDeb289bc814a0d";
    //    public static final String QQ_PUSH_SECRET_KEY="9d094808a57eee9f3dde6a2ab73b146d";
    //    public static final String QQ_PUSH_ACCESS_ID="2100350281";
    //    public static final String QQ_PUSH_ACCESS_KEY="AIJ5988K7VDB";
    // Tutorial
    const val TUTORIAL_COMPLETED: String = "tutorial_completed"

    // 2020 soba
    const val EVENT_SOBA2020: String = "soba2020"

    //2022 heart Dream
    const val AWARD_2022: String = "dream2022"
    const val AWARD_2023: String = "2023hda"

    const val AWARD_MODEL: String = "awardModel_2"

    //AAA awards
    const val EVENT_AAA2020: String = "aaa2020"

    //yend 2023
    const val EVENT_YEND: String = "yend2023"

    const val AWARD_COOLDOWN_TIME: Int = 7000

    //Support
    const val AD_TYPE_LIST: String = "ad_type_list"
    const val SELECTED_AD_TYPE_IDS: String = "selected_ad_type_id"
    const val AD_GUIDANCE: String = "ad_description"

    // firebase udp event logging
    const val ANALYTICS_EVENT_UDP: String = "udp"

    // maio
    const val USE_MAIO: Boolean = true
    const val MAIO_MEDIA_ID: String = "mf11ca96ebbde446e012d216cbce350f0"

    // appLovin Max
    const val USE_APPLOVIN: Boolean = true
    val APPLOVIN_MAX_UNIT_ID: String =
        if (BuildConfig.CELEB) "1a3ef9a4962e5ae2" else "4285dc8fbfc4f51a"

    //Client TimeZone Checking
    const val TIME_ZONE: String = "client_time_zone"


    // 직업별  type
    const val ACTOR: String = "A"
    const val SINGER: String = "S"
    const val ENTERTAINER: String = "E"

    //AppDriver
    const val APPDRIVER_FRONTURL: String = "https://appdriver.jp/5/v1/index/"
    val APPDRIVER_APP_ID: String = if (BuildConfig.CELEB) "73119" else "73117"
    val REWARD_ID: String = if (BuildConfig.CELEB) "4789" else "4786"
    val APPDRIVER_KEY: String =
        if (BuildConfig.CELEB) "75e28e5416990a8e9edab19246a98c1d" else "4fe1381a3d9016b97c3c75215a4c452f"


    //광고 종류
    const val APPLOVIN: String = "applovin"
    const val MEZZO: String = "mezzo"
    const val IRONSOURCE: String = "ironsource"
    const val ADMOB: String = "admob"
    const val MINT: String = "mint"
    const val PANGLE: String = "pangle"
    const val TAP_JOY: String = "tapjoy"


    //vm 관련  로그 체크용  - accessiblity와  일반 vm detect를 구별한다.
    const val VM_DETECT_LOG: String = "user.device"
    const val ACCESSIBILITY_DETECT_LOG: String = "user.accessiblity2"

    //Chatting
    const val CHATTING_LIST_RESET: Int = 111
    const val CHATTING_EXPLODED: Int = 121
    var CHATTING_IS_PAUSE: Boolean = false

    //노티  id 관련 정리
    var NOTIFICATION_GROUP_ID_CHAT_MSG: Int = 111111

    //채팅 방 관련 처리
    var IS_CHAT_ACTIVITY_FIRST_RUNNING: Boolean = true
    var OG_REQUEST_COUNT_CHECK: Int = 0

    //메인 화면 카테고리 (셀럽)
    const val MAIN_CHECK_VIEW: String = "main_check_view"

    //중국 결제 method 분류
    const val PAYMENT_ALIPAY: Int = 0
    const val PAYMENT_UNIONPAY: Int = 1
    const val PAYMENT_WECHATPAY: Int = 2

    //노티피케이션 로컬 db 저장 중  가장 최근 create at 값 용 shared key
    var KEY_RECENT_NOTIFICATION_CREATE_DATE: String = "recent_notification_create_date"

    //나의 정보 화면 사용되는 request코드 정리
    var REQUEST_GO_SHOP: Int = 101
    var RESULT_CODE_FROM_SHOP: Int = 102
    var REQUEST_GO_FREE_CHARGE: Int = 104


    //auto click  max count
    var AUTO_CLICKER_MAX_COUNT: Int = 9

    //auto clicker 접근성 감지 패키지 로컬 캐싱용 prf key값
    var AUTO_CLICK_CACHE_PREF_KEY: String = "auto_click_cache_pref_key"

    //auto click으로 인한  밴이 확정용 값 ->  -1일 경우 더이상의 maxcount는 오르지 않는다.
    var AUTO_CLICKER_BAN_CONFIRM_VALUE: Int = -1
    var AUTO_CLICK_COUNT_PREFERENCE_KEY: String = "auto_click_count"

    //비광 타이머 endtime  default 값
    //edntime이  defualt 값이면  비디오 광고 보기가 가능
    @JvmField
    var DEFAULT_VIDEO_DISABLE_TIME: Long = -1L

    //비광 타이머  edntime preference key
    @JvmField
    var VIDEO_TIMER_END_TIME_PREFERENCE_KEY: String = "video_disable_end_time"


    //딥링크 들어갈때   서포트  detail 이랑  certify 비교용
    var SUPPORT_DETAIL: Int = 0
    var SUPPORT_CERTIFY: Int = 1

    //나의 정보 화면에서 피드 들어갈때  체크용 preference
    var MY_INFO_TO_FEED_PREFERENCE_KEY: String = "is_from_my_info"

    //최근 검색어 저장
    @JvmField
    var SEARCH_HISTORY: String = "search_history"

    //유저 차단 목록 저장
    var USER_BLOCK_LIST: String = "user_block_list"

    //유저 차단 목록 1번이라도 받아왔는지 체크용
    var USER_BLOCK_FIRST: String = "user_block_first"

    //설정에서 차단 유저 피드 1번이라도 들어갔을 때 유저 정보 저장
    var BLOCK_USER_INFO: String = "block_user_info"

    //게시글 신고 목록 저장
    var ARTICLE_REPORT_LIST: String = "article_report_list"

    //댓글 리포트
    var TYPE_COMMENT_REPORT: Int = 1

    //아티클 리포트
    var TYPE_ARTICLE_REPORT: Int = 2

    //이모티콘.
    var EMOTICON_SET: String = "emoticon_set"

    //이모티콘 버전.
    var EMOTICON_VERSION: String = "emoticon_version"

    //이모티콘 JSON정보.
    var EMOTICON_ALL_INFO: String = "emoticon_all_info"

    //키보드 높이.
    var KEYBOARD_HEIGHT: String = "keyboard_height"

    // 이모티콘 키보드 안보이는 현상 방지용
    var MINIMUM_EMOTICON_KEYBOARD_HEIGHT: Int = 400

    //검색어 히스토리  저장 최대  사이즈
    var MAXIMUM_SEARCH_HISTORY_LIST: Int = 10


    //searchResultActivity -> top3 업데이트 됬을때  여부 체크
    var RESULT_TOP3_UPDATED: Int = 1111
    var REQUEST_TOP3_UPDATED: Int = 1112

    //라이브 좋아요 최대 여부 preference key
    var KEY_LIVE_ID_LIKE_MAX: String = "key_live_id_like_max"

    //테마픽 가이드화면
    var THEME_PICK_GUIDE: String = "theme_pick_guide"

    // 개별 공지/이벤트 가져오기
    var TYPE_NOTICE: String = "notices"
    var TYPE_EVENT: String = "event_list"

    //Google place sdk(스케쥴 맵에서 검색창).
    const val GOOGLE_PLACE_KEY: String = "QUl6YVN5Q0pZdFd0NktkRjVWMVlaSnE4cEJTLXZFOEpqNDNIdlI0"

    //Youtube player sdk(IntroductionActivity에서 확인).
    const val YOUTUBE_DATA_KEY: String = "QUl6YVN5QnVPWWVjdWhBcW9zRnJleG9BT0FRc1NOa3BmZFJVY0w4"

    //무충 naswall fluv 처리 관련
    const val FLUV_IN_HOUSE_OFFER_WALL_ITEM_KEY: String = "fluv"
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_FLUV: String = "param_intent_inhouse_offerwall_fluv"
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_PACKAGE_FLUV: String =
        "param_intent_inhouse_offerwall_package_fluv"

    //무충 셀럽처리.
    const val CELEB_IN_HOUSE_OFFER_WALL_ITEM_KEY: String = "actor"
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_CELEB: String =
        "param_intent_inhouse_offerwall_celeb"

    //무충 애돌처리
    const val IDOL_IN_HOUSE_OFFER_WALL_ITEM_KEY: String = "idol"

    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_IDOL: String = "param_intent_inhouse_offerwall_idol"

    const val FILLIT_IN_HOUSE_OFFER_WALL_ITEM_KEY: String = "fillit"
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_FILLIT: String =
        "param_intent_inhouse_offerwall_fillit"

    const val MIO_IN_HOUSE_OFFER_WALL_ITEM_KEY: String = "mio"
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_MIO_PACKAGE: String = "package"
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_MIO_DEBUG: String = "debug"

    //ncloud object storage bucket명
    val NCLOUD_INQUIRY_BUCKET: String =
        if (BuildConfig.CELEB) "ncloud_actor_inquiry" else "ncloud_idol_inquiry"

    //ncloud test bucket
    val NCLOUD_TEST_BUCKET: String =
        if (BuildConfig.CELEB) "ncloud_actor_test_main" else "ncloud_idol_test_main"

    //ncloud articles bucket
    val NCLOUD_ARTICLES_BUCKET: String =
        if (BuildConfig.CELEB) "ncloud_actor_articles" else "ncloud_idol_articles"

    const val TYPE_SOLO: String = "S"
    const val TYPE_GROUP: String = "G"
    const val TYPE_GEN_4: String = "4"
    const val TYPE_MALE: String = "M"
    const val TYPE_FEMALE: String = "F"

    const val RESPONSE_IS_ACTIVE_TIME_1: Int = 1

    const val LEAGUE_S: String = "S"
    const val LEAGUE_A: String = "A"

    //외부 URL 웹뷰로 보여줄지 아님 웹브라우저로 보여 줄지.
    const val FEATURE_WEB_VIEW: Boolean = false

    //업로드 관련.
    const val ARTICLE_SERVICE_UPLOAD: String = "article_service_upload"

    @JvmField
    val DOWNLOAD_FILE_PATH_PREFIX: String =
        if (BuildConfig.CELEB) "Choeaedol_Celeb_" else "Choeaedol_"

    const val PREF_NEW_PICKS: String = "pref_new_picks"

    const val PREF_MOST_PICKS: String = "pref_most_picks"

    const val PREF_HELP_INFO: String = "pref_help_info"

    const val PREF_FIRST_OPEN: String = "pref_first_open"

    const val PREF_MOST_CHART_CODE: String = "pref_most_chart_code"

    const val IS_EMAIL_SIGNUP: String = "is_email_signup"

    const val CHART_CODE_FOR_CERTIFICATE: String = "certificate_chartCode"

    const val MSG_UNDER_MAINTENANCE: String = "msg_under_maintenance" // 점검중 푸시 누르는 경우 처리용

    const val IS_VIDEO_SOUND_ON: String = "is_video_sound_on"

    // 썸네일 추출용 user agent
    const val META_OG_USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val BLUR_SIZE: Float = 200f

    const val MY_FAVORITE_TAG_ID: Int = 999

    const val NON_FAVORITE_IDOL_ID: Int = 99980

    const val COMMENT_COUNT: String = "comment_count"
}
