package com.amrit.commercehub11

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifier {
    private lateinit var app: Context

    fun init(c: Context) {
        app = c.applicationContext
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel("msgs", "Messages", NotificationManager.IMPORTANCE_HIGH))
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

    fun message(title: String, text: String, chat: String) {
        if (!Prefs.notifEnabled || !canPost()) return
        val n = NotificationCompat.Builder(app, "msgs").setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(openApp())
            .setPriority(NotificationCompat.PRIORITY_HIGH).setDefaults(NotificationCompat.DEFAULT_ALL).build()
        try { NotificationManagerCompat.from(app).notify(chat.hashCode(), n) } catch (_: SecurityException) {}
    }

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
