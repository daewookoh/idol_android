package net.ib.mn.presentation.main.menu

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.api.MiscApi
import net.ib.mn.data.remote.api.StampsApi
import net.ib.mn.domain.model.IconMenuItem
import net.ib.mn.domain.model.IconMenuType
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.MenuConfig
import net.ib.mn.domain.model.TextMenuItem
import net.ib.mn.domain.model.TextMenuType
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.SupportedLanguage
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Menu 페이지 ViewModel
 */
@HiltViewModel
class MenuPageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val miscApi: MiscApi,
    private val stampsApi: StampsApi
) : ViewModel() {

    private val _iconMenuItems = MutableStateFlow<List<IconMenuItem>>(emptyList())
    val iconMenuItems: StateFlow<List<IconMenuItem>> = _iconMenuItems.asStateFlow()

    private val _textMenuItems = MutableStateFlow<List<TextMenuItem>>(emptyList())
    val textMenuItems: StateFlow<List<TextMenuItem>> = _textMenuItems.asStateFlow()

    private val _bannerList = MutableStateFlow<List<InAppBanner>>(emptyList())
    val bannerList: StateFlow<List<InAppBanner>> = _bannerList.asStateFlow()

    private val _hasUnreadNotice = MutableStateFlow(false)
    private val _hasUnreadEvent = MutableStateFlow(false)
    private val _isAttendanceAvailable = MutableStateFlow(false)

    init {
        observeMenuConfig()
        loadBanners()
        checkBadgeStates()
    }

    private fun loadBanners() {
        viewModelScope.launch {
            preferencesManager.inAppBannerMenu.collect { bannerJson ->
                _bannerList.value = bannerJson?.let { json ->
                    runCatching {
                        val type = object : com.google.gson.reflect.TypeToken<List<InAppBanner>>() {}.type
                        com.google.gson.Gson().fromJson<List<InAppBanner>>(json, type)
                    }.getOrDefault(emptyList())
                } ?: emptyList()
            }
        }
    }

    private fun observeMenuConfig() {
        viewModelScope.launch {
            combine(
                preferencesManager.showLiveStreamingTab,
                preferencesManager.menuNoticeMain,
                preferencesManager.menuStoreMain,
                preferencesManager.menuFreeBoardMain,
                preferencesManager.showStoreEventMarker,
                preferencesManager.showFreeChargeMarker,
                _hasUnreadNotice,
                _hasUnreadEvent,
                _isAttendanceAvailable
            ) { flows: Array<Any?> ->
                MenuConfig(
                    menuNoticeMain = (flows[1] as? String) ?: "N",
                    menuStoreMain = (flows[2] as? String) ?: "N",
                    menuFreeBoardMain = (flows[3] as? String) ?: "N",
                    showStoreEventMarker = (flows[4] as? String) ?: "N",
                    showFreeChargeMarker = (flows[5] as? String) ?: "N",
                    showLiveStreamingTab = flows[0] as Boolean,
                    hasUnreadNotice = flows[6] as Boolean,
                    hasUnreadEvent = flows[7] as Boolean,
                    isAttendanceAvailable = flows[8] as Boolean,
                    showGameMenu = false
                )
            }.collect { config ->
                _iconMenuItems.value = buildIconMenuItems(config)
                _textMenuItems.value = buildTextMenuItems(config)
            }
        }
    }

    private fun checkBadgeStates() {
        viewModelScope.launch { checkUnreadNotices() }
        viewModelScope.launch { checkUnreadEvents() }
        viewModelScope.launch { checkAttendanceAvailable() }
    }

    private suspend fun checkUnreadNotices() {
        runCatching {
            val response = miscApi.getNotices()
            if (response.isSuccessful) {
                val json = JSONObject(response.body()?.string() ?: return)
                if (json.optBoolean("success", false)) {
                    val ids = extractIds(json.optJSONArray("objects"))
                    val readIds = preferencesManager.getReadNoticeIds()
                    _hasUnreadNotice.value = ids.any { it !in readIds }
                }
            }
        }
    }

    private suspend fun checkUnreadEvents() {
        runCatching {
            val response = miscApi.getEvents()
            if (response.isSuccessful) {
                val json = JSONObject(response.body()?.string() ?: return)
                if (json.optBoolean("success", false)) {
                    val ids = extractIds(json.optJSONArray("objects"))
                    val readIds = preferencesManager.getReadEventIds()
                    _hasUnreadEvent.value = ids.any { it !in readIds }
                }
            }
        }
    }

    private suspend fun checkAttendanceAvailable() {
        runCatching {
            val response = stampsApi.getStampsCurrent()
            if (response.isSuccessful) {
                val json = JSONObject(response.body()?.string() ?: return)
                if (json.optBoolean("success", false)) {
                    val stamp = json.optJSONObject("stamp")
                    // stamp 객체가 없거나 today 키가 없으면 출석 가능
                    val todayStamped = stamp?.takeIf { it.has("today") }?.optBoolean("today", false) ?: false
                    _isAttendanceAvailable.value = !todayStamped
                }
            }
        }
    }

    private fun extractIds(array: JSONArray?): Set<String> {
        if (array == null) return emptySet()
        return (0 until array.length())
            .mapNotNull { array.optJSONObject(it)?.optLong("id", 0)?.takeIf { id -> id > 0 }?.toString() }
            .toSet()
    }

    private fun buildIconMenuItems(config: MenuConfig): List<IconMenuItem> = buildList {
        add(IconMenuItem(
            id = "support",
            labelResId = R.string.support,
            iconResId = R.drawable.icon_menu_support_1,
            type = IconMenuType.SUPPORT
        ))

        add(IconMenuItem(
            id = "free_charge",
            labelResId = R.string.btn_free_heart_charge,
            iconResId = R.drawable.icon_menu_freeshop,
            type = IconMenuType.FREE_CHARGE,
            hasBadge = config.showFreeChargeMarker == "Y",
            badgeIconResId = R.drawable.icon_menu_up.takeIf { config.showFreeChargeMarker == "Y" }
        ))

        add(IconMenuItem(
            id = "attendance",
            labelResId = R.string.attendance_check,
            iconResId = R.drawable.icon_menu_attendance,
            type = IconMenuType.ATTENDANCE,
            hasBadge = config.isAttendanceAvailable,
            badgeIconResId = R.drawable.icon_menu_new.takeIf { config.isAttendanceAvailable }
        ))

        add(IconMenuItem(
            id = "event",
            labelResId = R.string.menu_menu00,
            iconResId = R.drawable.icon_menu_event_1,
            type = IconMenuType.EVENT,
            hasBadge = config.hasUnreadEvent,
            badgeIconResId = R.drawable.icon_menu_up.takeIf { config.hasUnreadEvent }
        ))

        if (config.menuStoreMain != "N") {
            add(IconMenuItem(
                id = "store",
                labelResId = R.string.label_store,
                iconResId = R.drawable.icon_menu_shop,
                type = IconMenuType.STORE,
                hasBadge = config.showStoreEventMarker == "Y",
                badgeIconResId = R.drawable.icon_menu_up.takeIf { config.showStoreEventMarker == "Y" }
            ))
        }

        if (config.menuNoticeMain != "N") {
            add(IconMenuItem(
                id = "notice",
                labelResId = R.string.setting_menu01,
                iconResId = R.drawable.icon_menu_notice,
                type = IconMenuType.NOTICE,
                hasBadge = config.hasUnreadNotice,
                badgeIconResId = R.drawable.icon_menu_new.takeIf { config.hasUnreadNotice }
            ))
        }

        if (config.menuFreeBoardMain != "N" && config.showLiveStreamingTab) {
            add(IconMenuItem(
                id = "free_board",
                labelResId = R.string.hometab_title_freeboard,
                iconResId = R.drawable.icon_menu_board,
                type = IconMenuType.FREE_BOARD
            ))
        }
    }

    private fun buildTextMenuItems(config: MenuConfig): List<TextMenuItem> = buildList {
        add(TextMenuItem(
            id = "vote_certificate",
            labelResId = R.string.certificate_title,
            iconResId = R.drawable.icon_sidemenu_votingcertificate,
            type = TextMenuType.VOTE_CERTIFICATE
        ))

        if (config.menuFreeBoardMain != "Y" && config.showLiveStreamingTab) {
            add(TextMenuItem(
                id = "free_board_text",
                labelResId = R.string.hometab_title_freeboard,
                iconResId = R.drawable.icon_sidemenu_board,
                type = TextMenuType.FREE_BOARD
            ))
        }

        if (config.menuStoreMain != "Y") {
            add(TextMenuItem(
                id = "store_text",
                labelResId = R.string.label_store,
                iconResId = R.drawable.icon_sidemenu_shop,
                type = TextMenuType.STORE,
                hasBadge = config.showStoreEventMarker == "Y"
            ))
        }

        if (config.menuNoticeMain != "Y") {
            add(TextMenuItem(
                id = "notice_text",
                labelResId = R.string.setting_menu01,
                iconResId = R.drawable.icon_sidemenu_notice,
                type = TextMenuType.NOTICE,
                hasBadge = config.hasUnreadNotice
            ))
        }

        add(TextMenuItem(
            id = "invite_friend",
            labelResId = R.string.menu_invite_friend,
            iconResId = R.drawable.icon_sidemenu_invite_friend,
            type = TextMenuType.INVITE_FRIEND
        ))

        add(TextMenuItem(
            id = "history",
            labelResId = R.string.menu_stats,
            iconResId = R.drawable.icon_sidemenu_record,
            type = TextMenuType.HISTORY
        ))

        add(TextMenuItem(
            id = "game",
            labelResId = R.string.menu_minigame,
            iconResId = R.drawable.icon_sidemenu_game,
            type = TextMenuType.GAME
        ))

        if (LocaleUtil.isExistCurrentLocale(context, SupportedLanguage.BOARD_KIN_QUIZZES_TOP100_LOCALES)) {
            add(TextMenuItem(
                id = "quiz",
                labelResId = R.string.menu_quiz,
                iconResId = R.drawable.icon_sidemenu_quiz,
                type = TextMenuType.QUIZ
            ))
        }

        add(TextMenuItem(
            id = "face",
            labelResId = R.string.menu_face,
            iconResId = R.drawable.icon_sidemenu_similar,
            type = TextMenuType.FACE
        ))
    }
}
