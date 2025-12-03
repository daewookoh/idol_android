package net.ib.mn.presentation.main

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.repository.MessageRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE

private const val TAG = "MainTopBarViewModel"

/**
 * 메인 TopBar의 타이머를 관리하는 ViewModel
 * old 프로젝트의 MainActivity.handleMessage()와 동일한 로직
 */
@HiltViewModel
class MainTopBarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _timerText = MutableStateFlow("")
    val timerText: StateFlow<String> = _timerText.asStateFlow()

    // 알림 아이콘의 new 뱃지 상태
    private val _hasNewNotification = MutableStateFlow(false)
    val hasNewNotification: StateFlow<Boolean> = _hasNewNotification.asStateFlow()

    /**
     * 새 알림이 있는지 여부 설정
     */
    fun setHasNewNotification(hasNew: Boolean) {
        _hasNewNotification.value = hasNew
    }

    /**
     * 새 알림 체크 API 호출
     * old 프로젝트의 UtilK.checkNewNotification()과 동일
     */
    fun checkNewNotification() {
        viewModelScope.launch {
            try {
                val afterDate = preferencesManager.getRecentNotificationDate()
                logD(TAG, "checkNewNotification: afterDate=$afterDate")

                val hasNew = messageRepository.checkNewNotification(afterDate)
                logD(TAG, "checkNewNotification: hasNew=$hasNew")

                _hasNewNotification.value = hasNew
            } catch (e: Exception) {
                logE(TAG, "checkNewNotification: Error", e)
            }
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var timerTask: Runnable? = null
    
    // 타이머 설정 (old 프로젝트의 setTimerConfiguration()과 동일)
    private var aggregatingTime: String? = null
    private var aggregatingTimeFormatOne: String? = null
    private var aggregatingTimeFormatFew: String? = null

    init {
        // 다국어 문자열 로드 (old 프로젝트의 setTimerConfiguration()과 동일)
        aggregatingTime = context.getString(R.string.aggregating_time)
        aggregatingTimeFormatOne = context.getString(R.string.deadline_format_one)
        aggregatingTimeFormatFew = context.getString(R.string.deadline_format_few)
    }
    
    fun startTimer() {
        stopTimer()
        
        timerTask = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 1000) // 1초마다 업데이트
            }
        }
        
        handler.post(timerTask!!)
    }
    
    fun stopTimer() {
        timerTask?.let {
            handler.removeCallbacks(it)
            timerTask = null
        }
    }
    
    private fun updateTimer() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        val nowSec = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND)

        val startSec = 23 * 3600 + 30 * 60
        val endSec = 23 * 3600 + 59 * 60 + 59

        val isAggregating = nowSec in startSec..endSec

        val strTime = if (isAggregating) {
            aggregatingTime ?: ""
        } else {
            val remainSec = if (nowSec < startSec) startSec - nowSec else 24 * 3600 - nowSec + startSec
            val remainMs = remainSec * 1000L
            when {
                remainMs <= 60000 -> String.format(aggregatingTimeFormatOne ?: "", 1)
                remainMs <= 600000 -> String.format(aggregatingTimeFormatFew ?: "", remainMs / 60000 + 1)
                else -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(remainMs))
            }
        }

        viewModelScope.launch { preferencesManager.setIsAggregatingTime(isAggregating) }
        _timerText.value = strTime
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

