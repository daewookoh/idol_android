package feature.common.exodusimagepicker.model

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 크랍한  이미지 파일용  모델
 *
 * @see croppedBitmap -> 이미지뷰에서 크롭된  비트맵을 받음
 * @see tempImageFileUri -> 크랍된 이미지뷰의 임시생성  uri를  받음.
 * */
@Parcelize
data class ImageFileModel(
    val croppedBitmap: Bitmap,
    val tempImageFileUri: Uri,
) : Parcelable