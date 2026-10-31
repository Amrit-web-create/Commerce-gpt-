package com.amrit.commercehub11

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Notifier {
    private lateinit var app: Context

    fun init(c: Context) {
        app = c.applicationContext
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel("msgs", "Messages", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel("calls", "Calls", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel("svc", "Background sync", NotificationManager.IMPORTANCE_LOW))
        }
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(): PendingIntent {
        val i = Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(app, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val recent = HashMap<String, MutableList<String>>()

    /** WhatsApp jaisa notification: Reply + Like buttons, chat khulne tak dikhta rahta hai */
    fun message(title: String, text: String, chat: String, mid: String, silent: Boolean) {
        if (!Prefs.notifEnabled || !canPost()) return
        val lines = recent.getOrPut(chat) { ArrayList() }
        lines.add(text); while (lines.size > 6) lines.removeAt(0)
        val open = Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        open.putExtra("open_chat", chat)
        val openPi = PendingIntent.getActivity(app, chat.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val ri = RemoteInput.Builder("reply").setLabel("Reply").build()
        val rIntent = Intent(app, NotifReceiver::class.java).setAction("rd.reply").putExtra("chat", chat).putExtra("mid", mid)
        val rPi = PendingIntent.getBroadcast(app, chat.hashCode() + 1, rIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val lIntent = Intent(app, NotifReceiver::class.java).setAction("rd.like").putExtra("chat", chat).putExtra("mid", mid)
        val lPi = PendingIntent.getBroadcast(app, chat.hashCode() + 2, lIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.forEach { style.addLine(it) }
        val n = NotificationCompat.Builder(app, "msgs").setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title).setContentText(text).setStyle(style).setNumber(lines.size)
            .setAutoCancel(false).setOnlyAlertOnce(false).setContentIntent(openPi)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL).setSilent(silent)
            .addAction(NotificationCompat.Action.Builder(R.drawable.ic_notif, "Reply", rPi).addRemoteInput(ri).setAllowGeneratedReplies(true).build())
            .addAction(NotificationCompat.Action.Builder(R.drawable.ic_notif, "\u2764 Like", lPi).build())
            .build()
        try { NotificationManagerCompat.from(app).notify(chat.hashCode(), n) } catch (_: SecurityException) {}
    }

    fun cancelChat(chat: String) {
        recent.remove(chat)
        try { NotificationManagerCompat.from(app).cancel(chat.hashCode()) } catch (_: Throwable) {}
    }

    fun call(title: String, text: String) {
        if (!canPost()) return
        val n = NotificationCompat.Builder(app, "calls").setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(openApp())
            .setCategory(NotificationCompat.CATEGORY_CALL).setPriority(NotificationCompat.PRIORITY_MAX)
            .setTimeoutAfter(45000).build()
        try { NotificationManagerCompat.from(app).notify(4242, n) } catch (_: SecurityException) {}
    }

    fun cancelCall() { try { NotificationManagerCompat.from(app).cancel(4242) } catch (_: Throwable) {} }

    fun serviceNotification(): Notification =
        NotificationCompat.Builder(app, "svc").setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Commerce Hub 11").setContentText("Message alerts on").setOngoing(true)
            .setContentIntent(openApp()).build()

    fun startService(ctx: Context) {
        if (Prefs.getString("rd_me", "").isEmpty()) return
        try { ContextCompat.startForegroundService(ctx, Intent(ctx, RdService::class.java)) } catch (_: Throwable) {}
    }

    fun stopService(ctx: Context) {
        try { ctx.stopService(Intent(ctx, RdService::class.java)) } catch (_: Throwable) {}
    }
}

/** Notification ON hone par background me messages sync rakhta hai */
class RdService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.init(this)
        val n = Notifier.serviceNotification()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(77, n)
        } catch (_: Throwable) { stopSelf(); return START_NOT_STICKY }
        val me = Prefs.getString("rd_me", "")
        if (me.isNotEmpty() && Prefs.notifEnabled) RdNet.start(me) else stopSelf()
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) { stopSelf() }

    override fun onDestroy() {
        if (!RdNet.uiOpen) RdNet.stop()
        super.onDestroy()
    }
}


/** Notification ke Reply / Like buttons yahin aate hain */
class NotifReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val chat = intent.getStringExtra("chat") ?: return
        val mid = intent.getStringExtra("mid") ?: ""
        Prefs.init(ctx); RdStore.init(ctx); RdCall.init(ctx); Notifier.init(ctx)
        val me = Prefs.getString("rd_me", "")
        if (me.isEmpty()) return
        val pr = goAsync()
        RdStore.load()
        if (!RdNet.running) RdNet.start(me)
        when (intent.action) {
            "rd.reply" -> {
                val txt = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("reply")?.toString()?.trim()
                if (!txt.isNullOrEmpty()) RdNet.send(chat, txt, null, false, null)
            }
            "rd.like" -> RdStore.get(mid)?.let { RdNet.react(it, "\u2764\uFE0F") }
        }
        Notifier.cancelChat(chat)
        RdNet.scope.launch { delay(5000); pr.finish() }
    }
}
