package io.github.yashkasera.alohomora.ui.components.waterfall

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.ui.theme.spanClient
import io.github.yashkasera.alohomora.ui.theme.spanConsumer
import io.github.yashkasera.alohomora.ui.theme.spanInternal
import io.github.yashkasera.alohomora.ui.theme.spanProducer
import io.github.yashkasera.alohomora.ui.theme.spanServer

/** Alpha applied to a bar whose trace the user has already opened, mirroring how TrafficItem dims. */
private const val VIEWED_ALPHA = 0.55f

/**
 * Fill colour for a span's bar.
 *
 * **Status beats kind.** A failed span is red regardless of what kind it is, because "which span
 * failed" is the first question anyone opens a waterfall to answer and a hue that means two things is
 * no answer at all. Colour is never the *only* error signal — the row also carries an ERROR chip,
 * because this panel gets screenshotted into tickets and read by people who cannot hover it.
 *
 * [kind] is a `String` rather than an enum because `Span.kind` carries whatever vocabulary the source
 * tracer used — an OpenTelemetry `SpanKind` name, Sentry's `op`, anything. Unrecognised values fall
 * back to the internal colour, so a newer SDK's kind renders plainly instead of crashing the panel.
 */
@Composable
@ReadOnlyComposable
fun spanBarColor(kind: String, isError: Boolean, isViewed: Boolean = false): Color {
    val base = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        when (kind.uppercase()) {
            "SERVER" -> MaterialTheme.colorScheme.spanServer
            "CLIENT", "HTTP", "HTTP.CLIENT" -> MaterialTheme.colorScheme.spanClient
            "PRODUCER" -> MaterialTheme.colorScheme.spanProducer
            "CONSUMER" -> MaterialTheme.colorScheme.spanConsumer
            else -> MaterialTheme.colorScheme.spanInternal
        }
    }
    return if (isViewed) base.copy(alpha = VIEWED_ALPHA) else base
}

/**
 * Colour for the faint bar drawn behind a collapsed row, spanning its whole subtree.
 *
 * Collapsing hides structure, not time — otherwise collapsing a slow parent makes the slowness itself
 * vanish from the waterfall.
 */
@Composable
@ReadOnlyComposable
fun spanSubtreeColor(kind: String, isError: Boolean): Color =
    spanBarColor(kind, isError).copy(alpha = 0.22f)
