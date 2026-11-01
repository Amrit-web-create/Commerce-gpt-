package com.amrit.commercehub11

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloat
import java.io.File
import java.text.BreakIterator

/** WhatsApp jaisa bubble: ek taraf chhoti si tail */
private class BubbleShape(val mine: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { 14.dp.toPx() }
        val tw = with(density) { 9.dp.toPx() }
        val th = with(density) { 12.dp.toPx() }
        val w = size.width; val h = size.height
        val p = Path()
        if (mine) {
            val x1 = w - tw
            p.moveTo(r, 0f); p.lineTo(w, 0f); p.lineTo(x1, th); p.lineTo(x1, h - r)
            p.arcTo(Rect(x1 - 2 * r, h - 2 * r, x1, h), 0f, 90f, false)
            p.lineTo(r, h)
            p.arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
            p.lineTo(0f, r)
            p.arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
        } else {
            val x0 = tw
            p.moveTo(0f, 0f); p.lineTo(w - r, 0f)
            p.arcTo(Rect(w - 2 * r, 0f, w, 2 * r), 270f, 90f, false)
            p.lineTo(w, h - r)
            p.arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
            p.lineTo(x0 + r, h)
            p.arcTo(Rect(x0, h - 2 * r, x0 + 2 * r, h), 90f, 90f, false)
            p.lineTo(x0, th)
        }
        p.close()
        return Outline.Generic(p)
    }
}

private fun dropLastGrapheme(s: String): String {
    if (s.isEmpty()) return s
    val bi = BreakIterator.getCharacterInstance()
    bi.setText(s)
    bi.last()
    val start = bi.previous()
    return if (start == BreakIterator.DONE) "" else s.substring(0, start)
}

