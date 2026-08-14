package io.github.yashkasera.alohomora.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.theme.AppTheme
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
            AppTheme {
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
        val bodyWidth =
            onNodeWithTag("body", useUnmergedTree = true).fetchSemanticsNode().size.width

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
            AppTheme {
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
        val bodyWidth =
            onNodeWithTag("body", useUnmergedTree = true).fetchSemanticsNode().size.width

        val ratio = bodyWidth.toFloat() / rootWidth.toFloat()
        assertTrue(abs(ratio - 0.5f) < 0.02f, "got ${(ratio * 100).toInt()}%")
    }

    /**
     * A scrollable code block in the content slot must lay out rather than throw.
     *
     * The exact shape the Events sheet shipped, and it crashed on opening any event: the sheet invokes
     * `content()` inside a `Column`, a `Column` measures non-weighted children with an unbounded main
     * axis, and `AlohomoraCodeBlock(isScrollable = true)` puts a `verticalScroll` at the bottom of that
     * chain — which Compose rejects outright with "Vertically scrollable component was measured with an
     * infinity maximum height constraints".
     *
     * Uses the real code block rather than a bare `Modifier.verticalScroll`, because the nesting is what
     * decides this: a hand-rolled `Column(verticalScroll)` in the same slot does *not* reproduce it, so a
     * simplified stand-in would pass while the shipped code crashed.
     *
     * The fix is available only because the slot is a `ColumnScope`, so content can take `weight(1f)`.
     */
    @Test
    fun `a scrollable code block in the content slot lays out instead of throwing`() =
        runComposeUiTest {
            setContent {
                AppTheme {
                    AlohomoraSideSheet(
                        visible = true,
                        onDismiss = {},
                        header = { Text("header") },
                        modifier = Modifier.testTag("root"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            AlohomoraCodeBlock(
                                content = (1..SCROLL_OVERFLOW_LINES).joinToString("\n") { "line $it" },
                                isScrollable = true,
                                modifier = Modifier.weight(1f).testTag("block"),
                            )
                        }
                    }
                }
            }

            val sheetHeight = onNodeWithTag("root").fetchSemanticsNode().size.height
            val blockHeight =
                onNodeWithTag("block", useUnmergedTree = true).fetchSemanticsNode().size.height

            assertTrue(blockHeight > 0, "the code block did not lay out")
            assertTrue(
                blockHeight <= sheetHeight,
                "the block took $blockHeight against a $sheetHeight sheet, so it was measured unbounded " +
                    "rather than clamped to the space left by the header",
            )
        }

    @Test
    fun `nothing renders while the sheet is hidden`() = runComposeUiTest {
        var visible by mutableStateOf(false)

        setContent {
            AppTheme {
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

    private companion object {
        /** Comfortably more lines than a test window is tall. */
        const val SCROLL_OVERFLOW_LINES = 200
    }
}
