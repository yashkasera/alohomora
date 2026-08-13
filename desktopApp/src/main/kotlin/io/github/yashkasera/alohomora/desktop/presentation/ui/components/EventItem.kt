package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.prettyProperties
import io.github.yashkasera.alohomora.desktop.presentation.model.clampLines
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Rows show a payload's head, not all of it. Six lines is about one screenful of a chatty stream while
 * still showing a typical three-or-four-key event whole.
 */
private const val MAX_ROW_PROPERTY_LINES = 6

@Composable
fun LazyItemScope.EventItem(
    event: Event,
    showProperties: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = when {
        event.isViewed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    AlohomoraCard(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth(),
        colors = AlohomoraCardDefaults.colors(
            containerColor = containerColor,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Text(
                    text = DateUtils.format(event.time, DateUtils.Format.HH_MM_SS),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Code Block - only shown if showProperties is true
            if (showProperties) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                // Clamped rather than scrollable or height-capped. A verticalScroll inside a LazyColumn row
                // fights the list for the wheel, an unbounded row lets one 200-key payload grow taller than
                // the window, and a bare height cap truncates without saying so. The detail sheet is where
                // the whole payload is readable.
                //
                // prettyProperties() also fixes what `properties?.toString() ?: "{}"` got wrong here:
                // JsonNull is not Kotlin null, so an event recorded without properties rendered the word
                // "null".
                val properties = remember(event.id, event.time) {
                    event.prettyProperties().clampLines(MAX_ROW_PROPERTY_LINES)
                }
                AlohomoraCodeBlock(
                    content = properties,
                    isScrollable = false,
                )
            }
        }
    }
}
