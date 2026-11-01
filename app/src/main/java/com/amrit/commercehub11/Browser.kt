package com.amrit.commercehub11

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.gestures.detectTapGestures
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

fun Context.findAct(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is ComponentActivity) return c; c = c.baseContext }
    return null
}

const val UA_MOBILE = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
const val UA_DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

class Pin(val title: String, val url: String)

class BTab(val view: WebView, val home: Boolean) {
    var title by mutableStateOf(if (home) "Home" else "New tab")
    var url by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var blank by mutableStateOf(true)
    var bg by mutableStateOf<Color?>(null)
    var failed by mutableStateOf(false)
    var desktop by mutableStateOf(false)
}

fun hostOf(u: String): String = try { (Uri.parse(u).host ?: u).removePrefix("www.") } catch (_: Throwable) { u }

/** Browser ka state global: back karke wapas aao to tabs wahi khule milte hain, sirf X dabane par clean hota hai */
object BrowserState {
    val tabs = mutableStateListOf<BTab>()
    val pins = mutableStateListOf<Pin>()
    var active by mutableIntStateOf(0)
    var barVisible by mutableStateOf(true)
    var fullscreen by mutableStateOf(false)
    var customView by mutableStateOf<View?>(null)
    var customCb: WebChromeClient.CustomViewCallback? = null
    var engine by mutableStateOf(Prefs.getString("rd_engine", "google"))
    var zoom by mutableIntStateOf(100)
    var fileCb: ValueCallback<Array<Uri>>? = null
    var chooserParams by mutableStateOf<WebChromeClient.FileChooserParams?>(null)
    private var pinsLoaded = false
    private var hostAct: ComponentActivity? = null
    const val MAX = 6

