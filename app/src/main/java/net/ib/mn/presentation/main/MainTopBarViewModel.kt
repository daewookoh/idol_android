package net.ib.mn.presentation.main

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 메인 TopBar의 타이머를 관리하는 ViewModel
 * old 프로젝트의 MainActivity.handleMessage()와 동일한 로직
 */
@HiltViewModel
class MainTopBarViewModel @Inject constructor() : ViewModel() {
    
    private val _timerText = MutableStateFlow("")
    val timerText: StateFlow<String> = _timerText.asStateFlow()
    
    private val handler = Handler(Looper.getMainLooper())
    private var timerTask: Runnable? = null
    
    // 타이머 설정 (old 프로젝트의 setTimerConfiguration()과 동일)
    private var aggregatingTime: String? = null
    private var aggregatingTimeFormatOne: String? = null
    private var aggregatingTimeFormatFew: String? = null
    private var begin: Date? = null
    private var end: Date? = null
    
    init {
        // 기본값 설정 (나중에 ConfigModel에서 가져올 수 있음)
        aggregatingTime = "집계중"
        aggregatingTimeFormatOne = "%d분 후"
        aggregatingTimeFormatFew = "%d분 후"
        
        // 한국시간 기준으로 집계 시간 설정 (23:30-24:00)
        // TODO: ConfigModel에서 실제 값 가져오기 (inactiveBegin, inactiveEnd)
        // begin과 end는 매일 업데이트되므로, updateTimer에서 매번 계산하도록 변경
        // 여기서는 null로 초기화하고, updateTimer에서 동적으로 계산
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
        // old 프로젝트와 동일한 시간 계산 방식
        // 한국시간 기준으로 begin과 end 계산 (매일 업데이트)
        val koreaTimeZone = TimeZone.getTimeZone("Asia/Seoul")
        val calendar = Calendar.getInstance(koreaTimeZone)
        
        // begin: 오늘 23:30
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        begin = calendar.time
        
        // end: 내일 00:00 (24:00)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        end = calendar.time
        
        // old 프로젝트의 handleMessage()와 동일한 로직
        val now = (Date().time + 32400000) % 86400000
        val beginTime = (begin!!.time + 32400000) % 86400000
        val endTime = (end!!.time + 32400000) % 86400000
        
        val strTime: String = if (now in beginTime..endTime) {
            // 집계 시간 (23:30-24:00)
            aggregatingTime ?: ""
        } else {
            // 집계 시간이 아닐 때: begin까지 남은 시간 표시 (00:00:00 형식)
            val time = if (endTime < now) beginTime + 86400000 else beginTime
            val deadline = time - now
            
            // 항상 00:00:00 형식으로 표시
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(deadline))
        }
        
        _timerText.value = strTime
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

