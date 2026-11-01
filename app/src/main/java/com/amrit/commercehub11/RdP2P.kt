package com.amrit.commercehub11

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Hamesha-on peer-to-peer link (WebRTC DataChannel).
 * Dono online hote hi apne aap judta hai. Messages, typing, ticks aur files seedha isi se jate hain.
 * Judne se pehle / toot jaye to server (Supabase) fallback chalta hai.
 */
object RdP2P {
    var open by mutableStateOf(false)

    private var pc: PeerConnection? = null
    private var dc: DataChannel? = null
    private var sid = ""
    private var connecting = false
    private var remoteSet = false
    private val pendingIce = ArrayList<IceCandidate>()
    private var watch: Job? = null
    private val rxExec = Executors.newSingleThreadExecutor()
    private var rxOut: OutputStream? = null
    private var rxFile: File? = null
    private var rxEvt: JSONObject? = null

    private fun offerer(): Boolean = RdNet.ME < RdNet.PEER

    fun start() {
        watch?.cancel()
        watch = RdNet.scope.launch {
            while (isActive) {
                delay(3000)
                if (RdNet.running && RdNet.peerOnline && !open && Conn.isOnline()) connect()
            }
        }
    }

    fun stop() { watch?.cancel(); close() }

    fun onPeerPresence(online: Boolean) {
        if (online) { if (!open) RdNet.scope.launch { delay(200); connect() } } else close()
    }

    fun connect() {
        if (open || connecting || !RdNet.peerOnline) return
        if (!offerer()) { RdNet.signal(JSONObject().put("t", "p2p").put("k", "hello")); return }
        startOffer()
    }

