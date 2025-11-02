package net.ib.mn.util

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Toast 유틸리티
 * Toast 메시지 표시를 통합 관리
 */
object ToastUtil {
    
    /**
     * Toast 메시지 표시
     * @param context Context
     * @param message 표시할 메시지
     * @param duration Toast 지속 시간 (기본: LENGTH_SHORT)
     */
    @JvmStatic
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Toast 메시지 표시 (리소스 ID 사용)
     * @param context Context
     * @param messageResId 메시지 리소스 ID
     * @param duration Toast 지속 시간 (기본: LENGTH_SHORT)
     */
    @JvmStatic
    fun show(context: Context, messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, messageResId, duration).show()
    }
    
    /**
     * Compose에서 사용할 수 있는 Toast 표시 함수
     * @param message 표시할 메시지
     * @param duration Toast 지속 시간 (기본: LENGTH_SHORT)
     */
    @Composable
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val context = LocalContext.current
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Compose에서 사용할 수 있는 Toast 표시 함수 (리소스 ID 사용)
     * @param messageResId 메시지 리소스 ID
     * @param duration Toast 지속 시간 (기본: LENGTH_SHORT)
     */
    @Composable
    fun showToast(messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
        val context = LocalContext.current
        Toast.makeText(context, messageResId, duration).show()
    }
}

