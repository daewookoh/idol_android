package net.ib.mn.util.link

import android.content.Context
import android.net.Uri
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.ServerUrl
import java.net.URL

/**
 * 링크 관련 유틸 클래스
 */
object LinkUtil {

    /**
     * 앱 공유 링크 URL 생성
     */
    fun getAppLinkUrl(
        context: Context,
        params: List<String>,
        queries: List<Pair<String, String>>? = null
    ): String {
        return buildUrl(context, params, queries, includeLocaleInPath = false)
    }

    /**
     * WebView용 앱 링크 URL 생성 (언어 코드가 경로에 포함)
     */
    fun getAppLinkUrlForWebView(
        context: Context,
        params: List<String>,
        queries: List<Pair<String, String>>? = null
    ): String {
        return buildUrl(context, params, queries, includeLocaleInPath = true)
    }

    private fun buildUrl(
        context: Context,
        params: List<String>,
        queries: List<Pair<String, String>>?,
        includeLocaleInPath: Boolean
    ): String {
        val sharedUrl = if (ServerUrl.isTestServer()) ServerUrl.HOST_TEST else ServerUrl.HOST
        val url = URL(sharedUrl)
        val locale = LocaleUtil.getShareLocale(context)

        return Uri.Builder()
            .scheme(url.protocol)
            .authority(url.host)
            .apply {
                params.forEach { appendPath(it) }

                val pathSuffix = if (includeLocaleInPath) "/$locale/" else "/"
                encodedPath(build().encodedPath + pathSuffix)

                queries?.forEach { (key, value) ->
                    appendQueryParameter(key, value)
                }

                if (!includeLocaleInPath) {
                    appendQueryParameter("locale", locale)
                }
            }
            .build()
            .toString()
    }
}
