package feature.common.exodusimagepicker.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PointF
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.ORIENTATION_USE_EXIF
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.FilePagingAdapter
import feature.common.exodusimagepicker.adapter.itemdecoration.RvItemDecoration
import feature.common.exodusimagepicker.base.ImagePickerBaseFragment
import feature.common.exodusimagepicker.databinding.FragmentSingleImgFilePickerBinding
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.util.Const
import feature.common.exodusimagepicker.util.GlobalVariable
import feature.common.exodusimagepicker.util.getBitmap
import feature.common.exodusimagepicker.util.saveImageCacheDirectory
import feature.common.exodusimagepicker.viewmodel.PickerViewModel

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: picker activity에서 start destination으로 실행되는 fragment
 * 파일 리스트와 해당 파일을 눌렀을때  상세보기 화면으로 구성된다.
 *
 * @see
 * */
class SingleImgFilePickerFragment :
    ImagePickerBaseFragment<FragmentSingleImgFilePickerBinding>(R.layout.fragment_single_img_file_picker) {

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

    // file 리스트 뿌리기용 리사이클러뷰 어뎁터
    lateinit var filePagingAdapter: FilePagingAdapter

    override fun FragmentSingleImgFilePickerBinding.onCreateView() {
        initSet()
        getDataFromViewModel()
        setListenerEvent()
    }

    // 초기 세팅
    private fun initSet() {
        // 리사이클러뷰 어뎁터 설정
        filePagingAdapter = FilePagingAdapter()
        binding.rvPhotos.apply {
            adapter = filePagingAdapter
        }

        // 포토뷰의  최대 스케일 범위를 지정해줌.
        binding.ivSelectedPhoto.maxScale = 6.0f

        // 포토뷰 -> 짧은 비율 기준으로 꽉차게 만들기  1:1 사각형에 꽉채워지게 만듬.
        binding.ivSelectedPhoto.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP)

        // 아이템 별 전 방향 간격 띄움 적용.
        binding.rvPhotos.addItemDecoration(RvItemDecoration())

        // mediapath 선택했던게 있으면  혹시  다시 실행되는 경우에  적용해줌.
        if (!pickerSharedViewModel.presentShownMediaPath.isNullOrEmpty()) {
            binding.ivSelectedPhoto.setImage(ImageSource.uri(pickerSharedViewModel.presentShownMediaPath ?: ""))
        }

        binding.ivSelectedPhoto.setOnStateChangedListener(object :
            SubsamplingScaleImageView(requireActivity()),
            SubsamplingScaleImageView.OnStateChangedListener {
            override fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener?) {
                super.setOnStateChangedListener(onStateChangedListener)
//                    Logger.v("dasdasda ")
            }

            override fun onScaleChanged(newScale: Float, origin: Int) {
//                    Logger.v("sdads a->" + newScale)
//                    Logger.v("sdads a->" + origin)
//                    Logger.v("asdadsa " + binding.ivSelectedPhoto.sHeight)
//                    Logger.v("asdadsa " + binding.ivSelectedPhoto.sWidth)
            }

            override fun onCenterChanged(newCenter: PointF?, origin: Int) {
            }
        })
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

                    filePagingAdapter.snapshot().items.find { it.id == GlobalVariable.selectedFileIds.first() }?.isSelected = true
                    filePagingAdapter.notifyDataSetChanged()
                } catch (e: Exception) { // 권한 요청시는 데이터가 없어  collection 함수 돌다  exception 나는 경우가 있어서 이렇게 감싸줌.
                    e.printStackTrace()
                }
            },
        )

        // 폴더 리스트가 보였다가  다시  취소하면  선택했던  이미지 사라지는 현상 있어서  한번더  유지 시켜줌.
        pickerSharedViewModel.isFolderListShown.observe(viewLifecycleOwner, { isFolderListShown ->
            if (!isFolderListShown) {
                binding.ivSelectedPhoto.setImage(ImageSource.uri(pickerSharedViewModel.presentShownMediaPath ?: ""))
                binding.ivSelectedPhoto.orientation = ORIENTATION_USE_EXIF
            }
        })

        // 엑티비티에  완료 버튼 클릭시
        pickerSharedViewModel.finishBtnClicked.observe(viewLifecycleOwner) { isBtnClicked ->
            if (isBtnClicked) { // 엑티비티에 있는 버튼 클릭 되었을 경우
                val intent = Intent()

                val multiImageFileList = mutableListOf<FileModel>()

                try {
                    // 한번이라도  미디어값이 선택된경우,  미디어 값이  무조건 있으므로,  result로 bitmap 보냄.
                    if (pickerSharedViewModel.isMediaFileSelectedOnce) {
                        // null return 시켜서  !!를  사용하여,  excepion이  나오게 함.

                        val selectedFileIdsHash = GlobalVariable.selectedFileIds.toHashSet()
                        val selectedList = filePagingAdapter.snapshot().items.find {
                            selectedFileIdsHash.contains(it.id)
                        }

                        selectedList.apply {
                            binding.ivSelectedPhoto.getBitmap()!!
                                .saveImageCacheDirectory(requireActivity()).let {
                                    this?.contentUri = it
                                }
                        }?.let {
                            multiImageFileList.add(
                                it,
                            )
                        }

                        intent.putExtra(
                            Const.PARAM_IMAGE_BYTEARRAY,
                            multiImageFileList as ArrayList<FileModel>,
                        )
                        requireActivity().setResult(
                            Const.PICKER_RESULT_CODE_FOR_IMAGE,
                            intent,
                        )
                    }
                } catch (e: Exception) { // exception이 생긴 경우에는  그냥 finish 처리를 해준다.
                    showToast("이미지 피커에 문제가 생겼습니다. ")
                }

                requireActivity().finish()
            }
        }
    }

    // 리스너 이벤트 모음
    @SuppressLint("ClickableViewAccessibility")
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

                // 사진 게시글이 안비어있을때만
                if (!filePagingAdapter.snapshot().isEmpty()) {
                    // 맨처음  들어갔을때만 이므로  isMediaFileSelectedOnce fals일때만 동작
                    if (!pickerSharedViewModel.isMediaFileSelectedOnce) {
                        // 선택 처리할것이므로, true값으로 바꿔준다.
                        pickerSharedViewModel.isMediaFileSelectedOnce = true

                        // 가장 첫번째  파일이 선택됨으로, 해당 파일 선택 됨으로 변경
                        filePagingAdapter.snapshot().first()?.isSelected = true

                        // 싱글 피커이므로 파일 선택시 리스트 비워주고, 다시 선택한다.
                        GlobalVariable.selectedFileIds.clear()
                        GlobalVariable.selectedFileIds.add(
                            0,
                            filePagingAdapter.snapshot().first()?.id ?: return@collectLatest,
                        )

                        filePagingAdapter.notifyDataSetChanged()

                        pickerSharedViewModel.frontPickedImageId =
                            filePagingAdapter.snapshot().first()?.id ?: return@collectLatest

                        // 선택한  미디어  path   뷰모델에 저장 해줌.
                        pickerSharedViewModel.setPresentShownPath(
                            filePagingAdapter.snapshot().first()?.relativePath
                                ?: return@collectLatest,
                        )

                        binding.ivSelectedPhoto.setImage(
                            ImageSource.uri(
                                filePagingAdapter.snapshot().first()?.relativePath
                                    ?: return@collectLatest,
                            ),
                        )

                        binding.ivSelectedPhoto.orientation =
                            SubsamplingScaleImageView.ORIENTATION_USE_EXIF // 이미지 자동 회전 방지
                    }
                }
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

                // 싱글 피커이므로 파일 선택시 리스트 비워주고, 다시 선택한다.
                GlobalVariable.selectedFileIds.clear()
                GlobalVariable.selectedFileIds.add(0, fileModel.id)
                filePagingAdapter.notifyDataSetChanged()

                // 선택한  미디어  path   뷰모델에 저장 해줌.
                pickerSharedViewModel.setPresentShownPath(relativePath)

                binding.ivSelectedPhoto.setImage(ImageSource.uri(relativePath))
                binding.ivSelectedPhoto.orientation = ORIENTATION_USE_EXIF // 이미지 자동 회전 방지
            }
        })
    }
}