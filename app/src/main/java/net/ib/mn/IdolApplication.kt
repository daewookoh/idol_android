package net.ib.mn

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
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
class IdolApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    /**
     * Coil ImageLoader 설정 (메모리 최적화)
     *
     * 최적화 전략:
     * 1. 메모리 캐시 크기 제한 (앱 메모리의 25%)
     * 2. 디스크 캐시 설정 (100MB)
     * 3. GIF/Video 디코더 지원
     * 4. 자동 리사이징 활성화
     * 5. Bitmap pooling 활성화
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // 메모리 캐시 설정 (앱 메모리의 25%)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)  // 메모리의 25%만 사용
                    .build()
            }
            // 디스크 캐시 설정 (100MB)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024)  // 100MB
                    .build()
            }
            // GIF 디코더 (Android 9 이상은 ImageDecoder, 이하는 GifDecoder)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // Video frame 디코더
                add(VideoFrameDecoder.Factory())
            }
            // 캐시 정책: 메모리 & 디스크 모두 사용
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // 크로스페이드 애니메이션 활성화
            .crossfade(true)
            .crossfade(200)
            // DEBUG 빌드에서만 로깅
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

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

        // DEBUG 빌드에서만 페이스북 Key Hash 로깅 (개발/테스트용)
        if (BuildConfig.DEBUG) {
            printFacebookKeyHash()
        }

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

    /**
     * 페이스북 Key Hash를 로그로 출력합니다 (DEBUG 빌드만).
     *
     * Facebook Developer Console에 등록해야 할 실제 Key Hash를 확인하기 위한 디버그용 함수입니다.
     * Logcat에서 "FACEBOOK_KEY_HASH" 태그로 필터링하여 확인하세요.
     * 
     * 등록 방법:
     * 1. 앱 실행 후 Logcat에서 "FACEBOOK_KEY_HASH" 태그로 검색
     * 2. 출력된 Key Hash를 복사
     * 3. Facebook Developer Console → Settings → Basic → Key Hashes에 추가
     */
    @Suppress("DEPRECATION")
    private fun printFacebookKeyHash() {
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

                Log.e("FACEBOOK_KEY_HASH", "========================================")
                Log.e("FACEBOOK_KEY_HASH", "Facebook Key Hash:")
                Log.e("FACEBOOK_KEY_HASH", "Package Name: $packageName")
                Log.e("FACEBOOK_KEY_HASH", "Key Hash: $keyHash")
                Log.e("FACEBOOK_KEY_HASH", "========================================")
                Log.e("FACEBOOK_KEY_HASH", "Copy this Key Hash to Facebook Developer Console:")
                Log.e("FACEBOOK_KEY_HASH", "https://developers.facebook.com/apps/")
                Log.e("FACEBOOK_KEY_HASH", "Settings → Basic → Key Hashes")
                Log.e("FACEBOOK_KEY_HASH", "Package: $packageName")
                Log.e("FACEBOOK_KEY_HASH", "Key Hash: $keyHash")
                Log.e("FACEBOOK_KEY_HASH", "========================================")
            }
        } catch (e: Exception) {
            Log.e("FACEBOOK_KEY_HASH", "Error getting Facebook key hash: ${e.message}", e)
        }
    }
}
