package app.server

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import SyncplayMobile.shared.BuildConfig
import app.R
import app.SyncplayActivity
import app.utils.loggy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Android foreground service for the Syncplay server.
 *
 * Shows the mandatory ongoing notification so Android won't kill the process
 * while the server is running. The actual server lives in
 * [ServerViewmodel.serverProcessScope] (a companion-object scope that outlives
 * the ViewModel), so it keeps running even when the user navigates away from
 * the Server Host screen.
 *
 * If the process IS killed (extreme memory pressure) and the service restarts
 * via START_STICKY, [onStartCommand] checks whether the server is already
 * running and, if not, shows a "Server restart required" notification so the
 * user knows to reopen the app and start the server again.  Full automatic
 * restart would require persisting the full [ServerConfig] which is
 * undesirable on a mobile device.
 */
class SyncplayServerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "${BuildConfig.APP_NAME} Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when a Syncplay server is running"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, 8999) ?: 8999

        if (ServerViewmodel.isServerRunning) {
            // Server is alive in the companion scope — show the live notification
            // and observe client count for updates.
            startForeground(NOTIFICATION_ID, buildNotification(port, 0))

            observerJob?.cancel()
            observerJob = serviceScope.launch {
                try {
                    // Collect from the companion scope's state. We don't have
                    // direct access to the StateFlow, so we poll or use a shared
                    // mechanism. For now, show a static notification — the user
                    // can see live stats by opening the app.
                    //
                    // In a production version you'd register a callback or
                    // observe a shared StateFlow from the companion object.
                } catch (_: Exception) {}
            }
        } else {
            // Server is NOT running (process was killed and restarted).
            // Show a notification telling the user to reopen the app.
            val reopenIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, SyncplayActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${BuildConfig.APP_NAME} Server")
                .setContentText("Tap to reopen — server needs restart")
                .setContentIntent(reopenIntent)
                .setSilent(true)
                .setOngoing(true)
                .build()
            startForeground(NOTIFICATION_ID, notification)

            // Stop ourselves so the notification doesn't linger forever
            // without a real server behind it.
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(port: Int, clients: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${BuildConfig.APP_NAME} Server")
            .setContentText("Running on port $port${if (clients > 0) " - $clients client(s)" else ""}")
            .setSilent(true)
            .setOngoing(true)
            .build()

    companion object {
        const val CHANNEL_ID = "syncplay_server"
        const val NOTIFICATION_ID = 2
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_CLIENTS = "extra_clients"
    }
}
