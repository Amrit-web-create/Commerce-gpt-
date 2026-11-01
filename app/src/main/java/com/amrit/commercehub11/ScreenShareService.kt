package com.amrit.commercehub11

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Android 10+ me screen capture ke liye foreground service zaruri hai */
class ScreenShareService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.init(this)
        val n = Notifier.serviceNotification()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(78, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(78, n)
        } catch (_: Throwable) { stopSelf(); return START_NOT_STICKY }
        @Suppress("DEPRECATION")
        val data: Intent? = intent?.getParcelableExtra("data")
        if (data != null) RdCall.startScreen(data) else stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, ScreenShareService::class.java)) } catch (_: Throwable) {}
        }
    }
}
