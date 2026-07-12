package com.hllous.plantravel.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ticks every time the app returns to the foreground. Realtime channels (Postgres Changes,
 * Broadcast) can silently miss events while the process is backgrounded and the socket is
 * suspended/reconnected, so every realtime observer restarts and re-fetches on each tick
 * instead of relying solely on push events.
 */
@Singleton
class AppForegroundSignal @Inject constructor() {
    private val _ticks = MutableStateFlow(0)
    val ticks: StateFlow<Int> = _ticks.asStateFlow()

    fun notifyForeground() {
        _ticks.value++
    }
}
