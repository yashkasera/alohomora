package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.app.isClearShortcut
import io.github.yashkasera.alohomora.desktop.app.isDeepLinkShortcut
import io.github.yashkasera.alohomora.desktop.app.isFocusSearchShortcut
import io.github.yashkasera.alohomora.desktop.app.isMockRulesShortcut
import io.github.yashkasera.alohomora.desktop.app.isModifierKeyOnly
import io.github.yashkasera.alohomora.desktop.app.isScreenshotShortcut
import io.github.yashkasera.alohomora.desktop.app.isTogglePropertiesShortcut
import io.github.yashkasera.alohomora.desktop.app.matchesNavigation
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.CommandPalette
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.LocalCopyFeedback
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.HelpDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.OtpPromptDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.buildCommandActions
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.AdbToolsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.CachePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ConfigPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DashboardContent
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DatabasePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DeepLinkBuilderSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ErrorDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ErrorsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.FeatureFlagsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.GitHistoryPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.LogcatPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.MockRulesSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TraceWaterfallSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TracesPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TrafficDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TrafficPanel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.CacheViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.FeatureFlagViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TracesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TrafficViewModel
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SWITCHING_SCRIM_ALPHA = 0.40f

private val PermanentDrawerShape = RoundedCornerShape(
    topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 0.dp,
)

