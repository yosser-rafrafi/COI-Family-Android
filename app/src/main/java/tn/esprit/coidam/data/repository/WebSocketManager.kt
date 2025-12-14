package tn.esprit.coidam.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.*
import tn.esprit.coidam.data.models.CallDto.IncomingCallDto
import tn.esprit.coidam.data.models.CallResponses.AgoraCredentials
import tn.esprit.coidam.data.models.Enums.CallStatus
import tn.esprit.coidam.data.models.Enums.CallType
import tn.esprit.coidam.data.models.Enums.ConnectionState

class WebSocketManager(private val context: Context) {
    private var socket: Socket? = null
    private val tokenManager = TokenManager(context)
    private val gson = Gson()
    private val TAG = "WebSocketManager"

    // États observables
    private val _incomingCall = MutableStateFlow<IncomingCallDto?>(null)
    val incomingCall: StateFlow<IncomingCallDto?> = _incomingCall

    private val _callAccepted = MutableStateFlow<Call?>(null)
    val callAccepted: StateFlow<Call?> = _callAccepted

    private val _callRejected = MutableStateFlow<Call?>(null)
    val callRejected: StateFlow<Call?> = _callRejected

    private val _callEnded = MutableStateFlow<Call?>(null)
    val callEnded: StateFlow<Call?> = _callEnded

