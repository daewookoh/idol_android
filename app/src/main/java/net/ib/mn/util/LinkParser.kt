package net.ib.mn.util

import java.net.URL
import java.util.regex.Pattern

/**
 * URL 링크 파싱 유틸리티
 * 텍스트에서 URL을 추출합니다.
 */
object LinkParser {

    private val URL_PATTERN = Pattern.compile(
        "(https?://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?)"
    )

    /**
     * 링크 데이터 모델
     */
    data class LinkData(
        val url: String,
        val title: String?,
        val description: String?,
        val imageUrl: String?,
        val host: String?
    )

    /**
     * 텍스트에서 첫 번째 URL 추출
     */
    fun extractFirstUrl(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    /**
     * 텍스트에서 모든 URL 추출
     */
    fun extractAllUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }

    /**
     * URL에서 기본 정보만 파싱 (OG 메타데이터 없이)
     * TODO: 서버에서 링크 프리뷰 API를 제공하면 해당 API 사용
     */
    fun parse(url: String): LinkData? {
        return try {
            val parsedUrl = URL(url)
            val host = parsedUrl.host

            LinkData(
                url = url,
                title = null,
                description = null,
                imageUrl = null,
                host = host
            )
        } catch (e: Exception) {
            logE("LinkParser", "Failed to parse URL: $url", e)
            null
        }
    }

    /**
     * URL이 유효한지 확인
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}
