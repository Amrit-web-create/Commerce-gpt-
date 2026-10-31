package com.amrit.commercehub11

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.URLEncoder

fun Context.findAct(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is ComponentActivity) return c; c = c.baseContext }
    return null
}

class BTab(val view: WebView, val home: Boolean) {
    var title by mutableStateOf(if (home) "Home" else "New tab")
    var url by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var blank by mutableStateOf(!home)
    var bg by mutableStateOf<Color?>(null)
}

/** Browser ka state global rehta hai: back karke wapas aao to tabs wahi khule milte hain, sirf X dabane par clean hota hai */
object BrowserState {
    val tabs = mutableStateListOf<BTab>()
    var active by mutableIntStateOf(0)
    var barVisible by mutableStateOf(true)
    var fullscreen by mutableStateOf(false)
    var customView by mutableStateOf<View?>(null)
    var customCb: WebChromeClient.CustomViewCallback? = null
    var engine by mutableStateOf(Prefs.getString("rd_engine", "google"))
    const val MAX = 5

    fun homeUrl(): String = Prefs.getString("rd_home_url", "").ifEmpty { "https://www.google.com" }

    fun searchUrl(q: String): String =
        (if (engine == "brave") "https://search.brave.com/search?q=" else "https://www.google.com/search?q=") + URLEncoder.encode(q, "UTF-8")

    fun resolve(q: String): String {
        val t = q.trim()
        if (t.startsWith("http://") || t.startsWith("https://")) return t
        if (!t.contains(" ") && t.contains(".")) return "https://$t"
        return searchUrl(t)
    }

    fun toggleEngine() {
        engine = if (engine == "brave") "google" else "brave"
        Prefs.putString("rd_engine", engine)
        Toaster.show(if (engine == "brave") "Search: Brave" else "Search: Google")
    }

    fun ensure(ctx: Context) {
        if (tabs.isNotEmpty()) return
        val t = make(ctx, true)
        tabs.add(t); active = 0
        t.view.loadUrl(homeUrl())
    }

    fun open(ctx: Context, url: String) {
        ensure(ctx)
        val t: BTab
        if (tabs.size < MAX) { t = make(ctx, false); tabs.add(t) } else t = tabs[active.coerceIn(0, tabs.size - 1)]
        t.blank = false
        t.view.loadUrl(url)
        active = tabs.indexOf(t)
        barVisible = true
    }

    fun newTab(ctx: Context) {
        ensure(ctx)
        if (tabs.size >= MAX) { Toaster.show("Max $MAX tabs"); return }
        tabs.add(make(ctx, false)); active = tabs.size - 1; barVisible = true
    }

    fun closeTab(i: Int) {
        if (i <= 0 || i >= tabs.size) return
        val t = tabs.removeAt(i)
        destroy(t)
        active = if (active >= i) (active - 1).coerceAtLeast(0) else active
    }

    private fun destroy(t: BTab) {
        try { (t.view.parent as? ViewGroup)?.removeView(t.view) } catch (_: Throwable) {}
        try { t.view.destroy() } catch (_: Throwable) {}
    }

    fun clear() {
        tabs.forEach { destroy(it) }
        tabs.clear(); active = 0; fullscreen = false; customView = null; barVisible = true
    }

