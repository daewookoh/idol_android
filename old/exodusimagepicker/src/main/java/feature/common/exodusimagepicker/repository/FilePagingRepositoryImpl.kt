package feature.common.exodusimagepicker.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import feature.common.exodusimagepicker.datasource.FilePagingSource
import feature.common.exodusimagepicker.enum.MediaFolderType
import feature.common.exodusimagepicker.model.FileModel

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: paging source 에  paging된 data요청용  repository 구현체
 *
 * */
class FilePagingRepositoryImpl(
    private val context: Context,
) : FilePagingRepository {
    override fun getFiles(
        mediaFolderType: MediaFolderType,
        folderId: Long?,
    ): Flow<PagingData<FileModel>> {
        return Pager(
            // initialLoadSize 지정 안해주면  pagesize *3 으로 지정해줌. ->그래서 맨처음 로드 할때는 90개 가져옴.
            config = PagingConfig(pageSize = 30),
            pagingSourceFactory = {
                FilePagingSource(
                    context = context,
                    folderId = folderId,
                    mediaFolderType = mediaFolderType,
                )
            },
        ).flow
    }
}