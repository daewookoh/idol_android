package feature.common.exodusimagepicker.fragment

import android.annotation.SuppressLint
import android.net.Uri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.FilePagingAdapter
import feature.common.exodusimagepicker.adapter.itemdecoration.RvItemDecoration
import feature.common.exodusimagepicker.base.ImagePickerBaseFragment
import feature.common.exodusimagepicker.databinding.FragmentVideoFilePickerBinding
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.util.GlobalVariable
import feature.common.exodusimagepicker.viewmodel.PickerViewModel
import feature.common.exodusimagepicker.viewmodel.VideoPickerViewModel

/**
 * Create Date: 2023-12-04
 *
 * @author jungSangMin
 * Description: 비디오 리스트가 보여지고 선택한  비디오가  플레이되는 화면이다.
 * 비디오 피커 화면
 *
 * @see
 * */
@UnstableApi
class VideoFilePickerFragment :
    ImagePickerBaseFragment<FragmentVideoFilePickerBinding>(R.layout.fragment_video_file_picker),
    Player.Listener {

    private var videoPlayer: ExoPlayer? = null

    // exoplayer 용
    // 미디어 uri
    private lateinit var mediaUri: Uri
    private lateinit var bandwidthMeter: BandwidthMeter
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var renderersFactory: DefaultRenderersFactory
    private lateinit var extendMediaItem: MediaItem
    private lateinit var loadControl: LoadControl

    private val filePagingRepository: FilePagingRepository by lazy {
        FilePagingRepositoryImpl(requireActivity())
    }

    // picker ativity와  공유하는  sharedviewmodel
    private val pickerSharedViewModel: PickerViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PickerViewModel(filePagingRepository) as T
        }
    }

    // 비디오 피커  화면 전용  쉐어드 뷰모델
    private val videoPickerViewModel: VideoPickerViewModel by activityViewModels()

    // file 리스트 뿌리기용 리사이클러뷰 어뎁터
    lateinit var filePagingAdapter: FilePagingAdapter

    override fun FragmentVideoFilePickerBinding.onCreateView() {
        initSet()
        getDataFromViewModel()
        setListenerEvent()
    }

    override fun onResume() {
        super.onResume()
        resumeVideoPlayer()
    }

    override fun onPause() {
        super.onPause()
        removePlayer()
    }

    // 초기 세팅
    private fun initSet() {
        // trim 타임 리셋해줌.
        videoPickerViewModel.resetTrimTime()

        // 리사이클러뷰 어뎁터 설정
        filePagingAdapter = FilePagingAdapter()
        binding.rvVideoThumbnails.apply {
            adapter = filePagingAdapter
        }

        // 아이템 별 전 방향 간격 띄움 적용.
        binding.rvVideoThumbnails.addItemDecoration(RvItemDecoration())
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            // 전체 플레이어가 끝나면, 다시 처음으로 돌아가서 실행
            Player.STATE_ENDED -> {
                reStartPlayer()
            }
        }
    }

    // 플레이어를 처음부터 다시 시작한다.
    private fun reStartPlayer() {
        binding.exoPlayerView.player?.apply {
            this.seekTo(0) // 맨처음으로 돌아감.
            this.playWhenReady = true
        }
    }

    // 플레이어 멈추고   clear 해준다.
    private fun removePlayer() {
        videoPlayer?.clearVideoSurface()
        videoPlayer?.stop()
        videoPlayer?.release()
        videoPlayer?.removeListener(this)

        binding.exoPlayerView.player = null
        videoPlayer = null
    }

    // 리스너 이벤트 세팅
    private fun setListenerEvent() {
        // 맨처음 들어왔을때 처리
        lifecycleScope.launch {
            filePagingAdapter.onPagesUpdatedFlow.collectLatest {
                // 한번이라도 미디어 파일을 선택했다면 처음 들어오는 경우가 아니므로 그냥  return
                if (pickerSharedViewModel.isMediaFileSelectedOnce) {
                    return@collectLatest
                }

                // 미디어 비어 있으면 return 처리
                if (filePagingAdapter.snapshot().isEmpty()) {
                    return@collectLatest
                }

                // 맨처음  들어갔을때만 이므로  isMediaFileSelectedOnce fals일때만 동작
                if (pickerSharedViewModel.isMediaFileSelectedOnce) {
                    return@collectLatest
                }

                // 비디오 게시글이 안비어있을때만
                // 선택 처리할것이므로, true값으로 바꿔준다.
                pickerSharedViewModel.isMediaFileSelectedOnce = true

                filePagingAdapter.snapshot().items.onEach {
                    it.isSelected = false
                }
                filePagingAdapter.snapshot().items.first().isSelected = true

                // 비디오 피커는 싱글 픽커이므로 파일 선택시 리스트 비워주고, 다시 선택한다.
                GlobalVariable.selectedFileIds.clear()
                GlobalVariable.selectedFileIds.add(
                    0,
                    filePagingAdapter.snapshot().items.first().id,
                )
                filePagingAdapter.notifyDataSetChanged()

                // 선택한  미디어  path   뷰모델에 저장 해줌.
                pickerSharedViewModel.setPresentShownPath(
                    filePagingAdapter.snapshot().items.first().relativePath
                        ?: return@collectLatest,
                )

                // 파일 데이터 캐싱
                videoPickerViewModel.videoMediaFile = filePagingAdapter.snapshot().items.first()

                // 비디오 플레이어 세팅
                removePlayer()
                setVideoPlayer(
                    filePagingAdapter.snapshot().items.first().relativePath
                        ?: return@collectLatest,
                )
            }
        }

        // 리스트의 파일  클릭시 선택된 사진 이미지뷰에 추가
        filePagingAdapter.setItemClickListener(object : FilePagingAdapter.ItemClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onItemClickListener(
                relativePath: String,
                fileModel: FileModel,
                position: Int,
                isMultiPickerType: Boolean,
            ) {
                // 한번이라도 미디어 파일이 선택된것이므로  true값 적용
                pickerSharedViewModel.isMediaFileSelectedOnce = true

                // 리사이클러뷰 리스트 전부 선택해제해주고 밑에서 선택한 파일만 선택 true값 적용해준다.
                filePagingAdapter.snapshot().items.onEach {
                    it.isSelected = false
                }
                filePagingAdapter.snapshot().items.find { it.id == fileModel.id }?.isSelected = true

                // 비디오 피커는 싱글 픽커이므로 파일 선택시 리스트 비워주고, 다시 선택한다.
                GlobalVariable.selectedFileIds.clear()
                GlobalVariable.selectedFileIds.add(0, fileModel.id)
                filePagingAdapter.notifyDataSetChanged()

                // 선택한  미디어  path   뷰모델에 저장 해줌.
                pickerSharedViewModel.setPresentShownPath(relativePath)

                // 파일 데이터 캐싱
                videoPickerViewModel.videoMediaFile = fileModel

                // 비디오 플레이어 세팅
                removePlayer()
                setVideoPlayer(relativePath)
            }
        })
    }

    private fun resumeVideoPlayer() {
        if (videoPickerViewModel.videoMediaFile?.relativePath == null) {
            return
        }

        removePlayer()
        setVideoPlayer(videoPickerViewModel.videoMediaFile?.relativePath ?: return)
    }

    // 비디오 플레이어 세팅
    private fun setVideoPlayer(relativePath: String) {
        // 미디어 uri 적용
        mediaUri = Uri.parse(relativePath)

        // 엑소 플레이어 세팅
        bandwidthMeter = DefaultBandwidthMeter.Builder(requireActivity()).build()
        trackSelector = DefaultTrackSelector(requireActivity())
        renderersFactory = DefaultRenderersFactory(requireActivity())
        loadControl = DefaultLoadControl()

        extendMediaItem = MediaItem.Builder().apply {
            this.setUri(mediaUri).setMimeType(MimeTypes.BASE_TYPE_VIDEO)
        }.build()

        binding.exoPlayerView.resizeMode = videoPickerViewModel.videoViewResizeMode
        binding.exoPlayerView.useController = false

        videoPlayer = ExoPlayer.Builder(requireActivity(), renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()

        videoPlayer?.addListener(this)
        binding.exoPlayerView.player = videoPlayer
        videoPlayer?.setMediaItem(extendMediaItem)
        videoPlayer?.prepare()
        videoPlayer?.playWhenReady = true
    }

    // 뷰모델로부터 데이터 받아옴.
    @SuppressLint("NotifyDataSetChanged")
    private fun getDataFromViewModel() {
        // 파일리스트 받아오면 여기로 observe해서  리스트 업데이트
        pickerSharedViewModel.fileList.observe(
            viewLifecycleOwner,
            Observer {
                lifecycleScope.launch {
                    // 어뎁터 상태 업데이트
                    filePagingAdapter.submitData(it)
                }

                try {
                    // 리사이클러뷰 리스트 전부 선택해제해주고 밑에서 선택한 파일만 선택 true값 적용해준다.
                    filePagingAdapter.snapshot().items.onEach {
                        it.isSelected = false
                    }

                    filePagingAdapter.snapshot().items.find { it.id == GlobalVariable.selectedFileIds.first() }?.isSelected =
                        true
                    filePagingAdapter.notifyDataSetChanged()
                } catch (e: Exception) { // 권한 요청시는 데이터가 없어  collection 함수 돌다  exception 나는 경우가 있어서 이렇게 감싸줌.
                    e.printStackTrace()
                }
            },
        )
    }
}