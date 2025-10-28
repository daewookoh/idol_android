package feature.common.exodusimagepicker.fragment

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.ThumbNailRvAdapter
import feature.common.exodusimagepicker.base.ImagePickerBaseFragment
import feature.common.exodusimagepicker.databinding.FragmentTrimVideoBinding
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.util.Const
import feature.common.exodusimagepicker.util.CustomRangeSlider
import feature.common.exodusimagepicker.viewmodel.PickerViewModel
import feature.common.exodusimagepicker.viewmodel.VideoPickerViewModel
import java.util.concurrent.TimeUnit

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 비디오 다듬기 화면
 *
 * @see
 * */

@UnstableApi
class TrimVideoFragment :
    ImagePickerBaseFragment<FragmentTrimVideoBinding>(R.layout.fragment_trim_video),
    Player.Listener {

    private val videoPickerViewModel: VideoPickerViewModel by activityViewModels()

    private val filePagingRepository: FilePagingRepository by lazy {
        FilePagingRepositoryImpl(requireActivity())
    }

    private lateinit var thumbnailRvAdapter: ThumbNailRvAdapter

    var videoPlayer: ExoPlayer? = null

    private var maxDurationMills : Int = 60000

    // picker ativity와  공유하는  sharedviewmodel
    private val pickerSharedViewModel: PickerViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PickerViewModel(filePagingRepository) as T
        }
    }

    override fun FragmentTrimVideoBinding.onCreateView() {
        initSet()
        setListenerEvent()
        getDataFromVm()
    }

    // 초기 세팅
    private fun initSet() {
        // 비디오 플레이어 세팅 하고 exoplayer 뷰에 연동
        videoPlayer = ExoPlayer.Builder(requireActivity()).build()
        videoPickerViewModel.videoPlayer = videoPlayer

        videoPlayer?.addListener(this)
        binding.exoPlayerView.player = videoPlayer

        thumbnailRvAdapter = ThumbNailRvAdapter()
        binding.rvVideoThumbnails.apply {
            adapter = thumbnailRvAdapter
        }

        binding.rangeSlider.setTickCount(videoPickerViewModel.videoFileEntireDuration)

        val maxDuration = arguments?.getInt(Const.PARAM_VIDEO_EDIT_MAX_DURATION) ?: 60
        maxDurationMills = TimeUnit.SECONDS.toMillis(maxDuration.toLong()).toInt()

        binding.rangeSlider.setMaxDifference(maxDurationMills)

        // max diff 이상 이하의  duration 일 경우에는  range index를  duration으로 잡아줌
        if (videoPickerViewModel.videoFileEntireDuration <= maxDurationMills) {
            // 처음 range 지정
            binding.rangeSlider.setRangeIndex(0, videoPickerViewModel.videoFileEntireDuration)
            videoPickerViewModel.apply {
                trimmedStartPosition = 0
                trimmedLastPosition = videoPickerViewModel.videoFileEntireDuration
            }
        } else { // max diff보다 큰 경우에는  max diff를 right index넣어주어  range를  max diff에 맞게 지정해준다.
            // 처음 range 지정
            binding.rangeSlider.setRangeIndex(0, maxDurationMills)
            videoPickerViewModel.apply {
                trimmedStartPosition = 0
                trimmedLastPosition = maxDurationMills
            }
        }

        // 비디오 플레이어 세팅
        setVideoPlayer(videoPickerViewModel.videoMediaFile?.relativePath ?: return)

        thumbnailRvAdapter.submitList(videoPickerViewModel.videoThumbnailList)
    }

    override fun onResume() {
        super.onResume()
        resumeVideoPlayer()
    }

    override fun onPause() {
        super.onPause()
        removePlayer()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            // 전체 플레이어가 끝나면, 다시 처음으로 돌아가서 실행
            Player.STATE_ENDED -> {
                reStartPlayer()
            }
        }
    }

    private fun resumeVideoPlayer() {
        if (videoPickerViewModel.videoMediaFile?.relativePath == null) {
            return
        }

        // 혹시나 video player null 이면 다시 세팅
        if (videoPlayer == null) {
            videoPlayer = ExoPlayer.Builder(requireActivity()).build()
            videoPlayer?.addListener(this)
            binding.exoPlayerView.player = videoPlayer
        }

        videoPlayer?.setMediaItem(
            MediaItem.fromUri(
                videoPickerViewModel.videoMediaFile?.relativePath
                    ?: "",
            ),
        )
        binding.exoPlayerView.resizeMode = videoPickerViewModel.videoViewResizeMode

        // 시작 포지션이 바뀌었으므로 trimmedStartPosition으로 이동해서 실행
        videoPlayer?.seekTo(videoPickerViewModel.trimmedStartPosition.toLong())

        videoPlayer?.prepare()
        videoPlayer?.playWhenReady = true
    }

    // 플레이어를 처음부터 다시 시작한다.
    private fun reStartPlayer() {
        binding.exoPlayerView.player?.apply {
            this.seekTo(videoPickerViewModel.trimmedStartPosition.toLong()) // 맨처음으로 돌아감.
            this.playWhenReady = true
        }
    }

    // 비디오 플레이어 세팅
    private fun setVideoPlayer(relativePath: String) {
        videoPlayer?.setMediaItem(MediaItem.fromUri(relativePath))
        binding.exoPlayerView.resizeMode = videoPickerViewModel.videoViewResizeMode
        videoPlayer?.prepare()
        videoPlayer?.playWhenReady = true

        // 세팅 끝났으니까  timer seekbar 실행
        showPlayTimerSeekThumb()
    }

    // 플레이어 멈추고   clear 해준다.
    private fun removePlayer() {
        videoPlayer?.clearVideoSurface()
        videoPlayer?.stop()
        videoPlayer?.release()
        videoPlayer = null
    }

    // 리스너 이벤트 세팅
    private fun setListenerEvent() {
        binding.seekbar.setOnTouchListener { v, event -> true }

        // range 조정
        binding.rangeSlider.setRangeChangeListener( object: CustomRangeSlider.OnRangeChangeListener {
            override fun onRangeChange(p0: CustomRangeSlider, leftIndex: Int, rightIndex: Int) {
                // 비디오 다듬기 ga action 적용안되어있다면,
                // range 조정 감지되면 true값 바ㅏ꿔주고, ga action을 보낸다.-한번만 보내주기위해서
                if (!videoPickerViewModel.isVideoTrimGaActionChecked) {
                    videoPickerViewModel.isVideoTrimGaActionChecked = true
                    //                videoPickerViewModel.setFirebaseUIAction(GaAction.UPLOAD_VIDEO_TRIM)
                }

                videoPickerViewModel.apply {
                    trimmedStartPosition = leftIndex
                    trimmedLastPosition = rightIndex
                    val currentDuration = trimmedLastPosition - trimmedStartPosition

                    if (currentDuration > maxDurationMills) {
                        val diff = currentDuration - maxDurationMills
                        trimmedLastPosition -= diff
                    }
                }

                // 시작 포지션이 바뀌었으므로 trimmedStartPosition으로 이동해서 실행
                videoPlayer?.seekTo(leftIndex.toLong())
            }
        })
    }

    // 비디오 플레이어의 진행 사항 seekbar thumb를 보여준다.
    private fun showPlayTimerSeekThumb() {
        binding.seekbar.max = videoPickerViewModel.videoFileEntireDuration

        // 1초마다 플레이 전체 duration 에서 현재 위치를 빼서 남은 시간을 계산해 보여준다.
        Observable.interval(1, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // 현재 progress보다 trim된 마지막 포지션이 적으면,  안되므로, 다시  이경우가 되면 다시 restart 한다.
                if (binding.seekbar.progress >= (videoPickerViewModel.trimmedLastPosition ?: 0)) {
                    reStartPlayer()
                }

                binding.seekbar.progress = videoPlayer?.currentPosition?.toInt() ?: 0
            }, {}).addTo(compositeDisposable)
    }

    // 뷰모델로부터 데이터 받아옴
    private fun getDataFromVm() {
    }
}