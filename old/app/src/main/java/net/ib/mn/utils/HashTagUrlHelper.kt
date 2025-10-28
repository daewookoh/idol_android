/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.utils

import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

class HashTagUrlHelper private constructor(
    private val mHashTagWordColor: Int,
    private val clickCallback: (clickedHashTag: String) -> Unit = {}
) {

    private var mTextView: TextView? = null

    object Creator {
        fun create(color: Int): HashTagUrlHelper {
            return HashTagUrlHelper(color)
        }
        fun create(color: Int, clickCallback: (clickedHashTag: String) -> Unit): HashTagUrlHelper {
            return HashTagUrlHelper(color, clickCallback)
        }
    }

    private val mTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
            if (text.isNotEmpty()) {
                eraseAndColorizeAllText(text)
            }
        }
    }

    //텍스트뷰 전용 해시태그.
    fun handleForTextView(textView: TextView?) {
        if (mTextView == null) {
            mTextView = textView
            mTextView?.movementMethod = LinkMovementMethod.getInstance()

            // in order to use spannable we have to set buffer type
            mTextView?.setText(mTextView!!.text, TextView.BufferType.SPANNABLE)
            mTextView?.let { setColorsToAllHashTagsUrls(it.text) }
        } else {
            throw RuntimeException("TextView is not null. You need to create a unique HashTagHelper for every TextView")
        }
    }

    fun handle(textView: TextView?) {
        if (mTextView == null) {
            mTextView = textView
            mTextView?.addTextChangedListener(mTextWatcher)
            mTextView?.movementMethod = LinkMovementMethod.getInstance()

            // in order to use spannable we have to set buffer type
            mTextView?.setText(mTextView!!.text, TextView.BufferType.SPANNABLE)

            mTextView?.let { setColorsToAllHashTagsUrls(it.text) }
        } else {
            throw RuntimeException("TextView is not null. You need to create a unique HashTagHelper for every TextView")
        }
    }

    private fun eraseAndColorizeAllText(text: CharSequence) {
        val spannable: Spannable = mTextView?.text as Spannable
        val spans: Array<CharacterStyle> = spannable.getSpans(
            0, text.length,
            CharacterStyle::class.java
        )
        for (span in spans) {
            spannable.removeSpan(span)
        }
        setColorsToAllHashTagsUrls(text)
    }

    private fun setColorsToAllHashTagsUrls(text: CharSequence) {
        var startIndexOfNextHashSign: Int
        var index = 0
        while (index < text.length - 1) {
            val sign = text[index]
            var nextNotLetterDigitCharIndex =
                index + 1 // we assume it is next. if if was not changed by findNextValidHashTagChar then index will be incremented by 1
            if (sign == '#') {
                startIndexOfNextHashSign = index
                nextNotLetterDigitCharIndex =
                    findNextValidHashTagChar(text, startIndexOfNextHashSign)
                setColorForHashTagToTheEnd(startIndexOfNextHashSign, nextNotLetterDigitCharIndex)
            }
            index = nextNotLetterDigitCharIndex
        }

        val urlList = UtilK.extractUrls(text.toString())
        var mutableUrlIndex = 0  //전체 텍스트 index 찾을 때 특정 포지션부터 시작하는 값 저장하는 변수

        //Url 개수만큼 for문
        for(i in urlList.indices){
            val startIndex = text.indexOf(urlList[i], mutableUrlIndex)
            mutableUrlIndex = startIndex + urlList[i].length

            setColorForHashTagToTheEnd(startIndex, startIndex+urlList[i].length)
        }
    }

    private fun findNextValidHashTagChar(text: CharSequence, start: Int): Int {
        var nonLetterDigitCharIndex = -1 // skip first sign '#"
        for (index in start + 1 until text.length) {

            val sign = text[index]

            //해시태그 사이 #이 들어가면  다시 invalid처리 -> 다음 해시태그처리로 넘어갈거임.
            val isValidSign = (sign != '\n') && (sign != ' ') && (sign != '#')

            if (!isValidSign) {
                nonLetterDigitCharIndex = index
                break
            }
        }
        if (nonLetterDigitCharIndex == -1) {

            // we didn't find non-letter. We are at the end of text
            nonLetterDigitCharIndex = text.length
        }

        return nonLetterDigitCharIndex
    }

    private fun setColorForHashTagToTheEnd(startIndex: Int, nextNotLetterDigitCharIndex: Int) {
        val s: Spannable = mTextView?.text as Spannable
        s.setSpan(
            object : ClickableSpan() {
                override fun onClick(p0: View) {
                    //클릭된 해시태그
                    val clickedHashTag = s.subSequence(startIndex, nextNotLetterDigitCharIndex).toString()
                    clickCallback(clickedHashTag)
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = mHashTagWordColor
                    ds.isUnderlineText = false
                }
            },
            startIndex, nextNotLetterDigitCharIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    //전체 해시 태그 가져오기
    fun getAllHashTags(withHashes: Boolean): List<String> {
        val text: String = mTextView?.text.toString()
        val spannable: Spannable = mTextView?.text as Spannable

        // use set to exclude duplicates
        val hashTags: MutableSet<String> = LinkedHashSet()
        for (span in spannable.getSpans(0, text.length, CharacterStyle::class.java)) {
            hashTags.add(
                text.substring(
                    if (!withHashes) spannable.getSpanStart(span) + 1 /*skip "#" sign*/ else spannable.getSpanStart(
                        span
                    ),
                    spannable.getSpanEnd(span)
                )
            )
        }
        return ArrayList(hashTags)
    }
}