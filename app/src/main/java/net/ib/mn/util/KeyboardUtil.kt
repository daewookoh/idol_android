package net.ib.mn.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager

/**
 * 키보드 유틸리티
 * 키보드 표시/숨김 처리를 통합 관리
 */
object KeyboardUtil {
    
    /**
     * 키보드 숨기기
     * @param context Context
     * @param view 현재 포커스된 View (null이면 Activity의 DecorView 사용)
     */
    @JvmStatic
    fun hideKeyboard(context: Context, view: View? = null) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
        
        val targetView = view ?: (context as? Activity)?.window?.decorView?.rootView
        targetView?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
    
    /**
     * 키보드 표시
     * @param context Context
     * @param view 키보드를 표시할 View
     */
    @JvmStatic
    fun showKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
        
        view.requestFocus()
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * Compose에서 사용할 수 있는 키보드 숨기기 함수
     * @param context Context (LocalContext.current)
     * @param focusManager FocusManager (LocalFocusManager.current)
     */
    @Composable
    fun hideKeyboardCompose() {
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        
        focusManager.clearFocus()
        
        val activity = context as? Activity
        activity?.let {
            val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.let { inputMethodManager ->
                val currentFocus = it.currentFocus
                if (currentFocus != null) {
                    inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                } else {
                    val decorView = it.window.decorView
                    inputMethodManager.hideSoftInputFromWindow(decorView.windowToken, 0)
                }
            }
        }
    }
}

