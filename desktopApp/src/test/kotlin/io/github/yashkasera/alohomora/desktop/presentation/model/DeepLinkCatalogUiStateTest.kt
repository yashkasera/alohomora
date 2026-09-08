package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkParam
import io.github.yashkasera.alohomora.common.deeplink.ParamType
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigItem
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepLinkCatalogUiStateTest {

    private fun item(def: DeepLinkDef) = ConfigItem(def, ConfigScope.LOCAL, ConfigState.LOCAL)

    private val kyc = DeepLinkDef(
        id = "d1",
        name = "Verify",
        module = "kyc",
        uriTemplate = "app://kyc/verify",
        params = listOf(DeepLinkParam(name = "step", type = ParamType.ENUM, allowedValues = listOf("selfie"))),
    )
    private val cardsHome = DeepLinkDef(id = "d2", name = "Home", module = "cards", uriTemplate = "app://cards/home")
    private val cardsBlock = DeepLinkDef(id = "d3", name = "Block", module = "cards", uriTemplate = "app://cards/block")

    private val state = DeepLinkCatalogUiState(defs = listOf(kyc, cardsHome, cardsBlock).map(::item))

    @Test
    fun `a blank query orders by module then name`() {
        val ids = state.visibleDefs.map { it.value.id }
        assertEquals(listOf("d3", "d2", "d1"), ids) // cards/Block, cards/Home, kyc/Verify
    }

    @Test
    fun `a query matches across name module and template`() {
        assertEquals(listOf("d1"), state.copy(query = "kyc").visibleDefs.map { it.value.id })
        assertEquals(setOf("d2", "d3"), state.copy(query = "cards").visibleDefs.map { it.value.id }.toSet())
    }

    @Test
    fun `a query matches an enum allowed value`() {
        assertTrue(state.copy(query = "selfie").visibleDefs.any { it.value.id == "d1" })
    }

    @Test
    fun `a non matching query returns nothing`() {
        assertTrue(state.copy(query = "zzz").visibleDefs.isEmpty())
    }

    @Test
    fun `module names and counts cover every module`() {
        assertEquals(listOf("cards", "kyc"), state.moduleNames)
        assertEquals(mapOf("cards" to 2, "kyc" to 1), state.moduleCounts)
    }

    @Test
    fun `module groups partition the visible defs by module`() {
        val groups = state.moduleGroups
        assertEquals(listOf("cards", "kyc"), groups.map { it.module })
        assertEquals(listOf("d3", "d2"), groups.first().defs.map { it.value.id })
    }

    @Test
    fun `a module filter narrows to that module`() {
        val ids = state.copy(moduleFilter = "cards").visibleDefs.map { it.value.id }
        assertEquals(listOf("d3", "d2"), ids)
    }
}
