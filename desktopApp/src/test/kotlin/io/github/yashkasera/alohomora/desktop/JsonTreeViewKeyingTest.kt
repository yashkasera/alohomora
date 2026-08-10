package io.github.yashkasera.alohomora.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression test for a stale-content bug in [JsonTreeView].
 *
 * It parsed and built its tree inside a bare `remember`, which survives an argument change in the same
 * composition slot. So swapping the payload in place — switching traffic entries inside an already-open
 * detail sheet, or selecting a second span's attributes — kept rendering the *first* document. Silent,
 * and from the outside indistinguishable from the device having sent the wrong body.
 *
 * Only a real composition catches this: the code reads correctly and the wrong `remember` compiles.
 */
@OptIn(ExperimentalTestApi::class)
class JsonTreeViewKeyingTest {

    @Test
    fun `swapping the json in place renders the new document`() = runComposeUiTest {
        var json by mutableStateOf("""{"first":"alpha"}""")

        setContent {
            AppTheme {
                JsonTreeView(json = json)
            }
        }

        onNodeWithText("first", substring = true).assertIsDisplayed()

        json = """{"second":"beta"}"""
        waitForIdle()

        onNodeWithText("second", substring = true).assertIsDisplayed()
        assertTrue(
            onAllNodesWithText("first", substring = true).fetchSemanticsNodes().isEmpty(),
            "the previous document must not survive a payload swap",
        )
    }
}
