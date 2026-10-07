package com.example.edgering.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-process state shared between the service and the UI. Resets if the process dies. */
object EdgeState {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    fun setRunning(value: Boolean) {
        _running.value = value
    }

    fun reportLatency(ms: Long) {
        _lastLatencyMs.value = ms
    }
}
