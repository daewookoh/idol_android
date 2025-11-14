package net.ib.mn.data.repository

import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ib.mn.data.remote.dto.UserSelfData
import net.ib.mn.data.remote.dto.toEntity
import net.ib.mn.presentation.main.myfavorite.MyFavoriteContract
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 정보 인메모리 캐시 Repository
 *
 * getUserSelf API 호출 시점에 로드된 UserSelfData를 메모리에 캐싱하여
 * 앱 전역에서 빠르게 접근할 수 있도록 함.
 *
 * 주요 기능:
 * 1. UserSelfData 캐싱 (Thread-safe)
 * 2. Flow를 통한 반응형 데이터 제공
 * 3. 사용자 기본 정보, 하트 보유 정보, 최애 아이돌 정보 캐싱
 * 4. favoriteIdolIds 리스트 캐싱 (추후 확장 가능)
 */
@Singleton
class UserCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val idolDao: net.ib.mn.data.local.dao.IdolDao,
    private val chartDatabaseRepository: ChartRankingRepository,
    private val userRepositoryProvider: javax.inject.Provider<net.ib.mn.domain.repository.UserRepository>,
    private val favoritesRepositoryProvider: javax.inject.Provider<net.ib.mn.domain.repository.FavoritesRepository>
) {

    companion object {
        private const val TAG = "UserCacheRepository"
    }

    // 사용자 정보 캐시
    private val _userData = MutableStateFlow<UserSelfData?>(null)
    val userData: Flow<UserSelfData?> = _userData.asStateFlow()

    // 최애 아이돌 ID 캐시
    private val _mostIdolId = MutableStateFlow<Int?>(null)
    val mostIdolId: Flow<Int?> = _mostIdolId.asStateFlow()

    // 최애 아이돌 카테고리 캐시 (M/F)
    private val _mostIdolCategory = MutableStateFlow<String?>(null)
    val mostIdolCategory: Flow<String?> = _mostIdolCategory.asStateFlow()

    // 최애 아이돌 차트 코드 캐시
    private val _mostIdolChartCode = MutableStateFlow<String?>(null)
    val mostIdolChartCode: Flow<String?> = _mostIdolChartCode.asStateFlow()

    // 최애 아이돌 MostFavoriteIdol 캐시
    private val _mostFavoriteIdol = MutableStateFlow<MyFavoriteContract.MostFavoriteIdol?>(null)
    val mostFavoriteIdol: Flow<MyFavoriteContract.MostFavoriteIdol?> = _mostFavoriteIdol.asStateFlow()

    // 좋아하는 아이돌 ID 리스트 (추후 확장 가능)
    private val _favoriteIdolIds = MutableStateFlow<List<Int>>(emptyList())
    val favoriteIdolIds: Flow<List<Int>> = _favoriteIdolIds.asStateFlow()

    // 하트 정보 (strongHeart, weakHeart)
    private val _heartInfo = MutableStateFlow<HeartInfo?>(null)
    val heartInfo: Flow<HeartInfo?> = _heartInfo.asStateFlow()

    // 사용자 선택 카테고리 (GLOBALS 탭 필터링용)
    private val _defaultCategory = MutableStateFlow<String?>(null)
    val defaultCategory: Flow<String?> = _defaultCategory.asStateFlow()

    // 사용자 선택 차트 코드 (랭킹 탭 초기 선택용)
    private val _defaultChartCode = MutableStateFlow<String?>(null)
    val defaultChartCode: Flow<String?> = _defaultChartCode.asStateFlow()

    /**
     * 하트 정보 데이터 클래스
     */
    data class HeartInfo(
        val strongHeart: Long,
        val weakHeart: Long,
        val hearts: Int
    )

    /**
     * getUserSelf 호출 시 사용자 데이터 저장
     *
     * 자동으로 수행되는 작업:
     * 1. 사용자 데이터 캐싱
     * 2. 최애 아이돌 정보 캐싱
     * 3. 하트 정보 캐싱
     * 4. 최애 아이돌을 로컬 DB에 upsert (StartUpViewModel에서 이동됨)
     *
     * @param userData UserSelfData
     */
    suspend fun setUserData(userData: UserSelfData) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "💾 Caching user data")
        Log.d(TAG, "  - User ID: ${userData.id}")
        Log.d(TAG, "  - Email: ${userData.email}")
        Log.d(TAG, "  - Username: ${userData.username}")
        Log.d(TAG, "  - Nickname: ${userData.nickname}")
        Log.d(TAG, "  - StrongHeart: ${userData.strongHeart}")
        Log.d(TAG, "  - WeakHeart: ${userData.weakHeart}")
        Log.d(TAG, "  - Hearts: ${userData.hearts}")
        Log.d(TAG, "  - Most: ${userData.most?.name} (id=${userData.most?.id})")
        Log.d(TAG, "========================================")

        _userData.value = userData

        // 최애 아이돌 정보 업데이트
        userData.most?.let { most ->
            _mostIdolId.value = most.id
            _mostIdolCategory.value = most.category

            // Award/DF 코드를 제외한 첫 번째 차트 코드
            val chartCode = most.chartCodes
                ?.firstOrNull { !it.startsWith("AW_") && !it.startsWith("DF_") }
                ?: most.chartCodes?.firstOrNull()

            _mostIdolChartCode.value = chartCode

            Log.d(TAG, "✅ Most idol cached:")
            Log.d(TAG, "  - ID: ${most.id}")
            Log.d(TAG, "  - Name: ${most.name}")
            Log.d(TAG, "  - Category: ${most.category}")
            Log.d(TAG, "  - ChartCode: $chartCode")

            // 최애 아이돌을 로컬 DB에 upsert (StartUpViewModel에서 이동됨)
            try {
                val idolEntity = most.toEntity()
                idolDao.upsert(idolEntity)
                Log.d(TAG, "✅ Most idol upserted to local DB: id=${most.id}, name=${most.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to upsert most idol to DB: ${e.message}", e)
            }
        } ?: run {
            _mostIdolId.value = null
            _mostIdolCategory.value = null
            _mostIdolChartCode.value = null
            Log.w(TAG, "⚠️ No most idol set")
        }

        // 하트 정보 업데이트
        val strongHeart = userData.strongHeart ?: 0L
        val weakHeart = userData.weakHeart ?: 0L
        val hearts = userData.hearts ?: 0

        _heartInfo.value = HeartInfo(
            strongHeart = strongHeart,
            weakHeart = weakHeart,
            hearts = hearts
        )

        Log.d(TAG, "✅ Heart info cached:")
        Log.d(TAG, "  - StrongHeart: $strongHeart")
        Log.d(TAG, "  - WeakHeart: $weakHeart")
        Log.d(TAG, "  - Hearts: $hearts")

        // 최애 아이돌 RankingItemData 업데이트
        updateMostFavoriteIdol()
    }

    /**
     * 사용자 데이터 가져오기 (동기)
     */
    fun getUserData(): UserSelfData? {
        return _userData.value
    }

    /**
     * 최애 아이돌 ID 가져오기 (동기)
     */
    fun getMostIdolId(): Int? {
        return _mostIdolId.value
    }

    /**
     * 최애 아이돌 카테고리 가져오기 (동기)
     */
    fun getMostIdolCategory(): String? {
        return _mostIdolCategory.value
    }

    /**
     * 최애 아이돌 차트 코드 가져오기 (동기)
     */
    fun getMostIdolChartCode(): String? {
        return _mostIdolChartCode.value
    }

    /**
     * 하트 정보 가져오기 (동기)
     */
    fun getHeartInfo(): HeartInfo? {
        return _heartInfo.value
    }

    /**
     * 최애 아이돌 MostFavoriteIdol 가져오기 (동기)
     */
    fun getMostFavoriteIdol(): MyFavoriteContract.MostFavoriteIdol? {
        return _mostFavoriteIdol.value
    }

    /**
     * 좋아하는 아이돌 ID 리스트 설정 (추후 확장)
     *
     * @param idolIds 좋아하는 아이돌 ID 리스트
     */
    fun setFavoriteIdolIds(idolIds: List<Int>) {
        _favoriteIdolIds.value = idolIds
        Log.d(TAG, "✅ Favorite idol IDs cached: ${idolIds.size} idols")
    }

    /**
     * 좋아하는 아이돌 ID 리스트 가져오기 (동기)
     */
    fun getFavoriteIdolIds(): List<Int> {
        return _favoriteIdolIds.value
    }

    /**
     * 하트 정보 업데이트 (투표 후 사용)
     *
     * @param strongHeart 업데이트된 strongHeart
     * @param weakHeart 업데이트된 weakHeart
     */
    fun updateHeartInfo(strongHeart: Long, weakHeart: Long) {
        val currentHearts = _heartInfo.value?.hearts ?: 0
        _heartInfo.value = HeartInfo(
            strongHeart = strongHeart,
            weakHeart = weakHeart,
            hearts = currentHearts
        )

        Log.d(TAG, "💗 Heart info updated:")
        Log.d(TAG, "  - StrongHeart: $strongHeart")
        Log.d(TAG, "  - WeakHeart: $weakHeart")
    }

    /**
     * 기본 카테고리 설정 (GLOBALS 탭 필터링용)
     *
     * @param category 카테고리 (M/F)
     */
    fun setDefaultCategory(category: String) {
        _defaultCategory.value = category
        Log.d(TAG, "✅ Default category set: $category")
    }

    /**
     * 기본 카테고리 가져오기 (동기)
     */
    fun getDefaultCategory(): String? {
        return _defaultCategory.value
    }

    /**
     * 기본 차트 코드 설정 (랭킹 탭 초기 선택용)
     *
     * @param chartCode 차트 코드
     */
    fun setDefaultChartCode(chartCode: String) {
        _defaultChartCode.value = chartCode
        Log.d(TAG, "✅ Default chart code set: $chartCode")
    }

    /**
     * 기본 차트 코드 가져오기 (동기)
     */
    fun getDefaultChartCode(): String? {
        return _defaultChartCode.value
    }

    /**
     * 최애 아이돌 MostFavoriteIdol 강제 업데이트 (public)
     *
     * 외부에서 호출 가능 (예: 투표 후 즉시 업데이트)
     */
    suspend fun refreshMostFavoriteIdol() {
        updateMostFavoriteIdol()
    }

    /**
     * 최애 아이돌의 하트 수만 직접 업데이트 (비밀의 방 등 캐시가 없는 경우)
     *
     * @param voteCount 증가시킬 하트 수
     */
    fun updateMostFavoriteIdolHeart(voteCount: Long) {
        val currentMostFavoriteIdol = _mostFavoriteIdol.value
        if (currentMostFavoriteIdol != null) {
            val newHeart = (currentMostFavoriteIdol.heart ?: 0L) + voteCount
            _mostFavoriteIdol.value = currentMostFavoriteIdol.copy(heart = newHeart)
        }
    }

    /**
     * 최애 아이돌 MostFavoriteIdol 업데이트
     *
     * chartCode의 rankItems에서 mostIdolId를 찾아서 업데이트하거나,
     * 없으면 DB에서 IdolEntity를 가져와서 가공
     */
    private suspend fun updateMostFavoriteIdol() {
        val mostIdolId = _mostIdolId.value
        val chartCode = _mostIdolChartCode.value

        if (mostIdolId == null) {
            _mostFavoriteIdol.value = null
            return
        }

        try {
            // 1. Room DB에서 차트 데이터 가져오기
            val chartData = chartCode?.let { chartDatabaseRepository.getChartData(it) }
            val rankItem = chartData?.rankItems?.find { it.id == mostIdolId.toString() }

            if (rankItem != null) {
                // rankItem이 있는 경우: MostFavoriteIdol로 변환
                _mostFavoriteIdol.value = MyFavoriteContract.MostFavoriteIdol(
                    idolId = mostIdolId,
                    name = rankItem.name,
                    top3ImageUrls = rankItem.top3ImageUrls,
                    top3VideoUrls = rankItem.top3VideoUrls,
                    rank = rankItem.rank,
                    heart = rankItem.heartCount,
                    chartCode = chartCode,
                    imageUrl = rankItem.photoUrl
                )
            } else {
                // rankItem이 없는 경우: IdolDao에서 가져와서 가공
                val idolEntity = idolDao.getIdolById(mostIdolId)

                if (idolEntity != null) {
                    _mostFavoriteIdol.value = MyFavoriteContract.MostFavoriteIdol(
                        idolId = mostIdolId,
                        name = idolEntity.name,
                        top3ImageUrls = net.ib.mn.util.IdolImageUtil.getTop3ImageUrls(idolEntity),
                        top3VideoUrls = net.ib.mn.util.IdolImageUtil.getTop3VideoUrls(idolEntity),
                        rank = null,
                        heart = idolEntity.heart,
                        chartCode = chartCode,
                        imageUrl = idolEntity.imageUrl
                    )
                } else {
                    _mostFavoriteIdol.value = null
                }
            }
        } catch (e: Exception) {
            _mostFavoriteIdol.value = null
            Log.e(TAG, "❌ Failed to update most favorite idol: ${e.message}", e)
        }
    }

    /**
     * 하트 수 포맷팅
     */
    private fun formatHeartCount(count: Int): String {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(count)
    }

    /**
     * 모든 캐시 클리어 (로그아웃 시 사용)
     */
    fun clearAll() {
        _userData.value = null
        _mostIdolId.value = null
        _mostIdolCategory.value = null
        _mostIdolChartCode.value = null
        _mostFavoriteIdol.value = null
        _favoriteIdolIds.value = emptyList()
        _heartInfo.value = null
        _defaultCategory.value = null
        _defaultChartCode.value = null
        Log.d(TAG, "🗑️ All user cache cleared")
    }

    /**
     * 캐시 상태 로깅 (디버깅용)
     */
    fun logCacheStatus() {
        Log.d(TAG, "========== User Cache Status ==========")
        Log.d(TAG, "User ID: ${_userData.value?.id}")
        Log.d(TAG, "Email: ${_userData.value?.email}")
        Log.d(TAG, "Most Idol ID: ${_mostIdolId.value}")
        Log.d(TAG, "Most Idol Category: ${_mostIdolCategory.value}")
        Log.d(TAG, "Most Idol ChartCode: ${_mostIdolChartCode.value}")
        Log.d(TAG, "Most Favorite Idol: ${_mostFavoriteIdol.value?.name} (id=${_mostFavoriteIdol.value?.idolId})")
        Log.d(TAG, "Favorite Idol Count: ${_favoriteIdolIds.value.size}")
        Log.d(TAG, "Heart Info: ${_heartInfo.value}")
        Log.d(TAG, "=========================================")
    }

    /**
     * UserSelf 최신 데이터 가져오기 (백그라운드 갱신용)
     */
    suspend fun refreshUserData() {
        try {
            val result = userRepositoryProvider.get().loadAndSaveUserSelf("no-cache")
            result.getOrThrow()
            Log.d(TAG, "✅ UserSelf refreshed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to refresh UserSelf: ${e.message}", e)
            throw e
        }
    }

    /**
     * 즐겨찾기 목록 최신화 (백그라운드 갱신용)
     */
    suspend fun refreshFavoriteIdols() {
        try {
            val result = favoritesRepositoryProvider.get().loadAndSaveFavoriteSelf()
            result.getOrThrow()
            Log.d(TAG, "✅ Favorites refreshed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to refresh Favorites: ${e.message}", e)
            throw e
        }
    }
}
