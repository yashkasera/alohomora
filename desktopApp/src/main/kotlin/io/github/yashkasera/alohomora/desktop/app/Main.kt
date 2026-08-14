package io.github.yashkasera.alohomora.desktop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.DevToolsDesktopApp
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.icons.Alohomora
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateChecker
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateInfo
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopEventPrefs
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopMcpPrefs
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopTrustPrefs
import io.github.yashkasera.alohomora.desktop.mcp.AlohomoraMcpServer
import io.github.yashkasera.alohomora.desktop.mcp.DeviceSessionHandle
import io.github.yashkasera.alohomora.desktop.mcp.DeviceSessionRegistry
import io.github.yashkasera.alohomora.desktop.mcp.McpConfirmationBroker
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AboutDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SettingsDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.UpdateBanner
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.icons.AlohomoraFull
import java.awt.Dimension
import java.util.UUID
import javax.swing.RootPaneContainer
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private const val DEFAULT_HOST = "127.0.0.1"
private const val DEFAULT_PORT = "53999"

private data class PendingSession(
    val deviceId: String,
    val host: String,
    val hostPort: Int,
    val devicePort: Int,
    val composition: DesktopAppComposition,
)

data class DeviceWindowSession(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val host: String,
    val hostPort: Int,
    val devicePort: Int,
    val composition: DesktopAppComposition,
)

// Height of the macOS traffic-light strip we reserve at the top of each window. The native buttons
// are a fixed pixel size, so this inset is applied at base density (outside any zoom scaling).
// internal so the About/Preferences dialog windows can reserve the same space.
internal val MacTitleBarHeight = 28.dp

/**
 * On macOS, hides the native title bar and lets the app's own dark background run under it. The three
 * client properties are ignored on Windows/Linux, so callers need no platform guard beyond [isMacOs].
 * Set once per window via [LaunchedEffect]; the native traffic-light controls and window dragging are
 * untouched. internal so every [DialogWindow] (About, Preferences, launcher) can share it.
 */
