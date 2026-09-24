package com.siaa.app.media

import android.content.Intent
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.siaa.core.model.DeviceProfile
import com.siaa.core.runtime.MediaControlEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarbudCommandRouterInstrumentedTest {
    private val router = EarbudCommandRouter()

    private fun mediaIntent(keyCode: Int, action: Int = KeyEvent.ACTION_DOWN) =
        Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
            Intent.EXTRA_KEY_EVENT,
            KeyEvent(action, keyCode)
        )

    @Test
    fun defaultFirmwareKeysMapToExpectedCommands() {
        assertEquals(
            MediaControlEvent.PLAY_PAUSE,
            router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE), null)?.first
        )
        assertEquals(
            MediaControlEvent.NEXT,
            router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_NEXT), null)?.first
        )
        assertEquals(
            MediaControlEvent.PREVIOUS,
            router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS), null)?.first
        )
        assertEquals(
            MediaControlEvent.STOP,
            router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_STOP), null)?.first
        )
    }

    @Test
    fun calibratedProfileOverridesDefaultMeaning() {
        val profile = DeviceProfile(
            id = 1L,
            name = "test buds",
            primaryKeyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
            secondaryKeyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            backKeyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            stopKeyCode = KeyEvent.KEYCODE_MEDIA_STOP,
            playPauseAvailable = true,
            nextAvailable = true,
            previousAvailable = true
        )
        assertEquals(
            MediaControlEvent.PLAY_PAUSE,
            router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_NEXT), profile)?.first
        )
        assertEquals(
            MediaControlEvent.NEXT,
            router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS), profile)?.first
        )
        assertEquals(
            MediaControlEvent.PREVIOUS,
            router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE), profile)?.first
        )
    }

    @Test
    fun keyUpAndUnknownKeysAreIgnored() {
        assertNull(router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.ACTION_UP), null))
        assertNull(router.fromMediaButtonIntent(mediaIntent(KeyEvent.KEYCODE_VOLUME_UP), null))
    }
}
