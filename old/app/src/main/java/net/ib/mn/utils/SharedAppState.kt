package net.ib.mn.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.ib.mn.core.data.repository.MessagesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedAppState @Inject constructor() {
    // 안읽은 공지가 있는지
    private val _hasUnreadNotice = MutableStateFlow<Boolean>(false)
    val hasUnreadNotice: StateFlow<Boolean> get() = _hasUnreadNotice

    // 안읽은 이벤트가 있는지
    private val _hasUnreadEvent = MutableStateFlow<Boolean>(false)
    val hasUnreadEvent: StateFlow<Boolean> get() = _hasUnreadEvent

    // 출석체크 할게 있는지
    private val _isAttendanceAvailable = MutableStateFlow<Boolean>(true)
    val isAttendanceAvailable: StateFlow<Boolean> get() = _isAttendanceAvailable

    // 이미지픽 결과 갱신이 필요한지 (결과화면에서 추가투표한 경우)
    private val _refreshImagePickResult = MutableSharedFlow<Boolean>()
    val refreshImagePickResult: SharedFlow<Boolean> get() = _refreshImagePickResult

    // 출첵 푸시 설정 유도 숨김 여부
    private val _isIncitePushHidden = MutableStateFlow(false)
    val isIncitePushHidden: StateFlow<Boolean> get() = _isIncitePushHidden

    // 안읽은 알림 있는지 여부
    private val _hasUnreadNotification = MutableStateFlow<Boolean>(false)
    val hasUnreadNotification: StateFlow<Boolean> get() = _hasUnreadNotification

    // 최애 변경이 있는지 여부
    private val _isMostChanged = MutableStateFlow<Boolean>(false)
    val isMostChanged: StateFlow<Boolean> get() = _isMostChanged

    // scroll to top 이벤트 처리용
    private val _scrollToTop = MutableSharedFlow<Boolean>()
    val scrollToTop: SharedFlow<Boolean> get() = _scrollToTop

    fun setUnreadNotice(value: Boolean) {
        _hasUnreadNotice.value = value
    }

    fun setUnreadEvent(value: Boolean) {
        _hasUnreadEvent.value = value
    }

    fun setAttendance(value: Boolean) {
        _isAttendanceAvailable.value = value
    }

    suspend fun setRefreshImagePickResult(value: Boolean) {
        _refreshImagePickResult.emit(value)
    }

    fun setIncitePushHidden(value: Boolean) {
        _isIncitePushHidden.value = value
    }

    fun setUnreadNotification(value: Boolean) {
        _hasUnreadNotification.value = value
    }

    fun setMostChanged(value: Boolean) {
        _isMostChanged.value = value
    }

    suspend fun setScrollToTop(value: Boolean) {
        _scrollToTop.emit(value)
    }
}