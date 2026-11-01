package com.amrit.commercehub11

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

sealed class PdfResult {
    data class Ok(val file: File) : PdfResult()
    object NotAvailable : PdfResult()
    object Offline : PdfResult()
    object Failed : PdfResult()
}

/** Drive / direct PDF ko app ke andar download karke native PdfRenderer se dikhata hai. (WebView / Chrome nahi) */
object PdfStore {
    fun driveId(url: String): String? {
        Regex("/d/([A-Za-z0-9_-]{10,})").find(url)?.let { return it.groupValues[1] }
        Regex("[?&]id=([A-Za-z0-9_-]{10,})").find(url)?.let { return it.groupValues[1] }
        return null
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun cacheFile(ctx: Context, key: String): File {
        val dir = File(ctx.filesDir, "pdfs").apply { mkdirs() }
        return File(dir, "$key.pdf")
    }

    private fun looksLikePdf(f: File): Boolean {
        if (!f.exists() || f.length() < 8) return false
        return try {
            f.inputStream().use { ins ->
                val b = ByteArray(1024)
                val n = ins.read(b)
                n > 4 && String(b, 0, n, Charsets.ISO_8859_1).contains("%PDF")
            }
        } catch (_: Throwable) { false }
    }

    private suspend fun download(url: String, target: File, onProgress: (Float, Long) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Linux; Android 13) CommerceHub11").build()
            Net.http.newCall(req).await().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val body = resp.body ?: return@withContext false
                val total = body.contentLength()
                var done = 0L
                body.byteStream().use { input ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(32768)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            onProgress(if (total > 0) done.toFloat() / total.toFloat() else -1f, done)
                        }
                    }
                }
            }
            if (looksLikePdf(tmp)) {
                target.delete()
                tmp.renameTo(target)
            } else {
                tmp.delete()
                false
            }
        } catch (_: Throwable) {
            tmp.delete()
            false
        }
    }

    suspend fun fetch(ctx: Context, url: String, onProgress: (Float, Long) -> Unit = { _, _ -> }): PdfResult {
        if (url.isBlank() || url.contains("YOUR_DRIVE")) return PdfResult.NotAvailable
        val id = driveId(url)
        val key = id ?: md5(url)
        val f = cacheFile(ctx, key)
        if (withContext(Dispatchers.IO) { looksLikePdf(f) }) return PdfResult.Ok(f)
        if (!Conn.isOnline()) return PdfResult.Offline

        val candidates = if (id != null) listOf(
            "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t",
            "https://drive.google.com/uc?export=download&id=$id&confirm=t"
        ) else listOf(url)

        for (u in candidates) {
            if (download(u, f, onProgress)) return PdfResult.Ok(f)
        }
        return if (Conn.isOnline()) PdfResult.Failed else PdfResult.Offline
    }
}

private sealed class PdfState {
    object Loading : PdfState()
    data class Ready(val file: File) : PdfState()
}

/** Chapter open karne ka poora flow: loader (avatar + progress) -> in-app PDF reader */
@Composable
fun PdfOverlay(title: String, url: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var state by remember(url) { mutableStateOf<PdfState>(PdfState.Loading) }
    var prog by remember(url) { mutableFloatStateOf(-1f) }
    var bytes by remember(url) { mutableLongStateOf(0L) }

    BackHandler { onClose() }

    LaunchedEffect(url) {
        val r = PdfStore.fetch(ctx, url) { p, b -> prog = p; bytes = b }
        if (r is PdfResult.Ok) { prog = 1f; delay(250) }
        when (r) {
            is PdfResult.Ok -> state = PdfState.Ready(r.file)
            PdfResult.NotAvailable -> { Toaster.show("Ye chapter abhi available nahi hai"); onClose() }
            PdfResult.Offline -> { Toaster.show("You are offline - pehle internet se chapter kholein"); onClose() }
            PdfResult.Failed -> { Toaster.show("Chapter open nahi ho paya, dobara try karein"); onClose() }
        }
    }

    when (val s = state) {
        is PdfState.Loading -> LoaderScreen(prog, bytes)
        is PdfState.Ready -> PdfReader(s.file, title, onClose)
    }
}

