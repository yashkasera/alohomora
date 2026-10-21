package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Download
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Share
import io.github.yashkasera.alohomora.ui.theme.dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareBottomSheet(
    onDismiss: () -> Unit,
    onShareCurl: () -> Unit,
    onShareText: () -> Unit,
    onShareFile: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AlohomoraBottomSheetModal(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                text = "Share",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.xxl),
            )

            ShareOption(
                icon = Icons.Share,
                title = "Share cURL",
                subtitle = "Share the curl command",
                onClick = {
                    onShareCurl()
                    onDismiss()
                },
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

            ShareOption(
                icon = Icons.Copy,
                title = "Share as Text",
                subtitle = "Share raw transaction text",
                onClick = {
                    onShareText()
                    onDismiss()
                },
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

            ShareOption(
                icon = Icons.Download,
                title = "Share as File",
                subtitle = "Share as .txt file",
                onClick = {
                    onShareFile()
                    onDismiss()
                },
            )
        }
    }
}
