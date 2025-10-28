package net.ib.mn.utils

/**
 * Created by parkboo on 2016. 7. 25..
 */
class KorString @JvmOverloads constructor(snt: String? = null) {
    var snt: StringBuffer?

    init {
        this.snt = StringBuffer(snt ?: "")
    }

    /**
     * 마지막 글자가 한글이고 받침이 존재하는지를 체크함 .
     */
    fun hasFinalConsonant(): Boolean {
        if (snt == null || snt!!.length == 0) return false
        val code = snt!!.substring(snt!!.length - 1).codePointAt(0)
        //한글 문자이고 받침이 존재함
        return if (code >= 44032 && code <= 55203 && (code - 44032) % 28 > 0) {
            true
        } else false
    }

    /**
     * 문자열을 더하는데, 별도의 처리를 하지 않고서
     * @param str
     * @return
     */
    fun appendOnly(str: String?): KorString {
        snt!!.append(str)
        return this
    }

    /**
     * 문자열을 더하는데, 첫번째 단어가 조사인경우에는 조사자동처리를 한다.
     * @param str
     * @return
     */
    fun append(str: String): KorString {
        val idx = str.indexOf(" ")
        return if (idx <= 0) appendOnly(str) else {
            appendJosa(str.substring(0, idx)).appendOnly(str.substring(idx))
        }
    }

    /**
     * 모든 문자열을 삭제한다.
     * @return
     */
    fun clear(): KorString {
        snt = StringBuffer()
        return this
    }

    /**
     * 조사를 더한다. (받침에 따라서 조사가 자동으로 변화하면서)
     * @param josa1
     * @return
     */
    fun appendJosa(josa1: String): KorString {
        if (hasFinalConsonant()) {
            for (i in josalist.indices) {
                if (josa1 == josalist[i]) {
                    return if (i % 2 == 0) appendOnly(josalist[i]) else appendOnly(
                        josalist[i - 1]
                    )
                }
            }
        } else {
            for (i in josalist.indices) {
                if (josa1 == josalist[i]) {
                    return if (i % 2 == 0) appendOnly(josalist[i + 1]) else appendOnly(
                        josalist[i]
                    )
                }
            }
        }
        return appendOnly(josa1)
    }

    override fun toString(): String {
        return snt.toString()
    }

    companion object {
        var josalist = arrayOf(
            "은", "는",
            "이", "가",
            "을", "를",
            "과", "와",
            "으로", "로",
            "으로서", "로서",
            "으로서의", "로서의",
            "으로써", "로써"
        )
    }
}
