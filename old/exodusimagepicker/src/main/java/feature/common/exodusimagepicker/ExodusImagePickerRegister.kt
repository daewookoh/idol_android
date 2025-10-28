package feature.common.exodusimagepicker

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.util.Const.PARAM_LOCALE
import feature.common.exodusimagepicker.util.Const.PARAM_PICKER_TYPE
import feature.common.exodusimagepicker.util.Const.PARAM_VIDEO_EDIT_MAX_DURATION
import java.util.Locale

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 외부 모듈을  실행할때  사용되는
 * exodus 이미지 피커  register 용  class 이다.
 *
 * @see registerForVideoPicker  비디오 피커 실행용.
 * */
class ExodusImagePickerRegister private constructor() {
    private lateinit var launcher: ActivityResultLauncher<Intent>

    companion object {

        // activity용
        fun registerForVideoPicker(
            activity: ComponentActivity,
            callback: (List<FileModel>?) -> Unit,
        ): ExodusImagePickerRegister {
            val exodusImagePickerRegister = ExodusImagePickerRegister()
            exodusImagePickerRegister.launcher =
                activity.registerForActivityResult(ExodusActivityVideoContract()) { resultVideoFileInfo ->
                    callback.invoke(resultVideoFileInfo)
                }
            return exodusImagePickerRegister
        }
    }

    // 미디어 피커  실행
    fun launchMediaPicker(context: Context, pickerType: Int, maxDuration: Int, locale: String) {
        launcher.launch(
            Intent(context, PickerActivity::class.java).apply {
                this.putExtra(PARAM_PICKER_TYPE, pickerType) // 피커 타입을 보내줌.
                this.putExtra(PARAM_VIDEO_EDIT_MAX_DURATION, maxDuration)
                this.putExtra(PARAM_LOCALE, locale)
            },
        )
    }
}