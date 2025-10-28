/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: fragment 관련 extension 함수
 *
 * */

package net.ib.mn.utils.ext

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @see
 * */

fun FragmentManager.asyncPopBackStack(afterCallBackStack: () -> Unit = {}) {
    Handler(Looper.getMainLooper()).post {
        this.popBackStack()
        afterCallBackStack()
    }
}

inline fun <T> Fragment.collectLatestStateFlow(
    flow: StateFlow<T>,
    crossinline collect: (T) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest {
                collect(it)
            }
        }
    }
}