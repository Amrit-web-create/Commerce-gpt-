package com.amrit.commercehub11

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

/** Notification tap se kisi chat ko seedha kholne ke liye */
object Nav {
    var openChat by mutableStateOf<String?>(null)
}

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("open_chat")?.let { Nav.openChat = it }
    }

    override fun onStart() {
        super.onStart()
        RdNet.changeForeground(true)
    }

    override fun onStop() {
        RdNet.changeForeground(false)
        if (!Prefs.notifEnabled && RdCall.state == "idle" && RdNet.running) RdNet.stop()
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) BrowserState.clear()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        Conn.start(this)
        RdStore.init(this)
        Notifier.init(this)
        RdCall.init(this)
        intent?.getStringExtra("open_chat")?.let { Nav.openChat = it }
        if (Prefs.notifEnabled && Notifier.canPost()) Notifier.startService(this)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var themeMode by remember { mutableStateOf(Prefs.themeMode) }
    val sysDark = isSystemInDarkTheme()
    val dark = themeMode == "dark" || (themeMode == "auto" && sysDark)
    val pal = if (dark) DarkPal else LightPal
    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window
        if (w != null) {
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }

    var screen by remember { mutableStateOf(if (Nav.openChat != null) "rd" else "main") }
    LaunchedEffect(Nav.openChat) { if (Nav.openChat != null) screen = "rd" }          // main | settings | contact | rd
    var tab by remember { mutableStateOf("home") }
    var openBook by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var pdf by remember { mutableStateOf<Pair<String, String>?>(null) }
    var supportOn by remember { mutableStateOf(Prefs.supportActive) }
    var drawer by remember { mutableStateOf(false) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val saved = remember { mutableStateListOf<String>().apply { addAll(Prefs.loadSavedBooks()) } }

    // ---- Notification toggle (default OFF) ----
    var notifOn by remember { mutableStateOf(Prefs.notifEnabled) }
    fun enableNotif() {
        Prefs.notifEnabled = true; notifOn = true
        Notifier.startService(ctx)
        Toaster.show("Notifications ON")
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enableNotif() else { Prefs.notifEnabled = false; notifOn = false; Toaster.show("Notification permission allow karein") }
    }
    val onNotif: (Boolean) -> Unit = { want ->
        if (want) {
            if (Notifier.canPost()) enableNotif() else permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Prefs.notifEnabled = false; notifOn = false
            Notifier.stopService(ctx)
            if (!RdNet.uiOpen) RdNet.stop()
        }
    }

    BackHandler(enabled = pdf == null && drawer.not() && screen == "main" && openBook != null) { openBook = null }
    BackHandler(enabled = screen == "settings" && pdf == null) { screen = "main" }

    val dens = LocalDensity.current
    CompositionLocalProvider(LocalPal provides pal, LocalDensity provides Density(dens.density, dens.fontScale * 1.3f)) {
        Box(Modifier.fillMaxSize().background(pal.bg).onSizeChanged { box = it }) {
            when (screen) {
                "settings" -> Box(Modifier.fillMaxSize().background(pal.bg).statusBarsPadding().navigationBarsPadding()) {
                    SettingsScreen(
                        themeMode, { themeMode = it; Prefs.themeMode = it }, notifOn, onNotif,
                        { screen = "main" }, { screen = "contact" }
                    )
                }
                "contact" -> Box(Modifier.fillMaxSize().background(pal.bg).statusBarsPadding().navigationBarsPadding()) {
                    ContactScreen({ screen = "settings" }, { screen = "rd" })
                }
                "rd" -> Box(Modifier.fillMaxSize().background(RdPal(dark).bg).statusBarsPadding()) {
                    RdRoot(dark) { screen = "settings" }
                }
                else -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    AppHeader(
                        supportOn, { supportOn = it; Prefs.supportActive = it; if (!it) drawer = false },
                        { screen = "settings" }, query, { query = it }
                    )
                    Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.widthIn(max = 512.dp).fillMaxWidth()) {
                            val book = booksData.firstOrNull { it.id == openBook }
                            if (book != null) {
                                BookDetail(
                                    book, saved.contains(book.id), { openBook = null },
                                    {
                                        if (saved.contains(book.id)) saved.remove(book.id) else saved.add(book.id)
                                        Prefs.saveSavedBooks(saved.toList())
                                    },
                                    { ch -> pdf = Pair(book.title + " - " + ch.name, ch.url) }
                                )
                            } else if (tab == "home") HomeTab(query) { openBook = it.id }
                            else MyBooksTab(saved.toList()) { openBook = it.id }
                        }
                    }
                    BottomNav(tab) { tab = it; openBook = null }
                }
            }

            pdf?.let { p -> Box(Modifier.fillMaxSize().background(pal.bg)) { PdfOverlay(p.first, p.second) { pdf = null } } }

            if (supportOn && pdf == null && (screen == "main" || screen == "settings")) FloatingAvatar(box) { drawer = true }
            if (drawer && supportOn) SupportDrawer { drawer = false }

            CallHost()
            ToastHost()
            ConnBanner()
        }
    }
}

@Composable
private fun ToastHost() {
    val pal = LocalPal.current
    val msg by Toaster.message.collectAsState()
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(msg) {
        val m = msg ?: return@LaunchedEffect
        shown = m.second; visible = true
        delay(2400)
        visible = false
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
            Text(
                shown, color = if (pal.dark) Slate.s900 else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp).shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(if (pal.dark) Color.White else Slate.s900).padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

/** "You are offline" / "Back online" chhota smooth banner */
@Composable
private fun ConnBanner() {
    val online by Conn.online.collectAsState()
    var state by remember { mutableStateOf("") }   // "", offline, online
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(online) {
        if (!online) { state = "offline" }
        else if (state == "offline") { state = "online"; delay(2200); state = "" }
        first = false
    }
    var last by remember { mutableStateOf("offline") }
    if (state.isNotEmpty()) last = state
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(state.isNotEmpty(), enter = slideInVertically { -it * 2 } + fadeIn(), exit = slideOutVertically { -it * 2 } + fadeOut()) {
            val off = last == "offline"
            Row(
                Modifier.shadow(8.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                    .background(if (off) Slate.s800 else Emerald600).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (off) Icons.Filled.WifiOff else Icons.Filled.Wifi, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (off) "You are offline" else "Back online", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
