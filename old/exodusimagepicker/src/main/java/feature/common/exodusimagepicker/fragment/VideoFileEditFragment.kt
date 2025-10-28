package feature.common.exodusimagepicker.fragment

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.VideoEditVpAdapter
import feature.common.exodusimagepicker.base.ImagePickerBaseFragment
import feature.common.exodusimagepicker.databinding.FragmentVideoEditBinding
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.util.Const
import feature.common.exodusimagepicker.util.saveImageCacheDirectory
import feature.common.exodusimagepicker.viewmodel.PickerViewModel
import feature.common.exodusimagepicker.viewmodel.VideoPickerViewModel
import java.io.File

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description:  비디오  파일  편집 화면이다.
 * 여기서 뷰 페이져로  편집 화면 이랑  썸네일 화면 두개로 나눠 진다. (현재는 편집 화면만 사용중)
 *
 * @see
 * */
class VideoFileEditFragment : ImagePickerBaseFragment<FragmentVideoEditBinding>(R.layout.fragment_video_edit) {

    private val videoPickerViewModel: VideoPickerViewModel by activityViewModels()

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

    override fun FragmentVideoEditBinding.onCreateView() {
        initSet()
        getDataFromVm()
    }

    // 초기 세팅
    private fun initSet() {
        pickerSharedViewModel.setLocale(context, pickerSharedViewModel.locale)
        videoPickerViewModel.setVideoEditVP(
            tabLayout = binding.tabVideoEdit,
            viewPager2 = binding.vpVideoEdit,
            fragment = requireActivity(),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // 썸네일 리스트 clear 시켜주고,  편집관련 화면 모두 remove해줌.
        videoPickerViewModel.videoThumbnailList.clear()
        (binding.vpVideoEdit.adapter as VideoEditVpAdapter).removeFragment()
    }

    // 뷰모델 데이터 세팅
    private fun getDataFromVm() {
        // 엑티비티에  완료 버튼 클릭시
        pickerSharedViewModel.finishBtnClicked.observe(viewLifecycleOwner) { _ ->
            videoPickerViewModel.showLoadingDialog(activity ?: return@observe, false)

            // 시작  끝 trim 시간
            val startTimeMills = videoPickerViewModel.trimmedStartPosition * 1000
            val endTimeMills = videoPickerViewModel.trimmedLastPosition * 1000

            val trimFile = Uri.parse(videoPickerViewModel.videoMediaFile?.relativePath).lastPathSegment?.let {
                File(context?.cacheDir,
                    it
                )
            }

            pickerSharedViewModel.trimVideo(
                inputPath = videoPickerViewModel.videoMediaFile?.relativePath ?: return@observe,
                outputPath = trimFile?.absolutePath ?: return@observe,
                startMs = videoPickerViewModel.trimmedStartPosition,
                endMs = videoPickerViewModel.trimmedLastPosition
            )

            val intent = Intent()
            val videoFileList = mutableListOf<FileModel>()

            val thumbnailBitmap = videoPickerViewModel.selectedThumbnailBitmap

            try {
                // 한번이라도  미디어값이 선택된경우,  미디어 값이  무조건 있으므로,  result로 bitmap 보냄.
                if (pickerSharedViewModel.isMediaFileSelectedOnce) {
                    videoPickerViewModel.videoMediaFile?.apply {
                        thumbnailImage = thumbnailBitmap?.saveImageCacheDirectory(
                            requireActivity(),
                        )
                        this.startTimeMills = startTimeMills
                        this.endTimeMills = endTimeMills
                        this.trimFilePath = trimFile.absolutePath
                    }

                    videoFileList.add(videoPickerViewModel.videoMediaFile ?: return@observe)

                    intent.putExtra(
                        Const.PARAM_VIDEO_URI,
                        videoFileList as ArrayList<FileModel>,
                    )
                    requireActivity().setResult(
                        Const.PICKER_RESULT_CODE_FOR_VIDEO,
                        intent,
                    )
                } // null return 시켜서  !!를  사용하여,  excepion이  나오게 함.
            } catch (e: Exception) { // exception이 생긴 경우에는  그냥 finish 처리를 해준다.
                e.printStackTrace()
            }

            videoPickerViewModel.removeLoadingDialog()

            requireActivity().finish()
        }
    }
}