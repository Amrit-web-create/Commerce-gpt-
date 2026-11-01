package com.amrit.commercehub11

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoCapturer
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

open class SdpAdapter : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

/** Peer to peer audio / video call (WebRTC). Signaling RdNet ke realtime channel se. */
object RdCall {
    var state by mutableStateOf("idle")        // idle | outgoing | incoming | connected
    var video by mutableStateOf(false)
    var muted by mutableStateOf(false)
    var camOff by mutableStateOf(false)
    var speaker by mutableStateOf(false)
    var connectedAt by mutableLongStateOf(0L)
    var info by mutableStateOf("")
    var quality by mutableIntStateOf(1080)     // 144 / 360 / 720 / 1080
    var sharing by mutableStateOf(false)
    private var videoSender: RtpSender? = null
    private var screenCap: VideoCapturer? = null
    private var screenSource: VideoSource? = null
    private var screenTrack: VideoTrack? = null
    private var screenHelper: SurfaceTextureHelper? = null

    private lateinit var app: Context
    private var egl: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null
    private var videoSource: VideoSource? = null
    private var capturer: CameraVideoCapturer? = null
    private var helper: SurfaceTextureHelper? = null
    private var remoteTrack: VideoTrack? = null
    private var localView: SurfaceViewRenderer? = null
    private var remoteView: SurfaceViewRenderer? = null
    private var callId = ""
    private var incomingOffer: String? = null
    private var incoming = false
    private var remoteSet = false
    private val pendingIce = ArrayList<IceCandidate>()
    private var player: MediaPlayer? = null
    private var timeout: Job? = null
    private var ended = false
    private var offerSdp = ""
    private var gotAnswer = false

    fun init(ctx: Context) { app = ctx.applicationContext }

    val eglContext: EglBase.Context? get() = egl?.eglBaseContext
    val pcFactory: PeerConnectionFactory? get() = factory
    fun iceServers(): List<PeerConnection.IceServer> = servers()

