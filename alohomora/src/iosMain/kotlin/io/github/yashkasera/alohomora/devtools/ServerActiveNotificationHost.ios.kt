package io.github.yashkasera.alohomora.devtools

import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

internal actual object ServerActiveNotificationHost {

    private const val NOTIFICATION_ID = "alohomora_server_active"

    actual fun show(port: Int, hasClient: Boolean) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        val content = UNMutableNotificationContent().apply {
            setTitle(if (hasClient) "Desktop connected" else "DevTools server active")
            setBody(if (hasClient) "Streaming on port $port" else "Listening on port $port")
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NOTIFICATION_ID,
            content = content,
            trigger = null,
        )

        // Never requestAuthorization: the system prompt is one-shot per install, so asking here would
        // spend the host app's only chance at it on a debug banner, ahead of its own push onboarding.
        // Post only when the app already holds permission; otherwise the banner is simply skipped.
        center.getNotificationSettingsWithCompletionHandler { settings ->
            if (settings != null && canPostWithoutPrompting(settings.authorizationStatus)) {
                center.addNotificationRequest(request, withCompletionHandler = null)
            }
        }
    }

    actual fun dismiss() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(listOf(NOTIFICATION_ID))
    }
}

/**
 * Whether a notification can be posted without asking. Provisional (delivered quietly to the
 * Notification Center) and ephemeral (App Clips) both deliver without a prompt; not-determined is the
 * one state where posting would require one, so it counts as no.
 */
internal fun canPostWithoutPrompting(status: UNAuthorizationStatus): Boolean =
    status == UNAuthorizationStatusAuthorized ||
        status == UNAuthorizationStatusProvisional ||
        status == UNAuthorizationStatusEphemeral
