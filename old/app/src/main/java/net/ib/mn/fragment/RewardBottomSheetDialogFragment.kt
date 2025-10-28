/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 보상형 바텀시트 다이얼로그
 *
 * */

package net.ib.mn.fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.model.DailyRewardModel
import net.ib.mn.databinding.BottomSheetRewardBinding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.EventHeartModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import java.text.NumberFormat
import java.util.Locale
import java.util.Random

class RewardBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetRewardBinding

    private var mGlideRequestManager: RequestManager? = null

    private var resId: Int = 0
    private var bonusHeart: Int = 0
    private var plusHeart: Int = 0
    private var msg: String? = null
    private var didReceive: Boolean = true
    private var isButton: Boolean = false
    private var dismissCallBack: (() -> Unit)? = null
    private var moveDailyPackScreenCallBack: ((Boolean) -> Unit)? = null
    private var quizContinue: () -> Unit = {}

    private var eventHeartModel: EventHeartModel? = null
    private var dailyRewardModel: DailyRewardModel? = null

    override fun getTheme(): Int = R.style.BottomSheetDialogRewardTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        theme,
    ).apply {
        // landscape 모드에서  bottomsheet 메뉴모두  expand되지 않아서  아래 설정값 넣어줌.
        this.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mGlideRequestManager = Glide.with(this)

        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_reward, container, false)

        when (resId) {
            FLAG_LOGIN_REWARD -> setLoginRewardDialog(eventHeartModel, moveDailyPackScreenCallBack)
            FLAG_ARTICLE_VOTE -> setArticleVoteRewardDialog(bonusHeart, msg, dismissCallBack)
            FLAG_IDOL_VOTE -> setIdolVoteRewardDialog(bonusHeart, dismissCallBack)
            FLAG_WIDE_PHOTO -> setWidePhotoRewardDialog(bonusHeart, isButton, dismissCallBack)
            FLAG_QUIZ_REWARD -> setQuizMainRewardDialog(bonusHeart, didReceive)
            FLAG_ATTENDANCE_REWARD -> setAttendanceRewardDialog(dailyRewardModel, dismissCallBack)
            FLAG_ATTENDANCE_VIDEO_REWARD -> setAttendanceRewardVideoDialog(bonusHeart)
            FLAG_ARTICLE_WRITE -> setArticleWriteDialog(bonusHeart, dismissCallBack)
            FLAG_VIDEO_REWARD -> setVideoRewardDialog(bonusHeart, plusHeart, dismissCallBack)
            FLAG_SURPRISE_DIA_REWARD -> setSurpriseDiaRewardDialog(bonusHeart, msg)
            FLAG_QUIZ_CHOOSE_REWARD -> setQuizChooseRewardDialog(bonusHeart, dismissCallBack)
            FLAG_ATTENDANCE_PLUS_HEART_REWARD -> setAttendancePlusHeartRewardDialog(dailyRewardModel, dismissCallBack)
            FLAG_BURNING_REWARD -> setBurningTimeRewardDialog(eventHeartModel, moveDailyPackScreenCallBack)
            FLAG_QUIZ_CONTINUE -> setQuizContinueDialog(quizContinue, dismissCallBack)
            FLAG_MISSION_COMPLETE -> setMissionRewardDialog(bonusHeart)
            FLAG_INVITE_REWARD -> setInviteRewardDialog(bonusHeart, plusHeart, dismissCallBack)
            FLAG_GAME_REWARD -> setGameRewardDialog(bonusHeart, dismissCallBack)
        }
        return binding.root
    }

    private fun setAttendanceRewardDialog(dailyRewardModel: DailyRewardModel?, dismissCallBack: (() -> Unit)?) {

        if (dailyRewardModel == null) {
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.reward3.tvHeart.text = numberFormat.format(dailyRewardModel.amount)
        binding.reward3.clReward.visibility = View.VISIBLE
        binding.reward3.tvReward.text = dailyRewardModel.name
        binding.btnClose.visibility = View.GONE

        if (dailyRewardModel.item?.equals("diamond") == true) {
            binding.imgReview.visibility = View.INVISIBLE
            binding.imgReviewDia.visibility = View.VISIBLE
            binding.imgReviewDia.setImageResource(R.drawable.img_attendance_reward_dia)
            binding.reward3.ivHeart.setImageResource(R.drawable.img_attendance_dia_renewal)
            binding.imgReviewDia.bringToFront()
        } else {
            binding.imgReview.visibility = View.VISIBLE
            binding.imgReviewDia.visibility = View.INVISIBLE
            binding.imgReview.setImageResource(R.drawable.img_attendance_consecutive_heart)
            binding.reward3.ivHeart.setImageResource(R.drawable.img_popup_heart)
            binding.imgReview.bringToFront()
        }

        binding.clConfirm.setOnClickListener {
            dismissCallBack?.invoke()
            this.dismiss()
        }

        binding.btnClose.setOnClickListener {
            dismissCallBack?.invoke()
            this.dismiss()
        }
    }

    // 추가 하트 더 받기 다이얼로그
    private fun setAttendancePlusHeartRewardDialog(dailyRewardModel: DailyRewardModel?, dismissCallBack: (() -> Unit)?) {
        if (dailyRewardModel == null) {
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        binding.reward1.tvHeart.text = numberFormat.format(dailyRewardModel.amount)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.reward1.tvReward.text = dailyRewardModel.name
        binding.btnClose.visibility = View.GONE

        binding.imgReview.visibility = View.VISIBLE
        binding.imgReviewDia.visibility = View.INVISIBLE
        binding.imgReview.setImageResource(R.drawable.img_attendance_heart)
        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack?.invoke()
            this.dismiss()
        }

        binding.btnClose.setOnClickListener {
            this.dismiss()
        }
    }

    private fun setAttendanceRewardVideoDialog(bonusHeart: Int) {
        if(bonusHeart <= 0) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.bringToFront()
        binding.imgReview.setImageResource(R.drawable.img_popup_vote)
        binding.reward1.tvReward.text = getString(R.string.interstitial_ad_reward)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)

        binding.clConfirm.setOnClickListener {
            this.dismiss()
        }

    }

    // 로그인 시 메인에서 나오는 다이얼로그
    private fun setLoginRewardDialog(eventHeartModel: EventHeartModel?, moveDailyPackScreenCallBack: ((Boolean) -> Unit)?) {

        if (eventHeartModel == null || moveDailyPackScreenCallBack == null) {
            this.dismiss()
            return
        }

        val account = IdolAccount.getAccount(context) ?: return

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        // 맨 위에 둬야 확인 버튼이 default로 처리 됨
        if (eventHeartModel.sorryHeart > 0) {   // 쏘리하트 보상
            binding.reward3.tvReward.text = getString(R.string.reward_sorry_heart)
            binding.reward3.tvHeart.text = numberFormat.format(eventHeartModel.sorryHeart)
            binding.reward3.tvRewardDetail.text = eventHeartModel.vipMessage
            binding.reward3.clReward.visibility = View.VISIBLE
            binding.reward3.tvRewardDetail.visibility = View.VISIBLE
            binding.viewLine2.visibility = View.VISIBLE
        }

        if (eventHeartModel.dailyHeart > 0) {   //접속 보상
            if(BuildConfig.CELEB){
                binding.reward1.tvReward.text = getString(R.string.reward_daily_connection_celeb)
            }else {
                binding.reward1.tvReward.text = getString(R.string.reward_daily_connection)
            }
            binding.reward1.tvHeart.text = numberFormat.format(eventHeartModel.dailyHeart)
            binding.reward1.clReward.visibility = View.VISIBLE
            binding.tvConfirm.text = getString(R.string.receive_more_heart_after_attendance)
        }
        if (account.mDailyPackHeart > 0) {  // 데일리팩 보상
            binding.reward2.tvReward.text = getString(R.string.reward_daily_pack)
            binding.reward2.tvHeart.text = numberFormat.format(account.mDailyPackHeart)
            binding.reward2.clReward.visibility = View.VISIBLE
            binding.viewLine1.visibility = View.VISIBLE

            // 메모리 부족으로 액티비티가 재생성될 때 데일리팩 하트 팝업이 계속 나오는 문제 처리
            account.mDailyPackHeart = 0
            context?.let {
                account.saveAccount(it)
            }
        }

        binding.imgReview.setImageResource(R.drawable.img_popup_attendance)
        binding.imgReview.bringToFront()

        // 10% 확률로 데일리팩 유도 버튼 보여주기
        val r = Random()
        val n = r.nextInt(100)

        // 데일리팩 가는 조건. 하얀하트고, 어워즈 진행중 아니고, 데일리팩 구독 중 아니고, 10프로 확률로
        val goDailyPackStatus = context?.packageName.equals("net.ib.mn") &&
                ConfigModel.getInstance(context).showHeartShop &&
                (
                        account.userModel != null &&
                                (
                                        account.userModel?.subscriptions == null ||
                                                account.userModel!!.subscriptions.isEmpty()
                                        )
                        ) &&
                n < 10

        if (goDailyPackStatus) {
            binding.tvConfirm.text = String.format(getString(R.string.btn_dailypack_format), 1000)
        }

        binding.clConfirm.setOnClickListener {
            moveDailyPackScreenCallBack(goDailyPackStatus)
            this.dismiss()
        }

        binding.btnClose.setOnClickListener {
            this.dismiss()
        }
    }

    // 게시글 투표 시 나오는 다이얼로그
    private fun setArticleVoteRewardDialog(bonusHeart: Int, msg: String?, dismissCallBack: (() -> Unit)?) {
        if(dismissCallBack == null) {
            this.dismiss()
            return
        }
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        binding.imgReview.setImageResource(R.drawable.img_popup_post_vote)
        binding.reward1.tvReward.text = getString(R.string.vote_heart_title)
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)

        if (msg != null) {
            binding.reward1.tvRewardDetail.text = msg
            binding.reward1.tvRewardDetail.visibility = View.VISIBLE
        }
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
    }

    // 아이돌 투표 시 나오는 다이얼로그
    private fun setIdolVoteRewardDialog(bonusHeart: Int, dismissCallBack: (() -> Unit)?) {
        if(dismissCallBack == null) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.bringToFront()
        binding.imgReview.setImageResource(R.drawable.img_popup_vote)
        binding.reward1.tvReward.text = getString(R.string.vote_heart_title)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE

        if (bonusHeart > 0) {
            binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        } else {
            dismissCallBack.invoke()
            this.dismiss()
        }

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
    }

    private fun setWidePhotoRewardDialog(bonusHeart: Int, isButton: Boolean, dismissCallBack: (() -> Unit)?) {
        if(dismissCallBack == null) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.reward1.tvReward.text = getString(R.string.reward_heartbox)
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE

        when {
            bonusHeart == 0 -> {
                val ableVideo = Util.getPreferenceLong(context, Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY, Const.DEFAULT_VIDEO_DISABLE_TIME) == Const.DEFAULT_VIDEO_DISABLE_TIME

                with(binding) {
                    imgReview.setImageResource(R.drawable.img_popup_heartbox_0)
                    reward1.tvReward.text = getString(R.string.lable_receive_no_heart)
                    reward1.clHeart.visibility = View.GONE
                    if(isButton && ableVideo) {  // 광고 볼 수 있을 경우
                        reward1.tvRewardDetail.text =
                            getString(R.string.label_see_video_for_heartbox)
                        tvConfirm.text = String.format(
                            getString(R.string.receive_more_heart_after_video_ads),
                            numberFormat.format(ConfigModel.getInstance(context).video_heart)
                        )
                        binding.reward1.tvRewardDetail.visibility = View.VISIBLE
                        binding.btnClose.visibility = View.VISIBLE
                    }
                }
            }

            bonusHeart < 20 -> {
                binding.imgReview.setImageResource(R.drawable.img_popup_heartbox_7)
            }

            bonusHeart < 100 -> {
                binding.imgReview.setImageResource(R.drawable.img_popup_heartbox_20)
            }

            bonusHeart < 1000 -> {
                binding.imgReview.setImageResource(R.drawable.img_popup_heartbox_100)
            }

            else -> {
                binding.imgReview.setImageResource(R.drawable.img_popup_heartbox_1000)
                binding.reward1.tvRewardDetail.text = getString(R.string.reward_heartbox1000_sub)
                binding.reward1.tvRewardDetail.visibility = View.VISIBLE
            }
        }

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
        binding.btnClose.setOnClickListener {
            this.dismiss()
        }
    }

    private fun setQuizMainRewardDialog(bonusHeart: Int, didReceive: Boolean) {
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.setImageResource(R.drawable.img_popup_quiz)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)

        if(didReceive){
            binding.reward1.tvReward.text = getString(R.string.reward_quiz)
        }else{
            binding.reward1.tvReward.text = getString(R.string.reward_didnt_receive_quiz)
        }

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            this.dismiss()
        }
    }

    private fun setArticleWriteDialog(bonusHeart: Int, dismissCallBack: (() -> Unit)?) {
        if(dismissCallBack == null) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.setImageResource(R.drawable.img_popup_post_heart)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        binding.reward1.tvReward.text = getString(R.string.reward_post_article)

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
    }

    private fun setVideoRewardDialog(bonusHeart: Int, plusHeart: Int = 0, dismissCallBack: (() -> Unit)?) {
        if (dismissCallBack == null) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.setImageResource(R.drawable.img_popup_vote)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        binding.reward1.tvReward.text = getString(R.string.reward_videoad)

        Log.d("#$#$#", "11111 $plusHeart")

        if (plusHeart > 0) {
            binding.reward2.clReward.visibility = View.VISIBLE
            binding.reward2.tvHeart.text = numberFormat.format(plusHeart)
            binding.reward2.tvReward.text = getString(R.string.reward_videoad_bonus)
        }

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
    }

    private fun setInviteRewardDialog(bonusHeart: Int, plusHeart: Int = 0, dismissCallBack: (() -> Unit)?) {
        if (dismissCallBack == null) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.setImageResource(R.drawable.img_popup_invite_friend)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        binding.reward1.tvReward.text = getString(R.string.reward_invite_mission_completion)

        if (plusHeart > 0) {
            binding.reward2.clReward.visibility = View.VISIBLE
            binding.reward2.tvHeart.text = numberFormat.format(plusHeart)
            binding.reward2.tvReward.text = getString(R.string.reward_invite_mission_bonus)
        }

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
    }

    private fun setSurpriseDiaRewardDialog(bonusDia: Int, msg: String?) {
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReviewDia.setImageResource(R.drawable.img_attendance_reward_dia)
        binding.reward1.tvHeart.text = numberFormat.format(bonusDia)

        binding.btnClose.visibility = View.GONE
        binding.imgReview.visibility = View.INVISIBLE
        binding.imgReviewDia.visibility = View.VISIBLE
        binding.reward1.clReward.visibility = View.VISIBLE

        if (msg != null && context != null) {
            binding.reward1.tvReward.text = msg
            binding.reward1.tvPlus.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_light_blue
                )
            )
            binding.reward1.tvHeart.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_light_blue
                )
            )
            binding.reward1.ivHeart.setImageResource(R.drawable.img_attendance_dia_renewal)
        }

        binding.imgReviewDia.bringToFront()

        binding.clConfirm.setOnClickListener {
            this.dismiss()
        }
    }

    private fun setQuizChooseRewardDialog(bonusHeart: Int, dismissCallBack: (() -> Unit)?) {
        if (dismissCallBack == null) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.setImageResource(R.drawable.img_popup_quiz)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        binding.reward1.tvReward.text = getString(R.string.quiz_get_heart_title)

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
    }

    private fun setBurningTimeRewardDialog(
        eventHeartModel: EventHeartModel?,
        moveDailyPackScreenCallBack: ((Boolean) -> Unit)?
    ) = with(binding) {

        if (eventHeartModel == null || moveDailyPackScreenCallBack == null) {
            this@RewardBottomSheetDialogFragment.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        if (eventHeartModel.burningHeart > 0) {   // 버닝 하트 보상.
            reward3.tvReward.text = getString(R.string.popup_burningtime_title)
            reward3.tvRewardDetail.text = String.format(
                getString(R.string.popup_burningtime_subtitle),
                numberFormat.format(eventHeartModel.burningHeart)
            )
            tvConfirm.text = getString(R.string.popup_burningtime_button)

            reward3.clReward.visibility = View.VISIBLE
            reward3.clHeart.visibility = View.GONE
            reward3.tvRewardDetail.visibility = View.VISIBLE
        }

        clConfirm.setOnClickListener {
            moveDailyPackScreenCallBack(false)
            this@RewardBottomSheetDialogFragment.dismiss()
        }

        btnClose.setOnClickListener {
            this@RewardBottomSheetDialogFragment.dismiss()
        }

        imgReview.setImageResource(R.drawable.img_popup_burning_time)
        imgReview.bringToFront()
    }

    private fun setQuizContinueDialog(quizContinue: () -> Unit, dismiss: (() -> Unit)?) =
        with(binding) {
            inQuiz.root.visibility = View.VISIBLE
            tvConfirm.text = getString(R.string.continue_quiz)
            tvCancel.visibility = View.VISIBLE
            btnClose.visibility = View.GONE

            clConfirm.setOnClickListener {
                quizContinue()
                this@RewardBottomSheetDialogFragment.dismiss()
            }

            tvCancel.setOnClickListener {
                dismiss?.invoke()
                this@RewardBottomSheetDialogFragment.dismiss()
            }
            imgReview.setImageResource(R.drawable.img_popup_heartbox_0)
            imgReview.bringToFront()
        }

    private fun setMissionRewardDialog(bonusHeart: Int) {

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.reward1.tvReward.text = getString(R.string.welcome_mission_all_clear_reward_title)
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            this.dismiss()
        }
    }

    private fun setGameRewardDialog(bonusHeart: Int, dismissCallBack: (() -> Unit)?) {
        if (dismissCallBack == null) {
            this.dismiss()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.imgReview.setImageResource(R.drawable.img_popup_game)
        binding.reward1.clReward.visibility = View.VISIBLE
        binding.btnClose.visibility = View.GONE
        binding.reward1.tvHeart.text = numberFormat.format(bonusHeart)
        binding.reward1.tvReward.text = getString(R.string.reward_game_result)

        binding.imgReview.bringToFront()

        binding.clConfirm.setOnClickListener {
            dismissCallBack.invoke()
            this.dismiss()
        }
    }

    companion object {
        const val FLAG_LOGIN_REWARD = 0
        const val FLAG_IDOL_VOTE = 1
        const val FLAG_ARTICLE_VOTE = 2
        const val FLAG_WIDE_PHOTO = 3
        const val FLAG_QUIZ_REWARD = 4
        const val FLAG_ATTENDANCE_REWARD = 5
        const val FLAG_ARTICLE_WRITE = 6
        const val FLAG_VIDEO_REWARD = 7
        const val FLAG_SURPRISE_DIA_REWARD = 8
        const val FLAG_QUIZ_CHOOSE_REWARD = 9
        const val FLAG_ATTENDANCE_VIDEO_REWARD = 10
        const val FLAG_ATTENDANCE_PLUS_HEART_REWARD = 11
        const val FLAG_BURNING_REWARD = 12
        const val FLAG_QUIZ_CONTINUE = 13
        const val FLAG_MISSION_COMPLETE = 14
        const val FLAG_INVITE_REWARD = 15
        const val FLAG_GAME_REWARD = 16


        @JvmStatic
        fun newInstance(resId: Int, bonusHeart: Int): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.bonusHeart = bonusHeart
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, bonusHeart: Int, msg: String): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.bonusHeart = bonusHeart
            f.msg = msg
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, bonusHeart: Int, msg: String, dismissCallBack: (() -> Unit)?): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.bonusHeart = bonusHeart
            f.msg = msg
            f.dismissCallBack = dismissCallBack
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, bonusHeart: Int, isButton: Boolean, callback: (() -> Unit)?): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.bonusHeart = bonusHeart
            f.dismissCallBack = callback
            f.isButton = isButton
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, bonusHeart: Int, dismissCallBack: (() -> Unit)?): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.bonusHeart = bonusHeart
            f.dismissCallBack = dismissCallBack
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, bonusHeart: Int, plusHeart: Int = 0, dismissCallBack: (() -> Unit)?): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.bonusHeart = bonusHeart
            f.plusHeart = plusHeart
            f.dismissCallBack = dismissCallBack
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, eventHeartModel: EventHeartModel?, moveDailyPackScreenCallBack: ((Boolean) -> Unit)?): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.eventHeartModel = eventHeartModel
            f.moveDailyPackScreenCallBack = moveDailyPackScreenCallBack
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, bonusHeart: Int, didReceive: Boolean): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.bonusHeart = bonusHeart
            f.didReceive = didReceive
            return f
        }

        fun newInstance(resId: Int, dailyRewardModel: DailyRewardModel?, dismissCallBack: (() -> Unit)?): RewardBottomSheetDialogFragment {
            val f = RewardBottomSheetDialogFragment()
            f.resId = resId
            f.dailyRewardModel = dailyRewardModel
            f.dismissCallBack = dismissCallBack
            return f
        }

        fun newInstance(
            resId: Int,
            quizContinue: () -> Unit,
            dismiss: () -> Unit
        ): RewardBottomSheetDialogFragment {
            return RewardBottomSheetDialogFragment().apply {
                this.resId = resId
                this.quizContinue = quizContinue
                this.dismissCallBack = dismiss
            }
        }

    }
}