@Composable
fun DevToolsDesktopApp(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    databaseViewModel: DatabaseViewModel,
    cacheViewModel: CacheViewModel,
    featureFlagsViewModel: FeatureFlagViewModel,
    tracesViewModel: TracesViewModel,
    eventsViewModel: EventsViewModel,
    trafficViewModel: TrafficViewModel,
    networkRulesViewModel: NetworkRulesViewModel,
    initialDeviceId: String? = null,
    showHelp: Boolean = false,
    onShowHelp: () -> Unit = {},
    onDismissHelp: () -> Unit = {},
    showCommandPalette: Boolean = false,
    onOpenCommandPalette: () -> Unit = {},
    onDismissCommandPalette: () -> Unit = {},
    showDeepLinkBuilder: Boolean = false,
    onOpenDeepLinkBuilder: () -> Unit = {},
    onDismissDeepLinkBuilder: () -> Unit = {},
    showMockRules: Boolean = false,
    onOpenMockRules: () -> Unit = {},
    onDismissMockRules: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onResetZoom: () -> Unit = {},
    isDark: Boolean = true,
    themeId: String = "default",
    onDisconnectWindow: () -> Unit,
    screenshotDir: String = "",
    screenshotShowToast: Boolean = true,
) {
    var activeSection by remember { mutableStateOf(DesktopSection.Traffic) }
    var searchFocusTrigger by remember { mutableLongStateOf(0L) }
    val copySnackbarState = remember { SnackbarHostState() }
    val copyScope = rememberCoroutineScope()

    val devices by devicesViewModel.devices.collectAsState()
    val adbCommandHistory by devicesViewModel.adbCommandHistory.collectAsState()
    val devToolsState by devToolsViewModel.uiState.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val deviceError by devToolsViewModel.deviceError.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }
    var selectedTrafficForSheet by remember { mutableStateOf<TrafficEntry?>(null) }
    var selectedErrorForSheet by remember {
        mutableStateOf<io.github.yashkasera.alohomora.common.Error?>(
            null,
        )
    }
    val selectedTraceId by tracesViewModel.selectedTraceId.collectAsState()
    val selectedEventId by eventsViewModel.selectedEventId.collectAsState()
    var selectedDeviceId by remember(initialDeviceId) { mutableStateOf(initialDeviceId) }
    var isModifierPhysicallyDown by remember { mutableStateOf(false) }
    var showModifierBadges by remember { mutableStateOf(false) }

    val anySideSheetOpen = selectedTrafficForSheet != null ||
        selectedErrorForSheet != null ||
        selectedTraceId != null ||
        selectedEventId != null ||
        showMockRules ||
        showDeepLinkBuilder ||
        showCommandPalette ||
        showHelp

    LaunchedEffect(isModifierPhysicallyDown, anySideSheetOpen) {
        if (isModifierPhysicallyDown && !anySideSheetOpen) {
            delay(250.milliseconds)
            showModifierBadges = true
        } else {
            showModifierBadges = false
        }
    }

    val onlineDevices = devices.filter { it.state == DeviceState.DEVICE }
    val hasConnectedDevice = onlineDevices.isNotEmpty()

    LaunchedEffect(Unit) {
        while (true) {
            devicesViewModel.refreshDevices()
            delay(3000.milliseconds)
        }
    }

    LaunchedEffect(devices) {
        if (selectedDeviceId.isNullOrBlank()) {
            selectedDeviceId = onlineDevices.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedDeviceId, buildInfo?.packageName) {
        devicesViewModel.startDashboardPolling(selectedDeviceId, buildInfo?.packageName)
    }


    val selectedDevice = devices.firstOrNull { it.id == selectedDeviceId }
    val isConnected = devToolsState.connection is DevToolsConnection.Connected
    var lastKnownPlatform by remember { mutableStateOf(selectedDevice?.platform) }
    if (selectedDevice != null) lastKnownPlatform = selectedDevice.platform
    val connectedPlatform = lastKnownPlatform
    val isAndroid = connectedPlatform == DevicePlatform.ANDROID

    val fallbackSection = DesktopSection.defaultFor(
        connectedPlatform ?: DevicePlatform.ANDROID,
    )

    val visibleSections = when (connectedPlatform) {
        null -> DesktopSection.entries.toList()
        else -> DesktopSection.forPlatform(connectedPlatform)
    }

    LaunchedEffect(selectedDeviceId) {
        selectedDevice?.let { device ->
            if (!activeSection.isSupportedBy(device.capabilities)) {
                activeSection = fallbackSection
            }
        }
    }

    val commandActions = buildCommandActions(
        visibleSections = visibleSections,
        onSectionChange = { activeSection = it },
        isConnected = isConnected,
        packageName = buildInfo?.packageName,
        selectedDeviceId = selectedDeviceId,
        isAndroid = isAndroid,
        onShowSettings = {
            onDismissCommandPalette()
            onShowSettings()
        },
        onShowHelp = {
            onDismissCommandPalette()
            onShowHelp()
        },
        onZoomIn = onZoomIn,
        onZoomOut = onZoomOut,
        onResetZoom = onResetZoom,
        onClearTraffic = { trafficViewModel.clearTraffic() },
        onClearTraces = { tracesViewModel.clearTraces() },
        onClearEvents = { eventsViewModel.clearEvents() },
        onForceStop = {
            buildInfo?.packageName?.let { pkg ->
                devicesViewModel.runCommand(selectedDeviceId, "shell am force-stop $pkg")
            }
        },
        onLaunchApp = {
            buildInfo?.packageName?.let { pkg ->
                devicesViewModel.runCommand(
                    selectedDeviceId,
                    "shell monkey -p $pkg -c android.intent.category.LAUNCHER 1",
                )
            }
        },
        onClearAppData = {
            buildInfo?.packageName?.let { pkg ->
                devicesViewModel.runCommand(selectedDeviceId, "shell pm clear $pkg")
            }
        },
        onTakeScreenshot = {
            val timestamp = System.currentTimeMillis()
            val defaultName = "alohomora_screenshot_${timestamp}.png"
            val localPath = if (screenshotDir.isNotEmpty()) {
                "$screenshotDir/$defaultName"
            } else {
                pickSavePath(defaultName, "Save Screenshot", ".png") ?: return@buildCommandActions
            }
            devicesViewModel.takeScreenshot(selectedDeviceId, localPath, screenshotShowToast)
        },
        onRebootDevice = {
            devicesViewModel.runCommand(selectedDeviceId, "reboot")
        },
        onToggleWifi = {
            devicesViewModel.toggleWifi(selectedDeviceId)
        },
        onToggleMobileData = {
            devicesViewModel.toggleMobileData(selectedDeviceId)
        },
        onClearLogcat = {
            devicesViewModel.runCommand(selectedDeviceId, "logcat -c")
        },
        onOpenDeepLinkBuilder = {
            onDismissCommandPalette()
            onOpenDeepLinkBuilder()
        },
        onFocusSearch = {
            onDismissCommandPalette()
            searchFocusTrigger = System.nanoTime()
        },
        onOpenMockRules = {
            onDismissCommandPalette()
            onOpenMockRules()
        },
        onClearErrors = { devToolsViewModel.clearErrors() },
    )

    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(activeSection) {
        rootFocus.requestFocus()
    }

    CompositionLocalProvider(
        LocalCopyFeedback provides { message ->
            copyScope.launch {
                copySnackbarState.currentSnackbarData?.dismiss()
                copySnackbarState.showSnackbar(message)
            }
        },
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.isModifierKeyOnly()) {
                    isModifierPhysicallyDown = event.type == KeyEventType.KeyDown
                    return@onPreviewKeyEvent false
                }
                if (isModifierPhysicallyDown) {
                    isModifierPhysicallyDown = false
                }

                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                if (event.key == Key.Escape) {
                    when {
                        showCommandPalette -> {
                            onDismissCommandPalette(); return@onPreviewKeyEvent true
                        }

                        showHelp -> {
                            onDismissHelp(); return@onPreviewKeyEvent true
                        }

                        showDeepLinkBuilder -> {
                            onDismissDeepLinkBuilder(); return@onPreviewKeyEvent true
                        }

                        showMockRules -> {
                            onDismissMockRules(); return@onPreviewKeyEvent true
                        }

                        selectedTrafficForSheet != null -> {
                            selectedTrafficForSheet = null; return@onPreviewKeyEvent true
                        }

                        selectedErrorForSheet != null -> {
                            selectedErrorForSheet = null; return@onPreviewKeyEvent true
                        }

                        selectedTraceId != null -> {
                            tracesViewModel.closeTrace(); return@onPreviewKeyEvent true
                        }

                        selectedEventId != null -> {
                            eventsViewModel.closeEvent(); return@onPreviewKeyEvent true
                        }
                    }
                    return@onPreviewKeyEvent false
                }

                val navIndex = event.matchesNavigation()
                if (navIndex >= 0 && navIndex < visibleSections.size) {
                    activeSection = visibleSections[navIndex]
                    searchFocusTrigger = System.nanoTime()
                    return@onPreviewKeyEvent true
                }

                if (event.isFocusSearchShortcut()) {
                    searchFocusTrigger = System.nanoTime()
                    return@onPreviewKeyEvent true
                }

                if (event.isClearShortcut()) {
                    when (activeSection) {
                        DesktopSection.Traffic -> trafficViewModel.clearTraffic()
                        DesktopSection.Traces -> tracesViewModel.clearTraces()
                        DesktopSection.Events -> eventsViewModel.clearEvents()
                        DesktopSection.Errors -> devToolsViewModel.clearErrors()
                        DesktopSection.Logcat -> logcatViewModel.clear()
                        else -> {}
                    }
                    return@onPreviewKeyEvent true
                }

                if (event.isScreenshotShortcut() && isAndroid && !selectedDeviceId.isNullOrBlank()) {
                    val timestamp = System.currentTimeMillis()
                    val defaultName = "alohomora_screenshot_${timestamp}.png"
                    val localPath = if (screenshotDir.isNotEmpty()) {
                        "$screenshotDir/$defaultName"
                    } else {
                        pickSavePath(defaultName, "Save Screenshot", ".png")
                    }
                    if (localPath != null) {
                        devicesViewModel.takeScreenshot(
                            selectedDeviceId,
                            localPath,
                            screenshotShowToast,
                        )
                    }
                    return@onPreviewKeyEvent true
                }

                if (event.isDeepLinkShortcut() && isAndroid && !selectedDeviceId.isNullOrBlank()) {
                    onOpenDeepLinkBuilder()
                    return@onPreviewKeyEvent true
                }

                if (event.isMockRulesShortcut() && isConnected) {
                    onOpenMockRules()
                    return@onPreviewKeyEvent true
                }

                if (event.isTogglePropertiesShortcut() && activeSection == DesktopSection.Events) {
                    eventsViewModel.toggleShowProperties()
                    return@onPreviewKeyEvent true
                }

                false
            },
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
        ) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.fillMaxWidth(0.2f),
                        windowInsets = WindowInsets.safeContent,
                        drawerShape = PermanentDrawerShape,
                    ) {
                        Sidebar(
                            connection = devToolsState.connection,
                            activeSection = activeSection,
                            devices = devices,
                            selectedDeviceId = selectedDeviceId,
                            appName = buildInfo?.projectName,
                            onDisconnect = onDisconnectWindow,
                            onSectionClick = {
                                activeSection = it
                                searchFocusTrigger = System.nanoTime()
                            },
                            onOpenCommandPalette = onOpenCommandPalette,
                            isModifierHeld = showModifierBadges,
                            visibleSections = visibleSections,
                        )
                    }
                },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!hasConnectedDevice) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(hostState = devicesViewModel.snackbarHostState) },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ) {
                            NoDevicePanel(onRefresh = { devicesViewModel.refreshDevices() })
                        }
                    } else {
                        AnimatedContent(
                            targetState = activeSection,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                            },
                        ) { section ->
                        when (section) {
                            DesktopSection.Dashboard -> DashboardContent(
                                devToolsViewModel = devToolsViewModel,
                                devicesViewModel = devicesViewModel,
                                selectedDevice = selectedDevice,
                                isRecording = isRecording,
                                onTakeScreenshot = screenshot@{
                                    val timestamp = System.currentTimeMillis()
                                    val defaultName = "alohomora_screenshot_${timestamp}.png"
                                    val localPath = if (screenshotDir.isNotEmpty()) {
                                        "$screenshotDir/$defaultName"
                                    } else {
                                        pickSavePath(defaultName, "Save Screenshot", ".png")
                                            ?: return@screenshot
                                    }
                                    devicesViewModel.takeScreenshot(
                                        selectedDeviceId,
                                        localPath,
                                        screenshotShowToast,
                                    )
                                },
                                onRecordScreen = record@{
                                    if (!isRecording) {
                                        val timestamp = System.currentTimeMillis()
                                        val defaultName = "alohomora_record_${timestamp}.mp4"
                                        val localPath =
                                            pickSavePath(defaultName, "Save Recording", ".mp4")
                                                ?: return@record
                                        val devicePath = "/sdcard/${File(localPath).name}"
                                        recordingDevicePath = devicePath
                                        recordingLocalPath = localPath
                                        isRecording = true
                                        devicesViewModel.startScreenRecord(
                                            selectedDeviceId,
                                            devicePath,
                                        )
                                    } else {
                                        devicesViewModel.stopScreenRecord(
                                            selectedDeviceId,
                                            recordingDevicePath,
                                            recordingLocalPath,
                                        )
                                        isRecording = false
                                        recordingDevicePath = null
                                        recordingLocalPath = null
                                    }
                                },
                                onTrafficItemClick = { selectedTrafficForSheet = it },
                                onEventViewClick = {},
                                onTrafficClick = { activeSection = DesktopSection.Traffic },
                                onEventsClick = { activeSection = DesktopSection.Events },
                                onOpenDeepLinkBuilder = { onOpenDeepLinkBuilder() },
                            )

                            DesktopSection.Logcat -> LogcatPanel(
                                devicesViewModel = devicesViewModel,
                                logcatViewModel = logcatViewModel,
                                selectedDeviceId = selectedDeviceId,
                                buildInfo = buildInfo,
                                searchFocusTrigger = searchFocusTrigger,
                            )

                            DesktopSection.Adb -> AdbToolsPanel(
                                devicesViewModel = devicesViewModel,
                                selectedDeviceId = selectedDeviceId,
                                adbCommandHistory = adbCommandHistory,
                                buildInfo = buildInfo,
                                screenshotDir = screenshotDir,
                                screenshotShowToast = screenshotShowToast,
                            )

                            DesktopSection.Traffic -> TrafficPanel(
                                trafficViewModel = trafficViewModel,
                                networkRulesViewModel = networkRulesViewModel,
                                onLogClick = { selectedTrafficForSheet = it },
                                onOpenMockRules = { onOpenMockRules() },
                                searchFocusTrigger = searchFocusTrigger,
                            )

                            DesktopSection.Traces -> TracesPanel(
                                tracesViewModel = tracesViewModel,
                                onTraceClick = tracesViewModel::openTrace,
                                searchFocusTrigger = searchFocusTrigger,
                            )

                            DesktopSection.Events -> EventsPanel(
                                eventsViewModel = eventsViewModel,
                                searchFocusTrigger = searchFocusTrigger,
                            )

                            DesktopSection.Cache -> CachePanel(
                                cacheViewModel = cacheViewModel,
                                searchFocusTrigger = searchFocusTrigger,
                            )

                            DesktopSection.FeatureFlags -> FeatureFlagsPanel(
                                featureFlagsViewModel = featureFlagsViewModel,
                                searchFocusTrigger = searchFocusTrigger,
                            )

                            DesktopSection.Errors -> ErrorsPanel(
                                devToolsViewModel = devToolsViewModel,
                                onErrorClick = { selectedErrorForSheet = it },
                                searchFocusTrigger = searchFocusTrigger,
                            )

                            DesktopSection.Config -> ConfigPanel(devToolsViewModel = devToolsViewModel)
                            DesktopSection.GitHistory -> GitHistoryPanel(devToolsViewModel = devToolsViewModel)
                            DesktopSection.Database -> DatabasePanel(databaseViewModel = databaseViewModel)
                        }
                        }
                    }

                    AnimatedVisibility(
                        visible = devToolsState.connection is DevToolsConnection.Reconnecting,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        val attempt =
                            (devToolsState.connection as? DevToolsConnection.Reconnecting)?.attempt
                                ?: 1
                        ReconnectingBanner(attempt = attempt)
                    }

                    deviceError?.let { error ->
                        DeviceErrorBanner(
                            message = error,
                            onDismiss = { devToolsViewModel.dismissDeviceError() },
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }

                    val connection = devToolsState.connection
                    if (connection is DevToolsConnection.AwaitingAuth && connection.otpRequired) {
                        OtpPromptDialog(
                            onSubmit = { devToolsViewModel.submitOtp(it) },
                            onCancel = { devToolsViewModel.disconnect() },
                        )
                    }

                    if (devToolsState.switching) {
                        SwitchingOverlay()
                    }
                }
            }

            TrafficDetailsSideSheet(
                traffic = selectedTrafficForSheet,
                devToolsViewModel = devToolsViewModel,
                networkRulesViewModel = networkRulesViewModel,
                onOpenMockRules = { onOpenMockRules() },
                onDismiss = { selectedTrafficForSheet = null },
            )

            val mockRules by networkRulesViewModel.mockRules.collectAsState()
            val mockCurrentSession by networkRulesViewModel.currentSession.collectAsState()
            val mockSessions by networkRulesViewModel.sessions.collectAsState()
            MockRulesSideSheet(
                visible = showMockRules,
                rules = mockRules,
                currentSession = mockCurrentSession,
                sessions = mockSessions,
                onAddRule = networkRulesViewModel::addRule,
                onUpdateRule = networkRulesViewModel::updateRule,
                onDeleteRule = networkRulesViewModel::deleteRule,
                onToggleRule = networkRulesViewModel::toggleRule,
                onToggleAll = networkRulesViewModel::toggleAllRules,
                onLoadSession = networkRulesViewModel::loadSession,
                onSaveSession = networkRulesViewModel::saveCurrentSession,
                onSaveAsSession = networkRulesViewModel::saveAsNewSession,
                onDeleteSession = networkRulesViewModel::deleteSession,
                onDetachSession = networkRulesViewModel::detachSession,
                onExport = networkRulesViewModel::exportSession,
                onImport = networkRulesViewModel::importFromFile,
                onDismiss = { onDismissMockRules() },
            )

            TraceWaterfallSideSheet(
                tracesViewModel = tracesViewModel,
                onDismiss = tracesViewModel::closeTrace,
            )

            EventDetailsSideSheet(
                eventsViewModel = eventsViewModel,
                devToolsViewModel = devToolsViewModel,
                onDismiss = eventsViewModel::closeEvent,
            )

            ErrorDetailsSideSheet(
                error = selectedErrorForSheet,
                devToolsViewModel = devToolsViewModel,
                onDismiss = { selectedErrorForSheet = null },
            )

            val deepLinkHistory by devicesViewModel.deepLinkHistory.collectAsState()
            DeepLinkBuilderSideSheet(
                visible = showDeepLinkBuilder,
                initialUrl = "",
                history = deepLinkHistory,
                onOpen = { url ->
                    devicesViewModel.openDeepLink(selectedDeviceId, url)
                },
                onRemoveHistoryEntry = devicesViewModel::removeDeepLinkEntry,
                onClearHistory = devicesViewModel::clearDeepLinkHistory,
                onDismiss = { onDismissDeepLinkBuilder() },
            )
        }

        SnackbarHost(
            hostState = copySnackbarState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (showCommandPalette) {
            CommandPalette(
                actions = commandActions,
                onDismiss = onDismissCommandPalette,
            )
        }

        if (showHelp) {
            HelpDialog(
                visibleSections = visibleSections,
                actions = commandActions,
                onDismiss = onDismissHelp,
            )
        }
    }
    }
}

@Composable
private fun SwitchingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SWITCHING_SCRIM_ALPHA))
            .clickable(indication = null, interactionSource = null) {},
        contentAlignment = Alignment.Center,
    ) {
        AlohomoraCircularProgressIndicator()
    }
}

@Preview
@Composable
private fun SwitchingOverlayPreview() {
    AppTheme(initialIsDark = true) {
        Surface(modifier = Modifier.size(400.dp, 300.dp)) {
            SwitchingOverlay()
        }
    }
}
