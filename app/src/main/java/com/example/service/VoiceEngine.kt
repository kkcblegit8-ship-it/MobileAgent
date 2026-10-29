package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _voiceStatus = MutableStateFlow("Voice Ready")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    init {
        mainHandler.post {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.setSpeechRate(1.08f)
                    isTtsReady = true
                }
            }
        }
    }

    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
        if (!_isTtsEnabled.value) {
            tts?.stop()
        }
    }

    fun speak(text: String) {
        if (!_isTtsEnabled.value || !isTtsReady || text.isBlank()) return
        mainHandler.post {
            tts?.speak(text.take(220), TextToSpeech.QUEUE_FLUSH, null, "nexus_tts")
        }
    }

    fun startListening(onFinalResult: (String) -> Unit) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _voiceStatus.value = "Speech recognition service unavailable on this device"
                return@post
            }
            try {
                speechRecognizer?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _voiceStatus.value = "Listening... Speak command"
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceStatus.value = "Capturing voice stream..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _voiceStatus.value = "Processing voice..."
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val currentPartial = _liveTranscript.value.trim()
                        if (currentPartial.isNotEmpty()) {
                            onFinalResult(currentPartial)
                        } else {
                            _voiceStatus.value = "Mic idle (tap to speak)"
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()?.trim().orEmpty()
                        if (spoken.isNotEmpty()) {
                            _liveTranscript.value = spoken
                            _voiceStatus.value = "Heard: \"$spoken\""
                            onFinalResult(spoken)
                        } else {
                            _voiceStatus.value = "Voice Ready"
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (partial.isNotEmpty()) {
                            _liveTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                _liveTranscript.value = ""
                _isListening.value = true
                recognizer.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                _voiceStatus.value = "Voice error: ${e.localizedMessage ?: "Failed"}"
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            runCatching { speechRecognizer?.stopListening() }
            _isListening.value = false
            _voiceStatus.value = "Voice Ready"
        }
    }

    fun shutdown() {
        mainHandler.post {
            runCatching { speechRecognizer?.destroy() }
            runCatching { tts?.shutdown() }
        }
    }
}
