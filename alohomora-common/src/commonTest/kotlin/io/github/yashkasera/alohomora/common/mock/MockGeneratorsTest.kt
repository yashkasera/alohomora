package io.github.yashkasera.alohomora.common.mock

import kotlin.test.Test
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
        assertTrue(parts.size == 2, "Expected decimal in amount: $result")
        assertTrue(parts[1].length == 2, "Expected 2 decimal places: $result")
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
}
