package net.ib.mn.presentation.search.result

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.manager.ArticleUpdateEvent
import net.ib.mn.domain.manager.ArticleUpdateManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.SearchIdolModel
import net.ib.mn.domain.model.SearchSupportModel
import net.ib.mn.domain.model.SearchWallpaperModel
import net.ib.mn.domain.repository.FavoritesRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.repository.SearchRepository
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import javax.inject.Inject

/**
 * SearchResult 화면 ViewModel
 *
 * old 프로젝트의 SearchResultActivity를 MVI 패턴으로 재구현
 * - 통합 검색 결과 표시
 * - 아이돌/서포트 더보기
 * - 잡담/게시글 무한 스크롤
 * - 즐겨찾기/최애 설정
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출, 즐겨찾기 관리)에만 집중
 */
@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val favoritesRepository: FavoritesRepository,
    private val usersRepository: UsersRepository,
    private val userCacheRepository: UserCacheRepository,
    private val idolRepository: IdolRepository,
    private val articleUpdateManager: ArticleUpdateManager,
    private val preferencesManager: PreferencesManager
) : BaseViewModel<SearchResultContract.State, SearchResultContract.Intent, SearchResultContract.Effect>() {

    companion object {
        private const val SMALL_TALK_LIMIT = 10
        private const val ARTICLE_LIMIT = 50
        private const val FAVORITE_DEBOUNCE_MS = 2000L
    }

    // 즐겨찾기 중복 클릭 방지용 (idolId -> 마지막 클릭 시간)
    private val favoriteClickTimes = mutableMapOf<Int, Long>()

    init {
        observeArticleUpdates()
        observeFavoriteChanges()
        observeMostChanges()
    }

    override fun createInitialState(): SearchResultContract.State = SearchResultContract.State()

    override fun handleIntent(intent: SearchResultContract.Intent) {
        when (intent) {
            is SearchResultContract.Intent.Search -> search(intent.keyword)
            is SearchResultContract.Intent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is SearchResultContract.Intent.NewSearch -> newSearch(intent.keyword)
            is SearchResultContract.Intent.ShowAllIdols -> showAllIdols()
            is SearchResultContract.Intent.ShowAllSupports -> showAllSupports()
            is SearchResultContract.Intent.LoadMoreSmallTalks -> loadMoreSmallTalks()
            is SearchResultContract.Intent.LoadMoreArticles -> loadMoreArticles()
            is SearchResultContract.Intent.ToggleFavorite -> toggleFavorite(intent.idol)
            is SearchResultContract.Intent.SetMost -> handleSetMost(intent.idol)
            is SearchResultContract.Intent.ConfirmSetMost -> confirmSetMost(intent.idol)
            is SearchResultContract.Intent.ClickIdol -> handleIdolClick(intent.idol)
            is SearchResultContract.Intent.ClickIdolCommunity -> handleIdolCommunityClick(intent.idol)
            is SearchResultContract.Intent.ClickIdolSmallTalk -> handleIdolSmallTalkClick(intent.idol)
            is SearchResultContract.Intent.ClickIdolSchedule -> handleIdolScheduleClick(intent.idol)
            is SearchResultContract.Intent.ClickSupport -> handleSupportClick(intent.support)
            is SearchResultContract.Intent.ClickWallpaper -> handleWallpaperClick(intent.wallpaper)
            is SearchResultContract.Intent.ClickArticle -> handleArticleClick(intent.article)
            is SearchResultContract.Intent.NavigateBack -> setEffect { SearchResultContract.Effect.NavigateBack }
        }
    }

    /**
     * 검색어 업데이트 (UI 상태만 변경)
     */
    private fun updateSearchQuery(query: String) {
        setState { copy(keyword = query) }
    }

    /**
     * 통합 검색 실행
     */
    private fun search(keyword: String) {
        setState {
            copy(
                keyword = keyword,
                isLoading = true,
                error = null,
                isEmpty = false
            )
        }

        searchRepository.search(
            keyword = keyword,
            category = null,
            offset = 0,
            limit = ARTICLE_LIMIT
        )
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is ApiResult.Success -> {
                        val data = result.data
                        val isEmpty = data.idols.isEmpty() &&
                                data.supports.isEmpty() &&
                                data.wallpapers.isEmpty() &&
                                data.smallTalks.isEmpty() &&
                                data.articles.isEmpty()

                        // 즐겨찾기/최애 상태 적용
                        val favoriteIds = userCacheRepository.getFavoriteIdolIds()
                        val mostIdolId = userCacheRepository.getMostIdolId()
                        val idolsWithState = data.idols.map { idol ->
                            idol.copy(
                                isFavorite = favoriteIds.contains(idol.id),
                                isMost = idol.id == mostIdolId
                            )
                        }

                        // 배경화면에 아이돌 이름 설정
                        val wallpapersWithIdolName = data.wallpapers.map { wallpaper ->
                            val idolName = data.idols.find { it.id == wallpaper.idolId }?.name
                            wallpaper.copy(idolName = idolName)
                        }

                        setState {
                            copy(
                                isLoading = false,
                                idols = idolsWithState,
                                supports = data.supports,
                                wallpapers = wallpapersWithIdolName,
                                smallTalks = data.smallTalks,
                                articles = data.articles,
                                smallTalkOffset = data.smallTalkOffset,
                                hasMoreSmallTalks = data.hasMoreSmallTalks,
                                hasMoreArticles = data.articles.size >= ARTICLE_LIMIT,
                                articleOffset = data.articles.size,
                                isEmpty = isEmpty,
                                error = null
                            )
                        }

                        // 아이돌 이름이 없는 배경화면은 DB에서 조회
                        loadMissingIdolNamesForWallpapers(wallpapersWithIdolName)

                        // 서포트의 아이돌 정보 로드
                        loadIdolInfoForSupports(data.supports)
                    }
                    is ApiResult.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = result.error.message
                            )
                        }
                    }
                }
            }
            .catch { e ->
                setState {
                    copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 새 검색 (기존 결과 초기화)
     */
    private fun newSearch(keyword: String) {
        setState {
            copy(
                keyword = keyword,
                idols = emptyList(),
                showAllIdols = false,
                supports = emptyList(),
                showAllSupports = false,
                wallpapers = emptyList(),
                smallTalks = emptyList(),
                articles = emptyList(),
                smallTalkOffset = 0,
                articleOffset = 0,
                hasMoreSmallTalks = true,
                hasMoreArticles = true,
                isEmpty = false
            )
        }
        search(keyword)
    }

    /**
     * 아이돌 더보기
     */
    private fun showAllIdols() {
        setState { copy(showAllIdols = true) }
    }

    /**
     * 서포트 더보기
     */
    private fun showAllSupports() {
        setState { copy(showAllSupports = true) }
    }

    /**
     * 잡담 더 로드 (무한 스크롤)
     */
    private fun loadMoreSmallTalks() {
        if (currentState.isLoadingMoreSmallTalk || !currentState.hasMoreSmallTalks) return

        setState { copy(isLoadingMoreSmallTalk = true) }

        searchRepository.search(
            keyword = currentState.keyword,
            category = "articlev2",  // 잡담게시판
            offset = currentState.smallTalkOffset + currentState.smallTalks.size,
            limit = SMALL_TALK_LIMIT
        )
            .onEach { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data
                        setState {
                            copy(
                                isLoadingMoreSmallTalk = false,
                                smallTalks = smallTalks + data.smallTalks,
                                smallTalkOffset = data.smallTalkOffset,
                                hasMoreSmallTalks = data.hasMoreSmallTalks
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoadingMoreSmallTalk = false) }
                    }
                    is ApiResult.Loading -> { /* ignore */ }
                }
            }
            .catch {
                setState { copy(isLoadingMoreSmallTalk = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 게시글 더 로드 (무한 스크롤)
     */
    private fun loadMoreArticles() {
        if (currentState.isLoadingMoreArticle || !currentState.hasMoreArticles) return

        setState { copy(isLoadingMoreArticle = true) }

        searchRepository.search(
            keyword = currentState.keyword,
            category = "article",
            offset = currentState.articleOffset,
            limit = ARTICLE_LIMIT
        )
            .onEach { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data
                        setState {
                            copy(
                                isLoadingMoreArticle = false,
                                articles = articles + data.articles,
                                articleOffset = articleOffset + data.articles.size,
                                hasMoreArticles = data.articles.size >= ARTICLE_LIMIT
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoadingMoreArticle = false) }
                    }
                    is ApiResult.Loading -> { /* ignore */ }
                }
            }
            .catch {
                setState { copy(isLoadingMoreArticle = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 즐겨찾기 토글
     * 최애인 경우 즐겨찾기 해제 불가 (Old 프로젝트와 동일)
     * 2초 debounce 적용하여 중복 클릭 방지
     */
    private fun toggleFavorite(idol: SearchIdolModel) {
        // 최애인 경우 즐겨찾기 해제 불가
        if (idol.isMost && idol.isFavorite) {
            return
        }

        // 중복 클릭 방지 (2초 debounce)
        val currentTime = System.currentTimeMillis()
        val lastClickTime = favoriteClickTimes[idol.id] ?: 0L
        if (currentTime - lastClickTime < FAVORITE_DEBOUNCE_MS) {
            return
        }
        favoriteClickTimes[idol.id] = currentTime

        viewModelScope.launch {
            val newFavoriteState = !idol.isFavorite

            // UI 즉시 업데이트
            updateIdolFavoriteState(idol.id, newFavoriteState)

            // API 호출
            if (newFavoriteState) {
                favoritesRepository.addFavorite(idol.id)
                    .onEach { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                // 로컬 캐시 업데이트 (favoriteId 반환됨)
                                userCacheRepository.addFavoriteToCache(idol.id, result.data)
                            }
                            is ApiResult.Error -> {
                                // 실패 시 롤백
                                updateIdolFavoriteState(idol.id, false)
                                setEffect { SearchResultContract.Effect.ShowToast(result.message ?: "Failed to add favorite") }
                            }
                            is ApiResult.Loading -> { /* ignore */ }
                        }
                    }
                    .catch { e ->
                        updateIdolFavoriteState(idol.id, false)
                        setEffect { SearchResultContract.Effect.ShowToast(e.message ?: "Failed to add favorite") }
                    }
                    .launchIn(viewModelScope)
            } else {
                // 즐겨찾기 삭제 시 favoriteId 필요
                val favoriteId = userCacheRepository.getFavoriteId(idol.id)
                if (favoriteId != null) {
                    favoritesRepository.removeFavorite(favoriteId)
                        .onEach { result ->
                            when (result) {
                                is ApiResult.Success -> {
                                    // 로컬 캐시에서 제거
                                    userCacheRepository.removeFavoriteFromCache(idol.id)
                                }
                                is ApiResult.Error -> {
                                    // 실패 시 롤백
                                    updateIdolFavoriteState(idol.id, true)
                                    setEffect { SearchResultContract.Effect.ShowToast(result.message ?: "Failed to remove favorite") }
                                }
                                is ApiResult.Loading -> { /* ignore */ }
                            }
                        }
                        .catch { e ->
                            updateIdolFavoriteState(idol.id, true)
                            setEffect { SearchResultContract.Effect.ShowToast(e.message ?: "Failed to remove favorite") }
                        }
                        .launchIn(viewModelScope)
                } else {
                    // favoriteId가 없으면 롤백
                    updateIdolFavoriteState(idol.id, true)
                }
            }
        }
    }

    private fun updateIdolFavoriteState(idolId: Int, isFavorite: Boolean) {
        setState {
            copy(
                idols = idols.map {
                    if (it.id == idolId) it.copy(isFavorite = isFavorite) else it
                }
            )
        }
    }

    /**
     * 최애 설정 처리 (다이얼로그 표시)
     */
    private fun handleSetMost(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.ShowSetMostDialog(idol) }
    }

    /**
     * 최애 설정 확정 (다이얼로그에서 확인 버튼 클릭 시)
     */
    private fun confirmSetMost(idol: SearchIdolModel) {
        viewModelScope.launch {
            try {
                val userResourceUri = userCacheRepository.getUserResourceUri()
                if (userResourceUri == null) {
                    setEffect { SearchResultContract.Effect.ShowToast("로그인이 필요합니다.") }
                    return@launch
                }

                val currentIsMost = idol.isMost
                val idolResourceUri = if (currentIsMost) {
                    null // 최애 해제
                } else {
                    idol.resourceUri ?: "/api/v1/idols/${idol.id}/"
                }

                val result = usersRepository.updateMost(userResourceUri, idolResourceUri)
                result.onSuccess {
                    val newIsMost = !currentIsMost

                    // UI 상태 업데이트
                    updateIdolMostState(idol.id, newIsMost)

                    // 최애 설정 시 즐겨찾기도 자동으로 on
                    if (newIsMost) {
                        updateIdolFavoriteState(idol.id, true)
                    }

                    // 로컬 캐시 업데이트
                    if (newIsMost) {
                        userCacheRepository.updateMostIdolCache(
                            idolId = idol.id,
                            idolCategory = idol.category,
                            idolChartCode = idol.chartCodes?.firstOrNull()
                        )
                        // 최애 설정 시 즐겨찾기 캐시에도 추가
                        if (!userCacheRepository.getFavoriteIdolIds().contains(idol.id)) {
                            userCacheRepository.addFavoriteToCache(idol.id, -1)
                        }
                    } else {
                        userCacheRepository.updateMostIdolCache(
                            idolId = null,
                            idolCategory = null,
                            idolChartCode = null
                        )
                    }
                }.onFailure { e ->
                    setEffect { SearchResultContract.Effect.ShowToast(e.message ?: "최애 설정에 실패했습니다.") }
                }
            } catch (e: Exception) {
                setEffect { SearchResultContract.Effect.ShowToast(e.message ?: "최애 설정에 실패했습니다.") }
            }
        }
    }

    private fun updateIdolMostState(idolId: Int, isMost: Boolean) {
        setState {
            copy(
                idols = idols.map {
                    // 새로운 최애가 설정되면 기존 최애 해제
                    if (isMost && it.isMost && it.id != idolId) {
                        it.copy(isMost = false)
                    } else if (it.id == idolId) {
                        it.copy(isMost = isMost)
                    } else {
                        it
                    }
                }
            )
        }
    }

    /**
     * 아이돌 클릭 처리
     */
    private fun handleIdolClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToCommunity(idol.id) }
    }

    /**
     * 아이돌 커뮤니티 버튼 클릭
     */
    private fun handleIdolCommunityClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToCommunity(idol.id, "community") }
    }

    /**
     * 아이돌 잡담 버튼 클릭
     */
    private fun handleIdolSmallTalkClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToSmallTalk(idol.id) }
    }

    /**
     * 아이돌 스케줄 버튼 클릭
     */
    private fun handleIdolScheduleClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToSchedule(idol.id) }
    }

    /**
     * 서포트 클릭 처리
     */
    private fun handleSupportClick(support: SearchSupportModel) {
        setEffect { SearchResultContract.Effect.NavigateToSupportDetail(support.id) }
    }

    /**
     * 배경화면 클릭 처리
     */
    private fun handleWallpaperClick(wallpaper: SearchWallpaperModel) {
        setEffect { SearchResultContract.Effect.NavigateToWallpaperDetail(wallpaper.idolId) }
    }

    /**
     * 게시글 클릭 처리
     */
    private fun handleArticleClick(article: ArticleModel) {
        setEffect { SearchResultContract.Effect.NavigateToArticleDetail(article.id) }
    }

    /**
     * 아이돌 이름이 없는 배경화면에 대해 DB에서 아이돌 이름 조회
     * old 프로젝트의 SearchedWallpaperIdolViewHolder.setCategory 로직 참고
     */
    private fun loadMissingIdolNamesForWallpapers(wallpapers: List<SearchWallpaperModel>) {
        val missingIdolIds = wallpapers
            .filter { it.idolName == null }
            .map { it.idolId }
            .distinct()

        if (missingIdolIds.isEmpty()) return

        viewModelScope.launch {
            val updatedWallpapers = currentState.wallpapers.toMutableList()

            missingIdolIds.forEach { idolId ->
                try {
                    val idol = idolRepository.getIdolById(idolId)
                    if (idol != null) {
                        val index = updatedWallpapers.indexOfFirst { it.idolId == idolId }
                        if (index != -1) {
                            updatedWallpapers[index] = updatedWallpapers[index].copy(idolName = idol.name)
                        }
                    }
                } catch (e: Exception) {
                    // 조회 실패 시 무시
                }
            }

            setState { copy(wallpapers = updatedWallpapers) }
        }
    }

    /**
     * 서포트의 아이돌 정보 및 광고 타입 정보 로드
     * old 프로젝트의 SupportViewHolder.bind 및 getTypeList 로직 참고
     * - idolId로 로컬 DB에서 아이돌 정보 조회
     * - typeId로 PreferencesManager에서 광고 타입 정보 조회
     */
    private fun loadIdolInfoForSupports(supports: List<SearchSupportModel>) {
        if (supports.isEmpty()) return

        viewModelScope.launch {
            val updatedSupports = currentState.supports.toMutableList()

            // 광고 타입 리스트 로드
            val adTypeList = preferencesManager.getAdTypeList()

            // 아이돌 ID 목록 추출
            val idolIds = supports
                .mapNotNull { it.idolId }
                .distinct()

            idolIds.forEach { idolId ->
                try {
                    val idol = idolRepository.getIdolById(idolId)
                    if (idol != null) {
                        // 그룹 이름 조회 (groupId가 있는 경우)
                        val groupName = if (idol.groupId > 0 && idol.type != "S") {
                            try {
                                idolRepository.getIdolById(idol.groupId)?.name
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        // 해당 idolId를 가진 모든 서포트 업데이트
                        updatedSupports.forEachIndexed { index, support ->
                            if (support.idolId == idolId) {
                                // 광고 타입 정보 조회
                                val adType = adTypeList.find { it.id == support.typeId }

                                updatedSupports[index] = support.copy(
                                    idolName = idol.name,
                                    idolNameEn = idol.nameEn,
                                    idolNameJp = idol.nameJp,
                                    idolNameZh = idol.nameZh,
                                    idolNameZhTw = idol.nameZhTw,
                                    idolType = idol.type,
                                    idolGroupId = idol.groupId,
                                    idolGroupName = groupName,
                                    adTypeName = adType?.name,
                                    adTypeCategory = adType?.category,
                                    adTypePeriod = adType?.period
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 조회 실패 시 무시
                }
            }

            // idolId가 없는 서포트도 광고 타입 정보는 업데이트
            updatedSupports.forEachIndexed { index, support ->
                if (support.idolId == null && support.typeId != null && support.adTypeName == null) {
                    val adType = adTypeList.find { it.id == support.typeId }
                    if (adType != null) {
                        updatedSupports[index] = support.copy(
                            adTypeName = adType.name,
                            adTypeCategory = adType.category,
                            adTypePeriod = adType.period
                        )
                    }
                }
            }

            setState { copy(supports = updatedSupports) }
        }
    }

    /**
     * ArticleUpdateManager의 이벤트를 구독하여 게시글 상태를 실시간으로 업데이트
     */
    private fun observeArticleUpdates() {
        articleUpdateManager.articleUpdateEvent
            .onEach { event ->
                when (event) {
                    is ArticleUpdateEvent.Updated -> {
                        updateArticleInList(event)
                    }
                    is ArticleUpdateEvent.Deleted -> {
                        removeArticleFromList(event.articleId)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 게시글 업데이트 이벤트 처리
     */
    private fun updateArticleInList(event: ArticleUpdateEvent.Updated) {
        setState {
            copy(
                // smallTalks 리스트 업데이트
                smallTalks = smallTalks.map { article ->
                    if (article.id == event.articleId) {
                        article.copy(
                            likeCount = event.likeCount ?: article.likeCount,
                            commentCount = event.commentCount ?: article.commentCount,
                            heart = event.heart ?: article.heart
                        )
                    } else {
                        article
                    }
                },
                // articles 리스트 업데이트
                articles = articles.map { article ->
                    if (article.id == event.articleId) {
                        article.copy(
                            likeCount = event.likeCount ?: article.likeCount,
                            commentCount = event.commentCount ?: article.commentCount,
                            heart = event.heart ?: article.heart
                        )
                    } else {
                        article
                    }
                }
            )
        }
    }

    /**
     * 게시글 삭제 이벤트 처리
     */
    private fun removeArticleFromList(articleId: String) {
        setState {
            copy(
                smallTalks = smallTalks.filter { it.id != articleId },
                articles = articles.filter { it.id != articleId }
            )
        }
    }

    /**
     * UserCacheRepository의 favoriteIdolIds Flow를 구독하여
     * 아이돌 목록의 isFavorite 상태를 실시간으로 업데이트
     */
    private fun observeFavoriteChanges() {
        userCacheRepository.favoriteIdolIds
            .onEach { favoriteIds ->
                // 현재 아이돌 목록의 즐겨찾기 상태를 로컬 캐시 기준으로 업데이트
                if (currentState.idols.isNotEmpty()) {
                    setState {
                        copy(
                            idols = idols.map { idol ->
                                idol.copy(isFavorite = favoriteIds.contains(idol.id))
                            }
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * UserCacheRepository의 mostIdolId Flow를 구독하여
     * 아이돌 목록의 isMost 상태를 실시간으로 업데이트
     */
    private fun observeMostChanges() {
        userCacheRepository.mostIdolId
            .onEach { mostId ->
                // 현재 아이돌 목록의 최애 상태를 로컬 캐시 기준으로 업데이트
                if (currentState.idols.isNotEmpty()) {
                    setState {
                        copy(
                            idols = idols.map { idol ->
                                idol.copy(isMost = idol.id == mostId)
                            }
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
