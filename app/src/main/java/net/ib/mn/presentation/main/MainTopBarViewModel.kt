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
        // begin, end는 ConfigModel에서 가져와야 함 (현재는 임시로 테스트용 값 설정)
        // TODO: ConfigModel에서 실제 값 가져오기
        // 임시로 테스트를 위해 현재 시간 기준으로 설정
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        begin = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        end = calendar.time
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
        if (begin == null || end == null) {
            _timerText.value = ""
            return
        }
        
        val now = (Date().time + 32400000) % 86400000
        val beginTime = (begin!!.time + 32400000) % 86400000
        val endTime = (end!!.time + 32400000) % 86400000
        
        val strTime: String = if (now in beginTime..endTime) {
            aggregatingTime ?: ""
        } else {
            val time = if (endTime < now) beginTime + 86400000 else beginTime
            val deadline = time - now
            
            when {
                deadline <= 60000 -> String.format(aggregatingTimeFormatOne ?: "", 1)
                deadline <= 600000 -> String.format(
                    aggregatingTimeFormatFew ?: "",
                    deadline / 60000 + 1
                )
                else -> {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    sdf.format(Date(deadline))
                }
            }
        }
        
        _timerText.value = strTime
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

