package com.amrit.commercehub11

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class BannerData(val chat: String, val title: String, val text: String, val stamp: Long)

/** Supabase realtime (Phoenix websocket) - broadcast + presence */
class RdRealtime(
    private val me: String, private val dev: String,
    private val onEvent: (String, JSONObject) -> Unit,
    private val onPresence: (Set<String>) -> Unit,
    private val onReady: (Boolean) -> Unit
) {
    private val topic = "realtime:rd-room"
    private var ws: WebSocket? = null
    private var ref = 0
    private var closed = false
    private val keys = HashSet<String>()
    private var hb: Job? = null
    @Volatile var ready = false
    private val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = Net.http.newBuilder().readTimeout(0, TimeUnit.SECONDS).pingInterval(20, TimeUnit.SECONDS).build()

    fun connect() {
        if (closed) return
        val url = RdCfg.SB_URL.replace("https://", "wss://") + "/realtime/v1/websocket?apikey=" + RdCfg.SB_KEY + "&vsn=1.0.0"
        ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { join() }
            override fun onMessage(webSocket: WebSocket, text: String) { handle(text) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { dropped() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { dropped() }
        })
    }

    private fun raw(t: String, e: String, p: JSONObject) {
        try {
            ws?.send(JSONObject().put("topic", t).put("event", e).put("payload", p).put("ref", (++ref).toString()).toString())
        } catch (_: Throwable) {}
    }

    private fun join() {
        val cfg = JSONObject()
            .put("broadcast", JSONObject().put("self", false).put("ack", false))
            .put("presence", JSONObject().put("key", "$me|$dev"))
            .put("postgres_changes", JSONArray())
        raw(topic, "phx_join", JSONObject().put("config", cfg).put("access_token", RdCfg.SB_KEY))
        hb?.cancel()
        hb = sc.launch { while (isActive) { delay(25000); raw("phoenix", "heartbeat", JSONObject()) } }
    }

    private fun handle(text: String) {
        try {
            val o = JSONObject(text)
            val p = o.optJSONObject("payload") ?: JSONObject()
            when (o.optString("event")) {
                "phx_reply" -> if (o.optString("topic") == topic && p.optString("status") == "ok" && !ready) {
                    ready = true
                    raw(topic, "presence", JSONObject().put("type", "presence").put("event", "track")
                        .put("payload", JSONObject().put("at", System.currentTimeMillis()).put("u", me).put("d", dev)))
                    onReady(true)
                }
                "broadcast" -> {
                    val inner = p.optJSONObject("payload")
                    if (inner != null) onEvent(p.optString("event"), inner)
                }
                "presence_state" -> {
                    keys.clear(); p.keys().forEach { keys.add(it) }; onPresence(HashSet(keys))
                }
                "presence_diff" -> {
                    p.optJSONObject("joins")?.keys()?.forEach { keys.add(it) }
                    p.optJSONObject("leaves")?.keys()?.forEach { keys.remove(it) }
                    onPresence(HashSet(keys))
                }
            }
        } catch (_: Throwable) {}
    }

    private fun dropped() {
        ready = false; keys.clear(); hb?.cancel()
        onReady(false); onPresence(emptySet())
        if (!closed) sc.launch { delay(3000); connect() }
    }

    fun send(event: String, payload: JSONObject): Boolean {
        if (!ready) return false
        raw(topic, "broadcast", JSONObject().put("type", "broadcast").put("event", event).put("payload", payload))
        return true
    }

    fun close() {
        closed = true; ready = false; hb?.cancel()
        try { ws?.close(1000, "bye") } catch (_: Throwable) {}
        sc.coroutineContext[Job]?.cancel()
    }
}

object RdNet {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val JSON = "application/json".toMediaType()
    private val RANK = mapOf("pending" to 0, "sent" to 1, "delivered" to 2, "seen" to 3)

    var ME = ""
    var PEER = ""
    private var DEV = ""
    var running = false
    var foreground = true
    var uiOpen = false
    var uiMode = "inbox"          // inbox | chat | browser
    var curChat: String? = null
    var peerOnline by mutableStateOf(false)
    var peerLastSeen by mutableLongStateOf(0L)
    var peerTyping by mutableStateOf(false)
    var banner by mutableStateOf<BannerData?>(null)

    private var rt: RdRealtime? = null
    private var peerDevs = setOf<String>()
    private var selfDevs = setOf<String>()
    private val devReg = mapOf("radhe" to HashMap<String, Long>(), "amrit" to HashMap<String, Long>())
    private val seenEvt = LinkedHashSet<String>()
    private val outbox = ArrayList<Pair<JSONObject, String>>() // evt, to(peer|self)
    private var fetching = false
    private var typingJob: Job? = null
    private var loop: Job? = null
    private val busy = HashSet<String>()
    private val mediaLock = Mutex()
    private var tgBusy = false

    fun uid(): String = UUID.randomUUID().toString()

