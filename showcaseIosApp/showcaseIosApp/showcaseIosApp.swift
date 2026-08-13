import SwiftUI
import AlohomoraKit
import UserNotifications

// Lets Alohomora's "server active" notification show as a banner while the app is foregrounded.
// iOS suppresses foreground notifications unless a delegate opts in; the library requests
// authorization but deliberately does not claim this delegate (a push SDK often owns it), so the
// host wires it up.
final class NotificationForegroundPresenter: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }
}

@main
struct ShowcaseApp: App {

    private let notificationPresenter = NotificationForegroundPresenter()

    init() {
        UNUserNotificationCenter.current().delegate = notificationPresenter

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
