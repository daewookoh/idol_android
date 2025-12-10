package net.ib.mn.domain.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 게시글 업데이트를 전역적으로 관리하는 매니저
 *
 * ArticleDetail에서 좋아요, 댓글, 투표 등이 변경되면
 * SearchResultScreen, CommunityScreen 등에서 실시간으로 반영할 수 있도록
 * SharedFlow를 통해 이벤트를 전파합니다.
 */
@Singleton
class ArticleUpdateManager @Inject constructor() {

    private val _articleUpdateEvent = MutableSharedFlow<ArticleUpdateEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val articleUpdateEvent: SharedFlow<ArticleUpdateEvent> = _articleUpdateEvent.asSharedFlow()

    /**
     * 게시글 업데이트 이벤트 발행
     */
    suspend fun emitUpdate(event: ArticleUpdateEvent) {
        _articleUpdateEvent.emit(event)
    }

    /**
     * 게시글 업데이트 이벤트 발행 (비동기)
     */
    fun tryEmitUpdate(event: ArticleUpdateEvent) {
        _articleUpdateEvent.tryEmit(event)
    }
}

/**
 * 게시글 업데이트 이벤트
 */
sealed class ArticleUpdateEvent {
    /**
     * 게시글 정보 업데이트 (좋아요, 댓글 수, 하트 등)
     */
    data class Updated(
        val articleId: String,
        val likeCount: Int? = null,
        val commentCount: Int? = null,
        val heart: Long? = null
    ) : ArticleUpdateEvent()

    /**
     * 게시글 삭제
     */
    data class Deleted(val articleId: String) : ArticleUpdateEvent()
}
