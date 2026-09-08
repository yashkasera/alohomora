package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.presentation.model.CatalogModuleGroup
import io.github.yashkasera.alohomora.desktop.presentation.model.DeepLinkCatalogUiState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheetHeader
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFloatingActionButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraSingleChoiceToggleGroup
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraToggleItem
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Link
import io.github.yashkasera.alohomora.ui.icons.Plus
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.icons.Share
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Master catalog of typed deep links (~50% width). Grouped into collapsible module sections by
 * default; a module jump-chip narrows to one module and a non-empty search flattens into a
 * cross-module ranked list ([DeepLinkCatalogUiState.visibleDefs]). Stateless over the state.
 */
@Composable
fun DeepLinkCatalogSideSheet(
    visible: Boolean,
    state: DeepLinkCatalogUiState,
    teamConnected: Boolean,
    onScopeChange: (ConfigScope) -> Unit,
    onQueryChange: (String) -> Unit,
    onModuleFilterChange: (String?) -> Unit,
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
            AlohomoraSideSheetHeader(
                title = "Deep link catalog",
                onClose = onDismiss,
                actions = {
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
                },
            )
        },
        floatingActionButton = {
            AlohomoraFloatingActionButton(onClick = onNew) {
                Icon(
                    imageVector = Icons.Plus,
                    contentDescription = "New deep link",
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

        if (state.moduleNames.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.xs,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AlohomoraFilterChip(
                    label = "All",
                    selected = state.moduleFilter == null,
                    onClick = { onModuleFilterChange(null) },
                )
                state.moduleNames.forEach { module ->
                    val count = state.moduleCounts[module] ?: 0
                    AlohomoraFilterChip(
                        label = "$module  $count",
                        selected = state.moduleFilter == module,
                        onClick = {
                            onModuleFilterChange(if (state.moduleFilter == module) null else module)
                        },
                    )
                }
            }
        }

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

        val defs = state.visibleDefs
        if (defs.isEmpty()) {
            EmptyState(
                icon = Icons.Link,
                title = if (state.query.isBlank()) "No deep links yet" else "No matches",
                subtitle = if (state.query.isBlank()) {
                    "Add one to start building the catalog."
                } else {
                    "Nothing matches \"${state.query}\"."
                },
                action = if (state.query.isBlank() && state.moduleFilter == null) {
                    { AlohomoraTextButton(text = "New deep link", onClick = onNew) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.margin.xxl),
            )
            return@AlohomoraSideSheet
        }

        // Grouped, collapsible modules when browsing; a flat ranked/scoped list when narrowed.
        val grouped = state.query.isBlank() && state.moduleFilter == null
        if (grouped) {
            val expanded = remember { mutableStateMapOf<String, Boolean>() }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.dimens.margin.xxl,
                    vertical = MaterialTheme.dimens.margin.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                items(state.moduleGroups, key = { it.module }) { group ->
                    ModuleSection(
                        group = group,
                        expanded = expanded[group.module] ?: true,
                        onToggle = { expanded[group.module] = !(expanded[group.module] ?: true) },
                        showShare = state.scope == ConfigScope.LOCAL,
                        onOpen = onOpen,
                        onShare = onShare,
                        onDelete = onDelete,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.dimens.margin.xxl,
                    vertical = MaterialTheme.dimens.margin.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                itemsIndexed(defs, key = { _, item -> item.value.id }) { _, item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DefRow(
                            def = item.value,
                            breadcrumb = true,
                            showShare = state.scope == ConfigScope.LOCAL,
                            onOpen = onOpen,
                            onShare = onShare,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }
}

/** One collapsible module: a tonal grouped container with a header, count, and flow-labelled rows. */
@Composable
private fun ModuleSection(
    group: CatalogModuleGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    showShare: Boolean,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val chevronRotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.lg,
                        vertical = MaterialTheme.dimens.margin.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                Icon(
                    imageVector = Icons.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.md).rotate(chevronRotation),
                )
                Text(
                    text = group.module.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                AlohomoraChip(label = group.defs.size.toString())
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(EXPAND_MS, easing = Emphasized)) + fadeIn(tween(EXPAND_MS)),
                exit = shrinkVertically(tween(COLLAPSE_MS, easing = Emphasized)) + fadeOut(tween(COLLAPSE_MS)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Defs arrive flow-sorted, so grouping keys stay contiguous.
                    group.defs.groupBy { it.value.flow }.forEach { (flow, items) ->
                        flow?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = MaterialTheme.dimens.margin.lg,
                                    top = MaterialTheme.dimens.margin.xs,
                                    bottom = MaterialTheme.dimens.margin.xs,
                                ),
                            )
                        }
                        items.forEach { item ->
                            DefRow(
                                def = item.value,
                                breadcrumb = false,
                                showShare = showShare,
                                onOpen = onOpen,
                                onShare = onShare,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One deep-link row: leading link glyph, name + meta, trailing share/delete. */
@Composable
private fun DefRow(
    def: DeepLinkDef,
    breadcrumb: Boolean,
    showShare: Boolean,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val paramCount = def.params.size
    val meta = when {
        breadcrumb -> listOfNotNull(def.module, def.flow).joinToString("/")
        paramCount > 0 -> "$paramCount param${if (paramCount == 1) "" else "s"}"
        else -> def.uriTemplate
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onOpen(def.id) })
            .padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = CircleShape,
            modifier = Modifier.size(MaterialTheme.dimens.margin.xxl),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = def.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showShare) {
            AlohomoraIconButton(onClick = { onShare(def.id) }) {
                Icon(
                    imageVector = Icons.Share,
                    contentDescription = "Share with team",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                )
            }
        }
        AlohomoraIconButton(onClick = { onDelete(def.id) }) {
            Icon(
                imageVector = Icons.Trash,
                contentDescription = "Delete",
                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
            )
        }
    }
}

private val SCOPE_ITEMS = listOf(
    AlohomoraToggleItem(id = ConfigScope.LOCAL.name, label = "Local"),
    AlohomoraToggleItem(id = ConfigScope.TEAM.name, label = "Team"),
)

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val EXPAND_MS = 300
private const val COLLAPSE_MS = 200
