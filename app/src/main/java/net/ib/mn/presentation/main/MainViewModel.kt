package net.ib.mn.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.UserInfo
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "USER_INFO"
    }

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    private val _logoutCompleted = MutableStateFlow(false)
    val logoutCompleted: StateFlow<Boolean> = _logoutCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "[MainViewModel] Subscribing to DataStore userInfo")
            android.util.Log.d(TAG, "========================================")

            // DataStore의 userInfo를 구독하여 _userInfo 업데이트
            preferencesManager.userInfo.collect { info ->
                android.util.Log.d(TAG, "[MainViewModel] DataStore userInfo received")

                if (info != null) {
                    android.util.Log.d(TAG, "[MainViewModel] ✓ User info updated from DataStore:")
                    android.util.Log.d(TAG, "[MainViewModel]   - ID: ${info.id}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Email: ${info.email}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Username: ${info.username}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Nickname: ${info.nickname}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Hearts: ${info.hearts}")
                    _userInfo.value = info
                } else {
                    android.util.Log.w(TAG, "[MainViewModel] ⚠️ UserInfo is null")
                    _userInfo.value = null
                }
            }
        }
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

                // 로그아웃 완료 플래그 설정
                _logoutCompleted.value = true

                android.util.Log.d(TAG, "[MainViewModel] ✓ Logout completed successfully")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[MainViewModel] ❌ Logout failed: ${e.message}", e)
            }
        }
    }
}