    val muted: Boolean get() = Prefs.getBool("rd_muted", false)

    /** Call signaling (offer/answer/ice/end) - sirf live peer ko */
    fun signal(evt: JSONObject) {
        if (!evt.has("id")) evt.put("id", uid())
        scope.launch { route(evt, "peer", true) }
    }

    fun logCall(text: String, incoming: Boolean) {
        val m = Msg(uid(), "p2p"); m.system = true; m.text = text; m.ts = System.currentTimeMillis()
        m.from = if (incoming) "peer" else "me"; m.read = true; m.status = "sent"
        RdStore.add(m)
    }

    fun sendNick(n: String) { queueEvt(JSONObject().put("t", "nick").put("id", uid()).put("nick", n)) }
    fun sendPhoto(b64: String) { queueEvt(JSONObject().put("t", "prof").put("id", uid()).put("photo", b64)) }

    private fun saveBase64Photo(b64: String, f: File) {
        try {
            if (b64.isEmpty()) { f.delete(); return }
            f.writeBytes(android.util.Base64.decode(b64, android.util.Base64.DEFAULT))
        } catch (_: Throwable) {}
    }

    /** Server par jo is user ke messages bache hain wo delete */
    suspend fun cleanServer(): Boolean {
        if (!Conn.isOnline() || ME.isEmpty()) return false
        val url = (RdCfg.SB_URL + "/rest/v1/" + RdCfg.T_MSG).toHttpUrl().newBuilder()
            .addQueryParameter("or", "(from_user.eq.$ME,to_user.eq.$ME)").build()
        val (c, _) = exec(Request.Builder().url(url).header("apikey", RdCfg.SB_KEY).header("Authorization", "Bearer " + RdCfg.SB_KEY).delete().build())
        return c in 200..299
    }
    private fun chanReady() = rt?.ready == true

    // ---------------------------------------------------------------- lifecycle
    fun start(me: String) {
        if (running && ME == me) return
        stop()
        ME = me; PEER = if (me == "radhe") "amrit" else "radhe"
        var d = Prefs.getString("rd_dev", "")
        if (d.isEmpty()) { d = "d" + UUID.randomUUID().toString().replace("-", "").take(10); Prefs.putString("rd_dev", d) }
        DEV = d
        running = true
        RdStore.load()
        rt = RdRealtime(me, DEV, { ev, p -> scope.launch { onBroadcast(ev, p) } }, { k -> scope.launch { onPresence(k) } }, { ok ->
            if (ok) scope.launch { fetchPending() }
        })
        rt?.connect()
        loop = scope.launch {
            touchLastSeen(); loadDevices(); fetchPending(); flushAll(); maybeClean()
            var n = 0
            while (isActive) {
                delay(3500)
                tgPoll()
                n++
                if (n % 3 == 0) fetchPending()
                if (n % 2 == 0) syncStatus()
                if (n % 13 == 0) { touchLastSeen(); maybeClean() }
            }
        }
    }

    fun stop() {
        running = false
        loop?.cancel(); loop = null
        rt?.close(); rt = null
        peerOnline = false; peerTyping = false
    }

    // ---------------------------------------------------------------- http helpers
    private fun sb(path: String) = Request.Builder().url(RdCfg.SB_URL + path)
        .header("apikey", RdCfg.SB_KEY).header("Authorization", "Bearer " + RdCfg.SB_KEY)

