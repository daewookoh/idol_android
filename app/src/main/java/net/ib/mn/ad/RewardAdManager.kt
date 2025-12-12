package net.ib.mn.ad

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import net.ib.mn.BuildConfig
import net.ib.mn.util.AdConstants
import net.ib.mn.util.logD
import net.ib.mn.util.logE

/**
 * 리워드 광고 관리자
 *
 * old 프로젝트: VideoAdManager.kt, MezzoPlayerActivity.kt
 *
 * 사용 방법:
 * 1. loadAd(context, unitId) - 광고 로드
 * 2. showAd(activity, onRewarded, onFailed) - 광고 표시
 */
class RewardAdManager {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /**
     * 광고 로드
     *
     * @param context Context
     * @param unitId 광고 단위 ID (null이면 테마픽용 기본값 사용)
     * @param onLoaded 로드 완료 콜백
     * @param onFailed 로드 실패 콜백
     */
    fun loadAd(
        context: Context,
        unitId: String? = null,
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        if (isLoading) {
            logD(TAG, "Ad is already loading")
            return
        }

        if (rewardedAd != null) {
            logD(TAG, "Ad is already loaded")
            onLoaded()
            return
        }

        isLoading = true

        // 디버그 모드에서는 테스트 광고 사용
        val adUnitId = if (BuildConfig.DEBUG) {
            AdConstants.ADMOB_REWARDED_VIDEO_TEST_UNIT_ID
        } else {
            unitId ?: AdConstants.ADMOB_REWARDED_VIDEO_ONEPICK_UNIT_ID
        }

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    logD(TAG, "Ad loaded successfully")
                    rewardedAd = ad
                    isLoading = false
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logE(TAG, "Ad failed to load: ${error.message}")
                    rewardedAd = null
                    isLoading = false
                    onFailed(error.message)
                }
            }
        )
    }

    /**
     * 광고 표시
     *
     * @param activity Activity
     * @param onRewarded 보상 지급 콜백 (광고 시청 완료)
     * @param onFailed 실패 콜백
     * @param onDismissed 광고 닫힘 콜백
     */
    fun showAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onFailed: (String) -> Unit = {},
        onDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            logE(TAG, "Ad is not ready")
            onFailed("Ad is not ready")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                logD(TAG, "Ad showed")
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logE(TAG, "Ad failed to show: ${error.message}")
                rewardedAd = null
                onFailed(error.message)
            }

            override fun onAdDismissedFullScreenContent() {
                logD(TAG, "Ad dismissed")
                rewardedAd = null
                onDismissed()
            }
        }

        ad.show(activity) { rewardItem ->
            logD(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewarded()
        }
    }

    /**
     * 광고 준비 여부
     */
    fun isAdReady(): Boolean = rewardedAd != null

    /**
     * 리소스 해제
     */
    fun release() {
        rewardedAd = null
        isLoading = false
    }

    companion object {
        private const val TAG = "RewardAdManager"

        @Volatile
        private var instance: RewardAdManager? = null

        fun getInstance(): RewardAdManager {
            return instance ?: synchronized(this) {
                instance ?: RewardAdManager().also { instance = it }
            }
        }
    }
}
