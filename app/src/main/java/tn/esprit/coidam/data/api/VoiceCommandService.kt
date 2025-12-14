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
import java.util.*

/**
 * Service Android natif pour:
 * - Speech Recognition (Speech-to-Text)
 * - Text-to-Speech (TTS)
 *
 * Ce service transcrit LOCALEMENT sur Android
 * Le WebSocketClient peut ensuite envoyer le texte au serveur
 */
class VoiceCommandService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var onCommandRecognized: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "VoiceCommandService"
    }

    init {
        initializeTTS()
        initializeSpeechRecognizer()
    }

    /**
     * ✅ Initialiser Text-to-Speech
     */
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.FRENCH
                _isReady.value = true
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    /**
     * ✅ Initialiser Speech Recognition
     */
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                isListening = false
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                isListening = false

                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions insuffisantes"
                    SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Délai réseau dépassé"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Aucune correspondance"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconnaissance occupée"
                    SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Délai d'attente dépassé"
                    else -> "Erreur inconnue"
                }

                speak(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "Recognized: $text")
                    _recognizedText.value = text
                    onCommandRecognized?.invoke(text)
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    /**
     * ✅ Démarrer l'écoute
     */
    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening")
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
            Log.d(TAG, "Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting listening", e)
            speak("Erreur lors du démarrage de l'écoute")
        }
    }

    /**
     * ✅ Arrêter l'écoute
     */
    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(TAG, "Stopped listening")
        }
    }

    /**
     * ✅ Parler (Text-to-Speech)
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")

        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started: $text")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS completed: $text")
                onComplete?.invoke()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error")
            }
        })
    }

    /**
     * ✅ Définir le callback pour les commandes
     */
    fun setOnCommandRecognized(callback: (String) -> Unit) {
        onCommandRecognized = callback
    }

    /**
     * ✅ Nettoyer les ressources
     */
    fun cleanup() {
        stopListening()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        Log.d(TAG, "Cleaned up resources")
    }
}