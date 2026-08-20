package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.runtime.Composable
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.plugin.InternalPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private open class FakePlugin(override val id: String) : CustomScreenPlugin {
    override val title: String get() = "Plugin $id"

    @Composable
    override fun Content() = Unit
}

private class FakeInternalPlugin(id: String) : FakePlugin(id), InternalPlugin

class OverviewModulesTest {

    @Test
    fun `two dashboard plugins produce distinct grid keys`() {
        val (internal, custom) = partitionDashboardModules(
            listOf(FakeInternalPlugin("alohomora_navigation_plugin"), FakePlugin("feature_flags")),
        )

        val keys = (builtInModules + internal + custom).map { it.gridKey }

        // LazyVerticalGrid throws on a duplicate key, which used to take the whole Overview
        // screen down as soon as a consumer registered any dashboard plugin.
        assertEquals(keys.size, keys.toSet().size, "duplicate grid keys: $keys")
        assertTrue("Extension:alohomora_navigation_plugin" in keys)
        assertTrue("Extension:feature_flags" in keys)
    }

    @Test
    fun `built-in modules key off their route class name`() {
        assertEquals(
            builtInModules.map { it.route::class.simpleName },
            builtInModules.map { it.gridKey },
        )
    }

    @Test
    fun `only consumer-registered plugins land in custom modules`() {
        val (internal, custom) = partitionDashboardModules(
            listOf(FakeInternalPlugin("alohomora_navigation_plugin"), FakePlugin("feature_flags")),
        )

        assertEquals(listOf("Plugin alohomora_navigation_plugin"), internal.map { it.title })
        assertEquals(listOf("Plugin feature_flags"), custom.map { it.title })
    }

    @Test
    fun `custom modules is empty when only internal plugins are registered`() {
        val (internal, custom) = partitionDashboardModules(
            listOf(FakeInternalPlugin("alohomora_navigation_plugin")),
        )

        assertEquals(1, internal.size)
        assertTrue(custom.isEmpty())
    }
}
