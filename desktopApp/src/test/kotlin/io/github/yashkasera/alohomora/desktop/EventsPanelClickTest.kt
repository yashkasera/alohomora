package io.github.yashkasera.alohomora.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.prettyProperties
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventDetailsContent
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Clicking a row in the real Events panel.
 *
 * The reported crash was "Vertically scrollable component was measured with an infinity maximum height
 * constraints" on opening any event, and it survived three narrower tests that each composed only part of
 * the tree. This one drives the panel itself through [EventsViewModel], which is the only arrangement that
 * reproduces what a user actually does.
 */
@OptIn(ExperimentalTestApi::class)
class EventsPanelClickTest {

    private var viewModel: EventsViewModel? = null

    @AfterTest
    fun tearDown() {
        viewModel?.close()
    }

    private fun event(id: Long, name: String, keys: Int = 1) = Event(
        id = id,
        name = name,
        properties = Json.encodeToJsonElement((1..keys).associate { "key$it" to "value$it" }),
        time = 10_000 + id,
    )

    @Test
    fun `clicking a row marks it viewed and opens it`() = runComposeUiTest {
        val repository = FakeDevToolsRepository(
            events = listOf(event(1, "App.Exception"), event(2, "App.Start")),
        )
        val vm = EventsViewModel(repository).also { viewModel = it }

        setContent {
            AppTheme {
                // Mirrors DevToolsDesktopApp: the sheet is a sibling overlay in the same Box as the
                // panel, not a child of it. That arrangement is the thing under test.
                Box(modifier = Modifier.fillMaxSize()) {
                    EventsPanel(eventsViewModel = vm)
                    val selected by vm.selectedEvent.collectAsState()
                    AlohomoraSideSheet(
                        visible = selected != null,
                        onDismiss = vm::closeEvent,
                        widthFraction = 0.4f,
                        header = { Text("header") },
                    ) {
                        selected?.let {
                            EventDetailsContent(
                                event = it,
                                properties = it.prettyProperties(),
                                isMuted = false,
                                onToggleMute = {},
                                onSolo = {},
                            )
                        }
                    }
                }
            }
        }

        onNodeWithText("App.Exception").performClick()
        waitForIdle()

        assertEquals(listOf(1L), repository.viewedEventIds)
        assertEquals(1L, vm.selectedEventId.value)
    }

    /**
     * A large payload is the case that made the crash certain rather than incidental, since the row's code
     * block and the sheet's both grow with it.
     */
    @Test
    fun `clicking a row with a large payload still lays out`() = runComposeUiTest {
        val repository = FakeDevToolsRepository(events = listOf(event(1, "App.Exception", keys = 200)))
        val vm = EventsViewModel(repository).also { viewModel = it }

        setContent {
            AppTheme {
                // Mirrors DevToolsDesktopApp: the sheet is a sibling overlay in the same Box as the
                // panel, not a child of it. That arrangement is the thing under test.
                Box(modifier = Modifier.fillMaxSize()) {
                    EventsPanel(eventsViewModel = vm)
                    val selected by vm.selectedEvent.collectAsState()
                    AlohomoraSideSheet(
                        visible = selected != null,
                        onDismiss = vm::closeEvent,
                        widthFraction = 0.4f,
                        header = { Text("header") },
                    ) {
                        selected?.let {
                            EventDetailsContent(
                                event = it,
                                properties = it.prettyProperties(),
                                isMuted = false,
                                onToggleMute = {},
                                onSolo = {},
                            )
                        }
                    }
                }
            }
        }

        onNodeWithText("App.Exception").performClick()
        waitForIdle()

        assertEquals(1L, vm.selectedEventId.value)
    }
}
