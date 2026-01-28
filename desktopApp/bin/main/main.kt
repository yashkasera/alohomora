import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.yashkasera.alohomora.Alohomora
import java.awt.Dimension
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp

fun main() = application {
    Alohomora.init()
    val terminal = remember { LocalTerminal() }
    Window(
        title = "Alohomora",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
//        AlohomoraApp()
        TerminalView(terminal)

    }
}

