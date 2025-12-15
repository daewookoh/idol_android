package net.ib.mn.presentation.mostset

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.FavoritesRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import javax.inject.Inject

/**
 * MostSet 화면 ViewModel
 *
 * old 프로젝트의 FavoriteSettingActivity를 MVI 패턴으로 재구현
 * - 현재 최애 아이돌 표시
 * - 아이돌 검색
 * - 즐겨찾기 추가/삭제
 * - 최애 설정/해제
 */
@HiltViewModel
class MostSetViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val usersRepository: UsersRepository,
    private val userCacheRepository: UserCacheRepository,
    private val idolDao: IdolDao,
    private val preferencesManager: PreferencesManager
) : BaseViewModel<MostSetContract.State, MostSetContract.Intent, MostSetContract.Effect>() {

    companion object {
        private const val TAG = "MostSetViewModel"
    }

    // 전체 아이돌 목록 캐시 (검색용)
    private var allIdols: List<MostSetContract.IdolItem> = emptyList()

    // 즐겨찾기 맵 (idol ID -> favorite ID)
    private val favoriteIdMap = mutableMapOf<Int, Int>()

    override fun createInitialState(): MostSetContract.State = MostSetContract.State()

    init {
        loadInitialData()
    }

    override fun handleIntent(intent: MostSetContract.Intent) {
        when (intent) {
            is MostSetContract.Intent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is MostSetContract.Intent.DoSearch -> doSearch()
            is MostSetContract.Intent.ToggleFavorite -> toggleFavorite(intent.idol)
            is MostSetContract.Intent.SetMost -> setMost(intent.idol)
            is MostSetContract.Intent.LoadInitialData -> loadInitialData()
        }
    }

    /**
     * 검색어 변경
     */
    private fun updateSearchQuery(query: String) {
        setState { copy(searchQuery = query) }
    }

    /**
     * 검색 실행
     */
    private fun doSearch() {
        val keyword = currentState.searchQuery.trim().lowercase()
        if (keyword.isBlank()) {
            setState { copy(isSearchMode = false, searchResults = emptyList()) }
            return
        }

        setEffect { MostSetContract.Effect.HideKeyboard }

        // 전체 아이돌에서 검색
        val results = allIdols.filter { idol ->
            idol.name.lowercase().contains(keyword) ||
                idol.nameEn.lowercase().contains(keyword) ||
                idol.nameJp.lowercase().contains(keyword) ||
                idol.nameZh.lowercase().contains(keyword) ||
                idol.nameZhTw.lowercase().contains(keyword) ||
                (idol.groupName?.lowercase()?.contains(keyword) == true)
        }.map { idol ->
            // 최신 favorite 상태 반영
            idol.copy(
                isFavorite = favoriteIdMap.containsKey(idol.id),
                favoriteId = favoriteIdMap[idol.id],
                isMost = idol.id == currentState.mostIdolId
            )
        }.sortedByDescending { it.heart }

        setState {
            copy(
                isSearchMode = true,
                searchResults = results,
                searchQuery = ""
            )
        }
    }

    /**
     * 즐겨찾기 토글
     */
    private fun toggleFavorite(idol: MostSetContract.IdolItem) {
        viewModelScope.launch {
            if (idol.isFavorite) {
                // 즐겨찾기 삭제
                val favoriteId = idol.favoriteId ?: favoriteIdMap[idol.id]
                if (favoriteId == null) {
                    logE(TAG, "Cannot remove favorite: favoriteId is null")
                    return@launch
                }

                favoritesRepository.removeFavorite(favoriteId)
                    .onEach { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                favoriteIdMap.remove(idol.id)
                                userCacheRepository.removeFavoriteFromCache(idol.id)
                                updateIdolInList(idol.id) { it.copy(isFavorite = false, favoriteId = null) }
                                logD(TAG, "Favorite removed: ${idol.name}")
                            }
                            is ApiResult.Error -> {
                                logE(TAG, "Failed to remove favorite: ${result.error.message}")
                                setEffect { MostSetContract.Effect.ShowToastRes(R.string.error_abnormal_exception) }
                            }
                            is ApiResult.Loading -> { /* ignore */ }
                        }
                    }
                    .catch { e ->
                        logE(TAG, "Exception removing favorite: ${e.message}", e)
                        setEffect { MostSetContract.Effect.ShowToastRes(R.string.error_abnormal_exception) }
                    }
                    .launchIn(viewModelScope)
            } else {
                // 즐겨찾기 추가
                favoritesRepository.addFavorite(idol.id)
                    .onEach { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                val newFavoriteId = result.data
                                favoriteIdMap[idol.id] = newFavoriteId
                                userCacheRepository.addFavoriteToCache(idol.id, newFavoriteId)
                                updateIdolInList(idol.id) { it.copy(isFavorite = true, favoriteId = newFavoriteId) }
                                logD(TAG, "Favorite added: ${idol.name}, favoriteId=$newFavoriteId")
                            }
                            is ApiResult.Error -> {
                                logE(TAG, "Failed to add favorite: ${result.error.message}")
                                setEffect { MostSetContract.Effect.ShowToast(result.error.message ?: "Error") }
                            }
                            is ApiResult.Loading -> { /* ignore */ }
                        }
                    }
                    .catch { e ->
                        logE(TAG, "Exception adding favorite: ${e.message}", e)
                        setEffect { MostSetContract.Effect.ShowToastRes(R.string.error_abnormal_exception) }
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    /**
     * 최애 설정/해제
     */
    private fun setMost(idol: MostSetContract.IdolItem?) {
        viewModelScope.launch {
            val userResourceUri = userCacheRepository.getUserResourceUri()
            if (userResourceUri == null) {
                logE(TAG, "Cannot set most: userResourceUri is null")
                return@launch
            }

            setState { copy(isLoading = true) }

            val idolResourceUri = idol?.resourceUri?.takeIf { it.isNotBlank() }
                ?: idol?.let { "/api/v1/idols/${it.id}/" }

            val result = usersRepository.updateMost(
                userResourceUri = userResourceUri,
                idolResourceUri = idolResourceUri
            )

            result.fold(
                onSuccess = { response ->
                    val success = response.optBoolean("success", false)
                    if (success) {
                        val prevMostId = currentState.mostIdolId

                        if (idol != null) {
                            // 최애 설정
                            setState {
                                copy(
                                    mostIdolId = idol.id,
                                    mostIdolName = idol.name,
                                    mostIdolImageUrl = idol.imageUrl ?: idol.imageUrl2,
                                    isLoading = false
                                )
                            }

                            // 캐시 업데이트
                            val chartCode = idol.chartCodes
                                ?.firstOrNull { !it.startsWith("AW_") && !it.startsWith("DF_") }
                                ?: idol.chartCodes?.firstOrNull()
                            userCacheRepository.updateMostIdolCache(idol.id, idol.category, chartCode)

                            // 이전 최애 상태 업데이트
                            prevMostId?.let { prevId ->
                                if (prevId != idol.id) {
                                    updateIdolInList(prevId) { it.copy(isMost = false) }
                                }
                            }

                            // 새 최애 상태 업데이트
                            updateIdolInList(idol.id) { it.copy(isMost = true, isFavorite = true) }

                            // 즐겨찾기에 자동 추가
                            if (!favoriteIdMap.containsKey(idol.id)) {
                                addFavoriteAfterSetMost(idol)
                            }

                            logD(TAG, "Most set: ${idol.name}")
                        } else {
                            // 최애 해제
                            setState {
                                copy(
                                    mostIdolId = null,
                                    mostIdolName = null,
                                    mostIdolImageUrl = null,
                                    isLoading = false
                                )
                            }

                            // 캐시 업데이트
                            userCacheRepository.updateMostIdolCache(null, null, null)

                            // 이전 최애 상태 업데이트
                            prevMostId?.let { prevId ->
                                updateIdolInList(prevId) { it.copy(isMost = false) }
                            }

                            logD(TAG, "Most cleared")
                        }

                        // UserSelf 갱신
                        viewModelScope.launch {
                            try {
                                userCacheRepository.refreshUserData()
                            } catch (e: Exception) {
                                logE(TAG, "Failed to refresh user data: ${e.message}", e)
                            }
                        }
                    } else {
                        setState { copy(isLoading = false) }
                        val msg = response.optString("msg", "Unknown error")
                        setEffect { MostSetContract.Effect.ShowToast(msg) }
                    }
                },
                onFailure = { e ->
                    setState { copy(isLoading = false) }
                    logE(TAG, "Failed to set most: ${e.message}", e)
                    setEffect { MostSetContract.Effect.ShowToastRes(R.string.error_abnormal_exception) }
                }
            )
        }
    }

    /**
     * 최애 설정 후 자동으로 즐겨찾기 추가
     */
    private fun addFavoriteAfterSetMost(idol: MostSetContract.IdolItem) {
        viewModelScope.launch {
            favoritesRepository.addFavorite(idol.id)
                .onEach { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val newFavoriteId = result.data
                            favoriteIdMap[idol.id] = newFavoriteId
                            userCacheRepository.addFavoriteToCache(idol.id, newFavoriteId)
                            updateIdolInList(idol.id) { it.copy(isFavorite = true, favoriteId = newFavoriteId) }
                        }
                        else -> { /* ignore */ }
                    }
                }
                .catch { /* ignore */ }
                .launchIn(viewModelScope)
        }
    }

    /**
     * 초기 데이터 로드
     */
    private fun loadInitialData() {
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            // CDN URL 로드
            val cdnUrl = preferencesManager.cdnUrl.first() ?: ""
            setState { copy(cdnUrl = cdnUrl) }

            // 현재 최애 정보 로드
            loadMostIdolInfo()

            // 전체 아이돌 로드
            loadAllIdols()

            // 즐겨찾기 로드
            loadFavorites()
        }
    }

    /**
     * 현재 최애 아이돌 정보 로드
     */
    private suspend fun loadMostIdolInfo() {
        val mostIdolId = userCacheRepository.getMostIdolId()
        if (mostIdolId != null) {
            val mostIdol = idolDao.getIdolById(mostIdolId)
            if (mostIdol != null) {
                val cdnUrl = currentState.cdnUrl
                val imageUrl = mostIdol.imageUrl?.let {
                    if (it.startsWith("http")) it else "$cdnUrl$it"
                } ?: mostIdol.imageUrl2?.let {
                    if (it.startsWith("http")) it else "$cdnUrl$it"
                }

                setState {
                    copy(
                        mostIdolId = mostIdolId,
                        mostIdolName = mostIdol.name,
                        mostIdolImageUrl = imageUrl
                    )
                }
            }
        }
    }

    /**
     * 전체 아이돌 로드 (로컬 DB에서)
     */
    private suspend fun loadAllIdols() {
        val idols = idolDao.getAll()
        val mostId = currentState.mostIdolId
        val cdnUrl = currentState.cdnUrl

        allIdols = idols.map { entity ->
            MostSetContract.IdolItem(
                id = entity.id,
                name = entity.name,
                nameEn = entity.nameEn,
                nameJp = entity.nameJp,
                nameZh = entity.nameZh,
                nameZhTw = entity.nameZhTw,
                groupId = entity.groupId,
                groupName = null, // IdolEntity에는 groupName 필드가 없음
                imageUrl = entity.imageUrl?.let { if (it.startsWith("http")) it else "$cdnUrl$it" },
                imageUrl2 = entity.imageUrl2?.let { if (it.startsWith("http")) it else "$cdnUrl$it" },
                heart = entity.heart,
                type = entity.type,
                category = entity.category,
                resourceUri = entity.resourceUri,
                isFavorite = favoriteIdMap.containsKey(entity.id),
                isMost = entity.id == mostId,
                favoriteId = favoriteIdMap[entity.id],
                isViewable = entity.isViewable,
                anniversary = entity.anniversary,
                anniversaryDays = entity.anniversaryDays ?: 0,
                miracleCount = entity.miracleCount,
                fairyCount = entity.fairyCount,
                angelCount = entity.angelCount
            )
        }
    }

    /**
     * 즐겨찾기 로드 (favoriteIdMap 업데이트용)
     */
    private fun loadFavorites() {
        favoritesRepository.getFavoritesSelf()
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> { /* ignore */ }
                    is ApiResult.Success -> {
                        favoriteIdMap.clear()
                        result.data.forEach { dto ->
                            favoriteIdMap[dto.idol.id] = dto.id
                        }
                        setState { copy(isLoading = false) }
                        updateAllIdolsFavoriteStatus()
                        logD(TAG, "Favorites loaded: ${result.data.size} items")
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false) }
                        logE(TAG, "Failed to load favorites: ${result.error.message}")
                    }
                }
            }
            .catch { e ->
                setState { copy(isLoading = false) }
                logE(TAG, "Exception loading favorites: ${e.message}", e)
            }
            .launchIn(viewModelScope)
    }

    /**
     * allIdols의 favorite 상태 업데이트
     */
    private fun updateAllIdolsFavoriteStatus() {
        val mostId = currentState.mostIdolId
        allIdols = allIdols.map { idol ->
            idol.copy(
                isFavorite = favoriteIdMap.containsKey(idol.id),
                favoriteId = favoriteIdMap[idol.id],
                isMost = idol.id == mostId
            )
        }
    }

    /**
     * 리스트에서 특정 아이돌 업데이트
     */
    private fun updateIdolInList(idolId: Int, transform: (MostSetContract.IdolItem) -> MostSetContract.IdolItem) {
        // searchResults 업데이트
        val updatedSearchResults = currentState.searchResults.map { idol ->
            if (idol.id == idolId) transform(idol) else idol
        }

        // allIdols 업데이트
        allIdols = allIdols.map { idol ->
            if (idol.id == idolId) transform(idol) else idol
        }

        setState { copy(searchResults = updatedSearchResults) }
    }
}
