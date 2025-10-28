/**
 * Copyright (C) 2025. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author parkboo <parkboo@myloveidol.com>
 * Description: 번역 버튼 UI 헬퍼
 *
 * */
package net.ib.mn.utils.vote

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import net.ib.mn.R
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK

object TranslateUiHelper {
    /**
     * 번역 버튼의 UI를 구성
     *
     * @param context context
     * @param view 실제 번역 버튼
     * @param content 번역 대상이 되는 텍스트
     * @param nation 해당 게시물의 원문 언어 코드
     * @param translateState 현재 번역 상태 (ORIGINAL, TRANSLATED 등)
     * @param isTranslatableCached 번역 가능한지 이미 판단한 경우 넣어줌 (null이면 새로 판단)
     * @param useTranslation 번역 사용 여부
     * @param onClick 번역 버튼 클릭 시 호출할 콜백
     *
     * @return true면 번역 버튼이 표시됨, false면 숨겨짐
     */
    fun bindTranslateButton(
        context: Context,
        view: TextView,
        content: String,
        systemLanguage: String,
        nation: String?,
        translateState: ArticleModel.TranslateState?,
        isTranslatableCached: Boolean?,
        useTranslation: Boolean,
        onClick: (() -> Unit)? = null
    ): Boolean {
        val shouldTranslate = useTranslation && // 번역기능이 활성화 되어있고
            content.isNotEmpty() && // 번역할 내용이 있고
            !nation.isNullOrEmpty() && // 게시물의 언어 코드가 있고
            !systemLanguage.startsWith(nation) && // 게시물 언어와 시스템 언어가 다르고
            UtilK.extractTranslatable(content).isNotEmpty() // 번역 가능한 내용이 있는 경우

        val isTranslatable = isTranslatableCached ?: shouldTranslate

        if (isTranslatable) {
            view.visibility = View.VISIBLE
            view.text = when (translateState) {
                ArticleModel.TranslateState.ORIGINAL, null -> getTranslateButtonTitle(context)
                ArticleModel.TranslateState.TRANSLATING -> context.getString(R.string.translating)
                ArticleModel.TranslateState.TRANSLATED -> context.getString(R.string.see_original)
            }

            val color = when (translateState) {
                ArticleModel.TranslateState.TRANSLATING -> R.color.text_dimmed
                else -> R.color.text_gray
            }
            view.setTextColor(ContextCompat.getColor(context, color))

            onClick?.let { view.setOnClickListener { onClick() } }

            return true
        } else {
            view.visibility = View.GONE
            return false
        }
    }

    // 한중일영서 이외의 언어는 "영어로 번역하기" 보여준다
    private fun getTranslateButtonTitle(context: Context): String {
        val locale = Util.getSystemLanguage(context).lowercase().replace("_", "-")
        ConfigModel.getInstance(context).translationLocales.forEach {
            if( locale.startsWith(it)) {
                return context.getString(R.string.see_translate)
            }
        }
        return context.getString(R.string.see_translate_to_english)
    }
}