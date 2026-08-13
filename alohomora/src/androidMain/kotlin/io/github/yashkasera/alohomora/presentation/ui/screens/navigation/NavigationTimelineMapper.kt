package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import io.github.yashkasera.alohomora.ActivityEvent
import io.github.yashkasera.alohomora.ActivityState
import io.github.yashkasera.alohomora.IntentSnapshot
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.DateUtils.Format

/**
 * One screen the user visited, ready to render as a timeline row.
 *
 * Reconstructed from raw [ActivityEvent] lifecycle callbacks by [NavigationTimelineMapper]. One item
 * is one Activity *instance* (a `CREATED`→`DESTROYED` span, or a still-open instance), never one
 * lifecycle callback.
 */
data class ActivityTimelineItem(
    val title: String,
    val subtitle: String?,
    val timestamp: String,
    val duration: String?,
    val isActive: Boolean,
    /** Human state of this screen, computed here so the UI never has to guess it. */
    val stateLabel: String,
    val badge: String? = null,
    val intentAction: String? = null,
    val intentData: String? = null,
    val intentExtras: Map<String, String>? = null,
)

/**
 * Rebuilds a chronological screen-navigation timeline from [ActivityTracker]'s flat lifecycle event
 * stream.
 *
 * Stateless and pure so it can be unit-tested without a `ViewModel`, Koin, or a live tracker (see
 * `NavigationTimelineMapperTest`). The device only ever hands us a flat, time-ordered list of
 * `CREATED/STARTED/RESUMED/PAUSED/STOPPED/DESTROYED` events across interleaved activities; the job
 * here is to collapse that back into one row per screen instance.
 */
internal object NavigationTimelineMapper {

    /**
     * Alohomora's own screens must not appear in the host app's history. Matched by exact class name,
     * not a package prefix: a host app can legitimately live under `io.github.yashkasera.alohomora.*`
     * (the showcase app does), so a prefix match would swallow its real screens.
     */
    private val INTERNAL_ACTIVITIES = setOf(
        "io.github.yashkasera.alohomora.DevToolsActivity",
        "io.github.yashkasera.alohomora.vpn.VpnConsentActivity",
    )

    private fun isInternal(activityName: String): Boolean = activityName in INTERNAL_ACTIVITIES

    private data class Session(
        val activityName: String,
        val startTime: Long,
        var endTime: Long?,
        var resumedAt: Long?,
        val intent: IntentSnapshot?,
    )

    fun map(
        events: List<ActivityEvent>,
        now: Long,
    ): List<ActivityTimelineItem> {
        val sessions = buildSessions(events)
        if (sessions.isEmpty()) return emptyList()

        val active = activeSession(sessions)

        return sessions.mapIndexed { index, session ->
            val isActive = session === active
            ActivityTimelineItem(
                title = simpleName(session.activityName),
                subtitle = summarizeIntent(session.intent),
                timestamp = DateUtils.format(session.startTime, Format.HH_MM_SS),
                duration = formatDuration((session.endTime ?: now) - session.startTime),
                isActive = isActive,
                stateLabel = stateLabel(session, isActive, index),
                badge = inferBadge(session.intent),
                intentAction = session.intent?.action,
                intentData = session.intent?.data,
                intentExtras = session.intent?.extras,
            )
        }
    }

    fun sessionDuration(events: List<ActivityEvent>): String {
        val hostEvents = events.filterNot { isInternal(it.activityName) }
        if (hostEvents.isEmpty()) return "00:00.00"

        val sorted = hostEvents.sortedBy { it.timestamp }
        return formatSessionDuration(sorted.last().timestamp - sorted.first().timestamp)
    }

    /** Screens the user actually opened this session — one per `CREATED`, internal screens excluded. */
    fun screensVisited(events: List<ActivityEvent>): Int =
        events.count {
            it.state == ActivityState.CREATED &&
                !isInternal(it.activityName)
        }

    /**
     * Collapse the flat event stream into one [Session] per screen instance, in creation order.
     *
     * A screen enters on `CREATED` and leaves on `DESTROYED`. Instances of the same activity are
     * matched FIFO (oldest open instance closes first), which keeps a config-change's
     * `CREATE→DESTROY→CREATE` from cross-wiring the two instances. A session with no `DESTROYED` is
     * still on the back stack (or in the foreground); the foreground one is picked in [activeSession].
     */
    private fun buildSessions(events: List<ActivityEvent>): List<Session> {
        val sorted = events
            .filterNot { isInternal(it.activityName) }
            .sortedBy { it.timestamp }

        val ordered = mutableListOf<Session>()
        val openByName = mutableMapOf<String, ArrayDeque<Session>>()

        sorted.forEach { event ->
            when (event.state) {
                ActivityState.CREATED -> {
                    val session = Session(
                        activityName = event.activityName,
                        startTime = event.timestamp,
                        endTime = null,
                        resumedAt = null,
                        intent = event.intentSnapshot,
                    )
                    ordered += session
                    openByName.getOrPut(event.activityName) { ArrayDeque() }.addLast(session)
                }

                ActivityState.RESUMED -> {
                    openByName[event.activityName]?.lastOrNull { it.endTime == null }
                        ?.let { if (it.resumedAt == null) it.resumedAt = event.timestamp }
                }

                ActivityState.DESTROYED -> {
                    val open = openByName[event.activityName] ?: return@forEach
                    val session = open.firstOrNull { it.endTime == null } ?: return@forEach
                    session.endTime = event.timestamp
                    open.remove(session)
                }

                else -> Unit
            }
        }

        return ordered
    }

    /**
     * The foreground screen: the most recently resumed instance that is still open. Falls back to the
     * last still-open session so a stream that never captured a `RESUMED` still highlights something.
     */
    private fun activeSession(sessions: List<Session>): Session? {
        val open = sessions.filter { it.endTime == null }
        if (open.isEmpty()) return null
        return open.filter { it.resumedAt != null }.maxByOrNull { it.resumedAt!! }
            ?: open.last()
    }

    // Lifecycle state (FOREGROUND/CLOSED) beats position (ENTRY POINT): a closed first screen is more
    // usefully "CLOSED" than "ENTRY POINT". ENTRY POINT only surfaces for a first screen still on the
    // stack that would otherwise read "BACKGROUND".
    private fun stateLabel(session: Session, isActive: Boolean, index: Int): String =
        when {
            isActive -> "FOREGROUND"
            session.endTime != null -> "CLOSED"
            index == 0 -> "ENTRY POINT"
            else -> "BACKGROUND"
        }

    private fun summarizeIntent(intent: IntentSnapshot?): String? {
        if (intent == null) return null

        val parts = mutableListOf<String>()
        intent.action?.let { parts += it }
        intent.data?.let { parts += it }
        if (intent.extras.isNotEmpty()) {
            parts += "extras: ${intent.extras.keys.joinToString()}"
        }
        return parts.joinToString(" • ").ifBlank { null }
    }

    private fun inferBadge(intent: IntentSnapshot?): String? {
        if (intent == null) return null

        return when {
            intent.action?.contains("VIEW", ignoreCase = true) == true -> "DEEPLINK"
            intent.extras.isNotEmpty() -> "EXTRA"
            else -> null
        }
    }

    private fun simpleName(fqcn: String): String = fqcn.substringAfterLast('.')

    private fun formatDuration(ms: Long): String =
        when {
            ms < 1_000 -> "${ms}ms"
            ms < 60_000 -> "${ms / 1_000}s"
            else -> "${ms / 60_000}m ${(ms % 60_000) / 1_000}s"
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
}
