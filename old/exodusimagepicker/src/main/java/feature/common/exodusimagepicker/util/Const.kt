package feature.common.exodusimagepicker.util

object Const {
    const val SHARED_PREFERENCE_NAME = "exodus_image_picker_preference"
    const val PARAM_PICKER_TYPE = "picker_type"
    const val PARAM_IMAGE_BYTEARRAY = "image_byte_array"
    const val PARAM_VIDEO_URI = "video_uri"
    const val PARAM_VIDEO_THUMBNAIL_VISIBLE = "video_thumbnail_visible"
    const val PARAM_VIDEO_EDIT_MAX_DURATION = "video_edit_max_duration"
    const val PARAM_LOCALE = "locale"

    const val PICKER_RESULT_CODE_FOR_IMAGE = 101
    const val PICKER_RESULT_CODE_FOR_VIDEO = 102

    const val singleImagePickerType = 0 // 싱글 이미지 피커 타입
    const val bannerImagePickerType = 1 // 배너 이미지 피커 타입
    const val multiImagePickerType = 2 // 멀티 이미지 피커 타입
    const val videoPickerType = 3 // 비디오 피커 타입
    const val singleImageCropType = 4
}