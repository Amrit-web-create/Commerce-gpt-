package com.amrit.commercehub11

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RdPal(val dark: Boolean) {
    val bg = if (dark) Color(0xFF0B141A) else Color.White
    val hd = if (dark) Color(0xFF111B21) else Color.White
    val tx = if (dark) Color(0xFFE9EDEF) else Color(0xFF111B21)
    val sub = if (dark) Color(0xFF8696A0) else Color(0xFF667781)
    val line = if (dark) Color(0xFF222D34) else Color(0xFFE9ECEF)
    val sec = if (dark) Color(0xFF1A262D) else Color(0xFFF1F2F6)
    val out = if (dark) Color(0xFF005C4B) else Color(0xFFD8FDD2)
    val inb = if (dark) Color(0xFF1F2C34) else Color.White
    val ac = Color(0xFF00A884)
    val tick = Color(0xFF53BDEB)
    val quote = Color.Black.copy(alpha = if (dark) 0.25f else 0.07f)
}

// ------------------------------------------------------------------ helpers
fun fmtHM(ts: Long): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(ts))

fun dayWord(ts: Long): String {
    val f = SimpleDateFormat("yyyyMMdd", Locale.US)
    val d = f.format(Date(ts)); val now = System.currentTimeMillis()
    return when (d) {
        f.format(Date(now)) -> "Today"
        f.format(Date(now - 86400000L)) -> "Yesterday"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))
    }
}

fun lastSeenText(ts: Long): String = "last seen " + dayWord(ts).let { if (it == "Today") "today" else if (it == "Yesterday") "yesterday" else "on $it" } + " at " + fmtHM(ts)

fun myName(): String = RdCfg.NAMES[RdNet.ME] ?: "Me"
fun peerName(): String = RdCfg.NAMES[RdNet.PEER] ?: "Chat"
fun chatTitle(c: String): String = when (c) {
    "p2p" -> RdStore.nick("peer").ifEmpty { peerName() }
    "tg" -> "Telegram"
    "me" -> myName() + " (You)"
    else -> "Saved"
}

fun previewOf(m: Msg?): String {
    if (m == null) return "No messages yet"
    if (m.deleted) return "\uD83D\uDEAB Deleted message"
    if (m.system && m.text.startsWith("nick:")) return "Nickname changed"
    if (m.system && m.text.startsWith("call:")) return if (m.text.contains("video")) "\uD83D\uDCF9 Video call" else "\uD83D\uDCDE Voice call"
    if (m.hasMedia) return when (mediaKind(m.mediaType)) {
        "image" -> "\uD83D\uDCF7 Photo"; "video" -> "\uD83C\uDFA5 Video"; "audio" -> if (m.voice) "\uD83C\uDFA4 Voice message" else "\uD83C\uDFB5 Audio"
        else -> "\uD83D\uDCC4 " + (m.mediaName ?: "File")
    }
    return m.text
}

@Composable
fun RdAvatar(kind: String, size: Dp, modifier: Modifier = Modifier) {
    val t = RdStore.tick
    val file: File? = when (kind) { "p2p" -> RdStore.photoFile("peer"); "me" -> RdStore.photoFile("me"); else -> null }
    val bg = when (kind) { "p2p" -> Color(0xFFD6336C); "tg" -> Color(0xFF229ED9); "me" -> Color(0xFFBFE3FF); else -> Color(0xFF111111) }
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        if (file != null && file.exists() && t >= 0) {
            FileImage(file, Modifier.fillMaxSize(), ContentScale.Crop, 512)
        } else when (kind) {
            "p2p" -> Text(peerName().take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
            "tg" -> Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
            "me" -> Text("\u2601\uFE0F", fontSize = (size.value * 0.46f).sp)
            else -> Icon(Icons.Filled.Bookmark, null, tint = Color.White, modifier = Modifier.size(size * 0.46f))
        }
    }
}

