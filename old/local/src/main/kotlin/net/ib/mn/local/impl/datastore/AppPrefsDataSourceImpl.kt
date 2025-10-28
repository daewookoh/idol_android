package net.ib.mn.local.impl.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ib.mn.common.util.logE
import net.ib.mn.common.util.logI
import net.ib.mn.data.local.datastore.AppPrefsDataSource
import net.ib.mn.data.model.InAppBannerPrefsEntity
import net.ib.mn.local.di.QualifierNames
import net.ib.mn.local.impl.datastore.AppPrefsDataSourceImpl.Keys.AD_COUNT_KEY
import net.ib.mn.local.impl.datastore.AppPrefsDataSourceImpl.Keys.AD_DATE_KEY
import net.ib.mn.local.impl.datastore.AppPrefsDataSourceImpl.Keys.AD_ENABLED_NOTIFICATION
import net.ib.mn.local.impl.datastore.AppPrefsDataSourceImpl.Keys.AD_MAX_COUNT_KEY
import net.ib.mn.local.impl.datastore.AppPrefsDataSourceImpl.Keys.FRIEND_INVITE_BANNER_TOOLTIP
import javax.inject.Inject
import javax.inject.Named

class AppPrefsDataSourceImpl @Inject constructor(
    @Named(QualifierNames.APP_PREFS) private val dataStore: DataStore<Preferences>
) : AppPrefsDataSource {

    private object Keys {
        val TYPE_MENU = stringPreferencesKey("type_menu")
        val TYPE_SEARCH = stringPreferencesKey("type_search")
        val AD_COUNT_KEY = intPreferencesKey("ad_count")
        val AD_DATE_KEY = longPreferencesKey("ad_date")
        val AD_MAX_COUNT_KEY = intPreferencesKey("ad_max_count")
        val AD_ENABLED_NOTIFICATION = booleanPreferencesKey("ad_enabled_notification")
        val FRIEND_INVITE_BANNER_TOOLTIP = booleanPreferencesKey("friend_invite_banner_tooltip")
    }

    private object BannerTypeLabels {
        const val MENU = "M"
        const val SEARCH = "S"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun setBannerData(bannerMap: Map<String, List<InAppBannerPrefsEntity>>) {
        logI("IN_APP_BANNER", "start setBannerData: size=${bannerMap.size}")
        for ((key, bannerList) in bannerMap) {
            logI("IN_APP_BANNER", "processing key=$key, bannerList=$bannerList")
            try {
                val jsonString = json.encodeToString(bannerList)
                when (key) {
                    BannerTypeLabels.MENU -> {
                        dataStore.edit { it[Keys.TYPE_MENU] = jsonString }
                        logI("IN_APP_BANNER", "saved menu")
                    }
                    BannerTypeLabels.SEARCH -> {
                        dataStore.edit { it[Keys.TYPE_SEARCH] = jsonString }
                        logI("IN_APP_BANNER", "saved search")
                    }
                    else -> logI("IN_APP_BANNER", "skipped unknown key")
                }
            } catch (e: Exception) {
                logE("IN_APP_BANNER", "❗Error serializing key=$key: ${e.message}")
            }
        }
        logI("IN_APP_BANNER", "end setBannerData")
    }

    override suspend fun getMenuBannerData(): List<InAppBannerPrefsEntity> {
        val preferences = dataStore.data.first()
        val jsonString = preferences[Keys.TYPE_MENU] ?: return emptyList()
        return try {
            json.decodeFromString<List<InAppBannerPrefsEntity>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSearchBannerData(): List<InAppBannerPrefsEntity> {
        val preferences = dataStore.data.first()
        val jsonString = preferences[Keys.TYPE_SEARCH] ?: return emptyList()
        return try {
            json.decodeFromString<List<InAppBannerPrefsEntity>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun initAdData(date: Long) {
        dataStore.edit {
            it[AD_COUNT_KEY] = 0
            it[AD_DATE_KEY] = date
        }
    }

    override suspend fun updateAdCount(currentCount: Int, maxCount: Int) {
        dataStore.edit {
            it[AD_COUNT_KEY] = currentCount
            it[AD_MAX_COUNT_KEY] = maxCount
        }
    }

    override suspend fun getAdData(): Triple<Int, Int, Long> {
        return try {
            val preferences = dataStore.data.first()
            Triple(
                preferences[AD_COUNT_KEY] ?: 0,
                preferences[AD_MAX_COUNT_KEY] ?: 0,
                preferences[AD_DATE_KEY] ?: 0L
            )
        } catch (e: Exception) {
            throw e
        }
    }


    override suspend fun isEnabledVideoAd(): Boolean {
        return dataStore.data
            .map { preferences ->
                val adCount = preferences[AD_COUNT_KEY] ?: 0
                val adMaxCount = preferences[AD_MAX_COUNT_KEY] ?: 0

                adCount < adMaxCount || adMaxCount == 0
            }
            .first()
    }

    override suspend fun getAdCount(): Int {
        return try {
            val preferences = dataStore.data.first()
            preferences[AD_COUNT_KEY] ?: 0
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun setAdNotification(isSet: Boolean) {
        dataStore.edit {
            it[AD_ENABLED_NOTIFICATION] = isSet
        }
    }

    override suspend fun isSetAdNotification(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences[AD_ENABLED_NOTIFICATION] ?: false
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun setInviteFriendBannerTooltip() {
        dataStore.edit {
            it[FRIEND_INVITE_BANNER_TOOLTIP] = false
        }
    }

    override suspend fun isShowInviteFriendBannerTooltip(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences[FRIEND_INVITE_BANNER_TOOLTIP] ?: true
        } catch (e: Exception) {
            throw e
        }
    }
}