package feature.common.exodusimagepicker.model

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 비디오  게시글 용 모델
 *
 * */
@Parcelize
data class VideoFileModel(
    val tempVideoFileUri: Uri, // 비디오 인코딩 완료된  파일 용 uri
    val tempVideoFileThumbnailBitmap: Bitmap? = null, // 임시적으로 비디오 썸네일 담을 비트맵
    val thumbnailImage: Uri? = null, // 비디오 썸네일 이미지 파일용  임시 uri
    var totalVideoDuration: Int = 0, // 비디오 전체 길이
) : Parcelable