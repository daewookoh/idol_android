package net.ib.mn.util

import android.content.Context
import android.net.Uri
import android.os.LocaleList
import net.ib.mn.data.local.entity.IdolEntity
import java.util.Locale

/**
 * Idol 이미지 관련 유틸리티
 *
 * Old 프로젝트의 UtilK.getTop3ImageUrl() 로직 이식
 */
object IdolImageUtil {

    /**
     * Top3 이미지 URL 리스트 생성
     *
     * Old 프로젝트 로직:
     * - top3ImageVer가 있으면 버전 파라미터로 cache busting
     * - top3Ids가 비어있으면 기본 이미지 (imageUrl, imageUrl2, imageUrl3) 사용
     *
     * @param idol IdolEntity
     * @return 3개 이미지 URL 리스트
     */
    fun getTop3ImageUrls(idol: IdolEntity): List<String?> {
        val top3Ids = idol.top3?.split(",")?.map { it.trim() } ?: emptyList()
        val top3ImageVer = idol.top3ImageVer ?: ""
        val urls = listOf(idol.imageUrl, idol.imageUrl2, idol.imageUrl3)

        android.util.Log.d("IdolImageUtil", "========================================")
        android.util.Log.d("IdolImageUtil", "getTop3ImageUrls for idol: ${idol.name} (ID: ${idol.id})")
        android.util.Log.d("IdolImageUtil", "  - imageUrl: ${idol.imageUrl}")
        android.util.Log.d("IdolImageUtil", "  - imageUrl2: ${idol.imageUrl2}")
        android.util.Log.d("IdolImageUtil", "  - imageUrl3: ${idol.imageUrl3}")
        android.util.Log.d("IdolImageUtil", "  - top3: ${idol.top3}")
        android.util.Log.d("IdolImageUtil", "  - top3ImageVer: ${idol.top3ImageVer}")
        android.util.Log.d("IdolImageUtil", "========================================")

        // top3ImageVer가 비어있거나 top3Ids가 모두 null/empty인 경우
        if (top3ImageVer.isEmpty() || top3Ids.all { it.isEmpty() || it == "0" }) {
            android.util.Log.d("IdolImageUtil", "→ Using default URLs (no top3 data)")
            return urls
        }

        return top3Ids.take(3).mapIndexed { index, id ->
            if (id.isNotEmpty() && id != "0") {
                val ver = top3ImageVer.split(",").getOrNull(index)?.trim() ?: ""
                val originalUrl = urls.getOrNull(index)
                originalUrl?.updateQueryParameter("ver", ver)
            } else {
                urls.getOrNull(index)
            }
        }.let { list ->
            // 3개 미만이면 나머지 기본 이미지로 채우기
            list + urls.drop(list.size)
        }.take(3)
    }

    /**
     * Top3 동영상 URL 리스트 생성
     *
     * 움짤이 있는 경우:
     * - .mp4 파일이면 그대로 사용
     * - _s_mv.jpg 파일이면 _m_mv.mp4로 변환
     * - 그 외에는 null 반환
     *
     * @param idol IdolEntity
     * @return 3개 동영상 URL 리스트 (없으면 null)
     */
    fun getTop3VideoUrls(idol: IdolEntity): List<String?> {
        val imageUrls = getTop3ImageUrls(idol)

        return imageUrls.map { url ->
            when {
                url == null -> null
                url.contains(".mp4") -> url
                url.contains("_s_mv.jpg") -> url.replace("_s_mv.jpg", "_m_mv.mp4")
                else -> null
            }
        }
    }

    /**
     * HeartPick 배너 이미지 URL에 언어별 분기 적용
     *
     * 지원 언어: 한국어(ko), 영어(en), 일본어(ja), 중국어 간체(zh-CN), 중국어 번체(zh-TW), 스페인어(es)
     *
     * URL 변환 예시:
     * - 한국어/영어: https://example.com/banner.png (그대로)
     * - 일본어: https://example.com/banner_ja.png
     * - 중국어 간체: https://example.com/banner_zhcn.png
     * - 중국어 번체: https://example.com/banner_zhtw.png
     * - 스페인어: https://example.com/banner_es.png
     *
     * @param context Context
     * @param bannerUrl 원본 배너 URL
     * @return 언어별 배너 URL (지원하지 않는 언어는 영어로 fallback)
     */
    fun getLocalizedBannerUrl(context: Context, bannerUrl: String?): String {
        if (bannerUrl.isNullOrEmpty()) return ""

        val locale = getSystemLocale()
        val language = locale.language
        val country = locale.country

        // 언어 코드 결정: 한국어/영어는 suffix 없음, 나머지는 suffix 추가
        val languageSuffix = when {
            language == "ko" -> ""  // 한국어: suffix 없음
            language == "ja" -> "_ja"  // 일본어
            language == "zh" && country == "CN" -> "_zhcn"  // 중국어 간체
            language == "zh" && country == "TW" -> "_zhtw"  // 중국어 번체
            language == "es" -> "_es"  // 스페인어
            else -> ""  // 기본값(영어): suffix 없음
        }

        // suffix가 없으면 원본 URL 반환
        if (languageSuffix.isEmpty()) {
            return bannerUrl
        }

        // 확장자 분리 및 언어 코드 삽입
        // 예: banner.png -> banner_ja.png
        val lastDotIndex = bannerUrl.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            val nameWithoutExt = bannerUrl.substring(0, lastDotIndex)
            val extension = bannerUrl.substring(lastDotIndex)
            "$nameWithoutExt$languageSuffix$extension"
        } else {
            // 확장자가 없는 경우 (드물지만) 그냥 뒤에 추가
            "$bannerUrl$languageSuffix"
        }
    }

    /**
     * HTTP URL을 HTTPS로 변환
     *
     * Android 네트워크 보안 정책으로 인해 HTTP (cleartext) 통신이 차단되므로
     * 모든 이미지 URL을 HTTPS로 변환하여 사용
     *
     * @return HTTPS로 변환된 URL
     */
    fun String?.toSecureUrl(): String {
        if (this.isNullOrEmpty()) return ""
        return this.replace("http://", "https://")
    }

    /**
     * 시스템 로케일 가져오기
     */
    private fun getSystemLocale(): Locale {
        return try {
            LocaleList.getDefault()[0]
        } catch (e: Exception) {
            Locale.getDefault()
        }
    }

    /**
     * URL에 쿼리 파라미터 추가/업데이트
     *
     * @param key 파라미터 키
     * @param value 파라미터 값
     * @return 업데이트된 URL
     */
    private fun String.updateQueryParameter(key: String, value: String): String {
        val uri = Uri.parse(this)
        val params = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }.toMutableMap()
        params[key] = value

        return uri.buildUpon()
            .clearQuery()
            .apply {
                params.forEach { (k, v) ->
                    appendQueryParameter(k, v)
                }
            }
            .build()
            .toString()
    }
}
