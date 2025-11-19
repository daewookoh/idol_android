package net.ib.mn.util

import net.ib.mn.BuildConfig

/**
 * 지원 언어 정의
 */
object SupportedLanguage {
    /**
     * 게시판, 친척, 퀴즈, TOP100 지원 로케일
     * CELEB 빌드와 일반 빌드에서 지원하는 언어가 다름
     */
    val BOARD_KIN_QUIZZES_TOP100_LOCALES = if (BuildConfig.CELEB) {
        listOf("ko", "en", "ja")
    } else {
        listOf("ko", "en", "ja", "zh", "es", "pt", "id", "vi", "th", "fr")
    }
}
