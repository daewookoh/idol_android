package net.ib.mn.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import net.ib.mn.R

/**
 * Intent 유틸리티
 * 외부 앱 호출 관련 Intent를 통합 관리
 */
object IntentUtil {

    /**
     * URL을 외부 브라우저로 열기
     * @param context Context
     * @param url 열 URL (http/https 스킴이 없으면 자동으로 https 추가)
     */
    @JvmStatic
    fun openUrl(context: Context, url: String) {
        val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtil.show(context, R.string.msg_error_ok)
        } catch (e: Exception) {
            ToastUtil.show(context, R.string.msg_error_ok)
        }
    }

    /**
     * 지도 앱으로 위치 열기
     * @param context Context
     * @param location 위치 검색어 (주소 또는 장소명)
     * @param useGoogleMaps true면 Google Maps 앱을 직접 호출, false면 기본 지도 앱 사용
     */
    @JvmStatic
    fun openMap(context: Context, location: String, useGoogleMaps: Boolean = true) {
        try {
            val encodedLocation = Uri.encode(location)
            val geoUri = Uri.parse("geo:0,0?q=$encodedLocation")
            val intent = Intent(Intent.ACTION_VIEW, geoUri)

            if (useGoogleMaps) {
                intent.setClassName(
                    "com.google.android.apps.maps",
                    "com.google.android.maps.MapsActivity"
                )
            }

            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Google Maps가 없으면 기본 지도 앱으로 시도
            if (useGoogleMaps) {
                openMap(context, location, useGoogleMaps = false)
            } else {
                ToastUtil.show(context, R.string.msg_error_ok)
            }
        } catch (e: Exception) {
            ToastUtil.show(context, R.string.msg_error_ok)
        }
    }

    /**
     * 텍스트 공유
     * @param context Context
     * @param text 공유할 텍스트
     * @param chooserTitle 공유 다이얼로그 제목 (null이면 기본값)
     */
    @JvmStatic
    fun shareText(context: Context, text: String, chooserTitle: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val title = chooserTitle ?: context.getString(R.string.title_share)
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            ToastUtil.show(context, R.string.msg_error_ok)
        }
    }

    /**
     * 텍스트 공유 (메시지 + URL 형식)
     * @param context Context
     * @param message 공유 메시지
     * @param url 공유할 URL
     * @param chooserTitle 공유 다이얼로그 제목 (null이면 기본값)
     */
    @JvmStatic
    fun shareTextWithUrl(
        context: Context,
        message: String,
        url: String,
        chooserTitle: String? = null
    ) {
        val text = if (message.isNotEmpty()) {
            "$message\n$url"
        } else {
            url
        }
        shareText(context, text, chooserTitle)
    }

    /**
     * Intent를 안전하게 실행
     * ActivityNotFoundException 발생 시 에러 메시지 표시
     * @param context Context
     * @param intent 실행할 Intent
     * @param errorMessageResId 에러 발생 시 표시할 메시지 리소스 ID
     */
    @JvmStatic
    fun startActivitySafely(
        context: Context,
        intent: Intent,
        errorMessageResId: Int = R.string.msg_error_ok
    ) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtil.show(context, errorMessageResId)
        } catch (e: Exception) {
            ToastUtil.show(context, errorMessageResId)
        }
    }
}
