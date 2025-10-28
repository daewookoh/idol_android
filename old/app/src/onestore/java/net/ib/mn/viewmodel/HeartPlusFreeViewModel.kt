/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: HeartPlusFreeActivity(무료충전소)의 ViewModel
 *
 * */

package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tapjoy.TJActionRequest
import com.tapjoy.TJError
import com.tapjoy.TJPlacement
import com.tapjoy.TJPlacementListener
import com.tapjoy.Tapjoy
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.TapjoyVariable
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import org.json.JSONArray

class HeartPlusFreeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _errorPopup = MutableLiveData<Event<String>>()
    val errorPopup: LiveData<Event<String>> = _errorPopup

    private val _showOfferWallTabs = MutableLiveData<JSONArray>()
    val showOfferWallTabs: LiveData<JSONArray> = _showOfferWallTabs

    private val _fragmentOfferWallClick = MutableLiveData<Int>()
    val fragmentOfferWallClick: LiveData<Int> = _fragmentOfferWallClick

    private val _isRequestPermission = MutableLiveData<Boolean>()
    val isRequestPermission: LiveData<Boolean> = _isRequestPermission

    private var heartPlusIndex: Int = FRAGMENT_NAS_WALL

    private var showRequestPermission: Boolean = false

    init {

        // 시스템에서 데이터 날아가는경우를 대비해서 값을 가져온다.
        getSaveState()
    }

    // 보여줄 탭 서버에서 보내준 값과, naswall까지 더해서 setting
    fun setOfferWallTabs(context: Context) {
        val showOfferWallTabs = JSONArray()
        showOfferWallTabs.put("naswall")

        ConfigModel.getInstance(context).showOfferwallTabs?.let {
            for (i in 0 until it.length()) {
                showOfferWallTabs.put(it[i])
            }
        }

        _showOfferWallTabs.value = showOfferWallTabs
    }

    fun isRequestPermission() {
        _isRequestPermission.value = showRequestPermission

        savedStateHandle["showRequestPermission"] = true
    }

    fun fragmentPosition() {
        _fragmentOfferWallClick.value = heartPlusIndex
    }

    fun fragmentItemClick(activity: HeartPlusFreeActivity, offerwall: String) {
        when (offerwall) {
            "naswall" -> {
                activity.setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "heartshop_naswall",
                )
                savedStateHandle["heartPlusIndex"] = FRAGMENT_NAS_WALL
                _fragmentOfferWallClick.value = FRAGMENT_NAS_WALL
            }
            "appdriver" -> {
                activity.setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "heartshop_app_driver",
                )
                savedStateHandle["heartPlusIndex"] = FRAGMENT_APP_DRIVER
                _fragmentOfferWallClick.value = FRAGMENT_APP_DRIVER
            }
            "metabs" -> {
                activity.setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "heartshop_metabs",
                )
                savedStateHandle["heartPlusIndex"] = FRAGMENT_METABS
                _fragmentOfferWallClick.value = FRAGMENT_METABS
            }
            "tapjoy" -> {
                activity.setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "heartshop_tapjoy",
                )
                UtilK.setTapJoy(activity) { isConnected ->

                    if (isConnected) {
                        showTapjoy(activity)
                        return@setTapJoy
                    }

                    _errorPopup.postValue(Event("TapJoy SDK connection failed"))
                }
            }
        }
    }

    private fun showTapjoy(activity: HeartPlusFreeActivity) {
        Util.showProgress(activity)
        Tapjoy.setUserID(IdolAccount.getAccount(activity)?.email, null)
        val offerWallPlacement = Tapjoy.getPlacement(
            if (BuildConfig.CELEB) TapjoyVariable.CELEB_PLACEMENT_NAME.value else TapjoyVariable.IDOL_PLACEMENT_NAME.value,
            object : TJPlacementListener {
                override fun onRequestSuccess(placement: TJPlacement) {}
                override fun onRequestFailure(placement: TJPlacement, error: TJError) {
                    try {
                        Util.closeProgress()
                        _errorPopup.postValue(
                            Event(
                                activity.getString(R.string.error_nextapps_error1)
                                    .plus(error.message),
                            ),
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onContentReady(placement: TJPlacement) {
                    Util.closeProgress()
                    if (placement.isContentReady) {
                        placement.showContent()
                    }
                }

                override fun onContentShow(placement: TJPlacement) {}
                override fun onContentDismiss(placement: TJPlacement) {}
                override fun onPurchaseRequest(
                    placement: TJPlacement,
                    request: TJActionRequest,
                    productId: String,
                ) {
                    Util.log("Tapjoy onPurchaseRequest:$productId")
                }

                override fun onRewardRequest(
                    placement: TJPlacement,
                    request: TJActionRequest,
                    itemId: String,
                    quantity: Int,
                ) {
                    Util.log("Tapjoy onPurchaseRequest: itemId=$itemId quantity=$quantity")
                }

                override fun onClick(tjPlacement: TJPlacement) {}
            },
        )
        offerWallPlacement.requestContent()
    }

    private fun getSaveState() {
        savedStateHandle.apply {
            get<Int>("heartPlusIndex")?.run {
                heartPlusIndex = this
            }
            get<Boolean>("showRequestPermission")?.run {
                showRequestPermission = this
            }
        }
    }

    companion object {
        private const val FRAGMENT_NAS_WALL = 1
        private const val FRAGMENT_METABS = 2
        private const val FRAGMENT_APP_DRIVER = 3
    }
}