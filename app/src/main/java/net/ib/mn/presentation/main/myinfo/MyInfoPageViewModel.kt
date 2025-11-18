package net.ib.mn.presentation.main.myinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.repository.UserCacheRepository
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * MyInfo 페이지 ViewModel
 * UserCacheRepository의 데이터를 구독하여 UI 상태를 관리
 */
@HiltViewModel
class MyInfoPageViewModel @Inject constructor(
    private val userCacheRepository: UserCacheRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MyInfoPageViewModel"

        // 레벨별 필요 하트 (old 프로젝트의 Const.LEVEL_HEARTS와 동일)
        private val LEVEL_HEARTS = intArrayOf(
            0,          // Level 0
            100,        // Level 1
            1000,       // Level 2
            2000,       // Level 3
            3000,       // Level 4
            5000,       // Level 5
            7000,       // Level 6
            10000,      // Level 7
            20000,      // Level 8
            35000,      // Level 9
            50000,      // Level 10
            65000,      // Level 11
            80000,      // Level 12
            100000,     // Level 13
            120000,     // Level 14
            150000,     // Level 15
            190000,     // Level 16
            240000,     // Level 17
            300000,     // Level 18
            400000,     // Level 19
            500000,     // Level 20
            650000,     // Level 21
            800000,     // Level 22
            1000000,    // Level 23
            1500000,    // Level 24
            2000000,    // Level 25
            2500000,    // Level 26
            3000000,    // Level 27
            3500000,    // Level 28
            4000000,    // Level 29
            5000000,    // Level 30
            7000000,    // Level 31
            10000000,   // Level 32
            13000000,   // Level 33
            16000000,   // Level 34
            20000000,   // Level 35
            25000000,   // Level 36
            30000000,   // Level 37
            40000000,   // Level 38
            50000000,   // Level 39
            100000000   // Level 40
        )
        private const val MAX_LEVEL = 40
    }

    // UI 상태
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _profileImageUrl = MutableStateFlow("")
    val profileImageUrl: StateFlow<String> = _profileImageUrl.asStateFlow()

    private val _level = MutableStateFlow(0)
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _favoriteIdolName = MutableStateFlow("")
    val favoriteIdolName: StateFlow<String> = _favoriteIdolName.asStateFlow()

    private val _favoriteIdolSubName = MutableStateFlow("")
    val favoriteIdolSubName: StateFlow<String> = _favoriteIdolSubName.asStateFlow()

    private val _levelProgress = MutableStateFlow(0)
    val levelProgress: StateFlow<Int> = _levelProgress.asStateFlow()

    private val _levelUpText = MutableStateFlow("")
    val levelUpText: StateFlow<String> = _levelUpText.asStateFlow()

    private val _totalExp = MutableStateFlow("")
    val totalExp: StateFlow<String> = _totalExp.asStateFlow()

    private val _subscriptionName = MutableStateFlow<String?>(null)
    val subscriptionName: StateFlow<String?> = _subscriptionName.asStateFlow()

    private val _hasNewFeed = MutableStateFlow(false)
    val hasNewFeed: StateFlow<Boolean> = _hasNewFeed.asStateFlow()

    private val _heartCount = MutableStateFlow("0")
    val heartCount: StateFlow<String> = _heartCount.asStateFlow()

    private val _strongHeart = MutableStateFlow("0")
    val strongHeart: StateFlow<String> = _strongHeart.asStateFlow()

    private val _weakHeart = MutableStateFlow("0")
    val weakHeart: StateFlow<String> = _weakHeart.asStateFlow()

    private val _diaCount = MutableStateFlow("0")
    val diaCount: StateFlow<String> = _diaCount.asStateFlow()

    init {
        // UserCacheRepository의 userData를 구독
        viewModelScope.launch {
            userCacheRepository.userData.collect { userData ->
                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "📥 UserData received from cache: ${userData != null}")
                android.util.Log.d(TAG, "========================================")

                if (userData != null) {
                    android.util.Log.d(TAG, "  - Nickname: ${userData.nickname}")
                    android.util.Log.d(TAG, "  - Level: ${userData.level}")
                    android.util.Log.d(TAG, "  - LevelHeart: ${userData.levelHeart}")
                    android.util.Log.d(TAG, "  - ProfileImage: ${userData.profileImage}")
                    android.util.Log.d(TAG, "  - Most: ${userData.most?.name}")

                    // UI 상태 업데이트
                    val level = userData.level ?: 0
                    val levelHeart = userData.levelHeart ?: 0L
                    val (progress, levelUpText) = calculateLevelProgress(level, levelHeart)

                    // 최애 아이돌 이름 처리
                    val (favoriteIdolName, favoriteIdolSubName) = parseMostIdolName(userData.most?.name)

                    _userName.value = userData.nickname ?: ""
                    _profileImageUrl.value = userData.profileImage ?: ""
                    _level.value = level
                    _favoriteIdolName.value = favoriteIdolName
                    _favoriteIdolSubName.value = favoriteIdolSubName
                    _levelProgress.value = progress
                    _levelUpText.value = levelUpText
                    _totalExp.value = NumberFormat.getNumberInstance(Locale.getDefault()).format(levelHeart)
                    _subscriptionName.value = null // TODO: subscriptions 필드 추가 시 구현
                    _hasNewFeed.value = false // TODO: 피드 새 알림 로직 추가 시 구현

                    // 하트/다이아 정보 업데이트
                    val strongHeartValue = userData.strongHeart ?: 0L
                    val weakHeartValue = userData.weakHeart ?: 0L
                    val totalHeart = strongHeartValue + weakHeartValue

                    _heartCount.value = NumberFormat.getNumberInstance(Locale.getDefault()).format(totalHeart)
                    _strongHeart.value = NumberFormat.getNumberInstance(Locale.getDefault()).format(strongHeartValue)
                    _weakHeart.value = NumberFormat.getNumberInstance(Locale.getDefault()).format(weakHeartValue)
                    _diaCount.value = NumberFormat.getNumberInstance(Locale.getDefault()).format(userData.diamond ?: 0)

                    android.util.Log.d(TAG, "✅ UI state updated:")
                    android.util.Log.d(TAG, "  - Nickname: ${userData.nickname}")
                    android.util.Log.d(TAG, "  - Progress: $progress%")
                    android.util.Log.d(TAG, "  - LevelUpText: $levelUpText")
                    android.util.Log.d(TAG, "  - TotalExp: ${_totalExp.value}")
                } else {
                    // userData가 null일 때는 기본값 유지
                    android.util.Log.d(TAG, "⚠️ UserData is null, keeping default state")
                }
                android.util.Log.d(TAG, "========================================")
            }
        }
    }

    /**
     * 레벨 프로그레스 계산
     *
     * @param level 현재 레벨
     * @param levelHeart 현재 레벨 하트
     * @return Pair<진행률(0-100), 다음레벨까지남은하트>
     */
    private fun calculateLevelProgress(level: Int, levelHeart: Long): Pair<Int, String> {
        if (level >= MAX_LEVEL) {
            return Pair(100, "")
        }

        if (level >= LEVEL_HEARTS.size - 1) {
            return Pair(100, "")
        }

        val currentLevelHeart = LEVEL_HEARTS[level]
        val nextLevelHeart = LEVEL_HEARTS[level + 1]
        val total = nextLevelHeart - currentLevelHeart
        val curr = levelHeart - currentLevelHeart

        val progress = if (total > 0) {
            ((curr.toFloat() / total.toFloat()) * 100.0f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val remainingHeart = (nextLevelHeart - levelHeart).coerceAtLeast(0)
        val levelUpText = NumberFormat.getNumberInstance(Locale.US).format(remainingHeart)

        return Pair(progress, levelUpText)
    }

    /**
     * 최애 아이돌 이름 파싱
     *
     * "도경수_디오" -> ("도경수", "디오")
     * "IVE_아이브" -> ("IVE", "아이브")
     * "NewJeans" -> ("NewJeans", "")
     *
     * @param name 최애 아이돌 이름 (예: "도경수_디오")
     * @return Pair<메인이름, 서브이름>
     */
    private fun parseMostIdolName(name: String?): Pair<String, String> {
        if (name.isNullOrEmpty()) {
            return Pair("", "")
        }

        return if (name.contains("_")) {
            val parts = name.split("_", limit = 2)
            Pair(parts[0], parts.getOrNull(1) ?: "")
        } else {
            Pair(name, "")
        }
    }
}
