package com.company.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhenResumedTest {

    private class TestScreen : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle
            get() = registry
    }

    @Test
    fun action_runsOnlyWhileResumed() {
        val screen = TestScreen()
        var count = 0
        val open = screen.onlyWhenResumed { count++ }

        screen.registry.currentState = Lifecycle.State.RESUMED
        open()
        // 화면 전환이 시작되면 떠나는 화면은 STARTED가 된다. 이때 두 번째 탭은 무시된다.
        screen.registry.currentState = Lifecycle.State.STARTED
        open()

        assertEquals(1, count)
    }

    @Test
    fun actionWithArguments_passesArgumentsWhileResumed() {
        val screen = TestScreen()
        val received = mutableListOf<Pair<String, Int>>()
        val open = screen.onlyWhenResumed { name: String, order: Int -> received += name to order }

        screen.registry.currentState = Lifecycle.State.CREATED
        open("a", 1)
        screen.registry.currentState = Lifecycle.State.RESUMED
        open("b", 2)

        assertEquals(listOf("b" to 2), received)
    }
}
