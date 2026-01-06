package tn.esprit.coidam.data.api

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import java.util.*

class VoiceCommandService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var onCommandRecognized: ((String) -> Unit)? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "VoiceCommandService"
    }

    init {
        initializeTTS()
        initializeSpeechRecognizer()
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.FRENCH
                _isReady.value = true
                Log.d(TAG, "✅ TTS initialized successfully")
            } else {
                Log.e(TAG, "❌ TTS initialization failed")
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "❌ Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "🎤 Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "🗣️ Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "🔇 Speech ended")
                isListening = false
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions insuffisantes"
                    SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Délai réseau dépassé"
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        Log.d(TAG, "⚠️ No speech detected (silence)")
                        coroutineScope.launch {
                            delay(500)
                            if (!isListening) {
                                startListening()
                            }
                        }
                        return
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconnaissance occupée"
                    SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        Log.d(TAG, "⚠️ Speech timeout (no speech)")
                        coroutineScope.launch {
                            delay(500)
                            if (!isListening) {
                                startListening()
                            }
                        }
                        return
                    }
                    else -> "Erreur inconnue ($error)"
                }

                Log.e(TAG, "❌ Speech recognition error: $errorMessage")
                isListening = false

                coroutineScope.launch {
                    delay(1000)
                    if (!isListening) {
                        startListening()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "✅ Recognized: $text")

                    val filteredText = filterFalsePositives(text)
                    if (filteredText != null) {
                        _recognizedText.value = filteredText
                        onCommandRecognized?.invoke(filteredText)
                    } else {
                        Log.d(TAG, "⚠️ Filtered out false positive: $text")
                        coroutineScope.launch {
                            delay(500)
                            if (!isListening) {
                                startListening()
                            }
                        }
                    }
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "📝 Partial: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (isListening) {
            Log.w(TAG, "⚠️ Already listening")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(TAG, "🎤 Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting listening", e)
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(TAG, "🔇 Stopped listening")
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        val wasListening = isListening
        if (wasListening) {
            stopListening()
        }

        Log.d(TAG, "🔊 Speaking: $text")
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")

        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "🗣️ TTS started")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "✅ TTS completed")
                onComplete?.invoke()

                if (wasListening) {
                    coroutineScope.launch {
                        delay(800) // Délai plus long pour éviter auto-détection
                        if (!isListening) {
                            startListening()
                        }
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "❌ TTS error")
                if (wasListening) {
                    coroutineScope.launch {
                        delay(500)
                        if (!isListening) {
                            startListening()
                        }
                    }
                }
            }
        })
    }

    private fun filterFalsePositives(text: String): String? {
        val lower = text.lowercase()

        val falsePositives = listOf(
            "micro activé",
            "micro désactivé",
            "mode automatique",
            "caméra et micro",
            "je prends la photo",
            "photo enregistrée",
            "reconnaissance terminée",
            "détection terminée",
            "prise de photo"
        )

        for (fp in falsePositives) {
            if (lower.contains(fp)) {
                return null
            }
        }

        val cleaned = text.trim()
        if (cleaned.length < 3) {
            return null
        }

        return cleaned
    }

    fun setOnCommandRecognized(callback: (String) -> Unit) {
        onCommandRecognized = callback
    }

    fun restartListeningAfterDelay(delayMs: Long = 1000) {
        coroutineScope.launch {
            delay(delayMs)
            if (!isListening) {
                startListening()
            }
        }
    }

    fun cleanup() {
        stopListening()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        Log.d(TAG, "✅ Cleaned up resources")
    }
}