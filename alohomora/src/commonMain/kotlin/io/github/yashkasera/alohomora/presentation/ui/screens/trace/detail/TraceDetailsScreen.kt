package io.github.yashkasera.alohomora.presentation.ui.screens.trace.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.presentation.ui.components.ShareBottomSheet
import io.github.yashkasera.alohomora.presentation.ui.components.SlackShareBottomSheet
import io.github.yashkasera.alohomora.presentation.ui.components.SlackShareOption
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Share
import io.github.yashkasera.alohomora.ui.icons.Slack
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TraceDetailsScreen(traceId: String, onBackClick: () -> Unit = {}) {
    val viewModel = koinViewModel<TraceDetailsViewModel> { parametersOf(traceId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val trace = state.trace

    if (trace == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = if (state.isLoading) "Loading trace..." else "Trace not found.",
                color = if (state.isLoading) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AlohomoraTopBar(
                title = "API Request",
                subtitle = state.trace?.path,
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.ArrowLeft,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    if (state.isSlackConfigured) {
                        AlohomoraIconButton(
                            onClick = viewModel::showSlackSheet,
                            content = {
                                Icon(
                                    imageVector = Icons.Slack,
                                    contentDescription = "Slack",
                                )
                            },
                        )
                    }
                    AlohomoraIconButton(
                        onClick = viewModel::showShareSheet,
                        content = {
                            Icon(
                                imageVector = Icons.Share,
                                contentDescription = "Share",
                            )
                        },
                    )
                },
            )
        },
    ) { padding ->
        TraceDetailsContent(
            trace = trace,
            modifier = Modifier.padding(padding),
        )
    }

    // Share Bottom Sheet
    if (state.showShareSheet) {
        ShareBottomSheet(
            onDismiss = viewModel::hideShareSheet,
            onShareCurl = viewModel::shareCurlViaSystem,
            onShareText = viewModel::shareTextViaSystem,
            onShareFile = viewModel::shareFileViaSystem,
        )
    }

    // Slack Share Bottom Sheet
    if (state.showSlackSheet) {
        SlackShareBottomSheet(
            title = "Share to Slack",
            isConfigured = state.isSlackConfigured,
            onDismiss = viewModel::hideSlackSheet,
            shareOptions = listOf(
                SlackShareOption(
                    icon = Icons.Share,
                    title = "Share cURL to Slack",
                    subtitle = "Send curl command",
                    onShare = viewModel::shareCurlToSlack,
                ),
                SlackShareOption(
                    icon = Icons.Copy,
                    title = "Share Text to Slack",
                    subtitle = "Send raw transaction text",
                    onShare = viewModel::shareTextToSlack,
                ),
            ),
        )
    }
}
