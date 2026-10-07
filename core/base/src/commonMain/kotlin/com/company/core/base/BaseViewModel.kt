package com.company.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class State

abstract class Intent

abstract class SideEffect


abstract class BaseViewModel<S: State, I: Intent, SE: SideEffect>(
    initialState: S
) : ViewModel() {
    val currentState: S
        get() = uiState.value

    private val _uiState: MutableStateFlow<S> = MutableStateFlow(initialState)
    val uiState: StateFlow<S>
        get() = _uiState.asStateFlow()

    private val _intent: MutableSharedFlow<I> = MutableSharedFlow()
    val intent = _intent.asSharedFlow()

    private val _sideEffect: Channel<SE> = Channel()
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        subscribeIntent()
    }
    /**
     * State 설정
     */
    fun reduce(reducer: S.() -> S) { _uiState.update { it.reducer() } }

    /**
     * Intent 설정
     */
    fun postIntent(intent: I) = viewModelScope.launch { _intent.emit(intent) }

    /**
     * SideEffect 설정
     */
    fun postSideEffect(builder: () -> SE) = viewModelScope.launch { _sideEffect.send(builder()) }

    /**
     * Intent 구독
     */
    private fun subscribeIntent() = viewModelScope.launch {
        intent.collect { handleIntent(it) }
    }
    /**
     * Intent 핸들러
     */
    abstract fun handleIntent(intent: I)
}