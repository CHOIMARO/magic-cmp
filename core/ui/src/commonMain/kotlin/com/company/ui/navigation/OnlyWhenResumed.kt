package com.company.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/*
 * 화면 이동 콜백을 "그 화면이 RESUMED일 때만" 실행하게 감싸는 함수들이다.
 *
 * 화면 전환이 시작되면 떠나는 화면은 RESUMED가 아니다. 그래서 버튼을 빠르게 두 번 눌러도
 * 두 번째 이동은 무시된다. AndroidX Lifecycle의 dropUnlessResumed와 같은 규칙이지만,
 * Composable이 아니고 인자가 있는 콜백도 감쌀 수 있다.
 *
 * 사용자 입력 없이 앱이 시작하는 이동(예: 오래 걸리는 처리가 끝난 뒤 다음 화면 열기)에는 쓰지 않는다.
 * 전환 중에 도착하면 그 이동까지 버려지기 때문이다.
 */

/**
 * Returns an action that runs [action] only while this screen is RESUMED.
 *
 * @param action Navigation action of the screen.
 */
fun LifecycleOwner.onlyWhenResumed(action: () -> Unit): () -> Unit = {
    if (isResumed) action()
}

/**
 * Returns an action that runs [action] only while this screen is RESUMED.
 *
 * @param action Navigation action of the screen. It takes one argument.
 */
fun <T> LifecycleOwner.onlyWhenResumed(action: (T) -> Unit): (T) -> Unit = { value ->
    if (isResumed) action(value)
}

/**
 * Returns an action that runs [action] only while this screen is RESUMED.
 *
 * @param action Navigation action of the screen. It takes two arguments.
 */
fun <A, B> LifecycleOwner.onlyWhenResumed(action: (A, B) -> Unit): (A, B) -> Unit = { first, second ->
    if (isResumed) action(first, second)
}

private val LifecycleOwner.isResumed: Boolean
    get() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
