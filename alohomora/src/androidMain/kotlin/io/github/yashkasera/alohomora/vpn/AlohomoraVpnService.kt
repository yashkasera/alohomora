package io.github.yashkasera.alohomora.vpn

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import io.github.yashkasera.alohomora.R
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import kotlinx.serialization.json.Json

@SuppressLint("VpnServicePolicy")
class AlohomoraVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private var packetForwarder: PacketForwarder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val profileJson = intent.getStringExtra(EXTRA_PROFILE)
                val profile = profileJson?.let {
                    runCatching { Json.decodeFromString<ThrottleProfile>(it) }.getOrNull()
                } ?: ThrottleProfiles.NONE

                startForegroundWithNotification()
                startTunnel(profile)
            }

            ACTION_UPDATE_PROFILE -> {
                val profileJson = intent.getStringExtra(EXTRA_PROFILE)
                val profile = profileJson?.let {
                    runCatching { Json.decodeFromString<ThrottleProfile>(it) }.getOrNull()
                } ?: return START_NOT_STICKY
                onProfileUpdated(profile)
            }

            ACTION_STOP -> {
                stopTunnel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTunnel(profile: ThrottleProfile) {
        if (tunInterface != null) return

        val builder = Builder()
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .addDnsServer("8.8.4.4")
            .setMtu(MTU)
            .setSession("Alohomora Throttle: ${profile.name}")
            .setBlocking(true)

        tunInterface = builder.establish()

        if (tunInterface == null) {
            println("[Alohomora] VPN: Failed to establish TUN interface")
            VpnThrottleController.onServiceStateChanged(VpnServiceState.Error("TUN establish failed"))
            stopSelf()
            return
        }

        println("[Alohomora] VPN: TUN interface established, profile=${profile.name}")

        val forwarder = PacketForwarder(this, tunInterface!!)
        forwarder.updateProfile(profile)
        packetForwarder = forwarder
        forwarder.start()

        VpnThrottleController.onServiceStateChanged(VpnServiceState.Running(profile))
    }

    private fun onProfileUpdated(profile: ThrottleProfile) {
        println("[Alohomora] VPN: Profile updated to ${profile.name}")
        packetForwarder?.updateProfile(profile)
    }

    private fun stopTunnel() {
        packetForwarder?.stop()
        packetForwarder = null
        tunInterface?.close()
        tunInterface = null
        println("[Alohomora] VPN: TUN interface closed")
        VpnThrottleController.onServiceStateChanged(VpnServiceState.Stopped)
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        println("[Alohomora] VPN: User revoked VPN permission")
        stopTunnel()
        VpnThrottleController.onServiceStateChanged(VpnServiceState.Error("VPN revoked by user"))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundWithNotification() {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alohomora)
            .setContentTitle("Alohomora VPN Throttle")
            .setContentText("Device-wide throttling active")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Alohomora VPN Throttle",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while Alohomora device-wide throttling is active"
                setShowBadge(false)
            },
        )
    }

    companion object {
        const val ACTION_START = "io.github.yashkasera.alohomora.vpn.START"
        const val ACTION_STOP = "io.github.yashkasera.alohomora.vpn.STOP"
        const val ACTION_UPDATE_PROFILE = "io.github.yashkasera.alohomora.vpn.UPDATE_PROFILE"
        const val EXTRA_PROFILE = "profile"
        const val MTU = 1500
        private const val CHANNEL_ID = "alohomora_vpn_active"
        private const val NOTIFICATION_ID = 0xA10C
    }
}

internal sealed interface VpnServiceState {
    data object Stopped : VpnServiceState
    data class Running(val profile: ThrottleProfile) : VpnServiceState
    data class Error(val message: String) : VpnServiceState
}
