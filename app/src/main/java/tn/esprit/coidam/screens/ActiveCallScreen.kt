// ActiveCallScreen.kt
import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import tn.esprit.coidam.data.repository.CallRepository
import tn.esprit.coidam.data.repository.WebSocketManager
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Call
import tn.esprit.coidam.data.models.Enums.CallStatus
import tn.esprit.coidam.data.models.Enums.CallType
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

@Composable
fun ActiveCallScreen(
    navController: NavController,
    callId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callRepository = remember { CallRepository(context) }
    val tokenManager = remember { TokenManager(context) }
    val webSocketManager = WebSocketManager.getInstance(context)

    var call by remember { mutableStateOf<Call?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var callDuration by remember { mutableStateOf(0) }
    var showEndDialog by remember { mutableStateOf(false) }

    // ✅ Observe bSocket events
    val callAccepted by webSocketManager.callAccepted.collectAsState()
    val callEnded by webSocketManager.callEnded.collectAsState()

    // Agora
    var agoraEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var remoteUid by remember { mutableStateOf(0) }

    // Timer pour durée appel
    LaunchedEffect(call?.status) {
        if (call?.status == CallStatus.ACTIVE) {
            while (true) {
                delay(1000)
                callDuration++
            }
        }
    }

    // Initialiser Agora
    LaunchedEffect(Unit) {
        try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = "7ab650f4830b4f6eb5885dbbefd2213a"   // ← remplace par ton App ID
                mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        Log.d("Agora", "✅ Joined channel: $channel uid:$uid")
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        Log.d("Agora", "👤 Remote user joined: uid=$uid")
                        remoteUid = uid
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        Log.d("Agora", "👋 Remote user offline: uid=$uid reason=$reason")
                        remoteUid = 0
                    }

                    override fun onError(err: Int) {
                        Log.e("Agora", "❌ Error: $err")
                    }
                }
            }
            agoraEngine = RtcEngine.create(config)
            agoraEngine?.apply {
                enableVideo()
                enableAudio()
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
                // Configure video settings
                val videoEncoder = VideoEncoderConfiguration().apply {
                    dimensions = VideoEncoderConfiguration.VD_640x360
                    frameRate = VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15.value // Use .value
                    bitrate = VideoEncoderConfiguration.STANDARD_BITRATE
                    orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                }
                setVideoEncoderConfiguration(videoEncoder)
            }
        } catch (e: Exception) {
            Log.e("Agora", "Initialization failed: ${e.message}")
        }
    }

    // Helper function to join Agora channel
    val joinAgoraChannel: (String, String, Int) -> Unit = { token, channel, uid ->
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }

        val resultCode = agoraEngine?.joinChannel(token, channel, uid, options)
        if (resultCode == 0) {
            Log.d("Agora", "🎥 Successfully joined channel: $channel")
        } else {
            Log.e("Agora", "❌ Failed to join channel: $channel, error: $resultCode")
        }
    }

    // Charger l'appel + rejoindre canal
    LaunchedEffect(callId) {
        val result = callRepository.getCall(callId)
        val currentUserId = tokenManager.getUserIdSync()
        val currentUserType = tokenManager.getUserTypeSync()
        
        result.onSuccess { fetched ->
            call = fetched
            
            // ✅ Déterminer si on est l'initiateur ou le recipient
            val isInitiator = (fetched.initiatedBy == "blind" && currentUserType == "blind") ||
                             (fetched.initiatedBy == "companion" && currentUserType == "companion")
            
            // ✅ Si appel en ringing ET qu'on est le recipient (pas l'initiateur), accepter d'abord
            if (fetched.status == CallStatus.RINGING && !isInitiator) {
                callRepository.acceptCall(callId).onSuccess { acceptResponse ->
                    call = acceptResponse.call
                    // ✅ Utiliser les credentials depuis AcceptCallResponse
                    val credentials = acceptResponse.agoraCredentials
                    joinAgoraChannel(credentials.token, credentials.channelName, credentials.uid)
                }.onFailure {
                    Log.e("Agora", "❌ Failed to accept call: ${it.message}")
                    navController.popBackStack()
                }
            } else {
                // ✅ Appel déjà actif OU on est l'initiateur (on attend via WebSocket), utiliser les credentials du call
                val token = fetched.agoraToken
                val channel = fetched.agoraChannelName
                val uid = fetched.agoraUid
                if (token != null && channel != null && uid != null) {
                    // ✅ L'initiateur rejoint immédiatement avec les credentials qu'il a reçus dans StartCallResponse
                    if (fetched.status == CallStatus.RINGING && isInitiator) {
                        // On attend que l'autre accepte via WebSocket, mais on peut déjà rejoindre
                        joinAgoraChannel(token, channel, uid)
                    } else if (fetched.status == CallStatus.ACTIVE) {
                        // Appel déjà actif, rejoindre
                        joinAgoraChannel(token, channel, uid)
                    }
                } else {
                    Log.e("Agora", "❌ Missing token, channel, or uid")
                    // Essayer de régénérer le token
                    callRepository.regenerateToken(callId).onSuccess { credentials ->
                        joinAgoraChannel(credentials.token, credentials.channelName, credentials.uid)
                    }
                }
            }
        }.onFailure {
            Log.e("Agora", "❌ Failed to get call: ${it.message}")
            navController.popBackStack()
        }
        isLoading = false
    }

    // ✅ Observe call-accepted event (when other party accepts)
    LaunchedEffect(callAccepted) {
        callAccepted?.let { acceptedCall ->
            if (acceptedCall.id == callId) {
                call = acceptedCall
                Log.d("ActiveCall", "✅ Call accepted by other party")
                // Rejoindre le canal si pas encore fait
                val token = acceptedCall.agoraToken
                val channel = acceptedCall.agoraChannelName
                val uid = acceptedCall.agoraUid
                if (token != null && channel != null && uid != null) {
                    joinAgoraChannel(token, channel, uid)
                }
                webSocketManager.clearCallAccepted()
            }
        }
    }

    // ✅ Observe call-ended event (when other party ends call)
    LaunchedEffect(callEnded) {
        callEnded?.let { endedCall ->
            if (endedCall.id == callId) {
                Log.d("ActiveCall", "🔚 Call ended by other party")
                agoraEngine?.leaveChannel()
                navController.popBackStack()
                webSocketManager.clearCallEnded()
            }
        }
    }

    // Nettoyage quand on quitte l'écran
    DisposableEffect(Unit) {
        onDispose {
            agoraEngine?.leaveChannel()
            RtcEngine.destroy()
            Log.d("Agora", "🧹 Agora destroyed")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else if (call != null) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                // Info appelant
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ✅ Afficher le nom de l'autre partie (pas la nôtre)
                    val name = if (call!!.initiatedBy == "blind") {
                        // Si initié par blind, afficher companion
                        call!!.companion?.let { "${it.firstName} ${it.lastName}" } ?: "Accompagnant"
                    } else {
                        // Si initié par companion, afficher blind
                        call!!.blindUser?.let { "${it.firstName} ${it.lastName}" } ?: "Utilisateur"
                    }
                    Text(name, fontSize = 24.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        when (call!!.status) {
                            CallStatus.RINGING -> "Appel en cours..."
                            CallStatus.ACTIVE -> String.format("%02d:%02d", callDuration/60, callDuration%60)
                            else -> call!!.status.name
                        },
                        fontSize = 16.sp,
                        color = Color.LightGray
                    )
                }

                // Vidéo distante
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    if (remoteUid != 0 && call!!.callType == CallType.VIDEO) {
                        AndroidView(factory = { ctx ->
                            SurfaceView(ctx).apply {
                                agoraEngine?.setupRemoteVideo(
                                    VideoCanvas(
                                        this,
                                        VideoCanvas.RENDER_MODE_HIDDEN,
                                        remoteUid
                                    )
                                )
                            }
                        }, modifier = Modifier.fillMaxSize())
                    } else {
                        // placeholder / attente
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Videocam, "", tint = Color.DarkGray, modifier = Modifier.size(80.dp))
                        }
                    }

                    // Vidéo locale mini
                    if (call!!.callType == CallType.VIDEO && !isCameraOff) {
                        AndroidView(factory = {ctx ->
                            SurfaceView(ctx).apply {
                                agoraEngine?.setupLocalVideo(VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                                agoraEngine?.startPreview()
                            }
                        }, modifier = Modifier
                            .size(120.dp, 160.dp)
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp)))
                    }
                }

                // Contrôles
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        isMuted = !isMuted
                        agoraEngine?.muteLocalAudioStream(isMuted)
                    }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    if (call!!.callType == CallType.VIDEO) {
                        IconButton(onClick = {
                            isCameraOff = !isCameraOff
                            agoraEngine?.muteLocalVideoStream(isCameraOff)
                        }) {
                            Icon(
                                imageVector = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    IconButton(onClick = {
                        // Terminer l'appel
                        showEndDialog = true
                    }) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        isSpeakerOn = !isSpeakerOn
                        agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
                    }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog fin d'appel
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("Terminer l'appel ?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val userType = tokenManager.getUserTypeSync() ?: "blind"
                        callRepository.endCall(callId, userType, "completed")
                        agoraEngine?.leaveChannel()
                        navController.popBackStack()
                    }
                }) {
                    Text("Oui", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Non")
                }
            }
        )
    }
}