// ------------------------------------------------------------------ root
@Composable
fun RdRoot(dark: Boolean, onExit: () -> Unit) {
    val pal = remember(dark) { RdPal(dark) }
    val ctx = LocalContext.current
    val d = LocalDensity.current
    var me by remember { mutableStateOf(Prefs.getString("rd_me", "")) }
    var chat by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("chats") }

    DisposableEffect(Unit) {
        val lo = ctx.findAct()?.lifecycle
        val ob = LifecycleEventObserver { _, _ ->
            val fg = lo?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
            RdNet.changeForeground(fg)
            if (fg) {
                if (!RdNet.running && me.isNotEmpty()) RdNet.start(me) else RdNet.nudge()
                RdNet.curChat?.let { RdNet.markRead(it) }
            } else if (!Prefs.notifEnabled && RdCall.state == "idle") RdNet.stop()
        }
        lo?.addObserver(ob)
        RdNet.uiOpen = true
        onDispose { lo?.removeObserver(ob); RdNet.uiOpen = false; RdNet.curChat = null; if (!Prefs.notifEnabled) RdNet.stop() }
    }
    LaunchedEffect(me) { if (me.isNotEmpty()) { RdStore.load(); RdNet.start(me); if (Prefs.notifEnabled) Notifier.startService(ctx) } }
    LaunchedEffect(chat, tab) {
        RdNet.curChat = chat
        RdNet.uiMode = if (chat != null) "chat" else if (tab == "browser") "browser" else "inbox"
        chat?.let { RdNet.markRead(it) }
    }

    LaunchedEffect(Nav.openChat, me) {
        val c = Nav.openChat
        if (c != null && me.isNotEmpty()) { chat = c; profile = null; tab = "chats"; Nav.openChat = null }
    }
    BackHandler(enabled = profile != null) { profile = null }
    BackHandler(enabled = profile == null && chat != null) { chat = null }

    val openLink: (String) -> Unit = { url -> BrowserState.open(ctx, url); chat = null; profile = null; tab = "browser" }

    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * 1.1f)) {
        Box(Modifier.fillMaxSize().background(pal.bg)) {
            when {
                me.isEmpty() -> WhoScreen(pal, onExit) { me = it; Prefs.putString("rd_me", it) }
                profile != null -> ProfileScreen(pal, profile!!, { profile = null }, openLink)
                chat != null -> ChatScreen(pal, chat!!, { chat = null }, { profile = chat }, openLink)
                else -> {
                    if (tab == "browser") BrowserScreen(pal, { tab = "chats" }, { tab = "chats" })
                    else InboxScreen(pal, onExit) { chat = it }
                    val hidePill = tab == "browser" && (BrowserState.fullscreen || !BrowserState.barVisible || BrowserState.customView != null)
                    AnimatedVisibility(
                        !hidePill, modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically { it * 2 } + fadeIn(), exit = slideOutVertically { it * 2 } + fadeOut()
                    ) { PillNav(tab, { tab = it }, Modifier.navigationBarsPadding().padding(bottom = 14.dp)) }
                }
            }
            BannerHost(pal) { c -> chat = c; profile = null; tab = "chats" }
        }
    }
}