    private fun build(): PeerConnection? {
        RdCall.ensureFactory()
        val f = RdCall.pcFactory ?: return null
        val cfg = PeerConnection.RTCConfiguration(RdCall.iceServers())
        cfg.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        val p = f.createPeerConnection(cfg, object : PeerConnection.Observer {
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                RdNet.scope.launch {
                    when (s) {
                        PeerConnection.IceConnectionState.FAILED -> close()
                        PeerConnection.IceConnectionState.DISCONNECTED -> { delay(5000); if (pc?.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED) close() }
                        else -> {}
                    }
                }
            }
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(c: IceCandidate?) {
                if (c == null) return
                RdNet.signal(JSONObject().put("t", "p2p").put("k", "ice").put("sid", sid).put("mid", c.sdpMid ?: "").put("idx", c.sdpMLineIndex).put("cand", c.sdp))
            }
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
            override fun onAddStream(s: MediaStream?) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onDataChannel(d: DataChannel?) { if (d != null) RdNet.scope.launch { attach(d) } }
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: RtpReceiver?, s: Array<out MediaStream>?) {}
        })
        pc = p
        return p
    }

    private fun startOffer() {
        connecting = true; sid = RdNet.uid(); remoteSet = false; pendingIce.clear()
        val p = build()
        if (p == null) { connecting = false; return }
        val ch = p.createDataChannel("rd", DataChannel.Init())
        if (ch != null) attach(ch)
        p.createOffer(object : SdpAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                p.setLocalDescription(SdpAdapter(), desc)
                RdNet.signal(JSONObject().put("t", "p2p").put("k", "offer").put("sid", sid).put("sdp", desc.description))
            }
        }, MediaConstraints())
        val mySid = sid
        RdNet.scope.launch { delay(10000); if (!open && connecting && sid == mySid) close() }
    }

    private fun attach(ch: DataChannel) {
        dc = ch
        ch.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                val st = ch.state()
                RdNet.scope.launch {
                    if (st == DataChannel.State.OPEN) {
                        if (dc === ch && !open) { open = true; connecting = false; RdNet.onP2pChange(true) }
                    } else if (st == DataChannel.State.CLOSED || st == DataChannel.State.CLOSING) {
                        if (dc === ch) close()
                    }
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                val bin = buffer.binary
                rxExec.execute { process(bytes, bin) }
            }
        })
    }

    private fun closeRx() {
        try { rxOut?.close() } catch (_: Throwable) {}
        rxOut = null; rxFile = null; rxEvt = null
    }

    private fun process(bytes: ByteArray, binary: Boolean) {
        if (binary) { try { rxOut?.write(bytes) } catch (_: Throwable) {}; return }
        val o = try { JSONObject(String(bytes, Charsets.UTF_8)) } catch (_: Throwable) { return }
        when (o.optString("t")) {
            "fh" -> {
                closeRx()
                val f = File(RdStore.app.cacheDir, "rx_" + o.optString("id").replace(Regex("[^\\w]"), "_"))
                rxFile = f; rxEvt = o
                rxOut = try { f.outputStream().buffered() } catch (_: Throwable) { null }
            }
            "fe" -> {
                val f = rxFile; val e = rxEvt
                closeRx()
                if (f != null && e != null) RdNet.scope.launch { RdNet.p2pMedia(e, f) }
            }
            else -> RdNet.scope.launch { RdNet.p2pEvt(o) }
        }
    }

    fun sendEvt(o: JSONObject): Boolean {
        val ch = dc ?: return false
        if (!open) return false
        return try { ch.send(DataChannel.Buffer(ByteBuffer.wrap(o.toString().toByteArray(Charsets.UTF_8)), false)) } catch (_: Throwable) { false }
    }

    suspend fun sendFile(header: JSONObject, file: File, onProg: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val ch = dc ?: return@withContext false
        if (!open) return@withContext false
        try {
            val h = JSONObject(header.toString()).put("t", "fh")
            if (!ch.send(DataChannel.Buffer(ByteBuffer.wrap(h.toString().toByteArray(Charsets.UTF_8)), false))) return@withContext false
            val total = file.length().coerceAtLeast(1L)
            var done = 0L
            file.inputStream().use { ins ->
                val buf = ByteArray(15000)
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    while (ch.bufferedAmount() > 1500000L) { delay(15); if (!open) return@withContext false }
                    if (!ch.send(DataChannel.Buffer(ByteBuffer.wrap(buf.copyOf(n)), true))) return@withContext false
                    done += n
                    onProg((done * 100 / total).toInt())
                }
            }
            val end = JSONObject().put("t", "fe").put("id", header.optString("id")).toString()
            ch.send(DataChannel.Buffer(ByteBuffer.wrap(end.toByteArray(Charsets.UTF_8)), false))
        } catch (_: Throwable) { false }
    }

    fun close() {
        val was = open
        open = false; connecting = false
        try { dc?.close() } catch (_: Throwable) {}
        try { pc?.close() } catch (_: Throwable) {}
        dc = null; pc = null; remoteSet = false
        rxExec.execute { closeRx() }
        if (was) RdNet.onP2pChange(false)
    }

    fun onSignal(e: JSONObject) {
        when (e.s("k")) {
            "hello" -> if (offerer()) connect()
            "offer" -> {
                if (offerer()) return
                close()
                sid = e.s("sid"); connecting = true; remoteSet = false; pendingIce.clear()
                val p = build() ?: return
                p.setRemoteDescription(object : SdpAdapter() {
                    override fun onSetSuccess() {
                        remoteSet = true
                        pendingIce.forEach { p.addIceCandidate(it) }; pendingIce.clear()
                        p.createAnswer(object : SdpAdapter() {
                            override fun onCreateSuccess(desc: SessionDescription?) {
                                if (desc == null) return
                                p.setLocalDescription(SdpAdapter(), desc)
                                RdNet.signal(JSONObject().put("t", "p2p").put("k", "answer").put("sid", sid).put("sdp", desc.description))
                            }
                        }, MediaConstraints())
                    }
                }, SessionDescription(SessionDescription.Type.OFFER, e.s("sdp")))
                val mySid = sid
                RdNet.scope.launch { delay(10000); if (!open && connecting && sid == mySid) close() }
            }
            "answer" -> if (e.s("sid") == sid && offerer()) {
                val p = pc ?: return
                p.setRemoteDescription(object : SdpAdapter() {
                    override fun onSetSuccess() {
                        remoteSet = true
                        pendingIce.forEach { p.addIceCandidate(it) }; pendingIce.clear()
                    }
                }, SessionDescription(SessionDescription.Type.ANSWER, e.s("sdp")))
            }
            "ice" -> if (e.s("sid") == sid) {
                val c = IceCandidate(e.s("mid"), e.optInt("idx"), e.s("cand"))
                val p = pc
                if (remoteSet && p != null) p.addIceCandidate(c) else pendingIce.add(c)
            }
        }
    }
}
