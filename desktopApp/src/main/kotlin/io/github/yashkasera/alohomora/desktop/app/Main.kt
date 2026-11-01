package io.github.yashkasera.alohomora.desktop.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.application
import io.github.yashkasera.alohomora.desktop.data.adb.AdbLocator
import io.github.yashkasera.alohomora.desktop.data.adb.DesktopAdbPrefs
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopEventPrefs
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopMcpPrefs
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopScreenshotPrefs
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopTrustPrefs
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateChecker
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateInfo
import io.github.yashkasera.alohomora.desktop.mcp.AlohomoraMcpServer
import io.github.yashkasera.alohomora.desktop.mcp.DeviceSessionHandle
import io.github.yashkasera.alohomora.desktop.mcp.DeviceSessionRegistry
import io.github.yashkasera.alohomora.desktop.mcp.McpConfirmationBroker
import io.github.yashkasera.alohomora.desktop.presentation.ui.DeviceWindow
import io.github.yashkasera.alohomora.desktop.presentation.ui.LauncherWindow
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AboutDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.McpConfirmationDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SettingsDialog
import java.awt.Desktop
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Upper bound on releasing every session at exit, so a wedged adb cannot hold quit hostage. */
private val EXIT_TEARDOWN_TIMEOUT = 5.seconds

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Apply the saved custom adb path before any device work so resolution honours it from the
    // first poll. Blank means "auto-detect".
    AdbLocator.configure(DesktopAdbPrefs.loadPath().ifBlank { null })
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

        // Session teardown outlives the window it belongs to — the window leaves composition the
        // moment its session is removed — so it runs here, on the application's own scope.
        val appScope = rememberCoroutineScope()
        // Bumped to bring an existing device window forward; keyed by session id.
        val focusRequests = remember { mutableStateMapOf<String, Int>() }
        var exiting by remember { mutableStateOf(false) }

        val closeSession: (DeviceWindowSession) -> Unit = { session ->
            sessions.removeAll { it.id == session.id }
            focusRequests.remove(session.id)
            appScope.launch { session.release() }
            if (sessions.isEmpty()) launcherVisible = true
        }
        val focusSession: (DeviceWindowSession) -> Unit = { session ->
            focusRequests[session.id] = (focusRequests[session.id] ?: 0) + 1
        }
        // Every exit path goes through here: the Exit menu items and the macOS quit handler.
        // exitApplication on its own disposed the windows without releasing a single session.
        val shutDown: (finish: () -> Unit) -> Unit = { finish ->
            if (!exiting) {
                exiting = true
                launcherVisible = false
                val open = sessions.toList()
                appScope.launch {
                    withTimeoutOrNull(EXIT_TEARDOWN_TIMEOUT) {
                        open.map { async { it.release() } }.awaitAll()
                    }
                    finish()
                }
            }
        }
        val exit = { shutDown(::exitApplication) }

        // Cmd+Q and Dock > Quit do not call exitApplication: AWT's default quit strategy is a
        // bare System.exit, which skipped teardown entirely. The response is completed only once
        // the sessions are released, rather than cancelled, so a logout or shutdown that asked
        // the app to quit still proceeds.
        DisposableEffect(Unit) {
            val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
            val handlesQuit = desktop?.isSupported(Desktop.Action.APP_QUIT_HANDLER) == true
            if (handlesQuit) {
                desktop.setQuitHandler { _, response -> shutDown { response.performQuit() } }
            }
            onDispose { if (handlesQuit) desktop.setQuitHandler(null) }
        }

        LaunchedEffect(Unit) {
            val info = UpdateChecker.check(DesktopBuildConfig.version)
            if (info != null && info.isUpdateAvailable) {
                updateInfo = info
            }
        }

        // MCP server: app-scoped (one listener) with per-device data bridged through the registry.
        val mcpRegistry = remember { DeviceSessionRegistry() }
        val mcpConfirmation = remember { McpConfirmationBroker() }
        var mcpEnabled by remember { mutableStateOf(DesktopMcpPrefs.loadEnabled()) }
        var mcpPort by remember { mutableIntStateOf(DesktopMcpPrefs.loadPort()) }
        var mcpWriteEnabled by remember { mutableStateOf(DesktopMcpPrefs.loadWriteEnabled()) }
        var screenshotDir by remember { mutableStateOf(DesktopScreenshotPrefs.loadDefaultDir()) }
        var screenshotShowToast by remember { mutableStateOf(DesktopScreenshotPrefs.loadShowToast()) }
        var adbPath by remember { mutableStateOf(DesktopAdbPrefs.loadPath()) }
        // Previews what the current path resolves to (custom value, else auto-detected) for the
        // Settings status line — no side effects, unlike the live AdbLocator.configure below.
        val adbResolvedPath = remember(adbPath) { AdbLocator.resolveWith(adbPath.ifBlank { null }) }
        val mcpServer = remember {
            AlohomoraMcpServer(
                registry = mcpRegistry,
                serverVersion = DesktopBuildConfig.version,
                configStore = sharedComposition.configStore,
                writeEnabled = { mcpWriteEnabled },
                confirmation = mcpConfirmation,
            )
        }
        val mcpStatus by mcpServer.status.collectAsState()
        val pendingMcpConfirmation by mcpConfirmation.pending.collectAsState()
        val configRepoUi by sharedComposition.configRepoViewModel.uiState.collectAsState()

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

        val appOverlays: @Composable () -> Unit = {
            if (showAbout) {
                AboutDialog(
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
                    screenshotDir = screenshotDir,
                    screenshotShowToast = screenshotShowToast,
                    onScreenshotDirChange = { dir ->
                        screenshotDir = dir
                        DesktopScreenshotPrefs.saveDefaultDir(dir)
                    },
                    onScreenshotShowToastChange = { show ->
                        screenshotShowToast = show
                        DesktopScreenshotPrefs.saveShowToast(show)
                    },
                    adbPath = adbPath,
                    adbResolvedPath = adbResolvedPath,
                    onAdbPathChange = { path ->
                        adbPath = path
                        DesktopAdbPrefs.savePath(path)
                        AdbLocator.configure(path.ifBlank { null })
                        sharedComposition.devicesViewModel.refreshDevices()
                    },
                    onClearTrustTokens = { DesktopTrustPrefs.clearAll() },
                    onClearMutedEvents = { DesktopEventPrefs.clearAll() },
                    onResetPreferences = {
                        DesktopThemePrefs.clear()
                        DesktopTrustPrefs.clearAll()
                        DesktopEventPrefs.clearAll()
                        DesktopMcpPrefs.clear()
                        DesktopScreenshotPrefs.clear()
                        DesktopAdbPrefs.clear()
                        AdbLocator.configure(null)
                        themeMode = ThemeMode.SYSTEM
                        themeId = "default"
                        mcpEnabled = false
                        mcpPort = DesktopMcpPrefs.DEFAULT_PORT
                        mcpWriteEnabled = false
                        screenshotDir = ""
                        screenshotShowToast = true
                        adbPath = ""
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
                    configRepo = configRepoUi,
                    onConfigRepoUrlChange = sharedComposition.configRepoViewModel::onUrlChange,
                    onConfigRepoPatChange = sharedComposition.configRepoViewModel::onPatChange,
                    onConfigRepoConnect = sharedComposition.configRepoViewModel::connect,
                    onConfigRepoInitialize = sharedComposition.configRepoViewModel::initialize,
                    onConfigRepoDisconnect = sharedComposition.configRepoViewModel::disconnect,
                    onConfigRepoSync = sharedComposition.configRepoViewModel::sync,
                    onDeveloperModeChange = sharedComposition.configRepoViewModel::setDeveloperMode,
                    onRevealRepo = sharedComposition.configRepoViewModel::revealRepo,
                    onDismiss = { showSettings = false },
                )
            }
        }

        pendingMcpConfirmation?.let { pending ->
            McpConfirmationDialog(
                pending = pending,
                isDarkState = sharedIsDark,
                themeId = themeId,
            )
        }

        if (launcherVisible) {
            LauncherWindow(
                sharedComposition = sharedComposition,
                isDarkState = sharedIsDark,
                themeId = themeId,
                updateInfo = updateInfo,
                updateDismissed = updateDismissed,
                onDismissUpdate = { updateDismissed = true },
                onShowSettings = { showSettings = true },
                onShowAbout = { showAbout = true },
                appOverlays = appOverlays,
                openSessions = { sessions.toList() },
                onFocusSession = { session ->
                    focusSession(session)
                    launcherVisible = false
                },
                onDiscardSession = { session -> appScope.launch { session.release() } },
                onOpenDeviceWindow = { session ->
                    val existing = sessions.firstOrNull { it.deviceId == session.deviceId }
                    if (existing == null) {
                        sessions += session
                    } else {
                        // The launcher checks for an open window before connecting, so this is
                        // a backstop. The duplicate must still be released, not just dropped:
                        // it carries its own Ktor client and view-model scopes.
                        appScope.launch { session.release() }
                        focusSession(existing)
                    }
                    launcherVisible = false
                },
                onClose = {
                    launcherVisible = false
                    if (sessions.isEmpty()) exit()
                },
                onExit = exit,
            )
        }

        if (!exiting) sessions.toList().forEach { session ->
            key(session.id) {
                DeviceWindow(
                    session = session,
                    focusRequest = focusRequests[session.id] ?: 0,
                    isDarkState = sharedIsDark,
                    isDark = effectiveIsDark,
                    themeId = themeId,
                    updateInfo = updateInfo,
                    updateDismissed = updateDismissed,
                    onDismissUpdate = { updateDismissed = true },
                    onShowSettings = { showSettings = true },
                    onShowAbout = { showAbout = true },
                    appOverlays = appOverlays,
                    onOpenLauncher = { launcherVisible = true },
                    onExit = exit,
                    onClose = { closeSession(session) },
                    screenshotDir = screenshotDir,
                    screenshotShowToast = screenshotShowToast,
                )
            }
        }
    }
}
