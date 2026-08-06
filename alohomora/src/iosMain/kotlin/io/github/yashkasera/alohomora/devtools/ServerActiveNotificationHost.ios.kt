package io.github.yashkasera.alohomora.devtools

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

        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    actual fun dismiss() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(listOf(NOTIFICATION_ID))
    }
}