    private fun parseCss(raw: String?): Color? {
        val s = (raw ?: "").trim().trim('"')
        try {
            if (s.startsWith("#")) {
                val h = s.substring(1)
                val full = if (h.length == 3) h.map { "$it$it" }.joinToString("") else h.take(6)
                return Color(0xFF000000L or full.toLong(16))
            }
            if (s.startsWith("rgb")) {
                val n = Regex("[\\d.]+").findAll(s).map { it.value.toFloat() }.toList()
                if (n.size >= 3 && (n.size < 4 || n[3] > 0.05f)) return Color(n[0] / 255f, n[1] / 255f, n[2] / 255f)
            }
        } catch (_: Throwable) {}
        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun make(ctx: Context, home: Boolean): BTab {
        val wv = WebView(ctx)
        val tab = BTab(wv, home)
        val st = wv.settings
        st.javaScriptEnabled = true
        st.domStorageEnabled = true
        st.mediaPlaybackRequiresUserGesture = false
        st.setSupportMultipleWindows(false)
        st.javaScriptCanOpenWindowsAutomatically = false
        st.loadWithOverviewMode = true
        st.useWideViewPort = true
        st.builtInZoomControls = true
        st.displayZoomControls = false
        try { CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true) } catch (_: Throwable) {}
        wv.webViewClient = object : WebViewClient() {
            // koi bhi link isi WebView me khulega (Chrome / dusra app nahi)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url.toString()
                return !(u.startsWith("http://") || u.startsWith("https://"))
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                tab.url = url; tab.blank = false; barVisible = true
            }

            override fun onPageFinished(view: WebView, url: String) {
                tab.url = url; tab.title = view.title ?: url
                val js = "(function(){var m=document.querySelector('meta[name=theme-color]');var c=m&&m.content;if(!c){var e=document.body||document.documentElement;c=getComputedStyle(e).backgroundColor;if(!c||c=='rgba(0, 0, 0, 0)'){c=getComputedStyle(document.documentElement).backgroundColor}}return c||''})()"
                view.evaluateJavascript(js) { r -> tab.bg = parseCss(r) }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadData("<html><body style='font-family:sans-serif;text-align:center;padding:80px 20px;color:#666'><h2>You are offline</h2><p>Internet connect karke refresh karein.</p></body></html>", "text/html", "utf-8")
                }
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { tab.progress = newProgress }
            override fun onReceivedTitle(view: WebView, title: String?) { if (!title.isNullOrEmpty()) tab.title = title }
            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                customView = view; customCb = callback
            }
            override fun onHideCustomView() { customView = null; customCb = null }
        }
        wv.setOnScrollChangeListener { _, _, y, _, oy ->
            val dy = y - oy
            if (dy > 14) barVisible = false else if (dy < -14) barVisible = true
        }
        return tab
    }
}

private fun setBars(ctx: Context, hide: Boolean) {
    val w = ctx.findAct()?.window ?: return
    val c = WindowCompat.getInsetsController(w, w.decorView)
    if (hide) {
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    } else c.show(WindowInsetsCompat.Type.systemBars())
}

