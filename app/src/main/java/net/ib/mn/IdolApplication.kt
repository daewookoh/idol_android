package net.ib.mn

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.facebook.FacebookSdk
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.util.Constants
import net.ib.mn.util.ServerUrl
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Application class for Hilt initialization and SNS SDK setup.
 *
 * SNS SDK 초기화 (old 프로젝트의 IdolApplication.kt와 동일):
 * 1. Kakao SDK: KakaoSdk.init()
 * 2. Facebook SDK: FacebookSdk.sdkInitialize()
 * 3. Line SDK: 별도 초기화 불필요 (사용 시 자동 초기화)
 * 4. Google Sign-In: 별도 초기화 불필요
 */
@HiltAndroidApp
class IdolApplication : Application() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()

        // 저장된 서버 URL 로드 (old 프로젝트 방식)
        initializeServerUrl()

        // Kakao SDK 초기화 (old 프로젝트와 동일)
        // China flavor에서는 Kakao를 사용하지 않으므로 제외
        if (!Constants.IS_CHINA) {
            KakaoSdk.init(this, Constants.KAKAO_APP_KEY)

            // DEBUG 빌드에서만 Key Hash 로깅 (개발/테스트용)
            if (BuildConfig.DEBUG) {
                printKeyHash()
            }
        }

        // Facebook SDK 초기화
        FacebookSdk.sdkInitialize(applicationContext)

        // Line SDK: 별도 초기화 불필요 (사용 시 자동 초기화)
        // Google Sign-In: 별도 초기화 불필요
    }

    /**
     * 저장된 서버 URL을 로드하여 ServerUrl.HOST를 설정합니다.
     * old 프로젝트의 BaseApplication.onCreate()와 동일한 로직
     */
    private fun initializeServerUrl() {
        runBlocking {
            val savedUrl = preferencesManager.serverUrl.first()
            if (!savedUrl.isNullOrEmpty()) {
                ServerUrl.setHost(savedUrl)
                Log.d("ServerUrl", "Loaded saved server URL: $savedUrl")
            } else {
                Log.d("ServerUrl", "Using default server URL: ${ServerUrl.HOST}")
            }
        }
    }

    /**
     * 현재 앱의 Key Hash를 로그로 출력합니다 (DEBUG 빌드만).
     *
     * Kakao Developers Console에 등록해야 할 실제 Key Hash를 확인하기 위한 디버그용 함수입니다.
     * Logcat에서 "KAKAO_KEY_HASH" 태그로 필터링하여 확인하세요.
     */
    @Suppress("DEPRECATION")
    private fun printKeyHash() {
        try {

            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)

                Log.e("KAKAO_KEY_HASH", "========================================")
                Log.e("KAKAO_KEY_HASH", "Package Name: $packageName")
                Log.e("KAKAO_KEY_HASH", "Key Hash: $keyHash")
                Log.e("KAKAO_KEY_HASH", "========================================")
                Log.e("KAKAO_KEY_HASH", "Constants Kakao App Key: ${Constants.KAKAO_APP_KEY}")
                Log.e("KAKAO_KEY_HASH", "========================================")
                Log.e("KAKAO_KEY_HASH", "Copy this Key Hash to Kakao Developers Console:")
                Log.e("KAKAO_KEY_HASH", "https://developers.kakao.com/")
                Log.e("KAKAO_KEY_HASH", "Package: $packageName")
                Log.e("KAKAO_KEY_HASH", "Key Hash: $keyHash")
                Log.e("KAKAO_KEY_HASH", "========================================")
            }
        } catch (e: Exception) {
            Log.e("KAKAO_KEY_HASH", "Error getting key hash: ${e.message}", e)
        }
    }
}
