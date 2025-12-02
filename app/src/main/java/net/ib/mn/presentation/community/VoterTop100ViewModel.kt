package net.ib.mn.presentation.community

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.remote.dto.VoterTop100Model
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.domain.model.ApiResult

/**
 * VoterTop100 화면의 UI 상태
 */
sealed interface VoterTop100UiState {
    data object Loading : VoterTop100UiState
    data class Success(
        val items: List<VoterTop100Model>,
        val myRank: String? = null,
        val idolName: String = "",
        val groupName: String = ""
    ) : VoterTop100UiState
    data class Error(val message: String) : VoterTop100UiState
}

/**
 * VoterTop100 ViewModel
 *
 * old 프로젝트의 HeartVoteRankingActivity를 참고하여 작성
 * 특정 아이돌에게 투표한 유저들의 Top100 랭킹
 */
@HiltViewModel(assistedFactory = VoterTop100ViewModel.Factory::class)
class VoterTop100ViewModel @AssistedInject constructor(
    private val usersRepository: UsersRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): VoterTop100ViewModel
    }

    private val idolId: Int = savedStateHandle.get<Int>("idolId") ?: 0
    private val idolName: String = savedStateHandle.get<String>("idolName") ?: ""
    private val groupName: String = savedStateHandle.get<String>("groupName") ?: ""

    private val _uiState = MutableStateFlow<VoterTop100UiState>(VoterTop100UiState.Loading)
    val uiState: StateFlow<VoterTop100UiState> = _uiState.asStateFlow()

    // Old 프로젝트의 IdolGson과 동일한 설정
    // FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES: JSON의 snake_case를 Kotlin의 camelCase로 변환
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    init {
        if (idolId > 0) {
            loadRankedUsers()
        } else {
            _uiState.value = VoterTop100UiState.Error("Invalid idol ID")
        }
    }

    /**
     * 투표한 유저 랭킹 데이터 로드
     *
     * old 프로젝트의 HeartVoteRankingActivity.loadList() 참고
     */
    fun loadRankedUsers() {
        viewModelScope.launch {
            usersRepository.getRankedUser(idolId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.value = VoterTop100UiState.Loading
                    }
                    is ApiResult.Success -> {
                        try {
                            val jsonString = result.data
                            Log.d("VoterTop100VM", "Parsing JSON response")

                            // JSON 파싱 - old 프로젝트와 동일한 구조
                            // { "ranks": { "objects": [...] }, "my_rank": "123" }
                            val responseType = object : TypeToken<Map<String, Any>>() {}.type
                            val responseMap: Map<String, Any> = gson.fromJson(jsonString, responseType)

                            val items = parseRankedUsers(responseMap)
                            val myRank = responseMap["my_rank"]?.toString()?.takeIf {
                                it != "null" && it.isNotEmpty()
                            }

                            Log.d("VoterTop100VM", "Parsed - Users: ${items.size}, MyRank: $myRank")

                            _uiState.value = VoterTop100UiState.Success(
                                items = items,
                                myRank = myRank,
                                idolName = idolName,
                                groupName = groupName
                            )
                        } catch (e: Exception) {
                            Log.e("VoterTop100VM", "Parse error: ${e.message}", e)
                            _uiState.value = VoterTop100UiState.Error(e.message ?: "Parse error")
                        }
                    }
                    is ApiResult.Error -> {
                        Log.e("VoterTop100VM", "API error: ${result.message}")
                        _uiState.value = VoterTop100UiState.Error(result.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    /**
     * 유저 랭킹 파싱
     *
     * old 프로젝트 HeartVoteRankingActivity 참고
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseRankedUsers(responseMap: Map<String, Any>): List<VoterTop100Model> {
        return try {
            val ranksObject = responseMap["ranks"] as? Map<String, Any> ?: return emptyList()
            val objectsArray = ranksObject["objects"] as? List<Map<String, Any>> ?: return emptyList()

            val items = objectsArray.mapNotNull { obj ->
                try {
                    val json = gson.toJson(obj)
                    // 첫 번째 유저 JSON 로그 출력 (디버깅용)
                    if (objectsArray.indexOf(obj) == 0) {
                        Log.d("VoterTop100VM", "First user JSON: $json")
                    }
                    gson.fromJson(json, VoterTop100Model::class.java)
                } catch (e: Exception) {
                    Log.e("VoterTop100VM", "Item parse error: ${e.message}")
                    null
                }
            }.filter {
                // levelHeart > 0 인 유저만 표시 (old 프로젝트와 동일)
                it.levelHeart > 0
            }.toMutableList()

            // 순위 설정 (0부터 시작)
            items.forEachIndexed { index, item ->
                item.rank = index
            }

            items
        } catch (e: Exception) {
            Log.e("VoterTop100VM", "parseRankedUsers error: ${e.message}", e)
            emptyList()
        }
    }
}
