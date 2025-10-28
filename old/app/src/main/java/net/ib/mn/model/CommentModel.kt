package net.ib.mn.model

import net.ib.mn.model.ArticleModel.TranslateState
import java.io.Serializable
import java.util.Date

class CommentModel : Serializable {
    var id: Int = 0
    var content: String? = null
    var user: UserModel? = null
    var article: ArticleModel? = null
    var createdAt: Date = Date()
    var resourceUri: String? = null
    var thumbnailUrl: String? = null
    var thumbHeight: Int = 0
    var thumbWidth: Int = 0
    var imageUrl: String? = null
    var umjjalUrl: String? = null
    var emoticonId: Int = NO_EMOTICON_ID

    //contentalt값 받아옴.
    var contentAlt: contentAlt? = null

    // 번역 관련
    var nation: String? = null
    var originalContent: String = "" // 번역 전 본문
    var translateState: TranslateState = TranslateState.ORIGINAL // 본문 번역됐는지 여부
    var isTranslatable: Boolean? = null // 번역 가능한지 여부

    companion object {
        /**
         *
         */
        private const val serialVersionUID = 1L
        const val NO_EMOTICON_ID: Int = -100
        val NO_IMAGE_URL: String? = null
        val NO_UMJJAL_URL: String? = null
    }
}
