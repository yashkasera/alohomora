package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
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
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.presentation.model.AdbCommandLogEntry
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.util.pickApkPath
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
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
) {
    val isDeviceSelected = !selectedDeviceId.isNullOrBlank()
    val wifiEnabled by devicesViewModel.wifiEnabled.collectAsState()
    val dataEnabled by devicesViewModel.dataEnabled.collectAsState()
    var packageName by remember { mutableStateOf("") }
    var packageEdited by remember { mutableStateOf(false) }
    var apkPath by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }
    var consoleExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedDeviceId) {
        devicesViewModel.refreshConnectivityState(selectedDeviceId)
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
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 40.dp, vertical = 20.dp)
                .fillMaxWidth(),
        ) {
            SectionHeader("App")
            AdbRow(title = "Package", subtitle = buildInfo?.packageName?.let { "Detected: $it" }) {
                AlohomoraTextField(
                    value = packageName,
                    onValueChange = {
                        packageEdited = true
                        packageName = it
                    },
                    placeholder = "com.example.app",
                    modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.margin.sm),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AdbActionButton(
                    text = "Launch",
                    enabled = isDeviceSelected && packageName.isNotBlank(),
                    onClick = {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell monkey -p $packageName -c android.intent.category.LAUNCHER 1",
                        )
                    },
                )
                AdbActionButton(
                    text = "Force Stop",
                    enabled = isDeviceSelected && packageName.isNotBlank(),
                    onClick = {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell am force-stop $packageName",
                        )
                    },
                )
                AdbActionButton(
                    text = "Clear Data",
                    enabled = isDeviceSelected && packageName.isNotBlank(),
                    onClick = {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell pm clear $packageName",
                        )
                    },
                )
                AdbActionButton(
                    text = "Uninstall",
                    enabled = isDeviceSelected && packageName.isNotBlank(),
                    onClick = {
                        devicesViewModel.uninstallPackage(selectedDeviceId, packageName)
                    },
                )
            }

            SectionHeader("Install APK")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AlohomoraTextField(
                    value = apkPath,
                    onValueChange = { apkPath = it },
                    placeholder = "APK path",
                    modifier = Modifier.weight(1f),
                )
                AdbActionButton(
                    text = "Browse",
                    enabled = true,
                    onClick = {
                        val picked = pickApkPath()
                        if (!picked.isNullOrBlank()) apkPath = picked
                    },
                )
                AdbActionButton(
                    text = "Install",
                    enabled = isDeviceSelected && apkPath.isNotBlank(),
                    onClick = { devicesViewModel.installApk(selectedDeviceId, apkPath) },
                )
            }

            SectionHeader("Capture")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.margin.sm),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AdbActionButton(
                    text = "Screenshot",
                    enabled = isDeviceSelected,
                    onClick = {
                        val ts = System.currentTimeMillis()
                        val localPath = pickSavePath("alohomora_screenshot_$ts.png", "Save Screenshot", ".png")
                            ?: return@AdbActionButton
                        devicesViewModel.takeScreenshot(selectedDeviceId, localPath)
                    },
                )
                AlohomoraFilledButton(
                    text = if (isRecording) "Stop Recording" else "Record Screen",
                    onClick = {
                        if (!isRecording) {
                            val ts = System.currentTimeMillis()
                            val localPath = pickSavePath("alohomora_record_$ts.mp4", "Save Recording", ".mp4")
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
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                )
                AdbActionButton(
                    text = "Bugreport",
                    enabled = isDeviceSelected,
                    onClick = {
                        val ts = System.currentTimeMillis()
                        val localPath = pickSavePath("alohomora_bugreport_$ts.zip", "Save Bugreport", ".zip")
                            ?: return@AdbActionButton
                        val devicePath = "/sdcard/${File(localPath).name}"
                        devicesViewModel.takeBugreport(selectedDeviceId, devicePath, localPath)
                    },
                )
            }

            SectionHeader("Device")
            AdbRow(
                title = "Wi-Fi",
                subtitle = wifiEnabled?.let { if (it) "Enabled" else "Disabled" } ?: "Unknown",
            ) {
                SwitchRow(
                    checked = wifiEnabled == true,
                    enabled = isDeviceSelected && wifiEnabled != null,
                    onCheckedChange = { devicesViewModel.toggleWifi(selectedDeviceId) },
                )
            }
            AdbRow(
                title = "Mobile data",
                subtitle = dataEnabled?.let { if (it) "Enabled" else "Disabled" } ?: "Unknown",
            ) {
                SwitchRow(
                    checked = dataEnabled == true,
                    enabled = isDeviceSelected && dataEnabled != null,
                    onCheckedChange = { devicesViewModel.toggleMobileData(selectedDeviceId) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.margin.sm),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AdbActionButton(
                    text = "Clear Logcat",
                    enabled = isDeviceSelected,
                    onClick = { devicesViewModel.runCommand(selectedDeviceId, "logcat -c") },
                )
                AdbActionButton(
                    text = "Reboot",
                    enabled = isDeviceSelected,
                    onClick = { devicesViewModel.runCommand(selectedDeviceId, "reboot") },
                )
                AdbActionButton(
                    text = "Restart ADB",
                    enabled = true,
                    onClick = { devicesViewModel.restartAdb() },
                )
            }
        }
    }

}

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
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(rendered) { entry ->
                            Text(
                                text = formatLogEntry(entry),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

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

@Composable
private fun AdbRow(
    title: String,
    subtitle: String?,
    trailingContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.margin.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
        Box(modifier = Modifier.widthIn(min = 220.dp)) {
            trailingContent()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun SwitchRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (checked) "On" else "Off",
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
        AlohomoraSwitch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            enabled = enabled,
        )
    }
}

private val logTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

private fun formatLogEntry(entry: AdbCommandLogEntry): String {
    val time = logTimeFormatter.format(Instant.ofEpochMilli(entry.timestamp))
    val deviceLabel = entry.deviceId?.let { "[$it]" } ?: "[no-device]"
    return "[$time] $deviceLabel ${entry.command}"
}

