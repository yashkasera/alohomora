package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Hero card for the DevTools server. The switch is the only toggle affordance — the card itself is
 * deliberately not clickable, so a stray tap while scrolling cannot flip the server.
 */
@Composable
internal fun OverviewStatusCard(
    state: OverviewState,
    onToggle: (Boolean) -> Unit,
    onPortChange: (String) -> Unit,
    onRememberDeviceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = state.deviceConnectionStatus
    val containerColor by animateColorAsState(
        targetValue = if (status == DevConnectionStatus.Connected) {
            MaterialTheme.alohomoraColors.successContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(),
    )

    AlohomoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AlohomoraTestTags.Overview.STATUS_CARD),
        shape = MaterialTheme.shapes.extraLarge,
        colors = AlohomoraCardDefaults.colors(containerColor = containerColor),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            StatusDecorShape(
                status = status,
                tint = when (status) {
                    DevConnectionStatus.Connected -> MaterialTheme.alohomoraColors.success
                    DevConnectionStatus.AwaitingAuth -> MaterialTheme.alohomoraColors.info
                    else -> MaterialTheme.alohomoraColors.accent
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 36.dp, y = (-36).dp)
                    .size(140.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.dimens.margin.xxl),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    ConnectionStatusDot(
                        state = when (status) {
                            DevConnectionStatus.Connected -> ConnectionDotState.Connected
                            DevConnectionStatus.AwaitingAuth -> ConnectionDotState.Reconnecting
                            DevConnectionStatus.Disconnected,
                            DevConnectionStatus.Off,
                                -> ConnectionDotState.Disconnected
                        },
                        modifier = Modifier.testTag(AlohomoraTestTags.Overview.STATUS_DOT),
                    )
                    Text(
                        "DEVTOOLS SERVER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    AlohomoraSwitch(
                        checked = state.serverEnabled,
                        onCheckedChange = onToggle,
                    )
                }
                AnimatedContent(
                    targetState = status,
                    transitionSpec = {
                        (
                            slideInVertically(
                                spring(dampingRatio = 0.6f, stiffness = 400f),
                            ) { it / 3 } + fadeIn()
                            ) togetherWith (slideOutVertically { -it / 3 } + fadeOut())
                    },
                ) { target ->
                    Text(
                        text = when (target) {
                            DevConnectionStatus.Connected -> "Connected"
                            DevConnectionStatus.AwaitingAuth -> "Awaiting code"
                            DevConnectionStatus.Disconnected -> "Waiting for a client"
                            DevConnectionStatus.Off -> "Server off"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.size(MaterialTheme.dimens.margin.md))
                AnimatedContent(
                    targetState = status,
                    transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
                ) { target ->
                    when (target) {
                        DevConnectionStatus.Off -> OffDetail(state, onPortChange)
                        DevConnectionStatus.Disconnected -> DisconnectedDetail(state)
                        DevConnectionStatus.AwaitingAuth ->
                            AwaitingAuthDetail(state, onRememberDeviceChange)

                        DevConnectionStatus.Connected -> ConnectedDetail(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun OffDetail(
    state: OverviewState,
    onPortChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
        AlohomoraTextField(
            label = "PORT",
            value = state.serverPort,
            onValueChange = onPortChange,
            singleLine = true,
            isError = state.serverError != null,
            supportingText = state.serverError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(140.dp)
                .testTag(AlohomoraTestTags.Overview.STATUS_PORT_FIELD),
        )
    }
}

@Composable
private fun DisconnectedDetail(state: OverviewState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        AlohomoraChip(
            label = "PORT ${state.serverPort}",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun AwaitingAuthDetail(
    state: OverviewState,
    onRememberDeviceChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
        state.pendingOtp?.let { otp ->
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
                otp.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.shapes.medium,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.alohomoraColors.info,
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onRememberDeviceChange.invoke(!state.rememberDevice)
                },
        ) {
            Text(
                text = "Remember this device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AlohomoraSwitch(
                checked = state.rememberDevice,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun ConnectedDetail(state: OverviewState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        AlohomoraChip(
            label = "PORT ${state.serverPort}",
            containerColor = MaterialTheme.alohomoraColors.success.copy(alpha = 0.15f),
            contentColor = MaterialTheme.alohomoraColors.success,
        )
        Text(
            text = "Streaming to desktop",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Decorative background ornament that morphs between [MaterialShapes] as the connection status
 * changes. The spring is one-shot per status change and the shape is static at rest, so the
 * default Off state never animates and `waitForIdle()` stays safe in device tests.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusDecorShape(
    status: DevConnectionStatus,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val target = when (status) {
        DevConnectionStatus.Off -> MaterialShapes.Circle
        DevConnectionStatus.Disconnected -> MaterialShapes.SoftBurst
        DevConnectionStatus.AwaitingAuth -> MaterialShapes.Clover8Leaf
        DevConnectionStatus.Connected -> MaterialShapes.Sunny
    }
    var shapePair by remember { mutableStateOf(target to target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target != shapePair.second) {
            shapePair = shapePair.second to target
            progress.snapTo(0f)
            progress.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 200f))
        }
    }
    val morph = remember(shapePair) { Morph(shapePair.first, shapePair.second) }
    val path = remember { Path() }
    Box(
        modifier = modifier.drawBehind {
            morph.toPath(progress.value, path)
            // MaterialShapes polygons are normalised to unit bounds; scale to the box.
            scale(size.width, size.height, pivot = Offset.Zero) {
                drawPath(path, tint.copy(alpha = 0.08f))
            }
        },
    )
}

@Preview
@Composable
private fun OverviewStatusCardPreview1() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                OverviewStatusCard(
                    state = OverviewState(
                        serverEnabled = false,
                        deviceConnectionStatus = DevConnectionStatus.Off,
                    ),
                    onToggle = {},
                    onPortChange = {},
                    onRememberDeviceChange = {},
                )
                OverviewStatusCard(
                    state = OverviewState(
                        serverEnabled = false,
                        deviceConnectionStatus = DevConnectionStatus.Off,
                        serverError = "Invalid port",
                    ),
                    onToggle = {},
                    onPortChange = {},
                    onRememberDeviceChange = {},
                )
                OverviewStatusCard(
                    state = OverviewState(
                        serverEnabled = true,
                        deviceConnectionStatus = DevConnectionStatus.Disconnected,
                    ),
                    onToggle = {},
                    onPortChange = {},
                    onRememberDeviceChange = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun OverviewStatusCardPreview2() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                OverviewStatusCard(
                    state = OverviewState(
                        serverEnabled = true,
                        deviceConnectionStatus = DevConnectionStatus.AwaitingAuth,
                        pendingOtp = "4829",
                        rememberDevice = true,
                    ),
                    onToggle = {},
                    onPortChange = {},
                    onRememberDeviceChange = {},
                )
                OverviewStatusCard(
                    state = OverviewState(
                        serverEnabled = true,
                        deviceConnectionStatus = DevConnectionStatus.Connected,
                    ),
                    onToggle = {},
                    onPortChange = {},
                    onRememberDeviceChange = {},
                )
            }
        }
    }
}
