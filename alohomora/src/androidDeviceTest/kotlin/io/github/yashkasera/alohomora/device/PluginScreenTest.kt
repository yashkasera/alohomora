package io.github.yashkasera.alohomora.device

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.screens.navigation.NavigationPlugin
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Overview
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val TEST_PLUGIN_ID = "alohomora_device_test_plugin"

/** Deliberately not a substring of any built-in module label, so a text match is unambiguous. */
private const val TEST_PLUGIN_LABEL = "Widget Workbench"

private const val TEST_PLUGIN_CONTENT = "alohomora_device_test_plugin_content"

private const val LAST_BUILT_IN_LABEL = "Git History"

private object TestPlugin : CustomScreenPlugin {
    override val id: String = TEST_PLUGIN_ID
    override val title: String = TEST_PLUGIN_LABEL
    override val description: String = "PLUGIN UNDER TEST"

    @Composable
    override fun Content() {
        Text("workbench body", modifier = Modifier.testTag(TEST_PLUGIN_CONTENT))
    }
}

/**
 * `CustomScreenPlugin` end to end: registration puts a card on Overview, the card routes to the
 * plugin's own composable, and unregistering takes it away again.
 */
class PluginScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Before
    fun registerTheTestPlugin() {
        Alohomora.registerPlugin(TestPlugin)
    }

    /**
     * `PluginRegistry` is process-global and nothing in `ConsoleTestRule` clears it. A test plugin
     * left registered here would collide in every later class that renders Overview.
     */
    @After
    fun unregisterTheTestPlugin() {
        Alohomora.unregisterPlugin(TEST_PLUGIN_ID)
    }

    @Test
    fun aRegisteredPluginGetsACardOnOverview() {
        compose.launchConsole(Routes.Overview)

        val tag = Overview.moduleCard("Extension:$TEST_PLUGIN_ID")
        compose.onNodeWithTag(Overview.GRID)
            .performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test
    fun tappingThePluginCardRendersThePluginsOwnContent() {
        compose.launchConsole(Routes.Overview)

        val tag = Overview.moduleCard("Extension:$TEST_PLUGIN_ID")
        compose.onNodeWithTag(Overview.GRID)
            .performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(TEST_PLUGIN_CONTENT).assertIsDisplayed()
    }

    @Test
    fun unregisteringRemovesThePluginCard() {
        Alohomora.unregisterPlugin(TEST_PLUGIN_ID)

        compose.launchConsole(Routes.Overview)

        compose.onNodeWithTag(Overview.GRID)
            .performScrollToNode(hasText(LAST_BUILT_IN_LABEL))
        compose.onNodeWithText(TEST_PLUGIN_LABEL).assertDoesNotExist()
    }

    /**
     * The case that used to crash: two dashboard plugins both routing to `Routes.Extension`,
     * giving `LazyVerticalGrid` two items keyed `Extension`. `NavigationPlugin` is registered
     * by `AlohomoraInitializer` at app start; `TestPlugin` is added in `@Before`. Their
     * coexistence is the assertion.
     */
    @Test
    fun twoDashboardPluginsCoexistOnOverview() {
        compose.launchConsole(Routes.Overview)

        val testTag = Overview.moduleCard("Extension:$TEST_PLUGIN_ID")
        compose.onNodeWithTag(Overview.GRID)
            .performScrollToNode(hasTestTag(testTag))
        compose.onNodeWithTag(testTag).assertIsDisplayed()

        val navTag = Overview.moduleCard("Extension:${NavigationPlugin.id}")
        compose.onNodeWithTag(Overview.GRID)
            .performScrollToNode(hasTestTag(navTag))
        compose.onNodeWithTag(navTag).assertIsDisplayed()
    }
}
