package io.github.yashkasera.alohomora.replay

/**
 * Constructors for Swift, which cannot reach [ReplayOutcome.Sent] and [ReplayOutcome.Failed].
 *
 * Kotlin/Native only exports declarations reachable from an exported signature. The one signature
 * that mentions an outcome — [TrafficReplayHandler.replay] — is typed as the interface, so the two
 * implementations are never reached and do not appear in the framework header at all.
 * [ReplayOutcome] lands in Swift as an empty protocol with no conformers, which makes
 * [TrafficReplayHandler] impossible to implement from Swift: a handler reads the request and then
 * has nothing it can construct to return.
 *
 * ```swift
 * final class URLSessionReplayHandler: Alohomora_commonTrafficReplayHandler {
 *     func replay(request: Alohomora_commonReplayRequest,
 *                 completionHandler: @escaping (Alohomora_commonReplayOutcome?, Error?) -> Void) {
 *         // ... send through the app's own session ...
 *         completionHandler(ReplayOutcomes.shared.sent(traceId: nil), nil)
 *     }
 * }
 * ```
 *
 * Two things this is deliberately *not*, both tried and reverted:
 *
 * - **Not a `companion object` on [ReplayOutcome].** An ObjC protocol has no class-side members, so
 *   a companion on an interface is dropped from the header without warning. It exported nothing.
 * - **Not declared in `:alohomora-common`.** The framework is built from `:alohomora` and lists no
 *   `export(...)`, so a common-module declaration is only pulled in when some `:alohomora` signature
 *   references it. Adding `export(projects.alohomoraCommon)` would work but renames every
 *   `Alohomora_common*` type in the header, breaking existing Swift call sites.
 *
 * Swift only ever produces outcomes — Alohomora is the sole consumer — so opaque values suffice and
 * neither concrete type needs exporting. Kotlin callers should keep using the constructors directly.
 * Do not delete this for lack of callers in this repo: the only caller is a host app's Swift, which
 * nothing here compiles.
 */
object ReplayOutcomes {

    /** @see ReplayOutcome.Sent */
    fun sent(traceId: String? = null): ReplayOutcome = ReplayOutcome.Sent(traceId)

    /** @see ReplayOutcome.Failed */
    fun failed(reason: String): ReplayOutcome = ReplayOutcome.Failed(reason)
}
