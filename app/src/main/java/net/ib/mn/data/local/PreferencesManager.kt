package net.ib.mn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import net.ib.mn.util.Constants
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    val hearts: Int?,
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
        val KEY_USER_HEARTS = intPreferencesKey("user_hearts")
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

        // Category
        val KEY_DEFAULT_CATEGORY = stringPreferencesKey(Constants.PREF_DEFAULT_CATEGORY)
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
                    hearts = preferences[KEY_USER_HEARTS],
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
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - Hearts: ${info.hearts}")
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

    val defaultCategory: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_CATEGORY] ?: Constants.TYPE_MALE
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
        hearts: Int?,
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
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Hearts: $hearts")
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
            if (hearts != null) preferences[KEY_USER_HEARTS] = hearts else preferences.remove(KEY_USER_HEARTS)
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

    suspend fun setDefaultCategory(category: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_CATEGORY] = category
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
}
