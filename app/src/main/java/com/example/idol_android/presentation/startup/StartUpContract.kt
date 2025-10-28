package com.example.idol_android.presentation.startup

import com.example.idol_android.base.UiEffect
import com.example.idol_android.base.UiIntent
import com.example.idol_android.base.UiState

/**
 * StartUp 화면의 MVI Contract.
 * old 프로젝트의 StartupActivity 로직을 MVI 패턴으로 구현.
 */
class StartUpContract {

    /**
     * UI State for StartUp screen.
     *
     * @property progress 프로그레스바 진행률 (0.0 ~ 1.0)
     * @property isLoading 로딩 중 여부
     * @property error 에러 메시지
     * @property currentStep 현재 진행 중인 단계
     * @property apiCallsCompleted 완료된 API 호출 수
     * @property totalApiCalls 전체 API 호출 수
     */
    data class State(
        val progress: Float = 0f,
        val isLoading: Boolean = true,
        val error: String? = null,
        val currentStep: String = "Initializing...",
        val apiCallsCompleted: Int = 0,
        val totalApiCalls: Int = 0
    ) : UiState

    /**
     * User intents (사용자 액션).
     */
    sealed class Intent : UiIntent {
        /**
         * 초기화 시작.
         */
        data object Initialize : Intent()

        /**
         * 재시도.
         */
        data object Retry : Intent()
    }

    /**
     * Side effects (일회성 이벤트).
     */
    sealed class Effect : UiEffect {
        /**
         * 메인 화면으로 이동.
         */
        data object NavigateToMain : Effect()

        /**
         * 에러 표시.
         */
        data class ShowError(val message: String) : Effect()
    }
}
