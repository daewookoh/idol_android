/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.activity.BoardActivity
import net.ib.mn.activity.EventActivity
import net.ib.mn.activity.FacedetectActivity
import net.ib.mn.activity.FreeboardActivity
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.activity.IdolQuizMainActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.activity.NoticeActivity
import net.ib.mn.activity.StatsActivity
import net.ib.mn.activity.VotingCertificateListActivity
import net.ib.mn.activity.WebViewGameActivity
import net.ib.mn.attendance.AttendanceActivity
import net.ib.mn.feature.friend.FriendInviteActivity
import net.ib.mn.feature.menu.IconMenuList
import net.ib.mn.feature.menu.listener.MenuIconItemClickListener
import net.ib.mn.feature.menu.listener.MenuTextItemClickListener
import net.ib.mn.support.SupportMainActivity
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.GlobalVariable
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.RemoteConfigUtil
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.SupportedLanguage
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.MainViewModel
import javax.inject.Inject

/**
 * @see
 * */
@AndroidEntryPoint
class MyInfoFragment : MyInfoBaseFragment(), OnScrollToTopListener {
    @Inject
    lateinit var sharedAppState: SharedAppState

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BuildConfig.CELEB) {
            if (requireActivity().isFinishing) {
                return
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()

        lifecycleScope.launch {
            combine(
                sharedAppState.hasUnreadNotice,
                sharedAppState.hasUnreadEvent,
                sharedAppState.isAttendanceAvailable
            ) {_, _, _ -> }
                .collect {
                    initUI()
                }
        }

        mainViewModel.inviteData.observe(viewLifecycleOwner, SingleEventObserver { payload ->
            startActivity(Intent(requireContext(), FriendInviteActivity::class.java).apply {
                putExtra(FriendInviteActivity.INVITE_PAYLOAD, payload)
            })
        })
        RemoteConfigUtil.fetchRemoteConfigIfNull(requireContext()) { isSuccess ->
            if (isSuccess) {
                initUI()
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)
        if(isAdded && isVisible) {
            // 공지/이벤트를 모두 읽거나 새 공지/이벤트가 추가되거나 하면 뱃지 갱신
            sharedAppState.setUnreadNotice(UtilK.hasUnreadNotice(requireContext()))
            sharedAppState.setUnreadEvent(UtilK.hasUnreadEvent(requireContext()))
        }
    }

    override fun onScrollToTop() {
        myInfoBinding?.menuScroll?.scrollTo(0, 0)
    }

    private fun initUI() {
        val isBasicUI = LocaleUtil.isExistCurrentLocale(
            requireContext(),
            SupportedLanguage.BOARD_KIN_QUIZZES_TOP100_LOCALES
        )

        myInfoBinding?.let {
            it.layoutAccount.visibility = View.GONE

            myInfoBinding!!.cvMenu.setContent {
                MaterialTheme {
                    IconMenuList(
                        isBasicUI = isBasicUI,
                        showGame = GlobalVariable.RemoteConfig?.game?.showMenu == true,
                        menuIconItemClickListener = object : MenuIconItemClickListener {
                            override fun onSupportClick() {
                                setButtonPressFirebaseEvent("menu_support")
                                startActivity(SupportMainActivity.createIntent(context ?: return))
                            }

                            override fun onFreeChargeClick() {
                                setButtonPressFirebaseEvent("menu_free_heart_charge")
                                startActivity(HeartPlusFreeActivity.createIntent(context ?: return))
                            }

                            override fun onAttendanceClick() {
                                setUiActionFirebaseGoogleAnalyticsFragment(
                                    GaAction.MENU_ATTENDANCE_CHECK.actionValue,
                                    GaAction.MENU_ATTENDANCE_CHECK.label,
                                )
                                startActivity(AttendanceActivity.createIntent(context ?: return))
                            }

                            override fun onEventClick() {
                                setButtonPressFirebaseEvent("menu_event")
                                startActivity(EventActivity.createIntent(context ?: return))
                            }

                            override fun onStoreClick() {
                                setButtonPressFirebaseEvent("menu_shop_main")

                                if (Util.mayShowLoginPopup(baseActivity)) {
                                    return
                                }
                                startActivity(NewHeartPlusActivity.createIntent(activity))
                            }

                            override fun onNoticeClick() {
                                setButtonPressFirebaseEvent("menu_notice")
                                startActivity(NoticeActivity.createIntent(context ?: return))
                            }

                            @OptIn(UnstableApi::class)
                            override fun onFreeBoardClick() {
                                setButtonPressFirebaseEvent("menu_board")
                                startActivity(BoardActivity.createIntent(context ?: return))
                            }
                        },
                        menuTextItemClickListener = object : MenuTextItemClickListener {
                            override fun onVoteCertificateClick() {
                                setButtonPressFirebaseEvent("menu_vote_certificates")
                                val i = Intent(requireContext(), VotingCertificateListActivity::class.java)
                                i.putExtra(Const.CHART_CODE_FOR_CERTIFICATE, mainViewModel.mainChartModel)
                                startActivity(i)
                            }

                            override fun onStoreClick() {
                                setButtonPressFirebaseEvent("menu_shop_main")

                                if (Util.mayShowLoginPopup(baseActivity)) {
                                    return
                                }
                                startActivity(NewHeartPlusActivity.createIntent(activity))
                            }

                            override fun onNoticeClick() {
                                setButtonPressFirebaseEvent("menu_notice")
                                startActivity(NoticeActivity.createIntent(context ?: return))
                            }

                            override fun onInviteFriendClick() {
                                setButtonPressFirebaseEvent("menu_go_invite_page")
                                mainViewModel.invite()
                            }

                            override fun onHistoryClick() {
                                setButtonPressFirebaseEvent("menu_record")
                                mainViewModel.mainChartModel
                                val i = Intent(requireContext(), StatsActivity::class.java)
                                i.putExtra(StatsActivity.PARAM_HISTORY_CHART_CODE, mainViewModel.getHistoryChartModel())
                                startActivity(i)
                            }

                            override fun onQuizClick() {
                                setButtonPressFirebaseEvent("menu_quiz")
                                startActivity(IdolQuizMainActivity.createIntent(context ?: return))
                            }

                            override fun onFaceClick() {
                                setButtonPressFirebaseEvent("menu_face")
                                startActivity(FacedetectActivity.createIntent(context ?: return))
                            }

                            @OptIn(UnstableApi::class)
                            override fun onFreeBoardClick() {
                                setButtonPressFirebaseEvent("menu_board")
                                startActivity(BoardActivity.createIntent(context ?: return))
                            }
                            
                            override fun onGameClick() {
                                val intent = Intent(requireContext(), WebViewGameActivity::class.java)
                                startActivity(intent)
                            }
                        },
                        onUpdateTutorial = {
                            mainViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                        }
                    )
                }
            }
        }
    }
}