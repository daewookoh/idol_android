package feature.common.exodusimagepicker.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.map
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.FilePagingAdapter
import feature.common.exodusimagepicker.adapter.FilePagingAdapter.Companion.MULTI_PICKER_TYPE
import feature.common.exodusimagepicker.adapter.FilePagingAdapter.Companion.SINGLE_PICKER_TYPE
import feature.common.exodusimagepicker.adapter.itemdecoration.RvItemDecoration
import feature.common.exodusimagepicker.base.ImagePickerBaseFragment
import feature.common.exodusimagepicker.databinding.FragmentMultiImgFilePickerBinding
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.util.Const
import feature.common.exodusimagepicker.util.GlobalVariable
import feature.common.exodusimagepicker.util.getBitmap
import feature.common.exodusimagepicker.util.saveImageCacheDirectory
import feature.common.exodusimagepicker.viewmodel.PickerViewModel
import feature.common.exodusimagepicker.viewmodel.PickerViewModel.Companion.DEFAULT_VALUE

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 멀티 이미지 피커 프래그먼트
 *
 * @see
 * */
class MultiImageFilePickerFragment : ImagePickerBaseFragment<FragmentMultiImgFilePickerBinding>(R.layout.fragment_multi_img_file_picker) {

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

    override fun FragmentMultiImgFilePickerBinding.onCreateView() {
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
        binding.ivInitialSelectedPhoto.maxScale = 6.0f

        // 포토뷰 -> 짧은 비율 기준으로 꽉차게 만들기  1:1 사각형에 꽉채워지게 만듬.
        binding.ivInitialSelectedPhoto.setMinimumScaleType(SCALE_TYPE_CENTER_CROP)

        // 아이템 별 전 방향 간격 띄움 적용.
        binding.rvPhotos.addItemDecoration(RvItemDecoration())

        // mediapath 선택했던게 있으면  혹시  다시 실행되는 경우에  적용해줌.
        if (!pickerSharedViewModel.presentShownMediaPath.isNullOrEmpty()) {
            binding.ivInitialSelectedPhoto.setImage(ImageSource.uri(pickerSharedViewModel.presentShownMediaPath ?: ""))
            binding.ivInitialSelectedPhoto.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        }
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
                    filePagingAdapter.submitData(it.map { it.copy() })
                }

