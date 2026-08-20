package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.presentation.model.AdbCommandLogEntry
import io.github.yashkasera.alohomora.desktop.presentation.model.DarkModeOption
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.util.pickApkPath
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenu
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenuItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AdbToolsPanel(
    devicesViewModel: DevicesViewModel,
    selectedDeviceId: String?,
    adbCommandHistory: List<AdbCommandLogEntry>,
    buildInfo: BuildInfo?,
    screenshotDir: String = "",
    screenshotShowToast: Boolean = true,
) {
    val isDeviceSelected = !selectedDeviceId.isNullOrBlank()
    val wifiEnabled by devicesViewModel.wifiEnabled.collectAsState()
    val dataEnabled by devicesViewModel.dataEnabled.collectAsState()
    val devOptions by devicesViewModel.developerOptionsState.collectAsState()
    val customOutput by devicesViewModel.customCommandOutput.collectAsState()

    var packageName by remember { mutableStateOf("") }
    var packageEdited by remember { mutableStateOf(false) }
    var apkPath by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }
    var consoleExpanded by remember { mutableStateOf(false) }
    var customCommand by remember { mutableStateOf("") }

    LaunchedEffect(selectedDeviceId) {
        devicesViewModel.refreshConnectivityState(selectedDeviceId)
        devicesViewModel.refreshDeveloperOptions(selectedDeviceId)
    }

    LaunchedEffect(buildInfo?.packageName) {
        val detected = buildInfo?.packageName
        if (!packageEdited && packageName.isBlank() && !detected.isNullOrBlank()) {
            packageName = detected
        }
    }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "ADB Shortcuts",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = if (isDeviceSelected) "Device: $selectedDeviceId" else "No device selected",
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = devicesViewModel.snackbarHostState)
        },
        bottomBar = {
            AdbConsoleBar(
                expanded = consoleExpanded,
                onToggle = { consoleExpanded = !consoleExpanded },
                history = adbCommandHistory,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 40.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
        ) {
            // -- APP --
            AdbToolsCard("App") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AlohomoraTextField(
                            value = packageName,
                            onValueChange = {
                                packageEdited = true
                                packageName = it
                            },
                            placeholder = "com.example.app",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!buildInfo?.packageName.isNullOrBlank()) {
                            Text(
                                "Detected: ${buildInfo.packageName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    AdbActionButton("Launch", isDeviceSelected && packageName.isNotBlank()) {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell monkey -p $packageName -c android.intent.category.LAUNCHER 1",
                        )
                    }
                    AdbActionButton("Force Stop", isDeviceSelected && packageName.isNotBlank()) {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell am force-stop $packageName",
                        )
                    }
                    AdbActionButton("Clear Data", isDeviceSelected && packageName.isNotBlank()) {
                        devicesViewModel.runCommand(selectedDeviceId, "shell pm clear $packageName")
                    }
                    AdbActionButton("Uninstall", isDeviceSelected && packageName.isNotBlank()) {
                        devicesViewModel.uninstallPackage(selectedDeviceId, packageName)
                    }
                }
            }

            // -- INSTALL APK --
            AdbToolsCard("Install APK") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    AlohomoraTextField(
                        value = apkPath,
                        onValueChange = { apkPath = it },
                        placeholder = "APK path",
                        modifier = Modifier.weight(1f),
                    )
                    AdbActionButton("Browse", enabled = true) {
                        val picked = pickApkPath()
                        if (!picked.isNullOrBlank()) apkPath = picked
                    }
                    AdbActionButton("Install", isDeviceSelected && apkPath.isNotBlank()) {
                        devicesViewModel.installApk(selectedDeviceId, apkPath)
                    }
                }
            }

            // -- CAPTURE --
            AdbToolsCard("Capture") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    AdbActionButton("Screenshot", isDeviceSelected) {
                        val ts = System.currentTimeMillis()
                        val defaultName = "alohomora_screenshot_$ts.png"
                        val localPath = if (screenshotDir.isNotEmpty()) {
                            "$screenshotDir/$defaultName"
                        } else {
                            pickSavePath(defaultName, "Save Screenshot", ".png")
                                ?: return@AdbActionButton
                        }
                        devicesViewModel.takeScreenshot(
                            selectedDeviceId,
                            localPath,
                            screenshotShowToast,
                        )
                    }
                    AlohomoraFilledButton(
                        text = if (isRecording) "Stop Recording" else "Record Screen",
                        onClick = {
                            if (!isRecording) {
                                val ts = System.currentTimeMillis()
                                val localPath = pickSavePath(
                                    "alohomora_record_$ts.mp4",
                                    "Save Recording",
                                    ".mp4",
                                )
                                    ?: return@AlohomoraFilledButton
                                val devicePath = "/sdcard/${File(localPath).name}"
                                recordingDevicePath = devicePath
                                recordingLocalPath = localPath
                                isRecording = true
                                devicesViewModel.startScreenRecord(selectedDeviceId, devicePath)
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
                        enabled = isDeviceSelected,
                        containerColor = if (isRecording) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        contentColor = MaterialTheme.colorScheme.background,
                    )
                    AdbActionButton("Bugreport", isDeviceSelected) {
                        val ts = System.currentTimeMillis()
                        val localPath =
                            pickSavePath("alohomora_bugreport_$ts.zip", "Save Bugreport", ".zip")
                                ?: return@AdbActionButton
                        val devicePath = "/sdcard/${File(localPath).name}"
                        devicesViewModel.takeBugreport(selectedDeviceId, devicePath, localPath)
                    }
                }
            }

            // -- CONNECTIVITY --
            AdbToolsCard("Connectivity") {
                ToggleRow(
                    label = "Wi-Fi",
                    subtitle = wifiEnabled?.let { if (it) "Enabled" else "Disabled" } ?: "Unknown",
                    checked = wifiEnabled == true,
                    enabled = isDeviceSelected && wifiEnabled != null,
                    onCheckedChange = { devicesViewModel.toggleWifi(selectedDeviceId) },
                )
                ToggleRow(
                    label = "Mobile data",
                    subtitle = dataEnabled?.let { if (it) "Enabled" else "Disabled" } ?: "Unknown",
                    checked = dataEnabled == true,
                    enabled = isDeviceSelected && dataEnabled != null,
                    onCheckedChange = { devicesViewModel.toggleMobileData(selectedDeviceId) },
                )
            }

            // -- DEVELOPER OPTIONS --
            AdbToolsCard("Developer Options") {
                ToggleRow(
                    label = "Show taps",
                    subtitle = when (devOptions.showTaps) {
                        true -> "Visible"
                        false -> "Hidden"
                        null -> "Unknown"
                    },
                    checked = devOptions.showTaps == true,
                    enabled = isDeviceSelected && devOptions.showTaps != null,
                    onCheckedChange = { devicesViewModel.toggleShowTaps(selectedDeviceId) },
                )
                ToggleRow(
                    label = "Layout bounds",
                    subtitle = when (devOptions.showLayoutBounds) {
                        true -> "Visible"
                        false -> "Hidden"
                        null -> "Unknown"
                    },
                    checked = devOptions.showLayoutBounds == true,
                    enabled = isDeviceSelected && devOptions.showLayoutBounds != null,
                    onCheckedChange = { devicesViewModel.toggleLayoutBounds(selectedDeviceId) },
                )
                ToggleRow(
                    label = "Animations",
                    subtitle = when (devOptions.animationsDisabled) {
                        true -> "Disabled"
                        false -> "Enabled"
                        null -> "Unknown"
                    },
                    checked = devOptions.animationsDisabled != true,
                    enabled = isDeviceSelected && devOptions.animationsDisabled != null,
                    onCheckedChange = { devicesViewModel.toggleAnimations(selectedDeviceId) },
                )
                ToggleRow(
                    label = "Don't keep activities",
                    subtitle = when (devOptions.dontKeepActivities) {
                        true -> "Enabled"
                        false -> "Disabled"
                        null -> "Unknown"
                    },
                    checked = devOptions.dontKeepActivities == true,
                    enabled = isDeviceSelected && devOptions.dontKeepActivities != null,
                    onCheckedChange = { devicesViewModel.toggleDontKeepActivities(selectedDeviceId) },
                )
                DarkModeRow(
                    current = devOptions.darkMode,
                    enabled = isDeviceSelected && devOptions.darkMode != null,
                    onSelect = { devicesViewModel.setDarkMode(selectedDeviceId, it) },
                )
                FontScaleRow(
                    current = devOptions.fontScale,
                    enabled = isDeviceSelected && devOptions.fontScale != null,
                    onSelect = { devicesViewModel.setFontScale(selectedDeviceId, it) },
                )
            }

            // -- SYSTEM --
            AdbToolsCard("System") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    AdbActionButton("Clear Logcat", isDeviceSelected) {
                        devicesViewModel.runCommand(selectedDeviceId, "logcat -c")
                    }
                    AdbActionButton("Reboot", isDeviceSelected) {
                        devicesViewModel.runCommand(selectedDeviceId, "reboot")
                    }
                    AdbActionButton("Restart ADB", enabled = true) {
                        devicesViewModel.restartAdb()
                    }
                }
            }

            // -- CUSTOM COMMAND --
            AdbToolsCard("Custom Command") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    AlohomoraTextField(
                        value = customCommand,
                        onValueChange = { customCommand = it },
                        placeholder = "dumpsys battery",
                        modifier = Modifier
                            .weight(1f)
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && isDeviceSelected && customCommand.isNotBlank()) {
                                    devicesViewModel.runCustomCommand(
                                        selectedDeviceId,
                                        customCommand,
                                    )
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                    AlohomoraFilledButton(
                        text = "Run",
                        onClick = {
                            devicesViewModel.runCustomCommand(
                                selectedDeviceId,
                                customCommand,
                            )
                        },
                        enabled = isDeviceSelected && customCommand.isNotBlank(),
                    )
                }
                Text(
                    "Runs as: adb shell <command>",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                val output = customOutput
                if (output != null) {
                    AlohomoraCodeBlock(
                        content = output.output,
                        accentBorder = output.isError,
                        modifier = Modifier.heightIn(max = 300.dp),
                    )
                    AlohomoraTextButton(
                        text = "Clear output",
                        onClick = { devicesViewModel.clearCustomCommandOutput() },
                        uppercase = false,
                        contentColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
        }
    }
}

