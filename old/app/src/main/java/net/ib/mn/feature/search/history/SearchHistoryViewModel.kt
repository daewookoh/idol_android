package net.ib.mn.feature.search.history

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.repository.SearchRepositoryImpl
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.datastore.GetSearchInAppBannerPrefsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.InAppBannerModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.livedata.Event
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: 검색 기록 화면 (핫 트렌드) 뷰모델
 *
 * */

@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val searchRepository: SearchRepositoryImpl,
    private val idolsRepository: IdolsRepository,
    private val getSearchInAppBannerPrefsUseCase: GetSearchInAppBannerPrefsUseCase
) : BaseViewModel() {

    private val _bannerList = MutableLiveData<Event<List<InAppBannerModel>>>()
    val bannerList: LiveData<Event<List<InAppBannerModel>>> get() = _bannerList
    private val _navigateCommunity = MutableLiveData<Event<IdolModel>>()
    val navigateCommunity: LiveData<Event<IdolModel>> get() = _navigateCommunity
    private val _navigateAppLink = MutableLiveData<Event<String>>()
    val navigateAppLink: LiveData<Event<String>> get() = _navigateAppLink
    private val _searchTrends = MutableLiveData<Event<JSONArray>>()
    val searchTrends: LiveData<Event<JSONArray>> get() = _searchTrends
    private val _searchSuggests = MutableLiveData<Event<JSONArray>>()
    val searchSuggests: LiveData<Event<JSONArray>> get() = _searchSuggests

    fun init(context: Context) {
        getBannerList(context)
        searchTrends()
    }

    private fun getBannerList(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val bannerList = getSearchInAppBannerPrefsUseCase()
            .mapListDataResource { it.toPresentation() }
            .awaitOrThrow()
        _bannerList.postValue(Event(bannerList ?: emptyList()))
    }

    fun clickBanner(context: Context, inAppBannerModel: InAppBannerModel) {
        val bannerLink = inAppBannerModel.link ?: return
        if (bannerLink.startsWith("idol")) {
            val link = bannerLink.split(":")
            val idolId = link[1].toInt()
            viewModelScope.launch {
                idolsRepository.getIdolsForSearch(
                    id = idolId,
                    listener = { response ->
                        try {
                            val idol = IdolGson.getInstance().fromJson(
                                response.getJSONArray("objects").getJSONObject(0)
                                    .toString(),
                                IdolModel::class.java,
                            )

                            _navigateCommunity.postValue(Event(idol))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            _errorToast.postValue(Event(""))
                        }
                    },
                    errorListener = {
                        _errorToast.postValue(Event(""))
                    }
                )
            }
        } else if (bannerLink.startsWith("http")) {
            _navigateAppLink.postValue(Event(bannerLink))
        }
    }

    fun searchTrends() {
        viewModelScope.launch {
            searchRepository.getTrend(
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            Logger.d("searchTrend response::$response")
                            _searchTrends.postValue(Event(response.getJSONArray("objects")))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        v("searchTrend response error")
                    }
                },
                {
                    v("searchTrend response error")
                }
            )
        }
    }

    fun searchSuggest(searchKeyword: String) {
        viewModelScope.launch {
            searchRepository.getSuggest(
                searchKeyword,
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            Logger.d("searchSuggest response::$response")
                            _searchSuggests.postValue(Event(response.getJSONArray("objects")))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        v("searchSuggest response error")
                    }
                },
                {
                    v("searchSuggest response error")
                }
            )
        }
    }
}