@Composable
fun ChatScreen(pal: RdPal, chat: String, onBack: () -> Unit, onProfile: () -> Unit, onLink: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val dens = LocalDensity.current
    val t = RdStore.tick
    val list = RdStore.msgs.filter { it.chat == chat && t >= 0 }.takeLast(400)
    val listState = rememberLazyListState()

    var zoom by remember { mutableIntStateOf(Prefs.getLong("rd_zoom", 100L).toInt().coerceIn(70, 200)) }
    var wall by remember { mutableStateOf(Prefs.getString("rd_wp_$chat", "default")) }
    var chatMuted by remember { mutableStateOf(Prefs.getBool("rd_mute_$chat", false)) }
    var text by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<ReplyRef?>(null) }
    var editing by remember { mutableStateOf<Msg?>(null) }
    val sel = remember { mutableStateListOf<String>() }
    var menu by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var emojiOpen by remember { mutableStateOf(false) }
    var attachOpen by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Msg?>(null) }
    var askDelete by remember { mutableStateOf(false) }
    var wallDlg by remember { mutableStateOf(false) }
    var lastTyping by remember { mutableLongStateOf(0L) }
    var recording by remember { mutableStateOf(false) }
    var recSecs by remember { mutableIntStateOf(0) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var pendingVideo by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(ctx) }
    val selecting = sel.isNotEmpty()
    val typingNow = chat == "p2p" && RdNet.peerTyping
    val imeBottom = WindowInsets.ime.getBottom(dens)

    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.stop(false); if (lastTyping != 0L) RdNet.sendTyping(false) } }
    // keyboard aate hi / naya msg aate hi / typing aate hi: sabse neeche scroll
    LaunchedEffect(list.size, imeBottom, typingNow) { if (list.isNotEmpty()) listState.scrollToItem(list.size) }
    LaunchedEffect(list.size, chat) { RdNet.markRead(chat) }
    LaunchedEffect(text) {
        if (chat == "p2p") {
            if (text.isNotEmpty()) {
                val now = System.currentTimeMillis()
                if (now - lastTyping > 1500L) { lastTyping = now; RdNet.sendTyping(true) }
                delay(2500); RdNet.sendTyping(false); lastTyping = 0L
            } else if (lastTyping != 0L) { RdNet.sendTyping(false); lastTyping = 0L }
        }
    }
    LaunchedEffect(recording) {
        while (recording) {
            delay(1000); recSecs += 1
            if (recSecs >= 300) { recording = false; val f = recorder.stop(true); if (f != null) RdNet.send(chat, "", PickedFile(f, "voice." + recorder.extension, recorder.mime, f.length()), true, null) }
        }
    }

    BackHandler(enabled = selecting) { sel.clear() }
    BackHandler(enabled = !selecting && (emojiOpen || attachOpen)) { emojiOpen = false; attachOpen = false }

    fun sendPicked(p: PickedFile?, voice: Boolean = false) {
        if (p == null) { Toaster.show("File nahi mil payi"); return }
        if (p.size > MAX_UPLOAD && !RdP2P.open) { Toaster.show("File 50 MB se badi hai (P2P connected hone par bhej sakte ho)"); return }
        RdNet.send(chat, "", p, voice, reply); reply = null
    }
    fun sendUris(uris: List<Uri>) { scope.launch { uris.forEach { sendPicked(copyUriToCache(ctx, it)) } } }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> if (uris.isNotEmpty()) sendUris(uris) }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) sendUris(uris) }
    val pickDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) sendUris(uris) }
    val pickWall = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val p = copyUriToCache(ctx, uri)
            if (p != null && scaleToJpeg(p.file, RdStore.wallFile(chat), 1600)) { wall = "image"; Prefs.putString("rd_wp_$chat", "image"); RdStore.touch() } else Toaster.show("Photo set nahi hua")
        }
    }
    val takePic = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null && f.exists()) sendPicked(PickedFile(f, f.name, "image/jpeg", f.length()))
    }
    fun launchCam() {
        val f = newCameraFile(ctx); cameraFile = f
        try { takePic.launch(fileUri(ctx, f)) } catch (_: Throwable) { Toaster.show("Camera open nahi ho paya") }
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g -> if (g) launchCam() else Toaster.show("Camera permission allow karein") }
    fun openCamera() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCam() else camPerm.launch(Manifest.permission.CAMERA)
    }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) { if (recorder.start()) { recording = true; recSecs = 0 } else Toaster.show("Recording shuru nahi ho paayi") } else Toaster.show("Microphone permission allow karein")
    }
    val callPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.RECORD_AUDIO] == true && (!pendingVideo || res[Manifest.permission.CAMERA] == true)) RdCall.start(pendingVideo)
        else Toaster.show("Call ke liye permission allow karein")
    }
    fun startCall(video: Boolean) {
        if (!RdNet.peerOnline) {
            RdNet.sendCallLog(video, "offline", 0L)
            Toaster.show(peerName() + " offline hai - missed call bhej diya")
            return
        }
        pendingVideo = video
        val need = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECORD_AUDIO)
        if (video && ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.CAMERA)
        if (need.isEmpty()) RdCall.start(video) else callPerm.launch(need.toTypedArray())
    }
    fun stopRecordAndSend() {
        if (!recording) return
        val mime = recorder.mime; val ext = recorder.extension
        val f = recorder.stop(true); recording = false
        if (f != null) sendPicked(PickedFile(f, "voice.$ext", mime, f.length()), true)
    }
    fun doSend() {
        val s = text.trim(); if (s.isEmpty()) return
        val e = editing
        if (e != null) { RdNet.edit(e, s); editing = null } else { RdNet.send(chat, s, null, false, reply); reply = null }
        text = ""
    }
    fun download(m: Msg) {
        val f = RdStore.blobFile(m.id)
        if (!f.exists()) { Toaster.show("File available nahi hai"); return }
        Toaster.show(if (saveToDownloads(ctx, f, m.mediaName ?: "file", m.mediaType ?: "")) "Downloads me save ho gaya" else "Save nahi hua")
    }

    val opt = wallOpt(if (wall == "image") "default" else wall)
    val wallFile = RdStore.wallFile(chat)
    val useImage = wall == "image" && wallFile.exists()

    val sysFont = ctx.resources.configuration.fontScale
    CompositionLocalProvider(LocalDensity provides Density(dens.density, sysFont * zoom / 100f)) {
        @Composable
        fun Body() {
            Column(Modifier.fillMaxSize().imePadding()) {
                // ---------------- header
                if (selecting) {
                    val chosen = list.filter { sel.contains(it.id) }
                    Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).tap { sel.clear() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, null, tint = pal.tx, modifier = Modifier.size(26.dp)) }
                        Text(sel.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.tx, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        HdrIcon(Icons.Outlined.ContentCopy, pal) {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("msg", chosen.filter { it.text.isNotEmpty() }.joinToString("\n") { it.text })); sel.clear(); Toaster.show("Copied")
                        }
                        if (chosen.isNotEmpty() && chosen.all { it.from == "me" && !it.deleted }) HdrIcon(Icons.Outlined.DeleteForever, pal) { chosen.forEach { RdNet.unsend(it) }; sel.clear() }
                        HdrIcon(Icons.Outlined.Delete, pal) { RdStore.deleteLocal(sel.toList()); sel.clear() }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().background(pal.hd).padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).tap { onBack() }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = pal.tx, modifier = Modifier.size(26.dp)) }
                        Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).tap { onProfile() }, verticalAlignment = Alignment.CenterVertically) {
                            RdAvatar(chat, 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(chatTitle(chat), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val sub = when {
                                    typingNow -> "typing..."
                                    chat == "p2p" && RdP2P.open && RdNet.peerActive -> "\u25CF connected"
                                    chat == "p2p" && RdNet.peerActive -> "online"
                                    chat == "p2p" && RdNet.peerLastSeen > 0 -> lastSeenText(RdNet.peerLastSeen)
                                    chat == "p2p" -> "offline"
                                    chat == "tg" -> "Telegram bot"
                                    chat == "me" -> "Message yourself"
                                    else -> "Only on this device"
                                }
                                val green = typingNow || sub.contains("connected") || sub == "online"
                                Text(sub, fontSize = 14.sp, color = if (green) pal.ac else pal.sub, maxLines = 1)
                            }
                        }
                        HdrIcon(if (chatMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications, pal) {
                            chatMuted = !chatMuted; Prefs.putBool("rd_mute_$chat", chatMuted)
                            Toaster.show(if (chatMuted) "Is chat ke notifications band" else "Is chat ke notifications on")
                        }
                        if (chat == "p2p") {
                            HdrIcon(Icons.Outlined.Videocam, pal) { startCall(true) }
                            HdrIcon(Icons.Outlined.Call, pal) { startCall(false) }
                        }
                        Box {
                            HdrIcon(Icons.Filled.MoreVert, pal) { menu = true }
                            DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(20.dp), containerColor = pal.hd, shadowElevation = 14.dp) {
                                ChatMenuItem(pal, "Profile", Icons.Outlined.Person) { menu = false; onProfile() }
                                ChatMenuItem(pal, "Select messages", Icons.Outlined.CheckCircle) { menu = false; list.lastOrNull()?.let { sel.add(it.id) } }
                                ChatMenuItem(pal, "Chat background", Icons.Filled.Palette) { menu = false; wallDlg = true }
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text("Zoom", fontSize = 15.sp, color = pal.sub)
                                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Min" to 80, "Normal" to 100, "Medium" to 125, "Extra" to 155).forEach { (lbl, v) ->
                                            Text(
                                                lbl, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (zoom == v) Color.White else pal.tx,
                                                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (zoom == v) pal.ac else pal.sec)
                                                    .tap { zoom = v; Prefs.putLong("rd_zoom", v.toLong()) }.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                                ChatMenuItem(pal, "Delete chat", Icons.Outlined.Delete) { menu = false; askDelete = true }
                                if (chat == "p2p") ChatMenuItem(pal, "Clean server data", Icons.Outlined.Delete) {
                                    menu = false
                                    scope.launch { Toaster.show(if (RdNet.cleanServer()) "Server data clean ho gaya" else "Clean nahi hua (internet check karein)") }
                                }
                            }
                        }
                    }
                }

                // ---------------- messages
                val maxW = (LocalConfiguration.current.screenWidthDp * 0.80f).dp.coerceAtMost(620.dp)
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
                    itemsIndexed(list, key = { _, m -> m.id }) { idx, m ->
                        Column(Modifier.fillMaxWidth()) {
                            val prev = list.getOrNull(idx - 1)
                            if (prev == null || dayWord(prev.ts) != dayWord(m.ts)) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(dayWord(m.ts), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pal.sub, textAlign = TextAlign.Center,
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(pal.hd.copy(alpha = 0.92f)).padding(horizontal = 14.dp, vertical = 6.dp))
                                }
                            }
                            val first = prev == null || prev.from != m.from || prev.system != m.system || dayWord(prev.ts) != dayWord(m.ts)
                            MsgRow(pal, m, first, sel.contains(m.id), maxW, menuFor == m.id,
                                menu = {
                                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        RdCfg.REACTS.forEach { e -> Text(e, fontSize = 28.sp, modifier = Modifier.clip(CircleShape).tap { RdNet.react(m, if (m.reactions["me"] == e) "" else e); menuFor = null }.padding(5.dp)) }
                                    }
                                    ChatMenuItem(pal, "Reply", Icons.AutoMirrored.Filled.Reply) { reply = ReplyRef(m.id, if (m.from == "me") "me" else "peer", previewOf(m), m.tgId); menuFor = null }
                                    if (m.text.isNotEmpty()) ChatMenuItem(pal, "Copy", Icons.Outlined.ContentCopy) {
                                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("msg", m.text)); menuFor = null; Toaster.show("Copied")
                                    }
                                    if (m.hasMedia) ChatMenuItem(pal, "Download", Icons.Outlined.Download) { menuFor = null; download(m) }
                                    if (m.from == "me" && m.text.isNotEmpty() && !m.hasMedia) ChatMenuItem(pal, "Edit", Icons.Filled.Edit) { editing = m; text = m.text; menuFor = null }
                                    if (m.from == "me") ChatMenuItem(pal, "Unsend", Icons.Outlined.DeleteForever) { menuFor = null; RdNet.unsend(m) }
                                    ChatMenuItem(pal, "Delete for me", Icons.Outlined.Delete) { menuFor = null; RdStore.deleteLocal(listOf(m.id)) }
                                    ChatMenuItem(pal, "Select", Icons.Outlined.CheckCircle) { menuFor = null; sel.add(m.id) }
                                },
                                onDismiss = { menuFor = null },
                                onLong = { if (selecting) { if (sel.contains(m.id)) sel.remove(m.id) else sel.add(m.id) } else if (!m.deleted) menuFor = m.id },
                                onTap = {
                                    if (selecting) { if (sel.contains(m.id)) sel.remove(m.id) else sel.add(m.id) }
                                    else if (m.hasMedia && !m.deleted) {
                                        if (mediaKind(m.mediaType) == "image") viewer = m
                                        else if (mediaKind(m.mediaType) != "audio") {
                                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(ctx, RdStore.blobFile(m.id)), m.mediaType ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch (_: Throwable) { Toaster.show("Is file ko kholne wala app nahi mila (double tap = download)") }
                                        }
                                    }
                                },
                                onDouble = {
                                    if (!selecting && !m.system && !m.deleted) {
                                        if (m.hasMedia && mediaKind(m.mediaType) != "image") download(m) else RdNet.react(m, RdCfg.REACTS[0])
                                    }
                                },
                                onLink = onLink,
                                onImgDownload = { download(m) },
                                onImgDelete = { RdStore.deleteLocal(listOf(m.id)) }
                            )
                        }
                    }
                    if (typingNow) item(key = "typing") { TypingBubble(pal) }
                }

                // ---------------- reply / edit bar
                if (reply != null || editing != null) {
                    Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (editing != null) "Editing message" else "Replying", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.ac)
                            Text(editing?.text ?: reply?.text ?: "", fontSize = 16.sp, color = pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(Modifier.size(40.dp).clip(CircleShape).tap { reply = null; editing = null; text = "" }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, null, tint = pal.sub) }
                    }
                }

                // ---------------- composer
                if (recording) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.weight(1f).height(60.dp).shadow(2.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(pal.hd).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFE53935)))
                            Spacer(Modifier.width(12.dp))
                            Text("%d:%02d".format(recSecs / 60, recSecs % 60), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = pal.tx, modifier = Modifier.weight(1f))
                            Text("Cancel", fontSize = 17.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(14.dp)).tap { recorder.stop(false); recording = false }.padding(10.dp))
                        }
                        Box(Modifier.size(60.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(pal.ac).tap { stopRecordAndSend() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.weight(1f).heightIn(min = 60.dp).shadow(2.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(pal.hd).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(46.dp).clip(CircleShape).tap { emojiOpen = !emojiOpen; attachOpen = false; if (emojiOpen) focus.clearFocus() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.EmojiEmotions, null, tint = pal.sub, modifier = Modifier.size(30.dp))
                            }
                            Box(Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 10.dp)) {
                                if (text.isEmpty()) Text("Message", color = pal.sub, fontSize = 18.sp)
                                BasicTextField(
                                    text, { text = it },
                                    textStyle = TextStyle(fontSize = 18.sp, color = pal.tx), cursorBrush = SolidColor(pal.tx), maxLines = 5,
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) { emojiOpen = false; attachOpen = false } }
                                )
                            }
                            Box(Modifier.size(46.dp).clip(CircleShape).tap { attachOpen = !attachOpen; emojiOpen = false; focus.clearFocus() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, null, tint = pal.sub, modifier = Modifier.size(30.dp))
                            }
                        }
                        Box(
                            Modifier.size(60.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(pal.ac).tap {
                                if (text.isNotBlank()) doSend()
                                else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    if (recorder.start()) { recording = true; recSecs = 0 } else Toaster.show("Recording shuru nahi ho paayi")
                                } else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            contentAlignment = Alignment.Center
                        ) { Icon(if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    }
                }

                // ---------------- emoji panel
                if (emojiOpen) {
                    Column(Modifier.fillMaxWidth().height(330.dp).background(pal.hd)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Emoji", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = pal.sub, modifier = Modifier.weight(1f))
                            Box(Modifier.size(44.dp).clip(CircleShape).tap { text = dropLastGrapheme(text) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Backspace, null, tint = pal.tx, modifier = Modifier.size(28.dp))
                            }
                        }
                        LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxSize()) {
                            gridItems(RD_EMOJIS) { e -> Text(e, fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.tap { text += e }.padding(6.dp)) }
                        }
                    }
                } else Spacer(Modifier.navigationBarsPadding())
            }

            // ---------------- attach sheet
            if (attachOpen) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).plainClick { attachOpen = false }, contentAlignment = Alignment.BottomCenter) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(pal.hd).navigationBarsPadding().padding(vertical = 26.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AttachCircle("Photos & Videos", Icons.Outlined.Image, Color(0xFF7F66FF), pal) { attachOpen = false; pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
                        AttachCircle("Camera", Icons.Filled.CameraAlt, Color(0xFFFF2D74), pal) { attachOpen = false; openCamera() }
                        AttachCircle("Audio", Icons.Outlined.Audiotrack, Color(0xFFFF7A01), pal) { attachOpen = false; pickAudio.launch(arrayOf("audio/*")) }
                        AttachCircle("Document", Icons.Outlined.InsertDriveFile, Color(0xFF5157AF), pal) { attachOpen = false; pickDoc.launch(arrayOf("*/*")) }
                    }
                }
            }
        }

        if (useImage) Box(Modifier.fillMaxSize()) {
            FileImage(wallFile, Modifier.fillMaxSize(), ContentScale.Crop, 1600)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (pal.dark) 0.35f else 0.0f)))
            Body()
        } else PatternBg(
            Color(if (pal.dark) opt.darkBase else opt.lightBase), (if (pal.dark) opt.darkStroke else opt.lightStroke).toInt(), true, pal.dark, Modifier.fillMaxSize()
        ) { Body() }

        // ---------------- dialogs
        if (askDelete) {
            Dialog(onDismissRequest = { askDelete = false }) {
                Column(Modifier.clip(RoundedCornerShape(26.dp)).background(pal.hd).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Delete chat?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = pal.tx)
                    Text("Is device se saare messages hat jayenge.", fontSize = 16.sp, color = pal.sub)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Cancel", fontSize = 17.sp, color = pal.sub, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(14.dp)).tap { askDelete = false }.padding(14.dp))
                        Text("Delete", fontSize = 17.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(14.dp)).tap {
                            RdStore.deleteLocal(RdStore.msgs.filter { it.chat == chat }.map { it.id }); askDelete = false
                        }.padding(14.dp))
                    }
                }
            }
        }
        if (wallDlg) {
            Dialog(onDismissRequest = { wallDlg = false }) {
                Column(Modifier.clip(RoundedCornerShape(26.dp)).background(pal.hd).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Chat background", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = pal.tx)
                    Text("Sirf is chat ke liye", fontSize = 14.sp, color = pal.sub)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WALL_OPTS.forEach { o ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).background(Color(if (pal.dark) o.darkBase else o.lightBase))
                                        .border(if (o.key == wall) 3.dp else 1.dp, if (o.key == wall) pal.ac else pal.line, RoundedCornerShape(14.dp))
                                        .tap { wall = o.key; Prefs.putString("rd_wp_$chat", o.key) }
                                )
                                Text(o.label, fontSize = 12.sp, color = pal.sub, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.sec).border(if (wall == "image") 2.dp else 0.dp, if (wall == "image") pal.ac else Color.Transparent, RoundedCornerShape(16.dp))
                            .tap { pickWall.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Image, null, tint = pal.ac, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Phone gallery se photo", fontSize = 17.sp, color = pal.tx, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Done", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.ac).tap { wallDlg = false }.padding(14.dp))
                }
            }
        }
        viewer?.let { m ->
            PhotoViewer(chatTitle(chat), RdStore.blobFile(m.id), chat, { viewer = null }, { download(m) }, { RdStore.deleteLocal(listOf(m.id)); viewer = null })
        }
    }
}

