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
    val t = RdStore.tick
    val list = RdStore.msgs.filter { it.chat == chat && t >= 0 }.takeLast(400)
    val listState = rememberLazyListState()

    var wall by remember { mutableStateOf(Prefs.getString("rd_wp", "default")) }
    var text by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<ReplyRef?>(null) }
    var editing by remember { mutableStateOf<Msg?>(null) }
    val sel = remember { mutableStateListOf<String>() }
    var menu by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    var attachOpen by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Msg?>(null) }
    var askDelete by remember { mutableStateOf(false) }
    var wallDlg by remember { mutableStateOf(false) }
    var typingOn by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recSecs by remember { mutableIntStateOf(0) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var pendingVideo by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(ctx) }
    val selecting = sel.isNotEmpty()

    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.stop(false) } }
    LaunchedEffect(list.size) { if (list.isNotEmpty()) listState.scrollToItem(list.size - 1) }
    LaunchedEffect(list.size, chat) { RdNet.markRead(chat) }
    LaunchedEffect(recording) { while (recording) { delay(1000); recSecs += 1; if (recSecs >= 300) { recording = false; val f = recorder.stop(true); if (f != null) RdNet.send(chat, "", PickedFile(f, "voice." + recorder.extension, recorder.mime, f.length()), true, null) } } }

    BackHandler(enabled = selecting) { sel.clear() }
    BackHandler(enabled = !selecting && (emojiOpen || attachOpen)) { emojiOpen = false; attachOpen = false }

    fun sendPicked(p: PickedFile?, voice: Boolean = false) {
        if (p == null) { Toaster.show("File nahi mil payi"); return }
        if (p.size > MAX_UPLOAD) { Toaster.show("File 50 MB se badi hai"); return }
        RdNet.send(chat, "", p, voice, reply); reply = null
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { sendPicked(copyUriToCache(ctx, uri)) } }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { sendPicked(copyUriToCache(ctx, uri)) } }
    val pickDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { sendPicked(copyUriToCache(ctx, uri)) } }
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
        if (!RdNet.peerOnline) { Toaster.show(peerName() + " abhi online nahi hai"); return }
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
        if (typingOn) { typingOn = false; RdNet.sendTyping(false) }
    }

    val opt = wallOpt(wall)
    PatternBg(
        Color(if (pal.dark) opt.darkBase else opt.lightBase), (if (pal.dark) opt.darkStroke else opt.lightStroke).toInt(), true, pal.dark, Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().imePadding()) {
            // ---------------- header
            if (selecting) {
                val chosen = list.filter { sel.contains(it.id) }
                Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).tap { sel.clear() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, null, tint = pal.tx, modifier = Modifier.size(26.dp)) }
                    Text(sel.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.tx, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    if (chosen.size == 1) HdrIcon(Icons.AutoMirrored.Filled.Reply, pal) {
                        val m = chosen[0]; reply = ReplyRef(m.id, if (m.from == "me") "me" else "peer", previewOf(m), m.tgId); sel.clear()
                    }
                    HdrIcon(Icons.Outlined.ContentCopy, pal) {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("msg", chosen.filter { it.text.isNotEmpty() }.joinToString("\n") { it.text })); sel.clear(); Toaster.show("Copied")
                    }
                    if (chosen.size == 1 && chosen[0].from == "me" && chosen[0].text.isNotEmpty() && !chosen[0].hasMedia && !chosen[0].deleted) HdrIcon(Icons.Filled.Edit, pal) {
                        editing = chosen[0]; text = chosen[0].text; sel.clear()
                    }
                    if (chosen.isNotEmpty() && chosen.all { it.from == "me" && !it.deleted }) HdrIcon(Icons.Outlined.DeleteForever, pal) { chosen.forEach { RdNet.unsend(it) }; sel.clear() }
                    HdrIcon(Icons.Outlined.Delete, pal) { RdStore.deleteLocal(sel.toList()); sel.clear() }
                }
                if (chosen.size == 1 && !chosen[0].deleted) {
                    Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RdCfg.REACTS.forEach { e -> Text(e, fontSize = 30.sp, modifier = Modifier.tap { RdNet.react(chosen[0], if (chosen[0].reactions["me"] == e) "" else e); sel.clear() }.padding(4.dp)) }
                    }
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
                                chat == "p2p" && RdNet.peerTyping -> "typing..."
                                chat == "p2p" && RdNet.peerOnline -> "online"
                                chat == "p2p" && RdNet.peerLastSeen > 0 -> lastSeenText(RdNet.peerLastSeen)
                                chat == "tg" -> "Telegram bot"
                                chat == "me" -> "Message yourself"
                                chat == "saved" -> "Only on this device"
                                else -> ""
                            }
                            if (sub.isNotEmpty()) Text(sub, fontSize = 14.sp, color = if (sub == "typing..." || sub == "online") pal.ac else pal.sub, maxLines = 1)
                        }
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
                            Text(
                                dayWord(m.ts), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pal.sub, textAlign = TextAlign.Center,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(pal.hd.copy(alpha = 0.92f)).padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                    val first = prev == null || prev.from != m.from || prev.system != m.system || dayWord(prev.ts) != dayWord(m.ts)
                    MsgRow(pal, m, first, sel.contains(m.id), maxW,
                        onLong = { if (!sel.contains(m.id)) sel.add(m.id) },
                        onTap = {
                            if (selecting) { if (sel.contains(m.id)) sel.remove(m.id) else sel.add(m.id) }
                            else if (m.hasMedia && !m.deleted) {
                                if (mediaKind(m.mediaType) == "image") viewer = m
                                else if (mediaKind(m.mediaType) != "audio") {
                                    try { ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(ctx, RdStore.blobFile(m.id)), m.mediaType ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch (_: Throwable) { Toaster.show("Is file ko kholne wala app nahi mila") }
                                }
                            }
                        },
                        onDouble = { if (!selecting && !m.system && !m.deleted) RdNet.react(m, RdCfg.REACTS[0]) },
                        onLink = onLink
                    )
                  }
                }
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
                                text, { text = it; val on = it.isNotEmpty(); if (on != typingOn) { typingOn = on; RdNet.sendTyping(on) } },
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
                    AttachCircle("Photos & Videos", Icons.Outlined.Image, Color(0xFF7F66FF), pal) { attachOpen = false; pickMedia.launch(arrayOf("image/*", "video/*")) }
                    AttachCircle("Camera", Icons.Filled.CameraAlt, Color(0xFFFF2D74), pal) { attachOpen = false; openCamera() }
                    AttachCircle("Audio", Icons.Outlined.Audiotrack, Color(0xFFFF7A01), pal) { attachOpen = false; pickAudio.launch(arrayOf("audio/*")) }
                    AttachCircle("Document", Icons.Outlined.InsertDriveFile, Color(0xFF5157AF), pal) { attachOpen = false; pickDoc.launch(arrayOf("*/*")) }
                }
            }
        }
    }

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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WALL_OPTS.forEach { o ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).background(Color(if (pal.dark) o.darkBase else o.lightBase))
                                    .border(if (o.key == wall) 3.dp else 1.dp, if (o.key == wall) pal.ac else pal.line, RoundedCornerShape(14.dp))
                                    .tap { wall = o.key; Prefs.putString("rd_wp", o.key) }
                            )
                            Text(o.label, fontSize = 12.sp, color = pal.sub, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                        }
                    }
                }
                Text("Done", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.ac).tap { wallDlg = false }.padding(14.dp))
            }
        }
    }
    viewer?.let { m -> PhotoViewer(chatTitle(chat), RdStore.blobFile(m.id), chat) { viewer = null } }
}

