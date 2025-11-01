package net.ib.mn.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

/**
 * Haptic 피드백 유틸리티
 * Old 프로젝트의 Util.vibratePhone과 동일한 기능
 */
object HapticUtil {
    @SuppressLint("WrongConstant")
    fun vibrate(context: Context, isEnabled: Boolean = true) {
        if (!isEnabled) return

        // VIBRATE 권한 체크
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.VIBRATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (e: Exception) {
            // 권한이 없거나 다른 오류 발생 시 무시
            android.util.Log.w("HapticUtil", "Failed to vibrate: ${e.message}")
        }
    }
}

