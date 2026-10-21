package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable
import platform.UIKit.UIViewController

/** No-op mirror of `:alohomora`'s `NativeScreenViewControllerProvider`. */
fun interface NativeScreenViewControllerProvider {
    fun createViewController(): UIViewController
}

/**
 * No-op mirror of `:alohomora`'s `NativeScreenPlugin`.
 *
 * Renders nothing and never invokes [viewControllerProvider], so a release build pays no cost.
 * Must match the debug constructor signature exactly.
 */
@Suppress("unused")
class NativeScreenPlugin(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val showInDashboard: Boolean = true,
    override val showInNavigation: Boolean = false,
    override val priority: Int = 100,
    override val actions: List<PluginAction> = emptyList(),
    override val dataFields: List<PluginDataField> = emptyList(),
    @Suppress("UNUSED_PARAMETER") viewControllerProvider: NativeScreenViewControllerProvider,
) : CustomScreenPlugin {

    @Composable
    override fun Content() = Unit
}
