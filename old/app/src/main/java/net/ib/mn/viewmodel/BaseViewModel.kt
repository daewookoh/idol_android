package net.ib.mn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.GetIdolsByTypeAndCategoryUseCase
import net.ib.mn.domain.usecase.SaveIdolsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toDomain
import net.ib.mn.model.toPresentation
import javax.inject.Inject

@HiltViewModel
class BaseViewModel @Inject constructor(
    private val saveIdolsUseCase: SaveIdolsUseCase,
    private val getIdolsByTypeAndCategoryUseCase: GetIdolsByTypeAndCategoryUseCase
) : ViewModel() {

    private val _idols = MutableStateFlow<List<IdolModel>>(emptyList())
    val idols = _idols.asStateFlow()

    fun saveIdols(idols: ArrayList<IdolModel>) = viewModelScope.launch(Dispatchers.IO) {
        saveIdolsUseCase(idols.map { it.toDomain() }).first()
    }

    fun getIdolsByTypeAndCategory(type: String?, category: String?) =
        viewModelScope.launch(Dispatchers.IO) {
            val dbIdols = getIdolsByTypeAndCategoryUseCase(type, category)
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            _idols.emit(dbIdols ?: emptyList())
        }
}