@Composable
private fun WhoScreen(pal: RdPal, onExit: () -> Unit, onPick: (String) -> Unit) {
    BackHandler { onExit() }
    val cols = mapOf("radhe" to Color(0xFFD6336C), "amrit" to Color(0xFF5157AF))
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Who's this?", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = pal.tx)
        Text("Apna profile chuno", fontSize = 16.sp, color = pal.sub, modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))
        RdCfg.NAMES.forEach { (k, v) ->
            val c = cols[k] ?: pal.ac
            Row(
                Modifier.widthIn(max = 460.dp).fillMaxWidth().padding(vertical = 8.dp).shadow(6.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp)).background(Brush.horizontalGradient(listOf(c, c.copy(alpha = 0.78f))))
                    .tap { onPick(k) }.padding(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Text(v.take(1), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(v, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Tap to continue", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ inbox
@Composable
private fun InboxScreen(pal: RdPal, onExit: () -> Unit, onOpen: (String) -> Unit) {
    val t = RdStore.tick
    var q by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(RdNet.muted) }
    BackHandler { onExit() }
    PatternBg(pal.bg, if (pal.dark) 0x0FFFFFFF else 0x14000000, false, pal.dark, Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().background(pal.bg)) {
                Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).tap { onExit() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = pal.tx, modifier = Modifier.size(26.dp))
                    }
                    Text("Inbox", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = pal.tx, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    Box {
                        Box(Modifier.size(48.dp).clip(CircleShape).tap { menu = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MoreVert, null, tint = pal.tx, modifier = Modifier.size(26.dp))
                        }
                        DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(18.dp), containerColor = pal.hd, shadowElevation = 12.dp) {
                            DropdownMenuItem(
                                text = { Text(if (muted) "Unmute notifications" else "Mute notifications", fontSize = 17.sp, color = pal.tx) },
                                leadingIcon = { Icon(if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null, tint = pal.sub) },
                                onClick = {
                                    menu = false; muted = !muted; Prefs.putBool("rd_muted", muted)
                                    Toaster.show(if (muted) "Notifications muted" else "Notifications on")
                                }
                            )
                        }
                    }
                }
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(pal.sec).padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, null, tint = pal.sub, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Text("Search chats", fontSize = 17.sp, color = pal.sub)
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(fontSize = 17.sp, color = pal.tx), cursorBrush = SolidColor(pal.tx), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            val chats = listOf("p2p", "tg", "me", "saved").filter { t >= 0 && chatTitle(it).lowercase().contains(q.lowercase().trim()) }
            Column(Modifier.fillMaxWidth().background(pal.bg)) {
                chats.forEach { c ->
                    val last = RdStore.msgs.lastOrNull { it.chat == c }
                    val unread = RdStore.msgs.count { it.chat == c && it.from == "peer" && !it.read && !it.system }
                    Row(Modifier.fillMaxWidth().tap { onOpen(c) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RdAvatar(c, 60.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chatTitle(c), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val typing = c == "p2p" && RdNet.peerTyping
                            Text(
                                if (typing) "typing..." else if (last == null) when (c) { "me" -> "Message yourself"; "saved" -> "Save anything here"; else -> "No messages yet" } else previewOf(last),
                                fontSize = 15.sp, color = if (typing) pal.ac else pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (last != null) Text(fmtHM(last.ts), fontSize = 13.sp, color = if (unread > 0) pal.ac else pal.sub)
                            if (unread > 0) Text(
                                unread.toString(), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp).clip(CircleShape).background(pal.ac).padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(pal.line))
                }
            }
        }
    }
}

/** Black expanding pill: Inbox | Browser */
@Composable
private fun PillNav(tab: String, onTab: (String) -> Unit, modifier: Modifier) {
    Row(
        modifier.shadow(14.dp, CircleShape).clip(CircleShape).background(Color(0xFF000000)).border(1.dp, Color(0xFF2A2A2A), CircleShape)
            .padding(6.dp).animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        PillBtn(Icons.Filled.Chat, "Inbox", tab == "chats") { onTab("chats") }
        PillBtn(Icons.Filled.Language, "Browser", tab == "browser") { onTab("browser") }
    }
}

@Composable
private fun PillBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sel: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(if (sel) Color.White else Color.Transparent).tap { onClick() }.padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (sel) Color.Black else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        AnimatedVisibility(sel, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
            Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

// ------------------------------------------------------------------ banner (dusri chat me ho to)
@Composable
private fun BannerHost(pal: RdPal, onOpen: (String) -> Unit) {
    val b = RdNet.banner
    LaunchedEffect(b?.stamp) { if (b != null) { delay(3600); if (RdNet.banner?.stamp == b.stamp) RdNet.banner = null } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(b != null, enter = slideInVertically { -it * 2 } + fadeIn(), exit = slideOutVertically { -it * 2 } + fadeOut()) {
            val d = b
            if (d != null) Row(
                Modifier.padding(10.dp).fillMaxWidth().widthIn(max = 520.dp).shadow(14.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                    .background(pal.hd).border(1.dp, pal.line, RoundedCornerShape(22.dp)).tap { RdNet.banner = null; onOpen(d.chat) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RdAvatar(d.chat, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(d.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = pal.tx)
                    Text(d.text, fontSize = 15.sp, color = pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ profile
@Composable
private fun ProfileScreen(pal: RdPal, chat: String, onBack: () -> Unit, onLink: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val t = RdStore.tick
    var tabSel by remember { mutableStateOf(0) }
    var viewer by remember { mutableStateOf<File?>(null) }
    var editNick by remember { mutableStateOf(false) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val photo = RdStore.photoFile(if (chat == "me") "me" else "peer")
    val canEdit = chat == "me"

    fun applyPhoto(src: File) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { makeSquareJpeg(src, RdStore.photoFile("me"), 512) }
            if (ok) {
                RdStore.touch()
                if (RdNet.running) { val b = withContext(Dispatchers.IO) { photoBase64(RdStore.photoFile("me")) }; RdNet.sendPhoto(b) }
                Toaster.show("Profile photo updated")
            } else Toaster.show("Photo set nahi hua")
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { val p = copyUriToCache(ctx, uri); if (p != null && p.size <= MAX_UPLOAD) applyPhoto(p.file) }
    }
    val takePic = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null && f.exists()) applyPhoto(f)
    }
    fun launchCam() {
        val f = newCameraFile(ctx); cameraFile = f
        try { takePic.launch(fileUri(ctx, f)) } catch (_: Throwable) { Toaster.show("Camera open nahi ho paya") }
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g -> if (g) launchCam() else Toaster.show("Camera permission allow karein") }
    fun openCamera() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCam() else camPerm.launch(Manifest.permission.CAMERA)
    }

    val status = when {
        chat == "p2p" && RdP2P.open -> "connected"
        chat == "p2p" && RdNet.peerActive -> "online"
        chat == "p2p" && RdNet.peerLastSeen > 0 -> lastSeenText(RdNet.peerLastSeen)
        chat == "tg" -> "Telegram bot"
        chat == "me" -> "Message yourself"
        chat == "saved" -> "Only on this device"
        else -> ""
    }
    val media = RdStore.msgs.filter { it.chat == chat && it.hasMedia && !it.deleted && mediaKind(it.mediaType) == "image" && t >= 0 }
    val urlRe = Regex("https?://[^\\s]+")
    val links = RdStore.msgs.filter { it.chat == chat && !it.deleted && ((it.hasMedia && mediaKind(it.mediaType) != "image") || urlRe.containsMatchIn(it.text)) }

    Column(Modifier.fillMaxSize().background(pal.sec)) {
        Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).tap { onBack() }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = pal.tx, modifier = Modifier.size(26.dp)) }
            Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pal.tx, modifier = Modifier.padding(start = 6.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(pal.hd).padding(vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    RdAvatar(chat, 130.dp, Modifier.shadow(10.dp, CircleShape).tap { viewer = if (photo.exists()) photo else File("") })
                    if (canEdit) Box(
                        Modifier.size(42.dp).clip(CircleShape).background(pal.ac).border(3.dp, pal.hd, CircleShape).tap { openCamera() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                }
                if (canEdit) Text(
                    "Change photo", color = pal.ac, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp).clip(RoundedCornerShape(20.dp)).tap { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(horizontal = 18.dp, vertical = 8.dp)
                )
                if (status.isNotEmpty()) Text(status, color = pal.ac, fontSize = 17.sp, modifier = Modifier.padding(top = 14.dp))
                Text(if (chat == "p2p") peerName() else chatTitle(chat), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = pal.tx, modifier = Modifier.padding(top = 6.dp))
            }
            if (chat == "p2p") {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().background(pal.hd).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(peerName() + "'s nickname (set by you)", fontSize = 15.sp, color = pal.sub)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(RdStore.nick("peer").ifEmpty { "Not set" }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = if (RdStore.nick("peer").isEmpty()) pal.sub else pal.tx, modifier = Modifier.weight(1f))
                        Box(Modifier.size(44.dp).clip(CircleShape).tap { editNick = true }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Edit, null, tint = pal.ac, modifier = Modifier.size(22.dp)) }
                    }
                    Text("Your nickname (set by " + peerName() + ")", fontSize = 15.sp, color = pal.sub)
                    val by = Prefs.getString("rd_nickby", "")
                    Text(by.ifEmpty { "Not set" }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = if (by.isEmpty()) pal.sub else pal.tx)
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().background(pal.hd)) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("Media", "Links & Files (" + links.size + ")").forEachIndexed { i, label ->
                        Column(Modifier.weight(1f).tap { tabSel = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (tabSel == i) pal.ac else pal.sub, modifier = Modifier.padding(vertical = 16.dp))
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (tabSel == i) pal.ac else Color.Transparent))
                        }
                    }
                }
                if (tabSel == 0) {
                    if (media.isEmpty()) Text("Abhi koi photo ya video nahi hai", fontSize = 16.sp, color = pal.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                    else Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        media.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { m -> Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).tap { viewer = RdStore.blobFile(m.id) }) { FileImage(RdStore.blobFile(m.id), Modifier.fillMaxSize(), ContentScale.Crop, 400) } }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                } else {
                    if (links.isEmpty()) Text("Koi link ya file nahi hai", fontSize = 16.sp, color = pal.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                    else links.forEach { m ->
                        val url = urlRe.find(m.text)?.value
                        Row(
                            Modifier.fillMaxWidth().tap {
                                if (url != null) onLink(url)
                                else try { ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(ctx, RdStore.blobFile(m.id)), m.mediaType ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch (_: Throwable) { Toaster.show("Is file ko kholne wala app nahi mila") }
                            }.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.InsertDriveFile, null, tint = pal.ac, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(url ?: (m.mediaName ?: "file"), fontSize = 16.sp, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (editNick) {
        var v by remember { mutableStateOf(RdStore.nick("peer")) }
        Dialog(onDismissRequest = { editNick = false }) {
            Column(Modifier.clip(RoundedCornerShape(26.dp)).background(pal.hd).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nickname", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pal.tx)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.sec).padding(16.dp)) {
                    if (v.isEmpty()) Text("Nickname likho", fontSize = 18.sp, color = pal.sub)
                    BasicTextField(v, { v = it }, singleLine = true, textStyle = TextStyle(fontSize = 20.sp, color = pal.tx), cursorBrush = SolidColor(pal.tx), modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", fontSize = 17.sp, color = pal.sub, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(14.dp)).tap { editNick = false }.padding(14.dp))
                    Text("Save", fontSize = 17.sp, color = pal.ac, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(14.dp)).tap {
                        val nn = v.trim()
                        if (nn != RdStore.nick("peer")) {
                            RdStore.setNick("peer", nn)
                            if (RdNet.running) { RdNet.sendNick(nn); RdNet.sendNickLog(nn) }
                        }
                        editNick = false
                    }.padding(14.dp))
                }
            }
        }
    }

    viewer?.let { f -> PhotoViewer(if (chat == "p2p") peerName() else chatTitle(chat), f, chat, onClose = { viewer = null }) }
}

/** WhatsApp jaisa photo viewer: kaala background, upar naam + back (+ download / delete), beech me photo */
@Composable
fun PhotoViewer(title: String, file: File, kind: String, onClose: () -> Unit, onDownload: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).tap { onClose() }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp).weight(1f))
                if (onDownload != null) Box(Modifier.size(48.dp).clip(CircleShape).tap { onDownload() }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Download, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                if (onDelete != null) Box(Modifier.size(48.dp).clip(CircleShape).tap { onDelete() }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            }
            Box(Modifier.fillMaxSize().padding(top = 70.dp), contentAlignment = Alignment.Center) {
                if (file.path.isNotEmpty() && file.exists()) FileImage(file, Modifier.fillMaxWidth(), ContentScale.Fit, 2400)
                else RdAvatar(kind, 320.dp, Modifier.fillMaxWidth(0.9f).aspectRatio(1f))
            }
        }
    }
}
