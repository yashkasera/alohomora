package io.github.yashkasera.alohomora.desktop

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventDetailsContent
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

/**
 * The Events sheet body, composed inside the real side sheet.
 *
 * Exists because this subtree shipped a crash that no unit test could reach: it holds the sheet's only
 * vertically scrollable component, and a scrollable measured with an infinite maximum height makes Compose
 * throw rather than mislay out. Only driving a real composition catches that, and only through the real
 * [AlohomoraSideSheet] — the constraints the sheet hands its content slot are the whole question.
 */
@OptIn(ExperimentalTestApi::class)
class EventDetailsContentTest {

    private fun event() = Event(
        id = 1,
        name = "App.Exception",
        properties = Json.encodeToJsonElement(mapOf("message" to "boom")),
        time = 10_000,
    )

    /** A payload far taller than the sheet, so the scroll is exercised rather than incidental. */
    private fun tallProperties() = (1..400).joinToString("\n") { "  \"key$it\": \"value$it\"," }

    @Test
    fun `a tall payload lays out inside the sheet instead of throwing`() = runComposeUiTest {
        setContent {
            AlohomoraTheme {
                AlohomoraSideSheet(
                    visible = true,
                    onDismiss = {},
                    header = { Text("header") },
                    modifier = Modifier.testTag("root"),
                ) {
                    EventDetailsContent(
                        event = event(),
                        properties = tallProperties(),
                        isMuted = false,
                        onToggleMute = {},
                        onSolo = {},
                    )
                }
            }
        }

        val sheetHeight = onNodeWithTag("root").fetchSemanticsNode().size.height
        assertTrue(sheetHeight > 0, "the sheet did not lay out")

        // The body must fit the sheet. Measured unbounded it would grow to the payload's full height,
        // which is what the infinite-constraint crash was the symptom of.
        val nameRow = onNodeWithText("App.Exception").fetchSemanticsNode()
        assertTrue(
            nameRow.boundsInRoot.bottom <= sheetHeight.toFloat(),
            "the body overflowed the sheet, so it was measured unbounded",
        )
    }

    @Test
    fun `the event name and read state are shown`() = runComposeUiTest {
        setContent {
            AlohomoraTheme {
                AlohomoraSideSheet(visible = true, onDismiss = {}, header = { Text("header") }) {
                    EventDetailsContent(
                        event = event().copy(isViewed = true),
                        properties = "{}",
                        isMuted = false,
                        onToggleMute = {},
                        onSolo = {},
                    )
                }
            }
        }

        onNodeWithText("App.Exception").assertIsDisplayed()
        onNodeWithText("Yes").assertIsDisplayed()
    }

    @Test
    fun `the mute button reports the current state and both actions fire`() = runComposeUiTest {
        var muted = 0
        var soloed = 0

        setContent {
            AlohomoraTheme {
                AlohomoraSideSheet(visible = true, onDismiss = {}, header = { Text("header") }) {
                    EventDetailsContent(
                        event = event(),
                        properties = "{}",
                        // Already muted, so the label must offer the way out rather than repeat the action.
                        isMuted = true,
                        onToggleMute = { muted++ },
                        onSolo = { soloed++ },
                    )
                }
            }
        }

        onNodeWithText("Unmute this event", ignoreCase = true).performClick()
        onNodeWithText("Show only this", ignoreCase = true).performClick()
        waitForIdle()

        assertEquals(1, muted)
        assertEquals(1, soloed)
    }
}
