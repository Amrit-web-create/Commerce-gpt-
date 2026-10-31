package com.amrit.commercehub11

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

class BTab(val view: WebView) {
    var title by mutableStateOf("New tab")
    var url by mutableStateOf("")
    var progress by mutableIntStateOf(0)
}

fun resolveQuery(q: String): String {
    val t = q.trim()
    if (t.startsWith("http://") || t.startsWith("https://")) return t
    if (!t.contains(" ") && t.contains(".")) return "https://$t"
    return "https://www.google.com/search?q=" + URLEncoder.encode(t, "UTF-8")
}

@SuppressLint("SetJavaScriptEnabled")
private fun makeTab(ctx: Context, onUpdate: () -> Unit): BTab {
    val wv = WebView(ctx)
    val tab = BTab(wv)
    wv.settings.javaScriptEnabled = true
    wv.settings.domStorageEnabled = true
    wv.settings.setSupportMultipleWindows(false)
    wv.settings.javaScriptCanOpenWindowsAutomatically = false
    wv.webViewClient = object : WebViewClient() {
        // koi bhi link isi WebView me khulega
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val u = request.url.toString()
            return !(u.startsWith("http://") || u.startsWith("https://"))
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { tab.url = url; onUpdate() }
        override fun onPageFinished(view: WebView, url: String) { tab.url = url; tab.title = view.title ?: url; onUpdate() }
        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                view.loadData("<html><body style='font-family:sans-serif;text-align:center;padding:60px 20px;color:#555'><h3>You are offline</h3><p>Internet connect karke refresh karein.</p></body></html>", "text/html", "utf-8")
            }
        }
    }
    wv.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) { tab.progress = newProgress }
        override fun onReceivedTitle(view: WebView, title: String?) { if (!title.isNullOrEmpty()) tab.title = title }
    }
    wv.loadUrl("https://www.google.com")
    return tab
}

@Composable
fun BrowserScreen(pal: RdPal, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val tabs = remember { mutableListOf(makeTab(ctx) { }) }
    var active by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val tab = tabs[active.coerceIn(0, tabs.size - 1)]
    val v = version // recompose trigger

    DisposableEffect(Unit) { onDispose { tabs.forEach { try { it.view.destroy() } catch (_: Throwable) {} } } }

    BackHandler {
        if (tab.view.canGoBack()) tab.view.goBack()
        else if (tabs.size > 1) { tabs.removeAt(active).view.destroy(); active = 0; version++ }
        else onClose()
    }

    Column(Modifier.fillMaxSize().background(pal.bg)) {
        Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(50)).tap { onClose() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, null, tint = pal.tx, modifier = Modifier.size(22.dp))
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(pal.inp).padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (input.isEmpty()) Text(if (v >= 0 && tab.url.isNotEmpty()) tab.url else "Search Google or type URL", fontSize = 13.sp, color = pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicTextField(
                    value = input, onValueChange = { input = it }, singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = pal.tx), cursorBrush = SolidColor(pal.tx),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (input.isNotBlank()) { tab.view.loadUrl(resolveQuery(input)); input = "" } }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(50)).tap { menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreVert, null, tint = pal.tx, modifier = Modifier.size(22.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Refresh") }, onClick = { menu = false; tab.view.reload() })
                    DropdownMenuItem(text = { Text("Copy link") }, onClick = {
                        menu = false
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("link", tab.view.url ?: "")); Toaster.show("Link copied")
                    })
                    DropdownMenuItem(text = { Text("Share via...") }, onClick = {
                        menu = false
                        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, tab.view.url ?: "")
                        try { ctx.startActivity(Intent.createChooser(i, "Share link")) } catch (_: Throwable) {}
                    })
                    if (tabs.size < 2) DropdownMenuItem(text = { Text("New tab") }, onClick = {
                        menu = false; tabs.add(makeTab(ctx) { }); active = tabs.size - 1; version++
                    })
                    DropdownMenuItem(text = { Text("Clear cookies & data") }, onClick = {
                        menu = false
                        try {
                            CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush()
                            WebStorage.getInstance().deleteAllData()
                            tabs.forEach { it.view.clearCache(true); it.view.clearHistory() }
                            Toaster.show("Cookies & cache cleared")
                        } catch (_: Throwable) {}
                    })
                }
            }
        }
        // max 2 tabs
        Row(Modifier.fillMaxWidth().background(pal.hd).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { i, t ->
                Text(
                    t.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (i == active) pal.ac else pal.sub,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(if (i == active) pal.inp else pal.hd).tap { active = i; version++ }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(pal.line)) {
            if (tab.progress in 1..99) Box(Modifier.fillMaxWidth(tab.progress / 100f).height(2.dp).background(pal.ac))
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 84.dp)) {
            androidx.compose.runtime.key(active) {
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { tab.view })
            }
        }
    }
}
