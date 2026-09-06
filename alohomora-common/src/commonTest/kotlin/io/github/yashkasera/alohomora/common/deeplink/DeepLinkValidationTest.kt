package io.github.yashkasera.alohomora.common.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepLinkValidationTest {

    private val def = DeepLinkDef(
        id = "d1",
        name = "KYC verify",
        module = "kyc",
        uriTemplate = "fampay://kyc/verify/{userId}?step={step}",
        params = listOf(
            DeepLinkParam(name = "userId", type = ParamType.UUID),
            DeepLinkParam(
                name = "step",
                type = ParamType.ENUM,
                allowedValues = listOf("intro", "docs", "selfie"),
            ),
        ),
        examples = listOf(
            "fampay://kyc/verify/123e4567-e89b-12d3-a456-426614174000?step=docs",
        ),
    )

    @Test
    fun `placeholders are extracted in order`() {
        assertEquals(listOf("userId", "step"), def.placeholders())
    }

    @Test
    fun `buildUri substitutes valid args`() {
        val result = def.buildUri(
            mapOf(
                "userId" to "123e4567-e89b-12d3-a456-426614174000",
                "step" to "docs",
            ),
        )
        assertEquals(
            "fampay://kyc/verify/123e4567-e89b-12d3-a456-426614174000?step=docs",
            result.url,
        )
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `buildUri reports a missing required parameter`() {
        val result = def.buildUri(mapOf("step" to "docs"))
        assertEquals(null, result.url)
        assertTrue(result.errors.any { it.contains("userId") })
    }

    @Test
    fun `buildUri rejects a value outside the enum`() {
        val result = def.buildUri(
            mapOf(
                "userId" to "123e4567-e89b-12d3-a456-426614174000",
                "step" to "nope",
            ),
        )
        assertEquals(null, result.url)
        assertTrue(result.errors.any { it.contains("step") })
    }

    @Test
    fun `buildUri rejects a malformed uuid`() {
        val result = def.buildUri(mapOf("userId" to "not-a-uuid", "step" to "docs"))
        assertEquals(null, result.url)
        assertTrue(result.errors.any { it.contains("userId") })
    }

    @Test
    fun `a consistent example validates clean`() {
        assertTrue(def.validateExamples().isEmpty())
    }

    @Test
    fun `an example with a bad enum value is flagged`() {
        val bad = def.copy(
            examples = listOf("fampay://kyc/verify/123e4567-e89b-12d3-a456-426614174000?step=wrong"),
        )
        assertTrue(bad.validateExamples().isNotEmpty())
    }
}
