package net.ib.mn.data.repository

import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.ib.mn.data.remote.dto.UserSelfData
import net.ib.mn.data.remote.dto.toEntity
import net.ib.mn.domain.model.MostPicksModel
import net.ib.mn.presentation.main.myfavorite.MyFavoriteContract
import net.ib.mn.util.NumberFormatUtil
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 정보 인메모리 캐시 Repository (SharedPreference 백업 지원)
 *
 * getUserSelf API 호출 시점에 로드된 UserSelfData를 메모리에 캐싱하여
 * 앱 전역에서 빠르게 접근할 수 있도록 함.
 *
 * 주요 기능:
 * 1. UserSelfData 캐싱 (Thread-safe)
 * 2. Flow를 통한 반응형 데이터 제공
 * 3. 사용자 기본 정보, 하트 보유 정보, 최애 아이돌 정보 캐싱
 * 4. favoriteIdolIds 리스트 캐싱
 * 5. **SharedPreference에 자동 백업 및 복원** (앱 재시작 시 데이터 유지)
 *
 * 아키텍처:
 * - UserCacheRepository = Single Source of Truth (메모리 캐시 + Flow 반응형)
 * - PreferencesManager = Persistent Storage (디스크 백업)
 * - 모든 데이터 변경 시 SharedPreference에 자동 동기화
 * - 앱 시작/캐시 손실 시 SharedPreference에서 복원
 */
