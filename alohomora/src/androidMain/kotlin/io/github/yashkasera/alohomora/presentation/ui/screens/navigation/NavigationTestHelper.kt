package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import io.github.yashkasera.alohomora.ActivityEvent
import io.github.yashkasera.alohomora.ActivityState
import io.github.yashkasera.alohomora.ActivityTracker
import io.github.yashkasera.alohomora.IntentSnapshot

/**
 * Helper object for testing Navigation History screen with mock data
 *
 * Usage:
 * ```kotlin
 * // Generate mock navigation history
 * NavigationTestHelper.generateMockHistory()
 *
 * // Clear all navigation history
 * NavigationTestHelper.clearHistory()
 *
 * // Add a custom navigation event
 * NavigationTestHelper.addMockActivity("MyCustomActivity", withDeeplink = true)
 * ```
 */
object NavigationTestHelper {

    /**
     * Generates a realistic navigation history with multiple activities
     * Useful for testing the Navigation History screen UI
     */
    fun generateMockHistory() {
        clearHistory()

        val now = System.currentTimeMillis()

        // Splash Activity
        addActivityLifecycle(
            name = "io.github.yashkasera.app.SplashActivity",
            startTime = now - 720_000, // 12 minutes ago
            duration = 5_000 // 5 seconds
        )

        // Home Activity
        addActivityLifecycle(
            name = "io.github.yashkasera.app.MainActivity",
            startTime = now - 715_000,
            duration = 120_000, // 2 minutes
            action = "android.intent.action.MAIN"
        )

        // Category Browse Activity
        addActivityLifecycle(
            name = "io.github.yashkasera.app.CategoryBrowseActivity",
            startTime = now - 595_000,
            duration = 72_000, // 1.2 minutes
            extras = mapOf("category" to "String", "filters" to "ArrayList")
        )

        // Search Landing Activity
        addActivityLifecycle(
            name = "io.github.yashkasera.app.SearchLandingActivity",
            startTime = now - 523_000,
            duration = 92_000, // 1.5 minutes
            extras = mapOf("query" to "String", "category" to "String")
        )

        // Product Detail (with deeplink)
        addActivityLifecycle(
            name = "io.github.yashkasera.app.ProductDetailFragment",
            startTime = now - 431_000,
            duration = 131_000, // 2.2 minutes
            action = "android.intent.action.VIEW",
            data = "myapp://product/12345"
        )

        // Back to Home (currently active)
        addActivityStart(
            name = "io.github.yashkasera.app.MainActivity",
            startTime = now - 300_000, // 5 minutes ago
            action = "android.intent.action.MAIN",
            extras = mapOf("userId" to "String", "sessionId" to "String")
        )
    }

    /**
     * Adds a simple mock activity to the navigation history
     */
    fun addMockActivity(
        activityName: String,
        withDeeplink: Boolean = false,
        withExtras: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        val fullName = if (activityName.contains(".")) {
            activityName
        } else {
            "com.example.app.$activityName"
        }

        addActivityLifecycle(
            name = fullName,
            startTime = now - 60_000,
            duration = 30_000,
            action = if (withDeeplink) "android.intent.action.VIEW" else null,
            data = if (withDeeplink) "myapp://example/123" else null,
            extras = if (withExtras) mapOf("key1" to "String", "key2" to "Int") else null
        )
    }

    /**
     * Clears all navigation history
     */
    fun clearHistory() {
        ActivityTracker.clear()
    }

    /**
     * Adds a complete activity lifecycle (Created -> Resumed -> Paused -> Destroyed)
     */
    private fun addActivityLifecycle(
        name: String,
        startTime: Long,
        duration: Long,
        action: String? = null,
        data: String? = null,
        extras: Map<String, String>? = null
    ) {
        val intent = if (action != null || data != null || extras != null) {
            IntentSnapshot(
                action = action,
                data = data,
                categories = null,
                flags = 0,
                extras = extras ?: emptyMap()
            )
        } else null

        // CREATED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime,
                state = ActivityState.CREATED,
                intentSnapshot = intent,
                taskId = 1
            )
        )

        // STARTED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 100,
                state = ActivityState.STARTED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // RESUMED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 200,
                state = ActivityState.RESUMED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // PAUSED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + duration - 200,
                state = ActivityState.PAUSED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // STOPPED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + duration - 100,
                state = ActivityState.STOPPED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // DESTROYED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + duration,
                state = ActivityState.DESTROYED,
                intentSnapshot = null,
                taskId = 1
            )
        )
    }

    /**
     * Adds a started but not destroyed activity (active/current)
     */
    private fun addActivityStart(
        name: String,
        startTime: Long,
        action: String? = null,
        data: String? = null,
        extras: Map<String, String>? = null
    ) {
        val intent = if (action != null || data != null || extras != null) {
            IntentSnapshot(
                action = action,
                data = data,
                categories = null,
                flags = 0,
                extras = extras ?: emptyMap()
            )
        } else null

        // CREATED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime,
                state = ActivityState.CREATED,
                intentSnapshot = intent,
                taskId = 1
            )
        )

        // STARTED
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 100,
                state = ActivityState.STARTED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // RESUMED (active)
        ActivityTracker._events.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 200,
                state = ActivityState.RESUMED,
                intentSnapshot = null,
                taskId = 1
            )
        )
    }
}

/**
 * Extension property to allow testing access to ActivityTracker events
 */
internal val ActivityTracker._events: MutableList<ActivityEvent>
    get() = this.events as MutableList<ActivityEvent>