    /** Video fullscreen: Chrome ki tarah poori window (decor view) me lagta hai => smooth + audio/video sahi */
    fun showCustom(view: View?, cb: WebChromeClient.CustomViewCallback?) {
        if (view == null) return
        removeCustom()
        customView = view; customCb = cb
        try { (hostAct?.window?.decorView as? ViewGroup)?.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) } catch (_: Throwable) {}
    }

    fun removeCustom() {
        val v = customView
        if (v != null) { try { (v.parent as? ViewGroup)?.removeView(v) } catch (_: Throwable) {} }
        customView = null
        try { customCb?.onCustomViewHidden() } catch (_: Throwable) {}
        customCb = null
    }

    fun loadPins() {
        if (pinsLoaded) return
        pinsLoaded = true
        try {
            val a = JSONArray(Prefs.getString("rd_pins", "[]"))
            for (i in 0 until a.length()) { val o = a.getJSONObject(i); pins.add(Pin(o.optString("t"), o.optString("u"))) }
        } catch (_: Throwable) {}
    }

    private fun savePins() {
        val a = JSONArray()
        pins.forEach { a.put(JSONObject().put("t", it.title).put("u", it.url)) }
        Prefs.putString("rd_pins", a.toString())
    }

    fun addPin(title: String, url: String) {
        loadPins()
        if (pins.any { hostOf(it.url) == hostOf(url) }) { Toaster.show("Already pinned"); return }
        pins.add(Pin(title.ifEmpty { hostOf(url) }, url)); savePins(); Toaster.show("Pinned to Home")
    }

    fun removePin(p: Pin) { pins.remove(p); savePins() }

    /** Google me AI Mode / AI overview nahi (udm=14 = sirf web results) */
    fun searchUrl(q: String): String {
        val e = URLEncoder.encode(q, "UTF-8")
        return if (engine == "brave") "https://search.brave.com/search?q=$e&source=web" else "https://www.google.com/search?q=$e&udm=14"
    }

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

    fun load(tab: BTab, url: String) {
        if (!Conn.isOnline()) { Toaster.show("You are offline"); return }
        tab.blank = false
        tab.view.loadUrl(url)
    }

    fun ensure(ctx: Context) {
        hostAct = ctx.findAct()
        loadPins()
        if (tabs.isNotEmpty()) return
        tabs.add(make(ctx, true)); active = 0
    }

    fun open(ctx: Context, url: String) {
        ensure(ctx)
        val t: BTab
        if (tabs.size < MAX) { t = make(ctx, false); tabs.add(t) } else t = tabs[active.coerceIn(0, tabs.size - 1)]
        active = tabs.indexOf(t); barVisible = true
        load(t, url)
    }

    fun newTab(ctx: Context) {
        ensure(ctx)
        if (tabs.size >= MAX) { Toaster.show("Max $MAX tabs"); return }
        tabs.add(make(ctx, false)); active = tabs.size - 1; barVisible = true
    }

    fun closeTab(i: Int) {
        if (i <= 0 || i >= tabs.size) return
        destroy(tabs.removeAt(i))
        active = if (active >= i) (active - 1).coerceAtLeast(0) else active
    }

    private fun destroy(t: BTab) {
        try { (t.view.parent as? ViewGroup)?.removeView(t.view) } catch (_: Throwable) {}
        try { t.view.destroy() } catch (_: Throwable) {}
    }

    fun clear() {
        tabs.forEach { destroy(it) }
        removeCustom()
        tabs.clear(); active = 0; fullscreen = false; barVisible = true
        fileCb?.onReceiveValue(null); fileCb = null
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

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun make(ctx: Context, home: Boolean): BTab {
        val wv = WebView(ctx)
        val tab = BTab(wv, home)
        val st = wv.settings
        st.javaScriptEnabled = true
        st.domStorageEnabled = true
        st.databaseEnabled = true
        st.mediaPlaybackRequiresUserGesture = false
        st.setSupportMultipleWindows(false)
        st.javaScriptCanOpenWindowsAutomatically = true
        st.loadWithOverviewMode = true
        st.useWideViewPort = true
        st.builtInZoomControls = true
        st.displayZoomControls = false
        st.allowFileAccess = false
        st.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        st.userAgentString = UA_MOBILE
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        st.textZoom = zoom
        try {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        } catch (_: Throwable) {}
        wv.setDownloadListener { url, ua, cd, mime, _ ->
            try {
                val r = DownloadManager.Request(Uri.parse(url))
                r.setMimeType(mime)
                CookieManager.getInstance().getCookie(url)?.let { r.addRequestHeader("cookie", it) }
                r.addRequestHeader("User-Agent", ua)
                val name = URLUtil.guessFileName(url, cd, mime)
                r.setTitle(name)
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                r.setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, name)
                (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(r)
                Toaster.show("Download shuru ho gaya")
            } catch (_: Throwable) { Toaster.show("Download nahi hua") }
        }
        wv.webViewClient = object : WebViewClient() {
            // koi bhi link isi WebView me khulega; offline ho to page wahi rehta hai
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url.toString()
                if (!(u.startsWith("http://") || u.startsWith("https://"))) return true
                if (!Conn.isOnline()) { Toaster.show("You are offline"); return true }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                tab.url = url; tab.blank = false; tab.failed = false; barVisible = true
            }

            override fun onPageFinished(view: WebView, url: String) {
                tab.url = url; tab.title = view.title ?: url
                try { CookieManager.getInstance().flush() } catch (_: Throwable) {}
                val js = "(function(){var m=document.querySelector('meta[name=theme-color]');var c=m&&m.content;if(!c){var e=document.body||document.documentElement;c=getComputedStyle(e).backgroundColor;if(!c||c=='rgba(0, 0, 0, 0)'){c=getComputedStyle(document.documentElement).backgroundColor}}return c||''})()"
                view.evaluateJavascript(js) { r -> tab.bg = parseCss(r) }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) tab.failed = true
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { tab.progress = newProgress }
            override fun onReceivedTitle(view: WebView, title: String?) { if (!title.isNullOrEmpty()) tab.title = title }
            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) { showCustom(view, callback) }
            override fun onHideCustomView() { removeCustom() }
            override fun onPermissionRequest(request: PermissionRequest?) { request?.grant(request.resources) }
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?): Boolean {
                fileCb?.onReceiveValue(null)
                fileCb = filePathCallback
                chooserParams = fileChooserParams
                return true
            }
        }
        // scroll / swipe karte hi bar chhup jata hai (Instagram reels jaise inner-scroll me bhi)
        var downY = 0f
        wv.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> downY = ev.y
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.y - downY
                    if (dy < -60f) { barVisible = false; downY = ev.y } else if (dy > 60f) { barVisible = true; downY = ev.y }
                }
                else -> {}
            }
            false
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

/** Video chal rahi ho to layout badalne ke baad bhi chalti rahe */
private fun keepPlaying(tab: BTab, change: () -> Unit) {
    tab.view.evaluateJavascript("(function(){var v=document.querySelector('video');return (v&&!v.paused)?'1':'0'})()") { r ->
        change()
        if (r != null && r.contains("1")) {
            tab.view.postDelayed({ tab.view.evaluateJavascript("(function(){var v=document.querySelector('video');if(v&&v.paused)v.play()})()", null) }, 700)
        }
    }
}

