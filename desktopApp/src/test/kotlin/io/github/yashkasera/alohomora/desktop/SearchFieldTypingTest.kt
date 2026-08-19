package io.github.yashkasera.alohomora.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Typing into a panel's search field.
 *
 * The reported symptom was that the caret jumps to the start on every keystroke, so a typed word comes out
 * reversed. That is what `BasicTextField(value: String)` does when its value round-trips through a flow:
 * the field re-renders from a value that has not caught up yet, and the `String` overload has no selection
 * to restore, so the caret lands at index 0.
 *
 * Driven through the real [EventsViewModel] on purpose — the round trip is the bug, and a directly-held
 * `remember` string would not reproduce it.
 */
@OptIn(ExperimentalTestApi::class)
class SearchFieldTypingTest {

    private var viewModel: EventsViewModel? = null

    @AfterTest
    fun tearDown() {
        viewModel?.close()
    }

    private fun event(id: Long, name: String) = Event(
        id = id,
        name = name,
        properties = Json.encodeToJsonElement(mapOf("k" to "v")),
        time = 10_000 + id,
    )

    private fun runTyping(text: String, assert: (EventsViewModel) -> Unit) = runComposeUiTest {
        val repository = FakeDevToolsRepository(events = listOf(event(1, "App.Start")))
        val vm = EventsViewModel(repository).also { viewModel = it }

        setContent {
            AppTheme {
                Box(modifier = Modifier.fillMaxSize()) { EventsPanel(eventsViewModel = vm) }
            }
        }

        onNodeWithText("Search…").performClick()
        waitForIdle()

        // One character at a time, settling in between. A single performTextInput inserts the whole
        // string in one edit and so never exercises the round trip that loses the caret — which is the
        // entire bug.
        text.forEach { char ->
            // Located by semantics rather than a tag: the placeholder disappears once text is present.
            onNode(hasSetTextAction()).performTextInput(char.toString())
            waitForIdle()
        }

        assert(vm)
    }

    @Test
    fun `a typed word arrives in the order it was typed`() = runTyping("start") { vm ->
        // "trats" is the signature of a caret reset to index 0 on every keystroke.
        assertEquals("start", vm.uiState.value.filters.query)
    }

    @Test
    fun `a single character types correctly`() = runTyping("s") { vm ->
        assertEquals("s", vm.uiState.value.filters.query)
    }
}
