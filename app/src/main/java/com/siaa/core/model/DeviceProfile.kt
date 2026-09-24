package com.siaa.core.model

import android.view.KeyEvent

/**
 * Hardware configuration profile for Bluetooth earbuds or wired headphones.
 * Allows custom mapping of media buttons to SIAA pedagogical actions.
 */
data class DeviceProfile(
    val id: Long = 1L,
    val name: String = "Default Earbuds",
    val primaryKeyCode: Int = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
    val secondaryKeyCode: Int = KeyEvent.KEYCODE_MEDIA_NEXT,
    val backKeyCode: Int = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
    val stopKeyCode: Int = KeyEvent.KEYCODE_MEDIA_STOP,
    val playPauseAvailable: Boolean = true,
    val nextAvailable: Boolean = true,
    val previousAvailable: Boolean = true
)
