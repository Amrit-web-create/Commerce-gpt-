package com.amrit.commercehub11

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Net {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (cont.isCancelled) {
                response.close()
            } else {
                cont.resume(response)
            }
        }
    })
    cont.invokeOnCancellation { try { cancel() } catch (_: Throwable) {} }
}

/** Internet on/off ka live state (offline / back online banner isi se chalta hai) */
object Conn {
    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online
    private var started = false

    fun isOnline(): Boolean = _online.value

    fun start(ctx: Context) {
        if (started) return
        started = true
        val cm = ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        _online.value = currentState(cm)
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        _online.value = true
                    }
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }

                override fun onLost(network: Network) {
                    _online.value = false
                }
            })
        } catch (_: Throwable) {
        }
    }

    private fun currentState(cm: ConnectivityManager): Boolean {
        return try {
            val n = cm.activeNetwork ?: return false
            val c = cm.getNetworkCapabilities(n) ?: return false
            c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Throwable) {
            true
        }
    }
}

/** Chhota toast (snackbar jaisa) */
object Toaster {
    val message = MutableStateFlow<Pair<Long, String>?>(null)
    fun show(text: String) {
        message.value = Pair(System.nanoTime(), text)
    }
}

object Images {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private fun decode(open: () -> java.io.InputStream?, maxPx: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        val longest = maxOf(opts.outWidth, opts.outHeight)
        while (longest / (sample * 2) >= maxPx) sample *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
        return open()?.use { BitmapFactory.decodeStream(it, null, o2) }
    }

    suspend fun asset(ctx: Context, name: String, maxPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "asset:$name:$maxPx"
        cache.get(key)?.let { return@withContext it }
        try {
            val bmp = decode({ ctx.assets.open(name) }, maxPx)
            if (bmp != null) cache.put(key, bmp)
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun file(f: File, maxPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "file:${f.absolutePath}:${f.lastModified()}:$maxPx"
        cache.get(key)?.let { return@withContext it }
        try {
            val bmp = decode({ if (f.exists()) f.inputStream() else null }, maxPx)
            if (bmp != null) cache.put(key, bmp)
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Remote image: disk cache => offline me bhi dikhti hai */
    suspend fun remote(ctx: Context, url: String, maxPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "url:$url:$maxPx"
        cache.get(key)?.let { return@withContext it }
        try {
            val dir = File(ctx.cacheDir, "img").apply { mkdirs() }
            val f = File(dir, md5(url))
            if (!f.exists() || f.length() == 0L) {
                val req = Request.Builder().url(url).build()
                Net.http.newCall(req).await().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val tmp = File(dir, md5(url) + ".tmp")
                    resp.body?.byteStream()?.use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
                    tmp.renameTo(f)
                }
            }
            val bmp = decode({ f.inputStream() }, maxPx)
            if (bmp != null) cache.put(key, bmp)
            bmp
        } catch (_: Throwable) {
            null
        }
    }
}

@Composable
fun AssetImage(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, maxPx: Int = 700) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, name) {
        value = Images.asset(ctx, name, maxPx)?.asImageBitmap()
    }
    bmp?.let { Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = scale) }
}

@Composable
fun FileImage(file: File, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, maxPx: Int = 900) {
    val bmp by produceState<ImageBitmap?>(null, file.absolutePath, file.lastModified()) {
        value = Images.file(file, maxPx)?.asImageBitmap()
    }
    bmp?.let { Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = scale) }
}

@Composable
fun RemoteImage(url: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop, maxPx: Int = 600) {
    val ctx = LocalContext.current
    val online by Conn.online.collectAsState()
    val bmp by produceState<ImageBitmap?>(null, url, online) {
        if (value == null) value = Images.remote(ctx, url, maxPx)?.asImageBitmap()
    }
    bmp?.let { Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = scale) }
}
