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
import net.ib.mn.domain.model.IconMenuItem
import net.ib.mn.domain.model.IconMenuType
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.MenuConfig
import net.ib.mn.domain.model.MenuItem
import net.ib.mn.domain.model.TextMenuItem
import net.ib.mn.domain.model.TextMenuType
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.SupportedLanguage
import javax.inject.Inject

/**
 * Menu 페이지 ViewModel
 */
@HiltViewModel
class MenuPageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "MenuPageViewModel"
    }

    private val _menuConfig = MutableStateFlow(MenuConfig.default())
    val menuConfig: StateFlow<MenuConfig> = _menuConfig.asStateFlow()

    private val _iconMenuItems = MutableStateFlow<List<IconMenuItem>>(emptyList())
    val iconMenuItems: StateFlow<List<IconMenuItem>> = _iconMenuItems.asStateFlow()

    private val _textMenuItems = MutableStateFlow<List<TextMenuItem>>(emptyList())
    val textMenuItems: StateFlow<List<TextMenuItem>> = _textMenuItems.asStateFlow()

    private val _bannerList = MutableStateFlow<List<InAppBanner>>(emptyList())
    val bannerList: StateFlow<List<InAppBanner>> = _bannerList.asStateFlow()

    init {
        observeMenuConfig()
        loadBanners()
    }

    /**
     * 배너 데이터 로드
     */
    private fun loadBanners() {
        viewModelScope.launch {
            preferencesManager.inAppBannerMenu.collect { bannerJson ->
                if (bannerJson != null) {
                    try {
                        // JSON 문자열을 InAppBanner 리스트로 변환
                        val type = object : com.google.gson.reflect.TypeToken<List<InAppBanner>>() {}.type
                        val banners: List<InAppBanner> = com.google.gson.Gson().fromJson(bannerJson, type)
                        _bannerList.value = banners
                        android.util.Log.d(TAG, "Loaded ${banners.size} banners from PreferencesManager")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Failed to parse banner JSON: ${e.message}")
                        _bannerList.value = emptyList()
                    }
                } else {
                    _bannerList.value = emptyList()
                }
            }
        }
    }

    /**
     * 메뉴 설정 변경 구독
     */
    private fun observeMenuConfig() {
        viewModelScope.launch {
            combine(
                preferencesManager.showLiveStreamingTab,
                preferencesManager.menuNoticeMain,
                preferencesManager.menuStoreMain,
                preferencesManager.menuFreeBoardMain,
                preferencesManager.showStoreEventMarker,
                preferencesManager.showFreeChargeMarker
            ) { flows: Array<Any?> ->
                val showLiveStreamingTab = flows[0] as Boolean
                val menuNoticeMain = flows[1] as? String
                val menuStoreMain = flows[2] as? String
                val menuFreeBoardMain = flows[3] as? String
                val showStoreEventMarker = flows[4] as? String
                val showFreeChargeMarker = flows[5] as? String

                MenuConfig(
                    menuNoticeMain = menuNoticeMain ?: "N",
                    menuStoreMain = menuStoreMain ?: "N",
                    menuFreeBoardMain = menuFreeBoardMain ?: "N",
                    showStoreEventMarker = showStoreEventMarker ?: "N",
                    showFreeChargeMarker = showFreeChargeMarker ?: "N",
                    showLiveStreamingTab = showLiveStreamingTab,
                    hasUnreadNotice = false,  // TODO: SharedAppState 연결
                    hasUnreadEvent = false,   // TODO: SharedAppState 연결
                    isAttendanceAvailable = false,  // TODO: SharedAppState 연결
                    showGameMenu = false  // TODO: RemoteConfig 연결
                )
            }.collect { config ->
                _menuConfig.value = config
                updateMenuItems(config)
                android.util.Log.d(TAG, "MenuConfig updated: $config")
            }
        }
    }

    /**
     * 메뉴 아이템 업데이트 (필터링 포함)
     */
    private fun updateMenuItems(config: MenuConfig) {
        _iconMenuItems.value = buildIconMenuItems(config)
        _textMenuItems.value = buildTextMenuItems(config)
    }

    /**
     * 아이콘 메뉴 아이템 생성
     */
    private fun buildIconMenuItems(config: MenuConfig): List<IconMenuItem> {
        val items = mutableListOf<IconMenuItem>()

        // 1. 고객센터 (항상 표시)
        items.add(
            IconMenuItem(
                id = "support",
                labelResId = R.string.support,
                iconResId = R.drawable.icon_menu_support_1,
                type = IconMenuType.SUPPORT
            )
        )

        // 2. 무료충전소 (항상 표시)
        items.add(
            IconMenuItem(
                id = "free_charge",
                labelResId = R.string.btn_free_heart_charge,
                iconResId = R.drawable.icon_menu_freeshop,
                type = IconMenuType.FREE_CHARGE,
                hasBadge = config.showFreeChargeMarker == "Y",
                badgeIconResId = if (config.showFreeChargeMarker == "Y") R.drawable.icon_menu_up else null
            )
        )

        // 3. 출석체크 (항상 표시)
        items.add(
            IconMenuItem(
                id = "attendance",
                labelResId = R.string.attendance_check,
                iconResId = R.drawable.icon_menu_attendance,
                type = IconMenuType.ATTENDANCE,
                hasBadge = config.isAttendanceAvailable,
                badgeIconResId = if (config.isAttendanceAvailable) R.drawable.icon_menu_new else null
            )
        )

        // 4. 이벤트 (항상 표시)
        items.add(
            IconMenuItem(
                id = "event",
                labelResId = R.string.menu_menu00,
                iconResId = R.drawable.icon_menu_event_1,
                type = IconMenuType.EVENT,
                hasBadge = config.hasUnreadEvent,
                badgeIconResId = if (config.hasUnreadEvent) R.drawable.icon_menu_up else null
            )
        )

        // 5. 상점 (menuStoreMain != "N"일 때 표시)
        if (config.menuStoreMain != "N") {
            items.add(
                IconMenuItem(
                    id = "store",
                    labelResId = R.string.label_store,
                    iconResId = R.drawable.icon_menu_shop,
                    type = IconMenuType.STORE,
                    hasBadge = config.showStoreEventMarker == "Y",
                    badgeIconResId = if (config.showStoreEventMarker == "Y") R.drawable.icon_menu_up else null
                )
            )
        }

        // 6. 공지사항 (menuNoticeMain != "N"일 때 표시)
        if (config.menuNoticeMain != "N") {
            items.add(
                IconMenuItem(
                    id = "notice",
                    labelResId = R.string.setting_menu01,
                    iconResId = R.drawable.icon_menu_notice,
                    type = IconMenuType.NOTICE,
                    hasBadge = config.hasUnreadNotice,
                    badgeIconResId = if (config.hasUnreadNotice) R.drawable.icon_menu_new else null
                )
            )
        }

        // 7. 자유게시판 (menuFreeBoardMain != "N" AND showLiveStreamingTab일 때 표시)
        if (config.menuFreeBoardMain != "N" && config.showLiveStreamingTab) {
            items.add(
                IconMenuItem(
                    id = "free_board",
                    labelResId = R.string.hometab_title_freeboard,
                    iconResId = R.drawable.icon_menu_board,
                    type = IconMenuType.FREE_BOARD
                )
            )
        }

        return items
    }

    /**
     * 텍스트 메뉴 아이템 생성
     */
    private fun buildTextMenuItems(config: MenuConfig): List<TextMenuItem> {
        val items = mutableListOf<TextMenuItem>()

        // 1. 투표 인증서 (항상 표시)
        items.add(
            TextMenuItem(
                id = "vote_certificate",
                labelResId = R.string.certificate_title,
                iconResId = R.drawable.icon_sidemenu_votingcertificate,
                type = TextMenuType.VOTE_CERTIFICATE
            )
        )

        // 2. 자유게시판 (menuFreeBoardMain != "Y" AND showLiveStreamingTab일 때 표시)
        if (config.menuFreeBoardMain != "Y" && config.showLiveStreamingTab) {
            items.add(
                TextMenuItem(
                    id = "free_board_text",
                    labelResId = R.string.hometab_title_freeboard,
                    iconResId = R.drawable.icon_sidemenu_board,
                    type = TextMenuType.FREE_BOARD
                )
            )
        }

        // 3. 상점 (menuStoreMain != "Y"일 때 표시)
        if (config.menuStoreMain != "Y") {
            items.add(
                TextMenuItem(
                    id = "store_text",
                    labelResId = R.string.label_store,
                    iconResId = R.drawable.icon_sidemenu_shop,
                    type = TextMenuType.STORE,
                    hasBadge = config.showStoreEventMarker == "Y"
                )
            )
        }

        // 4. 공지사항 (menuNoticeMain != "Y"일 때 표시)
        if (config.menuNoticeMain != "Y") {
            items.add(
                TextMenuItem(
                    id = "notice_text",
                    labelResId = R.string.setting_menu01,
                    iconResId = R.drawable.icon_sidemenu_notice,
                    type = TextMenuType.NOTICE,
                    hasBadge = config.hasUnreadNotice
                )
            )
        }

        // 5. 친구 초대 (항상 표시)
        items.add(
            TextMenuItem(
                id = "invite_friend",
                labelResId = R.string.menu_invite_friend,
                iconResId = R.drawable.icon_sidemenu_invite_friend,
                type = TextMenuType.INVITE_FRIEND
            )
        )

        // 6. 기록실 (항상 표시)
        items.add(
            TextMenuItem(
                id = "history",
                labelResId = R.string.menu_stats,
                iconResId = R.drawable.icon_sidemenu_record,
                type = TextMenuType.HISTORY
            )
        )

        // 7. 미니게임 (showGameMenu일 때 표시)
        if (config.showGameMenu) {
            items.add(
                TextMenuItem(
                    id = "game",
                    labelResId = R.string.menu_minigame,
                    iconResId = R.drawable.icon_sidemenu_game,
                    type = TextMenuType.GAME
                )
            )
        }

        // 8. 퀴즈 (현재 로케일이 지원 로케일에 포함될 때 표시)
        if (LocaleUtil.isExistCurrentLocale(context, SupportedLanguage.BOARD_KIN_QUIZZES_TOP100_LOCALES)) {
            items.add(
                TextMenuItem(
                    id = "quiz",
                    labelResId = R.string.menu_quiz,
                    iconResId = R.drawable.icon_sidemenu_quiz,
                    type = TextMenuType.QUIZ
                )
            )
        }

        // 9. 닮은꼴 (항상 표시)
        items.add(
            TextMenuItem(
                id = "face",
                labelResId = R.string.menu_face,
                iconResId = R.drawable.icon_sidemenu_similar,
                type = TextMenuType.FACE
            )
        )

        return items
    }

    /**
     * 메뉴 아이템 클릭 처리
     */
    fun onMenuItemClick(item: MenuItem) {
        android.util.Log.d(TAG, "Menu item clicked: ${item.id}")
        // TODO: Navigation 처리는 MenuPage에서 콜백으로 처리
    }
}