    private suspend fun exec(req: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            Net.http.newCall(req).await().use { Pair(it.code, it.body?.string() ?: "") }
        } catch (_: Throwable) { Pair(-1, "") }
    }

    private fun iso(t: Long): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date(t))
    }

    private fun parseIso(s: String): Long {
        return try {
            var x = s.replace("Z", "+00:00")
            x = x.replace(Regex("\\.(\\d{3})\\d*"), ".$1")
            val hasFrac = x.contains(".")
            val f = SimpleDateFormat(if (hasFrac) "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" else "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            f.parse(x)?.time ?: 0L
        } catch (_: Throwable) { 0L }
    }

    // ---------------------------------------------------------------- presence / devices
    private fun onPresence(keys: Set<String>) {
        val pd = HashSet<String>(); val sd = HashSet<String>()
        keys.forEach { k ->
            val i = k.indexOf('|'); if (i < 0) return@forEach
            val u = k.substring(0, i); val d = k.substring(i + 1)
            if (u == PEER) pd.add(d) else if (u == ME && d != DEV) sd.add(d)
        }
        val was = peerOnline
        peerDevs = pd; selfDevs = sd; peerOnline = pd.isNotEmpty()
        val now = System.currentTimeMillis()
        pd.forEach { devReg[PEER]?.put(it, now) }; sd.forEach { devReg[ME]?.put(it, now) }
        if (peerOnline && !was) scope.launch { fetchPending(); flushAll() }
        if (!peerOnline && was) { peerLastSeen = now; scope.launch { delay(1500); loadDevices() } }
        RdStore.touch()
    }

    private fun devsOf(which: String): Set<String> {
        val u = if (which == "self") ME else PEER
        val now = System.currentTimeMillis()
        val set = HashSet<String>()
        devReg[u]?.forEach { (d, t) -> if (now - t < RdCfg.DEV_TTL) set.add(d) }
        set.addAll(if (which == "self") selfDevs else peerDevs)
        set.remove(DEV)
        return set
    }

    private suspend fun touchLastSeen() {
        if (!Conn.isOnline() || ME.isEmpty()) return
        val body = JSONObject().put("id", "$ME|$DEV").put("user_id", ME).put("dev_id", DEV).put("last_seen", iso(System.currentTimeMillis()))
        exec(sb("/rest/v1/${RdCfg.T_DEV}").header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toString().toRequestBody(JSON)).build())
    }

    private suspend fun loadDevices() {
        if (!Conn.isOnline() || PEER.isEmpty()) return
        val url = (RdCfg.SB_URL + "/rest/v1/" + RdCfg.T_DEV).toHttpUrl().newBuilder()
            .addQueryParameter("select", "user_id,dev_id,last_seen")
            .addQueryParameter("last_seen", "gte." + iso(System.currentTimeMillis() - RdCfg.DEV_TTL)).build()
        val (code, body) = exec(Request.Builder().url(url).header("apikey", RdCfg.SB_KEY).header("Authorization", "Bearer " + RdCfg.SB_KEY).build())
        if (code !in 200..299) return
        try {
            val arr = JSONArray(body); var ls = 0L
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i); val t = parseIso(r.s("last_seen")); val u = r.s("user_id")
                devReg[u]?.let { m -> m[r.s("dev_id")] = maxOf(m[r.s("dev_id")] ?: 0L, t) }
                if (u == PEER) ls = maxOf(ls, t)
            }
            if (ls > 0 && !peerOnline) peerLastSeen = ls
            RdStore.touch()
        } catch (_: Throwable) {}
    }

    private suspend fun maybeClean() {
        if (!Conn.isOnline() || ME.isEmpty()) return
        val last = Prefs.getLong("rd_clean_ts", 0L)
        if (System.currentTimeMillis() - last < RdCfg.CLEAN_EVERY) return
        Prefs.putLong("rd_clean_ts", System.currentTimeMillis())
        fetchPending()
        val cut = iso(System.currentTimeMillis() - RdCfg.KEEP_MS)
        val url = (RdCfg.SB_URL + "/rest/v1/" + RdCfg.T_MSG).toHttpUrl().newBuilder()
            .addQueryParameter("select", "id,payload").addQueryParameter("created_at", "lt.$cut").addQueryParameter("limit", "500").build()
        val (code, body) = exec(Request.Builder().url(url).header("apikey", RdCfg.SB_KEY).header("Authorization", "Bearer " + RdCfg.SB_KEY).build())
        if (code !in 200..299) return
        try {
            val arr = JSONArray(body)
            if (arr.length() == 0) return
            val ids = ArrayList<String>(); val paths = JSONArray()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i); ids.add(r.getString("id"))
                val p = r.optJSONObject("payload")?.optJSONObject("media")?.s("path") ?: ""
                if (p.isNotEmpty()) paths.put(p)
            }
            val del = (RdCfg.SB_URL + "/rest/v1/" + RdCfg.T_MSG).toHttpUrl().newBuilder()
                .addQueryParameter("id", "in.(" + ids.joinToString(",") { "\"$it\"" } + ")").build()
            exec(Request.Builder().url(del).header("apikey", RdCfg.SB_KEY).header("Authorization", "Bearer " + RdCfg.SB_KEY).delete().build())
            if (paths.length() > 0) {
                exec(sb("/storage/v1/object/${RdCfg.BUCKET}").delete(JSONObject().put("prefixes", paths).toString().toRequestBody(JSON)).build())
            }
        } catch (_: Throwable) {}
    }

    // ---------------------------------------------------------------- routing
    private fun bsend(ev: String, p: JSONObject) { rt?.send(ev, p) }

    private suspend fun insertRows(e: JSONObject, user: String, devs: List<String>): Boolean {
        if (!Conn.isOnline() || devs.isEmpty()) return false
        val arr = JSONArray()
        devs.forEach { d ->
            arr.put(JSONObject().put("id", e.optString("id") + "~" + d).put("from_user", ME).put("to_user", user).put("to_dev", d).put("payload", e))
        }
        fun req(b: String) = sb("/rest/v1/${RdCfg.T_MSG}").header("Prefer", "return=minimal").post(b.toRequestBody(JSON)).build()
        var ok = exec(req(arr.toString())).first in 200..299
        if (!ok) {
            var any = false
            for (i in 0 until arr.length()) { val c = exec(req(arr.getJSONObject(i).toString())).first; if (c in 200..299 || c == 409) any = true }
            ok = any
        }
        if (ok) {
            val online = if (user == PEER) peerDevs else selfDevs
            val live = devs.filter { online.contains(it) }
            if (live.isNotEmpty()) bsend("poke", JSONObject().put("to", JSONArray(live)))
        }
        return ok
    }

    private suspend fun route(evt: JSONObject, to: String = "peer", ephemeral: Boolean = false, both: Boolean = false): Boolean {
        val which = if (to == "self") "self" else "peer"
        val u = if (which == "self") ME else PEER
        val online = if (which == "self") selfDevs else peerDevs
        val devs = devsOf(which)
        val e = JSONObject(evt.toString()).put("fu", ME).put("fd", DEV)
        if (which == "self") e.put("mirror", true)
        val hasMedia = evt.has("media") && !evt.isNull("media")
        var ok = false
        val bc = ArrayList<String>(); val dbd = ArrayList<String>()
        devs.forEach { d ->
            val live = online.contains(d) && chanReady()
            if (live && !hasMedia) bc.add(d)
            if (!ephemeral && (!live || hasMedia || both)) dbd.add(d)
        }
        if (bc.isNotEmpty()) { bsend("evt", JSONObject(e.toString()).put("to", JSONArray(bc))); ok = true }
        if (dbd.isNotEmpty() && insertRows(e, u, dbd)) ok = true
        if (devs.isEmpty() && !ephemeral && which == "peer" && insertRows(e, u, listOf("*"))) ok = true
        return ok
    }

    private fun queueEvt(evt: JSONObject, to: String = "peer", ephemeral: Boolean = false) {
        scope.launch {
            if (!route(evt, to, ephemeral) && !ephemeral) outbox.add(Pair(evt, to))
        }
    }

    private fun act(evt: JSONObject) {
        queueEvt(evt)
        if (devsOf("self").isNotEmpty()) queueEvt(JSONObject(evt.toString()).put("id", evt.optString("id") + "m"), "self")
    }

    private suspend fun flushAll() {
        if (outbox.isNotEmpty()) {
            val list = ArrayList(outbox); outbox.clear()
            for (it in list) if (!route(it.first, it.second)) outbox.add(it)
        }
        RdStore.msgs.filter { it.chat == "p2p" && it.from == "me" && it.q }.forEach { deliver(it) }
        RdStore.msgs.filter { it.chat == "tg" && it.from == "me" && it.status == "pending" }.forEach { tgDeliver(it) }
    }

    // ---------------------------------------------------------------- incoming
    private suspend fun onBroadcast(ev: String, p: JSONObject) {
        when (ev) {
            "poke" -> { val to = p.optJSONArray("to"); if (to == null || (0 until to.length()).any { to.optString(it) == DEV }) fetchPending() }
            "evt" -> {
                val to = p.optJSONArray("to")
                if (to != null && (0 until to.length()).none { to.optString(it) == DEV }) return
                try { handleEvt(p) } catch (_: Throwable) {}
            }
        }
    }

    suspend fun fetchPending() {
        if (!running || ME.isEmpty() || !Conn.isOnline() || fetching) return
        fetching = true
        var more = false
        try {
            val url = (RdCfg.SB_URL + "/rest/v1/" + RdCfg.T_MSG).toHttpUrl().newBuilder()
                .addQueryParameter("select", "*").addQueryParameter("to_user", "eq.$ME").addQueryParameter("consumed", "eq.false")
                .addQueryParameter("to_dev", "in.(\"$DEV\",\"*\")").addQueryParameter("order", "created_at.asc").addQueryParameter("limit", "200").build()
            val (code, body) = exec(Request.Builder().url(url).header("apikey", RdCfg.SB_KEY).header("Authorization", "Bearer " + RdCfg.SB_KEY).build())
            if (code in 200..299) {
                val arr = JSONArray(body); val done = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val row = arr.getJSONObject(i)
                    try { handleEvt(row.getJSONObject("payload")); done.add(row.getString("id")) } catch (_: Throwable) {}
                }
                if (done.isNotEmpty()) {
                    val u2 = (RdCfg.SB_URL + "/rest/v1/" + RdCfg.T_MSG).toHttpUrl().newBuilder()
                        .addQueryParameter("id", "in.(" + done.joinToString(",") { "\"$it\"" } + ")").build()
                    exec(Request.Builder().url(u2).header("apikey", RdCfg.SB_KEY).header("Authorization", "Bearer " + RdCfg.SB_KEY)
                        .header("Prefer", "return=minimal").patch("{\"consumed\":true}".toRequestBody(JSON)).build())
                }
                more = arr.length() >= 200
            }
        } catch (_: Throwable) {
        } finally { fetching = false }
        if (more) { delay(300); fetchPending() }
    }

    private suspend fun handleEvt(e: JSONObject) {
        val t = e.s("t"); if (t.isEmpty()) return
        val mine = e.s("fu") == ME
        val id = e.s("id")
        if (id.isNotEmpty() && t != "msg") {
            if (!seenEvt.add(id)) return
            if (seenEvt.size > 2000) seenEvt.remove(seenEvt.first())
        }
        when (t) {
            "msg" -> onMsg(e, mine)
            "ack" -> {
                val ids = e.optJSONArray("ids") ?: return
                for (i in 0 until ids.length()) { val m = RdStore.get(ids.optString(i)); if (m != null && m.from == "me") setStatus(m, e.s("s")) }
            }
            "sts" -> if (!mine) {
                val ids = e.optJSONArray("ids") ?: return
                val seen = JSONArray(); val del = JSONArray()
                for (i in 0 until ids.length()) { val m = RdStore.get(ids.optString(i)); if (m != null && m.from == "peer" && !m.system) (if (m.read) seen else del).put(m.id) }
                if (seen.length() > 0) queueEvt(JSONObject().put("t", "ack").put("id", uid()).put("ids", seen).put("s", "seen"))
                if (del.length() > 0) queueEvt(JSONObject().put("t", "ack").put("id", uid()).put("ids", del).put("s", "delivered"))
            }
            "edit" -> { val m = RdStore.get(e.s("mid")); if (m != null && !m.deleted && ((mine && m.from == "me") || (!mine && m.from == "peer"))) { m.text = e.s("text"); m.edited = true; RdStore.save(); RdStore.touch() } }
            "unsend" -> { val m = RdStore.get(e.s("mid")); if (m != null && ((mine && m.from == "me") || (!mine && m.from == "peer"))) RdStore.wipe(m) }
            "react" -> { val m = RdStore.get(e.s("mid")); if (m != null) applyReact(m, if (mine) "me" else "peer", e.s("emoji")) }
            "readsync" -> if (mine) {
                val ids = e.optJSONArray("ids") ?: return
                for (i in 0 until ids.length()) RdStore.get(ids.optString(i))?.read = true
                RdStore.save(); RdStore.touch()
            }
            "call" -> if (!mine) RdCall.onSignal(e)
            "nick" -> if (!mine) { Prefs.putString("rd_nickby", e.s("nick")); RdStore.touch() }
            "prof" -> if (!mine) { saveBase64Photo(e.s("photo"), RdStore.photoFile("peer")); RdStore.touch() }
            "typing" -> if (!mine) {
                peerTyping = e.optBoolean("on")
                typingJob?.cancel()
                if (peerTyping) typingJob = scope.launch { delay(3500); peerTyping = false }
            }
        }
    }

    private fun preview(m: Msg): String =
        if (m.hasMedia) (if (m.voice) "\uD83C\uDFA4 Voice message" else when (mediaKind(m.mediaType)) {
            "image" -> "\uD83D\uDCF7 Photo"; "video" -> "\uD83C\uDFA5 Video"; "audio" -> "\uD83C\uDFB5 Audio"; else -> "\uD83D\uDCC4 " + (m.mediaName ?: "File")
        }) else m.text

    /** Naya message: banner (dusri chat / browser me ho to) ya system notification (app background me ho to) */
    private fun notifyIncoming(m: Msg) {
        if (muted) return
        val title = if (m.chat == "tg") "Telegram" else (RdStore.nick("peer").ifEmpty { RdCfg.NAMES[PEER] ?: "Message" })
        if (curChat == m.chat && foreground && uiMode == "chat") return
        // system notification: hamesa (foreground me silent, background me alert)
        Notifier.message(title, preview(m), m.chat, m.id, foreground)
        if (foreground && (uiMode == "browser" || (uiMode == "chat" && curChat != m.chat))) banner = BannerData(m.chat, title, preview(m), System.currentTimeMillis())
    }

    private suspend fun onMsg(e: JSONObject, mine: Boolean) {
        val id = e.s("id")
        val ex = RdStore.get(id)
        if (ex != null) { if (!mine && !e.optBoolean("system") && ex.from == "peer") ackMsg(id, if (ex.read) "seen" else "delivered"); return }
        val m = Msg(id, "p2p")
        m.from = if (mine) "me" else "peer"
        m.ts = e.optLong("ts", System.currentTimeMillis()); m.text = e.s("text")
        m.fwd = e.optBoolean("fwd"); m.voice = e.optBoolean("voice"); m.system = e.optBoolean("system")
        e.optJSONObject("reply")?.let { rp ->
            m.replyId = rp.s("id"); val w = rp.s("who")
            m.replyWho = if (mine) w else (if (w == "me") "peer" else "me"); m.replyText = rp.s("t")
        }
        m.status = if (mine) "sent" else "received"; m.read = mine
        e.optJSONObject("media")?.let { md ->
            m.mediaName = md.s("name", "file"); m.mediaType = md.s("type"); m.mediaSize = md.optLong("size")
            val path = md.s("path")
            if (path.isNotEmpty()) {
                if (!downloadStorage(path, RdStore.blobFile(id))) { if (!Conn.isOnline()) throw IOException("offline"); m.mediaFailed = true }
            } else {
                m.mediaFailed = true
            }
            Unit
        }
        RdStore.add(m)
        if (!mine && !m.system) {
            val open = curChat == "p2p" && foreground && uiMode == "chat"
            if (open) { m.read = true; ackMsg(id, "seen"); readSync(listOf(id)) } else { ackMsg(id, "delivered"); notifyIncoming(m) }
        }
        RdStore.touch()
    }

    private suspend fun downloadStorage(path: String, target: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val enc = path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
            Net.http.newCall(sb("/storage/v1/object/${RdCfg.BUCKET}/$enc").build()).await().use { r ->
                if (!r.isSuccessful) return@withContext false
                r.body?.byteStream()?.use { i -> target.outputStream().use { i.copyTo(it) } }
            }
            target.exists() && target.length() > 0
        } catch (_: Throwable) { false }
    }

    // ---------------------------------------------------------------- status / read
    private fun setStatus(m: Msg, s: String) {
        if ((RANK[s] ?: 0) <= (RANK[m.status] ?: 0)) return
        m.status = s; RdStore.save(); RdStore.touch()
    }

    fun ackMsg(id: String, s: String) { queueEvt(JSONObject().put("t", "ack").put("id", uid()).put("ids", JSONArray().put(id)).put("s", s)) }
    private fun readSync(ids: List<String>) { if (ids.isNotEmpty() && devsOf("self").isNotEmpty()) scope.launch { route(JSONObject().put("t", "readsync").put("id", uid()).put("ids", JSONArray(ids)), "self", true) } }

    private fun syncStatus() {
        if (!running || !peerOnline) return
        val ids = JSONArray()
        RdStore.msgs.filter { it.chat == "p2p" && it.from == "me" && !it.system && !it.deleted && it.status != "seen" && it.status != "pending" }.takeLast(150).forEach { ids.put(it.id) }
        if (ids.length() > 0) queueEvt(JSONObject().put("t", "sts").put("id", uid()).put("ids", ids), "peer", true)
    }

    fun markRead(chat: String) {
        if (!foreground) return
        Notifier.cancelChat(chat)
        val ids = ArrayList<String>()
        RdStore.msgs.forEach { m -> if (m.chat == chat && m.from == "peer" && !m.read) { m.read = true; if (chat == "p2p" && !m.system) ids.add(m.id) } }
        if (ids.isNotEmpty()) { queueEvt(JSONObject().put("t", "ack").put("id", uid()).put("ids", JSONArray(ids)).put("s", "seen")); readSync(ids) }
        RdStore.save(); RdStore.touch()
    }

    fun sendTyping(on: Boolean) {
        if (curChat != "p2p" || !running) return
        scope.launch { route(JSONObject().put("t", "typing").put("id", uid()).put("on", on), "peer", true) }
    }

    // ---------------------------------------------------------------- sending
    private fun replyJson(m: Msg): Any = if (m.replyId == null) JSONObject.NULL else
        JSONObject().put("id", m.replyId).put("who", m.replyWho ?: "").put("t", m.replyText ?: "")

    private fun applyReply(m: Msg, r: ReplyRef?) { if (r != null) { m.replyId = r.id; m.replyWho = r.who; m.replyText = r.text; m.replyTg = r.tgId } }

    private fun copyBlob(m: Msg, p: PickedFile) {
        m.mediaName = p.name; m.mediaType = p.mime; m.mediaSize = p.size
        try { p.file.copyTo(RdStore.blobFile(m.id), true); p.file.delete() } catch (_: Throwable) { m.mediaFailed = true }
    }

    /** chat ke hisab se bhejna: p2p / tg / me / saved */
    fun send(chat: String, text: String, file: PickedFile?, voice: Boolean, reply: ReplyRef?) {
        when (chat) {
            "p2p" -> {
                val m = Msg(uid(), "p2p"); m.ts = System.currentTimeMillis(); m.text = text; m.voice = voice
                m.status = if (Conn.isOnline()) "sent" else "pending"; m.q = true; applyReply(m, reply)
                if (file != null) copyBlob(m, file)
                RdStore.add(m); deliver(m)
            }
            "tg" -> {
                val m = Msg("tg_" + uid(), "tg"); m.ts = System.currentTimeMillis(); m.text = text; m.voice = voice; m.status = "pending"; applyReply(m, reply)
                if (file != null) copyBlob(m, file)
                RdStore.add(m); scope.launch { tgDeliver(m) }
            }
            else -> {
                val m = Msg((if (chat == "me") "me_" else "sv_") + uid(), chat); m.ts = System.currentTimeMillis(); m.text = text
                m.voice = voice; m.read = true; applyReply(m, reply)
                if (file != null) copyBlob(m, file)
                RdStore.add(m)
            }
        }
    }

    private fun deliver(m: Msg) {
        if (busy.contains(m.id) || !m.q) return
        busy.add(m.id)
        scope.launch {
            try { if (m.hasMedia) mediaLock.withLock { deliverNow(m) } else deliverNow(m) } catch (_: Throwable) {}
            busy.remove(m.id)
        }
    }

    private suspend fun deliverNow(m: Msg) {
        if (m.deleted) return
        val evt = JSONObject().put("t", "msg").put("id", m.id).put("ts", m.ts).put("text", m.text).put("voice", m.voice)
            .put("fwd", m.fwd).put("al", "").put("reply", replyJson(m))
        if (m.hasMedia) {
            evt.put("media", JSONObject().put("name", m.mediaName).put("type", m.mediaType ?: "").put("size", m.mediaSize))
            m.prog = 0; RdStore.touch()
            val ok = uploadMedia(m, evt)
            m.prog = -1
            if (!ok) { RdStore.touch(); return }
        } else evt.put("media", JSONObject.NULL)
        val ok = route(evt, "peer", false, true)
        if (devsOf("self").isNotEmpty()) route(evt, "self", false, true)
        if (ok) { m.q = false; if (m.status == "pending") m.status = "sent"; RdStore.save() }
        RdStore.touch()
    }

    private suspend fun uploadMedia(m: Msg, evt: JSONObject): Boolean {
        if (!Conn.isOnline()) return false
        val f = RdStore.blobFile(m.id)
        if (!f.exists() || f.length() > RdCfg.MAX_DB_FILE) return false
        val safe = (m.mediaName ?: "file").replace(Regex("[^\\w.\\-]+"), "_")
        val path = ME + "/" + m.id + "_" + safe
        val req = sb("/storage/v1/object/${RdCfg.BUCKET}/$path").header("x-upsert", "true")
            .post(f.asRequestBody((m.mediaType ?: "application/octet-stream").toMediaTypeOrNull())).build()
        val ok = exec(req).first in 200..299
        if (ok) evt.getJSONObject("media").put("path", path)
        return ok
    }

    // ---------------------------------------------------------------- edit / react / unsend
    fun applyReact(m: Msg, who: String, emoji: String) {
        if (emoji.isNotEmpty()) m.reactions[who] = emoji else m.reactions.remove(who)
        RdStore.save(); RdStore.touch()
    }

    fun react(m: Msg, emoji: String) {
        applyReact(m, "me", emoji)
        if (m.chat == "p2p") act(JSONObject().put("t", "react").put("id", uid()).put("mid", m.id).put("emoji", emoji))
    }

    fun edit(m: Msg, text: String) {
        if (m.chat == "tg") { scope.launch { tgEdit(m, text) }; return }
        m.text = text; m.edited = true; RdStore.save(); RdStore.touch()
        if (m.chat == "p2p") act(JSONObject().put("t", "edit").put("id", uid()).put("mid", m.id).put("text", text))
    }

    fun unsend(m: Msg) {
        if (m.chat == "tg") { scope.launch { tgDelete(m) }; return }
        RdStore.wipe(m)
        if (m.chat == "p2p") act(JSONObject().put("t", "unsend").put("id", uid()).put("mid", m.id))
    }

    // ---------------------------------------------------------------- Telegram chat
    private var tgOffset: Long
        get() = Prefs.getLong("rd_tg_offset", 0L)
        set(v) = Prefs.putLong("rd_tg_offset", v)

    private suspend fun tgPoll() {
        if (tgBusy || !running || !Conn.isOnline()) return
        tgBusy = true
        try {
            val r = Tg.get(RdCfg.TG_TOKEN, "getUpdates?timeout=0&offset=$tgOffset") ?: return
            val arr = r.optJSONArray("result") ?: return
            for (i in 0 until arr.length()) {
                val u = arr.getJSONObject(i)
                try { tgHandle(u) } catch (_: Throwable) {}
                tgOffset = u.optLong("update_id") + 1
            }
        } finally { tgBusy = false }
    }

    private suspend fun tgHandle(u: JSONObject) {
        u.optJSONObject("edited_message")?.let { em ->
            if (em.optJSONObject("chat")?.optLong("id").toString() == RdCfg.TG_CHAT) {
                RdStore.get("tg_in_" + em.optLong("message_id"))?.let { o -> o.text = em.s("text", em.s("caption")); o.edited = true; RdStore.save(); RdStore.touch() }
            }
            return
        }
        val t = u.optJSONObject("message") ?: return
        if (t.optJSONObject("chat")?.optLong("id").toString() != RdCfg.TG_CHAT) return
        val id = "tg_in_" + t.optLong("message_id")
        if (RdStore.get(id) != null) return
        val m = Msg(id, "tg"); m.from = "peer"; m.ts = t.optLong("date") * 1000L
        m.text = t.s("text", t.s("caption")); m.status = "received"; m.tgId = t.optLong("message_id")
        var fid = ""; var type = ""; var name = "file"
        if (t.has("photo")) { val a = t.getJSONArray("photo"); fid = a.getJSONObject(a.length() - 1).s("file_id"); type = "image/jpeg"; name = "photo.jpg" }
        else if (t.has("video")) { val v = t.getJSONObject("video"); fid = v.s("file_id"); type = v.s("mime_type", "video/mp4"); name = v.s("file_name", "video.mp4") }
        else if (t.has("animation")) { val v = t.getJSONObject("animation"); fid = v.s("file_id"); type = v.s("mime_type", "video/mp4"); name = "animation.mp4" }
        else if (t.has("voice")) { val v = t.getJSONObject("voice"); fid = v.s("file_id"); type = v.s("mime_type", "audio/ogg"); name = "voice.ogg"; m.voice = true }
        else if (t.has("audio")) { val v = t.getJSONObject("audio"); fid = v.s("file_id"); type = v.s("mime_type", "audio/mpeg"); name = v.s("file_name", "audio.mp3") }
        else if (t.has("document")) { val v = t.getJSONObject("document"); fid = v.s("file_id"); type = v.s("mime_type", "application/octet-stream"); name = v.s("file_name", "file") }
        else if (t.has("sticker")) { val v = t.getJSONObject("sticker"); if (!v.optBoolean("is_animated") && !v.optBoolean("is_video")) { fid = v.s("file_id"); type = "image/webp"; name = "sticker.webp" } }
        if (fid.isNotEmpty()) {
            m.mediaName = name; m.mediaType = type
            val url = Tg.fileUrl(RdCfg.TG_TOKEN, fid)
            if (url == null || !Tg.download(url, RdStore.blobFile(id))) m.mediaFailed = true else m.mediaSize = RdStore.blobFile(id).length()
        } else if (m.text.isEmpty()) m.text = "[Unsupported message]"
        RdStore.add(m)
        if (!(curChat == "tg" && foreground && uiMode == "chat")) notifyIncoming(m) else markRead("tg")
    }

    private suspend fun tgDeliver(m: Msg) {
        if (busy.contains(m.id) || m.deleted || !Conn.isOnline()) return
        busy.add(m.id)
        try {
            val r = if (m.hasMedia) {
                val type = m.mediaType ?: ""
                val (method, field) = when {
                    m.voice && type.contains("ogg") -> Pair("sendVoice", "voice")
                    type == "image/gif" -> Pair("sendAnimation", "animation")
                    type.startsWith("image/") && m.mediaSize <= 10L * 1048576 -> Pair("sendPhoto", "photo")
                    type.startsWith("video/") -> Pair("sendVideo", "video")
                    else -> Pair("sendDocument", "document")
                }
                val fields = HashMap<String, String>(); fields["chat_id"] = RdCfg.TG_CHAT
                if (m.text.isNotEmpty()) fields["caption"] = m.text
                if (m.replyTg > 0) fields["reply_to_message_id"] = m.replyTg.toString()
                Tg.upload(RdCfg.TG_TOKEN, method, fields, field, RdStore.blobFile(m.id), m.mediaName ?: "file", type.ifEmpty { "application/octet-stream" })
            } else {
                val b = JSONObject().put("chat_id", RdCfg.TG_CHAT).put("text", m.text).put("allow_sending_without_reply", true)
                if (m.replyTg > 0) b.put("reply_to_message_id", m.replyTg)
                Tg.post(RdCfg.TG_TOKEN, "sendMessage", b)
            }
            if (r != null && r.optBoolean("ok")) {
                m.tgId = r.optJSONObject("result")?.optLong("message_id") ?: 0L
                m.status = "sent"; RdStore.save()
            }
        } catch (_: Throwable) {}
        busy.remove(m.id); RdStore.touch()
    }

    private suspend fun tgEdit(m: Msg, text: String) {
        if (m.tgId == 0L) { Toaster.show("Edit nahi ho sakta"); return }
        val r = Tg.post(RdCfg.TG_TOKEN, "editMessageText", JSONObject().put("chat_id", RdCfg.TG_CHAT).put("message_id", m.tgId).put("text", text))
        if (r != null && r.optBoolean("ok")) { m.text = text; m.edited = true; RdStore.save(); RdStore.touch() } else Toaster.show("Edit nahi hua")
    }

    private suspend fun tgDelete(m: Msg) {
        if (m.tgId == 0L) { Toaster.show("Unsend nahi ho sakta"); return }
        val r = Tg.post(RdCfg.TG_TOKEN, "deleteMessage", JSONObject().put("chat_id", RdCfg.TG_CHAT).put("message_id", m.tgId))
        if (r != null && r.optBoolean("ok")) RdStore.wipe(m) else Toaster.show("Unsend nahi hua (48 ghante se purana ho sakta hai)")
    }
}
