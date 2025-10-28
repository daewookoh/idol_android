package net.ib.mn.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ib.mn.R
import net.ib.mn.activity.EventActivity
import net.ib.mn.activity.FreeboardActivity
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.activity.IdolQuizMainActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.activity.NoticeActivity
import net.ib.mn.activity.SettingActivity
import net.ib.mn.activity.StatsActivity
import net.ib.mn.activity.VotingCertificateListActivity
import net.ib.mn.activity.WebViewGameActivity
import net.ib.mn.common.util.logE
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.language.LanguagePreferenceRepository
import net.ib.mn.feature.friend.FriendInviteActivity
import net.ib.mn.feature.friend.InvitePayload
import net.ib.mn.model.ConfigModel
import net.ib.mn.support.SupportMainActivity
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.GlobalVariable
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RemoteConfigUtil
import net.ib.mn.utils.SupportedLanguage
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import net.ib.mn.viewmodel.MainViewModel
import net.ib.mn.viewmodel.MainViewModel.TokenResult
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resume

@AndroidEntryPoint
class MyInfoFragment : MyInfoBaseFragment() {

    private lateinit var rootView: ViewGroup // for tutorial

    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var languagePreferenceRepository: LanguagePreferenceRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        setEventListener()

        // 개발 중 튜토리얼 전체 점검 시 사용
//        setTutorial()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        RemoteConfigUtil.fetchRemoteConfigIfNull(requireContext()) { isSuccess ->
            if (isSuccess) {
                initUI()
            }
        }
    }

    private fun initUI() {
        if (LocaleUtil.isExistCurrentLocale(requireContext(), SupportedLanguage.BOARD_KIN_QUIZZES_TOP100_LOCALES)) {
            myInfoBinding?.btnMenuQuiz?.visibility = View.VISIBLE
        } else {
            myInfoBinding?.btnMenuQuiz?.visibility = View.GONE
            myInfoBinding?.myinfoUnderbar4?.visibility = View.GONE
        }

        if (ConfigModel.getInstance(requireContext()).showStoreEventMarker.equals("Y")) {
            myInfoBinding?.ivStoreNew?.visibility = View.VISIBLE
        }

        // 게임 메뉴 표시 여부 확인
        if (GlobalVariable.RemoteConfig?.game?.showMenu == true) {
            myInfoBinding?.btnMenuGame?.visibility = View.VISIBLE
            myInfoBinding?.myinfoUnderbar10?.visibility = View.VISIBLE
        } else {
            myInfoBinding?.btnMenuGame?.visibility = View.GONE
            myInfoBinding?.myinfoUnderbar10?.visibility = View.GONE
        }

        rootView = myInfoBinding?.root?.findViewById(R.id.cl_myinfo_root)!!

        setNoticeAndEvent()
    }


    private fun setTutorial() {
        setupLottieTutorial(myInfoBinding?.lottieTutorialNotice!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialSetting!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialSupport!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialQuiz!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialEvent!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialStats!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialFreeCharge!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialStore!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialCertificate!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialBoard!!) {}
        setupLottieTutorial(myInfoBinding?.lottieTutorialInviteFriend!!) {}
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)
        if (isVisible) {
            if (activity != null && isAdded) {
                if (!UtilK.hasUnreadEvent(requireActivity())) {
                    myInfoBinding?.ivEventNew?.visibility = View.GONE
                }
                if (!UtilK.hasUnreadNotice(requireActivity())) {
                    myInfoBinding?.ivNoticeNew?.visibility = View.GONE
                }
            }
        }
    }

    private fun setNoticeAndEvent() {
        // getNoticesIdList 대신 사용 (StartupActivity에서 불렀음)
        if (Util.getPreference(requireContext(), Const.PREF_NOTICE_LIST).isEmpty()) {
            myInfoBinding?.ivNoticeNew?.visibility = View.GONE
        } else {
            val noticeList =
                Util.getPreference(requireContext(), Const.PREF_NOTICE_LIST).split(",")
                    .toTypedArray()
            checkNewNotice(noticeList)
        }

        // getEventIdList 대신 사용
        if (Util.getPreference(requireContext(), Const.PREF_EVENT_LIST).isEmpty()) {
            myInfoBinding?.ivEventNew?.visibility = View.GONE
        } else {
            val eventList = Util.getPreference(requireContext(), Const.PREF_EVENT_LIST).split(",")
                .toTypedArray()
            checkNewEvent(eventList)
        }
    }

    private fun checkNewEvent(list: Array<String>?): Boolean {
        val ivEventNew = myInfoBinding?.ivEventNew

        val readNoticeId = Util.getPreference(requireContext(), Const.PREF_EVENT_READ)
        val readNoticeArray = readNoticeId.split(",".toRegex()).toTypedArray()

        ivEventNew?.visibility = View.GONE
        if (list == null) {
            return false
        } else if (list.isNotEmpty() && readNoticeId == "") {
            ivEventNew?.visibility = View.VISIBLE
            return true
        } else {
            var alreadyRead = 0
            for (listItem in list) {
                if (Util.isFoundString(listItem, readNoticeArray)) {
                    alreadyRead++
                }
            }

            return if (list.size == alreadyRead) {
                false
            } else {
                ivEventNew?.visibility = View.VISIBLE
                true
            }
        }
    }

    private fun checkNewNotice(list: Array<String>?): Boolean {
        val ivNoticeNew = myInfoBinding?.ivNoticeNew

        val readNoticeId = Util.getPreference(requireContext(), Const.PREF_NOTICE_READ)
        val readNoticeArray = readNoticeId.split(",".toRegex()).toTypedArray()

        Util.log("read_notice_id$readNoticeId")
        Util.log("readString$readNoticeId")

        ivNoticeNew?.visibility = View.GONE
        if (list == null) {
            return false
        } else if (list.isNotEmpty() && readNoticeId == "") {
            ivNoticeNew?.visibility = View.VISIBLE
            return true
        } else {
            var alreadyRead = 0
            for (listItem in list) {
                if (Util.isFoundString(listItem, readNoticeArray)) {
                    alreadyRead++
                }
                Util.log("already_read is$alreadyRead")
            }

            return if (list.size == alreadyRead) {
                false
            } else {
                Util.log("it's visible")
                ivNoticeNew?.visibility = View.VISIBLE
                true
            }
        }
    }

    @UnstableApi
    private fun setEventListener() = with(myInfoBinding!!) {
        val activitiesMap = mapOf(
            btnMenuBoard to Triple("menu_board_free", FreeboardActivity::class.java, CelebTutorialBits.MENU_FREE_TALK),
            btnMenuNotice to Triple("menu_notice", NoticeActivity::class.java, CelebTutorialBits.MENU_NOTICE),
            btnMenuEvent to Triple("menu_event", EventActivity::class.java, CelebTutorialBits.MENU_EVENT),
            btnMenuSupport to Triple("menu_support", SupportMainActivity::class.java, CelebTutorialBits.MENU_SUPPORT),
            btnMenuCertificate to Triple("menu_certificate", VotingCertificateListActivity::class.java, CelebTutorialBits.MENU_CERTIFICATE),
            btnMenuQuiz to Triple("menu_quiz", IdolQuizMainActivity::class.java, CelebTutorialBits.MENU_QUIZ),
            btnMenuStats to Triple("menu_record", StatsActivity::class.java, CelebTutorialBits.MENU_STATS),
            btnMenuHeartShop to Triple("menu_free_heart_charge", HeartPlusFreeActivity::class.java, CelebTutorialBits.MENU_FREE_HEART),
            btnMenuStore to Triple("menu_shop_main", NewHeartPlusActivity::class.java, CelebTutorialBits.MENU_HEART_SHOP),
            btnMenuGame to Triple("menu_game", WebViewGameActivity::class.java, CelebTutorialBits.NO_TUTORIAL),
            btnMenuSetting to Triple("menu_setting", SettingActivity::class.java, CelebTutorialBits.MENU_SETTINGS),
            btnMenuInviteFriend to Triple("menu_friend_invite", FriendInviteActivity::class.java, CelebTutorialBits.MENU_FRIEND_INVITE)
        )

        val currentTutorialIndex = TutorialManager.getTutorialIndex()

        activitiesMap.forEach { (button, actionAndActivity) ->
            button.setOnClickListener {
                handleButtonClick(button, actionAndActivity, btnMenuBoard, btnMenuSetting)
            }

            if (currentTutorialIndex == actionAndActivity.third) {
                val lottieView = when (currentTutorialIndex) {
                    CelebTutorialBits.MENU_FREE_TALK -> myInfoBinding?.lottieTutorialBoard!!
                    CelebTutorialBits.MENU_NOTICE -> myInfoBinding?.lottieTutorialNotice!!
                    CelebTutorialBits.MENU_EVENT -> myInfoBinding?.lottieTutorialEvent!!
                    CelebTutorialBits.MENU_SUPPORT -> myInfoBinding?.lottieTutorialSupport!!
                    CelebTutorialBits.MENU_CERTIFICATE -> myInfoBinding?.lottieTutorialCertificate!!
                    CelebTutorialBits.MENU_QUIZ -> myInfoBinding?.lottieTutorialQuiz!!
                    CelebTutorialBits.MENU_STATS -> myInfoBinding?.lottieTutorialStats!!
                    CelebTutorialBits.MENU_FREE_HEART -> myInfoBinding?.lottieTutorialFreeCharge!!
                    CelebTutorialBits.MENU_HEART_SHOP -> myInfoBinding?.lottieTutorialStore!!
                    CelebTutorialBits.MENU_SETTINGS -> myInfoBinding?.lottieTutorialSetting!!
                    CelebTutorialBits.MENU_FRIEND_INVITE -> myInfoBinding?.lottieTutorialInviteFriend!!
                    else -> null
                }

                lottieView?.let {
                    setupLottieTutorial(it) {
                        updateTutorial(actionAndActivity.third)
                        handleButtonClick(button, actionAndActivity, btnMenuBoard, btnMenuSetting)
                    }
                }
            }
        }
    }

    private fun handleButtonClick(
        button: View,
        actionAndActivity: Triple<String, Class<*>, Int>,
        btnMenuBoard: View,
        btnMenuSetting: View
    ) {
        if ((button == btnMenuBoard || button == btnMenuSetting) && Util.mayShowLoginPopup(baseActivity)) {
            return
        }

        if (actionAndActivity.first == "menu_shop_main") {
            startActivity(NewHeartPlusActivity.createIntent(activity))
            return
        }

        if (actionAndActivity.first == "menu_friend_invite") {
            setUiActionFirebaseGoogleAnalyticsFragment(
                GaAction.FRIEND_GO_INVITE.actionValue,
                GaAction.FRIEND_GO_INVITE.label
            )
            invite()
            return
        }

        setUiActionFirebaseGoogleAnalyticsFragment(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            actionAndActivity.first
        )
        startActivity(Intent(requireActivity(), actionAndActivity.second))
    }

    private fun updateTutorial(tutorialIndex: Int) = lifecycleScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                    Logger.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", 0L)
                    TutorialManager.init(bitmask)
                } else {
                    Toast.makeText(requireContext(), response.toString(), Toast.LENGTH_SHORT).show()
                }
            },
            errorListener = { throwable ->
                Toast.makeText(requireContext(), throwable.message ?: "Error updating tutorial", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun invite() = lifecycleScope.launch {
        val languageDeferred = async { languagePreferenceRepository.getSystemLanguage() }
        val tokenDeferred = async { getWebTokenSuspend() }

        val language = languageDeferred.await()
        when (val tokenResult = tokenDeferred.await()) {
            is TokenResult.Success -> {
                val token = tokenResult.token
                startActivity(Intent(requireContext(), FriendInviteActivity::class.java).apply {
                    putExtra(FriendInviteActivity.INVITE_PAYLOAD, InvitePayload(language, token))
                })
            }

            is TokenResult.ApiError -> {
                logE("tokenResult.response")
            }

            is TokenResult.NetworkError -> {
                logE(tokenResult.throwable.message ?: "Unknown error")
            }
        }
    }

    private suspend fun getWebTokenSuspend(): TokenResult =
        suspendCancellableCoroutine { continuation ->
            val job =
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        usersRepository.getWebToken(
                            listener = { response ->
                                if (continuation.isActive) {
                                    val success = response.optBoolean("success", false)
                                    val token =
                                        response.optString("token").takeIf { it.isNotBlank() }
                                    if (success && token != null) {
                                        continuation.resume(TokenResult.Success(token))
                                    } else {
                                        continuation.resume(TokenResult.ApiError(response))
                                    }
                                }
                            },
                            errorListener = { throwable ->
                                if (continuation.isActive) {
                                    continuation.resume(TokenResult.NetworkError(throwable))
                                }
                            }
                        )
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(TokenResult.NetworkError(e))
                        }
                    }
                }

            continuation.invokeOnCancellation {
                job.cancel()
            }
        }

    private sealed class TokenResult {
        data class Success(val token: String) : TokenResult()
        data class ApiError(val response: JSONObject) : TokenResult()
        data class NetworkError(val throwable: Throwable) : TokenResult()
    }
}