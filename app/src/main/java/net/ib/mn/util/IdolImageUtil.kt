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
     * @param imageUrl 첫 번째 이미지 URL
     * @param imageUrl2 두 번째 이미지 URL
     * @param imageUrl3 세 번째 이미지 URL
     * @param top3 top3 ID 리스트 (쉼표로 구분된 문자열)
     * @param top3ImageVer top3 이미지 버전 (쉼표로 구분된 문자열)
     * @return 3개 이미지 URL 리스트
     */
    fun getTop3ImageUrls(
        imageUrl: String?,
        imageUrl2: String?,
        imageUrl3: String?,
        top3: String?,
        top3ImageVer: String?
    ): List<String?> {
        val top3Ids = top3?.split(",")?.map { it.trim() } ?: emptyList()
        val verString = top3ImageVer ?: ""
        val urls = listOf(imageUrl, imageUrl2, imageUrl3)

        // top3ImageVer가 비어있거나 top3Ids가 모두 null/empty인 경우
        if (verString.isEmpty() || top3Ids.all { it.isEmpty() || it == "0" }) {
            return urls
        }

        val result = top3Ids.take(3).mapIndexed { index, id ->
            if (id.isNotEmpty() && id != "0") {
                val ver = verString.split(",").getOrNull(index)?.trim() ?: ""
                val originalUrl = urls.getOrNull(index)
                originalUrl?.updateQueryParameter("ver", ver)
            } else {
                urls.getOrNull(index)
            }
        }.let { list ->
            // 3개 미만이면 나머지 기본 이미지로 채우기
            list + urls.drop(list.size)
        }.take(3)

        return result
    }

    /**
     * IdolEntity에서 Top3 이미지 URL 리스트 가져오기 (convenience method)
     */
    fun getTop3ImageUrls(idol: IdolEntity): List<String?> {
        return getTop3ImageUrls(
            imageUrl = idol.imageUrl,
            imageUrl2 = idol.imageUrl2,
            imageUrl3 = idol.imageUrl3,
            top3 = idol.top3,
            top3ImageVer = idol.top3ImageVer
        )
    }

    /**
     * Top3 동영상 URL 리스트 생성
     *
     * 움짤이 있는 경우:
     * - .mp4 파일이면 그대로 사용
     * - _s_mv.jpg 파일이면 _m_mv.mp4로 변환
     * - 그 외에는 null 반환
     *
     * @param imageUrl 첫 번째 이미지 URL
     * @param imageUrl2 두 번째 이미지 URL
     * @param imageUrl3 세 번째 이미지 URL
     * @param top3 top3 ID 리스트 (쉼표로 구분된 문자열)
     * @param top3ImageVer top3 이미지 버전 (쉼표로 구분된 문자열)
     * @return 3개 동영상 URL 리스트 (없으면 null)
     */
    fun getTop3VideoUrls(
        imageUrl: String?,
        imageUrl2: String?,
        imageUrl3: String?,
        top3: String?,
        top3ImageVer: String?
    ): List<String?> {
        val imageUrls = getTop3ImageUrls(imageUrl, imageUrl2, imageUrl3, top3, top3ImageVer)

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
     * IdolEntity에서 Top3 비디오 URL 리스트 가져오기 (convenience method)
     */
    fun getTop3VideoUrls(idol: IdolEntity): List<String?> {
        return getTop3VideoUrls(
            imageUrl = idol.imageUrl,
            imageUrl2 = idol.imageUrl2,
            imageUrl3 = idol.imageUrl3,
            top3 = idol.top3,
            top3ImageVer = idol.top3ImageVer
        )
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
     * 이모티콘 이미지 URL 생성
     *
     * old 프로젝트의 UtilK.fileImageUrl() 로직:
     * ${cdnUrl}/e/${emoticonId}.t.webp
     *
     * 서버에서 받아온 imageUrl/thumbnail을 무시하고,
     * emoticonId를 기반으로 CDN URL을 직접 생성
     *
     * @param cdnUrl CDN 베이스 URL
     * @param emoticonId Emoticon ID
     * @return 이모티콘 이미지 URL
     */
    fun getEmoticonImageUrl(
        cdnUrl: String,
        emoticonId: Int
    ): String {
        return "$cdnUrl/e/$emoticonId.t.webp"
    }

    /**
     * Trend 이미지 URL 생성
     *
     * old 프로젝트의 UtilK.trendImageUrl() 로직:
     * ${cdnUrl}/t/${id}.1_${IMAGE_SIZE_LOWEST}.webp
     * IMAGE_SIZE_LOWEST = "200x200"
     *
     * @param cdnUrl CDN 베이스 URL
     * @param trendId Trend ID
     * @param imageSize 이미지 사이즈 (기본값: 200x200)
     * @return Trend 이미지 URL
     */
    fun getTrendImageUrl(
        cdnUrl: String,
        trendId: Int,
        imageSize: String = "200x200"
    ): String {
        return "$cdnUrl/t/$trendId.1_$imageSize.webp"
    }

    /**
     * Hof (명예의 전당) 이미지 URL 생성
     *
     * old 프로젝트의 HallOfFameDayAdapter 로직:
     * ${cdnUrl}/h/${resourceId}.1_${IMAGE_SIZE_LOWEST}.webp
     * IMAGE_SIZE_LOWEST = "200x200"
     *
     * @param cdnUrl CDN 베이스 URL
     * @param hofId Resource ID (resource_uri에서 추출)
     * @param imageSize 이미지 사이즈 (기본값: 200x200)
     * @return Hall 이미지 URL
     */
    fun getHofImageUrl(
        cdnUrl: String,
        hofId: Int,
        imageSize: String = "200x200"
    ): String {
        return "$cdnUrl/h/$hofId.1_$imageSize.webp"
    }

    /**
     * Idol 이미지 URL 생성
     *
     * sourceApp에 따라 다른 경로 사용:
     * - "celeb": ${cdnUrl}/c/${idolId}.1_${imageSize}.webp
     * - 기타: ${cdnUrl}/i/${idolId}.1_${imageSize}.webp
     *
     * @param cdnUrl CDN 베이스 URL
     * @param idolId Idol ID
     * @param sourceApp 소스 앱 (celeb 등)
     * @param imageSize 이미지 사이즈 (기본값: 200x200)
     * @return Idol 이미지 URL
     */
    fun getIdolImageUrl(
        cdnUrl: String,
        idolId: Int,
        sourceApp: String? = null,
        imageSize: String = "200x200"
    ): String {
        val prefix = when (sourceApp) {
            "celeb" -> "/c/"
            else -> "/i/"
        }
        return "$cdnUrl$prefix$idolId.1_$imageSize.webp"
    }

    /**
     * Top1 이미지 URL 생성 (어워즈용)
     *
     * old 프로젝트의 UtilK.top1ImageUrl() 로직:
     * ${cdnUrl}/a/${top3Id}.1.${ver}_${imageSize}.webp
     *
     * @param cdnUrl CDN 베이스 URL
     * @param top3 top3 ID 문자열 (쉼표로 구분)
     * @param top3ImageVer top3 이미지 버전 (쉼표로 구분)
     * @param imageSize 이미지 사이즈 (기본값: 200x200)
     * @return Top1 이미지 URL (없으면 null)
     */
    fun getTop1ImageUrl(
        cdnUrl: String,
        top3: String?,
        top3ImageVer: String? = null,
        imageSize: String = "200x200"
    ): String? {
        if (top3.isNullOrEmpty()) return null

        val top3Ids = top3.split(",").map { it.trim() }
        val firstId = top3Ids.firstOrNull()

        if (firstId.isNullOrEmpty() || firstId == "null" || firstId == "0") {
            return null
        }

        val ver = top3ImageVer?.split(",")?.firstOrNull()?.trim() ?: ""
        return if (ver.isNotEmpty()) {
            "$cdnUrl/a/$firstId.1.${ver}_$imageSize.webp"
        } else {
            "$cdnUrl/a/$firstId.1_$imageSize.webp"
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
