package io.github.yashkasera.alohomora.common.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun `a complete definition is structurally valid`() {
        val errors = def.validateStructure()
        assertTrue(errors.isValid)
        assertEquals(0, errors.blockingCount)
    }

    @Test
    fun `blank name module and template each block`() {
        val errors = def.copy(name = " ", module = "", uriTemplate = "").validateStructure()
        assertTrue(errors.name != null)
        assertTrue(errors.module != null)
        assertTrue(errors.uriTemplate != null)
        assertFalse(errors.isValid)
        assertEquals(3, errors.blockingCount)
    }

    @Test
    fun `a template without a scheme is rejected`() {
        assertTrue(def.copy(uriTemplate = "kyc/verify").validateStructure().uriTemplate != null)
    }

    @Test
    fun `a nameless parameter blocks on its row`() {
        val errors = def.copy(
            params = listOf(DeepLinkParam(name = " ", type = ParamType.STRING)),
        ).validateStructure()
        assertTrue(errors.params[0] != null)
        assertFalse(errors.isValid)
    }

    @Test
    fun `a duplicate parameter name blocks the second row`() {
        val errors = def.copy(
            uriTemplate = "fampay://kyc/{a}/{b}",
            params = listOf(
                DeepLinkParam(name = "dup", type = ParamType.STRING),
                DeepLinkParam(name = "dup", type = ParamType.STRING),
            ),
        ).validateStructure()
        assertTrue(errors.params[0] == null)
        assertTrue(errors.params[1] != null)
    }

    @Test
    fun `an enum parameter with no allowed values blocks`() {
        val errors = def.copy(
            params = listOf(DeepLinkParam(name = "step", type = ParamType.ENUM)),
        ).validateStructure()
        assertTrue(errors.params[0] != null)
    }

    @Test
    fun `an undeclared placeholder is a warning not a block`() {
        val errors = def.copy(
            params = listOf(DeepLinkParam(name = "userId", type = ParamType.UUID)),
        ).validateStructure()
        assertTrue(errors.isValid)
        assertTrue(errors.warnings.any { it.contains("{step}") })
    }
}
