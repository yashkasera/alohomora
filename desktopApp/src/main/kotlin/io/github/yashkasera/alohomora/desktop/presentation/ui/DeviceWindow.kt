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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
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
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SideSheetHostState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SideSheetId
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
    val sideSheetHost = remember { SideSheetHostState() }
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
        // Escape is handled at the window level so it fires no matter where focus sits — a panel's
        // search field, a sheet's editor, or nothing. A focus-scoped handler only worked while the
        // root Box held focus, which is why Escape used to close sheets on the Dashboard alone.
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                sideSheetHost.dismissTop()
            } else {
                false
            }
        },
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
                        onClick = { sideSheetHost.open(SideSheetId.DeepLinkBuilder) },
                    )
                    Item(
                        "Mock Rules",
                        shortcut = KeyShortcut(
                            Key.M,
                            shift = true,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = { sideSheetHost.open(SideSheetId.MockRules) },
                    )
                    Item(
                        "Event Journeys",
                        shortcut = KeyShortcut(
                            Key.J,
                            shift = true,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = { sideSheetHost.open(SideSheetId.Journeys) },
                    )
                    Item(
                        "Deep Link Catalog",
                        shortcut = KeyShortcut(
                            Key.L,
                            shift = true,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = { sideSheetHost.open(SideSheetId.DeepLinkCatalog) },
                    )
                }
                Menu("Help") {
                    Item(
                        "Command Palette",
                        shortcut = KeyShortcut(Key.K, meta = isMacOs, ctrl = !isMacOs),
                        onClick = { sideSheetHost.open(SideSheetId.CommandPalette) },
                    )
                    Item(
                        "Keyboard Shortcuts",
                        shortcut = KeyShortcut(
                            Key.Slash,
                            meta = isMacOs,
                            ctrl = !isMacOs,
                        ),
                        onClick = { sideSheetHost.open(SideSheetId.Help) },
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
                        journeyViewModel = session.composition.journeyViewModel,
                        configRepoViewModel = session.composition.configRepoViewModel,
                        deepLinkCatalogViewModel = session.composition.deepLinkCatalogViewModel,
                        initialDeviceId = session.deviceId,
                        sideSheetHost = sideSheetHost,
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
