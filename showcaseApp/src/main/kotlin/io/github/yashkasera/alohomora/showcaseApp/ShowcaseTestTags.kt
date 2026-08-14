package io.github.yashkasera.alohomora.showcaseApp

/**
 * Test tags for the showcase app's own UI.
 *
 * Separate from `AlohomoraTestTags`, which addresses the console. This app builds against
 * `alohomora-noop` in release, and the no-op module deliberately does not depend on
 * `alohomora-ui` — so reaching for the console's tag object here would break the release variant
 * for the same reason the FAB uses a text label instead of an `ImageVector`.
 *
 * Lives in `src/main` rather than `src/androidTest` so the composables can reference it.
 */
object ShowcaseTestTags {
    const val POSTS_LIST = "showcase_posts_list"
    const val REFRESH = "showcase_refresh"
    const val ERROR_MESSAGE = "showcase_error_message"

    const val CRASH = "showcase_crash"
    const val RECORD_ERROR = "showcase_record_error"

    const val USERNAME = "showcase_username"
    const val AUTO_REFRESH = "showcase_auto_refresh"
    const val LAST_REFRESH = "showcase_last_refresh"

    const val OPEN_WEBVIEW = "showcase_open_webview"

    fun post(id: Long) = "showcase_post_$id"
}
