package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import kotlin.concurrent.Volatile

internal const val MOCK_ID_HEADER = "X-Alohomora-Mock-Id"

internal object NetworkRuleEngine {

    @Volatile
    private var _throttle: ThrottleProfile = ThrottleProfiles.NONE

    @Volatile
    private var _mockRules: List<CompiledMockRule> = emptyList()

    val throttle: ThrottleProfile get() = _throttle

    fun setThrottle(profile: ThrottleProfile) {
        _throttle = profile
    }

    fun setMockRules(rules: List<MockRule>) {
        _mockRules = rules.filter { it.enabled }.map(::CompiledMockRule)
    }

    fun findMatch(url: String, method: String?): MockRule? =
        _mockRules.firstOrNull { it.matches(url, method) }?.rule

    fun clear() {
        _throttle = ThrottleProfiles.NONE
        _mockRules = emptyList()
    }
}

internal class CompiledMockRule(val rule: MockRule) {
    private val regex: Regex? = if (rule.isRegex) {
        runCatching { Regex(rule.urlPattern) }.getOrNull()
    } else null

    fun matches(url: String, method: String?): Boolean {
        val urlMatch = regex?.containsMatchIn(url)
            ?: url.contains(rule.urlPattern, ignoreCase = true)
        val methodMatch = rule.method == null ||
            rule.method.equals(method, ignoreCase = true)
        return urlMatch && methodMatch
    }
}
