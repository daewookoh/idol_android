package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.CommentsApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.CommentModel
import net.ib.mn.domain.model.CommentsResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 댓글 관련 Repository
 */
@Singleton
class CommentsRepository @Inject constructor(
    private val commentsApi: CommentsApi,
    private val gson: Gson
) : BaseRepository() {

    /**
     * 댓글 목록 조회
     *
     * @param articleId 게시글 ID
     * @param cursor 페이징 커서 (null이면 첫 페이지)
     * @param limit 페이지당 개수
     * @return 댓글 목록
     */
    fun getComments(
        articleId: Long,
        cursor: String? = null,
        limit: Int = 20
    ): Flow<ApiResult<CommentsResponse>> = safeApiCallWithJsonString(
        apiCall = { commentsApi.getComments(articleId, cursor, limit) },
        parser = { json -> parseCommentsResponse(json) }
    )

    /**
     * 댓글 작성 (텍스트만)
     *
     * @param articleId 게시글 ID
     * @param content 댓글 내용
     * @param emoticonId 이모티콘 ID (없으면 null)
     * @return 작성된 댓글
     */
    fun writeComment(
        articleId: Long,
        content: String,
        emoticonId: Int? = null
    ): Flow<ApiResult<CommentModel>> = safeApiCallWithJsonString(
        apiCall = {
            commentsApi.writeComment(
                articleId = createPart("article", articleId.toString()),
                content = createPart("content", content),
                emoticon = emoticonId?.let { createPart("emoticon", it.toString()) }
            )
        },
        parser = { json -> gson.fromJson(json, CommentModel::class.java) }
    )

    /**
     * 댓글 작성 (이미지 포함)
     *
     * @param articleId 게시글 ID
     * @param content 댓글 내용
     * @param imageFile 이미지 파일
     * @return 작성된 댓글
     */
    fun writeCommentWithImage(
        articleId: Long,
        content: String,
        imageFile: File
    ): Flow<ApiResult<CommentModel>> = safeApiCallWithJsonString(
        apiCall = {
            val imagePart = MultipartBody.Part.createFormData(
                "image",
                imageFile.name,
                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            )
            commentsApi.writeCommentWithImage(
                articleId = createPart("article", articleId.toString()),
                content = createPart("content", content),
                image = imagePart
            )
        },
        parser = { json -> gson.fromJson(json, CommentModel::class.java) }
    )

    /**
     * 댓글 삭제
     *
     * @param commentId 댓글 ID
     * @return 성공 여부
     */
    fun deleteComment(commentId: Int): Flow<ApiResult<Boolean>> = safeApiCallWithJsonString(
        apiCall = { commentsApi.deleteComment(commentId) },
        parser = { true }
    )

    /**
     * 댓글 번역
     * old 프로젝트의 CommentsRepository.getComment과 동일
     * API 응답: { "object": { ... } } 구조
     *
     * @param commentId 댓글 ID
     * @return 번역된 댓글
     */
    fun translateComment(commentId: Int): Flow<ApiResult<CommentModel>> = safeApiCallWithJsonString(
        apiCall = { commentsApi.translateComment(commentId, "auto") },
        parser = { json ->
            val jsonObject = org.json.JSONObject(json)
            val objectJson = jsonObject.getJSONObject("object").toString()
            gson.fromJson(objectJson, CommentModel::class.java)
        }
    )

    // ============================================================
    // Private Helpers
    // ============================================================

    private fun parseCommentsResponse(json: String): CommentsResponse {
        return try {
            gson.fromJson(json, CommentsResponse::class.java)
        } catch (e: Exception) {
            // 단순 리스트로 반환되는 경우 처리
            val listType = object : TypeToken<List<CommentModel>>() {}.type
            val comments: List<CommentModel> = gson.fromJson(json, listType)
            CommentsResponse(comments = comments)
        }
    }

    private fun createPart(name: String, value: String): MultipartBody.Part {
        return MultipartBody.Part.createFormData(name, value)
    }
}
