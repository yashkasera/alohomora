package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.presentation.model.JourneyUiState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFloatingActionButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraSingleChoiceToggleGroup
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraToggleItem
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Plus
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.icons.Route
import io.github.yashkasera.alohomora.ui.icons.Share
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Master list of journeys (~50% width), mirroring `MockRulesSideSheet`. Scope toggle, search, rows
 * with a step-count chip, and a FAB to author a new one. Stateless: reads [JourneyUiState] and emits
 * intents so the view model owns all mutation.
 */
@Composable
fun JourneyListSideSheet(
    visible: Boolean,
    state: JourneyUiState,
    teamConnected: Boolean,
    onScopeChange: (ConfigScope) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onDelete: (String) -> Unit,
    onShare: (String) -> Unit,
    onSync: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlohomoraSideSheet(
        visible = visible,
        onDismiss = onDismiss,
        widthFraction = 0.5f,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.lg,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Text(
                    text = "Event journeys",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AlohomoraSingleChoiceToggleGroup(
                    items = SCOPE_ITEMS,
                    selectedId = state.scope.name,
                    onSelectedIdChange = { onScopeChange(ConfigScope.valueOf(it)) },
                )
                if (teamConnected) {
                    AlohomoraIconButton(onClick = onSync) {
                        Icon(
                            imageVector = Icons.RefreshCw,
                            contentDescription = "Sync",
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                    }
                }
                AlohomoraIconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.X,
                        contentDescription = "Close",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                }
            }
        },
        floatingActionButton = {
            AlohomoraFloatingActionButton(onClick = onNew) {
                Icon(
                    imageVector = Icons.Plus,
                    contentDescription = "New journey",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                )
            }
        },
    ) {
        AlohomoraSearchTextField(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimens.margin.xxl,
                    vertical = MaterialTheme.dimens.margin.md,
                ),
        )

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.xxl),
            )
        }
        state.lastProposal?.let { proposal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimens.margin.xxl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AlohomoraChip(label = "In review")
                Text(
                    text = "Shared as a ${proposal.reviewNoun}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                proposal.reviewUrl?.let { url ->
                    AlohomoraTextButton(text = "Open ${proposal.reviewNoun}", onClick = { onOpenUrl(url) })
                }
            }
        }

        val journeys = state.visibleJourneys
        if (journeys.isEmpty()) {
            Text(
                text = if (state.query.isBlank()) {
                    "No journeys yet. Create one, or build it from a recording."
                } else {
                    "No journeys match \"${state.query}\"."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.dimens.margin.xxl,
                    vertical = MaterialTheme.dimens.margin.lg,
                ),
            )
            return@AlohomoraSideSheet
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            items(journeys, key = { it.value.id }) { item ->
                val journey = item.value
                AlohomoraCard(onClick = { onOpen(journey.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.dimens.margin.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    ) {
                        Icon(
                            imageVector = Icons.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = journey.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (journey.description.isNotBlank()) {
                                Text(
                                    text = journey.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        AlohomoraChip(label = "${journey.steps.size} steps")
                        if (state.scope == ConfigScope.LOCAL) {
                            AlohomoraIconButton(onClick = { onShare(journey.id) }) {
                                Icon(
                                    imageVector = Icons.Share,
                                    contentDescription = "Share with team",
                                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                                )
                            }
                        }
                        AlohomoraIconButton(onClick = { onDelete(journey.id) }) {
                            Icon(
                                imageVector = Icons.Trash,
                                contentDescription = "Delete",
                                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val SCOPE_ITEMS = listOf(
    AlohomoraToggleItem(id = ConfigScope.LOCAL.name, label = "Local"),
    AlohomoraToggleItem(id = ConfigScope.TEAM.name, label = "Team"),
)
