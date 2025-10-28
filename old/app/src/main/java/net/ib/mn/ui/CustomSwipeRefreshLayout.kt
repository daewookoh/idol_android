/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 연속된 API호출을 막기위한 커스텀 스와이프 레이아웃.
 *
 * */

package net.ib.mn.ui

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * @see setCustomRefreshListener : 새로고침 리스너 설정.
 * */

class CustomSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs), CoroutineScope by MainScope() {

    private var isRefreshingInProgress = false

    fun setCustomRefreshListener(listener: suspend () -> Unit) {
        setOnRefreshListener {
            if (isRefreshingInProgress) {
                return@setOnRefreshListener
            }

            isRefreshingInProgress = true
            isRefreshing = true

            launch {
                try {
                    withTimeout(MAX_SWIPE_REFRESH_DURATION) {
                        listener()
                    }
                } finally {
                    delay(DELAY_SWIPE_REFRESH_DURATION) // 2초동안 새로 고침 방지.
                    isRefreshingInProgress = false
                    isRefreshing = false
                }
            }
        }
    }

    companion object {
        const val MAX_SWIPE_REFRESH_DURATION = 5000L
        const val DELAY_SWIPE_REFRESH_DURATION = 2000L
    }
}