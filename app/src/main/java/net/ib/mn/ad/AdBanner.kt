package net.ib.mn.ad

import android.app.Activity
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import net.ib.mn.BuildConfig

/**
 * Compose용 Adaptive Banner 광고 컴포넌트
 *
 * old 프로젝트(Exodus)의 WidePhotoFragment 광고 로직과 동일하게 동작
 * - 디버그 모드에서도 동일하게 광고 표시
 * - 광고 실패 시 디버그 모드에서만 Toast 표시
 *
 * @param modifier Modifier
 * @param onAdLoaded 광고 로드 성공 콜백
 * @param onAdFailed 광고 로드 실패 콜백
 */
@Composable
fun AdaptiveBanner(
    modifier: Modifier = Modifier,
    onAdLoaded: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // BannerHandle을 remember로 유지
    val bannerHandleHolder = remember { BannerHandleHolder() }

    DisposableEffect(Unit) {
        onDispose {
            bannerHandleHolder.handle?.detach()
            bannerHandleHolder.handle = null
        }
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { container ->
            // 이미 로드된 광고가 있으면 재로드하지 않음
            if (bannerHandleHolder.handle != null) return@AndroidView

            bannerHandleHolder.handle = AdManager.getInstance(context)
                .attachAndLoadBanner(
                    activity = activity,
                    container = container,
                    onFailed = { error ->
                        // old 프로젝트와 동일: 디버그 모드에서만 Toast 표시
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(
                                context,
                                "Ad failed: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onAdFailed(error.message)
                    },
                    onLoaded = {
                        onAdLoaded()
                    }
                )
        },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    )
}

/**
 * BannerHandle을 유지하기 위한 홀더 클래스
 */
private class BannerHandleHolder {
    var handle: BannerHandle? = null
}
