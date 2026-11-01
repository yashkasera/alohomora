package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkRuleEngineTest {

    @BeforeTest
    fun reset() {
        NetworkRuleEngine.clear()
    }

    @Test
    fun `empty rules match nothing`() {
        assertNull(NetworkRuleEngine.findMatch("https://example.com/api/users", "GET"))
    }

    @Test
    fun `substring match is case-insensitive`() {
        NetworkRuleEngine.setMockRules(listOf(rule(urlPattern = "/api/users")))
        assertNotNull(NetworkRuleEngine.findMatch("https://example.com/API/USERS?page=1", "GET"))
    }

    @Test
    fun `regex match works`() {
        NetworkRuleEngine.setMockRules(
            listOf(rule(urlPattern = "/users/\\d+", isRegex = true)),
        )
        assertNotNull(NetworkRuleEngine.findMatch("https://example.com/users/42", "GET"))
        assertNull(NetworkRuleEngine.findMatch("https://example.com/users/abc", "GET"))
    }

    @Test
    fun `invalid regex degrades to no match`() {
        NetworkRuleEngine.setMockRules(
            listOf(rule(urlPattern = "[invalid", isRegex = true)),
        )
        assertNull(NetworkRuleEngine.findMatch("https://example.com/anything", "GET"))
    }

    @Test
    fun `method null matches all methods`() {
        NetworkRuleEngine.setMockRules(listOf(rule(method = null)))
        assertNotNull(NetworkRuleEngine.findMatch("https://example.com/api", "GET"))
        assertNotNull(NetworkRuleEngine.findMatch("https://example.com/api", "POST"))
        assertNotNull(NetworkRuleEngine.findMatch("https://example.com/api", "DELETE"))
    }

    @Test
    fun `method filter is case-insensitive`() {
        NetworkRuleEngine.setMockRules(listOf(rule(method = "POST")))
        assertNotNull(NetworkRuleEngine.findMatch("https://example.com/api", "post"))
        assertNull(NetworkRuleEngine.findMatch("https://example.com/api", "GET"))
    }

    @Test
    fun `disabled rules are excluded`() {
        NetworkRuleEngine.setMockRules(
            listOf(rule(enabled = false)),
        )
        assertNull(NetworkRuleEngine.findMatch("https://example.com/api", "GET"))
    }

    @Test
    fun `first matching rule wins`() {
        NetworkRuleEngine.setMockRules(
            listOf(
                rule(id = "first", statusCode = 200),
                rule(id = "second", statusCode = 404),
            ),
        )
        val match = NetworkRuleEngine.findMatch("https://example.com/api", "GET")
        assertEquals("first", match?.id)
    }

    @Test
    fun `clear resets throttle and rules`() {
        NetworkRuleEngine.setThrottle(ThrottleProfiles.SLOW_3G)
        NetworkRuleEngine.setMockRules(listOf(rule()))
        NetworkRuleEngine.clear()

        assertEquals(ThrottleProfiles.NONE, NetworkRuleEngine.throttle)
        assertNull(NetworkRuleEngine.findMatch("https://example.com/api", "GET"))
    }

    @Test
    fun `throttle presets have sensible values`() {
        assertEquals(0L, ThrottleProfiles.NONE.latencyMs)
        assertEquals(0L, ThrottleProfiles.NONE.downloadBytesPerSec)
        assertTrue(ThrottleProfiles.EDGE.latencyMs > ThrottleProfiles.FAST_3G.latencyMs)
        assertTrue(ThrottleProfiles.EDGE.downloadBytesPerSec < ThrottleProfiles.FAST_3G.downloadBytesPerSec)
    }

    private fun rule(
        id: String = "test-rule",
        enabled: Boolean = true,
        urlPattern: String = "/api",
        isRegex: Boolean = false,
        method: String? = null,
        statusCode: Int = 200,
    ) = MockRule(
        id = id,
        enabled = enabled,
        urlPattern = urlPattern,
        isRegex = isRegex,
        method = method,
        statusCode = statusCode,
        responseBody = "{}",
        contentType = "application/json",
    )
}
