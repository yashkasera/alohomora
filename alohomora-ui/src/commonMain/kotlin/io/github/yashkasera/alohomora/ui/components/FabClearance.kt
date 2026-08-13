package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Trailing scroll space so a floating action button (or any bottom-anchored overlay) cannot cover
 * the last row of a vertical list. Add it as the final entry of a [LazyColumn]'s content.
 */
fun LazyListScope.fabClearanceItem() {
    item(key = FAB_CLEARANCE_KEY) {
        Spacer(Modifier.height(MaterialTheme.dimens.margin.fab))
    }
}

/** Grid counterpart of [fabClearanceItem]; spans the full row so it always sits below the content. */
fun LazyGridScope.fabClearanceItem() {
    item(key = FAB_CLEARANCE_KEY, span = { GridItemSpan(maxLineSpan) }) {
        Spacer(Modifier.height(MaterialTheme.dimens.margin.fab))
    }
}

private const val FAB_CLEARANCE_KEY = "alohomora-fab-clearance"
