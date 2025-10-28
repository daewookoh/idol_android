package net.ib.mn.utils

import android.os.Handler
import android.text.Editable
import android.text.TextWatcher

class DelayedTextWatcher(private val delayMillis: Long, private val callback: TextWatcherCallBack) : TextWatcher {
    private val handler: Handler = Handler()
    private var runnable: Runnable? = null

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // 텍스트 변경 전에 호출됩니다. 필요 없는 경우 구현하지 않습니다.
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // 텍스트 변경 중에 호출됩니다. 필요 없는 경우 구현하지 않습니다.
    }

    override fun afterTextChanged(s: Editable) {
        // 텍스트 변경 후에 호출됩니다. 이전에 예약된 작업이 있으면 취소합니다.
        if (runnable != null) {
            handler.removeCallbacks(runnable!!)
        }

        // delayMillis 이후에 실행될 작업을 예약합니다.
        runnable = Runnable {
            callback.onTextChanged(s)
        }
        runnable?.let { handler.postDelayed(it, delayMillis) }
    }

    fun interface TextWatcherCallBack{
        fun onTextChanged(s: CharSequence)
    }
}