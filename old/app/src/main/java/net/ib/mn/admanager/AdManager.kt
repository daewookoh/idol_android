package net.ib.mn.admanager

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import net.ib.mn.BuildConfig
import net.ib.mn.utils.Const
import net.ib.mn.utils.IdolSnackBar

class AdManager {

    var adManagerView: AdManagerAdView? = null
    private lateinit var adSize: AdSize

    fun setAdManager(context: Context) {
        // 테스트 해보고싶으면 listOf에 자기자신 Device id 를 넣는다.
        // Logcat에다가 setTestDeviceIds 검색하면 나옵니다.
        if (BuildConfig.DEBUG) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf()).build()
            )
        }

        adManagerView = AdManagerAdView(context).apply {
            adListener = object : AdListener() {

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    if (BuildConfig.DEBUG) {
                        IdolSnackBar.make(
                            findViewById(android.R.id.content),
                            "${p0.message} : ${p0.code}"
                        ).show()
                    }
                }

                override fun onAdLoaded() {
                }
            }
        }

        adManagerView?.adUnitId = if (BuildConfig.CELEB) {
            Const.AD_MANAGER_ADAPTIVE_CELEB_AD_UNIT_ID
        } else {
            Const.AD_MANAGER_ADAPTIVE_IDOL_AD_UNIT_ID
        }

        if (this::adSize.isInitialized) {
            adManagerView?.setAdSizes(
                adSize,
                AdSize.BANNER
            )
        }
    }

    fun loadAdManager() {
        val adRequest = AdManagerAdRequest
            .Builder()
            .build()
        adManagerView?.loadAd(adRequest)
    }

    fun setAdManagerSize(activity: Activity, view: View) {
        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = activity.windowManager?.currentWindowMetrics
            metrics?.bounds?.width()?.toFloat()
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            displayMetrics.widthPixels.toFloat()
        }

        var adWidthPixels = view.width.toFloat()

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0f) {
            adWidthPixels = screenWidth ?: 0f
        }

        val density = activity.resources.displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()

        adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    companion object {
        private var instance: AdManager? = null

        fun getInstance(): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager().also {
                    instance = it
                }
            }
        }

    }
}