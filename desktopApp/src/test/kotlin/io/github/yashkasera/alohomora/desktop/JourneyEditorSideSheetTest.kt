package io.github.yashkasera.alohomora.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyMatcher
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.JourneyEditorSideSheet
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import kotlin.test.Test

/**
 * Composition tests for the editor. The one invariant a unit test cannot reach: the editor's single
 * LazyColumn renders the steps section and the validation-report section together, and both are keyed
 * off the same step ids — so without namespaced keys it throws a duplicate-key error at measure time.
 */
@OptIn(ExperimentalTestApi::class)
class JourneyEditorSideSheetTest {

    private val draft = JourneyDefinition(
        id = "j1",
        name = "Checkout",
        steps = listOf(
            JourneyStep(id = "s1", eventName = "login"),
            JourneyStep(id = "s2", eventName = "pay"),
        ),
    )
    private val events = listOf(
        Event(name = "login", properties = null, time = 1L),
        Event(name = "pay", properties = null, time = 2L),
    )

    @Test
    fun `renders steps and a report together without a duplicate key crash`() = runComposeUiTest {
        val report = JourneyMatcher.evaluate(draft, events, evaluatedAt = 0L)

        setContent {
            AppTheme {
                JourneyEditorSideSheet(
                    draft = draft,
                    report = report,
                    recentEvents = events,
                    onNameChange = {},
                    onDescriptionChange = {},
                    onToggleOrdered = {},
                    onAddStepFromEvent = {},
                    onRemoveStep = {},
                    onStepSelectorChange = { _, _ -> },
                    onStepAssertionsChange = { _, _ -> },
                    onStepOnRepeatChange = { _, _ -> },
                    onValidate = {},
                    onValidateLive = {},
                    onDismiss = {},
                )
            }
        }

        // Reaching an assertion at all means measure/layout succeeded with unique keys.
        onNodeWithText("Edit journey").assertIsDisplayed()
    }
}
