package net.ib.mn.utils

import net.ib.mn.adapter.CommunityArticlePagerAdapter.Companion.VIEW_TYPE_COMMON
import net.ib.mn.adapter.CommunityArticlePagerAdapter.Companion.VIEW_TYPE_YOUTUBE
import net.ib.mn.model.ArticleModel

class YoutubeHelper {
    companion object {
        fun hasYoutubeLink(article: ArticleModel): Boolean {
            val youtubePrefixes = listOf(
                "https://youtu.be/",
                "https://www.youtube.com/",
                "https://m.youtube.com/",
                "https://youtube.com/"
            )

            // 유튜브 링크를 올리고 게시글 수정으로 썸네일 바꿔치는 트릭을 사용하면 유튜브 플레이어를 안보여준다
            return (youtubePrefixes.any { article.linkUrl?.startsWith(it, ignoreCase = true) ?: false } &&
                (article.imageUrl.isNullOrEmpty() || article.umjjalUrl.isNullOrEmpty()))
        }

        fun extractYoutubeVideoId(url: String): String? {
            val regex = Regex("""(?:v=|youtu\.be/|embed/|shorts/)([a-zA-Z0-9_-]{11})""")
            return regex.find(url)?.groups?.get(1)?.value
        }

        fun extractYoutubeVideoStartTime(url: String): Int {
            val regex = Regex("""[?&]t=(\d+)""")
            return regex.find(url)?.groups?.get(1)?.value?.toIntOrNull() ?: 0
        }

    }
}