package feature.common.exodusimagepicker.model

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 파일을 가져올때  들어갈  내용 나열
 *
 * @see id 파일의 id
 * @see contentUri 파일의 contentUri
 * @see name 파일의 이름
 * @see duration 파일의 재생길이 - 비디오의 경우 해당
 * @see mimeType 파일의  mimetype
 * @see root 파일의 상대 경로
 * */

@Entity(
    tableName = "videoFileTable",
)
@Parcelize
data class FileModel(
    @PrimaryKey @ColumnInfo(name = "id") var id: Long, // 미디어 id
    @ColumnInfo(name = "contentUri") @TypeConverters(TypeConverter::class) var contentUri: Uri?, // 미디어 uri
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "duration") val duration: Long?, // 비디오일때 duration
    @ColumnInfo(name = "mimeType") val mimeType: String,
    @ColumnInfo(name = "relativePath") val relativePath: String?,
    @ColumnInfo(name = "isVideoFile") val isVideoFile: Boolean = false, // 비디오 파일인지 여부
    @ColumnInfo(name = "isSelected") var isSelected: Boolean = false, // 선택되었는지 여부
    @ColumnInfo(name = "thumbnailImage") @TypeConverters(TypeConverter::class) var thumbnailImage: Uri? = null, // 비디오 썸네일 이미지 파일용  임시 uri
    @ColumnInfo(name = "startTimeMills") var startTimeMills: Int = 0,
    @ColumnInfo(name = "endTimeMills") var endTimeMills: Int = 0,
    @ColumnInfo(name = "totalVideoDuration") var totalVideoDuration: Int = 0, // 비디오 전체 길이
    @ColumnInfo(name = "content") var content: String? = null,
    @ColumnInfo(name = "mediaType") var mediaType: String? = null,
    @ColumnInfo(name = "trimFilePath") var trimFilePath: String? = null,
) : Parcelable