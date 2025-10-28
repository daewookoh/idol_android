package net.ib.mn.attendance

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.attendance.adapter.AttendanceAdapter
import net.ib.mn.core.data.repository.StampsRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.PostVideoAdNotificationUseCase
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.ActivityAttendanceBinding
import net.ib.mn.dialog.VideoAdNotifyToastFragment
import net.ib.mn.domain.usecase.datastore.GetAdCountUseCase
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.domain.usecase.datastore.IsSetAdNotificationPrefsUseCase
import net.ib.mn.domain.usecase.datastore.SetAdNotificationPrefsUseCase
import net.ib.mn.fragment.AdExceedDialogFragment
import net.ib.mn.fragment.AdsBottomSheetFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.DailyRewardModel
import net.ib.mn.model.StampModel
import net.ib.mn.model.StampRewardModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.RewardVideoManager
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.AttendanceViewModel
import net.ib.mn.viewmodel.AttendanceViewModelFactory
import org.json.JSONObject
import java.text.NumberFormat
import javax.inject.Inject

@AndroidEntryPoint
class AttendanceActivity : BaseActivity() {

    private lateinit var binding: ActivityAttendanceBinding
    private lateinit var attendanceAdapter: AttendanceAdapter

    private lateinit var attendanceViewModel: AttendanceViewModel

