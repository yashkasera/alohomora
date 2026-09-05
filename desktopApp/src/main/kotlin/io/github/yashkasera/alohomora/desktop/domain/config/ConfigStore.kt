package io.github.yashkasera.alohomora.desktop.domain.config

import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import kotlinx.serialization.KSerializer

/**
 * A versioned config artifact lives in one of two scopes.
 *
 * [LOCAL] is a zero-setup personal store on disk; [TEAM] is the git-backed shared repo. The split is
 * dual: every artifact is one or the other, and "Share with team" / "Copy to local" promote between.
 */
enum class ConfigScope { LOCAL, TEAM }

/**
 * The lifecycle state a user sees, git vocabulary hidden.
 *
 * [LOCAL] private on disk; [DRAFT] team-scoped edits not yet shared; [IN_REVIEW] a proposal is open;
 * [LIVE] merged to the main line.
 */
enum class ConfigState { LOCAL, DRAFT, IN_REVIEW, LIVE }

/**
 * A kind of stored artifact: its directory, how to (de)serialize it, and how to read its identity and
 * display name off a value. Carrying `idOf`/`nameOf` here keeps [LocalConfigStore] fully generic
 * without a marker interface bleeding store concerns into the pure domain models.
 */
sealed interface ConfigKind<T> {
    val dir: String
    val serializer: KSerializer<T>
    val idOf: (T) -> String
    val nameOf: (T) -> String

    object Journeys : ConfigKind<JourneyDefinition> {
        override val dir = "journeys"
        override val serializer = JourneyDefinition.serializer()
        override val idOf = JourneyDefinition::id
        override val nameOf = JourneyDefinition::name
    }
    // MockSets and DeepLinks adopt this in Phase 3.
}

/** A stored artifact plus the scope/state metadata the UI renders (never persisted in the file body). */
data class ConfigItem<T>(
    val value: T,
    val scope: ConfigScope,
    val state: ConfigState,
    /** Set for IN_REVIEW artifacts this app proposed. */
    val reviewUrl: String? = null,
)

/**
 * The neutral result of proposing a change to the team. Names no git or forge type on purpose so a
 * different VCS backend can satisfy [ConfigStore] without stubbing git-shaped methods.
 */
data class Proposal(
    val branch: String,
    val reviewUrl: String?,
    /** "merge request" | "pull request" | "change" — drives the UI label. */
    val reviewNoun: String,
    /** True when a forge adapter created the review; false when the user must open it manually. */
    val autoCreated: Boolean,
)

/** Outcome of a fetch-and-compare against the team's main line. */
sealed interface SyncStatus {
    data object NotConnected : SyncStatus
    data class UpToDate(val checkedAt: Long) : SyncStatus
    data class Drift(val behind: Int, val ahead: Int) : SyncStatus
}

/**
 * The one persistence seam. LOCAL is always available with zero setup; TEAM requires a connected
 * config repository and is added in Phase 2. The interface deliberately names no git/forge type.
 */
interface ConfigStore {
    suspend fun <T> list(kind: ConfigKind<T>, scope: ConfigScope): List<ConfigItem<T>>

    /** Debounced working-tree write / personal store write. Never a commit. */
    suspend fun <T> saveLocal(kind: ConfigKind<T>, item: T)

    suspend fun <T> deleteLocal(kind: ConfigKind<T>, id: String)

    /** Packages the artifact into a branch + proposal. Requires TEAM to be connected (Phase 2). */
    suspend fun <T> shareWithTeam(kind: ConfigKind<T>, item: T): Proposal

    /** Fetches the team main line and reports drift. */
    suspend fun sync(): SyncStatus
}
