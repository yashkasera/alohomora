package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import io.github.yashkasera.alohomora.desktop.app.DeviceWindowSession
import io.github.yashkasera.alohomora.desktop.app.applyMacTitleBar
import io.github.yashkasera.alohomora.desktop.app.isMacOs
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateInfo
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.UpdateBanner
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import java.awt.Dimension
import kotlin.math.roundToInt

@Composable
fun DeviceWindow(
    session: DeviceWindowSession,
    isDarkState: MutableState<Boolean>,
    isDark: Boolean,
    themeId: String,
    updateInfo: UpdateInfo?,
    updateDismissed: Boolean,
    onDismissUpdate: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    appOverlays: @Composable () -> Unit,
    onOpenLauncher: () -> Unit,
    onExit: () -> Unit,
    onSessionClosed: () -> Unit,
    screenshotDir: String = "",
    screenshotShowToast: Boolean = true,
) {
    val state = rememberWindowState(
        placement = WindowPlacement.Maximized,
        size = DpSize(1080.dp, 600.dp),
    )
    var showHelp by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var showDeepLinkBuilder by remember { mutableStateOf(false) }
    var showMockRules by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val zoomIn = { zoomScale = (zoomScale + 0.1f).coerceAtMost(2.0f) }
    val zoomOut = { zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.5f) }
    val resetZoom = { zoomScale = 1.0f }

    val closeWindow = {
        val devicesVm = session.composition.devicesViewModel
        val devToolsVm = session.composition.devToolsViewModel
        devicesVm.disconnectHost(session.host, session.hostPort)
        devicesVm.deactivateDevice(session.deviceId, session.hostPort)
        devToolsVm.disconnect()
        session.composition.close()
        onSessionClosed()
    }

    var deviceWasOnline by remember { mutableStateOf(true) }
    val devicesForReforward by session.composition.devicesViewModel.devices.collectAsState()
    val deviceIsOnline =
        devicesForReforward.any { it.id == session.deviceId && it.state == DeviceState.DEVICE }
    LaunchedEffect(deviceIsOnline) {
        if (deviceIsOnline && !deviceWasOnline) {
            val device = devicesForReforward.firstOrNull { it.id == session.deviceId }
            if (device?.platform == DevicePlatform.ANDROID) {
                session.composition.devicesViewModel.selectDevice(
                    session.deviceId, session.hostPort, session.devicePort,
                )
            }
        }
        deviceWasOnline = deviceIsOnline
    }

    val zoomSuffix =
        if (zoomScale != 1.0f) " (${(zoomScale * 100).roundToInt()}%)" else ""
    Window(
        title = "Alohomora - ${session.deviceId}$zoomSuffix",
        state = state,
        onCloseRequest = {
            session.composition.devToolsViewModel.disconnect()
            session.composition.close()
            onSessionClosed()
        },
    ) {
        AppTheme(isDarkState = isDarkState, themeId = themeId) {
            MenuBar {
                Menu("File") {
                    Item(
                        "Preferences",
                        shortcut = KeyShortcut(
                            Key.Comma,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = onShowSettings,
                    )
                    Separator()
                    Item(
                        "New Window",
                        shortcut = KeyShortcut(Key.N, meta = isMacOs, ctrl = !isMacOs),
                        onClick = onOpenLauncher,
                    )
                    Item(
                        "Close Window",
                        shortcut = KeyShortcut(Key.W, meta = isMacOs, ctrl = !isMacOs),
                        onClick = closeWindow,
                    )
                    Item("Exit", onClick = onExit)
                }
                Menu("Edit") {
                    Item(
                        "Find",
                        shortcut = KeyShortcut(Key.F, meta = isMacOs, ctrl = !isMacOs),
                        onClick = {},
                    )
                }
                Menu("View") {
                    Item(
                        "Zoom In",
                        shortcut = KeyShortcut(
                            Key.Equals,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = zoomIn,
                    )
                    Item(
                        "Zoom Out",
                        shortcut = KeyShortcut(
                            Key.Minus,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = zoomOut,
                    )
                    Item(
                        "Reset Zoom",
                        shortcut = KeyShortcut(
                            Key.Zero,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = resetZoom,
                    )
                }
                Menu("Device") {
                    Item(
                        "Take Screenshot",
                        shortcut = KeyShortcut(
                            Key.S,
                            shift = true,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = {
                            val timestamp = System.currentTimeMillis()
                            val defaultName = "alohomora_screenshot_${timestamp}.png"
                            val localPath = if (screenshotDir.isNotEmpty()) {
                                "$screenshotDir/$defaultName"
                            } else {
                                pickSavePath(
                                    defaultName, "Save Screenshot", ".png",
                                ) ?: return@Item
                            }
                            session.composition.devicesViewModel.takeScreenshot(
                                session.deviceId,
                                localPath,
                                screenshotShowToast,
                            )
                        },
                    )
                    Separator()
                    Item(
                        "Deep Link Builder",
                        shortcut = KeyShortcut(Key.L, meta = isMacOs, ctrl = !isMacOs),
                        onClick = { showDeepLinkBuilder = true },
                    )
                    Item(
                        "Mock Rules",
                        shortcut = KeyShortcut(
                            Key.M,
                            shift = true,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = { showMockRules = true },
                    )
                }
                Menu("Help") {
                    Item(
                        "Command Palette",
                        shortcut = KeyShortcut(Key.K, meta = isMacOs, ctrl = !isMacOs),
                        onClick = { showCommandPalette = true },
                    )
                    Item(
                        "Keyboard Shortcuts",
                        shortcut = KeyShortcut(
                            Key.Slash,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = { showHelp = true },
                    )
                    Item(
                        "About Alohomora",
                        onClick = onShowAbout,
                    )
                }
            }
            window.minimumSize = Dimension(1080, 600)
            applyMacTitleBar(window)

            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity, zoomScale) {
                Density(baseDensity.density * zoomScale, baseDensity.fontScale)
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                Column {
                    DevToolsDesktopApp(
                        modifier = Modifier.weight(1f),
                        devToolsViewModel = session.composition.devToolsViewModel,
                        devicesViewModel = session.composition.devicesViewModel,
                        logcatViewModel = session.composition.logcatViewModel,
                        databaseViewModel = session.composition.databaseViewModel,
                        cacheViewModel = session.composition.cacheViewModel,
                        featureFlagsViewModel = session.composition.featureFlagsViewModel,
                        pluginDataViewModel = session.composition.pluginDataViewModel,
                        tracesViewModel = session.composition.tracesViewModel,
                        eventsViewModel = session.composition.eventsViewModel,
                        trafficViewModel = session.composition.trafficViewModel,
                        networkRulesViewModel = session.composition.networkRulesViewModel,
                        initialDeviceId = session.deviceId,
                        showHelp = showHelp,
                        onShowHelp = { showHelp = true },
                        onDismissHelp = { showHelp = false },
                        showCommandPalette = showCommandPalette,
                        onOpenCommandPalette = { showCommandPalette = true },
                        onDismissCommandPalette = { showCommandPalette = false },
                        showDeepLinkBuilder = showDeepLinkBuilder,
                        onOpenDeepLinkBuilder = { showDeepLinkBuilder = true },
                        onDismissDeepLinkBuilder = { showDeepLinkBuilder = false },
                        showMockRules = showMockRules,
                        onOpenMockRules = { showMockRules = true },
                        onDismissMockRules = { showMockRules = false },
                        onShowSettings = onShowSettings,
                        onZoomIn = zoomIn,
                        onZoomOut = zoomOut,
                        onResetZoom = resetZoom,
                        onDisconnectWindow = closeWindow,
                        screenshotDir = screenshotDir,
                        screenshotShowToast = screenshotShowToast,
                    )

                    if (updateInfo != null && !updateDismissed) {
                        UpdateBanner(
                            updateInfo = updateInfo,
                            onDismiss = onDismissUpdate,
                        )
                    }
                }
            }
            appOverlays()
        }
    }
}