    private lateinit var rewardVideoAdManager: RewardVideoManager
    private var rewardAdDialog: Dialog? = null
    @Inject
    lateinit var videoAdUtil: VideoAdUtil
    @Inject
    lateinit var sharedAppState: SharedAppState
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var stampsRepository: StampsRepository
    @Inject
    lateinit var getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase
    @Inject
    lateinit var getAdCountUseCase: GetAdCountUseCase
    @Inject
    lateinit var postVideoAdNotificationUseCase: PostVideoAdNotificationUseCase
    @Inject
    lateinit var setAdNotificationUseCase: SetAdNotificationPrefsUseCase
    @Inject
    lateinit var isSetAdNotificationUseCase: IsSetAdNotificationPrefsUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_attendance)
        binding.clContainer.applySystemBarInsets()

        ViewCompat.setOnApplyWindowInsetsListener(binding.snackbarAnchor) { v, insets ->
            val bottom = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            v.layoutParams = v.layoutParams.apply { height = bottom }
            insets
        }
        
        attendanceViewModel = ViewModelProvider(
            this@AttendanceActivity, AttendanceViewModelFactory(this, usersRepository, stampsRepository, getIsEnableVideoAdPrefsUseCase, SavedStateHandle())
        )[AttendanceViewModel::class.java]

        rewardVideoAdManager = RewardVideoManager.getInstance()
        getDataFromVM()
        observeState()
        initSet()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MEZZO_PLAYER_REQ_CODE) {//비광 보고나서
            Util.handleVideoAdResult(
                this, false, true, requestCode, resultCode, data, GaAction.ATTENDANCE_VIDEO_AD.label
            ) { adType: String? ->
                videoAdUtil.onVideoSawCommon(this, true, adType) {
                    // no-op
                }
            }
        }
    }

    private fun initSet() {
        val actionbar = supportActionBar
        actionbar?.setTitle(R.string.attendance_check)

        Util.showProgress(this, true)
        attendanceViewModel.getAttendanceInfo(this)
    }

    private fun observeState() {
        lifecycleScope.launch {
            sharedAppState.isIncitePushHidden.collect {
                if (::attendanceAdapter.isInitialized) {
                    attendanceAdapter.notifyItemChanged(AttendanceAdapter.ATTENDANCE_HEADER)
                }
            }
        }
    }

    private fun getDataFromVM() {
        attendanceViewModel.getAttendanceInfoSuccess.observe(
            this,
            SingleEventObserver { attendanceSaveStateModel ->
                with(attendanceSaveStateModel) {
                    attendanceAdapter = AttendanceAdapter(user, stamp, rewards, lifecycleScope, sharedAppState)

                    binding.rvAttendance.apply {
                        adapter = attendanceAdapter
                    }

                    setItemListener()

                    attendanceViewModel.getDailyRewards(this@AttendanceActivity)
                }
            },
        )

        attendanceViewModel.errorToast.observe(
            this,
            SingleEventObserver { message ->
                Util.closeProgress()
                Util.showDefaultIdolDialogWithBtn1(
                    this@AttendanceActivity,
                    null,
                    message,
                    { Util.closeIdolDialog() },
                    true,
                )
            },
        )

        attendanceViewModel.setStampeSuccess.observe(
            this,
            SingleEventObserver { attendanceSaveStateModel ->
                with(attendanceSaveStateModel) {
                    attendanceAdapter.refreshGraphData(
                        stamp = stamp,
                        rewards = rewards,
                        successOfSetStamp,
                    )

                    if (!successOfSetStamp) {
                        return@with
                    }

                    calculateConsecutiveReward(stamp, rewards)
                }
            },
        )

        attendanceViewModel.getDailyRewards.observe(
            this,
            SingleEventObserver { dailyRewardList ->
                if (!::attendanceAdapter.isInitialized) {
                    return@SingleEventObserver
                }
                val copyRewardList = dailyRewardList.map { it.copy() }
                attendanceAdapter.setDailyRewards(copyRewardList)
                attendanceAdapter.submitList(copyRewardList)
                Util.closeProgress()
            },
        )

        attendanceViewModel.postDailyRewards.observe(this,
            SingleEventObserver { rewardedModel ->
                Util.closeProgress()

                showRewardPlusHeartBottomSheetDialog(rewardedModel)
            }
        )

        attendanceViewModel.moveScreenToVideo.observe(this, SingleEventObserver{ isEnable ->
            if (isEnable) {
                val endTime = Util.getPreferenceLong(
                    this@AttendanceActivity,
                    Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
                    Const.DEFAULT_VIDEO_DISABLE_TIME
                )

                val currentTime = System.currentTimeMillis()
                val isTimerRunning = endTime != Const.DEFAULT_VIDEO_DISABLE_TIME && endTime > currentTime

                if (isTimerRunning) {
                    lifecycleScope.launch {
                        val isSet: Boolean = isSetAdNotificationUseCase()
                            .mapDataResource { it }
                            .awaitOrThrow() ?: false

                        withContext(Dispatchers.Main) {
                            Util.showVideoAdDisableTimerDialog(
                                this@AttendanceActivity,
                                supportFragmentManager,
                                { v1: View? ->

                                    UtilK.videoDisableTimer(this@AttendanceActivity, null, null,null)
                                    Util.closeIdolDialog()
                                },
                                isAlreadySetNotification = isSet,
                                false,
                            ) {
                                lifecycleScope.launch {
                                    postVideoAdNotificationUseCase().collectLatest { result ->
                                        if (result.success) {
                                            VideoAdNotifyToastFragment().show(supportFragmentManager, "VideoAdNotifyToast")
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@AttendanceActivity, result.error?.message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                    setAdNotificationUseCase(true)
                                        .mapDataResource { it }
                                        .awaitOrThrow()
                                }
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch {
                        setAdNotificationUseCase(false)
                            .mapDataResource { it }
                            .awaitOrThrow()
                    }
                    startActivityForResult(
                        MezzoPlayerActivity.createIntent(this, Const.ADMOB_REWARDED_VIDEO_LEVELREWARD_UNIT_ID),
                        MEZZO_PLAYER_REQ_CODE
                    )
                }
            } else {
                val dialogFragment = AdExceedDialogFragment()
                dialogFragment.show(this.supportFragmentManager, "AdExceedDialogFragment")
            }
        })

        attendanceViewModel.moveToLink.observe(this, SingleEventObserver { link ->
            val intent = Intent(this@AttendanceActivity, AppLinkActivity::class.java)
                .apply { data = Uri.parse(link) }
            startActivity(intent)
        })
    }

    override fun onResume() {
        super.onResume()
        if (NotificationManagerCompat.from(this).areNotificationsEnabled() && ::attendanceAdapter.isInitialized) {
            attendanceAdapter.notifyItemChanged(AttendanceAdapter.ATTENDANCE_HEADER)
        }

        Util.checkVideoAdTimer(this)
        handleAdResult()
    }

    private fun handleAdResult() {
        when (val result = RewardVideoManager.adResult) {
            is RewardVideoManager.Companion.AdResult.Rewarded -> {
                showRewardAdBottomSheetDialog(result.amount)
            }
            is RewardVideoManager.Companion.AdResult.Closed -> {
                Util.showIdolDialogWithBtn1(
                    this,
                    null,
                    getString(R.string.video_ad_cancelled),
                ) { Util.closeIdolDialog() }
            }
            is RewardVideoManager.Companion.AdResult.Error -> {
                if (BuildConfig.DEBUG) {
                    IdolSnackBar.make(
                        findViewById(android.R.id.content),
                        "ERROR_MESSAGE : ${result.message}"
                    ).show()
                }
            }
            null -> { /* Do nothing */ }
        }
        RewardVideoManager.adResult = null // Consume the event
    }

    private fun setItemListener() {
        attendanceAdapter.setAttendanceHeaderListener(object :
            AttendanceAdapter.AttendanceHeaderListener {
            override fun btnAttendanceCheckClick(showAvailableAccount: Boolean, user: UserModel) {

                val isAbleAttendanceCheck = Util.getPreferenceBool(
                    this@AttendanceActivity,
                    Const.PREF_IS_ABLE_ATTENDANCE,
                    false
                )

                if (!isAbleAttendanceCheck) {
                    IdolSnackBar.make(findViewById(android.R.id.content), getString(R.string.stamp_already_done_and_retry))
                        .also { snack -> snack.setAnchorView(binding.snackbarAnchor) } 
                        .show()
                    return
                }

                if (showAvailableAccount) {
                    Util.showDefaultIdolDialogWithRedBtn2(
                        this@AttendanceActivity,
                        getString(R.string.attendance_available_account_title),
                        getString(R.string.attendance_available_account_subtitle),
                        user.nickname,
                        R.string.yes,
                        R.string.no,
                        true, false, false, false, {
                            attendanceViewModel.setStamp(this@AttendanceActivity)
                            Util.closeIdolDialog()
                        }
                    ) {
                        Util.closeIdolDialog()
                    }

                    return
                }

                attendanceViewModel.setStamp(this@AttendanceActivity)
            }
        })

        attendanceAdapter.setAttendanceHeartMoreListener(object :
            AttendanceAdapter.AttendanceHeartMoreListener {
            override fun btnLevelMoreClick(dailyRewardModel: DailyRewardModel) {
                with(GaAction.ATTENDANCE_CHECK_REWARD) {
                    setUiActionFirebaseGoogleAnalyticsActivityWithKey(
                        actionValue,
                        label,
                        mapOf(paramKey to dailyRewardModel.key)
                    )
                }

                Util.showProgress(this@AttendanceActivity, true)
                attendanceViewModel.postDailyRewards(context = this@AttendanceActivity, key = dailyRewardModel.key ?: return)
            }

            override fun btnAdsClick(dailyRewardModel: DailyRewardModel) {

                with(GaAction.ATTENDANCE_CHECK_REWARD) {
                    setUiActionFirebaseGoogleAnalyticsActivityWithKey(
                        actionValue,
                        label,
                        mapOf(paramKey to dailyRewardModel.key)
                    )
                }

                showAdsDialog(dailyRewardModel)
            }

            override fun btnLinkClick(dailyRewardModel: DailyRewardModel) {

                with(GaAction.ATTENDANCE_CHECK_REWARD) {
                    setUiActionFirebaseGoogleAnalyticsActivityWithKey(
                        actionValue,
                        label,
                        mapOf(paramKey to dailyRewardModel.key)
                    )
                }

                if (dailyRewardModel.linkUrl == null) {
                    return
                }

                dailyRewardModel.key?.let {
                    if (dailyRewardModel.key == GO_FREE_HEART_KEY) {
                        val intent = Intent(this@AttendanceActivity, AppLinkActivity::class.java)
                            .apply { data = Uri.parse(dailyRewardModel.linkUrl) }
                        startActivity(intent)

                        return
                    }
                    attendanceViewModel.postDailyRewards(context = this@AttendanceActivity, key = dailyRewardModel.key, link = dailyRewardModel.linkUrl)
                }
            }

            override fun btnVideoAdClick(dailyRewardModel: DailyRewardModel) {
                attendanceViewModel.moveScreenToVideo()
            }
        })
    }

    private fun showRewardAdDialog() {
        rewardAdDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        rewardAdDialog?.window!!.attributes = lpWindow
        rewardAdDialog?.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )

        rewardAdDialog?.setContentView(R.layout.dialog_reward_ad)
        rewardAdDialog?.setCanceledOnTouchOutside(false)
        rewardAdDialog?.setCancelable(false)

        val tvTitle: AppCompatTextView = rewardAdDialog!!.findViewById(R.id.tv_title)
        val tvMsg: AppCompatTextView = rewardAdDialog!!.findViewById(R.id.tv_msg)
        val btnCancel: AppCompatButton = rewardAdDialog!!.findViewById(R.id.btn_cancel)
        val btnOK = rewardAdDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)

        val amount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this))
            .format(ConfigModel.getInstance(this).video_heart).toString()
        tvMsg.text = Util.getColorText(
            String.format(getString(R.string.ad_intend_guide), amount),
            amount,
            ContextCompat.getColor(this, R.color.main),
        )
        tvTitle.text = getString(R.string.reward_sorry_heart)

        btnCancel.setOnClickListener {
            rewardAdDialog?.dismiss()
        }
        btnOK.setOnClickListener {
            Util.showLottie(this@AttendanceActivity, true)
            if (rewardAdDialog?.isShowing == true) {
                val account = getAccount(this@AttendanceActivity)
                val userId = account?.userModel?.id ?: 0
                
                rewardVideoAdManager.loadRewardAd(this, userId, object: RewardVideoManager.OnAdLoadListener {
                    override fun onAdLoadSuccess() {
                        Util.closeProgress()
                        rewardVideoAdManager.show(this@AttendanceActivity)
                    }

                    override fun onAdLoadFailed(loadAdError: LoadAdError) {
                        Util.closeProgress()
                        if (!BuildConfig.DEBUG) {
                            return
                        }
                        val errorCode = loadAdError.code
                        val errorMessage = loadAdError.message
                        IdolSnackBar.make(
                            findViewById(android.R.id.content),
                            "ERROR_CODE : $errorCode ERROR_MESSAGE : $errorMessage"
                        ).show()
                    }
                })
                rewardAdDialog?.cancel()
            }
        }

        rewardAdDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        rewardAdDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        rewardAdDialog?.dismiss()
    }

    private fun showAdsDialog(dailyRewardModel: DailyRewardModel) {

        val sheet =
            AdsBottomSheetFragment.newInstance(
                AdsBottomSheetFragment.FLAG_ADS,
                dailyRewardModel,
                onWebViewClick = {
                    Util.showProgress(this@AttendanceActivity, true)
                    attendanceViewModel.postDailyRewards(
                        context = this@AttendanceActivity,
                        key = dailyRewardModel.key ?: return@newInstance
                    )
                })

        val tag = "ads_dialog"
        val oldFrag = this.supportFragmentManager.findFragmentByTag(tag)

        if (oldFrag == null) {
            sheet.show((this).supportFragmentManager, tag)
        }

        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.BOTTOM_SHEET_AD_SHOW.actionValue,
            GaAction.BOTTOM_SHEET_AD_SHOW.label
        )
    }

    private fun showRewardBottomSheetDialog(rewardedModel: DailyRewardModel) {

        val rewardBottomSheetFragment = RewardBottomSheetDialogFragment.newInstance(
            RewardBottomSheetDialogFragment.FLAG_ATTENDANCE_REWARD,
            dailyRewardModel = rewardedModel,
            dismissCallBack = {
                val layoutManager = binding.rvAttendance.layoutManager as LinearLayoutManager
                layoutManager.scrollToPositionWithOffset(1, 0)
            }
        )

        val tag = "attendance_reward_dialog"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            rewardBottomSheetFragment.show(supportFragmentManager, tag)
        }
    }

    private fun showRewardPlusHeartBottomSheetDialog(rewardedModel: DailyRewardModel) {

        val rewardBottomSheetFragment = RewardBottomSheetDialogFragment.newInstance(
            RewardBottomSheetDialogFragment.FLAG_ATTENDANCE_PLUS_HEART_REWARD,
            rewardedModel,
            dismissCallBack = {
                if (rewardedModel.key == LEVEL_REWARD_KEY && !BuildConfig.CHINA) {
                    showRewardAdDialog()
                } else {
                    val layoutManager = binding.rvAttendance.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(1, 0)
                }
            }
        )

        val tag = "attendance_reward_plus_heart_dialog"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            rewardBottomSheetFragment.show(supportFragmentManager, tag)
        }
    }

    private fun showRewardAdBottomSheetDialog(bonusHeart: Int) {
        binding.root.post {
            if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) {
                return@post
            }
            val rewardBottomSheetFragment = RewardBottomSheetDialogFragment.newInstance(
                RewardBottomSheetDialogFragment.FLAG_ATTENDANCE_VIDEO_REWARD, ConfigModel.getInstance(this).video_heart)

            val tag = "attendance_reward_ad_dialog"
            val oldFrag = supportFragmentManager.findFragmentByTag(tag)
            if (oldFrag == null) {
                rewardBottomSheetFragment.show(supportFragmentManager, tag)
            }
        }
    }

    private fun calculateConsecutiveReward(
        stamp: JSONObject,
        rewards: List<Map<String, StampRewardModel>>
    ) {
        val stampModel =
            IdolGson.getInstance().fromJson(stamp.toString(), StampModel::class.java)
        val existStampDay = AttendanceDays.values().find { it.days == stampModel.days }

        val rewardModel = rewards.find {
            it.keys.contains(existStampDay?.day)
        }

        val stampReward = rewardModel?.get(existStampDay?.day)

        if (stampReward == null) {
            if (rewardVideoAdManager.rewardedAd != null && !BuildConfig.CHINA && rewardVideoAdManager.rewardAmount != 0) {
                showRewardAdDialog()
            }
            return
        }

        val rewardedModel: DailyRewardModel
        val consecutiveMsg: String

        if (stampReward.heart > 0) {
            consecutiveMsg = String.format(
                getString(
                    R.string.reward_attendance_heart
                ),
                stampModel.days.toString()
            )
            rewardedModel = DailyRewardModel(
                amount = stampReward.heart,
                name = consecutiveMsg,
                item = "heart",
            )
        } else {
            consecutiveMsg = String.format(
                getString(
                    R.string.reward_attendance_diamond,
                ),
                stampModel.days.toString()
            )

            rewardedModel = DailyRewardModel(
                amount = stampReward.diamond,
                name = consecutiveMsg,
                item = "diamond",
            )
        }

        showRewardBottomSheetDialog(rewardedModel)
    }

    companion object {
        private const val LEVEL_REWARD_KEY = "level_heart"
        private const val GO_FREE_HEART_KEY = "go_freeheart"

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, AttendanceActivity::class.java)
        }
    }
}