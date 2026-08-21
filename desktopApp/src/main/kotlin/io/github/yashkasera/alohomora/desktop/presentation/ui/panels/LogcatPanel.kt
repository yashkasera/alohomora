package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatControls
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatFilters
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatList
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun LogcatPanel(
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    selectedDeviceId: String?,
    buildInfo: BuildInfo? = null,
    modifier: Modifier = Modifier,
    searchFocusTrigger: Long = 0L,
) {
    val error by devicesViewModel.error.collectAsState()
    val uiState by logcatViewModel.uiState.collectAsState()

    LaunchedEffect(selectedDeviceId) {
        logcatViewModel.setSelectedDevice(selectedDeviceId)
        if (selectedDeviceId != null && !uiState.running) {
            logcatViewModel.start()
        }
    }

    LaunchedEffect(buildInfo?.packageName) {
        val pkg = buildInfo?.packageName
        if (!pkg.isNullOrBlank() && uiState.filterState.packageName.isBlank()) {
            logcatViewModel.updatePackageName(pkg)
        }
    }

    val subtitle = run {
        val total = uiState.entries.size
        val filtered = uiState.filteredEntries.size
        if (total == 0) "Live device logs with filters"
        else if (filtered == total) "$total entries"
        else "Showing $filtered of $total"
    }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Logcat",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = subtitle,
                actions = {
                    LogcatControls(
                        running = uiState.running,
                        onStart = { logcatViewModel.start() },
                        onStop = { logcatViewModel.stop() },
                    ) {
                        logcatViewModel.clear()
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize().padding(MaterialTheme.dimens.margin.xl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            val errorMessages = listOfNotNull(
                error?.takeIf { it.isNotBlank() },
                uiState.errorMessage?.takeIf { it.isNotBlank() },
            )
            AnimatedVisibility(
                visible = errorMessages.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.dimens.margin.md),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.CircleAlert,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                        Column {
                            errorMessages.forEach { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            LogcatFilters(
                filterState = uiState.filterState,
                availableTags = uiState.availableTags,
                onToggleLevel = logcatViewModel::toggleLevel,
                onTagFilterChange = logcatViewModel::updateTagFilter,
                onPackageChange = logcatViewModel::updatePackageName,
                onSearch = logcatViewModel::updateSearchQuery,
                onToggleRegex = logcatViewModel::toggleRegex,
                searchFocusTrigger = searchFocusTrigger,
            )

            LogcatList(
                entries = uiState.filteredEntries,
                modifier = Modifier.weight(1f),
                onTagClick = logcatViewModel::updateTagFilter,
            )
        }
    }
}
