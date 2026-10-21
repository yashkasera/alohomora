// Compile-only check (not a SwiftPM target): run via Validation/typecheck.sh.
// Proves a Swift caller can pass bare closures to all three plugin handler APIs.

import Foundation
#if canImport(UIKit)
import UIKit
#endif
import AlohomoraKitSwift

enum ClosureUsageSample {

    static func makeAction() -> PluginAction {
        PluginAction(
            id: "refresh",
            label: "Refresh",
            handler: { params in
                let name = params["name"] ?? "world"
                try await Task.sleep(nanoseconds: 1)
                return ["greeting": "hello \(name)"]
            }
        )
    }

    static func makeDataField(store: NSMutableDictionary) -> PluginDataField {
        PluginDataField(
            key: "flag",
            label: "Feature Flag",
            value: { (store["flag"] as? String) ?? "off" },
            onUpdate: { newValue in
                store["flag"] = newValue
            }
        )
    }

    // The protocol-based initializer must still compile unchanged (non-breaking).
    static func makeActionViaProtocol(handler: DevToolsActionHandler) -> PluginAction {
        PluginAction(id: "legacy", label: "Legacy", description: nil, parameters: [], handler: handler)
    }

    #if canImport(UIKit)
    static func makeScreen() -> NativeScreenPlugin {
        NativeScreenPlugin(
            id: "custom",
            title: "Custom Screen",
            viewControllerProvider: {
                let vc = UIViewController()
                vc.view.backgroundColor = .systemBackground
                return vc
            }
        )
    }
    #endif
}
