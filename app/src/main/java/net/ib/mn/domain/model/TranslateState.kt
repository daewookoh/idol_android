package net.ib.mn.domain.model

/**
 * 번역 상태
 * old 프로젝트의 ArticleModel.TranslateState와 동일
 */
enum class TranslateState {
    ORIGINAL,    // 원문 상태
    TRANSLATING, // 번역 중
    TRANSLATED   // 번역 완료
}
