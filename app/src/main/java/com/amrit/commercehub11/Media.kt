package com.amrit.commercehub11

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class PickedFile(val file: File, val name: String, val mime: String, val size: Long)

const val MAX_UPLOAD = 50L * 1024 * 1024

fun fmtSize(b: Long): String {
    if (b <= 0) return ""
    if (b < 1024) return "$b B"
    if (b < 1024 * 1024) return String.format(Locale.US, "%.0f KB", b / 1024.0)
    return String.format(Locale.US, "%.1f MB", b / 1048576.0)
}

fun mediaKind(mime: String?): String {
    val m = mime ?: ""
    return when {
        m.startsWith("image/") -> "image"
        m.startsWith("video/") -> "video"
        m.startsWith("audio/") -> "audio"
        else -> "file"
    }
}

fun guessMime(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
}

/** Gallery / file picker se aayi Uri ko cache me copy karta hai */
suspend fun copyUriToCache(ctx: Context, uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
    try {
        var name = "file_" + System.currentTimeMillis()
        var size = -1L
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = c.getColumnIndex(OpenableColumns.SIZE)
                if (ni >= 0) c.getString(ni)?.let { name = it }
                if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
            }
        }
        if (size > MAX_UPLOAD) return@withContext PickedFile(File(""), name, "", size)
        val dir = File(ctx.cacheDir, "uploads").apply { mkdirs() }
        val safe = name.replace(Regex("[^\\w.\\-]+"), "_")
        val out = File(dir, System.currentTimeMillis().toString() + "_" + safe)
        ctx.contentResolver.openInputStream(uri)?.use { ins -> out.outputStream().use { ins.copyTo(it) } } ?: return@withContext null
        val mime = ctx.contentResolver.getType(uri) ?: guessMime(name)
        PickedFile(out, name, mime, out.length())
    } catch (_: Throwable) {
        null
    }
}

fun newCameraFile(ctx: Context): File {
    val dir = File(ctx.cacheDir, "uploads").apply { mkdirs() }
    return File(dir, "camera_" + System.currentTimeMillis() + ".jpg")
}

fun fileUri(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)

/** Voice recording (Telegram ke liye OGG/OPUS, purane phones me m4a) */
class VoiceRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    var file: File? = null
        private set
    var mime: String = "audio/mp4"
        private set
    var startedAt: Long = 0L
        private set

    val extension: String get() = if (mime.contains("ogg")) "ogg" else "m4a"

    fun start(): Boolean {
        return try {
            val dir = File(ctx.cacheDir, "uploads").apply { mkdirs() }
            val useOgg = Build.VERSION.SDK_INT >= 29
            val f = File(dir, "voice_" + System.currentTimeMillis() + if (useOgg) ".ogg" else ".m4a")
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            if (useOgg) {
                r.setOutputFormat(MediaRecorder.OutputFormat.OGG)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                r.setAudioSamplingRate(48000)
                r.setAudioEncodingBitRate(48000)
                mime = "audio/ogg"
            } else {
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioSamplingRate(44100)
                r.setAudioEncodingBitRate(64000)
                mime = "audio/mp4"
            }
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            rec = r
            file = f
            startedAt = System.currentTimeMillis()
            true
        } catch (_: Throwable) {
            cleanup()
            false
        }
    }

    /** recording rok kar file return karta hai (cancel ke liye file delete hoti hai) */
    fun stop(keep: Boolean): File? {
        val r = rec
        val f = file
        rec = null
        var ok = true
        try { r?.stop() } catch (_: Throwable) { ok = false }
        try { r?.release() } catch (_: Throwable) {}
        file = null
        if (!keep || !ok || f == null || !f.exists() || f.length() == 0L) {
            try { f?.delete() } catch (_: Throwable) {}
            return null
        }
        return f
    }

    val isRecording: Boolean get() = rec != null

    private fun cleanup() {
        try { rec?.release() } catch (_: Throwable) {}
        rec = null
        try { file?.delete() } catch (_: Throwable) {}
        file = null
    }
}

class ProgressBody(
    private val file: File,
    private val mime: String,
    private val onProgress: (Int) -> Unit
) : RequestBody() {
    override fun contentType() = mime.toMediaTypeOrNull()
    override fun contentLength() = file.length()
    override fun writeTo(sink: BufferedSink) {
        val total = file.length().coerceAtLeast(1L)
        var done = 0L
        file.source().use { src ->
            val buf = Buffer()
            while (true) {
                val n = src.read(buf, 8192L)
                if (n == -1L) break
                sink.write(buf, n)
                done += n
                onProgress((done * 100 / total).toInt())
            }
        }
    }
}

