package com.siaa.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import com.siaa.core.runtime.SpeechPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Hybrid speech output for SIAA.
 *
 * Exact English strings that exist in assets/audio/audio_index.json are played from the
 * pre-rendered offline bank. Everything else falls back to Android TTS. The bundled bank is
 * intentionally labelled synthetic; it improves reproducibility/offline operation but does not
 * pretend to replace future human recordings for phonetic or accent-sensitive assessment.
 */
class BundledAudioSpeechPort(
    context: Context,
    private val fallback: SpeechPort = AndroidTtsSpeechPort(context)
) : SpeechPort {
    private val appContext = context.applicationContext
    private val lock = Any()
    @Volatile private var currentPlayer: MediaPlayer? = null

    private data class Entry(val rel: String, val kind: String)

    private val entries: Map<String, Entry> by lazy {
        runCatching {
            val externalIndex = File(appContext.filesDir, "siaa-content-active/audio/audio_index.json")
            val text = if (externalIndex.isFile) externalIndex.readText()
            else appContext.assets.open("audio/audio_index.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text).getJSONObject("entries")
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = root.getJSONObject(key)
                    put(key, Entry(obj.getString("rel"), obj.optString("kind", "unknown")))
                }
            }
        }.getOrElse { emptyMap() }
    }

    override suspend fun speak(text: String, languageTag: String, rate: Float) {
        if (text.isBlank()) return
        val isEnglish = languageTag.lowercase(Locale.ROOT).startsWith("en")
        val entry = if (isEnglish) entries[normalize(text)] else null
        if (entry == null) {
            fallback.speak(text, languageTag, rate)
            return
        }
        try {
            playAsset(entry.rel, rate)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // A corrupt/missing asset must not break a lesson: TTS remains the resilient fallback.
            fallback.speak(text, languageTag, rate)
        }
    }

    private suspend fun playAsset(rel: String, rate: Float) {
        val file = materialize(rel)
        suspendCancellableCoroutine<Unit> { cont ->
            val player = MediaPlayer()
            synchronized(lock) {
                currentPlayer?.let { old -> runCatching { old.stop() }; old.release() }
                currentPlayer = player
            }
            fun releaseCurrent() {
                synchronized(lock) {
                    if (currentPlayer === player) currentPlayer = null
                }
                runCatching { player.release() }
            }
            cont.invokeOnCancellation {
                runCatching { player.stop() }
                releaseCurrent()
            }
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener {
                    releaseCurrent()
                    if (cont.isActive) cont.resume(Unit)
                }
                player.setOnErrorListener { _, what, extra ->
                    releaseCurrent()
                    if (cont.isActive) cont.resumeWithException(
                        TtsPlaybackException("Bundled audio playback error what=$what extra=$extra for $rel")
                    )
                    true
                }
                player.prepare()
                if (rate != 1.0f) {
                    player.playbackParams = PlaybackParams().setSpeed(rate.coerceIn(0.65f, 1.35f))
                }
                player.start()
            } catch (t: Throwable) {
                releaseCurrent()
                if (cont.isActive) cont.resumeWithException(t)
            }
        }
    }

    private fun materialize(rel: String): File {
        val dir = File(appContext.cacheDir, "siaa-bundled-audio").apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-256").digest(rel.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(24)
        val ext = rel.substringAfterLast('.', "ogg")
        val target = File(dir, "$hash.$ext")
        if (!target.exists() || target.length() == 0L) {
            val external = File(appContext.filesDir, "siaa-content-active/$rel")
            if (external.isFile) external.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            else appContext.assets.open(rel).use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        }
        return target
    }

    override fun stop() {
        synchronized(lock) {
            currentPlayer?.let { player ->
                runCatching { player.stop() }
                runCatching { player.release() }
            }
            currentPlayer = null
        }
        fallback.stop()
    }

    override fun shutdown() {
        stop()
        fallback.shutdown()
    }

    private fun normalize(text: String): String = text.trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)
}
