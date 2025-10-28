package net.ib.mn.model

import android.net.Uri
import android.os.Parcelable
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize

/**
 * Create Date: 2023/12/06
 *
 * @author jungSangMin
 * Description: 공통 파일 모델.
 *
 * */

@Parcelize
data class CommonFileModel(
    var id: Long, // 미디어 id
    @TypeConverters(TypeConverter::class) var contentUri: Uri?, // 미디어 uri
    val name: String,
    val duration: Long?, // 비디오일때 duration
    val mimeType: String,
    val relativePath: String?,
    val isVideoFile: Boolean = false, // 비디오 파일인지 여부
    var isSelected: Boolean = false, // 선택되었는지 여부
    @TypeConverters(TypeConverter::class) var thumbnailImage: Uri? = null, // 비디오 썸네일 이미지 파일용  임시 uri
    var startTimeMills: Int = 0,
    var endTimeMills: Int = 0,
    var totalVideoDuration: Int = 0, // 비디오 전체 길이
    var content: String? = null,
    var mediaType: String? = null,
    var trimFilePath: String? = null,
) : Parcelable