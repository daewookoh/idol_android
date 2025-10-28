package feature.common.exodusimagepicker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 각 파일의  폴더정보 가져와서  구성할때 구성될 내용
 *
 * @see folderId 폴더별  식별 id
 * @see folderName 폴더의 이름
 * @see fileCount 각 폴더별 파일의 개수
 * @see folderThumbNail 각 폴더의 대표이미지
 * */
@Parcelize
data class FolderModel(
    val folderName: String, // 폴더 이름
    val folderId: Long?, // 폴더 아이디
    var fileCount: Int, // 폴더 수
    var folderThumbNail: String?, // 미디어 uri
) : Parcelable