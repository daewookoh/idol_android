package feature.common.exodusimagepicker.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.viewmodel.PickerViewModel

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: picker activity 용 뷰모델  만들기 위한 뷰모델 팩토리
 *
 * */
class PickerViewModelFactory(
    private val pagingRepository: FilePagingRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PickerViewModel::class.java) -> {
                PickerViewModel(repository = pagingRepository) as T
            }
            else -> {
                throw Exception("cannot create viewModel")
            }
        }
    }
}