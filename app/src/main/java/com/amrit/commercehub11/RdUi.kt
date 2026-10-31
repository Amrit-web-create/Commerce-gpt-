package com.amrit.commercehub11

import android.graphics.Bitmap
import android.graphics.Paint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RdPal(val dark: Boolean) {
    val bg = if (dark) Color(0xFF0B141A) else Color(0xFFFFFFFF)
    val hd = if (dark) Color(0xFF202C33) else Color(0xFFFFFFFF)
    val tx = if (dark) Color(0xFFE9EDEF) else Color(0xFF111B21)
    val sub = if (dark) Color(0xFF8696A0) else Color(0xFF667781)
    val line = if (dark) Color(0xFF2A3942) else Color(0xFFE9EDEF)
    val out = if (dark) Color(0xFF005C4B) else Color(0xFFD9FDD3)
    val inb = if (dark) Color(0xFF202C33) else Color(0xFFFFFFFF)
    val ac = Color(0xFF00A884)
    val inp = if (dark) Color(0xFF2A3942) else Color(0xFFF0F2F5)
    val tick = Color(0xFF53BDEB)
}

private val WALLS = listOf("default", "white", "blue", "red", "pink")
private fun wallColor(key: String, dark: Boolean): Color = when (key) {
    "white" -> if (dark) Color(0xFF111B21) else Color(0xFFFFFFFF)
    "blue" -> if (dark) Color(0xFF0C1A2E) else Color(0xFFD6E8FF)
    "red" -> if (dark) Color(0xFF2E0C0C) else Color(0xFFFFDEDE)
    "pink" -> if (dark) Color(0xFF2E0C1C) else Color(0xFFFFDCEC)
    else -> if (dark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
}

private fun emojiTile(): Bitmap {
    val s = 480
    val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.textSize = 46f
    p.alpha = 80
    val em = listOf("\u2764\uFE0F", "\u2B50", "\uD83D\uDE0A", "\u2615", "\uD83C\uDFB5", "\u2601\uFE0F", "\uD83C\uDF38", "\u2728", "\uD83C\uDF88", "\uD83C\uDF55", "\uD83D\uDCAC", "\uD83C\uDF08")
    em.forEachIndexed { i, e ->
        val x = (i % 4) * 120f + 30f
        val y = (i / 4) * 160f + 70f
        c.save(); c.rotate(((i * 37) % 40 - 20).toFloat(), x, y); c.drawText(e, x, y, p); c.restore()
    }
    return b
}

@Composable
private fun Wall(key: String, emoji: Boolean, pal: RdPal, modifier: Modifier) {
    val tile = remember { emojiTile() }
    val brush = remember(tile) { ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated)) }
    Box(modifier.background(wallColor(key, pal.dark)).then(if (emoji) Modifier.drawBehind { drawRect(brush) } else Modifier))
}

private fun Context.findActivity(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is ComponentActivity) return c; c = c.baseContext }
    return null
}

private fun fmtTime(ts: Long): String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))
private fun fmtDay(ts: Long): String {
    val f = SimpleDateFormat("yyyyMMdd", Locale.US)
    val d = f.format(Date(ts)); val now = System.currentTimeMillis()
    return when (d) {
        f.format(Date(now)) -> "Today"
        f.format(Date(now - 86400000L)) -> "Yesterday"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))
    }
}

private fun chatName(c: String): String = when (c) {
    "p2p" -> RdCfg.NAMES[RdNet.PEER] ?: "Chat"
    "tg" -> "Telegram"
    "me" -> "You (Message yourself)"
    else -> "Saved"
}

private fun previewOf(m: Msg?): String {
    if (m == null) return "No messages yet"
    if (m.deleted) return "\uD83D\uDEAB This message was deleted"
    if (m.hasMedia) return when (mediaKind(m.mediaType)) {
        "image" -> "\uD83D\uDCF7 Photo"; "video" -> "\uD83C\uDFA5 Video"; "audio" -> "\uD83C\uDFB5 Audio"; else -> "\uD83D\uDCC4 " + (m.mediaName ?: "File")
    }
    return m.text
}

