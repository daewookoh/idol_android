/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 바텀시트 광고 fragment. Theme Style 때문에 기존 BottomSheetFragment와 분리
 *
 * */

package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.webkit.*
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.RequestManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.R
import net.ib.mn.utils.GaAction
import com.bumptech.glide.Glide
import net.ib.mn.attendance.AttendanceActivity
import net.ib.mn.model.DailyRewardModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.AttendanceViewModel


class AdsBottomSheetFragment : BottomSheetDialogFragment(){
    private var mGlideRequestManager: RequestManager? = null

    private val attendanceViewModel: AttendanceViewModel by activityViewModels()

    val KEY_RESID = "resid"
    val KEY_FLAG = "flag"
    val KEY_DYNAMIC = "dynamic"

    var resId : Int = 0
    var flag: String = ""
    var dailyRewardModel : DailyRewardModel? = null

    lateinit var onWebViewClick: () -> Unit

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_RESID, resId)
        outState.putString(KEY_FLAG, flag)
        outState.putParcelable(KEY_DYNAMIC, dailyRewardModel)
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogAdTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        theme
    ).apply {
        //landscape 모드에서  bottomsheet 메뉴모두  expand되지 않아서  아래 설정값 넣어줌.
        this.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        getDataFromVm()

        mGlideRequestManager = Glide.with(this)
        if (savedInstanceState != null) {
            resId = savedInstanceState.getInt(KEY_RESID)
            flag = savedInstanceState.getString(KEY_FLAG, "")
            dailyRewardModel =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) savedInstanceState.getParcelable(
                    KEY_DYNAMIC,
                    DailyRewardModel::class.java
                ) else {
                    savedInstanceState.getParcelable(KEY_DYNAMIC)
                }
        }
        val view = inflater.inflate(resId, container, false)

        when (resId) {
            FLAG_ADS -> setAds(view)
            else -> return super.onCreateView(inflater, container, savedInstanceState)
        }
        return view
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            super.show(manager, tag)
            this.isCancelable = false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setAds(v : View) {
        val activity = context as AttendanceActivity
        val btnClose = v.findViewById<AppCompatImageButton>(R.id.btn_close)
        val wvAdImg = v.findViewById<WebView>(R.id.wv_ad_img)

        btnClose.setOnClickListener {
            this.dismiss()
        }

        //가로세로 스크롤 바 제거
        wvAdImg.isVerticalScrollBarEnabled = false
        wvAdImg.isHorizontalScrollBarEnabled = false

        wvAdImg?.settings?.javaScriptEnabled = true
            dailyRewardModel.let {
                wvAdImg.loadDataWithBaseURL(null, dailyRewardModel?.banner?.content ?: return, "text/html", "UTF-8", null)
                wvAdImg.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        try{
                            val intent = Intent(Intent.ACTION_VIEW, request?.url)
                            startActivity(intent)
                            onWebViewClick()
                        }catch (e: Exception) {
                            e.printStackTrace()
                        }
                        this@AdsBottomSheetFragment.dismiss()
                        return true
                    }
                }

                wvAdImg.layoutParams.height = Util.convertDpToPixel(activity, dailyRewardModel?.banner?.height?.toFloat() ?: 0f).toInt()
                wvAdImg.layoutParams.width = Util.convertDpToPixel(activity, dailyRewardModel?.banner?.width?.toFloat() ?: 0f).toInt()
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDataFromVm() {
        attendanceViewModel.bottomSheetDismiss.observe(this, SingleEventObserver{ isAdClick ->   //올바른 url을 봤을 때 바텀시트 광고 닫아줌.
            if(isAdClick){
                val fragment = BaseFragment()
                dailyRewardModel?.let {
                    fragment.setUiActionFirebaseGoogleAnalyticsFragment(
                        GaAction.BOTTOM_SHEET_AD_CLICK.actionValue,
                        GaAction.BOTTOM_SHEET_AD_CLICK.label
                    )
                }
                dismiss()
            }
        })
    }
    companion object {
        val FLAG_ADS = R.layout.bottom_sheet_ad

        @JvmStatic
        fun newInstance(resId: Int, dailyRewardModel: DailyRewardModel, onWebViewClick: () -> Unit) : AdsBottomSheetFragment {
            val f = AdsBottomSheetFragment()
            f.resId = resId
            f.dailyRewardModel = dailyRewardModel
            f.onWebViewClick = onWebViewClick
            return f
        }
    }

}