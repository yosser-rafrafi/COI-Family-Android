package tn.esprit.coidam.data.api

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import tn.esprit.coidam.data.local.TokenManager
import java.net.URISyntaxException
import android.util.Base64
import tn.esprit.coidam.data.models.Enums.ConnectionState
import tn.esprit.coidam.data.models.Voice.VoiceInstruction
import tn.esprit.coidam.data.models.Voice.VoiceResponse

/**
 * Client WebSocket pour les commandes vocales (SINGLETON)
 * Supporte 2 modes:
 * 1. Envoyer du texte transcrit localement (Android Speech Recognition)
 * 2. Envoyer de l'audio pour transcription serveur (Whisper)
 */
class VoiceWebSocketClient private constructor(private val context: Context) {

    private var socket: Socket? = null
    private val tokenManager = TokenManager(context)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    private var isConnecting = false  // ✅ Flag pour éviter connexions multiples

    private val _voiceInstruction = MutableStateFlow<VoiceInstruction?>(null)
    val voiceInstruction: StateFlow<VoiceInstruction?> = _voiceInstruction

    private val _voiceResponse = MutableStateFlow<VoiceResponse?>(null)
    val voiceResponse: StateFlow<VoiceResponse?> = _voiceResponse

    companion object {
        private const val TAG = "VoiceWebSocketClient"
        private const val SERVER_URL = ApiClient.BASE_URL
        private const val NAMESPACE = "/voice-commands"
        
        @Volatile
        private var INSTANCE: VoiceWebSocketClient? = null
        
        /**
         * Get singleton instance of VoiceWebSocketClient
         */
        fun getInstance(context: Context): VoiceWebSocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceWebSocketClient(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        // ✅ CORRECTION: Nettoyer l'instance au logout
        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.disconnect()
                INSTANCE = null
            }
        }
    }



    /**
     * Check if we should attempt to connect/reconnect
     */
    fun shouldReconnect(): Boolean {
        return socket == null || socket?.connected() == false
    }

    /**
     * ✅ Connexion au WebSocket avec vérification d'état robuste
     */
    suspend fun connect() {
        // ✅ Check 1: Is there an existing connected socket?
        if (socket?.connected() == true) {
            Log.d(TAG, "⚠️ Already connected, reusing existing socket")
            _connectionState.value = ConnectionState.CONNECTED
            return
        }
        
        // ✅ Check 2: Is there a connection already in progress?
        if (isConnecting) {
            Log.d(TAG, "⚠️ Connection already in progress")
            return
        }
        
        // ✅ Check 3: Clean up any existing socket before creating new one
        if (socket != null) {
            Log.d(TAG, "🧹 Cleaning up existing disconnected socket")
            socket?.off()
            socket?.disconnect()
            socket = null
        }

        try {
            isConnecting = true  // ✅ Set flag
            _connectionState.value = ConnectionState.CONNECTING

            val userId = tokenManager.getUserIdSync()
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "❌ No userId available")
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            }

            val linkedUserId = tokenManager.getLinkedUserIdSync()

            val options = IO.Options().apply {
                query = "userId=$userId&userType=blind" + 
                        if (linkedUserId != null) "&linkedUserId=$linkedUserId" else ""
                reconnection = true
                reconnectionAttempts = 5  // ✅ Finite attempts, not infinite
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 10000
            }

            val fullUrl = "$SERVER_URL$NAMESPACE"
            Log.d(TAG, "🔌 Creating new socket connection to: $fullUrl")
            Log.d(TAG, "👤 User ID: $userId")

            socket = IO.socket(fullUrl, options)
            setupSocketListeners()
            socket?.connect()

        } catch (e: URISyntaxException) {
            Log.e(TAG, "❌ Invalid URL", e)
            _connectionState.value = ConnectionState.ERROR
            isConnecting = false  // ✅ Clear flag
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection error", e)
            _connectionState.value = ConnectionState.ERROR
            isConnecting = false  // ✅ Clear flag
        }
    }

    /**
     * ✅ Configurer les listeners Socket.IO
     */
    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ Connected to server")
                _connectionState.value = ConnectionState.CONNECTED
                isConnecting = false  // ✅ Clear flag
            }

            on("connected") { args ->
                val data = args[0] as JSONObject
                Log.d(TAG, "Server confirmation: ${data.getString("message")}")
            }

            on("voice-instruction") { args ->
                val data = args[0] as JSONObject
                val instruction = VoiceInstruction(
                    action = data.getString("action"),
                    text = data.getString("text"),
                    navigation = data.optString("navigation").takeIf { it.isNotEmpty() }
                )
                Log.d(TAG, "🔊 Voice instruction: ${instruction.text}")
                _voiceInstruction.value = instruction
            }

            on("voice-response") { args ->
                val data = args[0] as JSONObject
                val response = VoiceResponse(
                    success = data.getBoolean("success"),
                    action = data.optString("action").takeIf { it.isNotEmpty() },
                    message = data.getString("message"),
                    speakText = data.optString("speakText").takeIf { it.isNotEmpty() },
                    navigation = data.optString("navigation").takeIf { it.isNotEmpty() },
                    transcription = data.optString("transcription").takeIf { it.isNotEmpty() }
                )
                Log.d(TAG, "📨 Voice response: ${response.message}")
                _voiceResponse.value = response
            }

            on("open-camera") { args ->
                val data = args[0] as JSONObject
                val instruction = VoiceInstruction(
                    action = "open-camera",
                    text = data.getString("speakText")
                )
                Log.d(TAG, "📸 Open camera request")
                _voiceInstruction.value = instruction
            }

            on("blind-detection-notification") { args ->
                val data = args[0] as JSONObject
                Log.d(TAG, "🔔 Blind detection notification received")
            }

            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "❌ Disconnected from server")
                _connectionState.value = ConnectionState.DISCONNECTED
                isConnecting = false  // ✅ Clear flag
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Connection error: ${args[0]}")
                _connectionState.value = ConnectionState.DISCONNECTED
                isConnecting = false  // ✅ Clear flag
            }
        }
    }

    /**
     * ✅ MODE 1: Envoyer une commande vocale TEXTE (déjà transcrite)
     * Utiliser ceci si vous transcrivez localement avec Android Speech Recognition
     */
    fun sendVoiceCommand(command: String) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected. Cannot send command.")
            return
        }

        val data = JSONObject().apply {
            put("command", command)
            put("timestamp", System.currentTimeMillis().toString())
        }

        Log.d(TAG, "📤 Sending voice command (text): $command")
        socket?.emit("voice-command", data)
    }

    /**
     * ✅ MODE 2: Envoyer un fichier AUDIO pour transcription serveur
     * Utiliser ceci si vous voulez que le serveur Whisper transcrive
     */
    fun sendVoiceCommandAudio(audioData: ByteArray, language: String = "fr") {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected. Cannot send audio.")
            return
        }

        // Convertir en base64
        val audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP)

        val data = JSONObject().apply {
            put("audioBase64", audioBase64)
            put("language", language)
            put("timestamp", System.currentTimeMillis().toString())
        }

        Log.d(TAG, "📤 Sending voice command (audio): ${audioData.size} bytes")
        socket?.emit("voice-command-audio", data)
    }

    /**
     * ✅ Démarrer la reconnaissance faciale
     */
    fun startFaceRecognition() {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected. Cannot start face recognition.")
            return
        }

        Log.d(TAG, "📤 Starting face recognition")
        socket?.emit("start-face-recognition")
    }

    /**
     * ✅ Envoyer les résultats de reconnaissance faciale
     */
    fun sendFaceRecognitionResult(result: Any) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected. Cannot send result.")
            return
        }

        val data = JSONObject().apply {
            put("result", JSONObject(result.toString()))
        }

        Log.d(TAG, "📤 Sending face recognition result")
        socket?.emit("face-recognition-result", data)
    }

    /**
     * ✅ Demander l'aide
     */
    fun requestHelp() {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected. Cannot request help.")
            return
        }

        Log.d(TAG, "📤 Requesting help")
        socket?.emit("request-help")
    }

    /**
     * ✅ Déconnexion et nettoyage
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting socket...")
        
        // Remove all event listeners first
        socket?.off()
        
        // Disconnect the socket
        socket?.disconnect()
        
        // Clear the reference
        socket = null
        
        // Update state
        _connectionState.value = ConnectionState.DISCONNECTED
        
        Log.d(TAG, "✅ Socket disconnected and cleaned up")
    }
}