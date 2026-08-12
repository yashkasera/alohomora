package io.github.yashkasera.alohomora.common.mock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TemplateEngineTest {

    @Test
    fun plainTextPassesThrough() {
        val input = """{"name": "Alice"}"""
        assertEquals(input, TemplateEngine.resolve(input))
    }

    @Test
    fun emptyStringPassesThrough() {
        assertEquals("", TemplateEngine.resolve(""))
    }

    @Test
    fun singlePlaceholderIsResolved() {
        val result = TemplateEngine.resolve("{{uuid}}")
        assertNotEquals("{{uuid}}", result)
        assertTrue(result.contains("-"), "Expected UUID format with dashes")
    }

    @Test
    fun multiplePlaceholdersAreResolved() {
        val template = """{"id": "{{uuid}}", "name": "{{name}}"}"""
        val result = TemplateEngine.resolve(template)
        assertTrue(!result.contains("{{uuid}}"), "uuid should be resolved")
        assertTrue(!result.contains("{{name}}"), "name should be resolved")
    }

    @Test
    fun parameterisedIntGenerator() {
        val result = TemplateEngine.resolve("{{int(10,20)}}")
        val value = result.toIntOrNull()
        assertTrue(value != null, "Expected integer output")
        assertTrue(value in 10..20, "Expected value between 10 and 20 but got $value")
    }

    @Test
    fun parameterisedAmountGenerator() {
        val result = TemplateEngine.resolve("{{amount(100,200)}}")
        val value = result.toDoubleOrNull()
        assertTrue(value != null, "Expected decimal output")
        assertTrue(value >= 100.0 && value <= 200.0, "Expected value between 100 and 200 but got $value")
        assertTrue(result.contains("."), "Expected decimal point")
    }

    @Test
    fun dateDirectionPast() {
        val result = TemplateEngine.resolve("{{date(past,7)}}")
        assertTrue(result.matches(Regex("""\d{4}-\d{2}-\d{2}""")), "Expected ISO date but got $result")
    }

    @Test
    fun dateDirectionFuture() {
        val result = TemplateEngine.resolve("{{date(future,30)}}")
        assertTrue(result.matches(Regex("""\d{4}-\d{2}-\d{2}""")), "Expected ISO date but got $result")
    }

    @Test
    fun unknownPlaceholderPassesThrough() {
        val template = "{{unknownGenerator}}"
        assertEquals(template, TemplateEngine.resolve(template))
    }

    @Test
    fun templateInsideJsonString() {
        val template = """{"email": "{{email}}", "active": {{bool}}}"""
        val result = TemplateEngine.resolve(template)
        assertTrue(!result.contains("{{email}}"), "email should be resolved")
        assertTrue(!result.contains("{{bool}}"), "bool should be resolved")
        assertTrue(result.contains("@example.com"), "email should contain @example.com")
    }

    @Test
    fun oneOfGenerator() {
        val result = TemplateEngine.resolve("{{oneOf(active,inactive,pending)}}")
        assertTrue(result in listOf("active", "inactive", "pending"), "Expected one of the options but got $result")
    }

    @Test
    fun timestampIsNumeric() {
        val result = TemplateEngine.resolve("{{timestamp}}")
        assertTrue(result.toLongOrNull() != null, "Expected numeric timestamp but got $result")
    }

    @Test
    fun boolIsBoolean() {
        val result = TemplateEngine.resolve("{{bool}}")
        assertTrue(result in listOf("true", "false"), "Expected true or false but got $result")
    }

    @Test
    fun firstNameAndLastName() {
        val first = TemplateEngine.resolve("{{firstName}}")
        val last = TemplateEngine.resolve("{{lastName}}")
        assertTrue(first.isNotBlank())
        assertTrue(last.isNotBlank())
        assertTrue(!first.contains(" "), "firstName should not contain space")
    }

    @Test
    fun floatGenerator() {
        val result = TemplateEngine.resolve("{{float(0,10)}}")
        val value = result.toDoubleOrNull()
        assertTrue(value != null, "Expected float output")
        assertTrue(value >= 0.0 && value <= 10.0, "Expected value in range but got $value")
    }

    @Test
    fun templateAtBoundaryPositions() {
        assertEquals(TemplateEngine.resolve("{{bool}}suffix").endsWith("suffix"), true)
        assertEquals(TemplateEngine.resolve("prefix{{bool}}").startsWith("prefix"), true)
    }
}