@Composable
private fun Welcome(pal: RdPal) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Welcome",
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(Color(0xFF00A884), Color(0xFF7F66FF), Color(0xFFFF2D74))),
                fontSize = 56.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, letterSpacing = 1.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BrowserScreen(pal: RdPal, onBack: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    LaunchedEffect(Unit) { BrowserState.ensure(ctx) }
    val tabs = BrowserState.tabs
    if (tabs.isEmpty()) return
    val idx = BrowserState.active.coerceIn(0, tabs.size - 1)
    val tab = tabs[idx]
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        BrowserState.tabs.forEach { try { it.view.onResume() } catch (_: Throwable) {} }
        onDispose {
            BrowserState.tabs.forEach { try { it.view.onPause() } catch (_: Throwable) {} }
            setBars(ctx, false)
        }
    }
    val fs = BrowserState.fullscreen || BrowserState.customView != null
    LaunchedEffect(fs) { setBars(ctx, fs) }

    val pageBg = if (tab.blank) pal.bg else (tab.bg ?: pal.bg)
    val fg = if (pageBg.luminance() > 0.5f) Color(0xFF111111) else Color.White
    val barShown = (BrowserState.barVisible || tab.blank) && !BrowserState.fullscreen
    val pad by animateDpAsState(if (barShown) 64.dp else 0.dp, tween(220), label = "pad")

    BackHandler {
        when {
            BrowserState.customView != null -> { BrowserState.customCb?.onCustomViewHidden(); BrowserState.customView = null }
            BrowserState.fullscreen -> BrowserState.fullscreen = false
            !tab.blank && tab.view.canGoBack() -> tab.view.goBack()
            idx > 0 -> BrowserState.closeTab(idx)
            else -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(pageBg)) {
        Box(Modifier.fillMaxSize().padding(top = pad)) {
            if (tab.blank) Welcome(pal)
            else key(tab) { AndroidView(factory = { tab.view }, modifier = Modifier.fillMaxSize()) }
        }
        if (!tab.blank && tab.progress in 1..99) {
            Box(Modifier.padding(top = pad).fillMaxWidth(tab.progress / 100f).height(3.dp).background(Color(0xFF00A884)))
        }

        // top bar: page ke colour jaisa, transparent feel
        AnimatedVisibility(
            barShown, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).background(pageBg).padding(start = 64.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(fg.copy(alpha = 0.08f)).padding(start = 6.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (BrowserState.engine == "brave") Color(0xFFFF5500) else Color(0xFF4285F4))
                            .tap { BrowserState.toggleEngine() },
                        contentAlignment = Alignment.Center
                    ) { Text(if (BrowserState.engine == "brave") "B" else "G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        val host = try { Uri.parse(tab.url).host ?: "" } catch (_: Throwable) { "" }
                        if (input.isEmpty()) Text(if (host.isNotEmpty() && !tab.blank) host.removePrefix("www.") else "Search", fontSize = 16.sp, color = fg.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BasicTextField(
                            value = input, onValueChange = { input = it }, singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = fg), cursorBrush = SolidColor(fg),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                if (input.isNotBlank()) { tab.blank = false; tab.view.loadUrl(BrowserState.resolve(input)); input = ""; focus.clearFocus() }
                            }),
                            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) input = "" }
                        )
                    }
                }
                BarBtn(fg) { BrowserState.newTab(ctx) }
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, fg.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).tap { showTabs = true },
                    contentAlignment = Alignment.Center
                ) { Text(tabs.size.toString(), color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                Box {
                    Box(Modifier.size(44.dp).clip(CircleShape).tap { menu = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MoreVert, null, tint = fg, modifier = Modifier.size(24.dp))
                    }
                    DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(18.dp), containerColor = pal.hd, shadowElevation = 12.dp) {
                        MenuItem(pal, "Refresh", Icons.Outlined.Refresh) { menu = false; tab.view.reload() }
                        MenuItem(pal, "Copy link", Icons.Outlined.Link) {
                            menu = false
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("link", tab.view.url ?: "")); Toaster.show("Link copied")
                        }
                        MenuItem(pal, "Share", Icons.Outlined.Share) {
                            menu = false
                            val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, tab.view.url ?: "")
                            try { ctx.startActivity(Intent.createChooser(i, "Share link")) } catch (_: Throwable) {}
                        }
                        MenuItem(pal, "Pin to home", Icons.Outlined.PushPin) {
                            menu = false
                            val u = tab.view.url ?: ""
                            if (u.isNotEmpty() && !tab.blank) { Prefs.putString("rd_home_url", u); Toaster.show("Pinned to Home tab") }
                        }
                        MenuItem(pal, "Full screen", Icons.Outlined.Fullscreen) { menu = false; BrowserState.fullscreen = true }
                        MenuItem(pal, "Clear cookies & data", Icons.Outlined.DeleteSweep) {
                            menu = false
                            try {
                                CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush()
                                WebStorage.getInstance().deleteAllData()
                                BrowserState.tabs.forEach { it.view.clearCache(true); it.view.clearHistory() }
                                Toaster.show("Cookies & cache cleared")
                            } catch (_: Throwable) {}
                        }
                        if (idx > 0) MenuItem(pal, "Close tab", Icons.Filled.Close) { menu = false; BrowserState.closeTab(idx) }
                    }
                }
            }
        }

        // X: hamesa dikhta hai. Dabane par browser poora clean (sab tabs band)
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp).size(44.dp).clip(CircleShape)
                .background(fg.copy(alpha = 0.10f)).tap { BrowserState.clear(); onClose() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Close, null, tint = fg, modifier = Modifier.size(24.dp)) }

        // video fullscreen (YouTube etc.)
        BrowserState.customView?.let { cv ->
            Box(Modifier.fillMaxSize().background(Color.Black)) { AndroidView(factory = { cv }, modifier = Modifier.fillMaxSize()) }
        }
    }

    if (showTabs) {
        Dialog(onDismissRequest = { showTabs = false }) {
            Column(Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(26.dp)).background(pal.hd).padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tabs", fontFamily = FontFamily.Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pal.tx)
                tabs.forEachIndexed { i, t ->
                    val host = try { Uri.parse(t.url).host ?: "" } catch (_: Throwable) { "" }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (i == idx) pal.ac.copy(alpha = 0.15f) else pal.sec)
                            .border(if (i == idx) 1.5.dp else 0.dp, if (i == idx) pal.ac else Color.Transparent, RoundedCornerShape(16.dp))
                            .tap { BrowserState.active = i; showTabs = false }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(if (t.blank) "New tab" else t.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (t.home) "Home" else host.removePrefix("www."), fontSize = 14.sp, color = pal.sub, maxLines = 1)
                        }
                        if (i > 0) Box(Modifier.size(38.dp).clip(CircleShape).tap { BrowserState.closeTab(i) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, null, tint = pal.sub)
                        }
                    }
                }
                if (tabs.size < BrowserState.MAX) Text(
                    "+  New tab", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.ac).tap { BrowserState.newTab(ctx); showTabs = false }.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun BarBtn(fg: Color, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).tap { onClick() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Add, null, tint = fg, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun MenuItem(pal: RdPal, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 17.sp, color = pal.tx) },
        leadingIcon = { Icon(icon, null, tint = pal.sub) },
        onClick = onClick
    )
}
