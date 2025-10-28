package feature.common.exodusimagepicker.util

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 버전 체크용
 *
 * @see isAndroid10Later 안드로이드 10 버전이상인지
 * @see isAndroid11Later 안드로이드 11 버전 이상인지
 * @see isAndroid13Later 안드로이드 13 버전 이상인지
 * */
object DeviceVersionUtil {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isAndroid11Later() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun isAndroid10Later() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun isAndroid13Later() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}