package feature.common.exodusimagepicker.fragment

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.ThumbNailRvAdapter
import feature.common.exodusimagepicker.base.ImagePickerBaseFragment
import feature.common.exodusimagepicker.databinding.FragmentPickVideoThumbnailBinding
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.util.Util
import feature.common.exodusimagepicker.util.addWhiteBorderOnBitmap
import feature.common.exodusimagepicker.viewmodel.PickerViewModel
import feature.common.exodusimagepicker.viewmodel.VideoPickerViewModel

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 비디오 썸네일 선택 하는 화면
 * 현재는 사용 하지 않지만 나중을 위해서 넣어둠.
 *
 * @see
 * */
class VideoThumbNailPickFragment : ImagePickerBaseFragment<FragmentPickVideoThumbnailBinding>(R.layout.fragment_pick_video_thumbnail) {

    private val videoPickerViewModel: VideoPickerViewModel by activityViewModels()

    private lateinit var thumbnailRvAdapter: ThumbNailRvAdapter
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

    override fun FragmentPickVideoThumbnailBinding.onCreateView() {
        initSet()
    }

    private val oldImageView: Bitmap? = null

    // 초기 세팅
    private fun initSet() {
        thumbnailRvAdapter = ThumbNailRvAdapter()
        binding.rvVideoThumbnails.apply {
            adapter = thumbnailRvAdapter
        }

        thumbnailRvAdapter.submitList(videoPickerViewModel.videoThumbnailList)

        // seekbar의 전체 길이는  비디오의  전체 duration으로 잡음.
        binding.seekbar.max = videoPickerViewModel.videoFileEntireDuration

        // 초기 썸네일 세팅
        setInitialThumb(INITIAL_THUMB_FRAME_TIME)

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                Glide.with(requireActivity())
                    .asBitmap()
                    .load(getBlockBitmap(p1))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(object : CustomTarget<Bitmap?>() {
                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap?>?,
                        ) {
                            videoPickerViewModel.selectedThumbnailBitmap = resource

                            // seekbar와 위 썸네일 뷰에  현재 시점의  이미지를 보여준다.
                            binding.seekbar.thumb = BitmapDrawable(
                                requireActivity().resources,
                                Bitmap.createScaledBitmap(
                                    resource, Util.convertDpToPixel(requireActivity(), 30f).toInt(),
                                    Util.convertDpToPixel(requireActivity(), 52f).toInt(), true,
                                )?.addWhiteBorderOnBitmap(10, requireActivity()),
                            )

                            binding.ivThumbnail.setImageBitmap(resource)
                        }
                    })
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    // 8개 썸네일 블록 이미지를 가져옵니다.
    fun getBlockBitmap(seekBarBlockTime: Int): Bitmap? {
        var position = videoPickerViewModel.videoThumbnailList.indexOfLast {
            seekBarBlockTime >= it.positionUs
        }

        if (position == -1) {
            position = 0
        }

        assert(position >= 0 && position < videoPickerViewModel.videoThumbnailList.size) {
            "비디오 썸네일 position 범위 오류"
        }

        videoPickerViewModel.apply {
            return videoThumbnailList[position].bitmap
        }
    }

    // 초기 썸네일 frame을 적용해준다.
    private fun setInitialThumb(initialThumbFrameTime: Long) {
        // 디버그 모드용 썸네일 리스트 사이즈 체크.
        assert(videoPickerViewModel.videoThumbnailList.isNotEmpty()) {
            "썸네일 리스트가 비어있습니다."
        }

        // 저장된 썸네일 리스트의 첫번째 아이템 비트맵 넣어줌.
        binding.ivThumbnail.setImageBitmap(videoPickerViewModel.videoThumbnailList[0].bitmap)

        videoPickerViewModel.selectedThumbnailBitmap = videoPickerViewModel.videoThumbnailList[0].bitmap

        // seekbar와 위 썸네일 뷰에  현재 시점의  이미지를 보여준다.
        binding.seekbar.thumb = BitmapDrawable(
            requireActivity().resources,
            videoPickerViewModel.videoThumbnailList[0].bitmap
                ?.let {
                    Bitmap.createScaledBitmap(
                        it, Util.convertDpToPixel(requireActivity(), 30f).toInt(),
                        Util.convertDpToPixel(requireActivity(), 52f).toInt(), true,
                    )?.addWhiteBorderOnBitmap(10, requireActivity())
                },
        )
    }

    companion object {
        const val INITIAL_THUMB_FRAME_TIME = 500000L // timeUs기준 0.5초 지정
    }
}