@Composable fun CallControlButton( icon: androidx.compose.ui.graphics.vector.ImageVector, backgroundColor: Color, onClick: () -> Unit ) { FloatingActionButton( onClick = onClick, containerColor = backgroundColor, modifier = Modifier.size(56.dp) ) { Icon( imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp) ) } } fun formatDuration(seconds: Int): String { val mins = seconds / 60 ; val secs = seconds % 60 ;return String.format("%02d:%02d", mins, secs) }
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ActiveCallScreenPreview() {
    MaterialTheme {
        // Mock call data
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF70CEE3),
                                        Color(0xFF129FA9)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Jean Dupont",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "05:32",
                        fontSize = 16.sp,
                        color = Color(0xFFCCCCCC)
                    )
                }

                // Video Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Vidéo de l'accompagnant",
                            color = Color(0xFF757575),
                            fontSize = 14.sp
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 40.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = Icons.Default.Mic,
                        backgroundColor = Color(0xFF424242),
                        onClick = {}
                    )

                    CallControlButton(
                        icon = Icons.Default.Videocam,
                        backgroundColor = Color(0xFF424242),
                        onClick = {}
                    )

                    FloatingActionButton(
                        onClick = {},
                        containerColor = Color(0xFFE53935),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Terminer",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    CallControlButton(
                        icon = Icons.Default.FlashOff,
                        backgroundColor = Color(0xFF424242),
                        onClick = {}
                    )

                    CallControlButton(
                        icon = Icons.Default.VolumeUp,
                        backgroundColor = Color(0xFF424242),
                        onClick = {}
                    )
                }
            }
        }
    }
}
