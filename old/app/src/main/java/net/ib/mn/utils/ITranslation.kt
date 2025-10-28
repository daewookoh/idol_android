package net.ib.mn.utils

import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.model.ArticleModel

/**
 * 번역 관련 기능 확장
 */

interface Translation {
    fun translateArticle(item: ArticleModel,
                         position: Int,
                         adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?,
                         articlesRepository: ArticlesRepository
    ) {
        if(item.translateState == ArticleModel.TranslateState.ORIGINAL) {
            item.translateState = ArticleModel.TranslateState.TRANSLATING
            adapter?.notifyItemChanged(position)

            val locale = "auto"
            MainScope().launch {
                articlesRepository.getArticle(
                    item.id.toLong(),
                    translate = locale,
                    listener = { response ->
                        try {
                            val gson = IdolGson.getInstance(true)
                            val model = gson.fromJson(response.toString(), ArticleModel::class.java)
                            model?.let {
                                // 번역을 하면 새로 가져온 model은 nation이 바뀌므로 번역 부분만 바꿔치기한다
                                item.originalContent = item.content ?: ""
                                item.originalTitle = item.title ?: ""
                                item.content = model.content
                                item.title = model.title
                                item.translateState = ArticleModel.TranslateState.TRANSLATED
                                adapter?.notifyItemChanged(position)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    errorListener = { throwable ->
                    }
                )
            }
        } else {
            item.content = item.originalContent
            item.title = item.originalTitle
            item.translateState = ArticleModel.TranslateState.ORIGINAL
            adapter?.notifyItemChanged(position)
        }
    }
}
