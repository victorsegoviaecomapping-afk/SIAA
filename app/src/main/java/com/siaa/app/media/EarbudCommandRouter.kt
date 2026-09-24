package com.siaa.app.media

import android.content.Intent
import android.view.KeyEvent
import com.siaa.core.model.DeviceProfile
import com.siaa.core.runtime.MediaControlEvent

class EarbudCommandRouter {
    fun rawKeyCode(intent: Intent): Int? {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return null
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return null
        if (event.action != KeyEvent.ACTION_DOWN) return null
        return event.keyCode
    }

    fun fromMediaButtonIntent(intent: Intent, profile: DeviceProfile? = null): Pair<MediaControlEvent, Int>? {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return null
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return null
        if (event.action != KeyEvent.ACTION_DOWN) return null
        val keyCode = event.keyCode

        if (profile != null) {
            val calibrated = when (keyCode) {
                profile.primaryKeyCode -> MediaControlEvent.PLAY_PAUSE
                profile.secondaryKeyCode -> MediaControlEvent.NEXT
                profile.backKeyCode -> MediaControlEvent.PREVIOUS
                profile.stopKeyCode -> MediaControlEvent.STOP
                else -> null
            }
            if (calibrated != null) {
                return calibrated to keyCode
            }
        }

        val control = when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> MediaControlEvent.PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_PLAY -> MediaControlEvent.PLAY
            KeyEvent.KEYCODE_MEDIA_PAUSE -> MediaControlEvent.PAUSE
            KeyEvent.KEYCODE_MEDIA_NEXT -> MediaControlEvent.NEXT
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaControlEvent.PREVIOUS
            KeyEvent.KEYCODE_MEDIA_STOP -> MediaControlEvent.STOP
            else -> return null
        }
        return control to keyCode
    }
}
