package net.ib.mn.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.utils.Logger.Companion.v

//import com.vungle.mediation.VungleAdapter;
//import com.vungle.mediation.VungleExtrasBuilder;
/**
 * Created by parkboo on 2017. 6. 5..
 */
class VideoAdManager(private var listener: OnAdManagerListener?) {
    private var mAd: RewardedAd? = null
    private var adRequest: AdRequest? = null
    var isAdReady = false
        private set

    interface OnAdManagerListener {
        /**
         * 광고 준비중
         */
        fun onAdPreparing()

        /**
         * 광고 준비됨
         */
        fun onAdReady()

        /**
         * 광고 시청 후 적립완료
         */
        fun onAdRewared()

        /**
         * 광고 없음
         */
        fun onAdFailedToLoad()

        /**
         * 광고 끔
         */
        fun onAdClosed()
    }

    fun initialize(context: Context) {

        // adcolony
        // admob initialize 해주기 전에 해줘야 됨
        // https://developers.google.com/admob/android/mediation/adcolony

        // admob  initialize 함.
        MobileAds.initialize(context) { initializationStatus: InitializationStatus? ->
            MobileAds.setAppMuted(true) //비디오 광고 소리 제거
            v("admob  initialize 됨 ")
        }
    }

    fun setListener(listener: OnAdManagerListener?) {
        this.listener = listener
    }

    fun requestAd(context: Context, unitId: String) {
        isAdReady = false
        if (listener != null) {
            listener!!.onAdPreparing()
        }
        adRequest =
            AdRequest.Builder() //              .addNetworkExtrasBundle(VungleAdapter.class, extrasVungle)
                .build()
        val adId = if(BuildConfig.DEBUG) Const.ADMOB_REWARDED_VIDEO_TEST_UNIT_ID
            else unitId
        RewardedAd.load(
            context,
            adId,
            adRequest!!,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mAd = null
                    v("onAdFailedToLoad => error msg  => " + loadAdError.message)
                    if (listener != null) {
                        listener!!.onAdFailedToLoad()
                    }
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    isAdReady = true
                    mAd = rewardedAd
                    v("request load $mAd adId=$adId")
                    if (listener != null) {
                        listener!!.onAdReady()
                    }
                }
            })
    }

    fun showAd(context: Context) {
        if (mAd != null) {
            mAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    v("onAdShowedFullScreenContent -> 실행됨 ")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    isAdReady = false
                    mAd = null
                    if (listener != null) {
                        listener!!.onAdFailedToLoad()
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    isAdReady = false
                    v("onAdDismissedFullScreenContent -> 실행됨 ")
                    if (listener != null) {
                        listener!!.onAdClosed()
                    }
                    mAd = null
                }
            }
            mAd!!.show((context as Activity)) { rewardItem: RewardItem ->
                isAdReady = false
                Util.log("onRewarded $rewardItem")
                if (listener != null) {
                    listener!!.onAdRewared()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @get:Synchronized
        var instance: VideoAdManager? = null
            private set

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context, listener: OnAdManagerListener?): VideoAdManager {
            if (instance == null) {
                instance = VideoAdManager(listener)
                instance!!.initialize(context)
            }
            instance!!.listener = listener
            return instance!!
        }
    }
}
