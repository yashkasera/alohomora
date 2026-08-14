package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsTimeWindow
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsUiState
import io.github.yashkasera.alohomora.ui.components.AlohomoraAssistChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraSingleChoiceToggleGroup
import io.github.yashkasera.alohomora.ui.components.AlohomoraToggleItem
import io.github.yashkasera.alohomora.ui.icons.Clock
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * The Events panel's filter controls.
 *
 * State in, callbacks out — the shape [io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatFilters]
 * established, so everything derived lives in `EventsViewModel` and this file stays a rendering of it.
 */
@Composable
fun EventsFilters(
    state: EventsUiState,
    onQueryChange: (String) -> Unit,
    onUnreadOnlyChange: (Boolean) -> Unit,
    onWindowChange: (EventsTimeWindow) -> Unit,
    onMark: () -> Unit,
    onClearMark: () -> Unit,
    onToggleMute: (String) -> Unit,
    onUnmuteAll: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
    searchFocusTrigger: Long = 0L,
) {
    val filters = state.filters
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) searchFocus.requestFocus()
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimens.margin.xxl,
                    vertical = MaterialTheme.dimens.margin.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlohomoraSearchTextField(
                query = filters.query,
                onQueryChange = onQueryChange,
                placeholder = "Filter by event name or property",
                onClear = { onQueryChange("") },
                modifier = Modifier.weight(1f).focusRequester(searchFocus),
            )

            AlohomoraFilterChip(
                label = "Unread",
                selected = filters.unreadOnly,
                onClick = { onUnreadOnlyChange(!filters.unreadOnly) },
            )

            // A toggle group rather than five more chips: exactly one window is always in force, and
            // `selectableGroup` plus Role.RadioButton says so to the accessibility tree for free.
            // Not uppercased — "30s" would render as "30S".
            AlohomoraSingleChoiceToggleGroup(
                items = EventsTimeWindow.entries.map { AlohomoraToggleItem(it.name, it.label) },
                selectedId = filters.window.name,
                onSelectedIdChange = { id -> onWindowChange(EventsTimeWindow.valueOf(id)) },
                uppercase = false,
            )

            // Labelled "Hide older" rather than "Mark" because there is no tooltip anywhere in this app
            // to explain a bare verb, and this control sits one row below a Trash that really does
            // delete on the device. The two must not read alike. Backed by `markFloorMillis`.
            if (filters.markFloorMillis == null) {
                AlohomoraOutlinedButton(
                    text = "Hide older",
                    onClick = onMark,
                    size = AlohomoraButtonSize.SMALL,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Clock,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                        )
                    },
                )
            } else {
                AlohomoraFilterChip(
                    label = "Since ${
                        DateUtils.format(
                            filters.markFloorMillis,
                            DateUtils.Format.HH_MM_SS,
                        )
                    }",
                    selected = true,
                    uppercase = false,
                    onClick = onClearMark,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.X,
                            contentDescription = "Show older events again",
                            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                        )
                    },
                )
            }

            // Only when something transient is narrowing the list. Mutes have their own control below,
            // because clearing them by accident would undo a deliberate, persisted choice.
            if (filters.hasTransientFilter) {
                AlohomoraFilterChip(
                    label = "Clear filters",
                    selected = false,
                    onClick = onClearFilters,
                )
            }
        }

        if (state.names.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.dimens.margin.xxl,
                        end = MaterialTheme.dimens.margin.xxl,
                        bottom = MaterialTheme.dimens.margin.sm,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(state.names, key = { it }) { name ->
                    // A muted chip stays present and de-emphasised: it is the only way back, so deriving
                    // this row from the *filtered* list would delete the control the user needs. The
                    // count is pre-mute for the same reason — see EventsUiState.nameCounts.
                    AlohomoraFilterChip(
                        label = "$name · ${state.nameCounts[name] ?: 0}",
                        selected = name !in filters.mutedNames,
                        uppercase = false,
                        onClick = { onToggleMute(name) },
                    )
                }

                if (filters.mutedNames.isNotEmpty()) {
                    item {
                        AlohomoraAssistChip(
                            label = "${filters.mutedNames.size} muted · unmute all",
                            uppercase = false,
                            onClick = onUnmuteAll,
                        )
                    }
                }
            }
        }
    }
}
