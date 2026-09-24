package com.siaa.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class ProductionSpeechRecognizer(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    fun start(languageTag:String="en-US", callback:(Result<String>)->Unit) {
        stop()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback(Result.failure(IllegalStateException("SpeechRecognizer no disponible"))); return
        }
        val r = if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer=r
        r.setRecognitionListener(object: RecognitionListener {
            override fun onResults(results: Bundle) { callback(Result.success(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())); stop() }
            override fun onError(error: Int) { callback(Result.failure(IllegalStateException("ASR error $error"))); stop() }
            override fun onReadyForSpeech(params: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle) {}
            override fun onEvent(eventType: Int, params: Bundle) {}
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (Build.VERSION.SDK_INT >= 33) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        })
    }
    fun stop() { recognizer?.runCatching { cancel(); destroy() }; recognizer=null }
}
