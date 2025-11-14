package net.ib.mn.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.UserInfo
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "USER_INFO"
    }

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    private val _logoutCompleted = MutableStateFlow(false)
    val logoutCompleted: StateFlow<Boolean> = _logoutCompleted.asStateFlow()

    // 즉시 반응하는 로컬 카테고리 상태 (UI 반응성 개선)
    private val _currentCategory = MutableStateFlow<String?>(null)
    val currentCategory: StateFlow<String?> = _currentCategory.asStateFlow()

    init {
        viewModelScope.launch {
            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "[MainViewModel] Subscribing to DataStore userInfo")
            android.util.Log.d(TAG, "========================================")

            // DataStore의 userInfo를 구독하여 _userInfo 업데이트
            preferencesManager.userInfo.collect { info ->
                android.util.Log.d(TAG, "[MainViewModel] ========================================")
                android.util.Log.d(TAG, "[MainViewModel] DataStore userInfo received")
                android.util.Log.d(TAG, "[MainViewModel] ========================================")

                if (info != null) {
                    android.util.Log.d(TAG, "[MainViewModel] ✓ User info updated from DataStore:")
                    android.util.Log.d(TAG, "[MainViewModel]   - ID: ${info.id}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Email: ${info.email}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Username: ${info.username}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Nickname: ${info.nickname}")
                    android.util.Log.d(TAG, "[MainViewModel]   - ProfileImage: ${info.profileImage}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Hearts: ${info.hearts}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Diamond: ${info.diamond}")
                    android.util.Log.d(TAG, "[MainViewModel]   - StrongHeart: ${info.strongHeart}")
                    android.util.Log.d(TAG, "[MainViewModel]   - WeakHeart: ${info.weakHeart}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Level: ${info.level}")
                    android.util.Log.d(TAG, "[MainViewModel]   - LevelHeart: ${info.levelHeart}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Power: ${info.power}")
                    android.util.Log.d(TAG, "[MainViewModel]   - ResourceUri: ${info.resourceUri}")
                    android.util.Log.d(TAG, "[MainViewModel]   - PushKey: ${info.pushKey}")
                    android.util.Log.d(TAG, "[MainViewModel]   - CreatedAt: ${info.createdAt}")
                    android.util.Log.d(TAG, "[MainViewModel]   - PushFilter: ${info.pushFilter}")
                    android.util.Log.d(TAG, "[MainViewModel]   - StatusMessage: ${info.statusMessage}")
                    android.util.Log.d(TAG, "[MainViewModel]   - TS: ${info.ts}")
                    android.util.Log.d(TAG, "[MainViewModel]   - ItemNo: ${info.itemNo}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Domain: ${info.domain}")
                    android.util.Log.d(TAG, "[MainViewModel]   - GiveHeart: ${info.giveHeart}")
                    android.util.Log.d(TAG, "[MainViewModel] ========================================")
                    _userInfo.value = info
                } else {
                    android.util.Log.w(TAG, "[MainViewModel] ⚠️ UserInfo is null")
                    _userInfo.value = null
                }
            }
        }

        // DataStore의 카테고리를 구독
        viewModelScope.launch {
            preferencesManager.defaultCategory.collect { category ->
                _currentCategory.value = category
                android.util.Log.d(TAG, "[MainViewModel] ✓ Category updated: $category")
            }
        }
    }

    /**
     * 카테고리 변경 (즉시 UI 업데이트 + 백그라운드 DataStore 저장)
     */
    fun setCategory(category: String) {
        // 1. 즉시 로컬 상태 업데이트 (UI가 바로 반응)
        _currentCategory.value = category

        // 2. 백그라운드에서 DataStore에 저장
        viewModelScope.launch {
            preferencesManager.setDefaultCategory(category)
        }
    }

    /**
     * 앱이 백그라운드에서 포그라운드로 돌아올 때 호출
     */
    fun onAppResume() {
        android.util.Log.d(TAG, "[MainViewModel] ========================================")
        android.util.Log.d(TAG, "[MainViewModel] 👁️ App resumed - refreshing all ranking caches")
        android.util.Log.d(TAG, "[MainViewModel] ========================================")
    }

    /**
     * 앱이 포그라운드에서 백그라운드로 갈 때 호출
     */
    fun onAppPause() {
        android.util.Log.d(TAG, "[MainViewModel] 🙈 App paused")
    }

    /**
     * 로그아웃 처리.
     *
     * 모든 저장된 데이터를 삭제하고 로그아웃 완료 상태를 업데이트합니다.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "[MainViewModel] 🔴 Logging out - clearing all data")

                // DataStore의 모든 데이터 삭제
                preferencesManager.clearAll()

                // Room DB의 모든 차트 데이터 삭제
                chartDatabaseRepository.clearAll()

                // 로그아웃 완료 플래그 설정
                _logoutCompleted.value = true

                android.util.Log.d(TAG, "[MainViewModel] ✓ Logout completed successfully")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[MainViewModel] ❌ Logout failed: ${e.message}", e)
            }
        }
    }
}
