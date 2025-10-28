package net.ib.mn.utils.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.core.app.ActivityCompat
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity.Companion.REQUEST_WRITE_EXTERNAL_STORAGE
import net.ib.mn.utils.Util
import java.util.Arrays

/**
 * Created by parkboo on 2017. 6. 19..
 *
 * 방통위 접근권한 규정 처리용 헬퍼. android 6.0 미만/이상 동시 대응용.
 */
object PermissionHelper {
    /**
     * 퍼미션이 필요하다면 팝업으로 동의 요청하고 이미 동의했다면 onPermissionAllowed()를 호출한다.
     * @param _requiredPermissions 요청할 퍼미션 배열. 없으면 null
     * @param _optionalPermissions 요청할 퍼미션 배열. 없으면 null
     * @param msgs 각 퍼미션별로 사용자에게 설명할 문구 배열.
     * @param requestCode android 6.0에서 사용할 퍼미션 요청 코드
     */
    @JvmStatic
    fun requestPermissionIfNeeded(
        activity: Activity?,
        _requiredPermissions: Array<String>?,
        _optionalPermissions: Array<String>?,
        msgs: Array<String>,
        requestCode: Int,
        listener: PermissionListener,
        isSignup: Boolean? = false
    ) {
        if (activity == null) {
            return
        }

        // READ_MEDIA_IMAGES 권한 삭제로 api 29 이상이면 MediaStore를 사용하므로 저장소 권한 묻지않게 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            listener.onPermissionAllowed()
            return
        }

        val sb = StringBuilder()
        var requiredPermissions = arrayOf<String>()
        var optionalPermissions = arrayOf<String>()
        if (_requiredPermissions != null) {
            requiredPermissions = _requiredPermissions
        }
        if (_optionalPermissions != null) {
            optionalPermissions = _optionalPermissions
        }
        val permissions =
            Arrays.copyOf(requiredPermissions, requiredPermissions.size + optionalPermissions.size)
        System.arraycopy(
            optionalPermissions,
            0,
            permissions,
            requiredPermissions.size,
            optionalPermissions.size
        )
        var count = 0
        if (requiredPermissions.size > 0 && msgs.size > 0) {
            sb.append(activity.getString(R.string.permission_required))
            sb.append("\n\n")
            for (i in requiredPermissions.indices) {
                sb.append(msgs[count++])
                sb.append("\n")
            }
        }
        if (optionalPermissions.size > 0 && msgs.size > 0) {
            sb.append("\n")
            sb.append(activity.getString(R.string.permission_optional))
            sb.append("\n\n")
            for (i in optionalPermissions.indices) {
                if (msgs[count].isEmpty()) {
                    break
                }
                sb.append(msgs[count++])
                sb.append("\n")
            }
        }
        sb.append("\n")
        sb.append(activity.getString(if (BuildConfig.CELEB) R.string.actor_permission_desc else R.string.permission_desc))

        // 6.0 미만
        val _key = StringBuilder()
        for (p in permissions) {
            _key.append(p)
        }
        val key = _key.toString()

        // 6.0 이상
        var allGranted = true
        for (requiredPermission in requiredPermissions) {
            allGranted = allGranted && ActivityCompat.checkSelfPermission(
                activity,
                requiredPermission
            ) == PackageManager.PERMISSION_GRANTED
        }
        for (optionalPermission in optionalPermissions) {
            allGranted = allGranted && ActivityCompat.checkSelfPermission(
                activity,
                optionalPermission
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            listener.onPermissionAllowed()
            return
        }

        // msg 빈칸으로 오면 그냥 기본 퍼미션창 보여주기
        if (msgs.size == 0) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
            return
        }
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            null,
            sb.toString()
        ) { _ ->
            if(isSignup == true) {
                listener.requestPermission(permissions)
            } else {
                ActivityCompat.requestPermissions(activity, permissions, requestCode)
            }
            Util.closeIdolDialog()
        }
    }

    interface PermissionListener {
        fun onPermissionAllowed()
        fun onPermissionDenied()

        fun requestPermission(permissions: Array<String>)
    }
}
