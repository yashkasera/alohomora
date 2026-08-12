package io.github.yashkasera.alohomora

open class AlohomoraExtension {
    var enabledVariants: Set<String> = setOf("debug")
    var maxCommits: Int = 10
    var versionName: String? = null
    var versionCode: Int = 1
    var slackWebhookUrl: String? = null
    var devIconBase: String? = "ic_launcher"
    var devIconBgRef: String? = null
    var devIconAppName: String? = null
    var devIconLabel: String? = null
    var devIconVariantBarPosition: DevIconVariantBarPosition = DevIconVariantBarPosition.TOP_RIGHT
}
