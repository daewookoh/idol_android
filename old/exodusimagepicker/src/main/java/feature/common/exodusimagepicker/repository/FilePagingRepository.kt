package feature.common.exodusimagepicker.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import feature.common.exodusimagepicker.enum.MediaFolderType
import feature.common.exodusimagepicker.model.FileModel

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: paging source 에  paging된 data요청용  repository
 *
 * @see
 * */
interface FilePagingRepository {
    fun getFiles(
        mediaFolderType: MediaFolderType,
        folderId: Long?,
    ): Flow<PagingData<FileModel>>
}