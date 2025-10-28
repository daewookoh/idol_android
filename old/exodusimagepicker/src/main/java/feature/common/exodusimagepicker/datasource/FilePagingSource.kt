package feature.common.exodusimagepicker.datasource

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.paging.PagingSource
import androidx.paging.PagingState
import feature.common.exodusimagepicker.enum.MediaFolderType
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.util.GlobalVariable
import kotlin.math.max

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 각 폴더별  미디어 파일 리스트를   뽑아서 페이징 처리를 한다.
 * jetpack library paging3을 이용하여  페이징 처리 적용 함.
 *
 * */
class FilePagingSource(
    context: Context,
    folderId: Long? = null, // 모든 사진 or 비디오 폴더의 경우  id가 null로 적용되야해서 nullable처리
    private val mediaFolderType: MediaFolderType, // 이미지인지 비디오인지 여부 체크
) : PagingSource<Int, FileModel>() {

    private val cursor: Cursor?

    // 8.0.0 버전대에서 duration 관련  이미지 피커 사용시 문제가 있어
    // 비디오 피커용 이미지 피커용으로 나눔.
    // 비디오 용
    private val projectionForVideo = arrayListOf(
        MediaStore.Files.FileColumns._ID, // 미디어 데이터 id
        MediaStore.Files.FileColumns.DISPLAY_NAME, // 미디어 데이터 이름
        MediaStore.Files.FileColumns.MIME_TYPE, // 미디어 mimtype
        MediaStore.Files.FileColumns.DATE_ADDED, // 추가된 날짜
        MediaStore.Files.FileColumns.DATE_MODIFIED, // 수정된 날짜
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Video.VideoColumns.DURATION, // 비디오 파일용 duration
    ).toTypedArray()

    private val projectionForImage = arrayListOf(
        MediaStore.Files.FileColumns._ID, // 미디어 데이터 id
        MediaStore.Files.FileColumns.DISPLAY_NAME, // 미디어 데이터 이름
        MediaStore.Files.FileColumns.MIME_TYPE, // 미디어 mimtype
        MediaStore.Files.FileColumns.DATE_ADDED, // 추가된 날짜
        MediaStore.Files.FileColumns.DATE_MODIFIED, // 수정된 날짜
        MediaStore.Files.FileColumns.DATA,
    ).toTypedArray()

    // PagingSource 불리자마자 우선  이미지 가져오는 처리를 진행해야된다.
    init {

        // 만약에  datasource 가 잘못등록된 경우  callback 옴 이때는  cursor 동작 close 시킨다.
        registerInvalidatedCallback {
            closeCursor()
        }

        // TODO: 2022/02/22 우리 앱은  gif를 안처리하므로, 모든  파일을 체크할때 gif는 배제한다.  -> 나중에 추가시 아래 두개 변수는  null로 바꿔줘야됨
        var selection: String? = "${MediaStore.Files.FileColumns.MIME_TYPE}=?"
        var selectionArgs: Array<String>? =
            arrayOf(MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp4").toString())
        val sortType =
            MediaStore.MediaColumns.DATE_MODIFIED + " DESC" // 파일이 가장 최근 수정된 시간으로 기준으로 내림 차순

        // TODO: 2022/02/20 나중에 mimetype이  좀더 세분화 되는 경우 아래 참고
        // 현재는  media content uri로 이미지 비디오 타입이 나눠서
        // 상관 없지만,  media type 및 특정 mimeType을 쿼리로 날릴수 있음.
        // MediaStore의 MEDIA_TYPE=?
        // MIME_TYPE

        folderId?.let { // null 이 아닌 경우 null이면 그냥 지나가니까  위 null 값 그대로 가짐.
            selection =
                "(${mediaFolderType.bucketDisplayId} = ?) AND (${MediaStore.Files.FileColumns.MIME_TYPE} = ?)"
            selectionArgs = arrayOf(
                folderId.toString(),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp4").toString(),
            )
        }

        // 위정보에대한 쿼리를 날릴 cursor
        cursor = context.contentResolver.query(
            mediaFolderType.mediaUri, // mediauri 에서 이미  이미지 타입인지 비디오 타입인지 체크가 됨.
            if (mediaFolderType == MediaFolderType.VIDEO_TYPE) {
                projectionForVideo
            } else {
                projectionForImage
            },
            selection,
            selectionArgs,
            sortType,
        )
    }

    // adapter refresh이거나  중간에 페이징 무효화가 되었을때  다시 호출된다  -> 다시 시작할 key(페이지)를 반환하게 해줌
    override fun getRefreshKey(state: PagingState<Int, FileModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(30) ?: anchorPage?.nextKey?.minus(30)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FileModel> {
        try {
            if (cursor == null) { // cursor null 일때  error 던짐
                return LoadResult.Error(Exception("Cursor is null"))
            }

            var page = params.key // 다음 페이지

            if (page == null) { // 맨처음일때는  null이옴.
                page = 0 // Null일 경우 페이지 1을 추가 해줌.
            }

            // page는  그다음부터  loadsize를 더한 만큼 추가가된다.
            val prevPage: Int? = if (page == 0) { // page가 1이면  맨처음이니까 => 이전 페이지는 null 값
                null
            } else { // 그다음부터는  현재 페이지에서 로드된 페이지를 빼주면됨 그게 30이니까  ,혹시 -로 나오면  0으로  해줌
                max(page - params.loadSize, 0)
            }

            // 그다음 curosr count로 미디어 전체 카운트를 체크하고 현재 page랑 같다는 것은 마지막 까지 온것이므로 null 처리를 해준다.
            val nextPage: Int? = if (cursor.count == page) {
                null
            } else { // 페이지 + 로드 사이즈가 cursor count보다 크면  더이상  가져올께 없는 거니까   null 처리
                if (page + params.loadSize >= cursor.count) {
                    null
                } else { // 그외에는  nextpage 처리에 -> 현재 page+ 앞으로 load할 size를  더해서  nextpage로 지정한다.
                    page + params.loadSize
                }
            }

            return LoadResult.Page(
                data = getMediaFiles(page = page, nextPage = nextPage, loadSize = params.loadSize),
                prevKey = prevPage,
                nextKey = nextPage,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return LoadResult.Error(e)
        }
    }

    // cursor를  close 시켜준다.
    private fun closeCursor() {
        if (cursor?.isClosed == false) {
            cursor.close()
        }
    }

    private fun getMediaFiles(page: Int, nextPage: Int?, loadSize: Int): List<FileModel> {
        val mediaList = ArrayList<FileModel>() // media list에 담김

        try {
            cursor?.let { cursor -> // cursor null이 아닐 경우
                repeat(loadSize) { index ->
                    if (cursor.moveToPosition(page + index)) {
                        if (cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                                .endsWith("mp4", ignoreCase = true)
                        ) {
                            mediaList.add(
                                FileModel(
                                    contentUri = mediaFolderType.mediaUri,
                                    id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)),
                                    name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)),
                                    mimeType = cursor.getString(
                                        cursor.getColumnIndexOrThrow(
                                            MediaStore.Files.FileColumns.MIME_TYPE
                                        )
                                    ),
                                    duration = cursor.getLongOrNull(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)),
                                    isVideoFile = (mediaFolderType == MediaFolderType.VIDEO_TYPE), // 비디오 타입일때는 true 적용
                                    relativePath = cursor.getStringOrNull(
                                        cursor.getColumnIndexOrThrow(
                                            MediaStore.MediaColumns.DATA,
                                        ),
                                    ),
                                ),
                            )
                        }
                    } else {
                        return@repeat
                    }
                }

                // medialist return할때  선택한 파일 아이디 리스트에  medialist가 있다면, 선택 true값을 적용해줌.
                mediaList.onEach { mediaFile ->
                    if (GlobalVariable.selectedFileIds.any { it == mediaFile.id }) {
                        mediaFile.isSelected = true
                    }
                }
            }
        } catch (e: Exception) { // exception이 나면  empty 리스트를 내보낸다.
            return emptyList()
        }

        return mediaList
    }
}