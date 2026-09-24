package com.siaa.app.media

import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import com.siaa.core.model.DeviceProfile
import com.siaa.core.runtime.MediaControlEvent

/**
 * Translates incoming Android media button intents (from Bluetooth or wired headsets)
 * into pedagogical SIAA events based on the active DeviceProfile.
 */
class EarbudCommandRouter {

    fun fromMediaButtonIntent(
        intent: Intent?,
        profile: DeviceProfile? = null
    ): Pair<MediaControlEvent, KeyEvent>? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return null

        val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }

        if (keyEvent == null || keyEvent.action != KeyEvent.ACTION_DOWN) {
            return null
        }

        val event = mapKeyCodeToEvent(keyEvent.keyCode, profile) ?: return null
        return Pair(event, keyEvent)
    }

    fun mapKeyCodeToEvent(keyCode: Int, profile: DeviceProfile?): MediaControlEvent? {
        if (profile != null) {
            return when (keyCode) {
                profile.primaryKeyCode -> MediaControlEvent.PLAY_PAUSE
                profile.secondaryKeyCode -> MediaControlEvent.NEXT
                profile.backKeyCode -> MediaControlEvent.PREVIOUS
                profile.stopKeyCode -> MediaControlEvent.STOP
                else -> null
            }
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> MediaControlEvent.PLAY_PAUSE

            KeyEvent.KEYCODE_MEDIA_NEXT -> MediaControlEvent.NEXT
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaControlEvent.PREVIOUS
            KeyEvent.KEYCODE_MEDIA_STOP -> MediaControlEvent.STOP
            else -> null
        }
    }
}
