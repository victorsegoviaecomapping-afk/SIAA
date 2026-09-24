package com.siaa.app

import com.siaa.core.runtime.MediaControlEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ObservedMediaCommand(
    val keyCode: Int,
    val event: MediaControlEvent,
    val timestamp: Long = System.currentTimeMillis()
)

class MediaDiagnostics {
    private val _lastEvent = MutableStateFlow("Sin eventos")
    val lastEvent = _lastEvent.asStateFlow()

    private val _observedCommands = MutableStateFlow<Set<String>>(emptySet())
    val observedCommands = _observedCommands.asStateFlow()

    private val _observedEvents = MutableStateFlow<List<ObservedMediaCommand>>(emptyList())
    val observedEvents = _observedEvents.asStateFlow()

    private val _lastRawKeyCode = MutableStateFlow<Int?>(null)
    val lastRawKeyCode = _lastRawKeyCode.asStateFlow()

    private val _assignedKeyCodes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val assignedKeyCodes = _assignedKeyCodes.asStateFlow()

    fun recordRawKeyCode(keyCode: Int) {
        _lastRawKeyCode.value = keyCode
        _lastEvent.value = "raw keyCode=$keyCode"
    }

    fun assignLast(role: String): Boolean {
        val code = _lastRawKeyCode.value ?: return false
        _assignedKeyCodes.value = _assignedKeyCodes.value + (role to code)
        _lastEvent.value = "keyCode=$code asignado a $role"
        return true
    }

    fun record(event: MediaControlEvent, keyCode: Int) {
        _lastRawKeyCode.value = keyCode
        val label = "keyCode=$keyCode -> ${event.name}"
        _lastEvent.value = label
        _observedCommands.value = _observedCommands.value + event.name
        _observedEvents.value = _observedEvents.value + ObservedMediaCommand(keyCode, event)
    }

    fun record(label: String) { _lastEvent.value = label }
    fun recordLabel(label: String) { _lastEvent.value = label }

    fun clear() {
        _lastEvent.value = "Sin eventos"
        _observedCommands.value = emptySet()
        _observedEvents.value = emptyList()
        _lastRawKeyCode.value = null
        _assignedKeyCodes.value = emptyMap()
    }
}