@Composable
fun LoaderScreen(progress: Float, bytes: Long) {
    val pal = LocalPal.current
    val inf = rememberInfiniteTransition(label = "loader")
    val bounce by inf.animateFloat(
        initialValue = 0f, targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "bounce"
    )
    val sweep by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100)), label = "sweep"
    )
    val animated by androidx.compose.animation.core.animateFloatAsState(progress.coerceIn(0f, 1f), tween(250), label = "p")
    Column(
        modifier = Modifier.fillMaxSize().background(pal.bg).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = bounce * density }
                .size(96.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(pal.chip)
                .border(1.dp, pal.cardLine, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            RemoteImage(
                url = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                modifier = Modifier.size(64.dp).clip(CircleShape),
                scale = ContentScale.Crop, maxPx = 300
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Opening your chapter...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.text, letterSpacing = 0.4.sp)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.width(240.dp).height(8.dp).clip(RoundedCornerShape(50)).background(if (pal.dark) Slate.s800 else Slate.s200)) {
            if (progress >= 0f) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(RoundedCornerShape(50)).background(pal.accent))
            } else {
                Box(Modifier.fillMaxHeight().fillMaxWidth(0.35f).offset(x = (sweep * 240f * 1.0f - 80f).dp).clip(RoundedCornerShape(50)).background(pal.accent))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (progress >= 0f) "${(progress * 100).toInt()}%" else if (bytes > 0) String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576.0) else "Preparing...",
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.sub
        )
    }
}

@Composable
fun PdfReader(file: File, title: String, onClose: () -> Unit) {
    val pal = LocalPal.current

    val renderer = remember(file) {
        try {
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        } catch (_: Throwable) {
            null
        }
    }
    DisposableEffect(renderer) { onDispose { try { renderer?.close() } catch (_: Throwable) {} } }

    LaunchedEffect(renderer) {
        if (renderer == null) {
            file.delete() // kharab file hatao taaki agli baar dobara download ho
            Toaster.show("PDF open nahi ho paya")
            onClose()
        }
    }
    if (renderer == null) return

    Column(modifier = Modifier.fillMaxSize().background(Slate.s950)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(pal.header).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.textStrong,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.8f)
            )
            Box(
                modifier = Modifier.clip(CircleShape).background(pal.chip).tap { onClose() }.padding(8.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = pal.text, modifier = Modifier.size(20.dp))
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).background(if (pal.dark) Slate.s900 else Slate.s100)) {
            ZoomablePages(renderer)
        }
    }
}

@Composable
private fun ZoomablePages(renderer: PdfRenderer) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    val mutex = remember { Mutex() }
    val pageCount = renderer.pageCount
    val cfg = LocalConfiguration.current
    val density = LocalDensity.current
    val targetPx = (cfg.screenWidthDp * density.density * 1.6f).toInt().coerceIn(800, 2400)
    val bitmaps = remember { android.util.LruCache<Int, Bitmap>(5) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1.05f) { scale = 1f; offX = 0f; offY = 0f } else { scale = 2.2f }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            val maxX = size.width * (newScale - 1f) / 2f
                            val maxY = size.height * (newScale - 1f) / 2f
                            offX = (offX + pan.x).coerceIn(-maxX, maxX)
                            offY = (offY + pan.y).coerceIn(-maxY, maxY)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale <= 1.01f) { scale = 1f; offX = 0f; offY = 0f }
                }
            }
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offX; translationY = offY
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items((0 until pageCount).toList(), key = { it }) { index ->
                PdfPageView(renderer, index, targetPx, mutex, bitmaps)
            }
        }
    }
}

@Composable
private fun PdfPageView(renderer: PdfRenderer, index: Int, targetPx: Int, mutex: Mutex, cache: android.util.LruCache<Int, Bitmap>) {
    val bmp by produceState<ImageBitmap?>(cache.get(index)?.asImageBitmap(), index) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                mutex.withLock {
                    try {
                        val page = renderer.openPage(index)
                        try {
                            val w = targetPx
                            val h = (page.height.toFloat() / page.width.toFloat() * w).toInt().coerceAtLeast(1)
                            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            b.eraseColor(AColor.WHITE)
                            page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            cache.put(index, b)
                            b.asImageBitmap()
                        } finally {
                            page.close()
                        }
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
        }
    }
    val b = bmp
    if (b != null) {
        Image(
            bitmap = b, contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height.toFloat()).background(Color.White),
            contentScale = ContentScale.FillWidth
        )
    } else {
        Box(Modifier.fillMaxWidth().aspectRatio(1f / 1.414f).background(Color.White))
    }
}
