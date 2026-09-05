package io.github.yashkasera.alohomora.desktop.data.config

/**
 * A readable filename stem derived from an artifact's display name. Reviewers browse the changed-files
 * list in a proposal, so `journeys/checkout-happy-path.json` beats an opaque id.
 *
 * Derived **once at creation** and never changed on rename — identity is the immutable id, matched by
 * scanning, so a filename drifting from the display name is only cosmetic and avoids git rename churn.
 */
internal fun slug(name: String): String {
    val cleaned = name
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(60)
        .trim('-')
    if (cleaned.isEmpty()) return "config"
    // Guard Windows reserved device names, which cannot be a filename stem on that OS.
    val reserved = setOf(
        "con", "prn", "aux", "nul",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9",
    )
    return if (cleaned in reserved) "$cleaned-config" else cleaned
}
