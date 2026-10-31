package com.amrit.commercehub11

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

class Bubble(val id: Long, val sender: String) {
    var text by mutableStateOf("")
    var status by mutableStateOf("")
    var localImage by mutableStateOf<File?>(null)
    var imageUrl by mutableStateOf<String?>(null)
    var videoUrl by mutableStateOf<String?>(null)
    var videoLoop by mutableStateOf(false)
    var videoControls by mutableStateOf(false)
    var audioUrl by mutableStateOf<String?>(null)
    var fileLabel by mutableStateOf<String?>(null)
    var caption by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var timerMs by mutableStateOf(-1L)
    var fading by mutableStateOf(false)
}

/** global.js ka Telegram support chat (Developer se baat) */
object SupportChat {
    const val TOKEN = "9066479270:AAFN07wjNP6SD4LPMveU6rkfg7VP5JpNEug"
    const val CHAT_ID = "8871892242"

    val bubbles = mutableStateListOf<Bubble>()
    private var nextId = 1L
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun newBubble(sender: String): Bubble {
        val b = Bubble(nextId++, sender)
        bubbles.add(b)
        return b
    }

    fun sendText(text: String, identity: String) {
        val b = newBubble("user")
        b.text = "You: $text"
        b.timerMs = 1000
        scope.launch {
            val md = JSONObject()
                .put("chat_id", CHAT_ID)
                .put("text", "Identity: `$identity`\nMessage: $text")
                .put("parse_mode", "Markdown")
            val r = Tg.post(TOKEN, "sendMessage", md)
            if (r == null || !r.optBoolean("ok")) {
                // Markdown parse fail ho to plain text me bhejo
                val plain = JSONObject()
                    .put("chat_id", CHAT_ID)
                    .put("text", "Identity: $identity\nMessage: $text")
                Tg.post(TOKEN, "sendMessage", plain)
            }
        }
    }

    fun sendMedia(p: PickedFile, text: String, identity: String) {
        val b = newBubble("user")
        val kind = mediaKind(p.mime)
        if (kind == "image") b.localImage = p.file
        b.fileLabel = if (kind == "image") null else p.name
        if (text.isNotEmpty()) b.text = "You: $text"
        b.status = "Sending… 0%"
        scope.launch {
            var method = "sendDocument"; var field = "document"
            if (p.mime == "image/gif") { method = "sendAnimation"; field = "animation" }
            else if (kind == "image" && p.size <= 10L * 1048576) { method = "sendPhoto"; field = "photo" }
            else if (kind == "video") { method = "sendVideo"; field = "video" }
            val caption = "Identity: $identity" + if (text.isNotEmpty()) "\nMessage: $text" else ""
            val fields = mapOf("chat_id" to CHAT_ID, "caption" to caption)
            var r = Tg.upload(TOKEN, method, fields, field, p.file, p.name, p.mime) { pc -> b.status = "Sending… $pc%" }
            if ((r == null || !r.optBoolean("ok")) && method == "sendPhoto") {
                r = Tg.upload(TOKEN, "sendDocument", fields, "document", p.file, p.name, p.mime) { pc -> b.status = "Sending… $pc%" }
            }
            val ok = r != null && r.optBoolean("ok")
            b.status = if (ok) "Sent" else "Failed"
            b.timerMs = if (ok) 1000 else 3000
        }
    }

    fun sendVoice(file: File, mime: String, identity: String) {
        val b = newBubble("user")
        b.fileLabel = "Voice recording"
        b.status = "Sending…"
        scope.launch {
            val ogg = mime.contains("ogg")
            val method = if (ogg) "sendVoice" else "sendDocument"
            val field = if (ogg) "voice" else "document"
            val name = "voice_" + System.currentTimeMillis() + if (ogg) ".ogg" else ".m4a"
            val r = Tg.upload(TOKEN, method, mapOf("chat_id" to CHAT_ID, "caption" to "Identity: $identity"), field, file, name, mime)
            val ok = r != null && r.optBoolean("ok")
            b.status = if (ok) "Sent" else "Failed"
            b.timerMs = if (ok) 1000 else 3000
            try { file.delete() } catch (_: Throwable) {}
        }
    }

    private var polling = false