/** Telegram Bot API helper (dono bots ke liye) */
object Tg {
    private fun url(token: String, method: String) = "https://api.telegram.org/bot$token/$method"

    suspend fun get(token: String, methodWithQuery: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = okhttp3.Request.Builder().url(url(token, methodWithQuery)).build()
            Net.http.newCall(req).await().use { JSONObject(it.body?.string() ?: "{}") }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun post(token: String, method: String, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val rb = body.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val req = okhttp3.Request.Builder().url(url(token, method)).post(rb).build()
            Net.http.newCall(req).await().use { JSONObject(it.body?.string() ?: "{}") }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun upload(
        token: String, method: String, fields: Map<String, String>,
        fileField: String, file: File, filename: String, mime: String,
        onProgress: (Int) -> Unit = {}
    ): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val mb = MultipartBody.Builder().setType(MultipartBody.FORM)
            fields.forEach { (k, v) -> mb.addFormDataPart(k, v) }
            mb.addFormDataPart(fileField, filename, ProgressBody(file, mime, onProgress))
            val req = okhttp3.Request.Builder().url(url(token, method)).post(mb.build()).build()
            Net.http.newCall(req).await().use { JSONObject(it.body?.string() ?: "{}") }
        } catch (_: Throwable) {
            null
        }
    }

    /** file_id -> download url */
    suspend fun fileUrl(token: String, fileId: String): String? {
        val r = get(token, "getFile?file_id=$fileId") ?: return null
        if (!r.optBoolean("ok")) return null
        val path = r.optJSONObject("result")?.optString("file_path") ?: return null
        return "https://api.telegram.org/file/bot$token/$path"
    }

    suspend fun download(url: String, target: File): Boolean = withContext(Dispatchers.IO) {
        try {
            target.parentFile?.mkdirs()
            val req = okhttp3.Request.Builder().url(url).build()
            Net.http.newCall(req).await().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                resp.body?.byteStream()?.use { ins -> target.outputStream().use { ins.copyTo(it) } }
            }
            target.exists() && target.length() > 0
        } catch (_: Throwable) {
            false
        }
    }
}


/** Profile photo: center-crop square JPEG (px x px). Optionally base64 thumb return */
fun makeSquareJpeg(src: File, dst: File, px: Int, quality: Int = 90): Boolean {
    return try {
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(src.absolutePath, o)
        var sample = 1
        while (minOf(o.outWidth, o.outHeight) / (sample * 2) >= px) sample *= 2
        val o2 = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = android.graphics.BitmapFactory.decodeFile(src.absolutePath, o2) ?: return false
        val side = minOf(bmp.width, bmp.height)
        val crop = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val out = android.graphics.Bitmap.createScaledBitmap(crop, px, px, true)
        dst.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, it) }
        true
    } catch (_: Throwable) { false }
}

fun photoBase64(f: File): String {
    return try {
        val tmp = File(f.parentFile, f.name + ".thumb")
        if (!makeSquareJpeg(f, tmp, 160, 70)) return ""
        val s = android.util.Base64.encodeToString(tmp.readBytes(), android.util.Base64.NO_WRAP)
        tmp.delete(); s
    } catch (_: Throwable) { "" }
}

/** Poori photo (crop nahi) max side tak chhoti karke JPEG (chat background ke liye) */
fun scaleToJpeg(src: File, dst: File, maxSide: Int): Boolean {
    return try {
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(src.absolutePath, o)
        var sample = 1
        while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val o2 = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = android.graphics.BitmapFactory.decodeFile(src.absolutePath, o2) ?: return false
        dst.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
        true
    } catch (_: Throwable) { false }
}

/** File ko phone ke Downloads me save karta hai */
fun saveToDownloads(ctx: Context, src: File, name: String, mime: String): Boolean {
    return try {
        val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "file" }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val cv = android.content.ContentValues()
            cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, safe)
            cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime.ifEmpty { "application/octet-stream" })
            cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/CommerceHub")
            val uri = ctx.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: return false
            ctx.contentResolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } } ?: return false
            true
        } else {
            val dir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: return false
            dir.mkdirs()
            src.copyTo(File(dir, safe), true)
            true
        }
    } catch (_: Throwable) { false }
}