@Composable
internal fun applyMacTitleBar(window: RootPaneContainer) {
    if (!isMacOs) return
    LaunchedEffect(window) {
        window.rootPane.apply {
            putClientProperty("apple.awt.fullWindowContent", true)
            putClientProperty("apple.awt.transparentTitleBar", true)
            putClientProperty("apple.awt.windowTitleVisible", false)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val initialMode = DesktopThemePrefs.loadMode()
    val initialThemeId = DesktopThemePrefs.loadThemeId()
    val initialIsDark = when (initialMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> true
    }
    System.setProperty("apple.awt.application.name", "Alohomora")
    System.setProperty(
        "apple.awt.application.appearance",
        if (initialIsDark) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua",
    )
    application {
        val sharedComposition = remember { DesktopAppComposition() }
        val sessions = remember { mutableStateListOf<DeviceWindowSession>() }
        var launcherVisible by remember { mutableStateOf(true) }
        var themeMode by remember { mutableStateOf(initialMode) }
        var themeId by remember { mutableStateOf(initialThemeId) }
        val systemDark = isSystemInDarkTheme()
        val effectiveIsDark = when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val sharedIsDark = remember { mutableStateOf(effectiveIsDark) }
        sharedIsDark.value = effectiveIsDark
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var updateDismissed by remember { mutableStateOf(false) }
        var showAbout by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val info = UpdateChecker.check(DesktopBuildConfig.version)
            if (info != null && info.isUpdateAvailable) {
                updateInfo = info
            }
        }

        // MCP server: app-scoped (one listener) with per-device data bridged through the registry.
        // Off unless the user opts in via Settings; write tools need a second opt-in.
        val mcpRegistry = remember { DeviceSessionRegistry() }
        val mcpConfirmation = remember { McpConfirmationBroker() }
        var mcpEnabled by remember { mutableStateOf(DesktopMcpPrefs.loadEnabled()) }
        var mcpPort by remember { mutableStateOf(DesktopMcpPrefs.loadPort()) }
        var mcpWriteEnabled by remember { mutableStateOf(DesktopMcpPrefs.loadWriteEnabled()) }
        val mcpServer = remember {
            AlohomoraMcpServer(
                registry = mcpRegistry,
                serverVersion = DesktopBuildConfig.version,
                writeEnabled = { mcpWriteEnabled },
                confirmation = mcpConfirmation,
            )
        }
        val mcpStatus by mcpServer.status.collectAsState()
        val pendingMcpConfirmation by mcpConfirmation.pending.collectAsState()

        // Mirror the open windows into the registry reactively. Combine the window list with the ADB
        // devices flow so late-arriving model/platform metadata refreshes the handles too — otherwise
        // list_devices could keep reporting null model/platform for a session opened before ADB reported.
        LaunchedEffect(Unit) {
            combine(
                snapshotFlow { sessions.toList() },
                sharedComposition.devicesViewModel.devices,
            ) { open, devices -> open to devices }.collect { (open, devices) ->
                mcpRegistry.update(
                    open.map { session ->
                        val device = devices.firstOrNull { it.id == session.deviceId }
                        DeviceSessionHandle(
                            deviceId = session.deviceId,
                            model = device?.model,
                            platform = device?.platform?.name,
                            devToolsRepository = session.composition.devToolsRepository,
                            networkRulesViewModel = session.composition.networkRulesViewModel,
                        )
                    },
                )
            }
        }

        LaunchedEffect(mcpEnabled, mcpPort) {
            if (mcpEnabled) mcpServer.start(mcpPort) else mcpServer.stop()
        }
        DisposableEffect(Unit) {
            onDispose { mcpServer.stop() }
        }

        if (showAbout) {
            AboutDialog(
                isDark = effectiveIsDark,
                themeId = themeId,
                updateInfo = updateInfo,
                onDismiss = { showAbout = false },
            )
        }

        if (showSettings) {
            SettingsDialog(
                isDark = effectiveIsDark,
                themeId = themeId,
                themeMode = themeMode,
                onThemeIdChange = { id ->
                    themeId = id
                    DesktopThemePrefs.saveThemeId(id)
                },
                onThemeModeChange = { mode ->
                    themeMode = mode
                    DesktopThemePrefs.saveMode(mode)
                },
                onClearTrustTokens = { DesktopTrustPrefs.clearAll() },
                onClearMutedEvents = { DesktopEventPrefs.clearAll() },
                onResetPreferences = {
                    DesktopThemePrefs.clear()
                    DesktopTrustPrefs.clearAll()
                    DesktopEventPrefs.clearAll()
                    DesktopMcpPrefs.clear()
                    themeMode = ThemeMode.SYSTEM
                    themeId = "default"
                    mcpEnabled = false
                    mcpPort = DesktopMcpPrefs.DEFAULT_PORT
                    mcpWriteEnabled = false
                },
                mcpEnabled = mcpEnabled,
                mcpPort = mcpPort,
                mcpStatus = mcpStatus,
                mcpWriteEnabled = mcpWriteEnabled,
                onMcpEnabledChange = { enabled ->
                    mcpEnabled = enabled
                    DesktopMcpPrefs.saveEnabled(enabled)
                },
                onMcpPortChange = { port ->
                    mcpPort = port
                    DesktopMcpPrefs.savePort(port)
                },
                onMcpWriteEnabledChange = { enabled ->
                    mcpWriteEnabled = enabled
                    DesktopMcpPrefs.saveWriteEnabled(enabled)
                },
                onDismiss = { showSettings = false },
            )
        }

        // A write tool (clear_captured) is asking the developer to approve a destructive action. Shown
        // at app scope in its own dialog window so it appears regardless of which window has focus.
        pendingMcpConfirmation?.let { pending ->
            DialogWindow(
                title = "Alohomora",
                onCloseRequest = { pending.resolve(false) },
                state = rememberDialogState(width = 440.dp, height = 220.dp),
                resizable = false,
            ) {
                // A maximized device window otherwise buries this on macOS — force it to the front.
                LaunchedEffect(Unit) {
                    window.isAlwaysOnTop = true
                    window.toFront()
                    window.requestFocus()
                }
                AppTheme(isDarkState = sharedIsDark, themeId = themeId) {
                    applyMacTitleBar(window)
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                // Clear the macOS traffic lights that overlay the transparent title bar.
                                .padding(top = if (isMacOs) MacTitleBarHeight else 0.dp)
                                .padding(MaterialTheme.dimens.margin.xxl),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        ) {
                            Text(pending.title, style = MaterialTheme.typography.titleMedium)
                            Text(pending.message, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm, Alignment.End),
                            ) {
                                AlohomoraOutlinedButton(
                                    text = "Deny",
                                    size = AlohomoraButtonSize.SMALL,
                                    onClick = { pending.resolve(false) },
                                )
                                AlohomoraFilledButton(
                                    text = "Allow",
                                    onClick = { pending.resolve(true) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (launcherVisible) {
            val state = rememberDialogState()
            DialogWindow(
                title = "Alohomora",
                state = state,
                onCloseRequest = {
                    launcherVisible = false
                    if (sessions.isEmpty()) exitApplication()
                },
                resizable = false,
            ) {
                AppTheme(isDarkState = sharedIsDark, themeId = themeId) {
                    window.minimumSize = Dimension(900, 560)
                    applyMacTitleBar(window)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(top = if (isMacOs) MacTitleBarHeight else 0.dp),
                    ) {
                        Column {
                            val pending = updateInfo
                            if (pending != null && !updateDismissed) {
                                UpdateBanner(
                                    updateInfo = pending,
                                    onDismiss = { updateDismissed = true },
                                )
                            }

                            LauncherScreen(
                                sharedDevicesComposition = sharedComposition,
                                onOpenDeviceWindow = { deviceId, host, hostPort, devicePort, composition ->
                                    val duplicate = sessions.any { it.deviceId == deviceId }
                                    if (!duplicate) {
                                        sessions += DeviceWindowSession(
                                            deviceId = deviceId,
                                            host = host,
                                            hostPort = hostPort,
                                            devicePort = devicePort,
                                            composition = composition,
                                        )
                                    }
                                    launcherVisible = false
                                },
                            )
                        }
                    }
                }
            }
        }

        sessions.toList().forEach { session ->
            key(session.id) {
                val state = rememberWindowState(
                    placement = WindowPlacement.Maximized,
                    size = DpSize(1080.dp, 600.dp),
                )
                var showHelp by remember { mutableStateOf(false) }
                var showCommandPalette by remember { mutableStateOf(false) }
                var zoomScale by remember { mutableStateOf(1.0f) }

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
                    sessions.removeAll { it.id == session.id }
                    if (sessions.isEmpty()) launcherVisible = true
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
                        sessions.removeAll { it.id == session.id }
                        if (sessions.isEmpty()) launcherVisible = true
                    },
                ) {
                    AppTheme(isDarkState = sharedIsDark, themeId = themeId) {
                        MenuBar {
                            Menu("File") {
                                Item(
                                    "Preferences",
                                    shortcut = KeyShortcut(
                                        Key.Comma,
                                        meta = isMacOs,
                                        ctrl = !isMacOs,
                                    ),
                                    onClick = { showSettings = true },
                                )
                                Separator()
                                Item(
                                    "New Window",
                                    shortcut = KeyShortcut(Key.N, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = { launcherVisible = true },
                                )
                                Item(
                                    "Close Window",
                                    shortcut = KeyShortcut(Key.W, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = closeWindow,
                                )
                                Item("Exit", onClick = ::exitApplication)
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
                                        val localPath =
                                            io.github.yashkasera.alohomora.desktop.util.pickSavePath(
                                                defaultName, "Save Screenshot", ".png",
                                            ) ?: return@Item
                                        session.composition.devicesViewModel.takeScreenshot(
                                            session.deviceId,
                                            localPath,
                                        )
                                    },
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
                                    onClick = { showAbout = true },
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
                                val pending = updateInfo
                                if (pending != null && !updateDismissed) {
                                    UpdateBanner(
                                        updateInfo = pending,
                                        onDismiss = { updateDismissed = true },
                                    )
                                }

                                DevToolsDesktopApp(
                                    devToolsViewModel = session.composition.devToolsViewModel,
                                    devicesViewModel = session.composition.devicesViewModel,
                                    logcatViewModel = session.composition.logcatViewModel,
                                    databaseViewModel = session.composition.databaseViewModel,
                                    cacheViewModel = session.composition.cacheViewModel,
                                    featureFlagsViewModel = session.composition.featureFlagsViewModel,
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
                                    onShowSettings = { showSettings = true },
                                    onZoomIn = zoomIn,
                                    onZoomOut = zoomOut,
                                    onResetZoom = resetZoom,
                                    isDark = effectiveIsDark,
                                    themeId = themeId,
                                    onDisconnectWindow = closeWindow,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LauncherScreen(
    sharedDevicesComposition: DesktopAppComposition,
    onOpenDeviceWindow: (deviceId: String, host: String, hostPort: Int, devicePort: Int, composition: DesktopAppComposition) -> Unit,
) {
    val devicesViewModel = sharedDevicesComposition.devicesViewModel
    val devices by devicesViewModel.devices.collectAsState()

    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var host by remember { mutableStateOf(DEFAULT_HOST) }
    var hostPort by remember { mutableStateOf(DEFAULT_PORT) }
    var devicePort by remember { mutableStateOf(DEFAULT_PORT) }
    var actionError by remember { mutableStateOf<String?>(null) }

    var pendingSession by remember { mutableStateOf<PendingSession?>(null) }
    var pendingConnectionState by remember { mutableStateOf<DevToolsConnection>(DevToolsConnection.Disconnected) }
    var otpInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val onlineDevices = devices.filter { it.state == DeviceState.DEVICE }
    val selectedDevice = onlineDevices.firstOrNull { it.id == selectedDeviceId }

    LaunchedEffect(Unit) {
        while (true) {
            devicesViewModel.refreshDevices()
            delay(3000)
        }
    }

    LaunchedEffect(onlineDevices) {
        if (selectedDeviceId == null || onlineDevices.none { it.id == selectedDeviceId }) {
            selectedDeviceId = onlineDevices.firstOrNull()?.id
        }
    }

    LaunchedEffect(pendingSession) {
        val session = pendingSession ?: return@LaunchedEffect
        session.composition.devToolsViewModel.uiState.collect { uiState ->
            pendingConnectionState = uiState.connection
            when (val conn = uiState.connection) {
                is DevToolsConnection.Connected -> {
                    onOpenDeviceWindow(
                        session.deviceId,
                        session.host,
                        session.hostPort,
                        session.devicePort,
                        session.composition,
                    )
                    pendingSession = null
                    otpInput = ""
                }

                is DevToolsConnection.Failed -> {
                    actionError = conn.reason
                    pendingSession = null
                    otpInput = ""
                }

                else -> {}
            }
        }
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AlohomoraFull,
                    contentDescription = null,
                    modifier = Modifier.width(256.dp,),
                )
                Spacer(modifier = Modifier.weight(1f))
                AlohomoraOutlinedButton(
                    text = "Refresh",
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                            imageVector = Icons.RefreshCw, contentDescription = null,
                        )
                    },
                    onClick = devicesViewModel::refreshDevices,
                    size = AlohomoraButtonSize.SMALL,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.margin.md))

            if (pendingSession != null) {
                when (val pending = pendingConnectionState) {
                    is DevToolsConnection.AwaitingAuth if pending.otpRequired -> {
                        Text(
                            "Authentication Required",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            "Enter the 4-digit code shown on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        AlohomoraTextField(
                            value = otpInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all(Char::isDigit)) otpInput = it
                            },
                            placeholder = "0000",
                            singleLine = true,
                            modifier = Modifier.width(160.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AlohomoraFilledButton(
                                text = "Confirm",
                                onClick = {
                                    pendingSession?.composition?.devToolsViewModel?.submitOtp(
                                        otpInput,
                                    )
                                    otpInput = ""
                                },
                                enabled = otpInput.length == 4,
                                uppercase = false,
                            )
                            AlohomoraOutlinedButton(
                                text = "Cancel",
                                size = AlohomoraButtonSize.SMALL,
                                onClick = {
                                    pendingSession?.composition?.devToolsViewModel?.disconnect()
                                    pendingSession = null
                                    otpInput = ""
                                },
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                space = MaterialTheme.dimens.margin.md,
                                alignment = Alignment.CenterVertically,
                            ),
                        ) {
                            CircularWavyProgressIndicator()
                            Text(
                                "Connecting…",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            (pendingConnectionState as? DevToolsConnection.Connecting)?.let { conn ->
                                Text(
                                    "${conn.host}:${conn.port}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
                            AlohomoraOutlinedButton(
                                text = "Cancel",
                                size = AlohomoraButtonSize.SMALL,
                                onClick = {
                                    pendingSession?.composition?.devToolsViewModel?.disconnect()
                                    pendingSession = null
                                },
                            )
                        }
                    }
                }
                if (!actionError.isNullOrBlank()) {
                    Text(actionError!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    "Select a device, connect, and open a dedicated window.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                if (onlineDevices.isEmpty()) {
                    Text(
                        "No online devices found. Connect a device and refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        onlineDevices.forEach { device ->
                            val selected = device.id == selectedDeviceId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                    )
                                    .clickable { selectedDeviceId = device.id }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Text(device.model ?: device.id)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(device.id, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlohomoraTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = "Host",
                        singleLine = true,
                        modifier = Modifier.width(220.dp),
                    )
                    AlohomoraTextField(
                        value = hostPort,
                        onValueChange = { hostPort = it.filter(Char::isDigit) },
                        label = "Host Port",
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                    AlohomoraTextField(
                        value = devicePort,
                        onValueChange = { devicePort = it.filter(Char::isDigit) },
                        label = "Device Port",
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                }

                if (!actionError.isNullOrBlank()) {
                    Text(actionError!!, color = MaterialTheme.colorScheme.error)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlohomoraFilledButton(
                        text = "Connect & Open Window",
                        onClick = {
                            if (selectedDevice == null) {
                                actionError = "Select an online device first"
                                return@AlohomoraFilledButton
                            }
                            val numericHostPort = hostPort.toIntOrNull() ?: DEFAULT_PORT.toInt()
                            val numericDevicePort = devicePort.toIntOrNull() ?: DEFAULT_PORT.toInt()
                            val isLocalHost = host == "127.0.0.1" || host == "localhost"

                            val composition =
                                DesktopAppComposition(sharedDevicesViewModel = devicesViewModel)

                            fun openSession(target1: DeviceUi, tunnel: DevToolsTarget) {
                                composition.devToolsViewModel.switchDevice(tunnel, target1.id)
                                pendingSession = PendingSession(
                                    target1.id,
                                    tunnel.displayHost,
                                    numericHostPort,
                                    numericDevicePort,
                                    composition,
                                )
                                actionError = null
                            }

                            scope.launch {
                                when (selectedDevice.platform) {
                                    // Physical iOS: no adb, and no host-side port to reserve.
                                    // usbmuxd tunnels straight to the device port over USB.
                                    DevicePlatform.IOS -> {
                                        val usbmuxId = selectedDevice.usbmuxDeviceId
                                        if (usbmuxId == null) {
                                            actionError =
                                                "Device is not reachable over USB. Reconnect the cable and trust this Mac."
                                        } else {
                                            openSession(
                                                selectedDevice,
                                                DevToolsTarget.Usbmux(usbmuxId, numericDevicePort),
                                            )
                                        }
                                    }

                                    // Simulator: nothing to tunnel. It runs on the host's network
                                    // stack, so the device's 127.0.0.1 is the host's 127.0.0.1.
                                    DevicePlatform.IOS_SIMULATOR ->
                                        openSession(
                                            selectedDevice,
                                            DevToolsTarget.Tcp("127.0.0.1", numericDevicePort),
                                        )

                                    DevicePlatform.ANDROID -> {
                                        // Wi-Fi adb needs `adb connect` before a forward exists.
                                        if (!isLocalHost) {
                                            val connectError = suspendConnectOverTcp(
                                                composition.devicesViewModel,
                                                selectedDevice.id, host, numericDevicePort,
                                            )
                                            if (connectError != null) {
                                                actionError = connectError
                                                return@launch
                                            }
                                        }
                                        val selectError = suspendSelectDevice(
                                            composition.devicesViewModel,
                                            selectedDevice.id, numericHostPort, numericDevicePort,
                                        )
                                        if (selectError != null) {
                                            actionError = selectError
                                        } else {
                                            openSession(
                                                selectedDevice,
                                                DevToolsTarget.Tcp(host, numericHostPort),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        enabled = selectedDevice != null,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "v${DesktopBuildConfig.version}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

/**
 * Callback-to-suspend adapters for the device activation calls.
 *
 * The launcher previously nested these four callbacks deep inside an onClick, which made the
 * ordering (adb connect -> forward -> DevTools switch) impossible to follow and impossible to
 * short-circuit on error.
 */
private suspend fun suspendConnectOverTcp(
    viewModel: DevicesViewModel,
    deviceId: String,
    host: String,
    port: Int,
): String? = suspendCancellableCoroutine { continuation ->
    viewModel.connectOverTcp(deviceId, host, port) { error -> continuation.resume(error) }
}

private suspend fun suspendSelectDevice(
    viewModel: DevicesViewModel,
    deviceId: String,
    hostPort: Int,
    devicePort: Int,
): String? = suspendCancellableCoroutine { continuation ->
    viewModel.selectDevice(deviceId, hostPort, devicePort) { error -> continuation.resume(error) }
}
