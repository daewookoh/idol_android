package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

class ArticleModel : Serializable, Cloneable {
    // 번역 상태 (번역전, 번역중, 번역 완료)에 대한 enum
    enum class TranslateState {
        ORIGINAL, TRANSLATING, TRANSLATED
    }

    val user: UserModel? = null
    var idol: IdolModel? = null
    var content: String? = null
    var imageUrl: String? = null
    var heart: Long = 0
    @SerializedName("num_comments")
    var commentCount: Int = 0
    var reportCount: Int = 0
    var createdAt: Date = Date()
    var refDate: String? = null
    @SerializedName("resource_uri")
    private var _resourceUri: String? = null

    var enterTime: String? = null

    @SerializedName("id")
    private var _id: Long = 0L
    // id 필드가 없던 시절 resourceUri를 통해서 id를 추출하고 String 형태로 관리하여 이를 유지한다 (pref로 저장하고 있음)
    var id: String
        get() {
            if (_id == 0L && _resourceUri != null) {
                val splitUri = _resourceUri!!
                    .split("/")
                    .filter { it.isNotEmpty() }
                return splitUri.lastOrNull() ?: "0"
            }
            return _id.toString()
        }
        set(value) {
            _id = value.toLong()
        }

    val idolId: String? = null
    var linkTitle: String? = null
    var linkDesc: String? = null
    var linkUrl: String? = null

    // 뱃지
    // 뱃지 기능
    var isViewable: String? = null
        private set

    // 삭제한 사용자
    val deletedBy: UserModel? = null

    // 최애공개여부
    var isMostOnly: String? = null

    // 뉴비
    val isWelcome: String? = null

    // 움짤
    // 움짤 비디오
    var umjjalUrl: String? = null
    var thumbnailUrl: String? = null

    // 자유게시판 태그
    var tagId: Int = 0

    var files: MutableList<RemoteFileModel> = mutableListOf()
    var viewCount: Int = 0
    var type: String? = null
    var likeCount: Int = 0
    var title: String? = null
    @SerializedName("user_like")
    var isUserLike: Boolean = false
    var isUserLikeCache: Boolean = false
    var isLoading: Boolean = false
    var isEdit: Boolean = false
    var imageVer: Int = 0

    // 번역 관련
    var nation: String? = null
    var originalContent: String = "" // 번역 전 본문
    var originalTitle: String = "" // 번역 전 제목 (덕게)
    var translateState: TranslateState = TranslateState.ORIGINAL // 본문 번역됐는지 여부
    var isTranslatable: Boolean? = null // 번역 가능한지 여부

    var resourceUri: String
        get() = _resourceUri ?: "/api/v1/articles/$id/"
        set(value) {
            _resourceUri = value
            _id = try { value.split("/").lastOrNull()?.toLong() ?: 0L } catch (e: Exception) {
                0L
            }
        }

    // 보안관이 삭제한 것으로 처리
    fun setDeleted() {
        isViewable = "X"
    }

    fun getIsWelcome(): Boolean {
        return isWelcome != null && isWelcome == "Y"
    }

    fun setIsMostOnly(isMostOnly: String) {
        if (isMostOnly.equals("public", ignoreCase = true)) {
            this.isMostOnly = "N"
        } else {
            this.isMostOnly = "Y"
        }
    }

    var isPopular: Boolean = false

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone() as ArticleModel
    }

    override fun toString(): String {
        return "ArticleModel{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageVer=" + imageVer +
                ", heart=" + heart +
                ", likeCount=" + likeCount +
                ", numComments=" + commentCount +
                ", reported=" + reportCount +
                ", createdAt=" + createdAt +
                ", user=" + (user?.nickname ?: "null") +
                ", idol=" + (if (idol != null) idol.toString() else "null") +
                ", isMostOnly=" + isMostOnly +
                ", isWelcome=" + isWelcome +
                ", isPopular=" + isPopular +
                ", tagId=" + tagId +
                ", viewCount=" + viewCount +
                ", type='" + type + '\'' +
                ", files=" + (files.size.toString() + " files") +
                ", translateState=" + translateState +
                ", originalContent='" + originalContent + '\'' +
                ", isTranslatable=" + isTranslatable +
                '}'
    }

    override fun hashCode(): Int {
        return _id.hashCode()
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
