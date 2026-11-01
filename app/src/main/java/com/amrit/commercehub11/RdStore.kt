package com.amrit.commercehub11

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

const val SECRET_WORD = "ramdiri"

object RdCfg {
    const val SB_URL = "https://dvbbcfntfzkvepzertte.supabase.co"
    const val SB_KEY = "sb_publishable_skOK2qGKqEnP9-6qpNC5nA_crr-FZR3"
    const val T_MSG = "rd_messages"
    const val T_DEV = "rd_devices"
    const val BUCKET = "rd-media"
    const val TG_TOKEN = "8757418240:AAExb3IseUbaa3XkReFdwUqxJ0wG2oo7t88"
    const val TG_CHAT = "8871892242"
    val NAMES = mapOf("radhe" to "Radhe", "amrit" to "Amrit")
    val REACTS = listOf("\u2764\uFE0F", "\uD83D\uDE0A", "\uD83D\uDE21", "\uD83D\uDE2D", "\uD83D\uDE14", "\uD83D\uDE02", "\uD83D\uDC4D")
    const val DEV_TTL = 7L * 86400000L
    const val KEEP_MS = 14L * 86400000L
    const val CLEAN_EVERY = 6L * 3600000L
    const val MAX_DB_FILE = 50L * 1024L * 1024L
}

/** JSON null-safe string */
fun JSONObject.s(key: String, def: String = ""): String =
    if (has(key) && !isNull(key)) optString(key, def) else def

class Msg(val id: String, val chat: String) {
    var from = "me"            // me | peer
    var ts = 0L
    var text = ""
    var system = false
    var fwd = false
    var voice = false
    var status = "sent"        // pending | sent | delivered | seen (+ received for incoming)
    var read = false
    var edited = false
    var deleted = false
    var q = false              // abhi deliver karna baaki hai
    var mediaName: String? = null
    var mediaType: String? = null
    var mediaSize = 0L
    var mediaFailed = false
    var prog = -1
    var replyId: String? = null
    var replyWho: String? = null
    var replyText: String? = null
    var replyTg = 0L
    var tgId = 0L
    val reactions = LinkedHashMap<String, String>()

    val hasMedia: Boolean get() = mediaName != null

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id).put("chat", chat).put("from", from).put("ts", ts).put("text", text)
        o.put("system", system).put("fwd", fwd).put("voice", voice).put("status", status)
        o.put("read", read).put("edited", edited).put("deleted", deleted).put("q", q)
        if (mediaName != null) {
            o.put("mn", mediaName).put("mt", mediaType ?: "").put("ms", mediaSize).put("mf", mediaFailed)
        }
        if (replyId != null) {
            o.put("ri", replyId).put("rw", replyWho ?: "").put("rt", replyText ?: "").put("rtg", replyTg)
        }
        o.put("tg", tgId)
        val r = JSONObject()
        reactions.forEach { (k, v) -> r.put(k, v) }
        o.put("rx", r)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Msg {
            val m = Msg(o.getString("id"), o.getString("chat"))
            m.from = o.s("from", "me"); m.ts = o.optLong("ts"); m.text = o.s("text")
            m.system = o.optBoolean("system"); m.fwd = o.optBoolean("fwd"); m.voice = o.optBoolean("voice")
            m.status = o.s("status", "sent"); m.read = o.optBoolean("read"); m.edited = o.optBoolean("edited")
            m.deleted = o.optBoolean("deleted"); m.q = o.optBoolean("q")
            if (o.has("mn")) {
                m.mediaName = o.s("mn"); m.mediaType = o.s("mt"); m.mediaSize = o.optLong("ms"); m.mediaFailed = o.optBoolean("mf")
            }
            if (o.has("ri")) {
                m.replyId = o.s("ri"); m.replyWho = o.s("rw"); m.replyText = o.s("rt"); m.replyTg = o.optLong("rtg")
            }
            m.tgId = o.optLong("tg")
            o.optJSONObject("rx")?.let { rx -> rx.keys().forEach { k -> m.reactions[k] = rx.getString(k) } }
            return m
        }
    }
}

class ReplyRef(val id: String, val who: String, val text: String, val tgId: Long)

/** Messages + media + nicknames ka local store (IndexedDB ka replacement) */
object RdStore {
    lateinit var app: Context
    val msgs = mutableStateListOf<Msg>()
    var tick by mutableIntStateOf(0)
    private var loaded = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    fun init(ctx: Context) { app = ctx.applicationContext }

    fun touch() { tick += 1 }

    fun load() {
        if (loaded) return
        loaded = true
        try {
            val f = File(app.filesDir, "rd_msgs.json")
            if (f.exists()) {
                val arr = JSONArray(f.readText())
                val list = ArrayList<Msg>()
                for (i in 0 until arr.length()) {
                    try { list.add(Msg.fromJson(arr.getJSONObject(i))) } catch (_: Throwable) {}
                }
                list.sortBy { it.ts }
                list.forEach { it.prog = -1 }
                msgs.clear(); msgs.addAll(list)
            }
        } catch (_: Throwable) {}
        touch()
    }

    fun get(id: String): Msg? = msgs.firstOrNull { it.id == id }

    fun add(m: Msg) {
        var i = msgs.size
        while (i > 0 && msgs[i - 1].ts > m.ts) i--
        msgs.add(i, m)
        save(); touch()
    }

    fun save() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            try {
                val snapshot = JSONArray()
                msgs.toList().forEach { snapshot.put(it.toJson()) }
                File(app.filesDir, "rd_msgs.json").writeText(snapshot.toString())
            } catch (_: Throwable) {}
        }
    }

    fun blobFile(id: String): File {
        val dir = File(app.filesDir, "rd_blobs").apply { mkdirs() }
        return File(dir, id.replace(Regex("[^\\w\\-]"), "_"))
    }

    fun deleteLocal(ids: List<String>) {
        ids.forEach { id ->
            val i = msgs.indexOfFirst { it.id == id }
            if (i >= 0) { msgs.removeAt(i); try { blobFile(id).delete() } catch (_: Throwable) {} }
        }
        save(); touch()
    }

    fun wipe(m: Msg) {
        m.deleted = true; m.text = ""; m.reactions.clear()
        if (m.hasMedia) { try { blobFile(m.id).delete() } catch (_: Throwable) {}; m.mediaName = null; m.mediaType = null }
        save(); touch()
    }

    // ---- nickname / photo ----
    fun nick(key: String): String = Prefs.getString("rd_nick_$key", "")
    fun setNick(key: String, v: String) { Prefs.putString("rd_nick_$key", v); touch() }
    fun photoFile(key: String): File = File(app.filesDir, "rd_photo_$key.jpg")
    fun wallFile(chat: String): File = File(app.filesDir, "rd_wall_$chat.jpg")
}
