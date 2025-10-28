package net.ib.mn.utils

import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.HeartpickRepository
import net.ib.mn.core.data.repository.comments.CommentsRepository
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommentModel
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 댓글(comment, reply) 번역 헬퍼
 * 하트픽 댓글은 api가 달라서 분기 처리
 */

interface CommentTranslation {
    fun translateComment(item: CommentModel, position: Int)
}

@Singleton
class CommentTranslationHelper @Inject constructor(
    private val commentsRepository: CommentsRepository,
    private val heartpickRepository: HeartpickRepository,
) {
    fun clickTranslate(item: CommentModel, adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>, position: Int) {
        if(item.translateState == ArticleModel.TranslateState.ORIGINAL) {
            item.translateState = ArticleModel.TranslateState.TRANSLATING
            adapter.notifyItemChanged(position)

            val locale = "auto"
            val listener = { response: JSONObject ->
                try {
                    val gson = IdolGson.getInstance(true)
                    val model = gson.fromJson(response.getJSONObject("object").toString(), CommentModel::class.java)
                    model?.let {
                        item.originalContent = item.content ?: ""
                        item.content = model.content
                        item.translateState = ArticleModel.TranslateState.TRANSLATED
                        adapter.notifyItemChanged(position)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Unit
            }

            MainScope().launch {
                // 하트픽이라면
                if(item.resourceUri?.contains("/replies") == true) {
                    heartpickRepository.getReply(
                        item.id.toInt(),
                        translate = locale,
                        listener = listener,
                        errorListener = { throwable ->
                            item.translateState = ArticleModel.TranslateState.ORIGINAL
                            adapter.notifyItemChanged(position)
                        }
                    )
                } else {
                    // 일반 댓글
                    commentsRepository.getComment(
                        item.id.toLong(),
                        translate = locale,
                        listener = listener,
                        errorListener = { throwable ->
                            item.translateState = ArticleModel.TranslateState.ORIGINAL
                            adapter.notifyItemChanged(position)
                        }
                    )
                }
            }
        } else {
            item.content = item.originalContent
            item.translateState = ArticleModel.TranslateState.ORIGINAL
            adapter.notifyItemChanged(position)
        }
    }
}