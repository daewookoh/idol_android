package feature.common.exodusimagepicker.viewmodel

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import feature.common.exodusimagepicker.enum.MediaFolderType
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.model.FolderModel
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.util.Util
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 피커엑티비티 용 뷰모델
 *
 * @see mediaFolderType 미디어 폴더 타입 -> pagingsource에 미디어파일  요청할떄 어떤 타입일지 결정한다.
 * @see folderId 미디어 폴더의 식별자 null일 경우 모든 사진 or 비디오 폴더임.
 * @see folderIndex 폴더가 여러개있을떄 리스트화해서 어떤 index인지 체크
 * @see fileList pickeractivity 가  observe할  파일리스트 라이브 데이터
 * @see getMediaFiles pagingsource에 파일 요청
 * */
class PickerViewModel constructor(
    private val repository: FilePagingRepository,
) : ViewModel() {

    // 현재 zoom 상태
    var isZoom = false

    // 멀티 또는 싱글 피커인지  체크해준다.
    var pickerMultiOrSingle: Int = SINGLE_PICKER_TYPE

    // 현재 클릭을 한번 한 이미지의 아이디이다.
    var presentClickedImageId = DEFAULT_VALUE

    // 현재 최상단에 있는 이미지 아이디 이다.
    var frontPickedImageId = DEFAULT_VALUE

    // 한번이상 미디어 파일  선택 했는지 여부 체크  false이면  빈값으로 bitmap업시 줘야됨.
    // 안그러면  빈 이미지 화면은 bitmap으로 넘겨지게됨.
    var isMediaFileSelectedOnce = false

    // 현재 폴더리스트 보이는지 여부 체크
    var isFolderListShownCheck = false

    // 미디어 폴더 리스트
    var mediaFolderList = mutableListOf<FolderModel>()

    var mediaFolderType = MediaFolderType.VIDEO_TYPE

    var folderId: Long? = null // 폴더 아이디
    var folderIndex = 0 // 폴더리스트에서 현재 선택된 폴더 index

    // 폴더 리스트 boolean 변경에 따라 값 업데이트를 위한 livedata
    private val _isFolderListShown = MutableLiveData<Boolean>()
    val isFolderListShown: LiveData<Boolean> = _isFolderListShown

    private val _fileList = MutableLiveData<PagingData<FileModel>>()
    val fileList: LiveData<PagingData<FileModel>> = _fileList

    var presentShownMediaPath: String? = null

    // 완료 버튼  클릭 여부를   fragment로 알리기 위한  라이브 데이터
    private val _finishBtnClicked = MutableLiveData<Boolean>()
    val finishBtnClicked: LiveData<Boolean> = _finishBtnClicked

    lateinit var locale: String

    fun setLocale(context: Context?, localeString: String) {
        locale = localeString
    }

    fun setPresentShownPath(mediaPath: String) {
        presentShownMediaPath = mediaPath
    }

    // 완료 버튼 클릭시
    fun finishBtnClick() {
        _finishBtnClicked.value = true
    }

    // 폴더리스트 show 여부 체크
    fun setFolderListShown(isShown: Boolean) {
        isFolderListShownCheck = isShown
        _isFolderListShown.value = isShown
    }

    // paginSource에  미디어 파일 요청한다.
    fun getMediaFiles() {
        viewModelScope.launch {
            repository.getFiles(
                mediaFolderType = mediaFolderType,
                folderId = folderId,
            ).cachedIn(viewModelScope).collectLatest {
                _fileList.value = it
            }
        }
    }

    fun trimVideo(inputPath: String, outputPath: String, startMs: Int, endMs: Int) {

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(inputPath)

            val trackCount = extractor.trackCount
            var videoTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until trackCount) {
                format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: return
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    break
                }
            }

            if (videoTrackIndex >= 0) {
                extractor.selectTrack(videoTrackIndex)
                muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val outputTrackIndex = muxer.addTrack(format!!)
                muxer.start()

                extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                val buffer = ByteBuffer.allocate(maxInputSize)
                val info = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        break
                    }

                    val presentationTimeUs = extractor.sampleTime
                    if (presentationTimeUs > endMs * 1000) {
                        break
                    }

                    info.offset = 0
                    info.size = sampleSize
                    info.presentationTimeUs = presentationTimeUs
                    info.flags = 0

                    if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }

                    muxer.writeSampleData(outputTrackIndex, buffer, info)
                    extractor.advance()
                }

                muxer.stop()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } finally {
            muxer?.release()
            extractor.release()
        }
    }

    companion object {
        const val DEFAULT_VALUE = -1L

        const val SINGLE_PICKER_TYPE = 0
        const val MULTI_PICKER_TYPE = 1
    }
}