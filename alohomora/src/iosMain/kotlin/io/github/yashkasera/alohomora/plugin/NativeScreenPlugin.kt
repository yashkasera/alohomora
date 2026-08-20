package io.github.yashkasera.alohomora.plugin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIViewController

fun interface NativeScreenViewControllerProvider {
    fun createViewController(): UIViewController
}

class NativeScreenPlugin(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val showInDashboard: Boolean = true,
    override val showInNavigation: Boolean = false,
    override val priority: Int = 100,
    override val actions: List<PluginAction> = emptyList(),
    private val viewControllerProvider: NativeScreenViewControllerProvider,
) : CustomScreenPlugin {

    @OptIn(ExperimentalForeignApi::class)
    @Composable
    override fun Content() {
        UIKitViewController(
            factory = { viewControllerProvider.createViewController() },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                isInteractive = true,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