@Composable
fun RdRoot(dark: Boolean, onExit: () -> Unit) {
    val pal = remember(dark) { RdPal(dark) }
    val ctx = LocalContext.current
    var me by remember { mutableStateOf(Prefs.getString("rd_me", "")) }
    var chat by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("chats") }
    var showWall by remember { mutableStateOf(false) }
    var wall by remember { mutableStateOf(Prefs.getString("rd_wp", "default")) }
    var wallEmoji by remember { mutableStateOf(Prefs.getBool("rd_wp_emoji", true)) }

    DisposableEffect(Unit) {
        val act = ctx.findActivity()
        val lo = act?.lifecycle
        val ob = LifecycleEventObserver { _, _ ->
            RdNet.foreground = lo?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
            if (RdNet.foreground) RdNet.curChat?.let { RdNet.markRead(it) }
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

    BackHandler(enabled = chat != null) { chat = null }
    BackHandler(enabled = chat == null && tab == "browser") { tab = "chats" }

    Box(Modifier.fillMaxSize().background(pal.bg)) {
        when {
            me.isEmpty() -> WhoScreen(pal, onExit) { me = it; Prefs.putString("rd_me", it) }
            chat != null -> ChatScreen(pal, chat!!, wall, wallEmoji) { chat = null }
            else -> {
                if (tab == "browser") BrowserScreen(pal) { tab = "chats" }
                else InboxScreen(pal, onExit) { chat = it }
                PillNav(pal, tab, { tab = it }, { showWall = true }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp))
            }
        }
        BannerHost(pal) { c -> chat = c; tab = "chats" }
        if (showWall) WallDialog(pal, wall, wallEmoji, { wall = it; Prefs.putString("rd_wp", it) }, { wallEmoji = it; Prefs.putBool("rd_wp_emoji", it) }) { showWall = false }
    }
}

@Composable
private fun WhoScreen(pal: RdPal, onExit: () -> Unit, onPick: (String) -> Unit) {
    BackHandler { onExit() }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Who are you?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = pal.tx)
        Spacer(Modifier.height(24.dp))
        RdCfg.NAMES.forEach { (k, v) ->
            Text(
                v, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(pal.ac).tap { onPick(k) }.padding(16.dp)
            )
        }
    }
}

@Composable
private fun Avatar(pal: RdPal, name: String, size: Int = 46) {
    val colors = mapOf("p" to Color(0xFFEC4899), "T" to Color(0xFF2AABEE), "Y" to Color(0xFF3B82F6), "S" to Color(0xFF10B981))
    val c = colors[name.take(1)] ?: Color(0xFFEC4899)
    Box(Modifier.size(size.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.3f).sp)
    }
}

