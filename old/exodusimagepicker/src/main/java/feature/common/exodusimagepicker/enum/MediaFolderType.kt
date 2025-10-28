package feature.common.exodusimagepicker.enum

import android.net.Uri
import android.provider.MediaStore

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 이미지  비디오에 맞는  폴더 뽑기위한  MediaStore 값 enum class
 *
 * @see IMAGE_TYPE 이미지 타입일 경우 요청하는 미디어 store 값
 * @see VIDEO_TYPE 비디오 타입일 경우 요청하는 미디어 store 값
 * */
enum class MediaFolderType(
    val mediaUri: Uri,
    val bucketDisplayName: String,
    val bucketDisplayId: String,
    val data: String,
) {
    IMAGE_TYPE(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.DATA,
    ),
    VIDEO_TYPE(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.DATA,
    ),
}