package net.ib.mn.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.ib.mn.data.local.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 욕설 필터링 유틸리티
 * Old 프로젝트의 Util.BadWordsFilterToHeart와 동일한 기능
 */
@Singleton
class BadWordFilter @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) {
    private var cachedBadWords: List<String>? = null

    /**
     * 텍스트에서 욕설을 하트로 치환
     */
    fun filter(text: String): String {
        val badWords = getBadWords()
        if (badWords.isEmpty()) return text

        var result = text
        badWords.forEach { word ->
            if (word.isNotBlank() && result.contains(word, ignoreCase = true)) {
                val replacement = "♡".repeat(word.length)
                result = result.replace(word, replacement, ignoreCase = true)
            }
        }
        return result
    }

    /**
     * 텍스트에 욕설이 포함되어 있는지 확인
     */
    fun containsBadWord(text: String): Boolean {
        val badWords = getBadWords()
        return badWords.any { word ->
            word.isNotBlank() && text.contains(word, ignoreCase = true)
        }
    }

    /**
     * 캐시된 욕설 목록 반환
     */
    private fun getBadWords(): List<String> {
        if (cachedBadWords == null) {
            cachedBadWords = loadBadWords()
        }
        return cachedBadWords ?: emptyList()
    }

    /**
     * PreferencesManager에서 욕설 목록 로드
     */
    private fun loadBadWords(): List<String> {
        return try {
            runBlocking {
                val json = preferencesManager.badWords.first()
                if (json.isNullOrEmpty()) {
                    emptyList()
                } else {
                    val listType = object : TypeToken<List<String>>() {}.type
                    gson.fromJson(json, listType) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            logE("BadWordFilter", "Failed to load bad words", e)
            emptyList()
        }
    }

    /**
     * 캐시 초기화 (설정 변경 시 호출)
     */
    fun clearCache() {
        cachedBadWords = null
    }
}
