package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.presentation.theme.CanvasBlack
import io.github.yashkasera.alohomora.presentation.theme.CanvasLightGray
import io.github.yashkasera.alohomora.presentation.theme.CanvasWhite
import io.github.yashkasera.alohomora.presentation.theme.CanvasDarkGray

/*@Composable
fun LogList(
//    logs: List<LogEntity>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasWhite)
            .padding(16.dp)
    ) {
        Text(
            "SYSTEM LOGS",
            style = MaterialTheme.typography.labelMedium,
            color = CanvasDarkGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Divider(color = CanvasBlack, thickness = 2.dp)

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(logs) { log ->
                LogLine(log)
                Divider(color = CanvasLightGray, thickness = 1.dp)
            }
        }
    }
}*/

/*@Composable
private fun LogLine(log: LogEntity) {
    val isError = log.level.name == "ERROR"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
    ) {
        Text(
            text = log.level.name.take(1),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = if (isError) CanvasBlack else CanvasDarkGray,
            modifier = Modifier.width(24.dp)
        )

        Text(
            text = "${log.tag}: ",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = CanvasBlack
        )

        Text(
            text = log.message,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = CanvasDarkGray
        )

        if (isError) {
             Text(
                text = " [!]",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = CanvasBlack
            )
        }
    }
}*/
