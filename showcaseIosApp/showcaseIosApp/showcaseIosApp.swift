import SwiftUI
import AlohomoraKit

@main
struct ShowcaseApp: App {

    init() {
        Alohomora.shared.doInit()
        _ = Alohomora.shared.registerURLProtocol()

        Alohomora.shared.recordFeatureFlag(key: "dark_mode_v2", value: "true", source: "Firebase Remote Config", type: "feature_flag", metadata: nil)
        Alohomora.shared.recordFeatureFlag(key: "checkout_redesign", value: "false", source: "Firebase Remote Config", type: "experiment", metadata: nil)
        Alohomora.shared.recordFeatureFlag(key: "max_cart_items", value: "25", source: "LaunchDarkly", type: "remote_config", metadata: nil)
        Alohomora.shared.recordFeatureFlag(
            key: "onboarding_flow",
            value: "variant_b",
            source: "LaunchDarkly",
            type: "experiment",
            metadata: ["cohort": "new_users", "rollout_pct": "50"]
        )
        Alohomora.shared.recordFeatureFlag(key: "enable_search_v3", value: "true", source: nil, type: "feature_flag", metadata: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .task {
                    _ = Alohomora.shared.startDevToolsServer(port: 53999)
                }
        }
    }
}
