package io.github.yashkasera.alohomora

open class AlohomoraExtension {
    var enabledVariants: Set<String> = setOf("debug")
    var maxCommits: Int = 10
    var versionName: String? = null
    var versionCode: Int = 1
    var slackWebhookUrl: String? = null
}
