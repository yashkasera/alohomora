package io.github.yashkasera.alohomora.common.mock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockGeneratorsTest {

    @Test
    fun uuidFormatIsValid() {
        val uuid = MockGenerators.uuid()
        assertTrue(
            uuid.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")),
            "Invalid UUID format: $uuid",
        )
    }

    @Test
    fun nameContainsSpace() {
        val name = MockGenerators.name()
        assertTrue(name.contains(" "), "Full name should contain a space: $name")
    }

    @Test
    fun emailContainsAtSign() {
        val email = MockGenerators.email()
        assertTrue(email.contains("@"), "Email should contain @: $email")
        assertTrue(email.endsWith("@example.com"), "Email should end with @example.com: $email")
    }

    @Test
    fun intRespectsBounds() {
        repeat(20) {
            val value = MockGenerators.int(5, 10).toInt()
            assertTrue(value in 5..10, "int(5,10) produced $value")
        }
    }

    @Test
    fun floatRespectsBounds() {
        repeat(20) {
            val value = MockGenerators.float(1.0, 2.0).toDouble()
            assertTrue(value >= 1.0 && value <= 2.0, "float(1,2) produced $value")
        }
    }

    @Test
    fun amountHasTwoDecimalPlaces() {
        val result = MockGenerators.amount(10.0, 100.0)
        val parts = result.split(".")
        assertEquals(parts.size, 2, "Expected decimal in amount: $result")
        assertEquals(parts[1].length, 2, "Expected 2 decimal places: $result")
    }

    @Test
    fun boolIsTrueOrFalse() {
        val result = MockGenerators.bool()
        assertTrue(result == "true" || result == "false", "bool should be true or false: $result")
    }

    @Test
    fun timestampIsPositive() {
        val ts = MockGenerators.timestamp().toLong()
        assertTrue(ts > 0, "Timestamp should be positive: $ts")
    }

    @Test
    fun dateProducesIsoFormat() {
        val date = MockGenerators.date("past", 30)
        assertTrue(
            date.matches(Regex("""\d{4}-\d{2}-\d{2}""")),
            "Expected ISO date: $date",
        )
    }

    @Test
    fun oneOfSelectsFromOptions() {
        val options = listOf("a", "b", "c")
        repeat(20) {
            val result = MockGenerators.oneOf(options)
            assertTrue(result in options, "oneOf should select from options: $result")
        }
    }

    @Test
    fun oneOfEmptyReturnsEmpty() {
        assertTrue(MockGenerators.oneOf(emptyList()).isEmpty())
    }

    @Test
    fun intWithReversedBoundsStaysInRange() {
        repeat(20) {
            val value = MockGenerators.int(10, 5).toInt()
            assertTrue(value in 5..10, "int(10,5) produced $value")
        }
    }

    @Test
    fun intWithEqualBoundsReturnsThatValue() {
        assertEquals("7", MockGenerators.int(7, 7))
    }

    @Test
    fun intAtIntMaxDoesNotOverflow() {
        assertEquals(Int.MAX_VALUE.toString(), MockGenerators.int(Int.MAX_VALUE, Int.MAX_VALUE))
        repeat(20) {
            val value = MockGenerators.int(Int.MAX_VALUE - 1, Int.MAX_VALUE).toLong()
            assertTrue(value in (Int.MAX_VALUE - 1L)..Int.MAX_VALUE.toLong(), "produced $value")
        }
    }

    @Test
    fun intAcrossTheWholeIntRange() {
        repeat(20) {
            val value = MockGenerators.int(Int.MIN_VALUE, Int.MAX_VALUE).toLong()
            assertTrue(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong(), "produced $value")
        }
    }

    @Test
    fun floatWithEqualBoundsReturnsThatValue() {
        assertEquals("1.0000", MockGenerators.float(1.0, 1.0))
    }

    @Test
    fun floatWithReversedBoundsStaysInRange() {
        repeat(20) {
            val value = MockGenerators.float(2.0, 1.0).toDouble()
            assertTrue(value >= 1.0 && value <= 2.0, "float(2,1) produced $value")
        }
    }

    @Test
    fun negativeFloatKeepsItsSignInFront() {
        assertEquals("-0.5000", MockGenerators.float(-0.5, -0.5))
        assertEquals("-12.2500", MockGenerators.float(-12.25, -12.25))
        repeat(20) {
            val result = MockGenerators.float(-1.0, 0.0)
            assertTrue(result.matches(Regex("""-?\d+\.\d{4}""")), "Malformed float: $result")
            val value = result.toDouble()
            assertTrue(value >= -1.0 && value <= 0.0, "float(-1,0) produced $value")
        }
    }

    @Test
    fun floatRoundingToZeroHasNoMinusSign() {
        assertEquals("0.0000", MockGenerators.float(-0.00001, -0.00001))
    }

    @Test
    fun negativeAmountKeepsItsSignInFront() {
        assertEquals("-3.07", MockGenerators.amount(-3.07, -3.07))
    }

    @Test
    fun hugeFloatFallsBackToAValidNumber() {
        val result = MockGenerators.float(1e300, 1e300)
        assertEquals(1e300, result.toDouble())
    }

    @Test
    fun dateWithZeroOffsetIsToday() {
        val today = MockGenerators.date("past", 0)
        assertTrue(today.matches(Regex("""\d{4}-\d{2}-\d{2}""")), "Expected ISO date: $today")
        assertEquals(today, MockGenerators.date("future", 0))
    }

    @Test
    fun dateWithNegativeOffsetIsToday() {
        assertEquals(MockGenerators.date("past", 0), MockGenerators.date("past", -5))
    }
}
