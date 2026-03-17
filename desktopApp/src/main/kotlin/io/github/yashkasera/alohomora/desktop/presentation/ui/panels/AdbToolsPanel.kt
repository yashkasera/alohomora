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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.presentation.model.AdbCommandLogEntry
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.util.pickApkPath
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.theme.CanvasSuccessGreen
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
    actionMessage: String?,
    actionError: String?,
) {
    val isDeviceSelected = !selectedDeviceId.isNullOrBlank()
    val wifiEnabled by devicesViewModel.wifiEnabled.collectAsState()
    val dataEnabled by devicesViewModel.dataEnabled.collectAsState()
    var packageName by remember { mutableStateOf("") }
    var packageEdited by remember { mutableStateOf(false) }
    var apkPath by remember { mutableStateOf("") }
    var deepLinkUrl by remember { mutableStateOf("") }
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
                subtitle = "Common device actions for developers and QA",
                showDivider = scrollState.canScrollBackward
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                AlohomoraHorizontalDivider()
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "ADB Console",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(
                            onClick = {
                                consoleExpanded = !consoleExpanded
                            },
                        ) {
                            Text(
                                if (consoleExpanded) "Hide" else "Show",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1A56DB),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (consoleExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val rendered = adbCommandHistory.asReversed()
                        if (rendered.isEmpty()) {
                            Text(
                                "No commands yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(rendered) { entry ->
                                    Text(
                                        text = formatLogEntry(entry),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(it)
                .padding(horizontal = 40.dp, vertical = 20.dp)
                .fillMaxWidth(),
        ) {
            SectionHeader("App Targets")
            AdbRow(
                title = "App package",
                subtitle = buildInfo?.packageName?.let { "Detected: $it" }
                    ?: "Set the app package for app actions",
            ) {
                AlohomoraTextField(
                    value = packageName,
                    onValueChange = {
                        packageEdited = true
                        packageName = it
                    },
                    placeholder = "com.example.app",
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
                )
            }
            AdbRow(
                title = "Deep link URL",
                subtitle = "Launch a deep link in the app",
            ) {
                AlohomoraTextField(
                    value = deepLinkUrl,
                    onValueChange = { deepLinkUrl = it },
                    placeholder = "myapp://home",
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
                )
            }
            AdbRow(
                title = "Install APK",
                subtitle = "Install or update an APK on the device",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlohomoraTextField(
                        value = apkPath,
                        onValueChange = { apkPath = it },
                        placeholder = "APK path",
                        singleLine = true,
                        modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AlohomoraFilledButton(
                        text = "Browse",
                        onClick = {
                            val picked = pickApkPath()
                            if (!picked.isNullOrBlank()) {
                                apkPath = picked
                            }
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AlohomoraFilledButton(
                        text = "Install",
                        onClick = { devicesViewModel.installApk(selectedDeviceId, apkPath) },
                        enabled = isDeviceSelected,
                    )
                }
            }
            AdbRow(
                title = "Uninstall app",
                subtitle = "Remove the app from the device",
            ) {
                AdbActionButton(
                    text = "Uninstall",
                    enabled = isDeviceSelected,
                    onClick = {
                        devicesViewModel.uninstallPackage(
                            selectedDeviceId,
                            packageName,
                        )
                    },
                )
            }
            AdbRow(
                title = "Clear app data",
                subtitle = "Reset app storage and cache",
            ) {
                AdbActionButton(
                    text = "Clear Data",
                    enabled = isDeviceSelected,
                    onClick = {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell pm clear $packageName",
                        )
                    },
                )
            }
            AdbRow(
                title = "Force stop app",
                subtitle = "Terminate the app process",
            ) {
                AdbActionButton(
                    text = "Force Stop",
                    enabled = isDeviceSelected,
                    onClick = {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell am force-stop $packageName",
                        )
                    },
                )
            }
            AdbRow(
                title = "Launch app",
                subtitle = "Start the main launcher activity",
            ) {
                AdbActionButton(
                    text = "Launch",
                    enabled = isDeviceSelected,
                    onClick = {
                        devicesViewModel.runCommand(
                            selectedDeviceId,
                            "shell monkey -p $packageName -c android.intent.category.LAUNCHER 1",
                        )
                    },
                )
            }
            AdbRow(
                title = "Open deep link",
                subtitle = "Send a deep link to the device",
            ) {
                AdbActionButton(
                    text = "Open",
                    enabled = isDeviceSelected,
                    onClick = { devicesViewModel.openDeepLink(selectedDeviceId, deepLinkUrl) },
                )
            }

            SectionHeader("Capture")
            AdbRow(
                title = "Screenshot",
                subtitle = "Save a PNG screenshot locally",
            ) {
                AdbActionButton(
                    text = "Take Screenshot",
                    enabled = isDeviceSelected,
                    onClick = {
                        val timestamp = System.currentTimeMillis()
                        val defaultName = "alohomora_screenshot_${timestamp}.png"
                        val localPath = pickSavePath(defaultName, "Save Screenshot", ".png")
                            ?: return@AdbActionButton
                        val devicePath = "/sdcard/${File(localPath).name}"
                        devicesViewModel.takeScreenshot(selectedDeviceId, devicePath, localPath)
                    },
                )
            }
            AdbRow(
                title = "Screen recording",
                subtitle = if (isRecording) "Recording…" else "Start or stop screen recording",
            ) {
                AlohomoraFilledButton(
                    text = if (isRecording) "Stop Recording" else "Start Recording",
                    onClick = {
                        if (!isRecording) {
                            val timestamp = System.currentTimeMillis()
                            val defaultName = "alohomora_record_${timestamp}.mp4"
                            val localPath = pickSavePath(defaultName, "Save Recording", ".mp4")
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
                    containerColor = if (isRecording) Color(0xFFC62828) else MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                )
            }
            AdbRow(
                title = "Bugreport",
                subtitle = "Save a bugreport ZIP locally",
            ) {
                AdbActionButton(
                    text = "Take Bugreport",
                    enabled = isDeviceSelected,
                    onClick = {
                        val timestamp = System.currentTimeMillis()
                        val defaultName = "alohomora_bugreport_${timestamp}.zip"
                        val localPath = pickSavePath(defaultName, "Save Bugreport", ".zip")
                            ?: return@AdbActionButton
                        val devicePath = "/sdcard/${File(localPath).name}"
                        devicesViewModel.takeBugreport(selectedDeviceId, devicePath, localPath)
                    },
                )
            }

            SectionHeader("Device Toggles")
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

            SectionHeader("System")
            AdbRow(
                title = "Clear logcat",
                subtitle = "Clear the device log buffer",
            ) {
                AdbActionButton(
                    text = "Clear",
                    enabled = isDeviceSelected,
                    onClick = { devicesViewModel.runCommand(selectedDeviceId, "logcat -c") },
                )
            }
            AdbRow(
                title = "Reboot device",
                subtitle = "Restart the device",
            ) {
                AdbActionButton(
                    text = "Reboot",
                    enabled = isDeviceSelected,
                    onClick = { devicesViewModel.runCommand(selectedDeviceId, "reboot") },
                )
            }
            AdbRow(
                title = "Restart ADB",
                subtitle = "Restart the ADB server",
            ) {
                AdbActionButton(
                    text = "Restart",
                    enabled = true,
                    onClick = { devicesViewModel.restartAdb() },
                )
            }

            if (!actionMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(actionMessage, color = CanvasSuccessGreen)
            }
            if (!actionError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(actionError, color = Color(0xFFC62828))
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
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AdbRow(
    title: String,
    subtitle: String?,
    trailingContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.widthIn(min = 220.dp)) {
            trailingContent()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
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
            color = if (enabled) Color.DarkGray else Color.LightGray,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
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
