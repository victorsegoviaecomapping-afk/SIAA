package com.siaa.core.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Fast first-run/session preflight so failures happen before a lesson starts. */
data class AudioPreflightReport(
    val privateOutput: Boolean,
    val spanishTts: Boolean,
    val englishTts: Boolean,
    val spanishLocale: String?,
    val englishLocale: String?,
    val outputTypes: List<Int>,
    val ready: Boolean,
    val message: String
)

class AudioPreflight(private val context: Context) {
    suspend fun run(requirePrivateOutput: Boolean = true): AudioPreflightReport {
        val manager = context.getSystemService(AudioManager::class.java)
        val outputs = manager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.map { it.type }.orEmpty()
        val privateOutput = outputs.any { it in SAFE }
        val init = CompletableDeferred<Int>()
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context.applicationContext) { status -> if (!init.isCompleted) init.complete(status) }
        val status = withTimeoutOrNull(6_000L) { init.await() } ?: TextToSpeech.ERROR
        var es: Locale? = null
        var en: Locale? = null
        if (status == TextToSpeech.SUCCESS) {
            es = choose(tts, listOf("es-PE","es-US","es-MX","es-ES","es"))
            en = choose(tts, listOf("en-US","en-GB","en-AU","en"))
        }
        tts.shutdown()
        val ready = (!requirePrivateOutput || privateOutput) && es != null && en != null
        val message = buildString {
            if (!privateOutput && requirePrivateOutput) append("Conecta audífonos. ")
            if (es == null) append("Falta voz TTS en español. ")
            if (en == null) append("Falta voz TTS en inglés. ")
            if (ready) append("Audio listo: ${es?.toLanguageTag()} + ${en?.toLanguageTag()}.")
        }.trim()
        return AudioPreflightReport(privateOutput, es != null, en != null, es?.toLanguageTag(), en?.toLanguageTag(), outputs, ready, message)
    }

    private fun choose(tts: TextToSpeech, tags: List<String>): Locale? = tags.asSequence()
        .map(Locale::forLanguageTag)
        .firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }

    companion object {
        private val SAFE = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_BLE_HEADSET
        )
    }
}
