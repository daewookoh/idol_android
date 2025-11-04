package net.ib.mn.util

import android.net.Uri
import net.ib.mn.data.local.entity.IdolEntity

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
