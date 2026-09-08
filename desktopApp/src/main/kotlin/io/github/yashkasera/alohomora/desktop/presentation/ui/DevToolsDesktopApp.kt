package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.key.KeyEventType
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
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.HelpDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.LocalCopyFeedback
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.LocalSideSheetHost
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.OtpPromptDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.RegisterSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SideSheetHostState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SideSheetId
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.buildCommandActions
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.AdbToolsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.CachePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ConfigPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DashboardContent
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DatabasePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DeepLinkBuilderSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DeepLinkCatalogSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DeepLinkDefEditorSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ErrorDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ErrorsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.FeatureFlagsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.GitHistoryPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.JourneyEditorSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.JourneyListSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.JourneyValidationPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.LogcatPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.MockRulesSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.PluginDataPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TraceWaterfallSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TracesPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TrafficDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TrafficPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.theme.AlohomoraMotion
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.CacheViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.FeatureFlagViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.JourneyViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TracesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TrafficViewModel
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.AlohomoraLoadingIndicator
import io.github.yashkasera.alohomora.ui.theme.AlohomoraDrawerShape
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SWITCHING_SCRIM_ALPHA = 0.40f

private const val JOURNEY_EVENT_PICKER_LIMIT = 50

@Composable
fun DevToolsDesktopApp(
    modifier: Modifier = Modifier,
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    databaseViewModel: DatabaseViewModel,
    cacheViewModel: CacheViewModel,
    featureFlagsViewModel: FeatureFlagViewModel,
    pluginDataViewModel: io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PluginDataViewModel,
    tracesViewModel: TracesViewModel,
    eventsViewModel: EventsViewModel,
    trafficViewModel: TrafficViewModel,
    networkRulesViewModel: NetworkRulesViewModel,
    journeyViewModel: JourneyViewModel,
    configRepoViewModel: io.github.yashkasera.alohomora.desktop.presentation.viewmodel.ConfigRepoViewModel,
    deepLinkCatalogViewModel: io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DeepLinkCatalogViewModel,
    initialDeviceId: String? = null,
    sideSheetHost: SideSheetHostState,
    onShowSettings: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onResetZoom: () -> Unit = {},
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
    val configRepoUi by configRepoViewModel.uiState.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }
    var selectedTrafficForSheet by remember { mutableStateOf<TrafficEntry?>(null) }
    var selectedErrorForSheet by remember {
        mutableStateOf<io.github.yashkasera.alohomora.common.Error?>(
            null,
        )
    }
    var selectedDeviceId by remember(initialDeviceId) { mutableStateOf(initialDeviceId) }
    var isModifierPhysicallyDown by remember { mutableStateOf(false) }
    var showModifierBadges by remember { mutableStateOf(false) }

    val anySideSheetOpen = sideSheetHost.isAnyOpen

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
            sideSheetHost.close(SideSheetId.CommandPalette)
            onShowSettings()
        },
        onShowHelp = {
            sideSheetHost.close(SideSheetId.CommandPalette)
            sideSheetHost.open(SideSheetId.Help)
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
            sideSheetHost.close(SideSheetId.CommandPalette)
            sideSheetHost.open(SideSheetId.DeepLinkBuilder)
        },
        onFocusSearch = {
            sideSheetHost.close(SideSheetId.CommandPalette)
            searchFocusTrigger = System.nanoTime()
        },
        onOpenMockRules = {
            sideSheetHost.close(SideSheetId.CommandPalette)
            sideSheetHost.open(SideSheetId.MockRules)
        },
        onOpenJourneys = {
            sideSheetHost.close(SideSheetId.CommandPalette)
            sideSheetHost.open(SideSheetId.Journeys)
        },
        onOpenDeepLinkCatalog = {
            sideSheetHost.close(SideSheetId.CommandPalette)
            sideSheetHost.open(SideSheetId.DeepLinkCatalog)
        },
        developerMode = configRepoUi.developerMode,
        onGitSync = {
            sideSheetHost.close(SideSheetId.CommandPalette)
            configRepoViewModel.sync()
        },
        onRevealRepo = {
            sideSheetHost.close(SideSheetId.CommandPalette)
            configRepoViewModel.revealRepo()
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
        LocalSideSheetHost provides sideSheetHost,
    ) {
        Box(
            modifier = modifier
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

                    // Escape is handled at the window level (see DeviceWindow) so it works regardless of
                    // which panel or sheet holds focus.

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
                        sideSheetHost.open(SideSheetId.DeepLinkBuilder)
                        return@onPreviewKeyEvent true
                    }

                    if (event.isMockRulesShortcut() && isConnected) {
                        sideSheetHost.open(SideSheetId.MockRules)
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
                            drawerShape = AlohomoraDrawerShape,
                        ) {
                            Sidebar(
                                connection = devToolsState.connection,
                                activeSection = activeSection,
                                devices = devices,
                                selectedDeviceId = selectedDeviceId,
                                appName = buildInfo?.appName,
                                onDisconnect = onDisconnectWindow,
                                onReconnect = { devToolsViewModel.reconnect() },
                                onSectionClick = {
                                    activeSection = it
                                    searchFocusTrigger = System.nanoTime()
                                },
                                onOpenCommandPalette = { sideSheetHost.open(SideSheetId.CommandPalette) },
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
                            // Motion specs are read here (a @Composable scope) and captured; the
                            // transitionSpec lambda below is not composable and cannot read them itself.
                            val sectionFade = AlohomoraMotion.scrimFade
                            val sectionEnterSlide = AlohomoraMotion.sectionEnter
                            val sectionExitSlide = AlohomoraMotion.sectionExit
                            AnimatedContent(
                                targetState = activeSection,
                                transitionSpec = {
                                    // Expressive spatial swap: incoming panel eases in from a few px right
                                    // on the spatial spring, outgoing leaves left on the faster spec, both
                                    // cross-fading on the effects spec. Small travel (it/24) so content
                                    // settles rather than shoves.
                                    (fadeIn(sectionFade)
                                        togetherWith
                                        fadeOut(sectionFade))
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
                                            val defaultName =
                                                "alohomora_screenshot_${timestamp}.png"
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
                                                val defaultName =
                                                    "alohomora_record_${timestamp}.mp4"
                                                val localPath =
                                                    pickSavePath(
                                                        defaultName,
                                                        "Save Recording",
                                                        ".mp4",
                                                    )
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
                                        onOpenDeepLinkBuilder = { sideSheetHost.open(SideSheetId.DeepLinkBuilder) },
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
                                        onOpenMockRules = { sideSheetHost.open(SideSheetId.MockRules) },
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
                                        onOpenJourneys = { sideSheetHost.open(SideSheetId.Journeys) },
                                    )

                                    DesktopSection.Cache -> CachePanel(
                                        cacheViewModel = cacheViewModel,
                                        searchFocusTrigger = searchFocusTrigger,
                                    )

                                    DesktopSection.FeatureFlags -> FeatureFlagsPanel(
                                        featureFlagsViewModel = featureFlagsViewModel,
                                        searchFocusTrigger = searchFocusTrigger,
                                    )

                                    DesktopSection.PluginData -> PluginDataPanel(
                                        pluginDataViewModel = pluginDataViewModel,
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
                    onOpenMockRules = { sideSheetHost.open(SideSheetId.MockRules) },
                    onDismiss = { selectedTrafficForSheet = null },
                )

                val mockRules by networkRulesViewModel.mockRules.collectAsState()
                val mockCurrentSession by networkRulesViewModel.currentSession.collectAsState()
                val mockSessions by networkRulesViewModel.sessions.collectAsState()
                val mockProposal by networkRulesViewModel.lastProposal.collectAsState()
                val mockShareMessage by networkRulesViewModel.shareMessage.collectAsState()
                val mockTeamSessions by networkRulesViewModel.teamSessions.collectAsState()
                val mockRulesVisible = sideSheetHost.isOpen(SideSheetId.MockRules)
                LaunchedEffect(mockRulesVisible, configRepoUi.isConnected) {
                    if (mockRulesVisible) networkRulesViewModel.refreshTeam()
                }
                MockRulesSideSheet(
                    visible = mockRulesVisible,
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
                    teamSessions = mockTeamSessions,
                    onShareSession = networkRulesViewModel::shareCurrentSession,
                    onLoadTeamSession = networkRulesViewModel::loadTeamSession,
                    lastProposal = mockProposal,
                    shareMessage = mockShareMessage,
                    onOpenUrl = { url ->
                        runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
                    },
                    onDismiss = { sideSheetHost.close(SideSheetId.MockRules) },
                )

                val journeyUi by journeyViewModel.uiState.collectAsState()
                val eventsForJourney by eventsViewModel.uiState.collectAsState()
                JourneyListSideSheet(
                    visible = sideSheetHost.isOpen(SideSheetId.Journeys),
                    state = journeyUi,
                    teamConnected = configRepoUi.isConnected,
                    onScopeChange = journeyViewModel::onScopeChange,
                    onQueryChange = journeyViewModel::onQueryChange,
                    onOpen = journeyViewModel::editExisting,
                    onNew = journeyViewModel::newJourney,
                    onDelete = journeyViewModel::deleteJourney,
                    onShare = journeyViewModel::share,
                    onSync = configRepoViewModel::sync,
                    onOpenUrl = { url ->
                        runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
                    },
                    onDismiss = { sideSheetHost.close(SideSheetId.Journeys) },
                )
                JourneyEditorSideSheet(
                    draft = journeyUi.editorDraft,
                    isNew = journeyUi.editorIsNew,
                    report = journeyUi.lastReport,
                    recentEvents = eventsForJourney.events.take(JOURNEY_EVENT_PICKER_LIMIT),
                    onNameChange = { name -> journeyViewModel.editDraft { it.copy(name = name) } },
                    onDescriptionChange = { desc ->
                        journeyViewModel.editDraft { it.copy(description = desc) }
                    },
                    onToggleOrdered = { ordered ->
                        journeyViewModel.editDraft { it.copy(ordered = ordered) }
                    },
                    onAddStepFromEvent = journeyViewModel::promoteEventToStep,
                    onRemoveStep = journeyViewModel::removeStep,
                    onStepSelectorChange = journeyViewModel::setStepSelector,
                    onStepAssertionsChange = journeyViewModel::setStepAssertions,
                    onStepOnRepeatChange = journeyViewModel::setStepOnRepeat,
                    onValidate = journeyViewModel::validate,
                    onValidateLive = {
                        journeyUi.editorDraft?.let { journeyViewModel.startLiveValidation(it) }
                    },
                    onDismiss = journeyViewModel::closeEditor,
                )
                JourneyValidationPanel(
                    visible = journeyUi.liveJourneyId != null,
                    journeyName = journeyUi.liveJourneyName,
                    report = journeyUi.lastReport,
                    onClose = journeyViewModel::stopLiveValidation,
                )

                val catalogUi by deepLinkCatalogViewModel.uiState.collectAsState()
                DeepLinkCatalogSideSheet(
                    visible = sideSheetHost.isOpen(SideSheetId.DeepLinkCatalog),
                    state = catalogUi,
                    teamConnected = configRepoUi.isConnected,
                    onScopeChange = deepLinkCatalogViewModel::onScopeChange,
                    onQueryChange = deepLinkCatalogViewModel::onQueryChange,
                    onModuleFilterChange = deepLinkCatalogViewModel::onModuleFilterChange,
                    onOpen = deepLinkCatalogViewModel::editExisting,
                    onNew = deepLinkCatalogViewModel::newDef,
                    onDelete = deepLinkCatalogViewModel::deleteDef,
                    onShare = deepLinkCatalogViewModel::share,
                    onSync = configRepoViewModel::sync,
                    onOpenUrl = { url ->
                        runCatching {
                            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                        }
                    },
                    onDismiss = { sideSheetHost.close(SideSheetId.DeepLinkCatalog) },
                )
                DeepLinkDefEditorSideSheet(
                    draft = catalogUi.editorDraft,
                    fieldErrors = catalogUi.fieldErrors,
                    validationErrors = catalogUi.validationErrors,
                    onNameChange = { v -> deepLinkCatalogViewModel.editDraft { it.copy(name = v) } },
                    onModuleChange = { v -> deepLinkCatalogViewModel.editDraft { it.copy(module = v) } },
                    onFlowChange = { v ->
                        deepLinkCatalogViewModel.editDraft { it.copy(flow = v.ifBlank { null }) }
                    },
                    onDescriptionChange = { v ->
                        deepLinkCatalogViewModel.editDraft {
                            it.copy(
                                description = v,
                            )
                        }
                    },
                    onUriTemplateChange = { v ->
                        deepLinkCatalogViewModel.editDraft {
                            it.copy(
                                uriTemplate = v,
                            )
                        }
                    },
                    onParamsChange = { params -> deepLinkCatalogViewModel.editDraft { it.copy(params = params) } },
                    onExamplesChange = { examples ->
                        deepLinkCatalogViewModel.editDraft { it.copy(examples = examples) }
                    },
                    onFire = { url -> devicesViewModel.openDeepLink(selectedDeviceId, url) },
                    onDismiss = deepLinkCatalogViewModel::closeEditor,
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
                    visible = sideSheetHost.isOpen(SideSheetId.DeepLinkBuilder),
                    initialUrl = "",
                    history = deepLinkHistory,
                    onOpen = { url ->
                        devicesViewModel.openDeepLink(selectedDeviceId, url)
                    },
                    onRemoveHistoryEntry = devicesViewModel::removeDeepLinkEntry,
                    onClearHistory = devicesViewModel::clearDeepLinkHistory,
                    onOpenCatalog = {
                        sideSheetHost.close(SideSheetId.DeepLinkBuilder)
                        sideSheetHost.open(SideSheetId.DeepLinkCatalog)
                    },
                    onSaveToCatalog = { url ->
                        deepLinkCatalogViewModel.createFromUrl(url)
                        sideSheetHost.close(SideSheetId.DeepLinkBuilder)
                        sideSheetHost.open(SideSheetId.DeepLinkCatalog)
                    },
                    onDismiss = { sideSheetHost.close(SideSheetId.DeepLinkBuilder) },
                )
            }

            SnackbarHost(
                hostState = copySnackbarState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            RegisterSideSheet(
                visible = sideSheetHost.isOpen(SideSheetId.CommandPalette),
                onDismiss = { sideSheetHost.close(SideSheetId.CommandPalette) },
            )
            if (sideSheetHost.isOpen(SideSheetId.CommandPalette)) {
                CommandPalette(
                    actions = commandActions,
                    onDismiss = { sideSheetHost.close(SideSheetId.CommandPalette) },
                )
            }

            RegisterSideSheet(
                visible = sideSheetHost.isOpen(SideSheetId.Help),
                onDismiss = { sideSheetHost.close(SideSheetId.Help) },
            )
            if (sideSheetHost.isOpen(SideSheetId.Help)) {
                HelpDialog(
                    visibleSections = visibleSections,
                    actions = commandActions,
                    onDismiss = { sideSheetHost.close(SideSheetId.Help) },
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
        AlohomoraLoadingIndicator()
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