@Composable
private fun ZoomBtn(label: String, pal: RdPal, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(pal.sec).tap { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.tx)
    }
}

@Composable
private fun TypingBubble(pal: RdPal) {
    val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    val a by inf.animateFloat(0.25f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(600), androidx.compose.animation.core.RepeatMode.Reverse), label = "dots")
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Start) {
        Row(Modifier.shadow(1.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(pal.inb).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { Box(Modifier.padding(horizontal = 3.dp).size(9.dp).alpha(a).clip(CircleShape).background(pal.sub)) }
            Spacer(Modifier.width(8.dp))
            Text("typing...", fontSize = 15.sp, color = pal.sub, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun HdrIcon(icon: ImageVector, pal: RdPal, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).tap { onClick() }, contentAlignment = Alignment.Center) { Icon(icon, null, tint = pal.tx, modifier = Modifier.size(26.dp)) }
}

@Composable
private fun ChatMenuItem(pal: RdPal, label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 18.sp, color = pal.tx, modifier = Modifier.padding(end = 20.dp)) },
        leadingIcon = { Icon(icon, null, tint = pal.tx, modifier = Modifier.size(26.dp)) },
        onClick = onClick
    )
}

@Composable
private fun AttachCircle(label: String, icon: ImageVector, color: Color, pal: RdPal, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).tap { onClick() }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(70.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
        Text(label, fontSize = 14.sp, color = pal.tx, modifier = Modifier.padding(top = 8.dp), maxLines = 1)
    }
}

