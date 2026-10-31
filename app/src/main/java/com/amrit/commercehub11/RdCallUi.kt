package com.amrit.commercehub11

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
private fun RtcView(modifier: Modifier, local: Boolean) {
    val ctx = LocalContext.current
    val egl = RdCall.eglContext ?: return
    val view = remember {
        SurfaceViewRenderer(ctx).apply {
            init(egl, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
            setMirror(local)
            if (local) setZOrderMediaOverlay(true)
            if (local) RdCall.attachLocal(this) else RdCall.attachRemote(this)
        }
    }
    DisposableEffect(view) { onDispose { RdCall.detach(view); try { view.release() } catch (_: Throwable) {} } }
    AndroidView(factory = { view }, modifier = modifier)
}

@Composable
private fun Ctl(icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(58.dp).clip(CircleShape).background(if (on) Color.White else Color.White.copy(alpha = 0.16f)).tap { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (on) Color.Black else Color.White, modifier = Modifier.size(26.dp)) }
}

/** Poori screen call UI: incoming / outgoing / connected (voice + HD video + screen share) */
@Composable
fun CallHost() {
    val st = RdCall.state
    if (st == "idle") return
    val ctx = LocalContext.current
    val name = RdStore.nick("peer").ifEmpty { RdCfg.NAMES[RdNet.PEER] ?: "Call" }
    val video = RdCall.video
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(RdCall.connectedAt) { while (RdCall.connectedAt > 0) { now = System.currentTimeMillis(); delay(1000) } }
    val secs = if (RdCall.connectedAt > 0) ((now - RdCall.connectedAt) / 1000).coerceAtLeast(0) else 0L

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        val micOk = res[Manifest.permission.RECORD_AUDIO] == true
        val camOk = !RdCall.video || res[Manifest.permission.CAMERA] == true
        if (micOk && camOk) RdCall.accept() else { Toaster.show("Permission allow karein"); RdCall.decline() }
    }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val d = r.data
        if (r.resultCode == Activity.RESULT_OK && d != null) {
            try { ContextCompat.startForegroundService(ctx, Intent(ctx, ScreenShareService::class.java).putExtra("data", d)) } catch (_: Throwable) {}
        }
    }
    fun acceptCall() {
        val need = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECORD_AUDIO)
        if (video && ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.CAMERA)
        if (need.isEmpty()) RdCall.accept() else perm.launch(need.toTypedArray())
    }

    BackHandler { if (st == "incoming") RdCall.decline() else RdCall.hangup() }

    val inf = rememberInfiniteTransition(label = "ring")
    val pulse by inf.animateFloat(1f, 1.35f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "pulse")
    val fade by inf.animateFloat(0.45f, 0f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "fade")

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B141A), Color(0xFF16262F), Color(0xFF0B141A))))
    ) {
        if (video && st == "connected") RtcView(Modifier.fillMaxSize(), false)
        if (!(video && st == "connected")) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(170.dp).scale(pulse).alpha(fade).clip(CircleShape).background(Color(0xFF00A884)))
                RdAvatar("p2p", 150.dp)
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = Color.White, fontFamily = FontFamily.Serif, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (st == "connected") "%02d:%02d".format(secs / 60, secs % 60) else RdCall.info,
                color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp
            )
            if (video && st == "connected") Text(
                "HD ${RdCall.quality}p" + if (RdCall.sharing) " \u2022 Sharing screen" else "",
                color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.weight(1f))

            if (st == "incoming") {
                Row(Modifier.fillMaxWidth().padding(bottom = 40.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0xFFE53935)).tap { RdCall.decline() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Text("Decline", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0xFF00A884)).tap { acceptCall() }, contentAlignment = Alignment.Center) {
                            Icon(if (video) Icons.Filled.Videocam else Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Text("Accept", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                if (video && st == "connected") {
                    Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(144, 360, 720, 1080).forEach { q ->
                            Text(
                                "${q}p", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (RdCall.quality == q) Color.Black else Color.White,
                                modifier = Modifier.clip(RoundedCornerShape(50))
                                    .background(if (RdCall.quality == q) Color.White else Color.White.copy(alpha = 0.16f))
                                    .tap { RdCall.changeQuality(q) }.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Row(
                    Modifier.padding(bottom = 28.dp).clip(RoundedCornerShape(40.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Ctl(if (RdCall.muted) Icons.Filled.MicOff else Icons.Filled.Mic, RdCall.muted) { RdCall.toggleMute() }
                    Ctl(Icons.Filled.VolumeUp, RdCall.speaker) { RdCall.toggleSpeaker() }
                    if (video) {
                        Ctl(if (RdCall.camOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam, RdCall.camOff) { RdCall.toggleCam() }
                        Ctl(Icons.Filled.Cameraswitch, false) { RdCall.flipCamera() }
                        Ctl(if (RdCall.sharing) Icons.Filled.StopScreenShare else Icons.Filled.ScreenShare, RdCall.sharing) {
                            if (RdCall.sharing) RdCall.stopScreen()
                            else {
                                try {
                                    val mp = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                    shareLauncher.launch(mp.createScreenCaptureIntent())
                                } catch (_: Throwable) { Toaster.show("Screen share available nahi") }
                            }
                        }
                    }
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE53935)).tap { RdCall.hangup() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
        if (video && st != "incoming") {
            RtcView(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 96.dp, end = 14.dp).size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(16.dp)).border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                true
            )
        }
    }
}
