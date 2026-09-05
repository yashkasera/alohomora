package io.github.yashkasera.alohomora.common.journey

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A declared flow to grade a captured session against ("did checkout fire login then add-to-cart then
 * pay?"). This is a versioned config artifact: [id] is the immutable identity, the filename is derived
 * from [name] at creation, and [schemaVersion] lets a newer desktop read an older file.
 *
 * Matching is pure and lives in [JourneyMatcher]; nothing here touches a platform.
 */
@Serializable
data class JourneyDefinition(
    val id: String,
    /** Per-file version so a v2 desktop still reads a v1 file. Bump only on a breaking shape change. */
    val schemaVersion: Int = 1,
    val name: String,
    /** Surfaced in the list row subtitle and the proposal body; never required. */
    val description: String = "",
    /** `false` (the non-tech default) = presence check; `true` = ordered cursor over the stream. */
    val ordered: Boolean = false,
    /** Ignore events outside the expected set. When `false`, an unexpected event fails the journey. */
    val allowUnexpected: Boolean = true,
    val steps: List<JourneyStep>,
)

/**
 * One expected event in a journey.
 *
 * The [selector]/[assertions] split is the identity fix for same-name events: the property that
 * decides *which* occurrence goes in [selector]; the property you *validate* goes in [assertions]. An
 * empty [selector] is a name-only presence match.
 */
@Serializable
data class JourneyStep(
    val id: String,
    /** Matches [io.github.yashkasera.alohomora.common.Event.name]. */
    val eventName: String,
    /** Which occurrence to match; each entry is a property path -> required value. Empty = name-only. */
    val selector: Map<String, JsonElement> = emptyMap(),
    /** Advanced checks run on a matched event; any failure is a hard fail. */
    val assertions: List<Assertion> = emptyList(),
    /** What to do when a consumed step's event is seen again. */
    val onRepeat: RepeatPolicy = RepeatPolicy.IGNORE,
)

/** A single check against a matched event's property at [path]. [value] is unused for [AssertOp.EXISTS]. */
@Serializable
data class Assertion(
    val path: String,
    val op: AssertOp,
    val value: JsonElement? = null,
)

enum class AssertOp { EQ, NEQ, EXISTS, LT, GT, CONTAINS }

enum class RepeatPolicy { FAIL, FLAG, IGNORE }
