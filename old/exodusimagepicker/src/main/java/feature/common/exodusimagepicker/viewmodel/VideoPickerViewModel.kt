package feature.common.exodusimagepicker.viewmodel

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.VideoEditVpAdapter
import feature.common.exodusimagepicker.fragment.VideoThumbNailPickFragment.Companion.INITIAL_THUMB_FRAME_TIME
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.model.Thumbnail
import feature.common.exodusimagepicker.util.livedata.Event
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 비디오 피커 용 shareviewmodel 이다.
 *
 * @see
 * */
@UnstableApi
class VideoPickerViewModel : ViewModel() {

    // 비디오뷰  리사이즈 여부  -> default 는 그냥 비율에 맞게 해줌.
    var videoViewResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    // 비디오 미디어  파일 캐싱
    var videoMediaFile: FileModel? = null

    private val compositeDisposable = CompositeDisposable()

    val videoThumbnailList = mutableListOf<Thumbnail>()

    // 비디오 썸네일을 축출해서 라이브 데이터로  던져준다.
    private val _isGetVideoThumbnailFinish = MutableLiveData<Event<Boolean>>()
    val isGetVideoThumbnailFinish: LiveData<Event<Boolean>> = _isGetVideoThumbnailFinish

    // 비디오 인코딩시 에러 나면  보내줌.
    private val _videoEncodingError = MutableLiveData<Event<String>>()
    val videoEncodingError: LiveData<Event<String>> = _videoEncodingError

    var videoFileEntireDuration = 0

    // 다듬기의 경우 너무 많이 콜백이 불려
    // gaaction을 한번만 불러주기위해  boolean 체크함.
    var isVideoTrimGaActionChecked = false

    // trim한  첫번째 position
    var trimmedStartPosition = 0

    // trim한  마지막  position
    var trimmedLastPosition = 0

    var loadingDialog: Dialog? = null

    var videoPlayer: ExoPlayer? = null

    // 선택한 썸네일 뷰모델에 캐싱
    var selectedThumbnailBitmap: Bitmap? = null

    // 혹시나 편집화면에서  비디오 피커 화면으로 돌아올때를 데비하여,
    // reset 코드 추가함.
    fun resetTrimTime() {
        trimmedStartPosition = 0
        trimmedLastPosition = 0
    }

    // 로딩 스크린 세팅
    fun showLoadingDialog(context: Context, isCancelable: Boolean) {
        if (loadingDialog == null) {
            loadingDialog = Dialog(context).apply {
                setContentView(R.layout.loading_entire_picker_screen)
                setCancelable(isCancelable)
                window?.setBackgroundDrawable(ColorDrawable(0))
            }
        }
        loadingDialog!!.show()
    }

    // 플레이어 멈추고   clear 해준다.
    fun removePlayer() {
        videoPlayer?.clearVideoSurface()
        videoPlayer?.stop()
        videoPlayer?.release()
        videoPlayer = null
    }

    // 로딩 다이얼로그 없애
    fun removeLoadingDialog() {
        if (this.loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }

    // 비디오 편집용 뷰페이져 세팅
    @SuppressLint("ClickableViewAccessibility")
    fun setVideoEditVP(
        tabLayout: TabLayout,
        viewPager2: ViewPager2,
        fragment: FragmentActivity,
    ) {
        viewPager2.apply {
            this.adapter = VideoEditVpAdapter(fragment)
        }

        TabLayoutMediator(
            tabLayout,
            viewPager2,
        ) { _, position ->

            when (position) {
                VIDEO_EDIT -> {
                    // TODO:: 썸네일 다듬기 추가 되면 스트링 추가.
                }

                VIDEO_THUMB_NAIL -> {
                    // TODO:: 썸네일 다듬기 추가 되면 스트링 추가.
                }
            }
        }.attach()
    }

    // 선택한 비디오의 썸네일을 추출한다.
    private fun getThumbnailList(file: File) {
        // 새롭게 인코딩된 파일로 다시 videomediaFile을 구성해서 뷰모델ㅇ 캐싱한다.
        videoMediaFile = videoMediaFile?.contentUri?.let {
            FileModel(
                id = videoMediaFile?.id ?: 0,
                contentUri = it,
                name = videoMediaFile?.name ?: "",
                duration = videoMediaFile?.duration,
                mimeType = videoMediaFile?.mimeType ?: "",
                relativePath = file.absolutePath,
                isVideoFile = true,
                isSelected = true,
            )
        }

        Observable.just {
            // 비디오의 전체 duration 체크
            videoFileEntireDuration = videoMediaFile?.duration?.toInt() ?: 0
            trimmedLastPosition = videoFileEntireDuration

            // 전체 duration에서 일정 간격의  8개의 포지션을 받아서 리스트에 넣어준다.
            val videoThumbNailTimePositionList = try {
                (1..videoFileEntireDuration step (videoFileEntireDuration / (THUMBNAIL_COUNT - 1)))
            } catch (e: Exception) {
                arrayOf(0).toList()
            }

            val mediaMetaRetriever = MediaMetadataRetriever()
            mediaMetaRetriever.setDataSource(videoMediaFile?.relativePath)

            // 위 얻은 8개의 포지션으로  썸네일을 추출하고 썸네일 리스트에 넣어준다.
            videoThumbNailTimePositionList.forEach {
                videoThumbnailList.add(Thumbnail(bitmap = mediaMetaRetriever.getFrameAtTime(it.toLong() * 1000), positionUs = it.toLong()))
            }

            // 초기 뷰모델 썸네일 비트맵 캐싱
            selectedThumbnailBitmap = getThumbNailInVideoPosition(INITIAL_THUMB_FRAME_TIME)

            // 최종 썸네일 리스트 리턴
            videoThumbnailList
        }.subscribeOn(Schedulers.io())
            .map { it.invoke().toList() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                _isGetVideoThumbnailFinish.value = Event(true)
            }, {
                _isGetVideoThumbnailFinish.value = Event(false)
            }).addTo(compositeDisposable)
    }

    // 선택한  비디오 파일의 썸네일을 계산해준다.
    fun getThumbnailList(context: Context) {
        showLoadingDialog(context, false)
        getThumbnailList(File(videoMediaFile?.relativePath ?: return))
    }

    // 각 비디오 포지션별  썸네일 받기
    fun getThumbNailInVideoPosition(timeMills: Long): Bitmap? {
        return MediaMetadataRetriever().apply { setDataSource(videoMediaFile?.relativePath) }
            .getFrameAtTime(timeMills)
    }

    // 비디오 맥스 사이즈를 넘는지 여부를 체크한다.
    fun isOverMaxVideoDuration(): Boolean {
        return (videoMediaFile?.duration?.toInt() ?: 0) > TimeUnit.MINUTES.toMillis(MAX_VIDEO_MINUTE)
    }

    fun getVideoDuration(): Int {
        return (videoMediaFile?.duration?.toInt() ?: 0)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

    companion object {
        const val VIDEO_EDIT = 0 // 비디오 다듬기
        const val VIDEO_THUMB_NAIL = 1 // 비디오 썸네일

        const val MAX_VIDEO_MINUTE = 3L

        // 뽑아낼 썸네일  갯수
        const val THUMBNAIL_COUNT = 8
    }
}