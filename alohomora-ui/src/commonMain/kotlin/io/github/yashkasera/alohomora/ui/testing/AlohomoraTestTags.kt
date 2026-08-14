package io.github.yashkasera.alohomora.ui.testing

/**
 * Stable `Modifier.testTag` identifiers for the Alohomora console.
 *
 * Public, and in `alohomora-ui` rather than `alohomora`, for two reasons. The console UI can only
 * be driven on a device — `runComposeUiTest` NPEs on the Android host reading `Build.FINGERPRINT` —
 * and the device tests live in two different modules (`:alohomora`'s own `androidDeviceTest` and
 * `:showcaseApp`'s `androidTest`), which do not share a source set. An `internal` vocabulary would
 * leave the second one hardcoding strings that rot silently when a tag is renamed. Consumers
 * embedding the console get the same benefit for free.
 *
 * `alohomora-ui` is not covered by the binary-compatibility validator (only `:alohomora` and
 * `:alohomora-noop` are), so this object costs no `.api` churn. Putting it in `:alohomora`'s
 * `commonMain` instead *would* change `alohomora.klib.api` — don't.
 *
 * Conventions:
 * - Every value is prefixed `alohomora_` so a tag can never collide with a host app's own.
 * - Row tags are keyed by the row's **domain id**, never its index. Index tags survive neither
 *   filtering nor the newest-first ordering the lists use.
 * - Tags address *interactive* nodes and *containers*. Text inside a row is not tagged: a row is a
 *   clickable `Card`, whose semantics subtree is merged, so `assertTextContains` on the row tag
 *   reads its contents without fighting the merge.
 */
object AlohomoraTestTags {

    /** Chrome shared by every screen. One tag serves all of them. */
    object Chrome {
        const val TOP_BAR_TITLE = "alohomora_top_bar_title"
        const val TOP_BAR_SUBTITLE = "alohomora_top_bar_subtitle"
        const val BACK = "alohomora_back"
        const val CLEAR_ALL = "alohomora_clear_all"
        const val SEARCH = "alohomora_search"
        const val EMPTY_STATE = "alohomora_empty_state"
        const val EMPTY_STATE_TITLE = "alohomora_empty_state_title"
        const val SCROLL_TO_TOP = "alohomora_scroll_to_top"
        const val CONFIRM_ACCEPT = "alohomora_confirm_accept"
        const val CONFIRM_DISMISS = "alohomora_confirm_dismiss"
    }

    object Overview {
        const val GRID = "alohomora_overview_grid"
        const val STATUS_CARD = "alohomora_overview_status_card"
        const val STATUS_PORT_FIELD = "alohomora_overview_status_port"
        const val STATUS_DOT = "alohomora_overview_status_dot"
        const val NEEDS_ATTENTION = "alohomora_overview_needs_attention"

        /**
         * [key] is the module's grid key: a built-in module's route simple name (`Traffic`,
         * `GitHistory`), or `Extension:<pluginId>` for a plugin card.
         *
         * Plugin cards are addressable per plugin id — pass `Extension:$pluginId`.
         */
        fun moduleCard(key: String) = "alohomora_overview_module_$key"
    }

    object Traffic {
        const val LIST = "alohomora_traffic_list"

        /** [id] is [io.github.yashkasera.alohomora.common.TrafficEntry.id]. */
        fun item(id: String) = "alohomora_traffic_item_$id"

        /** [method] is the uppercase HTTP method the chip filters on. */
        fun methodFilter(method: String) = "alohomora_traffic_filter_$method"
    }

    object TrafficDetails {
        const val ROOT = "alohomora_traffic_details"
        const val REPLAY = "alohomora_traffic_details_replay"
        const val SHARE = "alohomora_traffic_details_share"
        const val SHARE_SLACK = "alohomora_traffic_details_share_slack"
        const val REPLAY_RESULT_BANNER = "alohomora_traffic_details_replay_banner"
    }

