package io.github.yashkasera.alohomora.capture

import io.github.yashkasera.alohomora.InternalAlohomoraApi
import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlin.concurrent.Volatile
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/**
 * Where capture hands a finished traffic entry. `alohomora` installs its persistence here at init.
 *
 * Exists only because the interceptors moved to core while the repositories have not yet; once
 * persistence lives in core this collapses back into a direct call.
 */
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
@InternalAlohomoraApi
object CaptureSink {

    @Volatile
    var trafficSink: ((TrafficEntry) -> Unit)? = null

    fun persistTrafficEntry(entry: TrafficEntry) {
        trafficSink?.invoke(entry)
    }
}