    suspend fun pollOnce(ctx: Context) {
        if (polling || !Conn.isOnline()) return
        polling = true
        try {
            val last = Prefs.supportTgOffset
            val r = Tg.get(TOKEN, "getUpdates?offset=${last + 1}&timeout=0") ?: return
            if (!r.optBoolean("ok")) return
            val arr = r.optJSONArray("result") ?: return
            for (i in 0 until arr.length()) {
                val u = arr.getJSONObject(i)
                Prefs.supportTgOffset = u.optLong("update_id")
                if (!Prefs.eyeLiveMessages) continue
                val m = u.optJSONObject("message") ?: u.optJSONObject("edited_message") ?: continue
                if (m.optJSONObject("chat")?.optLong("id").toString() != CHAT_ID) continue
                if (m.has("text")) {
                    val b = newBubble("admin")
                    b.text = "Developer: " + m.optString("text")
                    b.timerMs = 3000
                } else {
                    showIncomingMedia(ctx, m)
                }
            }
        } finally {
            polling = false
        }
    }

    private suspend fun showIncomingMedia(ctx: Context, m: JSONObject) {
        var fileId: String? = null
        var kind = "file"
        var label = ""
        if (m.has("photo")) {
            val ph = m.getJSONArray("photo")
            fileId = ph.getJSONObject(ph.length() - 1).optString("file_id"); kind = "image"
        } else if (m.has("animation")) { fileId = m.getJSONObject("animation").optString("file_id"); kind = "video-loop" }
        else if (m.has("video")) { fileId = m.getJSONObject("video").optString("file_id"); kind = "video" }
        else if (m.has("video_note")) { fileId = m.getJSONObject("video_note").optString("file_id"); kind = "video" }
        else if (m.has("sticker")) {
            val s = m.getJSONObject("sticker")
            if (s.optBoolean("is_animated")) return
            fileId = s.optString("file_id"); kind = if (s.optBoolean("is_video")) "video-loop" else "image"
        } else if (m.has("voice")) { fileId = m.getJSONObject("voice").optString("file_id"); kind = "audio" }
        else if (m.has("audio")) { fileId = m.getJSONObject("audio").optString("file_id"); kind = "audio" }
        else if (m.has("document")) {
            val d = m.getJSONObject("document")
            val mt = d.optString("mime_type")
            label = d.optString("file_name", "file")
            if (mt.startsWith("image/") && mt != "image/gif") { fileId = d.optString("file_id"); kind = "image" }
            else if (mt == "image/gif") { fileId = d.optString("file_id"); kind = "video-loop" }
            else if (mt.startsWith("video/")) { fileId = d.optString("file_id"); kind = "video" }
        } else return

        val b = newBubble("admin")
        val cap = m.optString("caption", "")
        if (cap.isNotEmpty()) b.caption = "Developer: $cap"
        if (fileId == null) {
            b.fileLabel = "Developer: $label"
            b.timerMs = 3000
            return
        }
        b.loading = true
        val url = Tg.fileUrl(TOKEN, fileId)
        b.loading = false
        if (url == null) {
            b.fileLabel = "Developer: Media load nahi hua."
            b.timerMs = 3000
            return
        }
        when (kind) {
            "image" -> {
                Images.remote(ctx, url, 900)
                b.imageUrl = url
                b.timerMs = 3000
            }
            "audio" -> { b.audioUrl = url; b.timerMs = 3000 + 4000 }
            else -> {
                b.videoUrl = url
                b.videoLoop = kind == "video-loop"
                b.videoControls = kind == "video"
                b.timerMs = 3000 + 4000
            }
        }
    }
}

@Composable
fun FloatingAvatar(container: IntSize, onClick: () -> Unit) {
    val pal = LocalPal.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = with(density) { 96.dp.toPx() }
    val margin = with(density) { 10.dp.toPx() }
    val topLimit = with(density) { 20.dp.toPx() }
    val scope = rememberCoroutineScope()
    val ax = remember { Animatable(0f) }
    val ay = remember { Animatable(0f) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(container) {
        if (!ready && container.width > 0) {
            ax.snapTo(container.width - sizePx - with(density) { 16.dp.toPx() })
            ay.snapTo(container.height - sizePx - with(density) { 96.dp.toPx() })
            ready = true
        }
    }
    if (!ready) return

    Box(
        modifier = Modifier
            .offset { IntOffset(ax.value.roundToInt(), ay.value.roundToInt()) }
            .size(96.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF4F46E5), Color(0xFF9333EA), Color(0xFFEC4899)),
                    start = Offset(0f, sizePx), end = Offset(sizePx, 0f)
                )
            )
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .pointerInput(container) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch {
                            ax.snapTo(ax.value + drag.x)
                            val maxY = container.height - sizePx - topLimit
                            ay.snapTo((ay.value + drag.y).coerceIn(topLimit, maxY))
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            val center = ax.value + sizePx / 2f
                            val target = if (center < container.width / 2f) margin else container.width - sizePx - margin
                            ax.animateTo(target, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        }
                    }
                )
            }
            .padding(4.dp)
    ) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(Slate.s900)
                .border(3.dp, if (pal.dark) Slate.s800 else Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            RemoteImage(
                "https://api.dicebear.com/7.x/bottts/png?seed=CoolBuddy99&backgroundColor=b6e3f4,c0aede,d1d4f9&size=256",
                Modifier.fillMaxSize(), ContentScale.Crop, 300
            )
        }
    }
}

