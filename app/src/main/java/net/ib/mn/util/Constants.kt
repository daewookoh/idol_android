package net.ib.mn.util

import net.ib.mn.BuildConfig

/**
 * 앱 전역 상수 정의
 *
 * Multi-Flavor 지원 (old 프로젝트와 동일):
 * - app (idol_playstore): net.ib.mn
 * - onestore: com.exodus.myloveidol.twostore
 * - china: com.exodus.myloveidol.china
 * - celeb: com.exodus.myloveactor
 */
object Constants {

    // ============================================================
    // API Configuration (Flavor에 따라 자동 변경)
    // ============================================================

    /**
     * API Base URL
     * - idol (app, onestore, china): https://www.myloveidol.com/api/v1/
     * - celeb: https://www.myloveactor.com/api/v1/
     */
    val BASE_URL: String = BuildConfig.BASE_URL

    /**
     * Host URL (without /api/v1/)
     * - idol: https://www.myloveidol.com
     * - celeb: https://www.myloveactor.com
     */
    val HOST: String = BuildConfig.HOST

    /**
     * App ID (Flavor별로 다름)
     * - app: "" (빈 문자열)
     * - onestore: "twostore"
     * - china: "china"
     * - celeb: "4B418BC059C536ECE2DE206C3DC7C4D7"
     */
    val APP_ID: String = BuildConfig.APP_ID_VALUE

    /**
     * Kakao App Key (Flavor별로 다름)
     */
    val KAKAO_APP_KEY: String = BuildConfig.KAKAO_APP_KEY

    const val API_TIMEOUT = 30_000L // 30 seconds

    // ============================================================
    // Flavor Flags
    // ============================================================

    val IS_CELEB: Boolean = BuildConfig.CELEB
    val IS_ONESTORE: Boolean = BuildConfig.ONESTORE
    val IS_CHINA: Boolean = BuildConfig.CHINA

    // ============================================================
    // SharedPreferences / DataStore Keys
    // ============================================================

    // App Configuration
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_LANGUAGE = "language"
    const val KEY_APP_VERSION = "app_version"

    // User Data
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_TOKEN = "user_token"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"

    // Update Flags
    const val PREF_ALL_IDOL_UPDATE = "all_idol_update"
    const val PREF_DAILY_IDOL_UPDATE = "daily_idol_update"
    const val PREF_OFFICIAL_CHANNEL_UPDATE = "official_channel_update"
    const val PREF_SHOULD_CALL_OFFICIAL_CHANNEL = "should_call_official_channel"

    // Config Cache
    const val PREF_CONFIG_STARTUP = "config_startup"
    const val PREF_CONFIG_SELF = "config_self"
    const val PREF_BAD_WORDS = "bad_words"
    const val PREF_NOTICE_LIST = "notice_list"
    const val PREF_EVENT_LIST = "event_list"

    // User Status
    const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    const val KEY_USER_BLOCK_FIRST = "user_block_first"
    const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
    const val KEY_HEART_BOX_VIEWABLE = "heart_box_viewable"

    // Ad & Tracking
    const val PROPERTY_AD_ID = "ad_id"
    const val KEY_AD_TYPE_LIST = "ad_type_list"

    // Billing & IAB
    const val KEY_SUBSCRIPTION_STATUS = "subscription_status"
    const val KEY_IAB_PUBLIC_KEY = "iab_public_key"
    const val IAB_STATE_NORMAL = 1
    const val IAB_STATE_ABNORMAL = 0

    // Misc
    const val KEY_TIMEZONE = "timezone"
    const val KEY_AWARD_RANKING = "award_ranking"
    const val KEY_IS_VIDEO_SOUND_ON = "is_video_sound_on"

    // ============================================================
    // Intent Extras
    // ============================================================
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_IDOL = "from_idol"
    const val PARAM_INTENT_IN_HOUSE_OFFER_WALL_CELEB = "from_celeb"
    const val PARAM_DEEP_LINK = "deep_link"
    const val PARAM_SHARE_DATA = "share_data"

    // ============================================================
    // Build Flavors
    // ============================================================
    const val FLAVOR_CELEB = "celeb"
    const val FLAVOR_CHINA = "china"
    const val FLAVOR_ONESTORE = "onestore"

    // ============================================================
    // HTTP Status Codes (Custom)
    // ============================================================
    const val HTTP_UNAUTHORIZED = 401
    const val HTTP_MAINTENANCE = 88888
    const val HTTP_SUBSCRIPTION_ISSUE = 8000

    // ============================================================
    // Database
    // ============================================================
    const val DATABASE_NAME = "idol_database"
    const val DATABASE_VERSION = 1

    // ============================================================
    // Security
    // ============================================================
    // VM/Emulator detection packages
    val EMULATOR_PACKAGES = listOf(
        "com.google.android.launcher.layouts.genymotion",
        "com.bluestacks",
        "com.bignox.app",
        "com.vphone.launcher",
        "com.microvirt.launcher",
        "com.nox.app"
    )

    // ============================================================
    // Timing
    // ============================================================
    const val STARTUP_MIN_DURATION_MS = 2000L // Minimum 2 seconds on startup screen
    const val PROGRESS_UPDATE_INTERVAL_MS = 100L

    // ============================================================
    // Idol Types
    // ============================================================
    const val IDOL_TYPE_ALL = 0
    const val IDOL_TYPE_SOLO = 1
    const val IDOL_TYPE_GROUP = 2
    const val IDOL_TYPE_ACTOR = 3 // CELEB flavor only

    // ============================================================
    // Authentication / Login (old 프로젝트와 동일한 값)
    // ============================================================
    /**
     * LINE Channel ID (Flavor별로 다름)
     * - app: 1474745561
     * - onestore: 1594765998
     * - china: 1474745561
     * - celeb: 1537449973
     */
    val CHANNEL_ID: String = BuildConfig.LINE_CHANNEL_ID

    // Login domains (old Const.kt)
    const val DOMAIN_EMAIL = "email"
    const val DOMAIN_KAKAO = "kakao"
    const val DOMAIN_GOOGLE = "google"
    const val DOMAIN_LINE = "line"
    const val DOMAIN_FACEBOOK = "facebook"

    // Email postfix for social login (old Const.kt)
    const val POSTFIX_KAKAO = "@kakao.com"
    const val POSTFIX_LINE = "@line.com"
    const val POSTFIX_GOOGLE = "@google.com"
    const val POSTFIX_FACEBOOK = "@facebook.com"

    // ============================================================
    // Category Types (old Const.kt)
    // ============================================================
    const val TYPE_MALE: String = "M"
    const val TYPE_FEMALE: String = "F"
    const val PREF_DEFAULT_CATEGORY: String = "default_category"
}