@Singleton
class UserCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepositoryProvider: javax.inject.Provider<net.ib.mn.domain.repository.UserRepository>,
    private val favoritesRepositoryProvider: javax.inject.Provider<net.ib.mn.domain.repository.FavoritesRepository>,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager
) {

    companion object {
        private const val TAG = "UserCacheRepository"
    }

    // IO 작업용 CoroutineScope
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    // 좋아하는 아이돌 ID 리스트 (추후 확장 가능)
    private val _favoriteIdolIds = MutableStateFlow<List<Int>>(emptyList())
    val favoriteIdolIds: Flow<List<Int>> = _favoriteIdolIds.asStateFlow()

    // 즐겨찾기 ID 맵 (idol ID → favorite ID) - 즐겨찾기 삭제 시 사용
    private val _favoriteIdMap = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val favoriteIdMap: Flow<Map<Int, Int>> = _favoriteIdMap.asStateFlow()

    // 사용자 선택 카테고리 (GLOBALS 탭 필터링용)
    private val _defaultCategory = MutableStateFlow<String?>(null)
    val defaultCategory: Flow<String?> = _defaultCategory.asStateFlow()

    // 사용자 선택 차트 코드 (랭킹 탭 초기 선택용)
    private val _defaultChartCode = MutableStateFlow<String?>(null)
    val defaultChartCode: Flow<String?> = _defaultChartCode.asStateFlow()

    // 픽 참여 정보 (Support Bias Bar용)
    private val _mostPicksModel = MutableStateFlow<MostPicksModel?>(null)
    val mostPicksModel: Flow<MostPicksModel?> = _mostPicksModel.asStateFlow()

    init {
        // 앱 시작 시 SharedPreference에서 데이터 복원
        ioScope.launch {
            restoreFromPreferences()
        }
    }

    /**
     * SharedPreference에서 모든 캐시 데이터 복원
     */
    private suspend fun restoreFromPreferences() {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "🔄 Restoring cache from SharedPreference...")

            // UserSelfData 복원
            val userData = preferencesManager.getUserSelfData()
            if (userData != null) {
                _userData.value = userData
                Log.d(TAG, "✓ Restored UserSelfData: ${userData.email}")
            }

            // 최애 아이돌 정보 복원
            val mostIdolId = preferencesManager.getMostIdolId()
            val mostIdolCategory = preferencesManager.getMostIdolCategory()
            val mostIdolChartCode = preferencesManager.getMostIdolChartCode()

            if (mostIdolId != null) {
                _mostIdolId.value = mostIdolId
                _mostIdolCategory.value = mostIdolCategory
                _mostIdolChartCode.value = mostIdolChartCode
                Log.d(TAG, "✓ Restored most idol: id=$mostIdolId, category=$mostIdolCategory, chartCode=$mostIdolChartCode")
            }

            // 즐겨찾기 아이돌 ID 리스트 복원
            val favoriteIds = preferencesManager.getFavoriteIdolIds()
            if (favoriteIds.isNotEmpty()) {
                _favoriteIdolIds.value = favoriteIds
                Log.d(TAG, "✓ Restored ${favoriteIds.size} favorite idol IDs")
            }

            // 기본 카테고리 복원
            val defaultCategory = preferencesManager.getDefaultCategory()
            if (defaultCategory != null) {
                _defaultCategory.value = defaultCategory
                Log.d(TAG, "✓ Restored default category: $defaultCategory")
            }

            // 기본 차트 코드 복원
            val defaultChartCode = preferencesManager.getDefaultChartCode()
            if (defaultChartCode != null) {
                _defaultChartCode.value = defaultChartCode
                Log.d(TAG, "✓ Restored default chart code: $defaultChartCode")
            }

            // MostPicks 정보 복원
            val mostPicksModel = preferencesManager.getMostPicksModel()
            if (mostPicksModel != null) {
                _mostPicksModel.value = mostPicksModel
                Log.d(TAG, "✓ Restored most picks: $mostPicksModel")
            }

            Log.d(TAG, "✅ Cache restoration completed")
            Log.d(TAG, "========================================")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restore cache from SharedPreference: ${e.message}", e)
        }
    }

    /**
     * getUserSelf 호출 시 사용자 데이터 저장
     *
     * 자동으로 수행되는 작업:
     * 1. 사용자 데이터 캐싱
     * 2. 최애 아이돌 정보 캐싱
     * 3. 하트 정보 캐싱
     * 4. 최애 아이돌을 로컬 DB에 upsert
     * 5. **SharedPreference에 자동 백업**
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
        } ?: run {
            _mostIdolId.value = null
            _mostIdolCategory.value = null
            _mostIdolChartCode.value = null
            Log.w(TAG, "⚠️ No most idol set")
        }

        // **SharedPreference에 자동 백업**
        saveToPreferences(userData)
    }

    /**
     * SharedPreference에 모든 캐시 데이터 백업
     */
    private suspend fun saveToPreferences(userData: UserSelfData) {
        try {
            // UserSelfData 저장 (하트 정보 포함)
            preferencesManager.saveUserSelfData(userData)

            // 최애 아이돌 정보 저장
            val mostIdolId = _mostIdolId.value
            val mostIdolCategory = _mostIdolCategory.value
            val mostIdolChartCode = _mostIdolChartCode.value
            preferencesManager.saveMostIdolInfo(mostIdolId, mostIdolCategory, mostIdolChartCode)

            Log.d(TAG, "💾 Backed up to SharedPreference")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to backup to SharedPreference: ${e.message}", e)
        }
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
     * StrongHeart 가져오기 (동기)
     */
    fun getStrongHeart(): Long {
        return _userData.value?.strongHeart ?: 0L
    }

    /**
     * WeakHeart 가져오기 (동기)
     */
    fun getWeakHeart(): Long {
        return _userData.value?.weakHeart ?: 0L
    }

    /**
     * 좋아하는 아이돌 ID 리스트 설정
     *
     * @param idolIds 좋아하는 아이돌 ID 리스트
     */
    fun setFavoriteIdolIds(idolIds: List<Int>) {
        _favoriteIdolIds.value = idolIds
        Log.d(TAG, "✅ Favorite idol IDs cached: ${idolIds.size} idols")

        // SharedPreference에 백업
        ioScope.launch {
            try {
                preferencesManager.saveFavoriteIdolIds(idolIds)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save favorite IDs to SharedPreference: ${e.message}", e)
            }
        }
    }

    /**
     * 좋아하는 아이돌 ID 리스트 가져오기 (동기)
     */
    fun getFavoriteIdolIds(): List<Int> {
        return _favoriteIdolIds.value
    }

    /**
     * 즐겨찾기 ID 맵 설정 (idol ID → favorite ID)
     *
     * @param idMap idol ID → favorite ID 맵
     */
    fun setFavoriteIdMap(idMap: Map<Int, Int>) {
        _favoriteIdMap.value = idMap
        Log.d(TAG, "✅ Favorite ID map cached: ${idMap.size} entries")
    }

    /**
     * 즐겨찾기 ID 맵 가져오기 (동기)
     */
    fun getFavoriteIdMap(): Map<Int, Int> {
        return _favoriteIdMap.value
    }

    /**
     * 특정 아이돌의 favorite ID 가져오기
     *
     * @param idolId 아이돌 ID
     * @return favorite ID (없으면 null)
     */
    fun getFavoriteId(idolId: Int): Int? {
        return _favoriteIdMap.value[idolId]
    }

    /**
     * 즐겨찾기에 아이돌 추가 (로컬 캐시)
     *
     * @param idolId 아이돌 ID
     * @param favoriteId API에서 반환된 favorite ID
     */
    fun addFavoriteToCache(idolId: Int, favoriteId: Int) {
        // ID 리스트에 추가
        val currentIds = _favoriteIdolIds.value.toMutableList()
        if (!currentIds.contains(idolId)) {
            currentIds.add(idolId)
            _favoriteIdolIds.value = currentIds
        }

        // ID 맵에 추가
        val currentMap = _favoriteIdMap.value.toMutableMap()
        currentMap[idolId] = favoriteId
        _favoriteIdMap.value = currentMap

        Log.d(TAG, "✅ Added favorite to cache: idolId=$idolId, favoriteId=$favoriteId")

        // SharedPreference에 백업
        ioScope.launch {
            try {
                preferencesManager.saveFavoriteIdolIds(currentIds)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save favorite IDs to SharedPreference: ${e.message}", e)
            }
        }
    }

    /**
     * 즐겨찾기에서 아이돌 제거 (로컬 캐시)
     *
     * @param idolId 아이돌 ID
     */
    fun removeFavoriteFromCache(idolId: Int) {
        // ID 리스트에서 제거
        val currentIds = _favoriteIdolIds.value.toMutableList()
        currentIds.remove(idolId)
        _favoriteIdolIds.value = currentIds

        // ID 맵에서 제거
        val currentMap = _favoriteIdMap.value.toMutableMap()
        currentMap.remove(idolId)
        _favoriteIdMap.value = currentMap

        Log.d(TAG, "✅ Removed favorite from cache: idolId=$idolId")

        // SharedPreference에 백업
        ioScope.launch {
            try {
                preferencesManager.saveFavoriteIdolIds(currentIds)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save favorite IDs to SharedPreference: ${e.message}", e)
            }
        }
    }

    /**
     * 하트 정보 업데이트 (투표 후 사용)
     *
     * @param strongHeart 업데이트된 strongHeart
     * @param weakHeart 업데이트된 weakHeart
     */
    fun updateHeartInfo(strongHeart: Long, weakHeart: Long) {
        val currentUserData = _userData.value ?: return

        // UserSelfData 업데이트
        val updatedUserData = currentUserData.copy(
            strongHeart = strongHeart,
            weakHeart = weakHeart
        )
        _userData.value = updatedUserData

        Log.d(TAG, "💗 Heart info updated:")
        Log.d(TAG, "  - StrongHeart: $strongHeart")
        Log.d(TAG, "  - WeakHeart: $weakHeart")

        // SharedPreference에 백업
        ioScope.launch {
            try {
                preferencesManager.saveUserSelfData(updatedUserData)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save user data to SharedPreference: ${e.message}", e)
            }
        }
    }

    /**
     * 기본 카테고리 설정 (GLOBALS 탭 필터링용)
     *
     * @param category 카테고리 (M/F)
     */
    fun setDefaultCategory(category: String) {
        _defaultCategory.value = category
        Log.d(TAG, "✅ Default category set: $category")

        // SharedPreference에 백업
        ioScope.launch {
            try {
                preferencesManager.setDefaultCategory(category)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save default category to SharedPreference: ${e.message}", e)
            }
        }
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

        // SharedPreference에 백업
        ioScope.launch {
            try {
                preferencesManager.setDefaultChartCode(chartCode)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save default chart code to SharedPreference: ${e.message}", e)
            }
        }
    }

    /**
     * 기본 차트 코드 가져오기 (동기)
     */
    fun getDefaultChartCode(): String? {
        return _defaultChartCode.value
    }


    /**
     * 하트 수 포맷팅
     */
    private fun formatHeartCount(count: Int): String {
        return NumberFormatUtil.formatWithComma(count)
    }

    /**
     * 모든 캐시 클리어 (로그아웃 시 사용)
     */
    fun clearAll() {
        _userData.value = null
        _mostIdolId.value = null
        _mostIdolCategory.value = null
        _mostIdolChartCode.value = null
        _favoriteIdolIds.value = emptyList()
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
        Log.d(TAG, "StrongHeart: ${_userData.value?.strongHeart}")
        Log.d(TAG, "WeakHeart: ${_userData.value?.weakHeart}")
        Log.d(TAG, "Most Idol ID: ${_mostIdolId.value}")
        Log.d(TAG, "Most Idol Category: ${_mostIdolCategory.value}")
        Log.d(TAG, "Most Idol ChartCode: ${_mostIdolChartCode.value}")
        Log.d(TAG, "Favorite Idol Count: ${_favoriteIdolIds.value.size}")
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
     * MostPicksModel 설정
     */
    suspend fun setMostPicksModel(model: MostPicksModel?) {
        _mostPicksModel.value = model
        preferencesManager.saveMostPicksModel(model)
        Log.d(TAG, "✓ MostPicksModel cached: $model")
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

    /**
     * 사용자 resource URI 가져오기
     */
    fun getUserResourceUri(): String? {
        return _userData.value?.resourceUri
    }

    /**
     * 최애 아이돌 로컬 캐시 업데이트
     * API 호출 성공 후 호출하여 로컬 상태 동기화
     *
     * @param idolId 새 최애 아이돌 ID (null이면 최애 해제)
     * @param idolCategory 아이돌 카테고리 (M/F)
     * @param idolChartCode 아이돌 차트 코드
     */
    fun updateMostIdolCache(
        idolId: Int?,
        idolCategory: String?,
        idolChartCode: String?
    ) {
        _mostIdolId.value = idolId
        _mostIdolCategory.value = idolCategory
        _mostIdolChartCode.value = idolChartCode

        Log.d(TAG, "✅ Most idol cache updated:")
        Log.d(TAG, "  - ID: $idolId")
        Log.d(TAG, "  - Category: $idolCategory")
        Log.d(TAG, "  - ChartCode: $idolChartCode")

        // SharedPreference에 백업
        ioScope.launch {
            try {
                preferencesManager.saveMostIdolInfo(idolId, idolCategory, idolChartCode)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save most idol info to SharedPreference: ${e.message}", e)
            }
        }
    }
}
