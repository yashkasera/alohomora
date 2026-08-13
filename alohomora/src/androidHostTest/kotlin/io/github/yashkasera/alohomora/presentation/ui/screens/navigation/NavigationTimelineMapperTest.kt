package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import io.github.yashkasera.alohomora.ActivityEvent
import io.github.yashkasera.alohomora.ActivityState
import io.github.yashkasera.alohomora.IntentSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the reconstruction of a screen-navigation timeline from Android's flat lifecycle event
 * stream. Each case is a regression for a way the old `NavigationHistoryViewModel` got it wrong:
 * collapsing a multi-screen history to one row, ordering by destruction instead of creation, and
 * dropping the foreground screen's intent.
 *
 * Pure logic with an injected `now`, so it runs on the Android host with no device or Compose.
 */
class NavigationTimelineMapperTest {

    private val base = 1_000_000L
    private val host = "com.example.app"

    private fun created(name: String, t: Long, intent: IntentSnapshot? = null) =
        ActivityEvent(name, base + t, ActivityState.CREATED, intent, taskId = 1)

    private fun resumed(name: String, t: Long) =
        ActivityEvent(name, base + t, ActivityState.RESUMED, null, taskId = 1)

    private fun destroyed(name: String, t: Long) =
        ActivityEvent(name, base + t, ActivityState.DESTROYED, null, taskId = 1)

    @Test
    fun `keeps every screen when none are destroyed`() {
        // A -> B -> C, all still on the back stack (nothing destroyed).
        val events = listOf(
            created("$host.A", 0), resumed("$host.A", 10),
            created("$host.B", 100), resumed("$host.B", 110),
            created("$host.C", 200), resumed("$host.C", 210),
        )

        val items = NavigationTimelineMapper.map(events, now = base + 300)

        assertEquals(3, items.size)
        assertEquals(listOf("A", "B", "C"), items.map { it.title })
        assertEquals("ENTRY POINT", items[0].stateLabel)
        assertEquals("BACKGROUND", items[1].stateLabel)
        assertTrue(items[2].isActive)
        assertEquals("FOREGROUND", items[2].stateLabel)
    }

    @Test
    fun `orders by creation not destruction`() {
        // A created first but destroyed after B is created, so a destruction-ordered list would put
        // B above A. Creation order must keep A on top.
        val events = listOf(
            created("$host.A", 0), resumed("$host.A", 10),
            created("$host.B", 100), resumed("$host.B", 110),
            destroyed("$host.A", 200),
        )

        val items = NavigationTimelineMapper.map(events, now = base + 300)

        assertEquals(listOf("A", "B"), items.map { it.title })
    }

    @Test
    fun `foreground screen keeps its deeplink intent`() {
        val intent = IntentSnapshot(
            action = "android.intent.action.VIEW",
            data = "myapp://product/42",
            categories = null,
            flags = 0,
            extras = emptyMap(),
        )
        val events = listOf(
            created("$host.Detail", 0, intent),
            resumed("$host.Detail", 10),
        )

        val item = NavigationTimelineMapper.map(events, now = base + 100).single()

        assertTrue(item.isActive)
        assertEquals("myapp://product/42", item.intentData)
        assertEquals("DEEPLINK", item.badge)
    }

    @Test
    fun `destroyed screen is closed with a finite duration`() {
        val events = listOf(
            created("$host.A", 0), resumed("$host.A", 10),
            destroyed("$host.A", 5_000),
            created("$host.B", 6_000), resumed("$host.B", 6_010),
        )

        val items = NavigationTimelineMapper.map(events, now = base + 7_000)
        val a = items.single { it.title == "A" }

        assertFalse(a.isActive)
        assertEquals("CLOSED", a.stateLabel)
        assertEquals("5s", a.duration)
    }

    @Test
    fun `excludes internal alohomora screens`() {
        val events = listOf(
            created("$host.A", 0), resumed("$host.A", 10),
            created("io.github.yashkasera.alohomora.DevToolsActivity", 100),
            resumed("io.github.yashkasera.alohomora.DevToolsActivity", 110),
        )

        val items = NavigationTimelineMapper.map(events, now = base + 200)

        assertEquals(listOf("A"), items.map { it.title })
        assertEquals(1, NavigationTimelineMapper.screensVisited(events))
    }

    @Test
    fun `matches instances FIFO across a config change`() {
        // Rotation: A created, destroyed, re-created under the same name. FIFO must close the first
        // instance and leave the second open.
        val events = listOf(
            created("$host.A", 0), resumed("$host.A", 10),
            destroyed("$host.A", 100),
            created("$host.A", 110), resumed("$host.A", 120),
        )

        val items = NavigationTimelineMapper.map(events, now = base + 200)

        assertEquals(2, items.size)
        assertEquals("CLOSED", items[0].stateLabel)
        assertTrue(items[1].isActive)
    }

    @Test
    fun `empty input yields empty timeline and zeroed metrics`() {
        assertTrue(NavigationTimelineMapper.map(emptyList(), now = base).isEmpty())
        assertEquals("00:00.00", NavigationTimelineMapper.sessionDuration(emptyList()))
        assertEquals(0, NavigationTimelineMapper.screensVisited(emptyList()))
    }

    @Test
    fun `screensVisited counts screen entries not lifecycle callbacks`() {
        val events = listOf(
            created("$host.A", 0), resumed("$host.A", 10),
            ActivityEvent("$host.A", base + 20, ActivityState.PAUSED, null, 1),
            ActivityEvent("$host.A", base + 30, ActivityState.STOPPED, null, 1),
            created("$host.B", 100), resumed("$host.B", 110),
        )

        assertEquals(2, NavigationTimelineMapper.screensVisited(events))
    }

    @Test
    fun `active screen has no intent when created intent was absent`() {
        val events = listOf(
            created("$host.A", 0, intent = null),
            resumed("$host.A", 10),
        )

        val item = NavigationTimelineMapper.map(events, now = base + 100).single()

        assertNull(item.intentData)
        assertNull(item.badge)
    }
}
