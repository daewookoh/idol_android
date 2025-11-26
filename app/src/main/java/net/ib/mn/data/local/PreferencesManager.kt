package net.ib.mn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import net.ib.mn.util.Constants
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.ib.mn.domain.model.MostPicksModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 정보 데이터 클래스
 */
data class UserInfo(
    val id: Int,
    val email: String,
    val username: String,
    val nickname: String?,
    val profileImage: String?,
    val heart: Int?,
    val diamond: Int?,
    val strongHeart: Long?,
    val weakHeart: Long?,
    val level: Int?,
    val levelHeart: Long?,
    val power: Int?,
    val resourceUri: String?,
    val pushKey: String?,
    val createdAt: String?,
    val pushFilter: Int?,
    val statusMessage: String?,
    val ts: Int?,
    val itemNo: Int?,
    val domain: String?,
    val giveHeart: Int?
)


/**
 * DataStore를 사용한 Preferences 관리
 *
 * SharedPreferences의 현대적인 대체품
 * - Type-safe
 * - Coroutine 지원
 * - 에러 처리 개선
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "idol_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    // ============================================================
    // Keys
    // ============================================================
    companion object {
        // User Basic
        val KEY_USER_ID = intPreferencesKey(Constants.KEY_USER_ID)
        val KEY_USER_EMAIL = stringPreferencesKey(Constants.KEY_USER_EMAIL)
        val KEY_USER_USERNAME = stringPreferencesKey("user_username")
        val KEY_USER_NICKNAME = stringPreferencesKey("user_nickname")
        val KEY_USER_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        val KEY_USER_HEART = intPreferencesKey("user_heart")
        val KEY_ACCESS_TOKEN = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)

        // User Extended
        val KEY_USER_DIAMOND = intPreferencesKey("user_diamond")
        val KEY_USER_STRONG_HEART = longPreferencesKey("user_strong_heart")
        val KEY_USER_WEAK_HEART = longPreferencesKey("user_weak_heart")
        val KEY_USER_LEVEL = intPreferencesKey("user_level")
        val KEY_USER_LEVEL_HEART = longPreferencesKey("user_level_heart")
        val KEY_USER_POWER = intPreferencesKey("user_power")
        val KEY_USER_RESOURCE_URI = stringPreferencesKey("user_resource_uri")
        val KEY_USER_PUSH_KEY = stringPreferencesKey("user_push_key")
        val KEY_USER_CREATED_AT = stringPreferencesKey("user_created_at")
        val KEY_USER_PUSH_FILTER = intPreferencesKey("user_push_filter")
        val KEY_USER_STATUS_MESSAGE = stringPreferencesKey("user_status_message")
        val KEY_USER_TS = intPreferencesKey("user_ts")
        val KEY_USER_ITEM_NO = intPreferencesKey("user_item_no")
        val KEY_USER_DOMAIN = stringPreferencesKey("user_domain")
        val KEY_USER_GIVE_HEART = intPreferencesKey("user_give_heart")

        // Update Flags
        val KEY_ALL_IDOL_UPDATE = stringPreferencesKey(Constants.PREF_ALL_IDOL_UPDATE)
        val KEY_DAILY_IDOL_UPDATE = stringPreferencesKey(Constants.PREF_DAILY_IDOL_UPDATE)
        val KEY_SNS_CHANNEL_UPDATE = stringPreferencesKey("sns_channel_update")

        // ETag for caching
        val KEY_USER_SELF_ETAG = stringPreferencesKey("user_self_etag")

        // Tutorial Status
        val KEY_TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val KEY_FIRST_LOGIN = booleanPreferencesKey("first_login")

        // App Config
        val KEY_DARK_MODE = intPreferencesKey(Constants.KEY_DARK_MODE)
        val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey(Constants.KEY_IS_FIRST_LAUNCH)

        // Login Info
        val KEY_LOGIN_TIMESTAMP = longPreferencesKey("user_login_ts")
        val KEY_LOGIN_DOMAIN = stringPreferencesKey("user_login_domain")

        // Config Data (JSON strings)
        val KEY_BAD_WORDS = stringPreferencesKey("bad_words_json")
        val KEY_BOARD_TAGS = stringPreferencesKey("board_tags_json")
        val KEY_NOTICES = stringPreferencesKey("notices_json")
        val KEY_EVENTS = stringPreferencesKey("events_json")

        // ConfigSelf Data
        val KEY_LANGUAGE = stringPreferencesKey("config_self_language")
        val KEY_THEME = stringPreferencesKey("config_self_theme")
        val KEY_PUSH_ENABLED = booleanPreferencesKey("config_self_push_enabled")

        // Device Info
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")

        // Server Config
        val KEY_SERVER_URL = stringPreferencesKey("server_url")

        // UDP Config (from /configs/self/ API)
        val KEY_UDP_BROADCAST_URL = stringPreferencesKey("udp_broadcast_url")
        val KEY_UDP_STAGE = intPreferencesKey("udp_stage")
        val KEY_CDN_URL = stringPreferencesKey("cdn_url")
        val KEY_VIDEO_HEART = intPreferencesKey("video_heart")  // 비디오 광고 하트 보상

        // Menu Config (from /configs/self/ API)
        val KEY_MENU_NOTICE_MAIN = stringPreferencesKey("menu_notice_main")
        val KEY_MENU_STORE_MAIN = stringPreferencesKey("menu_store_main")
        val KEY_MENU_FREE_BOARD_MAIN = stringPreferencesKey("menu_free_board_main")
        val KEY_SHOW_STORE_EVENT_MARKER = stringPreferencesKey("show_store_event_marker")
        val KEY_SHOW_FREE_CHARGE_MARKER = stringPreferencesKey("show_free_charge_marker")
        val KEY_SHOW_LIVE_STREAMING_TAB = booleanPreferencesKey("show_live_streaming_tab")
        val KEY_IN_APP_BANNER_MENU = stringPreferencesKey("in_app_banner_menu")  // JSON 문자열

        // Category
        val KEY_DEFAULT_CATEGORY = stringPreferencesKey(Constants.PREF_DEFAULT_CATEGORY)
        val KEY_DEFAULT_CHART_CODE = stringPreferencesKey("default_chart_code")  // 기본 탭 선택용

        // Chart Idol IDs (5개 차트의 아이돌 ID 리스트)
        val KEY_CHART_SOLO_M_IDS = stringPreferencesKey("chart_solo_m_ids")  // SOLO 남성
        val KEY_CHART_SOLO_F_IDS = stringPreferencesKey("chart_solo_f_ids")  // SOLO 여성
        val KEY_CHART_GROUP_M_IDS = stringPreferencesKey("chart_group_m_ids")  // GROUP 남성
        val KEY_CHART_GROUP_F_IDS = stringPreferencesKey("chart_group_f_ids")  // GROUP 여성
        val KEY_CHART_GLOBAL_IDS = stringPreferencesKey("chart_global_ids")  // GLOBAL

        // Chart Rankings (5개 차트의 랭킹 데이터 - JSON 형식)
        val KEY_CHART_SOLO_M_RANKING = stringPreferencesKey("chart_solo_m_ranking")  // SOLO_M 랭킹 JSON
        val KEY_CHART_SOLO_F_RANKING = stringPreferencesKey("chart_solo_f_ranking")  // SOLO_F 랭킹 JSON
        val KEY_CHART_GROUP_M_RANKING = stringPreferencesKey("chart_group_m_ranking")  // GROUP_M 랭킹 JSON
        val KEY_CHART_GROUP_F_RANKING = stringPreferencesKey("chart_group_f_ranking")  // GROUP_F 랭킹 JSON
        val KEY_CHART_GLOBAL_RANKING = stringPreferencesKey("chart_global_ranking")  // GLOBAL 랭킹 JSON

        // User Cache Data (UserCacheRepository 백업용)
        val KEY_USER_SELF_DATA = stringPreferencesKey("user_self_data_json")  // UserSelfData JSON
        val KEY_MOST_IDOL_ID = intPreferencesKey("most_idol_id")  // 최애 아이돌 ID
        val KEY_MOST_IDOL_CATEGORY = stringPreferencesKey("most_idol_category")  // 최애 아이돌 카테고리 (M/F)
        val KEY_MOST_IDOL_CHART_CODE = stringPreferencesKey("most_idol_chart_code")  // 최애 아이돌 차트 코드
        val KEY_FAVORITE_IDOL_IDS = stringPreferencesKey("favorite_idol_ids_json")  // 즐겨찾기 아이돌 ID 리스트 JSON
        val KEY_MOST_PICKS_MODEL = stringPreferencesKey("most_picks_model_json")  // 픽 참여 정보 JSON

        // Free Board 선택 탭 저장
        val KEY_FREE_BOARD_SELECTED_TAG_ID = intPreferencesKey("free_board_selected_tag_id")

        // Notification
        val KEY_RECENT_NOTIFICATION_DATE = stringPreferencesKey("recent_notification_create_date")

        // Menu Badge (출석체크, 이벤트, 공지사항)
        val KEY_IS_ATTENDANCE_AVAILABLE = booleanPreferencesKey("is_attendance_available")
        val KEY_EVENT_READ_IDS = stringPreferencesKey("event_read_ids")  // 읽은 이벤트 ID 목록 (콤마 구분)
        val KEY_EVENT_LIST_IDS = stringPreferencesKey("event_list_ids")  // 서버 이벤트 ID 목록 (콤마 구분)
        val KEY_NOTICE_READ_IDS = stringPreferencesKey("notice_read_ids")  // 읽은 공지사항 ID 목록 (콤마 구분)
        val KEY_NOTICE_LIST_IDS = stringPreferencesKey("notice_list_ids")  // 서버 공지사항 ID 목록 (콤마 구분)

        // Ranking - 최애 이동 토스트
        val KEY_HAS_SHOWN_MY_FAV_TOAST = booleanPreferencesKey("has_shown_my_fav_toast")  // 최애 이동 토스트 표시 여부
    }

    // ============================================================
    // Read
    // ============================================================

    val accessToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ACCESS_TOKEN]
        }

    val loginEmail: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_USER_EMAIL]
        }

    val loginDomain: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_LOGIN_DOMAIN]
        }

    val allIdolUpdate: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ALL_IDOL_UPDATE]
        }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_IS_FIRST_LAUNCH] ?: true
        }

    val userSelfETag: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_USER_SELF_ETAG]
        }

    val dailyIdolUpdate: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DAILY_IDOL_UPDATE]
        }

    val snsChannelUpdate: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SNS_CHANNEL_UPDATE]
        }

    val tutorialCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_TUTORIAL_COMPLETED] ?: false
        }

    val userInfo: Flow<UserInfo?> = context.dataStore.data
        .map { preferences ->
            val userId = preferences[KEY_USER_ID]
            if (userId != null) {
                val info = UserInfo(
                    id = userId,
                    email = preferences[KEY_USER_EMAIL] ?: "",
                    username = preferences[KEY_USER_USERNAME] ?: "",
                    nickname = preferences[KEY_USER_NICKNAME],
                    profileImage = preferences[KEY_USER_PROFILE_IMAGE],
                    heart = preferences[KEY_USER_HEART],
                    diamond = preferences[KEY_USER_DIAMOND],
                    strongHeart = preferences[KEY_USER_STRONG_HEART],
                    weakHeart = preferences[KEY_USER_WEAK_HEART],
                    level = preferences[KEY_USER_LEVEL],
                    levelHeart = preferences[KEY_USER_LEVEL_HEART],
                    power = preferences[KEY_USER_POWER],
                    resourceUri = preferences[KEY_USER_RESOURCE_URI],
                    pushKey = preferences[KEY_USER_PUSH_KEY],
                    createdAt = preferences[KEY_USER_CREATED_AT],
                    pushFilter = preferences[KEY_USER_PUSH_FILTER],
                    statusMessage = preferences[KEY_USER_STATUS_MESSAGE],
                    ts = preferences[KEY_USER_TS],
                    itemNo = preferences[KEY_USER_ITEM_NO],
                    domain = preferences[KEY_USER_DOMAIN],
                    giveHeart = preferences[KEY_USER_GIVE_HEART]
                )
                android.util.Log.d("USER_INFO", "[PreferencesManager] DataStore emitting user info to collectors")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - ID: ${info.id}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Email: ${info.email}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Username: ${info.username}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Nickname: ${info.nickname}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - ProfileImage: ${info.profileImage}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Hearts: ${info.heart}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Diamond: ${info.diamond}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - StrongHeart: ${info.strongHeart}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - WeakHeart: ${info.weakHeart}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Level: ${info.level}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - LevelHeart: ${info.levelHeart}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Power: ${info.power}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Domain: ${info.domain}")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - TS: ${info.ts}")
                info
            } else {
                android.util.Log.d("USER_INFO", "[PreferencesManager] DataStore emitting null (no user info)")
                null
            }
        }

    val language: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_LANGUAGE]
        }

    val theme: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_THEME]
        }

    val pushEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_PUSH_ENABLED] ?: true
        }

    val deviceId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEVICE_ID]
        }

    val fcmToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_FCM_TOKEN]
        }

    val serverUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SERVER_URL]
        }

    // UDP Config
    val udpBroadcastUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_UDP_BROADCAST_URL]
        }

    val udpStage: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_UDP_STAGE] ?: 0
        }

    val cdnUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CDN_URL]
        }

    val videoHeart: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_VIDEO_HEART] ?: 0
        }

    val menuNoticeMain: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MENU_NOTICE_MAIN]
        }

    val menuStoreMain: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MENU_STORE_MAIN]
        }

    val menuFreeBoardMain: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MENU_FREE_BOARD_MAIN]
        }

    val showStoreEventMarker: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_STORE_EVENT_MARKER]
        }

    val showFreeChargeMarker: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_FREE_CHARGE_MARKER]
        }

    val showLiveStreamingTab: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_LIVE_STREAMING_TAB] ?: false
        }

    val inAppBannerMenu: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_IN_APP_BANNER_MENU]
        }

    val boardTags: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BOARD_TAGS]
        }

    val defaultCategory: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_CATEGORY] ?: Constants.TYPE_MALE
        }

    val defaultChartCode: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_CHART_CODE]
        }

    // ============================================================
    // Write
    // ============================================================

    suspend fun setAccessToken(token: String) {
        android.util.Log.d("USER_INFO", "[PreferencesManager] Writing access token to DataStore...")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Token preview: ${token.take(20)}...")

        context.dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = token
        }

        android.util.Log.d("USER_INFO", "[PreferencesManager] ✓ Access token written to DataStore")
    }

    suspend fun setAllIdolUpdate(timestamp: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ALL_IDOL_UPDATE] = timestamp
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun setDailyIdolUpdate(timestamp: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DAILY_IDOL_UPDATE] = timestamp
        }
    }

    suspend fun setSnsChannelUpdate(timestamp: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SNS_CHANNEL_UPDATE] = timestamp
        }
    }

    suspend fun setUserSelfETag(etag: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_SELF_ETAG] = etag
        }
    }

    suspend fun setUserInfo(
        id: Int,
        email: String,
        username: String,
        nickname: String?,
        profileImage: String?,
        heart: Int?,
        diamond: Int? = null,
        strongHeart: Long? = null,
        weakHeart: Long? = null,
        level: Int? = null,
        levelHeart: Long? = null,
        power: Int? = null,
        resourceUri: String? = null,
        pushKey: String? = null,
        createdAt: String? = null,
        pushFilter: Int? = null,
        statusMessage: String? = null,
        ts: Int? = null,
        itemNo: Int? = null,
        domain: String? = null,
        giveHeart: Int? = null
    ) {
        android.util.Log.d("USER_INFO", "[PreferencesManager] ========================================")
        android.util.Log.d("USER_INFO", "[PreferencesManager] Writing user info to DataStore...")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - ID: $id")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Email: $email")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Username: $username")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Nickname: $nickname")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - ProfileImage: $profileImage")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Heart: $heart")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Diamond: $diamond")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - StrongHeart: $strongHeart")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - WeakHeart: $weakHeart")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Level: $level")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - LevelHeart: $levelHeart")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Power: $power")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - ResourceUri: $resourceUri")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - PushKey: $pushKey")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - CreatedAt: $createdAt")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - PushFilter: $pushFilter")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - StatusMessage: $statusMessage")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - TS: $ts")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - ItemNo: $itemNo")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Domain: $domain")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - GiveHeart: $giveHeart")
        android.util.Log.d("USER_INFO", "[PreferencesManager] ========================================")

        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = id
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_USER_USERNAME] = username

            // Nullable 필드는 null이면 키 삭제, 아니면 저장
            if (nickname != null) preferences[KEY_USER_NICKNAME] = nickname else preferences.remove(KEY_USER_NICKNAME)
            if (profileImage != null) preferences[KEY_USER_PROFILE_IMAGE] = profileImage else preferences.remove(KEY_USER_PROFILE_IMAGE)
            if (heart != null) preferences[KEY_USER_HEART] = heart else preferences.remove(KEY_USER_HEART)
            if (diamond != null) preferences[KEY_USER_DIAMOND] = diamond else preferences.remove(KEY_USER_DIAMOND)
            if (strongHeart != null) preferences[KEY_USER_STRONG_HEART] = strongHeart else preferences.remove(KEY_USER_STRONG_HEART)
            if (weakHeart != null) preferences[KEY_USER_WEAK_HEART] = weakHeart else preferences.remove(KEY_USER_WEAK_HEART)
            if (level != null) preferences[KEY_USER_LEVEL] = level else preferences.remove(KEY_USER_LEVEL)
            if (levelHeart != null) preferences[KEY_USER_LEVEL_HEART] = levelHeart else preferences.remove(KEY_USER_LEVEL_HEART)
            if (power != null) preferences[KEY_USER_POWER] = power else preferences.remove(KEY_USER_POWER)
            if (resourceUri != null) preferences[KEY_USER_RESOURCE_URI] = resourceUri else preferences.remove(KEY_USER_RESOURCE_URI)
            if (pushKey != null) preferences[KEY_USER_PUSH_KEY] = pushKey else preferences.remove(KEY_USER_PUSH_KEY)
            if (createdAt != null) preferences[KEY_USER_CREATED_AT] = createdAt else preferences.remove(KEY_USER_CREATED_AT)
            if (pushFilter != null) preferences[KEY_USER_PUSH_FILTER] = pushFilter else preferences.remove(KEY_USER_PUSH_FILTER)
            if (statusMessage != null) preferences[KEY_USER_STATUS_MESSAGE] = statusMessage else preferences.remove(KEY_USER_STATUS_MESSAGE)
            if (ts != null) preferences[KEY_USER_TS] = ts else preferences.remove(KEY_USER_TS)
            if (itemNo != null) preferences[KEY_USER_ITEM_NO] = itemNo else preferences.remove(KEY_USER_ITEM_NO)
            if (domain != null) preferences[KEY_USER_DOMAIN] = domain else preferences.remove(KEY_USER_DOMAIN)
            if (giveHeart != null) preferences[KEY_USER_GIVE_HEART] = giveHeart else preferences.remove(KEY_USER_GIVE_HEART)
        }

        android.util.Log.d("USER_INFO", "[PreferencesManager] ✓ User info written to DataStore")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - DataStore will now emit new value to all collectors")
    }

    suspend fun setTutorialCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TUTORIAL_COMPLETED] = completed
        }
    }

    suspend fun setFirstLogin(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_LOGIN] = isFirst
        }
    }

    suspend fun setBadWords(badWords: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BAD_WORDS] = gson.toJson(badWords)
        }
    }

    suspend fun setBoardTags(tags: Any) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BOARD_TAGS] = gson.toJson(tags)
        }
    }

    suspend fun setNotices(notices: Any) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTICES] = gson.toJson(notices)
        }
    }

    suspend fun setEvents(events: Any) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EVENTS] = gson.toJson(events)
        }
    }

    suspend fun setLoginTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOGIN_TIMESTAMP] = timestamp
        }
    }

    suspend fun setLoginDomain(domain: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOGIN_DOMAIN] = domain
        }
    }

    suspend fun setLoginEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_EMAIL] = email
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    suspend fun setPushEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PUSH_ENABLED] = enabled
        }
    }

    suspend fun setDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVICE_ID] = deviceId
        }
    }

    suspend fun setFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FCM_TOKEN] = token
        }
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_URL] = url
        }
    }

    // UDP Config setters
    suspend fun setUdpBroadcastUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_UDP_BROADCAST_URL] = url
        }
    }

    suspend fun setUdpStage(stage: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_UDP_STAGE] = stage
        }
    }

    suspend fun setCdnUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CDN_URL] = url
        }
    }

    suspend fun setVideoHeart(videoHeart: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VIDEO_HEART] = videoHeart
        }
    }

    // Menu Config setters
    suspend fun setMenuNoticeMain(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[KEY_MENU_NOTICE_MAIN] = value
            } else {
                preferences.remove(KEY_MENU_NOTICE_MAIN)
            }
        }
    }

    suspend fun setMenuStoreMain(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[KEY_MENU_STORE_MAIN] = value
            } else {
                preferences.remove(KEY_MENU_STORE_MAIN)
            }
        }
    }

    suspend fun setMenuFreeBoardMain(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[KEY_MENU_FREE_BOARD_MAIN] = value
            } else {
                preferences.remove(KEY_MENU_FREE_BOARD_MAIN)
            }
        }
    }

    suspend fun setShowStoreEventMarker(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[KEY_SHOW_STORE_EVENT_MARKER] = value
            } else {
                preferences.remove(KEY_SHOW_STORE_EVENT_MARKER)
            }
        }
    }

    suspend fun setShowFreeChargeMarker(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[KEY_SHOW_FREE_CHARGE_MARKER] = value
            } else {
                preferences.remove(KEY_SHOW_FREE_CHARGE_MARKER)
            }
        }
    }

    suspend fun setShowLiveStreamingTab(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_LIVE_STREAMING_TAB] = value
        }
    }

    suspend fun setInAppBannerMenu(bannerListJson: String?) {
        context.dataStore.edit { preferences ->
            if (bannerListJson != null) {
                preferences[KEY_IN_APP_BANNER_MENU] = bannerListJson
            } else {
                preferences.remove(KEY_IN_APP_BANNER_MENU)
            }
        }
    }

    suspend fun setDefaultCategory(category: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_CATEGORY] = category
        }
    }

    suspend fun setDefaultChartCode(chartCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_CHART_CODE] = chartCode
        }
    }

    /**
     * 하트 값만 업데이트 (투표 후 사용)
     */
    suspend fun updateUserHearts(strongHeart: Long, weakHeart: Long) {
        android.util.Log.d("PreferencesManager", "💗 Updating user hearts in DataStore...")
        android.util.Log.d("PreferencesManager", "  - strongHeart: $strongHeart")
        android.util.Log.d("PreferencesManager", "  - weakHeart: $weakHeart")

        context.dataStore.edit { preferences ->
            preferences[KEY_USER_STRONG_HEART] = strongHeart
            preferences[KEY_USER_WEAK_HEART] = weakHeart
        }

        android.util.Log.d("PreferencesManager", "✅ User hearts updated in DataStore")
    }

    /**
     * 모든 데이터 삭제
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * 토큰 및 로그인 정보를 제외한 모든 데이터 삭제
     *
     * URL scheme으로 서버 변경 시 사용:
     * - 유저 정보, 하트, 다이아몬드 등 삭제
     * - 캐시 데이터 삭제 (BadWords, BoardTags, Notices, Events)
     * - 설정 데이터 삭제 (UDP, CDN, Category 등)
     *
     * 유지되는 데이터:
     * - ACCESS_TOKEN (로그인 토큰)
     * - USER_EMAIL (로그인 이메일)
     * - LOGIN_DOMAIN (로그인 도메인)
     * - SERVER_URL (서버 URL)
     */
    suspend fun clearAllExceptAuth() {
        android.util.Log.d("PreferencesManager", "🔄 Clearing all data except auth credentials...")

        context.dataStore.edit { preferences ->
            // 토큰 및 로그인 정보 백업 (serverUrl은 백업하지 않음 - 서버 변경 시 새 URL로 교체되어야 함)
            val savedToken = preferences[KEY_ACCESS_TOKEN]
            val savedEmail = preferences[KEY_USER_EMAIL]
            val savedDomain = preferences[KEY_LOGIN_DOMAIN]

            android.util.Log.d("PreferencesManager", "  - Backing up auth credentials:")
            android.util.Log.d("PreferencesManager", "    Token: ${if (savedToken != null) "present" else "null"}")
            android.util.Log.d("PreferencesManager", "    Email: $savedEmail")
            android.util.Log.d("PreferencesManager", "    Domain: $savedDomain")

            // 모든 데이터 삭제
            preferences.clear()

            // 토큰 및 로그인 정보 복원 (serverUrl은 복원하지 않음)
            savedToken?.let { preferences[KEY_ACCESS_TOKEN] = it }
            savedEmail?.let { preferences[KEY_USER_EMAIL] = it }
            savedDomain?.let { preferences[KEY_LOGIN_DOMAIN] = it }

            android.util.Log.d("PreferencesManager", "✅ All data cleared except auth credentials")
        }
    }

    // ============================================================
    // Chart Idol IDs
    // ============================================================

    /**
     * 차트별 아이돌 ID 리스트 저장
     * @param chartCode 차트 코드 (예: "SOLO_M", "GROUP_F", "GLOBAL")
     * @param idolIds 아이돌 ID 리스트
     */
    suspend fun saveChartIdolIds(chartCode: String, idolIds: List<Int>) {
        val idsJson = gson.toJson(idolIds)
        context.dataStore.edit { preferences ->
            when (chartCode) {
                "SOLO_M" -> preferences[KEY_CHART_SOLO_M_IDS] = idsJson
                "SOLO_F" -> preferences[KEY_CHART_SOLO_F_IDS] = idsJson
                "GROUP_M" -> preferences[KEY_CHART_GROUP_M_IDS] = idsJson
                "GROUP_F" -> preferences[KEY_CHART_GROUP_F_IDS] = idsJson
                "GLOBAL" -> preferences[KEY_CHART_GLOBAL_IDS] = idsJson
            }
        }
        android.util.Log.d("PreferencesManager", "✓ Saved ${idolIds.size} idol IDs for chart: $chartCode")
    }

    /**
     * 차트별 아이돌 ID 리스트 읽기
     * @param chartCode 차트 코드
     * @return 아이돌 ID 리스트 (없으면 빈 리스트)
     */
    suspend fun getChartIdolIds(chartCode: String): List<Int> {
        val key = when (chartCode) {
            "SOLO_M" -> KEY_CHART_SOLO_M_IDS
            "SOLO_F" -> KEY_CHART_SOLO_F_IDS
            "GROUP_M" -> KEY_CHART_GROUP_M_IDS
            "GROUP_F" -> KEY_CHART_GROUP_F_IDS
            "GLOBAL" -> KEY_CHART_GLOBAL_IDS
            else -> return emptyList()
        }

        return context.dataStore.data.map { preferences ->
            val idsJson = preferences[key]
            if (idsJson != null) {
                try {
                    gson.fromJson(idsJson, Array<Int>::class.java).toList()
                } catch (e: Exception) {
                    android.util.Log.e("PreferencesManager", "Failed to parse idol IDs for $chartCode", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }.first()
    }

    // ============================================================
    // Chart Rankings (JSON 기반 - DB 대체)
    // ============================================================

    /**
     * 차트별 랭킹 데이터 저장
     * @param chartCode 차트 코드 (예: "SOLO_M", "GROUP_F", "GLOBAL")
     * @param rankings 랭킹 아이템 리스트
     */
    suspend fun saveChartRanking(chartCode: String, rankings: List<net.ib.mn.ui.components.RankingItem>) {
        val rankingsJson = gson.toJson(rankings)
        context.dataStore.edit { preferences ->
            when (chartCode) {
                "SOLO_M", "PR_S_M" -> preferences[KEY_CHART_SOLO_M_RANKING] = rankingsJson
                "SOLO_F", "PR_S_F" -> preferences[KEY_CHART_SOLO_F_RANKING] = rankingsJson
                "GROUP_M", "PR_G_M" -> preferences[KEY_CHART_GROUP_M_RANKING] = rankingsJson
                "GROUP_F", "PR_G_F" -> preferences[KEY_CHART_GROUP_F_RANKING] = rankingsJson
                "GLOBAL", "GLOBALS" -> preferences[KEY_CHART_GLOBAL_RANKING] = rankingsJson
            }
        }
        android.util.Log.d("PreferencesManager", "✓ Saved ${rankings.size} ranking items for chart: $chartCode")
    }

    /**
     * 차트별 랭킹 데이터 실시간 리스닝 (Flow)
     * @param chartCode 차트 코드
     * @return Flow<List<RankingItem>> - 실시간 업데이트되는 랭킹 리스트
     */
    fun observeChartRanking(chartCode: String): Flow<List<net.ib.mn.ui.components.RankingItem>> {
        val key = when (chartCode) {
            "SOLO_M", "PR_S_M" -> KEY_CHART_SOLO_M_RANKING
            "SOLO_F", "PR_S_F" -> KEY_CHART_SOLO_F_RANKING
            "GROUP_M", "PR_G_M" -> KEY_CHART_GROUP_M_RANKING
            "GROUP_F", "PR_G_F" -> KEY_CHART_GROUP_F_RANKING
            "GLOBAL", "GLOBALS" -> KEY_CHART_GLOBAL_RANKING
            else -> return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return context.dataStore.data.map { preferences ->
            val rankingsJson = preferences[key]
            if (rankingsJson != null) {
                try {
                    gson.fromJson(rankingsJson, Array<net.ib.mn.ui.components.RankingItem>::class.java).toList()
                } catch (e: Exception) {
                    android.util.Log.e("PreferencesManager", "Failed to parse rankings for $chartCode", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    /**
     * 차트별 랭킹 데이터 일회성 읽기
     * @param chartCode 차트 코드
     * @return 랭킹 아이템 리스트 (없으면 빈 리스트)
     */
    suspend fun getChartRanking(chartCode: String): List<net.ib.mn.ui.components.RankingItem> {
        return observeChartRanking(chartCode).first()
    }

    // ============================================================
    // User Cache Data (UserCacheRepository 백업용)
    // ============================================================

    /**
     * UserSelfData 저장
     */
    suspend fun saveUserSelfData(userSelfData: net.ib.mn.data.remote.dto.UserSelfData) {
        val json = gson.toJson(userSelfData)
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_SELF_DATA] = json
        }
        android.util.Log.d("PreferencesManager", "✓ Saved UserSelfData to SharedPreference")
    }

    /**
     * UserSelfData 로드
     */
    suspend fun getUserSelfData(): net.ib.mn.data.remote.dto.UserSelfData? {
        val json = context.dataStore.data.first()[KEY_USER_SELF_DATA]
        return if (json != null) {
            try {
                gson.fromJson(json, net.ib.mn.data.remote.dto.UserSelfData::class.java)
            } catch (e: Exception) {
                android.util.Log.e("PreferencesManager", "Failed to parse UserSelfData", e)
                null
            }
        } else {
            null
        }
    }

    /**
     * 최애 아이돌 정보 저장
     */
    suspend fun saveMostIdolInfo(idolId: Int?, category: String?, chartCode: String?) {
        context.dataStore.edit { preferences ->
            if (idolId != null) preferences[KEY_MOST_IDOL_ID] = idolId else preferences.remove(KEY_MOST_IDOL_ID)
            if (category != null) preferences[KEY_MOST_IDOL_CATEGORY] = category else preferences.remove(KEY_MOST_IDOL_CATEGORY)
            if (chartCode != null) preferences[KEY_MOST_IDOL_CHART_CODE] = chartCode else preferences.remove(KEY_MOST_IDOL_CHART_CODE)
        }
        android.util.Log.d("PreferencesManager", "✓ Saved most idol info: id=$idolId, category=$category, chartCode=$chartCode")
    }

    /**
     * 최애 아이돌 ID 로드
     */
    suspend fun getMostIdolId(): Int? {
        return context.dataStore.data.first()[KEY_MOST_IDOL_ID]
    }

    /**
     * 최애 아이돌 카테고리 로드
     */
    suspend fun getMostIdolCategory(): String? {
        return context.dataStore.data.first()[KEY_MOST_IDOL_CATEGORY]
    }

    /**
     * 최애 아이돌 차트 코드 로드
     */
    suspend fun getMostIdolChartCode(): String? {
        return context.dataStore.data.first()[KEY_MOST_IDOL_CHART_CODE]
    }

    /**
     * 즐겨찾기 아이돌 ID 리스트 저장
     */
    suspend fun saveFavoriteIdolIds(idolIds: List<Int>) {
        val json = gson.toJson(idolIds)
        context.dataStore.edit { preferences ->
            preferences[KEY_FAVORITE_IDOL_IDS] = json
        }
        android.util.Log.d("PreferencesManager", "✓ Saved ${idolIds.size} favorite idol IDs")
    }

    /**
     * 즐겨찾기 아이돌 ID 리스트 로드
     */
    suspend fun getFavoriteIdolIds(): List<Int> {
        val json = context.dataStore.data.first()[KEY_FAVORITE_IDOL_IDS]
        return if (json != null) {
            try {
                gson.fromJson(json, Array<Int>::class.java).toList()
            } catch (e: Exception) {
                android.util.Log.e("PreferencesManager", "Failed to parse favorite idol IDs", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * MostPicksModel 저장
     */
    suspend fun saveMostPicksModel(model: MostPicksModel?) {
        context.dataStore.edit { preferences ->
            if (model != null) {
                val json = gson.toJson(model)
                preferences[KEY_MOST_PICKS_MODEL] = json
            } else {
                preferences.remove(KEY_MOST_PICKS_MODEL)
            }
        }
        android.util.Log.d("PreferencesManager", "✓ Saved most picks model: $model")
    }

    /**
     * MostPicksModel 로드
     */
    suspend fun getMostPicksModel(): MostPicksModel? {
        val json = context.dataStore.data.first()[KEY_MOST_PICKS_MODEL]
        return if (json != null) {
            try {
                gson.fromJson(json, MostPicksModel::class.java)
            } catch (e: Exception) {
                android.util.Log.e("PreferencesManager", "Failed to parse MostPicksModel", e)
                null
            }
        } else {
            null
        }
    }

    /**
     * 기본 카테고리 가져오기 (UserCacheRepository용)
     */
    suspend fun getDefaultCategory(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_DEFAULT_CATEGORY]
    }

    /**
     * 기본 차트 코드 가져오기 (UserCacheRepository용)
     */
    suspend fun getDefaultChartCode(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_DEFAULT_CHART_CODE]
    }

    // ============================================================
    // Free Board Tab Selection
    // ============================================================

    /**
     * Free Board 선택된 탭 ID 저장
     * 첫 진입시 HOT(0)이 기본, 이후 마지막 선택 탭 유지
     */
    suspend fun setFreeBoardSelectedTagId(tagId: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FREE_BOARD_SELECTED_TAG_ID] = tagId
        }
    }

    /**
     * Free Board 선택된 탭 ID 가져오기
     * @return 저장된 탭 ID (없으면 null - 첫 진입시 HOT 선택)
     */
    suspend fun getFreeBoardSelectedTagId(): Int? {
        return context.dataStore.data.first()[KEY_FREE_BOARD_SELECTED_TAG_ID]
    }

    // ============================================================
    // Notification
    // ============================================================

    /**
     * 최근 알림 날짜 저장
     * 알림 화면 진입시 저장하여, 이후 새 알림 체크에 사용
     */
    suspend fun setRecentNotificationDate(date: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RECENT_NOTIFICATION_DATE] = date
        }
    }

    /**
     * 최근 알림 날짜 가져오기
     * @return 저장된 날짜 (없으면 null - 전체 알림 조회)
     */
    suspend fun getRecentNotificationDate(): String? {
        return context.dataStore.data.first()[KEY_RECENT_NOTIFICATION_DATE]
    }

    // ============================================================
    // Notice Badge (읽지 않은 공지사항)
    // ============================================================

    /**
     * 읽은 공지사항 ID 리스트 Flow
     */
    val readNoticeIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val idsString = preferences[KEY_NOTICE_READ_IDS]
            if (idsString.isNullOrEmpty()) emptySet()
            else idsString.split(",").toSet()
        }

    /**
     * 공지사항을 읽음 처리
     */
    suspend fun markNoticeAsRead(noticeId: String) {
        context.dataStore.edit { preferences ->
            val currentIds = preferences[KEY_NOTICE_READ_IDS]
            val idsSet = if (currentIds.isNullOrEmpty()) mutableSetOf()
            else currentIds.split(",").toMutableSet()
            idsSet.add(noticeId)
            preferences[KEY_NOTICE_READ_IDS] = idsSet.joinToString(",")
        }
    }

    /**
     * 읽은 공지사항 ID 리스트 가져오기 (일회성)
     */
    suspend fun getReadNoticeIds(): Set<String> {
        val idsString = context.dataStore.data.first()[KEY_NOTICE_READ_IDS]
        return if (idsString.isNullOrEmpty()) emptySet()
        else idsString.split(",").toSet()
    }

    /**
     * 특정 공지사항이 읽혔는지 확인
     */
    suspend fun isNoticeRead(noticeId: String): Boolean {
        return getReadNoticeIds().contains(noticeId)
    }

    // ============================================================
    // Event Badge (읽지 않은 이벤트)
    // ============================================================

    /**
     * 읽은 이벤트 ID 리스트 Flow
     */
    val readEventIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val idsString = preferences[KEY_EVENT_READ_IDS]
            if (idsString.isNullOrEmpty()) emptySet()
            else idsString.split(",").toSet()
        }

    /**
     * 이벤트를 읽음 처리
     */
    suspend fun markEventAsRead(eventId: String) {
        context.dataStore.edit { preferences ->
            val currentIds = preferences[KEY_EVENT_READ_IDS]
            val idsSet = if (currentIds.isNullOrEmpty()) mutableSetOf()
            else currentIds.split(",").toMutableSet()
            idsSet.add(eventId)
            preferences[KEY_EVENT_READ_IDS] = idsSet.joinToString(",")
        }
    }

    /**
     * 읽은 이벤트 ID 리스트 가져오기 (일회성)
     */
    suspend fun getReadEventIds(): Set<String> {
        val idsString = context.dataStore.data.first()[KEY_EVENT_READ_IDS]
        return if (idsString.isNullOrEmpty()) emptySet()
        else idsString.split(",").toSet()
    }

    /**
     * 특정 이벤트가 읽혔는지 확인
     */
    suspend fun isEventRead(eventId: String): Boolean {
        return getReadEventIds().contains(eventId)
    }

    // ============================================================
    // Ranking - 최애 이동 토스트
    // ============================================================

    /**
     * 최애 이동 토스트 표시 여부 Flow
     */
    val hasShownMyFavToast: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_HAS_SHOWN_MY_FAV_TOAST] ?: false
        }

    /**
     * 최애 이동 토스트 표시 완료 처리
     */
    suspend fun setHasShownMyFavToast(hasShown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_SHOWN_MY_FAV_TOAST] = hasShown
        }
    }

    /**
     * 최애 이동 토스트 표시 여부 가져오기 (일회성)
     */
    suspend fun getHasShownMyFavToast(): Boolean {
        return context.dataStore.data.first()[KEY_HAS_SHOWN_MY_FAV_TOAST] ?: false
    }

    /**
     * 최애 이동 토스트 초기화 (앱 시작 시 호출)
     * old 프로젝트의 StartupActivity에서 호출하던 로직
     */
    suspend fun resetMyFavToast() {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_SHOWN_MY_FAV_TOAST] = false
        }
    }
}
