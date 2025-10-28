package net.ib.mn.model

import com.google.gson.annotations.SerializedName
/**
 *댓글 이모티콘에서  댓글 comment와 이모티콘을 함께 보냈때
 *해당 댓글에서 보내주는 alt값을  처리를 위한 data class
 */
data class contentAlt(
    @SerializedName("emoticon_id") val emoticonId: Int?=CommentModel.NO_EMOTICON_ID,
    @SerializedName("is_emoticon") val isEmoticon: Boolean =false,
    @SerializedName("text") val text: String?="",
    @SerializedName("image_url") val imageUrl: String?=CommentModel.NO_IMAGE_URL,
    @SerializedName("is_image") val isImage: Boolean? =false,
    @SerializedName("umjjal_url") val umjjalUrl: String?=CommentModel.NO_UMJJAL_URL,
    @SerializedName("is_umjjal") val isUmjjal: Boolean? =false
)
