package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.github.yashkasera.alohomora.desktop.app.isShortcutModifier
import io.github.yashkasera.alohomora.desktop.app.displayModifier
import io.github.yashkasera.alohomora.desktop.presentation.ui.DesktopSection
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.launch

private val SECTION_DESCRIPTIONS = mapOf(
    DesktopSection.Dashboard to "Device metrics: battery level, memory usage, CPU load, jank frames, screenshots, and screen recording.",
    DesktopSection.Logcat to "Live Android log stream with filtering by tag, package, log level, and free-text search.",
    DesktopSection.Adb to "Run ADB commands, install or uninstall APKs, open deep links, and toggle Wi-Fi or mobile data.",
    DesktopSection.Traffic to "Live HTTP traffic capture with full request/response inspection, cURL export, Slack sharing, and replay.",
    DesktopSection.Events to "Custom analytics events streamed from the connected app in real time.",
    DesktopSection.Database to "Browse SQLite databases and tables on the connected device.",
    DesktopSection.Cache to "Inspect SharedPreferences and UserDefaults cache keys and values.",
    DesktopSection.GitHistory to "Git commit history and build metadata embedded from the app's source repository.",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HelpDialog(
    visibleSections: List<DesktopSection>,
    actions: List<CommandAction>,
    isDark: Boolean,
    themeId: String = "default",
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        AppTheme(initialIsDark = isDark, themeId = themeId) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Surface(
                modifier = Modifier
                    .fillMaxHeight(0.7f)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when {
                            event.key == Key.Escape -> { onDismiss(); true }
                            event.key == Key.W && event.isShortcutModifier() -> { onDismiss(); true }
                            else -> false
                        }
                    },
            ) {
                val tabs = listOf("Shortcuts", "Features")
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val scope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize()) {
                    AlohomoraPrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            AlohomoraTab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = tab,
                                uppercase = false,
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> ShortcutsTab(actions = actions)
                            1 -> FeaturesTab(visibleSections = visibleSections)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutsTab(actions: List<CommandAction>) {
    val mod = displayModifier()
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
    ) {
        val shortcutActions = actions.filter { it.shortcutDisplay != null }
        var lastCategory: ActionCategory? = null

        shortcutActions.forEach { action ->
            if (action.category != lastCategory) {
                if (lastCategory != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
                }
                lastCategory = action.category
                Text(
                    text = action.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.xs),
                )
            }
            ShortcutRow(label = action.label, shortcut = action.shortcutDisplay!!)
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
        Text(
            text = "Data",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.xs),
        )
        ShortcutRow(label = "Clear active panel", shortcut = "$mod+Shift+Del")
        ShortcutRow(label = "Close drawer / dialog", shortcut = "Esc")
        ShortcutRow(label = "Command Palette", shortcut = "$mod+K")
    }
}

@Composable
private fun ShortcutRow(label: String, shortcut: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.margin.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        ShortcutChip(shortcut)
    }
}

@Composable
private fun FeaturesTab(visibleSections: List<DesktopSection>) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        visibleSections.forEach { section ->
            val description = SECTION_DESCRIPTIONS[section] ?: return@forEach
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.margin.sm),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (section != visibleSections.last()) {
                AlohomoraHorizontalDivider()
            }
        }
    }
}
