package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.presentation.ui.components.ShareBottomSheet
import io.github.yashkasera.alohomora.presentation.ui.components.SlackShareBottomSheet
import io.github.yashkasera.alohomora.presentation.ui.components.SlackShareOption
import io.github.yashkasera.alohomora.presentation.ui.components.rememberClipboardCopy
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Repeat
import io.github.yashkasera.alohomora.ui.icons.Share
import io.github.yashkasera.alohomora.ui.icons.Slack
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TrafficDetailsScreen(
    trafficId: String,
    onBackClick: () -> Unit = {},
    onOpenTraffic: (String) -> Unit = {},
    onReplay: (String) -> Unit = {},
) {
    val viewModel = koinViewModel<TrafficDetailsViewModel> { parametersOf(trafficId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val trace = state.trace
    val clipboardCopy = rememberClipboardCopy()

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
        snackbarHost = { SnackbarHost(clipboardCopy.snackbarHostState) },
        topBar = {
            AlohomoraTopBar(
                title = "Traffic Log",
                subtitle = state.trace?.path,
                navigationIcon = {
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                    ) {
                        Icon(
                            imageVector = Icons.ArrowLeft,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    if (state.canReplay) {
                        AlohomoraIconButton(
                            onClick = { onReplay(trafficId) },
                            modifier = Modifier.testTag(AlohomoraTestTags.TrafficDetails.REPLAY),
                            content = {
                                Icon(
                                    imageVector = Icons.Repeat,
                                    contentDescription = "Replay",
                                )
                            },
                        )
                    }
                    if (state.isSlackConfigured) {
                        AlohomoraIconButton(
                            onClick = viewModel::showSlackSheet,
                            modifier = Modifier.testTag(
                                AlohomoraTestTags.TrafficDetails.SHARE_SLACK,
                            ),
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
                        modifier = Modifier.testTag(AlohomoraTestTags.TrafficDetails.SHARE),
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
        Column(
            modifier = Modifier
                .padding(padding)
                .testTag(AlohomoraTestTags.TrafficDetails.ROOT),
        ) {
            state.replayResultTraceId?.let { resultId ->
                ReplayResultBanner(onClick = { onOpenTraffic(resultId) })
            }
            TrafficDetailsContent(trace = trace, onCopy = clipboardCopy.copy)
        }
    }

    if (state.showShareSheet) {
        ShareBottomSheet(
            onDismiss = viewModel::hideShareSheet,
            onShareCurl = viewModel::shareCurlViaSystem,
            onShareText = viewModel::shareTextViaSystem,
            onShareFile = viewModel::shareFileViaSystem,
        )
    }

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

/** Links a trace to the response its replay produced. */
@Composable
private fun ReplayResultBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlohomoraTestTags.TrafficDetails.REPLAY_RESULT_BANNER)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.md,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Repeat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
        )
        Text(
            text = "This request was replayed. Tap to open the result.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Preview
@Composable
private fun ReplayResultBannerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReplayResultBanner(onClick = {})
        }
    }
}
