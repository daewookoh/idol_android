package net.ib.mn.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.datastore.GetMenuInAppBannerPrefsUseCase
import net.ib.mn.model.InAppBannerModel
import net.ib.mn.model.toPresentation
import javax.inject.Inject

@HiltViewModel
class MyInfoViewModel @Inject constructor(
    private val getMenuInAppBannerPrefsUseCase: GetMenuInAppBannerPrefsUseCase
) : ViewModel() {

    private val _bannerList = MutableLiveData<List<InAppBannerModel>>()
    val bannerList: LiveData<List<InAppBannerModel>> get() = _bannerList

    init {
        getBannerList()
    }

    private fun getBannerList() = viewModelScope.launch(Dispatchers.IO) {
        val bannerList = getMenuInAppBannerPrefsUseCase()
            .mapListDataResource { it.toPresentation() }
            .awaitOrThrow()
        _bannerList.postValue(bannerList ?: emptyList())
    }
}