package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every timestamp in this project is **milliseconds**.
 *
 * Two bugs came from not having that rule. Git `%ct` is seconds and both GitHistory screens
 * formatted it as milliseconds, so every commit rendered as `1970-01-21`. And the entity `time`
 * defaults were `epochSeconds` while every explicit write used `toEpochMilliseconds()`, so
 * anything relying on a default sorted ~1000x too low and sank to the bottom of a
 * newest-first list.
 *
 * The root cause was [DateUtils.Format] carrying a per-format default unit — choosing a *look*
 * silently chose an *input unit*.
 */
class TimestampUnitTest {

    /** 2025-07-30T18:26:40Z, comfortably inside the range where the two units differ visibly. */
    private val millis = 1_753_900_000_000L

    @Test
    fun `formatting defaults to milliseconds regardless of the chosen format`() {
        // The regression: READABLE_DATE_TIME and ISO_DATE_TIME_SECONDS used to default to
        // SECONDS purely because of how they look, so a caller holding millis got a 1970 date
        // unless they knew to override the unit.
        DateUtils.Format.entries.forEach { format ->
            assertEquals(
                DateUtils.format(millis, format, DateUtils.TimeUnit.MILLISECONDS),
                DateUtils.format(millis, format),
                "$format must default to milliseconds",
            )
        }
    }

    @Test
    fun `the 1970 symptom is gone for readable formats`() {
        val readable = DateUtils.format(millis, DateUtils.Format.READABLE_DATE_TIME)

        assertTrue(readable.contains("2025"), "expected a 2025 date, got '$readable'")
        assertTrue(!readable.contains("1970"), "still rendering the epoch: '$readable'")
    }

    @Test
    fun `seconds are still formattable when explicitly declared`() {
        // Not every source is ours; the unit stays overridable, it just is not implied.
        val asSeconds = DateUtils.format(
            millis / 1_000,
            DateUtils.Format.READABLE_DATE_TIME,
            DateUtils.TimeUnit.SECONDS,
        )

        assertEquals(DateUtils.format(millis, DateUtils.Format.READABLE_DATE_TIME), asSeconds)
    }

    @Test
    fun `entity time defaults are milliseconds`() {
        // A second-precision default would be ~1.7e9 rather than ~1.7e12.
        val floor = 1_000_000_000_000L // year 2001 in millis; any seconds value is far below

        assertTrue(
            Event(name = "e", properties = null).time > floor,
            "Event.time default is not milliseconds",
        )
        assertTrue(Error(reason = "r").time > floor, "Error.time default is not milliseconds")
        assertTrue(Screen(name = "s").time > floor, "Screen.time default is not milliseconds")
    }

    @Test
    fun `a defaulted entity sorts above an older explicit one`() {
        // The practical consequence of the old mismatch: a defaulted event looked ~55 years old
        // and sank to the bottom of a newest-first list.
        val defaulted = Event(name = "now", properties = null)
        val explicitlyOlder = Event(name = "old", properties = null, time = millis)

        val sorted = listOf(explicitlyOlder, defaulted).sortedByDescending { it.time }

        assertEquals("now", sorted.first().name)
    }
}
