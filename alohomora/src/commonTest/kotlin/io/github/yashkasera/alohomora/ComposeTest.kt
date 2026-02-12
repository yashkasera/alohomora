package io.github.yashkasera.alohomora

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ComposeTest {

    @Test
    fun simpleCheck() = runComposeUiTest {
        setContent {
            var txt by remember { mutableStateOf("Go") }
            Column {
                Text(
                    text = txt,
                    modifier = Modifier.testTag("t_text")
                )
                AlohomoraFilledButton(
                    text = "click me",
                    onClick = { txt += "." },
                    modifier = Modifier.testTag("t_button"),
                )
            }
        }

        onNodeWithTag("t_button").apply {
            repeat(3) { performClick() }
        }
        onNodeWithTag("t_text").assertTextEquals("Go...")
    }
}