    /** The editable request sheet the replay action opens. */
    object ReplaySheet {
        const val ROOT = "alohomora_replay_sheet"
        const val METHOD = "alohomora_replay_sheet_method"
        const val URL = "alohomora_replay_sheet_url"
        const val HEADERS = "alohomora_replay_sheet_headers"
        const val BODY = "alohomora_replay_sheet_body"
        const val SEND = "alohomora_replay_sheet_send"
        const val ERROR = "alohomora_replay_sheet_error"
    }

    object Traces {
        const val LIST = "alohomora_traces_list"
        const val ERROR_FILTER = "alohomora_traces_filter_errors"

        /** [traceId] is the shared id of every span in the trace. */
        fun item(traceId: String) = "alohomora_traces_item_$traceId"
    }

    object TraceDetails {
        const val ROOT = "alohomora_trace_details"
        const val HEADER = "alohomora_trace_details_header"
        const val VIEW_TOGGLE = "alohomora_trace_details_view_toggle"
        const val WATERFALL = "alohomora_trace_details_waterfall"
        const val SPAN_LIST = "alohomora_trace_details_span_list"
        const val SPAN_SHEET = "alohomora_trace_details_span_sheet"

        fun span(spanId: String) = "alohomora_trace_details_span_$spanId"
        fun spanCollapse(spanId: String) = "alohomora_trace_details_collapse_$spanId"
    }

    object Events {
        const val LIST = "alohomora_events_list"
        const val PROPERTIES_TOGGLE = "alohomora_events_properties_toggle"
        const val DETAILS_SHEET = "alohomora_events_details_sheet"

        fun item(id: Long) = "alohomora_events_item_$id"
    }

    object Errors {
        const val LIST = "alohomora_errors_list"

        fun item(id: Long) = "alohomora_errors_item_$id"
    }

    object ErrorDetails {
        const val ROOT = "alohomora_error_details"
        const val STACK_TRACE = "alohomora_error_details_stack_trace"

        /** Copies reason, place and stack trace to the clipboard. This screen has no share action. */
        const val COPY = "alohomora_error_details_copy"
    }

    object Cache {
        const val LIST = "alohomora_cache_list"
        const val FOOTER = "alohomora_cache_footer"

        fun item(key: String) = "alohomora_cache_item_$key"
    }

    object Database {
        const val SELECTOR = "alohomora_database_selector"
        const val SELECTOR_SHEET = "alohomora_database_selector_sheet"
        const val TABLES = "alohomora_database_tables"
        const val TABS = "alohomora_database_tabs"
        const val BROWSE = "alohomora_database_browse"
        const val QUERY_EDITOR = "alohomora_database_query_editor"
        const val QUERY_RUN = "alohomora_database_query_run"
        const val QUERY_RESULT = "alohomora_database_query_result"
        const val QUERY_STATUS = "alohomora_database_query_status"
        const val SCHEMA = "alohomora_database_schema"

        fun database(name: String) = "alohomora_database_option_$name"
        fun table(name: String) = "alohomora_database_table_$name"

        /** [title] is the tab label: `BROWSE`, `QUERY` or `SCHEMA`. */
        fun tab(title: String) = "alohomora_database_tab_$title"
    }

    object FeatureFlags {
        const val LIST = "alohomora_feature_flags_list"
        const val SOURCES = "alohomora_feature_flags_sources"
        const val FOOTER = "alohomora_feature_flags_footer"

        fun item(key: String) = "alohomora_feature_flags_item_$key"
        fun sourceFilter(source: String) = "alohomora_feature_flags_source_$source"
    }

    object Config {
        const val ROOT = "alohomora_config"
        const val BUILD_INFO = "alohomora_config_build_info"
        const val ENVIRONMENT = "alohomora_config_environment"

        /** [label] is the grid cell's label, e.g. `Branch`, `Build Variant`. */
        fun info(label: String) = "alohomora_config_info_$label"
    }

    object GitHistory {
        const val LIST = "alohomora_git_history_list"

        /** [sha] is the full commit sha. */
        fun commit(sha: String) = "alohomora_git_history_commit_$sha"
    }
}
