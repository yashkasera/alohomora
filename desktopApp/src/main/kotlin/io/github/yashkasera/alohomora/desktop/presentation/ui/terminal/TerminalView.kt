package io.github.yashkasera.alohomora.desktop.presentation.ui.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TerminalView(terminal: LocalTerminal) {
    val scope = rememberCoroutineScope()
    var output by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            while (terminal.isAlive()) {
                val line = terminal.readLine() ?: break
                output += line + "\n"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(12.dp),
    ) {
        SelectionContainer(
            modifier = Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = output,
            )
        }

        AlohomoraTextField(
            value = input,
            onValueChange = { input = it },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    terminal.write(input + "\n")
                    input = ""
                },
            ) {
                terminal.write(input + "\n")
                input = ""
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        AlohomoraFilledButton(
            text = "Run",
            onClick = {
                terminal.write(input + "\n")
                input = ""
            },
        )
    }
}
