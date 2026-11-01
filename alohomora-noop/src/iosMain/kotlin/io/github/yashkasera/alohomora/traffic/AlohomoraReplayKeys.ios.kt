package io.github.yashkasera.alohomora.traffic

/**
 * No-op mirror of `:alohomora`'s `AlohomoraReplayKeys`.
 *
 * Nothing reads these in a release build, but a host app's Swift replay handler still names them and
 * has to compile and link against the release framework. The header value is spelled out rather than
 * taken from `ReplayMarker`, because `:alohomora-noop` deliberately does not depend on
 * `:alohomora-common`. **Keep both values in lockstep with the debug declaration.**
 */
@Suppress("unused")
object AlohomoraReplayKeys {
    const val PROPERTY_KEY: String = "AlohomoraReplayOf"

    const val HEADER: String = "X-Alohomora-Replay-Of"
}