@Composable
private fun MiniBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).tap { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun AudioRow(pal: RdPal, m: Msg) {
    val f = RdStore.blobFile(m.id)
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableIntStateOf(0) }
    var dur by remember { mutableIntStateOf(0) }
    val holder = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(m.id) { onDispose { try { holder[0]?.release() } catch (_: Throwable) {}; holder[0] = null } }
    LaunchedEffect(playing) { while (playing) { pos = try { holder[0]?.currentPosition ?: 0 } catch (_: Throwable) { 0 }; delay(250) } }
    Row(Modifier.widthIn(min = 220.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(pal.ac).tap {
            try {
                var p = holder[0]
                if (p == null) {
                    p = MediaPlayer(); p.setDataSource(f.absolutePath); p.prepare(); dur = p.duration
                    p.setOnCompletionListener { playing = false; pos = 0 }
                    holder[0] = p
                }
                if (playing) { p.pause(); playing = false } else { p.start(); playing = true }
            } catch (_: Throwable) { Toaster.show("Audio play nahi hua") }
        }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(pal.sub.copy(alpha = 0.3f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(if (dur > 0) pos / dur.toFloat() else 0f).background(pal.ac))
            }
            Text(if (m.voice) "Voice message" else (m.mediaName ?: "Audio"), fontSize = 13.sp, color = pal.sub, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Call ka record: connected = greenish, baaki red/pink (screenshot jaisa) */
@Composable
private fun CallLog(pal: RdPal, m: Msg) {
    val parts = m.text.split(":")
    val kind = parts.getOrElse(1) { "voice" }
    val res = parts.getOrElse(2) { "failed" }
    val secs = parts.getOrElse(3) { "0" }.toLongOrNull() ?: 0L
    val ok = res == "connected"
    val bg = if (ok) (if (pal.dark) Color(0xFF123D2B) else Color(0xFFDDF5DE)) else (if (pal.dark) Color(0xFF4A2226) else Color(0xFFFDE1E3))
    val tint = if (ok) Color(0xFF1B9E4B) else Color(0xFFE53935)
    val mineSide = m.from == "me"
    val sub = when (res) {
        "connected" -> "Connected \u2022 %d:%02d".format(secs / 60, secs % 60)
        "offline" -> "Not connected \u2022 offline"
        "noanswer" -> if (mineSide) "Not connected \u2022 no answer" else "Missed call"
        "declined" -> "Not connected \u2022 declined"
        "busy" -> "Not connected \u2022 busy"
        "cancel" -> if (mineSide) "Not connected \u2022 cancelled" else "Missed call"
        else -> "Not connected \u2022 failed"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (mineSide) Arrangement.End else Arrangement.Start) {
        Row(Modifier.widthIn(min = 250.dp, max = 330.dp).shadow(1.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(bg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = if (pal.dark) 0.18f else 0.85f)), contentAlignment = Alignment.Center) {
                Icon(if (kind == "video") Icons.Outlined.Videocam else Icons.Outlined.Call, null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (kind == "video") "Video call" else "Voice call", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = pal.tx)
                Text(sub, fontSize = 15.sp, color = pal.sub)
            }
            Text(fmtHM(m.ts), fontSize = 13.sp, color = pal.sub, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun MsgRow(
    pal: RdPal, m: Msg, first: Boolean, selected: Boolean, maxW: Dp, menuOpen: Boolean, menu: @Composable () -> Unit, onDismiss: () -> Unit,
    onLong: () -> Unit, onTap: () -> Unit, onDouble: () -> Unit, onLink: (String) -> Unit, onImgDownload: () -> Unit, onImgDelete: () -> Unit
) {
    if (m.system) {
        if (m.text.startsWith("call:")) { CallLog(pal, m); return }
        if (m.text.startsWith("nick:")) {
            val n = m.text.removePrefix("nick:")
            val line = if (m.from == "me") (if (n.isEmpty()) "You removed " + peerName() + "'s nickname" else "You set " + peerName() + "'s nickname to \"" + n + "\"")
                       else (if (n.isEmpty()) peerName() + " removed your nickname" else peerName() + " set your nickname to \"" + n + "\"")
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(line, fontSize = 14.sp, color = pal.sub, textAlign = TextAlign.Center, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(pal.hd.copy(alpha = 0.95f)).padding(horizontal = 16.dp, vertical = 8.dp))
            }
            return
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(m.text, fontSize = 15.sp, color = pal.tx, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(pal.hd.copy(alpha = 0.95f)).padding(horizontal = 16.dp, vertical = 8.dp))
        }
        return
    }
    val mine = m.from == "me"
    val shape: Shape = if (first) BubbleShape(mine) else RoundedCornerShape(14.dp)
    val tailPad = if (first) 9.dp else 0.dp
    val urls = if (m.deleted) emptyList() else Regex("https?://[^\\s]+").findAll(m.text).map { it.value }.take(2).toList()
    Row(
        Modifier.fillMaxWidth().background(if (selected) pal.ac.copy(alpha = 0.22f) else Color.Transparent).padding(top = if (first) 8.dp else 2.dp, bottom = 1.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Column(
                Modifier.widthIn(max = maxW).shadow(1.dp, shape).clip(shape).background(if (mine) pal.out else pal.inb)
                    .pointerInput(m.id) { detectTapGestures(onLongPress = { onLong() }, onDoubleTap = { onDouble() }, onTap = { onTap() }) }
                    .padding(start = 12.dp + (if (!mine) tailPad else 0.dp), end = 12.dp + (if (mine) tailPad else 0.dp), top = 8.dp, bottom = 6.dp)
            ) {
                if (m.replyId != null && !m.deleted) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).height(IntrinsicSize.Min).clip(RoundedCornerShape(10.dp)).background(pal.quote)) {
                        Box(Modifier.width(5.dp).fillMaxHeight().background(pal.ac))
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(if (m.replyWho == "me") "You" else chatTitle(m.chat), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pal.ac, maxLines = 1)
                            Text(m.replyText ?: "", fontSize = 16.sp, color = pal.tx.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (m.deleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDEAB ", fontSize = 16.sp)
                        Text("This message was deleted", fontSize = 16.sp, fontStyle = FontStyle.Italic, color = pal.sub)
                    }
                } else {
                    if (m.hasMedia) {
                        val f = RdStore.blobFile(m.id)
                        when {
                            m.mediaFailed && !f.exists() -> Text("\u26A0 Media load nahi hua", fontSize = 15.sp, color = pal.sub)
                            mediaKind(m.mediaType) == "image" -> Box {
                                FileImage(f, Modifier.widthIn(max = 320.dp).heightIn(max = 340.dp).clip(RoundedCornerShape(10.dp)), ContentScale.Fit, 900)
                                Row(Modifier.align(Alignment.TopEnd).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MiniBtn(Icons.Outlined.Download) { onImgDownload() }
                                    MiniBtn(Icons.Outlined.Delete) { onImgDelete() }
                                }
                            }
                            mediaKind(m.mediaType) == "audio" -> AudioRow(pal, m)
                            else -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                Icon(if (mediaKind(m.mediaType) == "video") Icons.Outlined.Videocam else Icons.Outlined.InsertDriveFile, null, tint = pal.ac, modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(m.mediaName ?: "file", fontSize = 16.sp, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text((if (m.mediaSize > 0) fmtSize(m.mediaSize) + " \u2022 " else "") + "double tap = download", fontSize = 12.sp, color = pal.sub)
                                }
                            }
                        }
                        if (m.prog >= 0) Text("Sending " + m.prog + "%", fontSize = 13.sp, color = pal.sub)
                    }
                    if (m.text.isNotEmpty()) Text(m.text, fontSize = 17.sp, color = pal.tx, lineHeight = 23.sp)
                    urls.forEach { u ->
                        Row(
                            Modifier.padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(pal.quote)
                                .pointerInput(u) { detectTapGestures(onTap = { onLink(u) }, onLongPress = { onLong() }) }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Link, null, tint = pal.ac, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(u.removePrefix("https://").removePrefix("http://").take(40), fontSize = 15.sp, color = pal.ac, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(Modifier.align(Alignment.End).padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (m.reactions.isNotEmpty() && !m.deleted) Text(m.reactions.values.joinToString(""), fontSize = 15.sp, modifier = Modifier.padding(end = 8.dp))
                    if (m.edited && !m.deleted) Text("edited ", fontSize = 12.sp, color = pal.sub)
                    Text(fmtHM(m.ts), fontSize = 12.sp, color = pal.sub)
                    if (mine && !m.deleted) {
                        Spacer(Modifier.width(4.dp))
                        when (m.status) {
                            "pending" -> Icon(Icons.Filled.Schedule, null, tint = pal.sub, modifier = Modifier.size(15.dp))
                            "sent" -> Icon(Icons.Filled.Done, null, tint = pal.sub, modifier = Modifier.size(17.dp))
                            "delivered" -> Icon(Icons.Filled.DoneAll, null, tint = pal.sub, modifier = Modifier.size(17.dp))
                            "seen" -> Icon(Icons.Filled.DoneAll, null, tint = pal.tick, modifier = Modifier.size(17.dp))
                            else -> {}
                        }
                    }
                }
            }
            DropdownMenu(menuOpen, onDismiss, shape = RoundedCornerShape(22.dp), containerColor = pal.hd, shadowElevation = 16.dp) { menu() }
        }
    }
}