@Composable
private fun InboxScreen(pal: RdPal, onExit: () -> Unit, onOpen: (String) -> Unit) {
    val t = RdStore.tick
    var q by remember { mutableStateOf("") }
    BackHandler { onExit() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).tap { onExit() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = pal.tx)
            }
            Text("Inbox", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.tx, modifier = Modifier.padding(start = 6.dp))
        }
        Box(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(24.dp)).background(pal.inp).padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (q.isEmpty()) Text("Search", fontSize = 14.sp, color = pal.sub)
            BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(fontSize = 14.sp, color = pal.tx), cursorBrush = SolidColor(pal.tx), modifier = Modifier.fillMaxWidth())
        }
        val chats = listOf("p2p", "tg", "me", "saved").filter { t >= 0 && chatName(it).lowercase().contains(q.lowercase().trim()) }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
            items(chats, key = { it }) { c ->
                val last = RdStore.msgs.lastOrNull { it.chat == c }
                val unread = RdStore.msgs.count { it.chat == c && it.from == "peer" && !it.read }
                Row(Modifier.fillMaxWidth().tap { onOpen(c) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(pal, if (c == "p2p") "p" + chatName(c) else chatName(c))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(chatName(c), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = pal.tx, maxLines = 1)
                        Text(previewOf(last), fontSize = 13.sp, color = pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (last != null) Text(fmtTime(last.ts), fontSize = 11.sp, color = if (unread > 0) pal.ac else pal.sub)
                        if (unread > 0) Text(unread.toString(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp).clip(CircleShape).background(pal.ac).padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

/** Bottom expanding pill navigation */
@Composable
private fun PillNav(pal: RdPal, tab: String, onTab: (String) -> Unit, onStyle: () -> Unit, modifier: Modifier) {
    Row(
        modifier.shadow(10.dp, CircleShape).clip(CircleShape).background(pal.hd).border(1.dp, pal.line, CircleShape).padding(6.dp).animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        PillItem(pal, Icons.Filled.Chat, "Chats", tab == "chats") { onTab("chats") }
        PillItem(pal, Icons.Filled.Language, "Browser", tab == "browser") { onTab("browser") }
        PillItem(pal, Icons.Filled.Palette, "Style", false) { onStyle() }
    }
}

@Composable
private fun PillItem(pal: RdPal, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sel: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(if (sel) pal.ac else Color.Transparent).tap { onClick() }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (sel) Color.White else pal.sub, modifier = Modifier.size(22.dp))
        AnimatedVisibility(sel, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun BannerHost(pal: RdPal, onOpen: (String) -> Unit) {
    val b = RdNet.banner
    LaunchedEffect(b?.stamp) { if (b != null) { delay(3500); if (RdNet.banner?.stamp == b.stamp) RdNet.banner = null } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(b != null, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
            val d = b
            if (d != null) Row(
                Modifier.padding(10.dp).fillMaxWidth().widthIn(max = 480.dp).shadow(10.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                    .background(pal.hd).border(1.dp, pal.line, RoundedCornerShape(16.dp))
                    .tap { RdNet.banner = null; onOpen(d.chat) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(pal, if (d.chat == "p2p") "p" + d.title else d.title, 38)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(d.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = pal.tx)
                    Text(d.text, fontSize = 13.sp, color = pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun WallDialog(pal: RdPal, wall: String, emoji: Boolean, onWall: (String) -> Unit, onEmoji: (Boolean) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.clip(RoundedCornerShape(20.dp)).background(pal.hd).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Chat background", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = pal.tx)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WALLS.forEach { k ->
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp)).background(wallColor(k, pal.dark))
                            .border(if (k == wall) 3.dp else 1.dp, if (k == wall) pal.ac else pal.line, RoundedCornerShape(12.dp)).tap { onWall(k) },
                        contentAlignment = Alignment.Center
                    ) { Text(k.take(1).uppercase(), color = pal.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Emoji pattern", color = pal.tx, fontSize = 15.sp, modifier = Modifier.weight(1f))
                SupportSwitch(emoji, onEmoji)
            }
            Text("Done", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.ac).tap { onClose() }.padding(12.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChatScreen(pal: RdPal, chat: String, wall: String, wallEmoji: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val t = RdStore.tick
    val list = RdStore.msgs.filter { it.chat == chat && t >= 0 }.takeLast(300)
    val state = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<ReplyRef?>(null) }
    var editing by remember { mutableStateOf<Msg?>(null) }
    var action by remember { mutableStateOf<Msg?>(null) }
    var viewer by remember { mutableStateOf<Msg?>(null) }
    var menu by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    var typingOn by remember { mutableStateOf(false) }

    LaunchedEffect(list.size) { if (list.isNotEmpty()) state.scrollToItem(list.size - 1) }
    LaunchedEffect(list.size, chat) { RdNet.markRead(chat) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val p = copyUriToCache(ctx, uri)
            if (p == null) Toaster.show("File nahi mil payi")
            else if (p.size > MAX_UPLOAD) Toaster.show("File 50 MB se badi hai")
            else RdNet.send(chat, "", p, false, reply).also { reply = null }
        }
    }

    fun doSend() {
        val s = text.trim(); if (s.isEmpty()) return
        val e = editing
        if (e != null) { RdNet.edit(e, s); editing = null } else { RdNet.send(chat, s, null, false, reply); reply = null }
        text = ""; if (typingOn) { typingOn = false; RdNet.sendTyping(false) }
    }

    BackHandler { if (viewer != null) viewer = null else onBack() }

    Box(Modifier.fillMaxSize()) {
        Wall(wall, wallEmoji, pal, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).tap { onBack() }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = pal.tx) }
                Avatar(pal, if (chat == "p2p") "p" + chatName(chat) else chatName(chat), 40)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(chatName(chat), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = pal.tx, maxLines = 1)
                    val sub = when {
                        chat == "p2p" && RdNet.peerTyping -> "typing..."
                        chat == "p2p" && RdNet.peerOnline -> "online"
                        chat == "p2p" && RdNet.peerLastSeen > 0 -> "last seen " + fmtDay(RdNet.peerLastSeen) + " " + fmtTime(RdNet.peerLastSeen)
                        chat == "tg" -> "Telegram bot"
                        chat == "me" -> "Message yourself"
                        chat == "saved" -> "Only on this device"
                        else -> ""
                    }
                    if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp, color = if (sub == "typing..." || sub == "online") pal.ac else pal.sub)
                }
                Box {
                    Box(Modifier.size(40.dp).clip(CircleShape).tap { menu = true }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.MoreVert, null, tint = pal.tx) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Clear chat") }, onClick = { menu = false; RdStore.deleteLocal(RdStore.msgs.filter { it.chat == chat }.map { it.id }) })
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = state, contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items(list, key = { it.id }) { m ->
                    val idx = list.indexOf(m)
                    if (idx == 0 || fmtDay(list[idx - 1].ts) != fmtDay(m.ts)) {
                        Text(fmtDay(m.ts), fontSize = 12.sp, color = pal.sub, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                    MsgBubble(pal, m, t, { action = m }, { if (m.hasMedia && !m.deleted) viewer = m }, { RdNet.react(m, RdCfg.REACTS[0]) })
                }
            }
            if (reply != null || editing != null) {
                Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (editing != null) "Editing message" else "Replying", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.ac)
                        Text(editing?.text ?: reply?.text ?: "", fontSize = 13.sp, color = pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).tap { reply = null; editing = null; text = "" }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, null, tint = pal.sub, modifier = Modifier.size(18.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(pal.hd).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).tap { emojiOpen = !emojiOpen }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EmojiEmotions, null, tint = pal.sub) }
                    Box(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        if (text.isEmpty()) Text("Message", color = pal.sub, fontSize = 16.sp)
                        BasicTextField(text, {
                            text = it
                            val on = it.isNotEmpty()
                            if (on != typingOn) { typingOn = on; RdNet.sendTyping(on) }
                        }, textStyle = TextStyle(fontSize = 16.sp, color = pal.tx), cursorBrush = SolidColor(pal.tx), maxLines = 5, modifier = Modifier.fillMaxWidth())
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).tap { pick.launch(arrayOf("*/*")) }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, null, tint = pal.sub) }
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(pal.ac).tap { doSend() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            if (emojiOpen) {
                LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxWidth().height(220.dp).background(pal.hd)) {
                    gridItems(RD_EMOJIS) { e -> Text(e, fontSize = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.tap { text += e }.padding(6.dp)) }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    action?.let { m ->
        Dialog(onDismissRequest = { action = null }) {
            Column(Modifier.clip(RoundedCornerShape(20.dp)).background(pal.hd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!m.deleted) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RdCfg.REACTS.forEach { e -> Text(e, fontSize = 28.sp, modifier = Modifier.tap { RdNet.react(m, if (m.reactions["me"] == e) "" else e); action = null }.padding(4.dp)) }
                    }
                    ActionItem(pal, "Reply") { reply = ReplyRef(m.id, if (m.from == "me") "me" else "peer", previewOf(m), m.tgId); action = null }
                    if (m.text.isNotEmpty()) ActionItem(pal, "Copy") {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("msg", m.text)); action = null
                    }
                    if (m.from == "me" && m.text.isNotEmpty() && !m.hasMedia) ActionItem(pal, "Edit") { editing = m; text = m.text; action = null }
                }
                ActionItem(pal, "Delete for me") { RdStore.deleteLocal(listOf(m.id)); action = null }
                if (m.from == "me" && !m.deleted) ActionItem(pal, "Unsend") { RdNet.unsend(m); action = null }
            }
        }
    }

    viewer?.let { m ->
        val f = RdStore.blobFile(m.id)
        if (mediaKind(m.mediaType) == "image") {
            Dialog(onDismissRequest = { viewer = null }) {
                Box(Modifier.fillMaxSize().background(Color.Black).tap { viewer = null }, contentAlignment = Alignment.Center) {
                    FileImage(f, Modifier.fillMaxSize(), ContentScale.Fit, 2000)
                }
            }
        } else {
            LaunchedEffect(m.id) {
                try {
                    val i = Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(ctx, f), m.mediaType ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(i)
                } catch (_: Throwable) { Toaster.show("Is file ko kholne wala app nahi mila") }
                viewer = null
            }
        }
    }
}

@Composable
private fun ActionItem(pal: RdPal, label: String, onClick: () -> Unit) {
    Text(label, fontSize = 16.sp, color = pal.tx, modifier = Modifier.fillMaxWidth().tap { onClick() }.padding(vertical = 12.dp, horizontal = 8.dp))
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MsgBubble(pal: RdPal, m: Msg, tick: Int, onLong: () -> Unit, onTap: () -> Unit, onDouble: () -> Unit) {
    val mine = m.from == "me"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(10.dp)).background(if (mine) pal.out else pal.inb)
                .pointerInput(m.id) { detectTapGestures(onLongPress = { onLong() }, onDoubleTap = { onDouble() }, onTap = { onTap() }) }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            if (tick < 0) Text("")
            if (m.fwd) Text("Forwarded", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = pal.sub)
            if (m.replyId != null && !m.deleted) {
                Column(Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.07f)).padding(6.dp)) {
                    Text(if (m.replyWho == "me") "You" else chatName(m.chat), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.ac)
                    Text(m.replyText ?: "", fontSize = 13.sp, color = pal.sub, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (m.deleted) {
                Text("\uD83D\uDEAB This message was deleted", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = pal.sub)
            } else {
                if (m.hasMedia) {
                    val f = RdStore.blobFile(m.id)
                    when {
                        m.mediaFailed && !f.exists() -> Text("\u26A0 Media load nahi hua", fontSize = 13.sp, color = pal.sub)
                        mediaKind(m.mediaType) == "image" -> FileImage(f, Modifier.heightIn(max = 260.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Fit, 800)
                        else -> Text(
                            (if (m.voice) "\uD83C\uDFA4 Voice message" else "\uD83D\uDCC4 " + (m.mediaName ?: "file")) + (if (m.mediaSize > 0) "  \u2022  " + fmtSize(m.mediaSize) else ""),
                            fontSize = 14.sp, color = pal.tx, modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    if (m.prog >= 0) Text("Uploading...", fontSize = 11.sp, color = pal.sub)
                }
                if (m.text.isNotEmpty()) Text(m.text, fontSize = 16.sp, color = pal.tx)
            }
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (m.edited && !m.deleted) Text("edited ", fontSize = 10.sp, color = pal.sub)
                Text(fmtTime(m.ts), fontSize = 10.sp, color = pal.sub)
                if (mine && !m.deleted) {
                    Spacer(Modifier.width(3.dp))
                    when (m.status) {
                        "pending" -> Icon(Icons.Filled.Schedule, null, tint = pal.sub, modifier = Modifier.size(13.dp))
                        "sent" -> Icon(Icons.Filled.Done, null, tint = pal.sub, modifier = Modifier.size(14.dp))
                        "delivered" -> Icon(Icons.Filled.DoneAll, null, tint = pal.sub, modifier = Modifier.size(14.dp))
                        "seen" -> Icon(Icons.Filled.DoneAll, null, tint = pal.tick, modifier = Modifier.size(14.dp))
                        else -> {}
                    }
                }
            }
            if (m.reactions.isNotEmpty() && !m.deleted) Text(m.reactions.values.joinToString(" "), fontSize = 14.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}
