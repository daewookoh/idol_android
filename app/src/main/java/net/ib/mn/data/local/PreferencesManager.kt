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
    val hearts: Int?
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
        // User
        val KEY_USER_ID = intPreferencesKey(Constants.KEY_USER_ID)
        val KEY_USER_EMAIL = stringPreferencesKey(Constants.KEY_USER_EMAIL)
        val KEY_USER_USERNAME = stringPreferencesKey("user_username")
        val KEY_USER_NICKNAME = stringPreferencesKey("user_nickname")
        val KEY_USER_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        val KEY_USER_HEARTS = intPreferencesKey("user_hearts")
        val KEY_ACCESS_TOKEN = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)

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
                    hearts = preferences[KEY_USER_HEARTS]
                )
                android.util.Log.d("USER_INFO", "[PreferencesManager] DataStore emitting user info to collectors")
                android.util.Log.d("USER_INFO", "[PreferencesManager]   - ID: ${info.id}, Email: ${info.email}, Username: ${info.username}")
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
        hearts: Int?
    ) {
        android.util.Log.d("USER_INFO", "[PreferencesManager] Writing user info to DataStore...")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - ID: $id")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Email: $email")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Username: $username")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Nickname: $nickname")
        android.util.Log.d("USER_INFO", "[PreferencesManager]   - Hearts: $hearts")

        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = id
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_USER_USERNAME] = username
            nickname?.let { preferences[KEY_USER_NICKNAME] = it }
            profileImage?.let { preferences[KEY_USER_PROFILE_IMAGE] = it }
            hearts?.let { preferences[KEY_USER_HEARTS] = it }
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

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
