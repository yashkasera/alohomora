package io.github.yashkasera.alohomora

open class AlohomoraExtension {
    /**
     * Explicit allowlist of full variant names (e.g. `"freeDebug"`). Matched verbatim against
     * `variant.name`. Left empty by default: with product flavors a variant is named
     * `<flavor><BuildType>`, so a plain `"debug"` here matches nothing. Prefer [enabledBuildTypes]
     * /[enabledFlavors] and reach for this only to pin one exact variant.
     */
    var enabledVariants: Set<String> = emptySet()

    /**
     * Build types Alohomora is generated for, regardless of flavor. Defaults to `debug`
     */
    var enabledBuildTypes: Set<String> = setOf("debug")

    /**
     * Flavors to narrow [enabledBuildTypes] to. Empty means every flavor (the common case). Set it
     * to, say, `setOf("free")` to generate Alohomora only for `free*` variants of the enabled build
     * types.
     */
    var enabledFlavors: Set<String> = emptySet()

    var appName: String? = null
    var maxCommits: Int = 10
    var versionName: String? = null
    var versionCode: Int = 1
    var slackWebhookUrl: String? = null
    var devIconBase: String? = "ic_launcher"
    var devIconBgRef: String? = null
    var devIconLabel: String? = null
    var devIconVariantBarPosition: DevIconVariantBarPosition = DevIconVariantBarPosition.TOP_RIGHT
}