@Composable
private fun CircleBtn(icon: ImageVector, onClick: () -> Unit, filled: Boolean = false, tint: Color? = null, bg: Color? = null) {
    val pal = LocalPal.current
    val background = bg ?: if (filled) (if (pal.dark) Color.White else Color.Black) else pal.chip
    val fg = tint ?: if (filled) (if (pal.dark) Color.Black else Color.White) else pal.text
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(background)
            .then(if (filled) Modifier else Modifier.border(1.dp, pal.chipLine, CircleShape))
            .tap { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = fg, modifier = Modifier.size(if (filled) 16.dp else 20.dp)) }
}

@Composable
private fun BubbleView(b: Bubble) {
    val pal = LocalPal.current
    val ctx = LocalContext.current
    LaunchedEffect(b.timerMs) {
        if (b.timerMs >= 0) {
            delay(b.timerMs)
            b.fading = true
            delay(400)
            SupportChat.bubbles.remove(b)
        }
    }
    val alpha by androidx.compose.animation.core.animateFloatAsState(if (b.fading) 0f else 1f, tween(400), label = "fade")
    val isUser = b.sender == "user"
    val bg = if (isUser) (if (pal.dark) Color.White else Color.Black) else (if (pal.dark) Slate.s700 else Slate.s200)
    val fg = if (isUser) (if (pal.dark) Color.Black else Color.White) else (if (pal.dark) Slate.s100 else Slate.s900)
    Row(Modifier.fillMaxWidth().alpha(alpha), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (b.loading) Text("Loading…", fontSize = 12.sp, color = fg.copy(alpha = 0.7f))
            b.localImage?.let { FileImage(it, Modifier.heightIn(max = 160.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Fit, 700) }
            b.imageUrl?.let { RemoteImage(it, Modifier.heightIn(max = 176.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Fit, 900) }
            b.videoUrl?.let { url ->
                AndroidView(
                    modifier = Modifier.size(width = 220.dp, height = 160.dp).clip(RoundedCornerShape(8.dp)),
                    factory = { c ->
                        VideoView(c).apply {
                            setVideoURI(Uri.parse(url))
                            setOnPreparedListener { mp ->
                                mp.isLooping = b.videoLoop
                                if (b.videoLoop) mp.setVolume(0f, 0f)
                                if (b.timerMs < 0) b.timerMs = 3000
                                start()
                            }
                            setOnErrorListener { _, _, _ -> if (b.timerMs < 0) b.timerMs = 3000; true }
                        }
                    }
                )
                LaunchedEffect(url) { delay(8000); if (b.timerMs < 0) b.timerMs = 3000 }
            }
            b.audioUrl?.let { url ->
                Text("Developer: Voice / Audio", fontSize = 12.sp, color = fg)
                DisposableEffect(url) {
                    val mp = MediaPlayer()
                    try {
                        mp.setDataSource(url)
                        mp.setOnPreparedListener { it.start() }
                        mp.prepareAsync()
                    } catch (_: Throwable) {}
                    onDispose { try { mp.release() } catch (_: Throwable) {} }
                }
            }
            b.fileLabel?.let { Text(it, fontSize = 12.sp, color = fg) }
            if (b.text.isNotEmpty()) Text(b.text, fontSize = 12.sp, color = fg)
            b.caption?.let { Text(it, fontSize = 12.sp, color = fg) }
            if (b.status.isNotEmpty()) Text(b.status, fontSize = 10.sp, color = fg.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun SupportDrawer(onClose: () -> Unit) {
    val pal = LocalPal.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var identity by remember { mutableStateOf(Prefs.identity) }
    var message by remember { mutableStateOf("") }
    var eye by remember { mutableStateOf(Prefs.eyeLiveMessages) }
    var attachOpen by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PickedFile?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recSecs by remember { mutableIntStateOf(0) }
    val recorder = remember { VoiceRecorder(ctx) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.stop(false) } }

    // Developer ke messages poll (4 sec)
    LaunchedEffect(Unit) {
        while (true) {
            SupportChat.pollOnce(ctx)
            delay(4000)
        }
    }

    fun accept(p: PickedFile?) {
        if (p == null) { Toaster.show("File nahi mil payi"); return }
        if (p.size > MAX_UPLOAD) { Toaster.show("File 50 MB se badi hai. 50 MB tak file hi bheji ja sakti hai."); return }
        pending?.let { try { it.file.delete() } catch (_: Throwable) {} }
        pending = p
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { accept(copyUriToCache(ctx, uri)) }
    }
    val pickAny = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { accept(copyUriToCache(ctx, uri)) }
    }
    val takePic = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null && f.exists()) accept(PickedFile(f, f.name, "image/jpeg", f.length()))
    }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (recorder.start()) { recording = true; recSecs = 0 } else Toaster.show("Recording shuru nahi ho paayi")
        } else Toaster.show("Microphone permission allow karke dobara try karein.")
    }

    fun launchCam() {
        val f = newCameraFile(ctx)
        cameraFile = f
        try { takePic.launch(fileUri(ctx, f)) } catch (_: Throwable) { Toaster.show("Camera open nahi ho paya") }
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCam() else Toaster.show("Camera permission allow karein")
    }
    fun openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCam()
        else camPerm.launch(Manifest.permission.CAMERA)
    }

    fun stopAndSendVoice() {
        if (!recording) return
        val mime = recorder.mime
        val f = recorder.stop(true)
        recording = false
        if (f != null) SupportChat.sendVoice(f, mime, identity)
    }

    LaunchedEffect(recording) {
        while (recording) {
            delay(1000)
            recSecs += 1
            if (recSecs >= 300) { stopAndSendVoice() }
        }
    }

    fun send() {
        val text = message.trim()
        val p = pending
        if (p != null) {
            SupportChat.sendMedia(p, text, identity)
            pending = null
            message = ""
            return
        }
        if (text.isEmpty()) return
        SupportChat.sendText(text, identity)
        message = ""
    }

    fun closeDrawer() {
        if (recording) { recorder.stop(false); recording = false }
        onClose()
    }

    androidx.activity.compose.BackHandler { closeDrawer() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).plainClick { }.imePadding()) {
        val maxH = maxHeight * 0.88f
        Column(
            Modifier.align(Alignment.BottomCenter).widthIn(max = 512.dp).fillMaxWidth().heightIn(max = maxH)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(pal.header)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(pal.accent).padding(6.dp)) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = pal.onAccent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "For any issue please contact us and send message", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = pal.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.alpha(if (eye) 1f else 0.5f).clip(RoundedCornerShape(12.dp)).background(pal.chip)
                        .tap { eye = !eye; Prefs.eyeLiveMessages = eye }.padding(8.dp)
                ) { Icon(if (eye) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = if (pal.dark) Slate.s300 else Slate.s700, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(pal.chip).tap { closeDrawer() }.padding(8.dp)) {
                    Icon(Icons.Filled.Close, null, tint = if (pal.dark) Slate.s300 else Slate.s700, modifier = Modifier.size(16.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(pal.chip))

            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Identity
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (pal.dark) Slate.s800.copy(alpha = 0.5f) else Slate.s50)
                        .border(1.dp, pal.cardLine, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("YOUR IDENTITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pal.sub, letterSpacing = 0.8.sp)
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.header)
                            .border(1.dp, pal.chipLine, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (identity.isEmpty()) Text("Enter your identity...", fontSize = 12.sp, color = Slate.s400)
                        BasicTextField(
                            value = identity, onValueChange = { identity = it; Prefs.identity = it },
                            singleLine = true, visualTransformation = PasswordVisualTransformation(),
                            textStyle = TextStyle(fontSize = 12.sp, color = pal.text, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                            cursorBrush = SolidColor(pal.text), modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Live messages
                val listState = rememberScrollState()
                LaunchedEffect(SupportChat.bubbles.size) { listState.animateScrollTo(listState.maxValue) }
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (pal.dark) Slate.s800.copy(alpha = 0.5f) else Slate.s50)
                        .border(1.dp, pal.cardLine, RoundedCornerShape(16.dp))
                        .verticalScroll(listState).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
                ) {
                    if (SupportChat.bubbles.isEmpty()) {
                        Text(
                            "Send a message to start conversation.", fontSize = 12.sp, color = Slate.s400, fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                    SupportChat.bubbles.toList().forEach { BubbleView(it) }
                }

                // Attachment preview
                pending?.let { p ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.header)
                            .border(1.dp, pal.chipLine, RoundedCornerShape(16.dp))
                    ) {
                        if (mediaKind(p.mime) == "image") {
                            Box(Modifier.fillMaxWidth().heightIn(max = 256.dp).background(Slate.s950), contentAlignment = Alignment.Center) {
                                FileImage(p.file, Modifier.fillMaxWidth().heightIn(max = 256.dp), ContentScale.Fit, 1000)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(pal.chip), contentAlignment = Alignment.Center) {
                                Icon(
                                    if (mediaKind(p.mime) == "image") Icons.Outlined.Image else Icons.Outlined.InsertDriveFile,
                                    null, tint = pal.body, modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when (mediaKind(p.mime)) { "image" -> "PHOTO"; "video" -> "VIDEO"; else -> "FILE" },
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate.s400, letterSpacing = 0.6.sp
                                )
                                Text(
                                    p.name + " · " + fmtSize(p.size), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = pal.text, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(36.dp).clip(CircleShape).background(pal.chip).tap {
                                try { p.file.delete() } catch (_: Throwable) {}
                                pending = null
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, null, tint = pal.text, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Attach menu
                if (attachOpen) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.header)
                            .border(1.dp, pal.chipLine, RoundedCornerShape(16.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttachItem("Camera", Icons.Filled.CameraAlt, Modifier.weight(1f)) {
                            attachOpen = false
                            openCamera()
                        }
                        AttachItem("Photo & Video", Icons.Outlined.Image, Modifier.weight(1f)) {
                            attachOpen = false
                            pickMedia.launch(arrayOf("image/*", "video/*"))
                        }
                        AttachItem("File", Icons.Outlined.InsertDriveFile, Modifier.weight(1f)) {
                            attachOpen = false
                            pickAny.launch(arrayOf("*/*"))
                        }
                    }
                }

                // Recording status
                if (recording) {
                    val inf = rememberInfiniteTransition(label = "rec")
                    val a by inf.animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot")
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (pal.dark) Slate.s800 else Slate.s50)
                            .border(1.dp, pal.chipLine, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).alpha(a).clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(Modifier.width(8.dp))
                        Text("Recording", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                        Spacer(Modifier.width(8.dp))
                        Text("%02d:%02d".format(recSecs / 60, recSecs % 60), fontSize = 12.sp, color = pal.sub)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Send", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (pal.dark) Color.Black else Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (pal.dark) Color.White else Color.Black)
                                .tap { stopAndSendVoice() }.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Chat bar
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleBtn(Icons.Filled.Add, { if (!recording) attachOpen = !attachOpen })
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(pal.chip)
                            .border(1.dp, pal.chipLine, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (message.isEmpty()) Text("Type message here...", fontSize = 12.sp, color = Slate.s400)
                        BasicTextField(
                            value = message, onValueChange = { message = it }, singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, color = pal.text), cursorBrush = SolidColor(pal.text),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { send() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CircleBtn(
                        if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                        {
                            if (recording) stopAndSendVoice()
                            else {
                                attachOpen = false
                                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    if (recorder.start()) { recording = true; recSecs = 0 } else Toaster.show("Recording shuru nahi ho paayi")
                                } else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        tint = if (recording) Color(0xFFDC2626) else null,
                        bg = if (recording) Color(0xFFFEF2F2) else null
                    )
                    CircleBtn(Icons.AutoMirrored.Filled.Send, { send() }, filled = true)
                }
            }
        }
    }
}

@Composable
private fun AttachItem(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val pal = LocalPal.current
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).tap { onClick() }.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(pal.chip), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = pal.text, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pal.text, maxLines = 1)
    }
}
