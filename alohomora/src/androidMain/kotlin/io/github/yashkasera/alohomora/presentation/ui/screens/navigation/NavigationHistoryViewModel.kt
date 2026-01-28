package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import io.github.yashkasera.alohomora.ActivityEvent
import io.github.yashkasera.alohomora.ActivityState
import io.github.yashkasera.alohomora.ActivityTracker
import io.github.yashkasera.alohomora.IntentSnapshot

data class ActivityTimelineItem(
    val title: String,
    val subtitle: String?,
    val timestamp: String,
    val duration: String?,
    val isActive: Boolean,
    val badge: String? = null,
    val intentAction: String? = null,
    val intentData: String? = null,
    val intentExtras: Map<String, String>? = null
)

private data class ActivitySession(
    val activityName: String,
    val startTime: Long,
    val endTime: Long?,
    val isActive: Boolean,
    val intentSummary: String?,
    val badge: String?,
    val intentSnapshot: IntentSnapshot?
)

data class NavigationHistoryState(
    val timelineEvents: List<ActivityTimelineItem> = emptyList(),
    val sessionDuration: String = "00:00.00",
    val stackOperations: Int = 0,
)

internal class NavigationHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(NavigationHistoryState())
    val state: StateFlow<NavigationHistoryState> = _state.asStateFlow()

    init {
        loadNavigationHistory()
    }

    private fun buildSessions(
        events: List<ActivityEvent>
    ): List<ActivitySession> {

        val sorted = events.sortedBy { it.timestamp }
        val sessions = mutableListOf<ActivitySession>()

        var currentStart: ActivityEvent? = null
        var lastResume: ActivityEvent? = null

        sorted.forEach { event ->
            when (event.state) {
                ActivityState.CREATED -> {
                    currentStart = event
                }

                ActivityState.RESUMED -> {
                    lastResume = event
                }

                ActivityState.DESTROYED -> {
                    val start = currentStart ?: return@forEach

                    sessions += ActivitySession(
                        activityName = start.activityName,
                        startTime = start.timestamp,
                        endTime = event.timestamp,
                        isActive = false,
                        intentSummary = summarizeIntent(start.intentSnapshot),
                        badge = inferBadge(start.intentSnapshot),
                        intentSnapshot = start.intentSnapshot
                    )

                    currentStart = null
                }

                else -> Unit
            }
        }

        // Handle currently active activity
        lastResume?.let { resumed ->
            sessions += ActivitySession(
                activityName = resumed.activityName,
                startTime = resumed.timestamp,
                endTime = null,
                isActive = true,
                intentSummary = summarizeIntent(resumed.intentSnapshot),
                badge = "CURRENT",
                intentSnapshot = resumed.intentSnapshot
            )
        }

        return sessions
    }


    fun map(
        events: List<ActivityEvent>,
        now: Long = System.currentTimeMillis()
    ): List<ActivityTimelineItem> {
        if (events.isEmpty()) return emptyList()

        val sessions = buildSessions(events)
        val activeSession = sessions.lastOrNull { it.isActive }

        return sessions.map { session ->
            ActivityTimelineItem(
                title = simpleName(session.activityName),
                subtitle = session.intentSummary,
                timestamp = formatTimestamp(session.startTime),
                duration = formatDuration(
                    (session.endTime ?: now) - session.startTime
                ),
                isActive = session == activeSession,
                badge = session.badge,
                intentAction = session.intentSnapshot?.action,
                intentData = session.intentSnapshot?.data,
                intentExtras = session.intentSnapshot?.extras,
            )
        }
    }

    private fun summarizeIntent(intent: IntentSnapshot?): String? {
        if (intent == null) return null

        val parts = mutableListOf<String>()

        intent.action?.let { parts += it }
        intent.data?.let { parts += it }

        if (intent.extras.isNotEmpty()) {
            parts += "extras: ${intent.extras.keys.joinToString()}"
        }

        return parts.joinToString(" • ")
    }

    private fun inferBadge(intent: IntentSnapshot?): String? {
        if (intent == null) return null

        return when {
            intent.action?.contains("VIEW", ignoreCase = true) == true ->
                "DEEPLINK"

            intent.extras.isNotEmpty() ->
                "EXTRA"

            else -> null
        }
    }

    private fun simpleName(fqcn: String): String =
        fqcn.substringAfterLast('.')

    private fun formatDuration(ms: Long): String =
        when {
            ms < 1_000 -> "${ms}ms"
            ms < 60_000 -> "${ms / 1_000}s"
            else -> "${ms / 60_000}m ${(ms % 60_000) / 1_000}s"
        }



    private fun loadNavigationHistory() {
        var events = ActivityTracker.events

        // Use mock data if no events available
        if (events.isEmpty()) {
            generateMockActivityEvents()
            events = ActivityTracker.events
        }

        val timelineEvents = map(events)
        val sessionDuration = calculateSessionDuration(events)
        val stackOperations = events.size

        _state.value = NavigationHistoryState(
            timelineEvents = timelineEvents,
            sessionDuration = sessionDuration,
            stackOperations = stackOperations
        )
    }

    private fun calculateSessionDuration(events: List<ActivityEvent>): String {
        if (events.isEmpty()) return "00:00.00"

        val sorted = events.sortedBy { it.timestamp }
        val firstEvent = sorted.first()
        val lastEvent = sorted.last()

        val durationMs = lastEvent.timestamp - firstEvent.timestamp
        return formatSessionDuration(durationMs)
    }

    private fun formatSessionDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format("%02d:%02d.%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d.%02d", minutes, seconds, (ms % 1000) / 10)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${dateTime.hour.toString().padStart(2, '0')}:${
                dateTime.minute.toString().padStart(2, '0')
            }:${dateTime.second.toString().padStart(2, '0')}.${
                dateTime.nanosecond.toString().take(3)
            }"
        } catch (e: Exception) {
            "Invalid timestamp"
        }
    }

    fun refresh() {
        loadNavigationHistory()
    }

    /**
     * Generates mock ActivityEvent data in ActivityTracker for testing
     * when no real navigation events are available
     */
    private fun generateMockActivityEvents() {
        ActivityTracker.clear()

        val now = System.currentTimeMillis()
        val basePackage = "io.github.yashkasera.app"

        // 1. SplashActivity (12 minutes ago, 5 seconds duration)
        addMockActivityLifecycle(
            name = "$basePackage.SplashActivity",
            startTime = now - 720_000,
            duration = 5_000,
            intent = IntentSnapshot(
                action = "android.intent.action.MAIN",
                data = null,
                categories = null,
                flags = 0,
                extras = mapOf("deepLink" to "null")
            )
        )

        // 2. CategoryBrowseActivity (11.4 minutes ago, 1m 12s duration)
        addMockActivityLifecycle(
            name = "$basePackage.CategoryBrowseActivity",
            startTime = now - 684_000,
            duration = 72_000,
            intent = null
        )

        // 3. SearchLandingActivity (10.2 minutes ago, 1m 32s duration)
        addMockActivityLifecycle(
            name = "$basePackage.SearchLandingActivity",
            startTime = now - 612_000,
            duration = 92_000,
            intent = IntentSnapshot(
                action = null,
                data = null,
                categories = null,
                flags = 0,
                extras = mapOf(
                    "query" to "String",
                    "category" to "String",
                    "filters" to "ArrayList"
                )
            )
        )

        // 4. ProductDetailFragment (8 minutes ago, 2m 11s duration)
        addMockActivityLifecycle(
            name = "$basePackage.ProductDetailFragment",
            startTime = now - 480_000,
            duration = 131_000,
            intent = IntentSnapshot(
                action = "android.intent.action.VIEW",
                data = "myapp://product/12345",
                categories = null,
                flags = 0,
                extras = mapOf(
                    "product_id" to "'PRD_00129'",
                    "source" to "'search_res'"
                )
            )
        )

        // 5. HomeDashboardFragment (5 minutes ago, currently active)
        addMockActivityStart(
            name = "$basePackage.HomeDashboardFragment",
            startTime = now - 300_000,
            intent = IntentSnapshot(
                action = "android.intent.action.MAIN",
                data = null,
                categories = null,
                flags = 0,
                extras = emptyMap()
            )
        )
    }

    /**
     * Adds a complete activity lifecycle (Created -> Resumed -> Paused -> Destroyed)
     */
    private fun addMockActivityLifecycle(
        name: String,
        startTime: Long,
        duration: Long,
        intent: IntentSnapshot?
    ) {
        val mutableEvents = ActivityTracker.events as MutableList

        // CREATED
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime,
                state = ActivityState.CREATED,
                intentSnapshot = intent,
                taskId = 1
            )
        )

        // STARTED
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 50,
                state = ActivityState.STARTED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // RESUMED
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 100,
                state = ActivityState.RESUMED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // PAUSED
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + duration - 100,
                state = ActivityState.PAUSED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // STOPPED
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + duration - 50,
                state = ActivityState.STOPPED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // DESTROYED
        mutableEvents.add(
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
    private fun addMockActivityStart(
        name: String,
        startTime: Long,
        intent: IntentSnapshot?
    ) {
        val mutableEvents = ActivityTracker.events as MutableList

        // CREATED
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime,
                state = ActivityState.CREATED,
                intentSnapshot = intent,
                taskId = 1
            )
        )

        // STARTED
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 50,
                state = ActivityState.STARTED,
                intentSnapshot = null,
                taskId = 1
            )
        )

        // RESUMED (currently active)
        mutableEvents.add(
            ActivityEvent(
                activityName = name,
                timestamp = startTime + 100,
                state = ActivityState.RESUMED,
                intentSnapshot = null,
                taskId = 1
            )
        )
    }
}
