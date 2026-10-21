// Closure-based convenience initializers for AlohomoraKit's plugin handler APIs.
// The Kotlin handlers export as ObjC protocols, which Swift can't SAM-convert;
// these inits wrap a bare closure in a private adapter. Additive and non-breaking.

#if canImport(UIKit)
import UIKit
#endif

@_exported import AlohomoraKit

// MARK: - PluginAction

private final class ClosureDevToolsActionHandler: NSObject, DevToolsActionHandler {
    private let body: ([String: String]) async throws -> [String: String]

    init(_ body: @escaping ([String: String]) async throws -> [String: String]) {
        self.body = body
    }

    func execute(params: [String: String]) async throws -> [String: String] {
        try await body(params)
    }
}

extension PluginAction {
    public convenience init(
        id: String,
        label: String,
        description: String? = nil,
        parameters: [Alohomora_commonActionParameter] = [],
        handler: @escaping ([String: String]) async throws -> [String: String]
    ) {
        self.init(
            id: id,
            label: label,
            description: description,
            parameters: parameters,
            handler: ClosureDevToolsActionHandler(handler)
        )
    }
}

// MARK: - PluginDataField

private final class ClosurePluginDataUpdateHandler: NSObject, PluginDataUpdateHandler {
    private let body: (String) async throws -> Void

    init(_ body: @escaping (String) async throws -> Void) {
        self.body = body
    }

    func onUpdate(newValue: String) async throws {
        try await body(newValue)
    }
}

extension PluginDataField {
    public convenience init(
        key: String,
        label: String,
        type: String = "string",
        value: @escaping () -> String,
        options: [String]? = nil,
        readOnly: Bool = false,
        onUpdate: @escaping (String) async throws -> Void
    ) {
        self.init(
            key: key,
            label: label,
            type: type,
            value: value,
            options: options,
            readOnly: readOnly,
            onUpdate: ClosurePluginDataUpdateHandler(onUpdate)
        )
    }
}

// MARK: - NativeScreenPlugin (iOS only)

#if canImport(UIKit)

private final class ClosureNativeScreenViewControllerProvider: NSObject, NativeScreenViewControllerProvider {
    private let body: () -> UIViewController

    init(_ body: @escaping () -> UIViewController) {
        self.body = body
    }

    func createViewController() -> UIViewController {
        body()
    }
}

extension NativeScreenPlugin {
    public convenience init(
        id: String,
        title: String,
        description: String? = nil,
        showInDashboard: Bool = true,
        showInNavigation: Bool = false,
        priority: Int32 = 100,
        actions: [PluginAction] = [],
        dataFields: [PluginDataField] = [],
        viewControllerProvider: @escaping () -> UIViewController
    ) {
        self.init(
            id: id,
            title: title,
            description: description,
            showInDashboard: showInDashboard,
            showInNavigation: showInNavigation,
            priority: priority,
            actions: actions,
            dataFields: dataFields,
            viewControllerProvider: ClosureNativeScreenViewControllerProvider(viewControllerProvider)
        )
    }
}

#endif
