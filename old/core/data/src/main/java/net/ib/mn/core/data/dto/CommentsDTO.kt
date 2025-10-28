package net.ib.mn.core.data.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray

/**
 * 			params.put("user", account.getUserResourceUri());
 * 			params.put("article", article.getResourceUri());
 *
 *             //이모티콘 댓글 맞을때
 * 			if(emoticonId != CommentModel.NO_EMOTICON_ID){
 *                 params.put("emoticon",emoticonId);
 * 			}
 * 			params.put("content", comment);
 *
 */
@Serializable
data class WriteCommentDTO(
//    @SerialName("user") val user: String, // 구시대의 유물로 더이상 사용하지 않음. 히스토리 확인을 위해 남겨둠
    @SerialName("article") val articleId: Long, // resource uri 형식, id 형식 둘 다 가능
    @SerialName("emoticon") val emoticon: Int? = null,
    @SerialName("content") val content: String
)
