package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.replay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.replay.ReplayBlockedReason
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditor
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditorState
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.Repeat
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class BodyMode(val label: String) {
    JSON("JSON"),
    TEXT("Text"),
}

@Composable
internal fun ReplayScreen(
    trafficId: String,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<ReplayViewModel> { parametersOf(trafficId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.replaySent) {
        if (state.replaySent) onBackClick()
    }

    ReplayScreenContent(
        state = state,
        onBackClick = onBackClick,
        onMethodChange = viewModel::updateMethod,
        onUrlChange = viewModel::updateUrl,
        onHeadersChange = viewModel::updateHeaders,
        onBodyChange = viewModel::updateBody,
        onSend = viewModel::send,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReplayScreenContent(
    state: ReplayState,
    onBackClick: () -> Unit,
    onMethodChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onHeadersChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Replay",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
            )
        },
        bottomBar = {
            // `sourceTrace != null` keeps Send off the "Traffic entry not found" state, where the
            // old top-bar button still rendered with nothing to send.
            if (!state.isLoading && state.blockedReason == null && state.sourceTrace != null) {
                SendBar(
                    isReplaying = state.isReplaying,
                    enabled = !state.isReplaying
                        && state.url.isNotBlank()
                        && state.method.isNotBlank(),
                    onSend = onSend,
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                AlohomoraCircularProgressIndicator()
            }

            state.sourceTrace == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Traffic entry not found.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.blockedReason != null -> BlockedContent(
                reason = state.blockedReason,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> EditorContent(
                state = state,
                onMethodChange = onMethodChange,
                onUrlChange = onUrlChange,
                onHeadersChange = onHeadersChange,
                onBodyChange = onBodyChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

/**
 * Sticky bottom Send bar. A `bottomBar` rather than a content overlay so the Scaffold reserves its
 * height — the field-heavy editor can never scroll beneath it — and `imePadding` keeps it riding
 * above the keyboard.
 */
@Composable
private fun SendBar(
    isReplaying: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = MaterialTheme.dimens.margin.lg)
            .padding(vertical = MaterialTheme.dimens.margin.md),
    ) {
        AlohomoraFilledButton(
            text = if (isReplaying) "Sending…" else "Send",
            onClick = onSend,
            enabled = enabled,
            size = AlohomoraButtonSize.LARGE,
            leadingIcon = if (isReplaying) {
                {
                    AlohomoraCircularProgressIndicator(
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        strokeWidth = MaterialTheme.dimens.stroke.medium,
                    )
                }
            } else {
                {
                    Icon(
                        Icons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AlohomoraTestTags.ReplaySheet.SEND),
        )
    }
}

@Composable
private fun BlockedContent(
    reason: ReplayBlockedReason,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AlohomoraCard(
            shape = MaterialTheme.shapes.extraLarge,
            colors = AlohomoraCardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.padding(MaterialTheme.dimens.margin.xxxl),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.dimens.icon.xl)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AlertTriangle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                }
                Text(
                    text = "Replay unavailable",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = when (reason) {
                        ReplayBlockedReason.INCOMPLETE_TRACE ->
                            "This request is missing its URL or method and cannot be replayed."

                        ReplayBlockedReason.UNPARSEABLE_BODY ->
                            "The request body was multipart or streaming and cannot be reconstructed."

                        ReplayBlockedReason.TRUNCATED_BODY ->
                            "The request body exceeded the capture limit. Replaying would send corrupted data."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditorContent(
    state: ReplayState,
    onMethodChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onHeadersChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isJsonContent = state.contentType.orEmpty().contains("json", ignoreCase = true)
    var bodyMode by remember { mutableStateOf(if (isJsonContent) BodyMode.JSON else BodyMode.TEXT) }
    val jsonBodyState = remember { JsonEditorState(state.body) }
    var textBody by remember { mutableStateOf(state.body) }

    Column(
        modifier = modifier
            .testTag(AlohomoraTestTags.ReplaySheet.ROOT)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
    ) {
        Spacer(Modifier.height(MaterialTheme.dimens.margin.xs))

        // Hero: method + URL
        AlohomoraCard(
            shape = MaterialTheme.shapes.extraLarge,
            colors = AlohomoraCardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(MaterialTheme.dimens.icon.standard)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                        )
                    }
                    Text(
                        text = "Request",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                AlohomoraTextField(
                    value = state.method,
                    onValueChange = onMethodChange,
                    label = "Method",
                    singleLine = true,
                    enabled = !state.isReplaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AlohomoraTestTags.ReplaySheet.METHOD),
                )
                AlohomoraTextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    label = "URL",
                    singleLine = false,
                    enabled = !state.isReplaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AlohomoraTestTags.ReplaySheet.URL),
                )
            }
        }

        // Headers section
        SectionHeader(title = "Headers")
        AlohomoraTextField(
            value = state.headers,
            onValueChange = onHeadersChange,
            placeholder = "Accept: application/json",
            singleLine = false,
            enabled = !state.isReplaying,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AlohomoraTestTags.ReplaySheet.HEADERS),
        )

        // Body section
        SectionHeader(title = "Body")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            BodyMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = bodyMode == mode,
                    enabled = !state.isReplaying,
                    onClick = {
                        if (bodyMode != mode) {
                            if (mode == BodyMode.JSON) jsonBodyState.setText(textBody)
                            else textBody = jsonBodyState.text
                            bodyMode = mode
                            val currentText = when (mode) {
                                BodyMode.JSON -> jsonBodyState.text
                                BodyMode.TEXT -> textBody
                            }
                            onBodyChange(currentText)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = BodyMode.entries.size,
                        baseShape = MaterialTheme.shapes.small,
                    ),
                ) {
                    Text(mode.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        when (bodyMode) {
            // Only one body editor composes at a time, so the shared BODY tag is unambiguous.
            BodyMode.JSON -> JsonEditor(
                state = jsonBodyState,
                readOnly = state.isReplaying,
                minLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .testTag(AlohomoraTestTags.ReplaySheet.BODY),
            )

            BodyMode.TEXT -> AlohomoraTextField(
                value = textBody,
                onValueChange = {
                    textBody = it
                    onBodyChange(it)
                },
                placeholder = "Request body",
                singleLine = false,
                enabled = !state.isReplaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .testTag(AlohomoraTestTags.ReplaySheet.BODY),
            )
        }

        // Error banner
        AnimatedVisibility(
            visible = !state.replayError.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            AlohomoraCard(
                shape = MaterialTheme.shapes.large,
                colors = AlohomoraCardDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.testTag(AlohomoraTestTags.ReplaySheet.ERROR),
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AlertTriangle,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                    Text(
                        text = state.replayError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // The bottomBar reserves its own height, so only a modest trailing gap is needed.
        Spacer(Modifier.height(MaterialTheme.dimens.margin.lg))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

// ── Previews ────────────────────────────────────────────────────────────────

private val previewTrace = TrafficEntry(
    id = "preview-1",
    method = "POST",
    url = "https://api.example.com/v2/users/profile",
    host = "api.example.com",
    path = "/v2/users/profile",
    status = 200,
)

@Preview
@Composable
private fun ReplayEditorPreview() {
    AppTheme {
        ReplayScreenContent(
            state = ReplayState(
                isLoading = false,
                sourceTrace = previewTrace,
                method = "POST",
                url = "https://api.example.com/v2/users/profile",
                headers = "Accept: application/json\nContent-Type: application/json",
                body = "{\"name\": \"Jane Doe\", \"email\": \"jane@example.com\"}",
                contentType = "application/json",
            ),
            onBackClick = {},
            onMethodChange = {},
            onUrlChange = {},
            onHeadersChange = {},
            onBodyChange = {},
            onSend = {},
        )
    }
}

@Preview
@Composable
private fun ReplayErrorPreview() {
    AppTheme {
        ReplayScreenContent(
            state = ReplayState(
                isLoading = false,
                sourceTrace = previewTrace,
                method = "GET",
                url = "https://api.example.com/v2/health",
                headers = "",
                body = "",
                replayError = "Connection refused: host unreachable",
            ),
            onBackClick = {},
            onMethodChange = {},
            onUrlChange = {},
            onHeadersChange = {},
            onBodyChange = {},
            onSend = {},
        )
    }
}

@Preview
@Composable
private fun ReplaySendingPreview() {
    AppTheme {
        ReplayScreenContent(
            state = ReplayState(
                isLoading = false,
                sourceTrace = previewTrace,
                method = "POST",
                url = "https://api.example.com/v2/users/profile",
                headers = "Accept: application/json",
                body = "{}",
                isReplaying = true,
            ),
            onBackClick = {},
            onMethodChange = {},
            onUrlChange = {},
            onHeadersChange = {},
            onBodyChange = {},
            onSend = {},
        )
    }
}

@Preview
@Composable
private fun ReplayBlockedPreview() {
    AppTheme {
        ReplayScreenContent(
            state = ReplayState(
                isLoading = false,
                sourceTrace = previewTrace,
                blockedReason = ReplayBlockedReason.TRUNCATED_BODY,
            ),
            onBackClick = {},
            onMethodChange = {},
            onUrlChange = {},
            onHeadersChange = {},
            onBodyChange = {},
            onSend = {},
        )
    }
}
