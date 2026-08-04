package io.github.yashkasera.alohomora.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Geometry and dismissal for the shared side sheet.
 *
 * Worth testing because both the Traffic and Traces sheets now route through it, so a regression here
 * breaks two panels at once. The specific thing under test is that `fillMaxWidth(widthFraction)` on the
 * surface actually yields that fraction while sharing a `Row` with a `weight(1f)` scrim — a layout
 * interaction that reads plausibly either way and is only settled by measuring.
 */
@OptIn(ExperimentalTestApi::class)
class AlohomoraSideSheetTest {

    @Test
    fun `the sheet occupies the requested fraction of the window`() = runComposeUiTest {
        setContent {
            AlohomoraTheme {
                AlohomoraSideSheet(
                    visible = true,
                    onDismiss = {},
                    widthFraction = 0.7f,
                    header = { Text("header") },
                    modifier = Modifier.testTag("root"),
                ) {
                    Text("body", modifier = Modifier.fillMaxSize().testTag("body"))
                }
            }
        }

        val rootWidth = onNodeWithTag("root").fetchSemanticsNode().size.width
        val bodyWidth = onNodeWithTag("body", useUnmergedTree = true).fetchSemanticsNode().size.width

        assertTrue(rootWidth > 0, "root did not lay out")
        val ratio = bodyWidth.toFloat() / rootWidth.toFloat()
        assertTrue(
            abs(ratio - 0.7f) < 0.02f,
            "expected the sheet to take ~70% of the window, got ${(ratio * 100).toInt()}%",
        )
    }

    @Test
    fun `a narrower fraction leaves more room for the scrim`() = runComposeUiTest {
        setContent {
            AlohomoraTheme {
                AlohomoraSideSheet(
                    visible = true,
                    onDismiss = {},
                    widthFraction = 0.5f,
                    header = { Text("header") },
                    modifier = Modifier.testTag("root"),
                ) {
                    Text("body", modifier = Modifier.fillMaxSize().testTag("body"))
                }
            }
        }

        val rootWidth = onNodeWithTag("root").fetchSemanticsNode().size.width
        val bodyWidth = onNodeWithTag("body", useUnmergedTree = true).fetchSemanticsNode().size.width

        val ratio = bodyWidth.toFloat() / rootWidth.toFloat()
        assertTrue(abs(ratio - 0.5f) < 0.02f, "got ${(ratio * 100).toInt()}%")
    }

    @Test
    fun `nothing renders while the sheet is hidden`() = runComposeUiTest {
        var visible by mutableStateOf(false)

        setContent {
            AlohomoraTheme {
                AlohomoraSideSheet(
                    visible = visible,
                    onDismiss = {},
                    header = { Text("header") },
                ) {
                    Text("body")
                }
            }
        }

        assertTrue(
            onAllNodesWithText("body").fetchSemanticsNodes().isEmpty(),
            "a hidden sheet must not compose its content",
        )

        visible = true
        waitForIdle()

        onNodeWithText("body").assertIsDisplayed()
        onNodeWithText("header").assertIsDisplayed()
    }
}
