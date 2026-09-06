package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkParam
import io.github.yashkasera.alohomora.common.deeplink.ParamType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepLinkCatalogRendererTest {

    private val kyc = DeepLinkDef(
        id = "d1",
        name = "KYC verify",
        module = "kyc",
        flow = "onboarding",
        description = "Enter KYC verification.",
        uriTemplate = "fampay://kyc/verify/{userId}",
        params = listOf(DeepLinkParam(name = "userId", type = ParamType.UUID)),
        examples = listOf("fampay://kyc/verify/123e4567-e89b-12d3-a456-426614174000"),
    )
    private val cards = DeepLinkDef(
        id = "d2",
        name = "Card home",
        module = "cards",
        uriTemplate = "fampay://cards/home",
    )

    @Test
    fun `render is deterministic regardless of input order`() {
        val a = DeepLinkCatalogRenderer.render(listOf(kyc, cards))
        val b = DeepLinkCatalogRenderer.render(listOf(cards, kyc))
        assertEquals(a, b)
    }

    @Test
    fun `render includes an index and a module heading`() {
        val md = DeepLinkCatalogRenderer.render(listOf(kyc, cards))
        assertTrue(md.contains("## Index"))
        assertTrue(md.contains("## kyc"))
        assertTrue(md.contains("Card home — `cards`"))
        assertTrue(md.contains("fampay://kyc/verify/{userId}"))
    }

    @Test
    fun `modules are ordered alphabetically`() {
        val md = DeepLinkCatalogRenderer.render(listOf(kyc, cards))
        assertTrue(md.indexOf("## cards") < md.indexOf("## kyc"))
    }
}
