package net.ib.mn.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import net.ib.mn.util.logD

/**
 * Navigation 3 네비게이터.
 * 백스택을 직접 소유하고 제어하는 Navigation 3의 핵심 클래스.
 *
 * Navigation 3의 주요 장점:
 * 1. 개발자가 백스택을 완전히 제어 (SnapshotStateList<T>)
 * 2. 네비게이션 상태가 Compose 상태로 관리됨
 * 3. 백스택 변경이 자동으로 UI에 반영
 * 4. 멀티 백스택 지원 (탭 네비게이션 등)
 */
@Stable
class AppNavigator(
    val backStack: SnapshotStateList<Screen>,
    private var _pendingDeepLink: Screen? = null
) {
    /**
     * 새 화면으로 네비게이션
     */
    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    /**
     * 특정 화면으로 이동하면서 그 화면까지의 스택을 제거
     * Nav2의 popUpTo(inclusive = true)와 유사
     */
    fun navigateAndClearStack(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    /**
     * 이전 화면으로 돌아가기
     * @return 돌아갈 수 있으면 true, 아니면 false
     */
    fun popBackStack(): Boolean {
        logD("AppNavigator", "popBackStack() called, backStack.size=${backStack.size}, backStack=${backStack.map { it::class.simpleName }}")
        return if (backStack.size > 1) {
            val removed = backStack.removeAt(backStack.lastIndex)
            logD("AppNavigator", "popBackStack() removed: ${removed::class.simpleName}, new size=${backStack.size}")
            true
        } else {
            logD("AppNavigator", "popBackStack() failed - backStack.size <= 1")
            false
        }
    }

    /**
     * 현재 화면 가져오기
     */
    val currentScreen: Screen?
        get() = backStack.lastOrNull()

    /**
     * 백스택 크기
     */
    val stackSize: Int
        get() = backStack.size

    /**
     * 대기 중인 딥링크가 있으면 가져오고 소비 (한 번만 사용)
     */
    fun consumePendingDeepLink(): Screen? {
        val deepLink = _pendingDeepLink
        _pendingDeepLink = null
        return deepLink
    }

    /**
     * 대기 중인 딥링크가 있는지 확인
     */
    fun hasPendingDeepLink(): Boolean = _pendingDeepLink != null
}

/**
 * AppNavigator를 생성하고 기억하는 Composable 함수.
 *
 * @param startDestination 시작 화면
 * @param pendingDeepLink 대기 중인 딥링크 화면 (StartUp 완료 후 이동)
 */
@Composable
fun rememberAppNavigator(
    startDestination: Screen = Screen.StartUp(),
    pendingDeepLink: Screen? = null
): AppNavigator {
    val backStack = remember { mutableStateListOf(startDestination) }
    return remember(backStack, pendingDeepLink) {
        AppNavigator(backStack, pendingDeepLink)
    }
}
