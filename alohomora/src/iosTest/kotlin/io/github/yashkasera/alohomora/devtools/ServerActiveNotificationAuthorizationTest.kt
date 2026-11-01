package io.github.yashkasera.alohomora.devtools

import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerActiveNotificationAuthorizationTest {

    @Test
    fun postsOnlyWhereTheAppAlreadyHoldsPermission() {
        assertTrue(canPostWithoutPrompting(UNAuthorizationStatusAuthorized))
        assertTrue(canPostWithoutPrompting(UNAuthorizationStatusProvisional))
        assertTrue(canPostWithoutPrompting(UNAuthorizationStatusEphemeral))
    }

    @Test
    fun neverPostsWhereThatWouldNeedThePrompt() {
        // Not-determined is the host app's one-shot prompt; spending it here pre-empts its onboarding.
        assertFalse(canPostWithoutPrompting(UNAuthorizationStatusNotDetermined))
        assertFalse(canPostWithoutPrompting(UNAuthorizationStatusDenied))
    }
}
