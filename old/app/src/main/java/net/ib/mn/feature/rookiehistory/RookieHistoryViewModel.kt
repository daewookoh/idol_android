package net.ib.mn.feature.rookiehistory

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.model.BaseModel
import net.ib.mn.model.CharityModel
import net.ib.mn.model.SuperRookie
import net.ib.mn.utils.UtilK.Companion.getSystemLanguage
import org.json.JSONException

class RookieHistoryViewModel(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val idolsRepository: IdolsRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<RookieHistoryUiState> =
        MutableStateFlow(RookieHistoryUiState.Loading)
    val uiState: StateFlow<RookieHistoryUiState> = _uiState

    private var charityModels: ArrayList<CharityModel> = ArrayList()

    init {
        loadRookieHistories()
    }

    private fun loadRookieHistories() {
        viewModelScope.launch {
            val charityResult = async { getCharityHistory() }
            val superRookieHistoryResult = async { getSuperRookieHistory() }

            val charityResponse = charityResult.await()
            val superRookieHistoryResponse = superRookieHistoryResult.await()

            if (charityResponse.data != null && superRookieHistoryResponse.data != null) {
                charityModels = charityResponse.data

                val sortedList = superRookieHistoryResponse.data.sortedBy { it.ordinal }
                _uiState.emit(RookieHistoryUiState.Success(charityResponse.data, sortedList))
            } else {
                _uiState.emit(RookieHistoryUiState.Error(charityResponse.message ?: superRookieHistoryResponse.message ?: "Failed to load histories"))
            }
        }
    }

    private suspend fun getCharityHistory(): BaseModel<ArrayList<CharityModel>> {
        return withContext(Dispatchers.IO) {
            val result = CompletableDeferred<BaseModel<ArrayList<CharityModel>>>()
            idolsRepository.getCharityHistory("R", context.getSystemLanguage(),
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            val gson = getInstance(false)
                            val charityType = object : TypeToken<ArrayList<CharityModel>>() {}.type
                            val charityModels: ArrayList<CharityModel> = gson.fromJson(response.getJSONArray("objects").toString(), charityType)
                            result.complete(BaseModel(data = charityModels))
                        } catch (e: JSONException) {
                            result.complete(BaseModel(message = e.message))
                        }
                    } else {
                        result.complete(BaseModel(message = "Failed to load histories"))
                    }
                }, { throwable ->
                    result.complete(BaseModel(message = throwable.message))

                })
            result.await()
        }
    }

    private suspend fun getSuperRookieHistory(): BaseModel<ArrayList<SuperRookie>> {
        return withContext(Dispatchers.IO) {
            val result = CompletableDeferred<BaseModel<ArrayList<SuperRookie>>>()
            idolsRepository.getSuperRookieHistory(context.getSystemLanguage(),
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            val gson = getInstance(false)
                            val superRookieType = object : TypeToken<ArrayList<SuperRookie>?>() {}.type
                            val superRookies: ArrayList<SuperRookie> = gson.fromJson(response.getJSONArray("objects").toString(), superRookieType)
                            result.complete(BaseModel(superRookies))
                        } catch (e: JSONException) {
                            result.complete(BaseModel(message = e.message))
                        }
                    } else {
                        result.complete(BaseModel(message = "Failed to load super rookie histories"))
                    }
                }, { throwable ->
                    result.complete(BaseModel(message = throwable.message))
                })
            result.await()
        }
    }
}

class RookieHistoryViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val idolsRepository: IdolsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RookieHistoryViewModel::class.java)) {
            return RookieHistoryViewModel(context, savedStateHandle, idolsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}