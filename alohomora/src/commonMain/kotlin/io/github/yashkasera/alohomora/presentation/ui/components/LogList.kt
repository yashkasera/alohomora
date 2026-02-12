package io.github.yashkasera.alohomora.presentation.ui.components

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
