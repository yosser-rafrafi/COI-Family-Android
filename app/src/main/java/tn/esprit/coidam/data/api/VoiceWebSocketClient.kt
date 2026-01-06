package tn.esprit.coidam.data.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Enums.ConnectionState
import tn.esprit.coidam.data.models.Voice.VoiceInstruction
import tn.esprit.coidam.data.models.Voice.VoiceResponse
import java.io.ByteArrayOutputStream
import java.net.URISyntaxException

class VoiceWebSocketClient private constructor(private val context: Context) {

    private var socket: Socket? = null
    private val tokenManager = TokenManager(context)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    private var isConnecting = false

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

        fun getInstance(context: Context): VoiceWebSocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceWebSocketClient(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.disconnect()
                INSTANCE = null
            }
        }
    }

    fun shouldReconnect(): Boolean {
        return socket == null || socket?.connected() == false
    }

    suspend fun connect() {
        if (socket?.connected() == true) {
            Log.d(TAG, "⚠️ Already connected, reusing existing socket")
            _connectionState.value = ConnectionState.CONNECTED
            return
        }

        if (isConnecting) {
            Log.d(TAG, "⚠️ Connection already in progress")
            return
        }

        if (socket != null) {
            Log.d(TAG, "🧹 Cleaning up existing disconnected socket")
            socket?.off()
            socket?.disconnect()
            socket = null
        }

        try {
            isConnecting = true
            _connectionState.value = ConnectionState.CONNECTING

            val userId = tokenManager.getUserIdSync()
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "❌ No userId available")
                _connectionState.value = ConnectionState.DISCONNECTED
                isConnecting = false
                return
            }

            val linkedUserId = tokenManager.getLinkedUserIdSync()

            val options = IO.Options().apply {
                query = "userId=$userId&userType=blind" +
                        if (linkedUserId != null) "&linkedUserId=$linkedUserId" else ""
                reconnection = true
                reconnectionAttempts = 5
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
            isConnecting = false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection error", e)
            _connectionState.value = ConnectionState.ERROR
            isConnecting = false
        }
    }

    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ Connected to server - Socket ID: ${socket?.id()}")
                _connectionState.value = ConnectionState.CONNECTED
                isConnecting = false
            }

            on("connected") { args ->
                val data = args[0] as JSONObject
                Log.d(TAG, "✅ Server confirmation: ${data.getString("message")}")
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
                isConnecting = false
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "❌ Connection error: ${args[0]}")
                _connectionState.value = ConnectionState.DISCONNECTED
                isConnecting = false
            }
        }
    }

    fun sendVoiceCommand(command: String) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "⚠️ Not connected. Cannot send command. State: ${_connectionState.value}")
            return
        }

        val data = JSONObject().apply {
            put("command", command)
            put("timestamp", System.currentTimeMillis().toString())
        }

        Log.d(TAG, "📤 Sending voice command (text): $command")
        socket?.emit("voice-command", data)
    }

    fun sendVoiceCommandAudio(audioData: ByteArray, language: String = "fr") {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "⚠️ Not connected. Cannot send audio.")
            return
        }

        val audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP)

        val data = JSONObject().apply {
            put("audioBase64", audioBase64)
            put("language", language)
            put("timestamp", System.currentTimeMillis().toString())
        }

        Log.d(TAG, "📤 Sending voice command (audio): ${audioData.size} bytes")
        socket?.emit("voice-command-audio", data)
    }

    fun startFaceRecognition() {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "⚠️ Not connected. Cannot start face recognition.")
            return
        }

        Log.d(TAG, "📤 Starting face recognition")
        socket?.emit("start-face-recognition")
    }

    fun sendFaceRecognitionResult(result: Any) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "⚠️ Not connected. Cannot send result.")
            return
        }

        val data = JSONObject().apply {
            put("result", JSONObject(result.toString()))
        }

        Log.d(TAG, "📤 Sending face recognition result")
        socket?.emit("face-recognition-result", data)
    }

    /**
     * ✅ CORRECTION MAJEURE: Envoi de photo avec vérification robuste
     */
    fun sendPhotoForProcessing(bitmap: Bitmap) {
        // ✅ Vérifier PLUSIEURS fois l'état de connexion
        val currentState = _connectionState.value
        val isSocketConnected = socket?.connected() == true

        Log.d(TAG, "📸 sendPhotoForProcessing called")
        Log.d(TAG, "   - ConnectionState: $currentState")
        Log.d(TAG, "   - Socket?.connected(): $isSocketConnected")
        Log.d(TAG, "   - Socket ID: ${socket?.id()}")

        if (currentState != ConnectionState.CONNECTED || !isSocketConnected) {
            Log.e(TAG, "❌ CANNOT SEND PHOTO: Not connected!")
            Log.e(TAG, "   - ConnectionState: $currentState")
            Log.e(TAG, "   - Socket connected: $isSocketConnected")
            return
        }

        try {
            // ✅ Compression plus agressive pour réduire la taille
            val outputStream = ByteArrayOutputStream()
            val compressionQuality = 60 // Réduire de 70% à 60%

            Log.d(TAG, "📸 Compressing bitmap (quality: $compressionQuality%)...")
            val compressionSuccess = bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                compressionQuality,
                outputStream
            )

            if (!compressionSuccess) {
                Log.e(TAG, "❌ Bitmap compression failed!")
                return
            }

            val imageBytes = outputStream.toByteArray()
            val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            Log.d(TAG, "✅ Photo compressed successfully")
            Log.d(TAG, "   - Original size: ${bitmap.byteCount} bytes")
            Log.d(TAG, "   - Compressed size: ${imageBytes.size} bytes")
            Log.d(TAG, "   - Base64 length: ${imageBase64.length}")
            Log.d(TAG, "   - Compression ratio: ${String.format("%.1f", (imageBytes.size.toFloat() / bitmap.byteCount) * 100)}%")

            val data = JSONObject().apply {
                put("imageBase64", imageBase64)
                put("timestamp", System.currentTimeMillis().toString())
            }

            // ✅ Vérifier UNE DERNIÈRE FOIS avant émission
            if (socket?.connected() != true) {
                Log.e(TAG, "❌ Socket disconnected just before emit!")
                return
            }

            Log.d(TAG, "📤 Emitting 'process-photo' event...")

            socket?.emit("process-photo", data, Ack { args ->
                if (args.isNotEmpty() && args[0] != null) {
                    Log.e(TAG, "❌ Server error on process-photo: ${args[0]}")
                } else {
                    Log.d(TAG, "✅ process-photo acknowledged by server")
                }
            })

            Log.d(TAG, "✅ process-photo event emitted successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in sendPhotoForProcessing: ${e.message}", e)
            e.printStackTrace()
        }
    }

    fun requestHelp() {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "⚠️ Not connected. Cannot request help.")
            return
        }

        Log.d(TAG, "📤 Requesting help")
        socket?.emit("request-help")
    }

    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting socket...")

        socket?.off()
        socket?.disconnect()
        socket = null

        _connectionState.value = ConnectionState.DISCONNECTED
        isConnecting = false

        Log.d(TAG, "✅ Socket disconnected and cleaned up")
    }
}