// -- Reusable card wrapper matching ConfigPanel's pattern --

@Composable
private fun AdbToolsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        AlohomoraOutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.dimens.margin.lg,
                    vertical = MaterialTheme.dimens.margin.md,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                content()
            }
        }
    }
}

// -- Toggle row used by Connectivity and Developer Options --

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (checked) "On" else "Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
            AlohomoraSwitch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
                enabled = enabled,
            )
        }
    }
}

// -- Dropdown row for Dark Mode --

@Composable
private fun DarkModeRow(
    current: DarkModeOption?,
    enabled: Boolean,
    onSelect: (DarkModeOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Dark mode", style = MaterialTheme.typography.bodyMedium)
            Text(
                current?.label ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
        Box {
            AlohomoraOutlinedButton(
                text = current?.label ?: "—",
                onClick = { expanded = true },
                enabled = enabled,
            )
            AlohomoraDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DarkModeOption.entries.forEach { option ->
                    AlohomoraDropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// -- Dropdown row for Font Scale --

private val FONT_SCALE_OPTIONS = listOf(
    0.85f to "0.85x",
    1.0f to "1.0x (Default)",
    1.15f to "1.15x",
    1.30f to "1.30x",
)

@Composable
private fun FontScaleRow(
    current: Float?,
    enabled: Boolean,
    onSelect: (Float) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = current?.let { scale ->
        FONT_SCALE_OPTIONS.firstOrNull { it.first == scale }?.second ?: "${scale}x"
    } ?: "Unknown"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Font scale", style = MaterialTheme.typography.bodyMedium)
            Text(
                currentLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
        Box {
            AlohomoraOutlinedButton(
                text = currentLabel,
                onClick = { expanded = true },
                enabled = enabled,
            )
            AlohomoraDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                FONT_SCALE_OPTIONS.forEach { (scale, label) ->
                    AlohomoraDropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(scale)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// -- Shared button helper --

@Composable
private fun AdbActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AlohomoraOutlinedButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
    )
}

// -- Console bar --

@Composable
private fun AdbConsoleBar(
    expanded: Boolean,
    onToggle: () -> Unit,
    history: List<AdbCommandLogEntry>,
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        AlohomoraHorizontalDivider()
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "ADB Console",
                    style = MaterialTheme.typography.titleSmall,
                )
                AlohomoraTextButton(
                    text = if (expanded) "Hide" else "Show",
                    onClick = onToggle,
                    uppercase = false,
                    contentColor = MaterialTheme.alohomoraColors.accent,
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
                val rendered = history.asReversed()
                if (rendered.isEmpty()) {
                    Text(
                        "No commands yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.shapes.small,
                            )
                            .padding(MaterialTheme.dimens.margin.sm),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                    ) {
                        items(rendered) { entry ->
                            Text(
                                text = formatLogEntry(entry),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entry.isError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val logTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

private fun formatLogEntry(entry: AdbCommandLogEntry): String {
    val time = logTimeFormatter.format(Instant.ofEpochMilli(entry.timestamp))
    val deviceLabel = entry.deviceId?.let { "[$it]" } ?: "[no-device]"
    val base = "[$time] $deviceLabel ${entry.command}"
    val output = entry.output
    return if (output != null) {
        "$base\n  → ${output.lines().joinToString("\n    ")}"
    } else {
        base
    }
}
