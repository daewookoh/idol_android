package feature.common.exodusimagepicker.fragment

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import feature.common.exodusimagepicker.PickerActivity.Companion.KEY_SELECTED_FOLDER
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.adapter.FolderRvAdapter
import feature.common.exodusimagepicker.base.ImagePickerBaseFragment
import feature.common.exodusimagepicker.databinding.FragmentFolderListBinding
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.viewmodel.PickerViewModel

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 사용자의  폴더 리스트가 뿌려질  프래그먼트이다.
 * picker 엑티비티에서  상단 폴더 선택 버튼을 누르면 나오게 됨.
 *
 * @see
 * */
class FolderListFragment :
    ImagePickerBaseFragment<FragmentFolderListBinding>(R.layout.fragment_folder_list) {

    private val filePagingRepository: FilePagingRepository by lazy {
        FilePagingRepositoryImpl(requireActivity())
    }

    private lateinit var folderRvAdapter: FolderRvAdapter
    private lateinit var navController: NavController

    // picker ativity와  공유하는  sharedviewmodel
    private val pickerSharedViewModel: PickerViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PickerViewModel(filePagingRepository) as T
        }
    }

    override fun FragmentFolderListBinding.onCreateView() {
        initSet()
        setListenerEvent()
    }

    private fun initSet() {
        folderRvAdapter = FolderRvAdapter()
        binding.rvFolderList.apply {
            adapter = folderRvAdapter
        }
        folderRvAdapter.submitList(pickerSharedViewModel.mediaFolderList)

        navController = findNavController()
    }

    private fun setListenerEvent() {
        // 폴더 아이템 클릭시
        folderRvAdapter.setItemClickListener(object : FolderRvAdapter.ItemClickListener {
            override fun onItemClickListener(position: Int) {
                sendSelectedFolderInfo(position)
            }
        })
    }

    // 선택한 폴더의 리스트 인덱스를 넘겨서 pickeractivity에서 해당 폴더의 값을 가져오게 한다.
    // 값을 보내고, popBackStack 으로 다시 돌아감.
    private fun sendSelectedFolderInfo(folderIndex: Int) {
        navController.previousBackStackEntry?.savedStateHandle?.set(
            KEY_SELECTED_FOLDER,
            folderIndex,
        )
        pickerSharedViewModel.setFolderListShown(false) // 폴더는 닫음 처리해준다.
        navController.popBackStack()
    }
}