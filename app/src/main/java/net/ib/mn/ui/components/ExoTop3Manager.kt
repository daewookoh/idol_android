package net.ib.mn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ExoTop3 전역 재생 관리자
 *
 * 중요 로직:
 * "한 화면에 여러개의 ExoTop3가 있을때 최근 오픈한 ExoTop3에서만 동영상이 플레이 되고, 나머지는 멈추게돼"
 *
 * 사용법:
 * 1. MainActivity에서 ExoTop3Manager를 생성하고 CompositionLocalProvider로 제공
 * 2. 각 ExoTop3는 고유 ID를 가지고 등록
 * 3. 활성화될 때 setActivePlayer(id) 호출
 * 4. 비활성화될 때 자동으로 정리
 */
object ExoTop3Manager {
    private val _activePlayerId = MutableStateFlow<String?>(null)
    val activePlayerId: StateFlow<String?> = _activePlayerId.asStateFlow()

    /**
     * 활성 플레이어 설정
     *
     * @param id ExoTop3 고유 ID (예: "ranking_page_0", "ranking_page_1")
     */
    fun setActivePlayer(id: String) {
        _activePlayerId.value = id
    }

    /**
     * 활성 플레이어 해제
     */
    fun clearActivePlayer() {
        _activePlayerId.value = null
    }

    /**
     * 특정 ID가 활성 상태인지 확인
     */
    fun isActive(id: String): Boolean {
        return _activePlayerId.value == id
    }
}

/**
 * CompositionLocal for ExoTop3Manager
 */
val LocalExoTop3Manager = staticCompositionLocalOf { ExoTop3Manager }

/**
 * ExoTop3 활성화 관리를 위한 Composable 유틸리티
 *
 * @param id 고유 ExoTop3 ID
 * @param isVisible 현재 화면에 표시 여부
 * @param onActiveChanged 활성 상태 변경 콜백
 */
@Composable
fun rememberExoTop3ActiveState(
    id: String,
    isVisible: Boolean,
    manager: ExoTop3Manager = LocalExoTop3Manager.current
): Boolean {
    DisposableEffect(id, isVisible) {
        if (isVisible) {
            manager.setActivePlayer(id)
        }

        onDispose {
            if (manager.isActive(id)) {
                manager.clearActivePlayer()
            }
        }
    }

    return remember(id) {
        manager.isActive(id)
    }
}
