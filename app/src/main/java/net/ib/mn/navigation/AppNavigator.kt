package net.ib.mn.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

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
    val backStack: SnapshotStateList<Screen>
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
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            true
        } else {
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
}

/**
 * AppNavigator를 생성하고 기억하는 Composable 함수.
 *
 * @param startDestination 시작 화면
 */
@Composable
fun rememberAppNavigator(
    startDestination: Screen = Screen.StartUp()
): AppNavigator {
    val backStack = remember { mutableStateListOf(startDestination) }
    return remember(backStack) {
        AppNavigator(backStack)
    }
}
