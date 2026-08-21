package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.clampLines
import io.github.yashkasera.alohomora.common.isCrashEvent
import io.github.yashkasera.alohomora.common.prettyProperties
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.rememberViewedStateColors
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

private const val MAX_ROW_PROPERTY_LINES = 6

@Composable
fun LazyItemScope.EventItem(
    event: Event,
    showProperties: Boolean,
    onClick: () -> Unit,
) {
    val isFatal = event.isCrashEvent
    val viewedColors = rememberViewedStateColors(
        isViewed = event.isViewed,
        unviewedTitleColor = if (isFatal) MaterialTheme.alohomoraColors.fatal
        else MaterialTheme.colorScheme.onSurface,
        viewedTitleColor = if (isFatal) MaterialTheme.alohomoraColors.fatal
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    AlohomoraCard(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = viewedColors.containerColor.value,
        ),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (isFatal) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(MaterialTheme.dimens.stroke.medium)
                        .background(MaterialTheme.alohomoraColors.fatal),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.lg,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = viewedColors.titleColor.value,
                    )

                    Text(
                        text = DateUtils.format(event.time, DateUtils.Format.HH_MM_SS),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (showProperties) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

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
}