    private val _callMissed = MutableStateFlow<Call?>(null)
    val callMissed: StateFlow<Call?> = _callMissed

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    suspend fun connect() {
        try {
            val userId = tokenManager.getUserIdSync()
            if (userId == null) {
                Log.e(TAG, "❌ Cannot connect: userId is null")
                return
            }

            // ⚠️ REMPLACER PAR VOTRE IP SERVEUR
            val serverUrl = ApiClient.BASE_URL // Remove trailing slash
            val wsUrl = "$serverUrl/video-call"
            
            Log.d(TAG, "🔌 Attempting to connect to WebSocket: $wsUrl")
            Log.d(TAG, "🔌 User ID: $userId")
            
            // Store wsUrl for error logging
            val finalWsUrl = wsUrl

            val options = IO.Options().apply {
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                reconnectionAttempts = Integer.MAX_VALUE
                timeout = 10000
            }

            socket = IO.socket(wsUrl, options)

            socket?.apply {
                // ✅ Événement de connexion
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "✅ WebSocket connected to server")
                    _connectionState.value = ConnectionState.CONNECTED

                    // Rejoindre la room avec userId
                    val joinData = JSONObject().apply {
                        put("userId", userId)
                    }
                    Log.d(TAG, "📤 Emitting join-room with userId: $userId")
                    emit("join-room", joinData)
                }

                // ❌ Événement de déconnexion
                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "❌ WebSocket disconnected")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Disconnected — trying to reconnect automatically…")
                    _connectionState.value = ConnectionState.RECONNECTING
                }

                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Reconnected")
                    _connectionState.value = ConnectionState.CONNECTED
                }

                // ⚠️ Erreur de connexion
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.firstOrNull()
                    Log.e(TAG, "❌ Connection error: $error")
                    Log.e(TAG, "❌ Failed to connect to: $finalWsUrl")
                    Log.e(TAG, "❌ Check: 1) Server is running, 2) URL is correct, 3) Network is accessible")
                    _connectionState.value = ConnectionState.ERROR
                }

                // 🎉 Room rejoint avec succès
                on("joined-room") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val joinedUserId = data.getString("userId")
                        val socketId = data.optString("socketId", "unknown")
                        Log.d(TAG, "🎉✅✅✅ JOINED ROOM SUCCESSFULLY! ✅✅✅")
                        Log.d(TAG, "🎉 User ID: $joinedUserId")
                        Log.d(TAG, "🎉 Socket ID: $socketId")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing joined-room: ${e.message}", e)
                    }
                }

                // 📞 APPEL ENTRANT
                on("incoming-call") { args ->
                    try {
                        val data = args[0] as JSONObject
                        Log.d(TAG, "📞 RAW incoming-call data: $data")

                        val callJson = data.getJSONObject("call")
                        val agoraJson = data.optJSONObject("agoraCredentials")

                        val call = parseCallFromJson(callJson)
                        val agoraCredentials = agoraJson?.let { parseAgoraCredentials(it) }

                        Log.d(TAG, "📞 Incoming call from: ${call.id}")
                        _incomingCall.value = IncomingCallDto(call, agoraCredentials)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing incoming call: ${e.message}", e)
                    }
                }

                // ✅ APPEL ACCEPTÉ
                on("call-accepted") { args ->
                    try {
                        val callJson = args[0] as JSONObject
                        val call = parseCallFromJson(callJson)

                        Log.d(TAG, "✅ Call accepted: ${call.id}")
                        _callAccepted.value = call
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing call accepted: ${e.message}", e)
                    }
                }

                // ❌ APPEL REJETÉ
                on("call-rejected") { args ->
                    try {
                        val callJson = args[0] as JSONObject
                        val call = parseCallFromJson(callJson)

                        Log.d(TAG, "❌ Call rejected: ${call.id}")
                        _callRejected.value = call
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing call rejected: ${e.message}", e)
                    }
                }

                // 🔚 APPEL TERMINÉ
                on("call-ended") { args ->
                    try {
                        val callJson = args[0] as JSONObject
                        val call = parseCallFromJson(callJson)

                        Log.d(TAG, "🔚 Call ended: ${call.id}")
                        _callEnded.value = call
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing call ended: ${e.message}", e)
                    }
                }

                // 📵 APPEL MANQUÉ
                on("call-missed") { args ->
                    try {
                        val callJson = args[0] as JSONObject
                        val call = parseCallFromJson(callJson)

                        Log.d(TAG, "📵 Call missed: ${call.id}")
                        _callMissed.value = call
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing call missed: ${e.message}", e)
                    }
                }

                // Se connecter
                Log.d(TAG, "🔌 Calling socket.connect()...")
                connect()
                Log.d(TAG, "🔌 socket.connect() called, waiting for connection...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up socket: ${e.message}", e)
            Log.e(TAG, "❌ Stack trace:", e)
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "WebSocket disconnected and cleaned up")
    }

    fun clearIncomingCall() {
        _incomingCall.value = null
    }

    fun clearCallAccepted() {
        _callAccepted.value = null
    }

    fun clearCallRejected() {
        _callRejected.value = null
    }

    fun clearCallEnded() {
        _callEnded.value = null
    }

    private fun parseCallFromJson(json: JSONObject): Call {
        return Call(
            id = json.getString("_id"),
            blindUser = json.optJSONObject("blindUser")?.let { parseUser(it) },
            companion = json.optJSONObject("companion")?.let { parseUser(it) },
            status = parseCallStatus(json.getString("status")),
            callType = parseCallType(json.getString("callType")),
            agoraChannelName = json.optString("agoraChannelName").ifEmpty { null },
            agoraToken = json.optString("agoraToken").ifEmpty { null },
            agoraUid = if (json.has("agoraUid") && !json.isNull("agoraUid")) json.getInt("agoraUid") else null,
            initiatedBy = json.getString("initiatedBy"),
            endedBy = json.optString("endedBy").ifEmpty { null },
            endReason = json.optString("endReason").ifEmpty { null },
            startedAt = json.optString("startedAt").ifEmpty { null },
            endedAt = json.optString("endedAt").ifEmpty { null },
            duration = if (json.has("duration") && !json.isNull("duration")) json.getInt("duration") else null,
            createdAt = json.getString("createdAt"),
            updatedAt = json.optString("updatedAt").ifEmpty { json.getString("createdAt") } // Fallback to createdAt if updatedAt missing
        )
    }

    private fun parseUser(json: JSONObject): UserResponse {
        return UserResponse(
            id = json.getString("_id"),
            firstName = json.optString("firstName", "Unknown"), // Provide default if missing
            lastName = json.optString("lastName", "User"),    // Provide default if missing
            email = json.optString("email", "unknown@example.com") // Provide default if missing
        )
    }

    private fun parseAgoraCredentials(json: JSONObject): AgoraCredentials {
        return AgoraCredentials(
            token = json.getString("token"),
            channelName = json.getString("channelName"),
            uid = json.getInt("uid"),
            expiresAt = if (json.has("expiresAt")) json.optLong("expiresAt") else null
        )
    }

    private fun parseCallStatus(status: String): CallStatus {
        return when (status.lowercase()) {
            "pending" -> CallStatus.PENDING
            "ringing" -> CallStatus.RINGING
            "active" -> CallStatus.ACTIVE
            "completed" -> CallStatus.COMPLETED
            "missed" -> CallStatus.MISSED
            "rejected" -> CallStatus.REJECTED
            "failed" -> CallStatus.FAILED
            else -> CallStatus.FAILED
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WebSocketManager? = null

        fun getInstance(context: Context): WebSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
private fun parseCallType(type: String): CallType {
    return when (type.lowercase()) {
        "audio" -> CallType.AUDIO
        "video" -> CallType.VIDEO
        else -> CallType.AUDIO
    }
}

