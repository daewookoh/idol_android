package net.ib.mn.presentation.main.myfavorite

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.FavoritesRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.data.remote.udp.IdolBroadcastManager
import net.ib.mn.util.IdolImageUtil
import javax.inject.Inject

/**
 * My Favorite ViewModel
 *
 * 최애 관리 화면의 비즈니스 로직 처리
 *
 * OLD 프로젝트의 FavoriteIdolBaseFragment 로직을 참고하여 구현
 */
@HiltViewModel
class MyFavoriteViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val idolRepository: IdolRepository,
    private val rankingRepository: net.ib.mn.domain.repository.RankingRepository,
    private val preferencesManager: PreferencesManager,
    private val userRepository: net.ib.mn.domain.repository.UserRepository,
    private val configRepository: net.ib.mn.domain.repository.ConfigRepository,
    private val broadcastManager: IdolBroadcastManager
) : BaseViewModel<MyFavoriteContract.State, MyFavoriteContract.Intent, MyFavoriteContract.Effect>() {

    private var udpSubscriptionJob: Job? = null
    private var isScreenVisible = false
    private val logTag = "MyFavoriteVM"

    override fun createInitialState(): MyFavoriteContract.State {
        return MyFavoriteContract.State()
    }

    override fun handleIntent(intent: MyFavoriteContract.Intent) {
        when (intent) {
            is MyFavoriteContract.Intent.LoadFavorites -> loadFavorites()
            is MyFavoriteContract.Intent.RefreshFavorites -> refreshFavorites()
            is MyFavoriteContract.Intent.OnIdolClick -> onIdolClick(intent.idolId)
            is MyFavoriteContract.Intent.OnSettingClick -> onSettingClick()
            is MyFavoriteContract.Intent.OnPageVisible -> onPageVisible()
            is MyFavoriteContract.Intent.OnScreenVisible -> onScreenVisible()
            is MyFavoriteContract.Intent.OnScreenHidden -> onScreenHidden()
        }
    }

    init {
        // 초기 데이터는 onPageVisible에서 로드
    }

    /**
     * 화면이 보일 때 호출 - UDP 구독 시작
     */
    private fun onScreenVisible() {
        android.util.Log.d(logTag, "👁️ Screen became visible")
        isScreenVisible = true

        // 데이터가 있으면 새로고침
        if (currentState.favoriteIdols.isNotEmpty()) {
            loadFavorites()
        }

        // UDP 구독 시작
        startUdpSubscription()
    }

    /**
     * 화면이 숨겨질 때 호출 - UDP 구독 중지
     */
    private fun onScreenHidden() {
        android.util.Log.d(logTag, "🙈 Screen hidden")
        isScreenVisible = false
        stopUdpSubscription()
    }

    /**
     * UDP 구독 시작
     */
    private fun startUdpSubscription() {
        // 이미 구독 중이면 중복 방지
        if (udpSubscriptionJob?.isActive == true) {
            android.util.Log.d(logTag, "⚠️ UDP already subscribed, skipping")
            return
        }

        android.util.Log.d(logTag, "📡 Starting UDP subscription")
        udpSubscriptionJob = viewModelScope.launch {
            broadcastManager.updateEvent.collect { changedIds ->
                // 화면이 보이지 않으면 무시
                if (!isScreenVisible) {
                    android.util.Log.d(logTag, "⏭️ Screen not visible, ignoring UDP update")
                    return@collect
                }

                android.util.Log.d(logTag, "🔄 UDP update event received - ${changedIds.size} idols changed")

                // 최애 목록 새로고침
                loadFavorites()
            }
        }
    }

    /**
     * UDP 구독 중지
     */
    private fun stopUdpSubscription() {
        udpSubscriptionJob?.cancel()
        udpSubscriptionJob = null
        android.util.Log.d(logTag, "🛑 Stopped UDP subscription")
    }

    override fun onCleared() {
        super.onCleared()
        stopUdpSubscription()
        android.util.Log.d(logTag, "♻️ ViewModel cleared")
    }

    /**
     * 페이지가 visible 될 때 호출
     * getUserSelf를 호출해서 most idol ID를 갱신하고, favorites 목록 로드
     */
    private fun onPageVisible() {
        android.util.Log.d(logTag, "📱 Page visible - refreshing user data")

        viewModelScope.launch {
            // getUserSelf 호출하여 most idol ID 갱신
            userRepository.getUserSelf().collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        result.data.objects.firstOrNull()?.most?.let { most ->
                            preferencesManager.setMostIdol(most.id, most.type, most.groupId)
                            android.util.Log.d(logTag, "💾 Updated most idol: id=${most.id}")
                        }
                        loadFavorites()
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e(logTag, "❌ getUserSelf error: ${result.message}")
                        loadFavorites() // 에러 발생해도 캐시된 데이터 표시
                    }
                    is ApiResult.Loading -> {}
                }
            }
        }
    }

    /**
     * 최애 목록 로드
     *
     * 로직:
     * 1. Favorites API로 즐겨찾기 목록 가져오기 (chartCode, league 정보 포함)
     * 2. 로컬 DB에서 실시간 heart 수 병합
     * 3. ChartCode별 그루핑 및 DB 전체와 비교하여 순위 계산
     * 4. 섹션 헤더 추가
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            try {
                val mostIdolId = preferencesManager.mostIdolId.firstOrNull()
                android.util.Log.d(logTag, "🎯 Most Idol ID: $mostIdolId")

                // Favorites API 호출
                var favoriteResult: ApiResult<List<net.ib.mn.data.remote.dto.FavoriteDto>>? = null
                favoritesRepository.getFavoritesSelf().collect { result ->
                    favoriteResult = result
                    if (result is ApiResult.Success || result is ApiResult.Error) {
                        return@collect
                    }
                }

                when (favoriteResult) {
                    is ApiResult.Success -> {
                        val favoriteDtos = (favoriteResult as ApiResult.Success).data
                        android.util.Log.d(logTag, "✅ Loaded ${favoriteDtos.size} favorites")

                        // 로컬 DB에서 실시간 heart 수 병합
                        val favoritesWithRealTimeHearts = favoriteDtos.map { dto ->
                            val localEntity = idolRepository.getIdolById(dto.idol.id)
                            val realTimeHeart = localEntity?.heart ?: dto.idol.heart ?: 0L
                            dto.copy(idol = dto.idol.copy(heart = realTimeHeart))
                        }

                        // ChartCodeInfo 맵 생성 (섹션 이름 표시용)
                        val mainChartModel = configRepository.getMainChartModel()
                        val chartCodeInfoMap = buildMap<String, String> {
                            mainChartModel?.males?.forEach { info ->
                                info.code?.let { put(it, info.fullName ?: info.name ?: it) }
                            }
                            mainChartModel?.females?.forEach { info ->
                                info.code?.let { put(it, info.fullName ?: info.name ?: it) }
                            }
                        }

                        // ChartCode별로 그루핑
                        val groupedByChartCode = favoritesWithRealTimeHearts.groupBy { dto ->
                            dto.idol.getChartCode() ?: "UNKNOWN"
                        }
                        android.util.Log.d(logTag, "✅ Grouped by: ${groupedByChartCode.keys}")

                        // 각 그룹 내에서 순위 계산 및 섹션 헤더 추가
                        // DB의 같은 chartCode 모든 아이돌과 비교하여 랭킹 산정
                        val favoriteIdolsWithSections = buildList {
                            groupedByChartCode.forEach { (chartCode, dtos) ->
                                // chartCode를 type과 category로 분리 (예: PR_S_F -> type=S, category=F)
                                val parts = chartCode.split("_")
                                if (parts.size >= 3) {
                                    val type = parts[1]  // S or G
                                    val category = parts[2]  // M or F

                                    // DB에서 같은 type+category의 모든 아이돌 조회
                                    val allIdolsInGroup = idolRepository.getIdolsByTypeAndCategory(type, category)
                                        .sortedByDescending { it.heart }
                                    val maxScore = allIdolsInGroup.firstOrNull()?.heart ?: 0L
                                    val sectionName = chartCodeInfoMap[chartCode] ?: chartCode

                                    // 섹션 헤더 추가
                                    add(MyFavoriteContract.FavoriteIdol(
                                        idolId = -1,
                                        name = "",
                                        imageUrl = "",
                                        chartCode = chartCode,
                                        isSection = true,
                                        sectionName = sectionName,
                                        sectionMaxScore = maxScore
                                    ))

                                    // 즐겨찾기한 아이돌들의 순위를 전체 리스트에서 계산
                                    dtos.forEach { dto ->
                                        val idolHeart = dto.idol.heart ?: 0L
                                        val rank = allIdolsInGroup.count { it.heart > idolHeart } + 1

                                        add(MyFavoriteContract.FavoriteIdol(
                                            idolId = dto.idol.id,
                                            name = dto.idol.name ?: "Unknown",
                                            imageUrl = dto.idol.imageUrl ?: "",
                                            rank = rank,
                                            score = dto.idol.heart,
                                            chartCode = chartCode,
                                            isSection = false,
                                            sectionMaxScore = maxScore
                                        ))
                                    }

                                    android.util.Log.d(logTag, "  '$sectionName': ${dtos.size} idols, maxScore=$maxScore")
                                }
                            }
                        }

                        android.util.Log.d(logTag, "✅ Total items: ${favoriteIdolsWithSections.size}")

                        // Most Idol 찾기 및 TopFavorite 생성
                        val topFavorite = mostIdolId?.let { id ->
                            favoritesWithRealTimeHearts.find { it.idol.id == id }
                        }?.let { dto ->
                            val mostIdolRank = favoriteIdolsWithSections
                                .find { it.idolId == dto.idol.id && !it.isSection }
                                ?.rank

                            android.util.Log.d(logTag, "✅ Most Idol: ${dto.idol.name}, rank=$mostIdolRank")
                            MyFavoriteContract.TopFavorite(
                                idolId = dto.idol.id,
                                name = dto.idol.name ?: "Unknown",
                                groupName = dto.idol.groupName,
                                top3ImageUrls = listOf(dto.idol.imageUrl, dto.idol.imageUrl2, dto.idol.imageUrl3),
                                top3VideoUrls = emptyList(),
                                league = dto.idol.league,
                                rank = mostIdolRank,
                                heart = dto.idol.heart
                            )
                        }

                        setState {
                            copy(
                                isLoading = false,
                                favoriteIdols = favoriteIdolsWithSections,
                                topFavorite = topFavorite,
                                error = null
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        val errorMsg = (favoriteResult as ApiResult.Error).message ?: "Unknown error"
                        android.util.Log.e(logTag, "❌ Error: $errorMsg")
                        setState { copy(isLoading = false, error = errorMsg) }
                        setEffect { MyFavoriteContract.Effect.ShowError(errorMsg) }
                    }
                    else -> {
                        android.util.Log.e(logTag, "❌ Unexpected result state")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(logTag, "❌ Exception in loadFavorites", e)
                setState { copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun refreshFavorites() {
        loadFavorites()
        setEffect { MyFavoriteContract.Effect.ShowToast("새로고침 완료") }
    }

    private fun onIdolClick(idolId: Int) {
        setEffect { MyFavoriteContract.Effect.NavigateToIdolDetail(idolId) }
    }

    private fun onSettingClick() {
        setEffect { MyFavoriteContract.Effect.NavigateToFavoriteSetting }
    }
}
