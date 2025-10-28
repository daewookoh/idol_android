package net.ib.mn.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const.ADMOB_REWARDED_VIDEO_LEVELREWARD_UNIT_ID

class RewardVideoManager private constructor() {
    var rewardedAd: RewardedAd? = null
    private var isLoading = false
    var rewardAmount = 0

    fun loadRewardAd(context: Context, userId: Int?, listener: OnAdLoadListener?) {
        if (isLoading) return
        isLoading = true

        val uid = userId ?: run { isLoading = false; return }
        val adId = if (BuildConfig.DEBUG) {
            Const.ADMOB_REWARDED_VIDEO_TEST_UNIT_ID
        } else {
            ADMOB_REWARDED_VIDEO_LEVELREWARD_UNIT_ID
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                    rewardedAd?.setImmersiveMode(true)
                    rewardAmount = ConfigModel.getInstance(context).video_heart

                    val serverSideVerificationOptions = ServerSideVerificationOptions.Builder()
                        .setUserId(uid.toString())
                        .build()
                    rewardedAd?.setServerSideVerificationOptions(serverSideVerificationOptions)

                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            if (adResult !is AdResult.Rewarded) {
                                adResult = AdResult.Closed
                            }
                            rewardedAd = null
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            adResult = AdResult.Error(adError.message)
                            rewardedAd = null
                        }
                    }
                    listener?.onAdLoadSuccess()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    listener?.onAdLoadFailed(loadAdError)
                }
            }
        )
    }

    fun show(activity: Activity) {
        val ad = rewardedAd ?: run {
            adResult = AdResult.Error("Ad not ready")
            return
        }
        adResult = null // Reset before showing
        ad.show(activity) { reward ->
            adResult = AdResult.Rewarded(reward.amount)
        }
    }

    interface OnAdLoadListener {
        fun onAdLoadFailed(loadAdError: LoadAdError)
        fun onAdLoadSuccess()
    }

    companion object {
        sealed class AdResult {
            object Closed : AdResult()
            data class Rewarded(val amount: Int) : AdResult()
            data class Error(val message: String) : AdResult()
        }

        var adResult: AdResult? = null

        private var instance: RewardVideoManager? = null
        @JvmStatic
        fun getInstance(): RewardVideoManager {
            if (instance == null) instance = RewardVideoManager()
            return instance!!
        }
    }
}
