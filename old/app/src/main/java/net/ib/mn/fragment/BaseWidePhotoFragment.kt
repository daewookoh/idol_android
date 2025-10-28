/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.BaseWidePhotoViewModel
import net.ib.mn.viewmodel.BaseWidePhotoViewModelFactory
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
open class BaseWidePhotoFragment : BaseDialogFragment() {

    lateinit var baseWidePhotoViewModel: BaseWidePhotoViewModel
    val mHeartBoxSendHandler = MyHandler(this)
    var targetSdkVersion: Int = 0
    var isInquiry: Boolean = false
    protected var isAggregatingTime = false
    @Inject
    lateinit var articlesRepository : ArticlesRepositoryImpl
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var videoAdUtil: VideoAdUtil
    @Inject
    lateinit var getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAggregatingTime = Util.getPreferenceBool(activity, Const.PREF_IS_AGGREGATING_TIME, false)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomDialog)

        // 현재 application info를 가지고와서 현재  타겟 sdk  값을  알아낸다.
        val packageInfo: PackageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
        val applicationInfo: ApplicationInfo? = packageInfo.applicationInfo

        targetSdkVersion = applicationInfo?.targetSdkVersion ?: 0
        baseWidePhotoViewModel.articleModel(this.requireArguments().getSerializable(PARAM_MODEL) as ArticleModel)
        isInquiry = this.requireArguments().getBoolean(PARAM_IS_INQUIRY)
        val articleId = baseWidePhotoViewModel.articleModel.id
        if(articleId.isNotEmpty() && articleId.toLong() > 0) {
            baseWidePhotoViewModel.viewCountArticle(context, baseWidePhotoViewModel.articleModel.id.toLong())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeVM()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseWidePhotoViewModel = ViewModelProvider(
            this, BaseWidePhotoViewModelFactory(context, articlesRepository, getIsEnableVideoAdPrefsUseCase)
        )[BaseWidePhotoViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            if (dialog.window != null) {
                dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            }
        }
    }

    @SuppressLint("WrongConstant", "UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().registerReceiver(baseWidePhotoViewModel.downloadCompleteBroadCastReceiver, filter, RECEIVER_EXPORTED)
        } else {
            requireActivity().registerReceiver(baseWidePhotoViewModel.downloadCompleteBroadCastReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(baseWidePhotoViewModel.downloadCompleteBroadCastReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BaseActivity.Companion.MEZZO_PLAYER_REQ_CODE) {
            Util.handleVideoAdResult(
                baseActivity, false, true, requestCode, resultCode, data, "heartbox_videoad"
            ) { adType: String? ->
                //비광 시청 후 처리
                videoAdUtil.onVideoSawCommon(baseActivity, true, adType) {}
            }
        }
    }

    private fun observeVM() = with(baseWidePhotoViewModel) {
        moveScreenToVideo.observe(viewLifecycleOwner, SingleEventObserver { isEnable ->
            if (isEnable) {
                startActivityForResult(
                    MezzoPlayerActivity.createIntent(context, Const.ADMOB_REWARDED_VIDEO_HEARTBOX_UNIT_ID),
                    BaseActivity.Companion.MEZZO_PLAYER_REQ_CODE
                )
            } else {
                val dialogFragment = AdExceedDialogFragment()
                dialogFragment.show(parentFragmentManager, "AdExceedDialogFragment")
            }
        })
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        when(requestCode) {
            REQUEST_READ_SMS -> {
                // BEGIN_INCLUDE(permission_result)
                // Received permission result for camera permission.
                Util.log("Received response for read phone state permission request.")

                // Check if the only required permission has been granted
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // read phone state permission has been granted, preview can be displayed
                    if (activity != null && isAdded) {
                        Toast.makeText(context, getString(R.string.msg_heart_box_event_ok), Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                // Log.i(TAG, "read phone state permission was NOT granted.")
                if (activity != null && isAdded) {
                    Toast.makeText(context, getString(R.string.msg_heart_box_event_fail), Toast.LENGTH_SHORT).show()
                }
            }
            BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // Check if the only required permission has been granted
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // read phone state permission has been granted, preview can be displayed
                    if (activity != null && isAdded) {
                        Toast.makeText(context, getString(R.string.msg_download_ok), Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                // Log.i(TAG, "read phone state permission was NOT granted.")
                if (activity != null && isAdded) {
                    Toast.makeText(context, getString(R.string.msg_download_fail), Toast.LENGTH_SHORT).show()
                }

            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    class MyHandler(fragment: BaseWidePhotoFragment) : Handler() {
        private val mFragment: WeakReference<BaseWidePhotoFragment> = WeakReference(fragment)

        override fun handleMessage(msg: Message) {
            val fragment = mFragment.get()
            fragment?.handleMessage(msg)
        }
    }

    // Handler 에서 호출하는 함수
    private fun handleMessage(msg: Message) {
        if (activity != null && isAdded) {
            lifecycleScope.launch {
                usersRepository.provideHeart(
                    type = "heartbox",
                    listener = { response ->
                        if (!response.optBoolean("success")) {
                            return@provideHeart
                        }
                        Util.closeProgress()
                        if (activity != null && isAdded) {
                            if (response.optBoolean("viewable")) {
                                Util.setPreference(activity, Const.PREF_HEART_BOX_VIEWABLE, true)
                            } else {
                                Util.setPreference(activity, Const.PREF_HEART_BOX_VIEWABLE, false)
                            }
                            val heartBox = response.optLong("heart")
                            val isButton: Boolean = response.optBoolean("button")
                            setRewardBottomSheetFragment(heartBox.toInt(), isButton)
                        }
                    }, errorListener = { throwable ->
                    }
                )
            }
        }
    }

    private fun setRewardBottomSheetFragment(bonusHeart: Int, isButton: Boolean) {
        val mBottomSheetDialogFragment = RewardBottomSheetDialogFragment.newInstance(RewardBottomSheetDialogFragment.FLAG_WIDE_PHOTO, bonusHeart, isButton) {
            val ableVideo = Util.getPreferenceLong(context, Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY, Const.DEFAULT_VIDEO_DISABLE_TIME) == Const.DEFAULT_VIDEO_DISABLE_TIME

            if (bonusHeart == 0 && isButton && ableVideo) {
                baseWidePhotoViewModel.moveScreenToVideo()
            }
        }

        val tag = "reward_wide_photo"
        val oldTag: Fragment? = childFragmentManager.findFragmentByTag(tag)
        if (oldTag == null) {
            mBottomSheetDialogFragment.show(childFragmentManager, tag)
        }
    }

    companion object {
        const val REQUEST_READ_SMS = 0
        const val MEZZO_PLAYER_REQ_CODE = 900

        const val PARAM_MODEL = "paramModel"
        const val PARAM_IS_INQUIRY = "paramIsInquiry"
    }
}