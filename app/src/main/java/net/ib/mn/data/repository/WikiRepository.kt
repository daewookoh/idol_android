package net.ib.mn.data.repository

import net.ib.mn.data.remote.api.WikiApi
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.util.ServerUrl
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WikiRepository - 위키 관련 Repository
 *
 * old 프로젝트의 WikiActivity + IdolsRepository.getWikiName() 로직 기반
 *
 * 기능:
 * 1. getWikiName: 아이돌 ID와 로케일로 위키 이름 조회
 * 2. getWikiUrl: 위키 URL 생성 및 리다이렉트 URL 조회
 */
interface WikiRepository {
    /**
     * 위키 이름 조회
     * @param idolId 아이돌 ID
     * @param locale 로케일 (ko, en, ja, zh-cn, zh-tw)
     * @return 위키 이름 또는 null
     */
    suspend fun getWikiName(idolId: Int, locale: String): ApiResult<String>

    /**
     * 위키 URL 조회 (리다이렉트 포함)
     * @param idolId 아이돌 ID
     * @param locale 로케일
     * @return 최종 위키 URL 또는 null
     */
    suspend fun getWikiUrl(idolId: Int, locale: String): ApiResult<String>
}

@Singleton
class WikiRepositoryImpl @Inject constructor(
    private val wikiApi: WikiApi
) : WikiRepository {

    override suspend fun getWikiName(idolId: Int, locale: String): ApiResult<String> {
        return try {
            val response = wikiApi.getWikiName(idolId, locale)
            if (response.isSuccessful && response.body()?.success == true) {
                val wikiName = response.body()?.wikiName
                if (wikiName != null) {
                    ApiResult.Success(wikiName)
                } else {
                    ApiResult.Error(ApiError.Business(gcode = 0, message = "Wiki name not found"))
                }
            } else {
                ApiResult.Error(ApiError.fromHttpCode(response.code(), "Failed to get wiki name"))
            }
        } catch (e: Exception) {
            ApiResult.Error(ApiError.Unknown(exception = e))
        }
    }

    override suspend fun getWikiUrl(idolId: Int, locale: String): ApiResult<String> {
        return try {
            // 1. 위키 이름 조회
            val wikiNameResult = getWikiName(idolId, locale)
            if (wikiNameResult is ApiResult.Error) {
                return wikiNameResult
            }
            val wikiName = (wikiNameResult as ApiResult.Success).data

            // 2. 특수문자 인코딩 (old 프로젝트의 Util.encodingSpecialChar와 동일)
            val encodedWikiName = encodeSpecialChar(wikiName)

            // 3. 위키 URL 생성
            val wikiUrl = "${ServerUrl.HOST}/wiki/${locale}/idol/${encodedWikiName}/"

            // 4. 리다이렉트 URL 조회
            val redirectResponse = wikiApi.redirect(wikiUrl)
            if (redirectResponse.isSuccessful && redirectResponse.body()?.success == true) {
                val finalUrl = redirectResponse.body()?.url
                if (finalUrl != null) {
                    ApiResult.Success(finalUrl)
                } else {
                    ApiResult.Error(ApiError.Business(gcode = 0, message = "Redirect URL not found"))
                }
            } else {
                ApiResult.Error(ApiError.fromHttpCode(redirectResponse.code(), "Failed to get redirect URL"))
            }
        } catch (e: Exception) {
            ApiResult.Error(ApiError.Unknown(exception = e))
        }
    }

    /**
     * 특수문자 인코딩
     * old 프로젝트의 Util.encodingSpecialChar()와 동일
     */
    private fun encodeSpecialChar(text: String): String {
        return try {
            URLEncoder.encode(text, "UTF-8")
        } catch (e: Exception) {
            text
        }
    }
}