    fun ensureFactory() {
        if (factory != null) return
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(app).createInitializationOptions())
        val e = EglBase.create()
        egl = e
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(e.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(e.eglBaseContext))
            .createPeerConnectionFactory()
    }

    // ------------------------------------------------------------ renderer attach (UI)
    fun attachLocal(r: SurfaceViewRenderer) { localView = r; videoTrack?.addSink(r) }
    fun attachRemote(r: SurfaceViewRenderer) { remoteView = r; remoteTrack?.addSink(r) }
    fun detach(r: SurfaceViewRenderer) {
        try { videoTrack?.removeSink(r) } catch (_: Throwable) {}
        try { remoteTrack?.removeSink(r) } catch (_: Throwable) {}
        if (localView === r) localView = null
        if (remoteView === r) remoteView = null
    }

    // ------------------------------------------------------------ pc
    private fun servers(): List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer()
    )

    private fun buildPc(): Boolean {
        ensureFactory()
        val f = factory ?: return false
        val cfg = PeerConnection.RTCConfiguration(servers())
        cfg.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        val p = f.createPeerConnection(cfg, object : PeerConnection.Observer {
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                RdNet.scope.launch {
                    when (s) {
                        PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> markConnected()
                        PeerConnection.IceConnectionState.FAILED -> finish("failed", true)
                        PeerConnection.IceConnectionState.DISCONNECTED -> { delay(6000); if (pc?.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED) finish("failed", true) }
                        else -> {}
                    }
                }
            }
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(c: IceCandidate?) {
                if (c == null) return
                RdNet.signal(JSONObject().put("t", "call").put("k", "ice").put("cid", callId)
                    .put("mid", c.sdpMid ?: "").put("idx", c.sdpMLineIndex).put("cand", c.sdp))
            }
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
            override fun onAddStream(s: MediaStream?) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onDataChannel(d: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: RtpReceiver?, s: Array<out MediaStream>?) {
                val t = r?.track()
                if (t is VideoTrack) {
                    remoteTrack = t
                    remoteView?.let { t.addSink(it) }
                }
            }
        }) ?: return false
        pc = p
        // audio
        val aSrc = f.createAudioSource(MediaConstraints())
        audioTrack = f.createAudioTrack("a0", aSrc)
        audioTrack?.setEnabled(!muted)
        p.addTrack(audioTrack, listOf("s0"))
        // video
        if (video) {
            try {
                val en = Camera2Enumerator(app)
                val name = en.deviceNames.firstOrNull { en.isFrontFacing(it) } ?: en.deviceNames.first()
                val cap = en.createCapturer(name, null)
                val src = f.createVideoSource(false)
                val h = SurfaceTextureHelper.create("capture", egl?.eglBaseContext)
                cap.initialize(h, app, src.capturerObserver)
                val d = dims(quality)
                cap.startCapture(d.first, d.second, 30)
                capturer = cap; videoSource = src; helper = h
                val vt = f.createVideoTrack("v0", src)
                videoTrack = vt
                localView?.let { vt.addSink(it) }
                videoSender = p.addTrack(vt, listOf("s0"))
                applyBitrate()
            } catch (_: Throwable) { Toaster.show("Camera start nahi hua") }
        }
        return true
    }

    private fun dims(q: Int): Pair<Int, Int> = when (q) {
        144 -> Pair(256, 144); 360 -> Pair(640, 360); 720 -> Pair(1280, 720); else -> Pair(1920, 1080)
    }

    private fun applyBitrate() {
        try {
            val sender = videoSender ?: return
            val prm = sender.getParameters()
            val bps = when (quality) { 144 -> 200000; 360 -> 700000; 720 -> 2500000; else -> 5000000 }
            prm.encodings.forEach { it.maxBitrateBps = bps; it.maxFramerate = 30 }
            sender.setParameters(prm)
        } catch (_: Throwable) {}
    }

    /** 144p se 1080p tak quality badalna */
    fun changeQuality(q: Int) {
        quality = q
        try { val d = dims(q); capturer?.changeCaptureFormat(d.first, d.second, 30) } catch (_: Throwable) {}
        applyBitrate()
    }

    /** Screen share: camera track ki jagah screen track bhejta hai (video call me) */
    fun startScreen(data: Intent) {
        val f = factory ?: return
        if (!video || videoSender == null) { Toaster.show("Screen share video call me chalta hai"); return }
        try {
            val cap = ScreenCapturerAndroid(data, object : MediaProjection.Callback() {
                override fun onStop() { RdNet.scope.launch { stopScreen() } }
            })
            val src = f.createVideoSource(true)
            val h = SurfaceTextureHelper.create("screen", egl?.eglBaseContext)
            cap.initialize(h, app, src.capturerObserver)
            val dm = app.resources.displayMetrics
            cap.startCapture(dm.widthPixels, dm.heightPixels, 20)
            val t = f.createVideoTrack("s1", src)
            screenCap = cap; screenSource = src; screenHelper = h; screenTrack = t
            videoSender?.setTrack(t, false)
            localView?.let { v -> videoTrack?.removeSink(v); t.addSink(v) }
            sharing = true
        } catch (_: Throwable) { Toaster.show("Screen share start nahi hua"); ScreenShareService.stop(app) }
    }

    fun stopScreen() {
        if (!sharing && screenCap == null) return
        try { videoSender?.setTrack(videoTrack, false) } catch (_: Throwable) {}
        localView?.let { v -> try { screenTrack?.removeSink(v) } catch (_: Throwable) {}; videoTrack?.addSink(v) }
        try { screenCap?.stopCapture() } catch (_: Throwable) {}
        try { screenCap?.dispose() } catch (_: Throwable) {}
        try { screenTrack?.dispose() } catch (_: Throwable) {}
        try { screenSource?.dispose() } catch (_: Throwable) {}
        try { screenHelper?.dispose() } catch (_: Throwable) {}
        screenCap = null; screenTrack = null; screenSource = null; screenHelper = null
        sharing = false
        ScreenShareService.stop(app)
    }

    private fun setAudioMode(on: Boolean) {
        try {
            val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (on) { am.mode = AudioManager.MODE_IN_COMMUNICATION; speaker = video; am.isSpeakerphoneOn = video }
            else { am.mode = AudioManager.MODE_NORMAL; am.isSpeakerphoneOn = false }
        } catch (_: Throwable) {}
    }

    fun toggleSpeaker() {
        speaker = !speaker
        try { (app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isSpeakerphoneOn = speaker } catch (_: Throwable) {}
    }

    fun toggleMute() { muted = !muted; audioTrack?.setEnabled(!muted) }
    fun toggleCam() { camOff = !camOff; videoTrack?.setEnabled(!camOff) }
    fun flipCamera() { try { capturer?.switchCamera(null) } catch (_: Throwable) {} }

    // ------------------------------------------------------------ ring
    private fun startRing() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val mp = MediaPlayer()
            mp.setDataSource(app, uri)
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            mp.isLooping = true
            mp.prepare(); mp.start()
            player = mp
        } catch (_: Throwable) {}
        try {
            val v = app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pat = longArrayOf(0, 800, 700)
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pat, 0)) else v.vibrate(pat, 0)
        } catch (_: Throwable) {}
    }

    private fun stopRing() {
        try { player?.stop() } catch (_: Throwable) {}
        try { player?.release() } catch (_: Throwable) {}
        player = null
        try { (app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel() } catch (_: Throwable) {}
        Notifier.cancelCall()
    }

    // ------------------------------------------------------------ actions
    fun start(isVideo: Boolean) {
        if (state != "idle" || !Conn.isOnline()) { if (!Conn.isOnline()) Toaster.show("You are offline"); return }
        ensureFactory()
        ended = false; incoming = false; video = isVideo; muted = false; camOff = false; remoteSet = false
        callId = RdNet.uid(); state = "outgoing"; info = "Calling..."
        pendingIce.clear()
        setAudioMode(true)
        if (!buildPc()) { finish("failed", false); return }
        val c = MediaConstraints()
        c.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        c.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
        pc?.createOffer(object : SdpAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                pc?.setLocalDescription(SdpAdapter(), desc)
                offerSdp = desc.description
                RdNet.signal(JSONObject().put("t", "call").put("k", "offer").put("cid", callId).put("video", isVideo).put("sdp", offerSdp))
            }
        }, c)
        gotAnswer = false
        RdNet.scope.launch {
            repeat(14) {
                delay(3000)
                if (state == "outgoing" && !gotAnswer && !incoming && offerSdp.isNotEmpty())
                    RdNet.signal(JSONObject().put("t", "call").put("k", "offer").put("cid", callId).put("video", isVideo).put("sdp", offerSdp))
            }
        }
        timeout?.cancel()
        timeout = RdNet.scope.launch { delay(45000); if (state == "outgoing") finish("noanswer", true) }
    }

    fun accept() {
        val offer = incomingOffer ?: return
        if (state != "incoming") return
        stopRing()
        timeout?.cancel()
        setAudioMode(true)
        if (!buildPc()) { finish("failed", true); return }
        state = "outgoing"; info = "Connecting..."
        pc?.setRemoteDescription(object : SdpAdapter() {
            override fun onSetSuccess() {
                remoteSet = true
                pendingIce.forEach { pc?.addIceCandidate(it) }; pendingIce.clear()
                val c = MediaConstraints()
                c.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                c.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (video) "true" else "false"))
                pc?.createAnswer(object : SdpAdapter() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        if (desc == null) return
                        pc?.setLocalDescription(SdpAdapter(), desc)
                        RdNet.signal(JSONObject().put("t", "call").put("k", "answer").put("cid", callId).put("sdp", desc.description))
                    }
                }, c)
            }
        }, SessionDescription(SessionDescription.Type.OFFER, offer))
    }

    fun decline() { if (state == "incoming") finish("declined", true) }
    fun hangup() {
        if (state == "idle") return
        finish(if (state == "connected") "hangup" else "cancel", true)
    }

    private fun markConnected() {
        if (state == "connected" || state == "idle") return
        timeout?.cancel()
        state = "connected"; connectedAt = System.currentTimeMillis(); info = ""
    }

    /** Peer ka signal (RdNet se) */
    fun onSignal(e: JSONObject) {
        val k = e.s("k"); val cid = e.s("cid")
        when (k) {
            "offer" -> {
                if (state != "idle" && cid == callId) return
                if (state != "idle") { RdNet.signal(JSONObject().put("t", "call").put("k", "end").put("cid", cid).put("why", "busy")); return }
                ended = false; incoming = true; callId = cid; video = e.optBoolean("video"); muted = false; camOff = false; remoteSet = false
                incomingOffer = e.s("sdp"); pendingIce.clear()
                state = "incoming"; info = if (video) "Incoming video call" else "Incoming voice call"
                startRing()
                if (!RdNet.foreground) Notifier.call(RdCfg.NAMES[RdNet.PEER] ?: "Call", info)
                timeout?.cancel()
                timeout = RdNet.scope.launch { delay(45000); if (state == "incoming") finish("missed", false) }
            }
            "answer" -> if (cid == callId && state == "outgoing" && !incoming && !gotAnswer) {
                gotAnswer = true
                pc?.setRemoteDescription(object : SdpAdapter() {
                    override fun onSetSuccess() {
                        remoteSet = true
                        pendingIce.forEach { pc?.addIceCandidate(it) }; pendingIce.clear()
                    }
                }, SessionDescription(SessionDescription.Type.ANSWER, e.s("sdp")))
                info = "Ringing..."
            }
            "ice" -> if (cid == callId) {
                val c = IceCandidate(e.s("mid"), e.optInt("idx"), e.s("cand"))
                if (remoteSet && pc != null) pc?.addIceCandidate(c) else pendingIce.add(c)
            }
            "end" -> if (cid == callId && state != "idle") {
                val why = e.s("why")
                finish(when (why) { "cancel" -> "missed"; "busy" -> "busy"; "declined" -> "declined"; else -> "remoteend" }, false)
            }
        }
    }

    private fun logText(why: String, secs: Long): String {
        val kind = if (video) "video" else "voice"
        val t = "%d:%02d".format(secs / 60, secs % 60)
        return when {
            why == "hangup" || why == "remoteend" -> (if (secs > 0) "${kind.replaceFirstChar { it.uppercase() }} call \u2022 $t" else "${kind.replaceFirstChar { it.uppercase() }} call ended")
            why == "missed" -> if (incoming) "Missed $kind call" else "No answer"
            why == "noanswer" -> "No answer"
            why == "declined" -> if (incoming) "Declined $kind call" else "Call declined"
            why == "cancel" -> "Cancelled $kind call"
            why == "busy" -> "User busy"
            else -> "Call failed"
        }
    }

    private fun resultOf(why: String, secs: Long): String = when {
        secs > 0 -> "connected"
        why == "declined" -> "declined"
        why == "busy" -> "busy"
        why == "noanswer" || why == "missed" -> "noanswer"
        why == "cancel" -> "cancel"
        else -> "failed"
    }

    private fun finish(why: String, notifyPeer: Boolean) {
        if (ended || state == "idle") return
        ended = true
        timeout?.cancel()
        if (notifyPeer) RdNet.signal(JSONObject().put("t", "call").put("k", "end").put("cid", callId).put("why", if (why == "hangup") "hangup" else if (why == "declined") "declined" else if (why == "cancel" || why == "noanswer") "cancel" else "hangup"))
        val secs = if (connectedAt > 0) (System.currentTimeMillis() - connectedAt) / 1000 else 0
        if (!incoming) RdNet.sendCallLog(video, resultOf(why, secs), secs)
        stopRing()
        stopScreen()
        try { capturer?.stopCapture() } catch (_: Throwable) {}
        try { capturer?.dispose() } catch (_: Throwable) {}
        try { videoTrack?.dispose() } catch (_: Throwable) {}
        try { videoSource?.dispose() } catch (_: Throwable) {}
        try { helper?.dispose() } catch (_: Throwable) {}
        try { audioTrack?.dispose() } catch (_: Throwable) {}
        try { pc?.close() } catch (_: Throwable) {}
        try { pc?.dispose() } catch (_: Throwable) {}
        videoSender = null; capturer = null; videoTrack = null; videoSource = null; helper = null; audioTrack = null; pc = null; remoteTrack = null
        setAudioMode(false)
        connectedAt = 0L; speaker = false
        state = "idle"; info = ""
        incomingOffer = null
    }
}
