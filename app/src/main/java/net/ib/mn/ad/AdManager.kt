package net.ib.mn.ad

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import net.ib.mn.BuildConfig
import net.ib.mn.util.AdConstants
import java.lang.ref.WeakReference

/**
 * AdManager - 광고 관리 싱글톤
 *
 * old 프로젝트(Exodus)의 AdManager.kt와 동일한 방식으로 구현
 * 디버그/릴리즈 모드에서 동일하게 동작 (old 프로젝트와 동일)
 */
class AdManager private constructor(private val appContext: Context) {

    private var adViewRef: WeakReference<AdManagerAdView>? = null
    private var adSize: AdSize? = null

    /**
     * 광고 SDK 초기화
     * @param debug 디버그 모드 여부
     * @param testDeviceIds 테스트 디바이스 ID 목록
     */
    fun init(debug: Boolean = BuildConfig.DEBUG, testDeviceIds: List<String> = emptyList()) {
        if (debug) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
            )
        }
    }

    /**
     * 배너 광고를 컨테이너에 부착하고 로드
     *
     * @param activity Activity 인스턴스
     * @param container 광고를 표시할 ViewGroup
     * @param onFailed 광고 로드 실패 콜백
     * @param onLoaded 광고 로드 성공 콜백
     * @return BannerHandle 광고 해제용 핸들
     */
    fun attachAndLoadBanner(
        activity: Activity,
        container: ViewGroup,
        onFailed: (LoadAdError) -> Unit = {},
        onLoaded: () -> Unit = {}
    ): BannerHandle {
        // 기존 뷰가 남아 있다면 완전 해제
        detachBanner()

        val adView = AdManagerAdView(activity).apply {
            adUnitId = if (BuildConfig.CELEB) {
                AdConstants.AD_MANAGER_ADAPTIVE_CELEB_AD_UNIT_ID
            } else {
                AdConstants.AD_MANAGER_ADAPTIVE_IDOL_AD_UNIT_ID
            }
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailed(error)
                }
                override fun onAdLoaded() {
                    onLoaded()
                }
            }
        }

        // 크기 계산: container가 레이아웃 된 뒤 폭 기준으로 Adaptive 크기 산정
        computeAdaptiveSizeAfterLaidOut(activity, container) { size ->
            adSize = size
            adView.setAdSizes(size, AdSize.BANNER)
            container.addView(adView)
            adView.loadAd(AdManagerAdRequest.Builder().build())
        }

        adViewRef = WeakReference(adView)

        return BannerHandle(WeakReference(adView), WeakReference(container))
    }

    /**
     * 배너 광고 해제
     */
    fun detachBanner() {
        adViewRef?.get()?.let { view ->
            try {
                view.adListener = object : AdListener() {}
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
            } catch (_: Throwable) {
            }
        }
        adViewRef = null
    }

    /**
     * 화면 회전/인셋 변화 등으로 컨테이너 폭이 바뀔 때 호출 (선택 사항)
     */
    fun updateBannerSizeIfNeeded(activity: Activity, container: ViewGroup) {
        val view = adViewRef?.get() ?: return
        computeAdaptiveSizeAfterLaidOut(activity, container) { size ->
            if (size != null) {
                adSize = size
                view.setAdSizes(size, AdSize.BANNER)
            }
        }
    }

    /**
     * 컨테이너 폭 기준 Adaptive Banner 크기 계산
     */
    private fun computeAdaptiveSizeAfterLaidOut(
        activity: Activity,
        container: ViewGroup,
        onSize: (AdSize?) -> Unit
    ) {
        if (container.width > 0) {
            val size = adaptiveSize(activity, container.width)
            onSize(size)
        } else {
            container.post {
                val size = adaptiveSize(activity, container.width)
                onSize(size)
            }
        }
    }

    private fun adaptiveSize(activity: Activity, pxWidth: Int): AdSize? {
        if (pxWidth <= 0) return null
        val density = activity.resources.displayMetrics.density
        val adWidth = (pxWidth / density).toInt().coerceAtLeast(1)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    companion object {
        @Volatile private var instance: AdManager? = null

        fun getInstance(context: Context): AdManager =
            instance ?: synchronized(this) {
                instance ?: AdManager(context.applicationContext).also { instance = it }
            }
    }
}

/**
 * 배너 광고 핸들 - 광고 해제용
 */
class BannerHandle internal constructor(
    private val adViewRef: WeakReference<AdManagerAdView>,
    private val containerRef: WeakReference<ViewGroup>
) {
    fun detach() {
        adViewRef.get()?.let { adView ->
            try {
                adView.adListener = object : AdListener() {}
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView.destroy()
            } catch (_: Throwable) { }
        }
        containerRef.get()?.removeAllViews()
    }
}
