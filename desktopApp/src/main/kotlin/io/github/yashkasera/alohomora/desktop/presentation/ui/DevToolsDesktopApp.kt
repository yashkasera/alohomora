package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PrefsViewModel
import io.github.yashkasera.alohomora.presentation.ui.components.icons.AlertTriangle
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import io.github.yashkasera.alohomora.ui.theme.CanvasSuccessGreen

@Composable
fun DevToolsDesktopApp(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    databaseViewModel: DatabaseViewModel,
    prefsViewModel: PrefsViewModel,
) {
    var activeSection by remember { mutableStateOf(DesktopSection.Dashboard) }

    AlohomoraTheme(onThemeChanged = {}) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar
                Sidebar(
                    activeSection = activeSection,
                    onSectionClick = { activeSection = it }
                )

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 24.dp)
                ) {
                    Header()
                    Spacer(modifier = Modifier.height(32.dp))
                    DashboardContent()
                    Spacer(modifier = Modifier.weight(1f))
                    Footer()
                }
            }
        }
    }
}

@Composable
fun Sidebar(
    activeSection: DesktopSection,
    onSectionClick: (DesktopSection) -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color(0xFFF8F9FD))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A56DB))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Console.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "PROJECT",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(start = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Apollo Mobile", fontWeight = FontWeight.Medium)
            Icon(Icons.AlertTriangle, contentDescription = null, size = 16.dp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        DesktopSection.entries.forEach { section ->
            SidebarItem(
                section = section,
                isSelected = activeSection == section,
                onClick = { onSectionClick(section) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Text("DA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("DevAdmin", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Pro License", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SidebarItem(
    section: DesktopSection,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFEDF2FF) else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF1A56DB) else Color.DarkGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            section.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            section.title,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text("OVERVIEW", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("System Status", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {},
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AlertTriangle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {},
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AlertTriangle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Build")
            }
        }
    }
}

@Composable
fun DashboardContent() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DeviceInfoCard(modifier = Modifier.weight(2f))
            CurrentBuildCard(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatCard(title = "Network Traffic", value = "2.4", unit = "MB/s", subtitle = "LAST 5 MINUTES", modifier = Modifier.weight(1f))
            StatCard(title = "CPU Usage", value = "42", unit = "%", subtitle = "CORE 0-3", modifier = Modifier.weight(1f))
            FrameRateCard(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            RecentEventsCard(modifier = Modifier.weight(2f))
            QuickActionsCard(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DeviceInfoCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CanvasSuccessGreen))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CONNECTED VIA ADB WI-FI", style = MaterialTheme.typography.labelSmall, color = CanvasSuccessGreen, fontWeight = FontWeight.Bold)
            }
            Text("Pixel 7 Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Android 13 (Tiramisu) • API Level 33", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("BATTERY", "84%", "Charging")
                InfoItem("MEMORY", "4.2", "/ 12 GB")
                InfoItem("LATENCY", "12ms", null, valueColor = Color(0xFF1A56DB))
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, subValue: String?, valueColor: Color = Color.Black) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
            if (subValue != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(subValue, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
fun CurrentBuildCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Current Build", fontWeight = FontWeight.Bold)
                Surface(color = Color(0xFFDEF7EC), shape = RoundedCornerShape(4.dp)) {
                    Text("PASSING", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF03543F), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("v2.4.0", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("beta", color = Color.LightGray, style = MaterialTheme.typography.titleMedium)
            }
            Text("Branch: feature/payment-gateway", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.weight(1f))

            LinearProgressIndicator(progress = { 0.7f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = Color(0xFF1A56DB), trackColor = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pipeline #4024", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("3m 12s duration", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, unit: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            // Add a simple sparkline/graph placeholder here
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0xFFF9FAFB)))
        }
    }
}

@Composable
fun FrameRateCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Frame Rate", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("60", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CanvasSuccessGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FPS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            Text("RENDER TIME", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f).background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Text("Jank Frames", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("0", fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f).background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Text("Frame Time", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("16.6ms", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecentEventsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("VIEW ALL LOGS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A56DB), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            val events = listOf(
                EventItemData("Screen_View: HomeFragment", "User navigated to home dashboard", "10:42:05.122", CanvasSuccessGreen),
                EventItemData("API_Warn: Slow Response", "GET /api/v1/user/profile took 1200ms", "10:41:58.890", Color(0xFFF59E0B)),
                EventItemData("Auth_Success", "User DevAdmin authenticated via Biometric", "10:40:12.004", Color(0xFF3B82F6))
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(events) { event ->
                    EventRow(event)
                    if (event != events.last()) HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp, color = Color(0xFFF3F4F6))
                }
            }
        }
    }
}

data class EventItemData(val title: String, val subtitle: String, val time: String, val dotColor: Color)

@Composable
fun EventRow(event: EventItemData) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(event.dotColor))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(event.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(event.time, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
    }
}

@Composable
fun QuickActionsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            QuickActionButton("Take Screenshot", "Cmd+S")
            QuickActionButton("Record Screen", "Cmd+R")
            QuickActionButton("Clear App Data", null)
            QuickActionButton("Restart ADB", null)
        }
    }
}

@Composable
fun QuickActionButton(text: String, shortcut: String?) {
    OutlinedButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (shortcut != null) {
                Text(shortcut, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun Footer() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("© 2023 Android Unified Console.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Documentation", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Support", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Privacy", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color = Color.Black) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = tint
    )
}