@Composable
private fun HomePage(pal: RdPal, onOpen: (Pin) -> Unit) {
    var menuFor by remember { mutableStateOf<Pin?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            "Welcome",
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(Color(0xFF00A884), Color(0xFF7F66FF), Color(0xFFFF2D74))),
                fontSize = 56.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, letterSpacing = 1.sp
            ),
            textAlign = TextAlign.Center
        )
        if (BrowserState.pins.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            BrowserState.pins.toList().chunked(4).forEach { row ->
                Row(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        Box(Modifier.weight(1f)) {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).pointerInput(p) { detectTapGestures(onTap = { onOpen(p) }, onLongPress = { menuFor = p }) }.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val colors = listOf(0xFF7F66FF, 0xFFFF2D74, 0xFFFF7A01, 0xFF00A884, 0xFF229ED9, 0xFF5157AF)
                                val c = Color(colors[Math.abs(hostOf(p.url).hashCode()) % colors.size])
                                Box(Modifier.size(64.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                                    Text(hostOf(p.url).take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                                    RemoteImage("https://www.google.com/s2/favicons?domain=" + hostOf(p.url) + "&sz=128", Modifier.size(64.dp).clip(CircleShape).background(Color.White).padding(14.dp), ContentScale.Fit, 200)
                                }
                                Text(hostOf(p.url), fontSize = 13.sp, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            }
                            DropdownMenu(menuFor === p, { menuFor = null }, shape = RoundedCornerShape(18.dp), containerColor = pal.hd, shadowElevation = 12.dp) {
                                DropdownMenuItem(
                                    text = { Text("Delete", fontSize = 17.sp, color = Color(0xFFE53935)) },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null, tint = Color(0xFFE53935)) },
                                    onClick = { menuFor = null; BrowserState.removePin(p) }
                                )
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun BrowserScreen(pal: RdPal, onBack: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val dens = LocalDensity.current
    LaunchedEffect(Unit) { BrowserState.ensure(ctx) }
    val tabs = BrowserState.tabs
    if (tabs.isEmpty()) return
    val idx = BrowserState.active.coerceIn(0, tabs.size - 1)
    val tab = tabs[idx]
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    val online by Conn.online.collectAsState()

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        BrowserState.fileCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
        BrowserState.fileCb = null
    }
    val cp = BrowserState.chooserParams
    LaunchedEffect(cp) {
        if (cp != null) {
            BrowserState.chooserParams = null
            try { fileLauncher.launch(cp.createIntent()) } catch (_: Throwable) { BrowserState.fileCb?.onReceiveValue(null); BrowserState.fileCb = null }
        }
    }
    // internet wapas aate hi jo page fail hua tha wo reload
    LaunchedEffect(online) { if (online) BrowserState.tabs.forEach { if (it.failed) { it.failed = false; it.view.reload() } } }
    LaunchedEffect(BrowserState.zoom) { BrowserState.tabs.forEach { it.view.settings.textZoom = BrowserState.zoom } }

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
    val showBar = (BrowserState.barVisible || tab.blank) && !BrowserState.fullscreen
    val prog by animateFloatAsState(if (showBar) 1f else 0f, tween(260, easing = FastOutSlowInEasing), label = "bar")
    val barPx = with(dens) { 64.dp.toPx() }

    BackHandler {
        when {
            BrowserState.customView != null -> BrowserState.removeCustom()
            BrowserState.fullscreen -> keepPlaying(tab) { BrowserState.fullscreen = false }
            !tab.blank && tab.view.canGoBack() -> tab.view.goBack()
            idx > 0 -> BrowserState.closeTab(idx)
            else -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(pageBg)) {
        // page: GPU transform se upar-niche (relayout nahi) => smooth
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = barPx * prog }) {
            if (tab.blank) HomePage(pal) { p -> BrowserState.load(tab, p.url) }
            else key(tab) { AndroidView(factory = { (tab.view.parent as? ViewGroup)?.removeView(tab.view); tab.view }, modifier = Modifier.fillMaxSize()) }
        }
        if (!tab.blank && tab.progress in 1..99) {
            Box(Modifier.fillMaxWidth(tab.progress / 100f).height(3.dp).graphicsLayer { translationY = barPx * prog }.background(Color(0xFF00A884)))
        }

        // top bar: page ke colour jaisa, scroll par smooth slide
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(64.dp)
                .graphicsLayer { translationY = -barPx * (1f - prog); alpha = prog }
                .background(pageBg).padding(start = 64.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(fg.copy(alpha = 0.08f)).padding(start = 6.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(if (BrowserState.engine == "brave") Color(0xFFFF5500) else Color(0xFF4285F4)).tap { BrowserState.toggleEngine() },
                    contentAlignment = Alignment.Center
                ) { Text(if (BrowserState.engine == "brave") "B" else "G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    val host = hostOf(tab.url)
                    if (input.isEmpty()) Text(if (host.isNotEmpty() && !tab.blank) host else "Search", fontSize = 16.sp, color = fg.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(
                        value = input, onValueChange = { input = it }, singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = fg), cursorBrush = SolidColor(fg),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (input.isNotBlank()) { BrowserState.load(tab, BrowserState.resolve(input)); input = ""; focus.clearFocus() }
                        }),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) input = "" }
                    )
                }
            }
            Box(Modifier.size(44.dp).clip(CircleShape).tap { BrowserState.newTab(ctx) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, null, tint = fg, modifier = Modifier.size(26.dp))
            }
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, fg.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).tap { showTabs = true },
                contentAlignment = Alignment.Center
            ) { Text(tabs.size.toString(), color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Box {
                Box(Modifier.size(44.dp).clip(CircleShape).tap { menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreVert, null, tint = fg, modifier = Modifier.size(24.dp))
                }
                DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(20.dp), containerColor = pal.hd, shadowElevation = 14.dp) {
                    MenuItem(pal, "Refresh", Icons.Outlined.Refresh) { menu = false; if (Conn.isOnline()) tab.view.reload() else Toaster.show("You are offline") }
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
                        if (u.isNotEmpty() && !tab.blank) BrowserState.addPin(tab.title, u) else Toaster.show("Pehle koi site kholo")
                    }
                    MenuItem(pal, if (tab.desktop) "Mobile site" else "Desktop site", Icons.Outlined.Computer) {
                        menu = false
                        tab.desktop = !tab.desktop
                        tab.view.settings.userAgentString = if (tab.desktop) UA_DESKTOP else UA_MOBILE
                        if (!tab.blank && Conn.isOnline()) tab.view.reload()
                    }
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Zoom", fontSize = 17.sp, color = pal.tx, modifier = Modifier.weight(1f))
                        Box(Modifier.size(38.dp).clip(CircleShape).background(pal.sec).tap { BrowserState.zoom = (BrowserState.zoom - 10).coerceAtLeast(50) }, contentAlignment = Alignment.Center) { Text("\u2212", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.tx) }
                        Text(BrowserState.zoom.toString() + "%", fontSize = 15.sp, color = pal.tx, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                        Box(Modifier.size(38.dp).clip(CircleShape).background(pal.sec).tap { BrowserState.zoom = (BrowserState.zoom + 10).coerceAtMost(300) }, contentAlignment = Alignment.Center) { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.tx) }
                    }
                    MenuItem(pal, "Full screen", Icons.Outlined.Fullscreen) { menu = false; keepPlaying(tab) { BrowserState.fullscreen = true } }
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

        // X: hamesa dikhta hai. Dabane par browser poora clean (sab tabs band)
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp).size(44.dp).clip(CircleShape)
                .background(fg.copy(alpha = 0.10f)).tap { BrowserState.clear(); onClose() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Close, null, tint = fg, modifier = Modifier.size(24.dp)) }

    }

    if (showTabs) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTabs = false }) {
            Column(Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(26.dp)).background(pal.hd).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tabs", fontFamily = FontFamily.Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pal.tx)
                tabs.forEachIndexed { i, t ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (i == idx) pal.ac.copy(alpha = 0.15f) else pal.sec)
                            .border(if (i == idx) 1.5.dp else 0.dp, if (i == idx) pal.ac else Color.Transparent, RoundedCornerShape(16.dp))
                            .tap { BrowserState.active = i; showTabs = false }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(if (t.blank) "Home" else t.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = pal.tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (t.blank) "Welcome" else hostOf(t.url), fontSize = 14.sp, color = pal.sub, maxLines = 1)
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
private fun MenuItem(pal: RdPal, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 17.sp, color = pal.tx) },
        leadingIcon = { Icon(icon, null, tint = pal.sub) },
        onClick = onClick
    )
}
