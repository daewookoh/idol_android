package com.example.idol_android.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI architecture.
 *
 * @param STATE The type of UI state
 * @param INTENT The type of user intents
 * @param EFFECT The type of side effects
 */
abstract class BaseViewModel<STATE : UiState, INTENT : UiIntent, EFFECT : UiEffect> : ViewModel() {

    /**
     * Initial state that must be provided by child classes.
     */
    abstract fun createInitialState(): STATE

    /**
     * Handle user intents. Must be implemented by child classes.
     */
    abstract fun handleIntent(intent: INTENT)

    private val initialState: STATE by lazy { createInitialState() }

    private val _uiState: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    val uiState: StateFlow<STATE> = _uiState.asStateFlow()

    private val _effect: Channel<EFFECT> = Channel()
    val effect = _effect.receiveAsFlow()

    /**
     * Get current state value.
     */
    protected val currentState: STATE
        get() = uiState.value

    /**
     * Set new state.
     */
    protected fun setState(reduce: STATE.() -> STATE) {
        val newState = currentState.reduce()
        _uiState.value = newState
    }

    /**
     * Send one-time effect.
     */
    protected fun setEffect(builder: () -> EFFECT) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }

    /**
     * Public method to receive intents from UI.
     */
    fun sendIntent(intent: INTENT) {
        handleIntent(intent)
    }
}
