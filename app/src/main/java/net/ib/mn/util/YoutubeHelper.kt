package net.ib.mn.util

object YoutubeHelper {

    private val YOUTUBE_PREFIXES = listOf(
        "https://youtu.be/",
        "https://www.youtube.com/",
        "https://m.youtube.com/",
        "https://youtube.com/"
    )

    private val VIDEO_ID_REGEX = Regex("""(?:v=|youtu\.be/|embed/|shorts/)([a-zA-Z0-9_-]{11})""")
    private val START_TIME_REGEX = Regex("""[?&]t=(\d+)""")
    private val YOUTUBE_LINK_REGEX = Regex("""https?://(www\.)?(youtube\.com|youtu\.be)/[^\s]+""")

    fun isYoutubeLink(linkUrl: String?, imageUrl: String?, umjjalUrl: String?): Boolean {
        return YOUTUBE_PREFIXES.any { linkUrl?.startsWith(it, ignoreCase = true) == true } &&
            (imageUrl.isNullOrEmpty() || umjjalUrl.isNullOrEmpty())
    }

    fun extractVideoId(url: String): String? = VIDEO_ID_REGEX.find(url)?.groupValues?.get(1)

    fun extractStartTime(url: String): Int = START_TIME_REGEX.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    fun getShortUrl(url: String): String = extractVideoId(url)?.let { "https://youtu.be/$it" } ?: url

    fun removeYoutubeLink(content: String): String = content.replace(YOUTUBE_LINK_REGEX, "").trim()
}