                try {
                    // 싱글인 경우
                    if (pickerSharedViewModel.pickerMultiOrSingle == SINGLE_PICKER_TYPE) {
                        // 리사이클러뷰 리스트 전부 선택해제해주고 밑에서 선택한 파일만 선택 true값 적용해준다.
                        filePagingAdapter.snapshot().items.onEach {
                            it.isSelected = false
                        }

                        filePagingAdapter.snapshot().items.find { it.id == GlobalVariable.selectedFileIds.first() }?.isSelected = true
                        filePagingAdapter.notifyDataSetChanged()
                    } else { // 멀티인경우

                        // 리사이클러뷰 리스트 선택했었던 id들  다시한번 true값으로 세팅 해줌.
                        filePagingAdapter.snapshot().items.onEach { filemodel ->
                            if (GlobalVariable.selectedFileIds.any { it == filemodel.id }) {
                                // 이전에 선택했던 파일들이므로, 선택여부 true해줌.
                                filemodel.isSelected = true
                            }
                        }

                        // 피커 타입을 멀티 피커로 변경함.
                        filePagingAdapter.changePickerType(MULTI_PICKER_TYPE)

                        // 뷰모델에도 현재 타입 캐싱해줌.
                        pickerSharedViewModel.pickerMultiOrSingle = MULTI_PICKER_TYPE
                    }
                } catch (e: Exception) { // 권한 요청시는 데이터가 없어  collection 함수 돌다  exception 나는 경우가 있어서 이렇게 감싸줌.
                    e.printStackTrace()
                }
            },
        )

        // 엑티비티에  완료 버튼 클릭시
        pickerSharedViewModel.finishBtnClicked.observe(viewLifecycleOwner) { isBtnClicked ->
            if (isBtnClicked) { // 엑티비티에 있는 버튼 클릭 되었을 경우

                if (GlobalVariable.selectedFileIds.isEmpty()) {
                    // TODO: 2022/06/16 여기  이미지 피커 전용  string resource 적용 해야됨.
                    showSnackBar("한개이상 선택이 되어야 합니다.")
                    return@observe
                }

                val intent = Intent()

                val multiImageFileList = mutableListOf<FileModel>()

                try {
                    // 한번이라도  미디어값이 선택된경우,  미디어 값이  무조건 있으므로,  result로 bitmap 보냄.
                    if (pickerSharedViewModel.isMediaFileSelectedOnce) {
                        // 싱글 피커인 상황일때
                        if (pickerSharedViewModel.pickerMultiOrSingle == SINGLE_PICKER_TYPE) {
                            requireActivity().setResult(
                                Const.PICKER_RESULT_CODE_FOR_IMAGE,
                                intent,
                            )

                            val selectedFileIdsHash = GlobalVariable.selectedFileIds.toHashSet()
                            val selectedList = filePagingAdapter.snapshot().items.first {
                                selectedFileIdsHash.contains(it.id)
                            }

                            selectedList.apply {
                                binding.ivInitialSelectedPhoto.getBitmap()!!
                                    .saveImageCacheDirectory(requireActivity()).let {
                                        this.contentUri = it
                                    }
                            }.let {
                                multiImageFileList.add(
                                    it,
                                )
                            }

                            intent.putExtra(
                                Const.PARAM_IMAGE_BYTEARRAY,
                                multiImageFileList as ArrayList<FileModel>,
                            )
                        } else { // 멀티피커 상황일때

                            val selectedFileIdsHash = GlobalVariable.selectedFileIds.toHashSet()
                            val selectedList = filePagingAdapter.snapshot().items.filter {
                                selectedFileIdsHash.contains(it.id)
                            }

                            selectedList.forEach { fileModel ->
                                binding.flMultiImgContainer.children.first {
                                    fileModel.id.toInt() == it.id
                                }.getBitmap()!!.saveImageCacheDirectory(requireActivity()).let {
                                    fileModel.contentUri = it

                                    multiImageFileList.add(fileModel)

                                    intent.putExtra(
                                        Const.PARAM_IMAGE_BYTEARRAY,
                                        multiImageFileList as ArrayList<FileModel>,
                                    )

                                    requireActivity().setResult(
                                        Const.PICKER_RESULT_CODE_FOR_IMAGE,
                                        intent,
                                    )
                                }
                            }
                        }
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

                        binding.ivInitialSelectedPhoto.setImage(
                            ImageSource.uri(
                                filePagingAdapter.snapshot().first()?.relativePath
                                    ?: return@collectLatest,
                            ),
                        )

                        binding.ivInitialSelectedPhoto.orientation =
                            SubsamplingScaleImageView.ORIENTATION_USE_EXIF // 이미지 자동 회전 방지
                    }
                }
            }
        }

        // 싱글 피커로  타입 변환 버튼
        binding.ivTransferToSinglePicker.setOnClickListener {
            try {
                // 싱글피커 전환  아이콘 gone처리 및 멀티피커 전환 아이콘 보여줌.
                binding.ivTransferToSinglePicker.visibility = View.GONE
                binding.ivTransferToMultiPicker.visibility = View.VISIBLE

                // 싱글 피커용  selected photo 이미지뷰를 보여줌.
                binding.ivInitialSelectedPhoto.visibility = View.VISIBLE

                // 싱글 피커니까 기존 global 선택 id는  clear 시켜준다.
                GlobalVariable.selectedFileIds.clear()

                // 멀티피커에서 다시 돌아오는 상황임으로,  그동안 선택했던 애들을 다 날리고 다시  현재 선택된  이미지의 id를 넣어준다.
                if (pickerSharedViewModel.frontPickedImageId != DEFAULT_VALUE) {
                    GlobalVariable.selectedFileIds.add(
                        GlobalVariable.selectedFileIds.size,
                        pickerSharedViewModel.frontPickedImageId,
                    )
                }

                // 현재 맨앞에 있던  이미지가  선택된 이미지로 적용한다.
                filePagingAdapter.snapshot().items.onEach {
                    it.isSelected = it.id == pickerSharedViewModel.frontPickedImageId
                }

                // 해당하는 이미지를 넣어줌.
                binding.ivInitialSelectedPhoto.setImage(ImageSource.uri(filePagingAdapter.snapshot().items.find { it.id == pickerSharedViewModel.frontPickedImageId }?.relativePath ?: ""))
                binding.ivInitialSelectedPhoto.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF

                // 피커 타임을  싱글 이미지 피커로 변경함.
                filePagingAdapter.changePickerType(SINGLE_PICKER_TYPE)

                // 뷰모델에도 현재 타입 캐싱해줌.
                pickerSharedViewModel.pickerMultiOrSingle = SINGLE_PICKER_TYPE
                // 싱글 피커 전환이므로, 멀티 피커 container에 있던 뷰들은 전부 지워준다.
                binding.flMultiImgContainer.removeAllViews()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 멀티 피커 타입으로 변환 버튼
        binding.ivTransferToMultiPicker.setOnClickListener {
            try {
                // 멀티피커 전환 ga action 보냄.
//                pickerSharedViewModel.setFirebaseUIAction(GaAction.UPLOAD_IMAGE_MULTIPLE)

                // 실글피커 전환 아이콘은 보여주고, 멀티피커이므로 싱글피커용 이미지뷰 및  멀티피커 전환 아이콘은
                // 없애준다.
                binding.ivTransferToSinglePicker.visibility = View.VISIBLE
                binding.ivTransferToMultiPicker.visibility = View.GONE
                binding.ivInitialSelectedPhoto.visibility = View.GONE

                // 혹시  멀티피커 컨테이너에 남아있는 뷰가 있을수 있으므로, remove view를 해준다.
                binding.flMultiImgContainer.removeAllViews()

                // 선택한 파일 리스트도   clear 해줌.
                GlobalVariable.selectedFileIds.clear()

                // 멀티피커에서 선택하면 동적으로  추가되는 이미지뷰 이다.
                val multiPickerSelectedImageview = SubsamplingScaleImageView(requireActivity())

                // 최근 선택되었던 이미지뷰 id를 selectedfiles 리스트에  넣어줌.
                // 싱글 피커에서 전환시  현재 선택하고 있었던  이미지를 멀티피커 첫번째 선택 이미지로 셋팅 하기 위해서이다.
                // 망약에 default value이면 선택했던 이미지가 아예 없던 상황이므로, 그냥 넘어감.
                if (pickerSharedViewModel.frontPickedImageId != DEFAULT_VALUE) {
                    GlobalVariable.selectedFileIds.add(GlobalVariable.selectedFileIds.size, pickerSharedViewModel.frontPickedImageId)

                    // 위 추가된 이미지용  이미지뷰 동적 추가함.
                    binding.flMultiImgContainer.addView(
                        multiPickerSelectedImageview.apply {
                            maxScale = 6.0f

                            // 포토뷰 -> 짧은 비율 기준으로 꽉차게 만들기  1:1 사각형에 꽉채워지게 만듬.
                            setMinimumScaleType(SCALE_TYPE_CENTER_CROP)

                            setImage(ImageSource.uri(filePagingAdapter.snapshot().items.find { it.id == pickerSharedViewModel.frontPickedImageId }?.relativePath ?: ""))
                            this.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF // 이미지 자동 회전 방지
                            this.id = pickerSharedViewModel.frontPickedImageId.toInt()
                            setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.background_100))
                        },
                    )
                }

                // 피커 타입을 멀티 피커로 변경함.
                filePagingAdapter.changePickerType(MULTI_PICKER_TYPE)

                // 뷰모델에도 현재 타입 캐싱해줌.
                pickerSharedViewModel.pickerMultiOrSingle = MULTI_PICKER_TYPE
            } catch (e: Exception) {
                e.printStackTrace()
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
                try {
                    // 한번이라도 미디어 파일이 선택된것이므로  true값 적용
                    pickerSharedViewModel.isMediaFileSelectedOnce = true

                    // 멀티 필커 형태에서  게시글을 눌렀을때
                    if (isMultiPickerType) {
                        // 선택했던 이미지 id중에  현재 선택한 id가 있는 경우
                        if (GlobalVariable.selectedFileIds.any { it == fileModel.id }) {
                            // 바로 이전에 클릭한 이미지 id와 현재 클릭한 이미지 id가 같으면,
                            // 선택해제를 진행해야한다.
                            if (pickerSharedViewModel.presentClickedImageId == fileModel.id) {
                                // 해제는 1개 초과일때만 진행한다. 1개일때는  해제 못하게 해줌.
                                if (GlobalVariable.selectedFileIds.size > 1) {
                                    // 이전 클릭한 이미지 id 값은 다시 default 로 바꿔주고
                                    pickerSharedViewModel.presentClickedImageId = DEFAULT_VALUE

                                    // 선택 파일 목록에서  현재 파일 제거 해준다.
                                    GlobalVariable.selectedFileIds.remove(fileModel.id)
                                    binding.flMultiImgContainer.removeView(
                                        binding.flMultiImgContainer.findViewById(
                                            fileModel.id.toInt(),
                                        ),
                                    )

                                    // 그리고 가장 앞에  보이는 imageid를  선택한 이미지의 가장 마지막 포지션으로 주고,
                                    // 해당 마지막 포지션이 앞으로 나오게 해준다.
                                    pickerSharedViewModel.frontPickedImageId = GlobalVariable.selectedFileIds.last()
                                    (binding.flMultiImgContainer.findViewById(pickerSharedViewModel.frontPickedImageId.toInt()) as SubsamplingScaleImageView).bringToFront()
                                } else { // 1개일때는 그냥 선택 목록에서만 없애줌.

                                    // 이전 클릭한 이미지 id 값은 다시 default 로 바꿔주고
                                    pickerSharedViewModel.presentClickedImageId = DEFAULT_VALUE

                                    // 선택 파일 목록에서  현재 파일 제거 해준다.
                                    GlobalVariable.selectedFileIds.remove(fileModel.id)
                                }

                                filePagingAdapter.snapshot().items.find { it.id == fileModel.id }?.isSelected = false
                                filePagingAdapter.notifyDataSetChanged()
                                return // 취소작업 끝났으니까 return 시켜줌.
                            } else { // 최근에 누른게 현재 누른거랑 다를때 -> 이전에 선택한 리스트가 있는 경우라면,  맨앞으로 가지고온다.

                                pickerSharedViewModel.frontPickedImageId = fileModel.id
                                pickerSharedViewModel.presentClickedImageId = fileModel.id
                                if (GlobalVariable.selectedFileIds.size > 1) {
                                    (binding.flMultiImgContainer.findViewById(fileModel.id.toInt()) as SubsamplingScaleImageView).bringToFront()
                                }
                            }
                        } else { // 이전에 선택한 목록에 없을 경우 -> 새 이미지를 추가해준다.

                            val newImageView = SubsamplingScaleImageView(requireActivity())

                            if (GlobalVariable.selectedFileIds.size >= MAX_SELECTABLE_IMAGE_COUNT) {
                                // TODO: 2022/06/16 여기  이미지 피커 전용  string resource 적용 해야됨.
                                showSnackBar("이미지는 최대 10개까지 선택 가능합니다.")
                                return
                            }

// 선택한 파일 리스트에 현재 선택한 파일 id 추가 해줌.
                            GlobalVariable.selectedFileIds.add(GlobalVariable.selectedFileIds.size, fileModel.id)

                            // 이미지뷰 추가
                            binding.flMultiImgContainer.addView(
                                newImageView.apply {
                                    maxScale = 6.0f

                                    // 포토뷰 -> 짧은 비율 기준으로 꽉차게 만들기  1:1 사각형에 꽉채워지게 만듬.
                                    setMinimumScaleType(SCALE_TYPE_CENTER_CROP)

                                    setImage(ImageSource.uri(relativePath))
                                    this.id = fileModel.id.toInt()
                                    setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.background_100))
                                    this.orientation =
                                        SubsamplingScaleImageView.ORIENTATION_USE_EXIF // 이미지 자동 회전 방지
                                },
                            )

                            //
                            pickerSharedViewModel.presentClickedImageId = fileModel.id
                            pickerSharedViewModel.frontPickedImageId = fileModel.id
                        }

                        filePagingAdapter.snapshot().items.find { it.id == fileModel.id }?.isSelected =
                            true

                        binding.ivInitialSelectedPhoto.setImage(ImageSource.uri(relativePath))
                        binding.ivInitialSelectedPhoto.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF

                        filePagingAdapter.notifyDataSetChanged()
                    } else { // 싱글 피커 형태에서 클릭시

                        // 리사이클러뷰 리스트 전부 선택해제해주고 밑에서 선택한 파일만 선택 true값 적용해준다.
                        filePagingAdapter.snapshot().items.onEach {
                            it.isSelected = false
                        }

                        filePagingAdapter.snapshot().items.find { it.id == fileModel.id }?.isSelected =
                            true

                        // 싱글 피커이므로 파일 선택시 리스트 비워주고, 다시 선택한다.
                        GlobalVariable.selectedFileIds.clear()
                        GlobalVariable.selectedFileIds.add(0, fileModel.id)
                        filePagingAdapter.notifyDataSetChanged()

                        pickerSharedViewModel.frontPickedImageId = fileModel.id

                        // 선택한  미디어  path   뷰모델에 저장 해줌.
                        pickerSharedViewModel.setPresentShownPath(relativePath)

                        binding.ivInitialSelectedPhoto.setImage(ImageSource.uri(relativePath))
                        binding.ivInitialSelectedPhoto.orientation =
                            SubsamplingScaleImageView.ORIENTATION_USE_EXIF // 이미지 자동 회전 방지
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    companion object {
        const val MAX_SELECTABLE_IMAGE_COUNT = 10
    }
}