package net.ib.mn.util

import net.ib.mn.BuildConfig

/**
 * 광고 관련 상수
 *
 * old 프로젝트(Exodus)의 Const.kt 광고 관련 상수와 동일
 */
object AdConstants {

    // ============================================================
    // AdManager (GAM) Unit IDs
    // ============================================================

    /** Idol 앱용 Adaptive Banner 광고 단위 ID */
    const val AD_MANAGER_ADAPTIVE_IDOL_AD_UNIT_ID: String = "/9176203,22915258703/1829623"

    /** Celeb 앱용 Adaptive Banner 광고 단위 ID */
    const val AD_MANAGER_ADAPTIVE_CELEB_AD_UNIT_ID: String = "/9176203,22915258703/1829625"

    // ============================================================
    // AdMob Unit IDs (테스트용)
    // ============================================================

    /** AdMob 네이티브 광고 테스트 단위 ID */
    const val ADMOB_NATIVE_AD_TEST_UNIT_ID: String = "ca-app-pub-3940256099942544/2247696110"

    /** AdMob 네이티브 광고 Idol 단위 ID */
    const val ADMOB_NATIVE_AD_IDOL_UNIT_ID: String = "ca-app-pub-4951070488234097/8779424757"

    /** AdMob 네이티브 광고 Celeb 단위 ID */
    const val ADMOB_NATIVE_AD_CELEB_UNIT_ID: String = "ca-app-pub-4951070488234097/4352165051"

    // ============================================================
    // Store Items (구독 상품)
    // ============================================================

    /** 데일리팩 상품 코드 - 광고 제거 기준 */
    val STORE_ITEM_DAILY_PACK: String =
        if (BuildConfig.CELEB) "daily_pack_actor_android" else "daily_pack_android"

    // ============================================================
    // Helper Functions
    // ============================================================

    /**
     * 광고 표시 여부 결정
     *
     * old 프로젝트의 BaseWidePhotoViewModel.updateShouldShowBanner() 로직과 동일
     * 디버그/릴리즈 모드에서 동일하게 동작
     *
     * @param isChinaBuild 중국 빌드 여부
     * @param hasDailyPackSubscription 데일리팩 구독 여부
     * @return 광고 표시 여부
     */
    fun shouldShowAd(
        isChinaBuild: Boolean = BuildConfig.CHINA,
        hasDailyPackSubscription: Boolean = false
    ): Boolean {
        return !isChinaBuild && !hasDailyPackSubscription
    }
}