private fun Modifier.wrapCenter(): Modifier = this

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

@Composable
private fun MsgRow(pal: RdPal, m: Msg, first: Boolean, selected: Boolean, maxW: Dp, onLong: () -> Unit, onTap: () -> Unit, onDouble: () -> Unit, onLink: (String) -> Unit) {
    if (m.system) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(pal.hd.copy(alpha = 0.95f)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (m.text.contains("ideo")) Icons.Outlined.Videocam else Icons.Outlined.Call, null, tint = if (m.text.contains("issed") || m.text.contains("eclined") || m.text.contains("No answer")) Color(0xFFE53935) else pal.ac, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(m.text, fontSize = 15.sp, color = pal.tx)
                Spacer(Modifier.width(10.dp))
                Text(fmtHM(m.ts), fontSize = 12.sp, color = pal.sub)
            }
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
                        mediaKind(m.mediaType) == "image" -> FileImage(f, Modifier.widthIn(max = 320.dp).heightIn(max = 340.dp).clip(RoundedCornerShape(10.dp)), ContentScale.Fit, 900)
                        mediaKind(m.mediaType) == "audio" -> AudioRow(pal, m)
                        else -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                            Icon(if (mediaKind(m.mediaType) == "video") Icons.Outlined.Videocam else Icons.Outlined.InsertDriveFile, null, tint = pal.ac, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(m.mediaName ?: "file", fontSize = 16.sp, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (m.mediaSize > 0) Text(fmtSize(m.mediaSize), fontSize = 13.sp, color = pal.sub)
                            }
                        }
                    }
                    if (m.prog >= 0) Text("Uploading...", fontSize = 13.sp, color = pal.sub)
                }
                if (m.text.isNotEmpty()) Text(m.text, fontSize = 17.sp, color = pal.tx, lineHeight = 23.sp)
                urls.forEach { u ->
                    Row(
                        Modifier.padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(pal.quote).tap { onLink(u) }.padding(horizontal = 10.dp, vertical = 8.dp),
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
    }
}
