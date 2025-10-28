package feature.common.exodusimagepicker

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.util.Const

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 비디오 피커 리턴용  constract
 *
 * @see
 * */
class ExodusActivityVideoContract : ActivityResultContract<Intent, List<FileModel>?>() {

    lateinit var context: Context

    // 이미지 파일 리스트
    private val videoFileModelList = mutableListOf<FileModel>()

    override fun createIntent(context: Context, input: Intent): Intent {
        this.context = context
        return input
    }
    override fun parseResult(resultCode: Int, intent: Intent?): List<FileModel>? { // 바이트 어레이로  리턴하는 값을  비트맵으로 다시 변환해서 리턴해준다.

        // 이미지 파일 리스트  혹시 남아있을수 있으므로, clear 를 해준다.
        videoFileModelList.clear()

        return try {
            val pickedVideoUri = intent?.getParcelableArrayListExtra<FileModel>(Const.PARAM_VIDEO_URI) as ArrayList<FileModel>
            pickedVideoUri.first().apply {
                this.totalVideoDuration = pickedVideoUri.first().totalVideoDuration
            }
            pickedVideoUri
        } catch (e: Exception) {
            null
        }
    }
}