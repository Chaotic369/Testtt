package com.example.edgering.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.edgering.MainActivity
import com.example.edgering.R
import com.example.edgering.overlay.OverlayController
import com.example.edgering.overlay.SpikeAppSource
import com.example.edgering.overlay.TriggerSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that owns the edge overlay. Stopping it (notification action, the in-app
 * switch, or the system) always removes every overlay window.
 */
class EdgeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: OverlayController? = null
    private var activeSide: TriggerSide? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        // Must be called promptly after startForegroundService, so before any other work.
        if (!enterForeground()) {
            toast(R.string.toast_service_failed)
            shutdown()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            toast(R.string.toast_overlay_permission_missing)
            shutdown()
            return START_NOT_STICKY
        }
        val side = TriggerSide.fromName(intent?.getStringExtra(EXTRA_SIDE))
        if (controller == null || activeSide != side) {
            controller?.remove()
            val created = OverlayController(
                context = this,
                scope = scope,
                side = side,
                onLatency = EdgeState::reportLatency,
                onMessage = ::toast,
            )
            if (!created.install()) {
                toast(R.string.toast_overlay_failed)
                shutdown()
                return START_NOT_STICKY
            }
            controller = created
            activeSide = side
            scope.launch {
                val items = withContext(Dispatchers.Default) { SpikeAppSource.load(applicationContext, ITEM_LIMIT) }
                controller?.setItems(items)
            }
        }
        EdgeState.setRunning(true)
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller?.refreshTriggerLayout()
    }

    override fun onDestroy() {
        controller?.remove()
        controller = null
        scope.cancel()
        EdgeState.setRunning(false)
        super.onDestroy()
    }

    private fun shutdown() {
        controller?.remove()
        controller = null
        activeSide = null
        EdgeState.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun enterForeground(): Boolean = try {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        true
    } catch (e: RuntimeException) {
        false
    }

    private fun buildNotification(): Notification {
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, EdgeService::class.java).setAction(ACTION_STOP),
            flags,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_edgering)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_stat_edgering, getString(R.string.action_stop), stop)
            .build()
    }

    private fun toast(@StringRes message: Int) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ACTION_STOP = "com.example.edgering.action.STOP"
        const val EXTRA_SIDE = "side"
        private const val CHANNEL_ID = "edge_service"
        private const val NOTIFICATION_ID = 1
        private const val ITEM_LIMIT = 12

        /** Starts the service from the foreground. Returns false if the system refused. */
        fun start(context: Context, side: TriggerSide): Boolean = try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, EdgeService::class.java).putExtra(EXTRA_SIDE, side.name),
            )
            true
        } catch (e: RuntimeException) {
            false
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EdgeService::class.java))
        }
    }
}
