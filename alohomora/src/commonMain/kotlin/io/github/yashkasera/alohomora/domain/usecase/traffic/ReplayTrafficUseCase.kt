package io.github.yashkasera.alohomora.domain.usecase.traffic

import io.github.yashkasera.alohomora.replay.ReplayOutcome
import io.github.yashkasera.alohomora.replay.ReplayRequest
import io.github.yashkasera.alohomora.replay.TrafficReplayRegistry

/**
 * Re-sends a request through the handler the host app registered.
 *
 * Goes to [TrafficReplayRegistry] rather than a repository because there is nothing to persist here:
 * the app's own interceptor captures the replay as it happens, exactly as it captures any other
 * request, so writing a second record would double-count it.
 */
internal class ReplayTrafficUseCase {
    suspend operator fun invoke(request: ReplayRequest): ReplayOutcome =
        TrafficReplayRegistry.replay(request)
}
