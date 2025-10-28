package net.ib.mn

import junit.framework.Assert.assertEquals
import net.ib.mn.utils.UtilK
import org.junit.Test

class TranslationTest {
    @Test
    fun `URL이 포함된 문자열 테스트`() {
        val input = "방금 좋은 글을 읽었어요! https://example.com"
        val expected = "방금 좋은 글을 읽었어요!"
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `해시태그가 포함된 문자열 테스트`() {
        val input = "오늘 날씨가 정말 좋네요! #날씨 #맑음"
        val expected = "오늘 날씨가 정말 좋네요!"
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `URL과 해시태그가 모두 포함된 문자열 테스트`() {
        val input = "여기 확인해보세요 https://example.com #정보 #꿀팁"
        val expected = "여기 확인해보세요"
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `URL과 해시태그가 연속적으로 있는 경우 테스트`() {
        val input = "https://abc.com #테스트 https://xyz.com #확인"
        val expected = ""
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `특수문자와 함께 있는 URL과 해시태그 테스트`() {
        val input = "대박!#놀람 https://wow.com?! #충격"
        val expected = "대박!"
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `URL과 해시태그가 없는 깨끗한 문자열 테스트`() {
        val input = "이건 정말 깔끔한 문장이에요."
        val expected = "이건 정말 깔끔한 문장이에요."
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `이모지를 포함하는 문자열 테스트`() {
        val input = "❤️이모지를 포함하는 문자열 😊❤️"
        val expected = "이모지를 포함하는 문자열"
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `이모지 URL 해시태그 모두를 포함하는 문자열 테스트`() {
        val input = "이모지를 포함하는 문자열 😊 https://example.com #테스트"
        val expected = "이모지를 포함하는 문자열"
        assertEquals(expected, UtilK.extractTranslatable(input))
    }

    @Test
    fun `언급이 있는 문자열 테스트`() {
        val input = "언급이 있는 문자열 @{3099076:게섯거라}"
        val expected = "언급이 있는 문자열"
        assertEquals(expected, UtilK.extractTranslatable(input))
    }
}