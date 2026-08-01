import SwiftUI
import AlohomoraKit

@main
struct ShowcaseApp: App {

    init() {
        // init() takes no arguments any more. It previously accepted a Koin declaration, which
        // leaked the library's DI container into the host app; Alohomora now builds an isolated
        // container internally so it cannot collide with the app's own.
        Alohomora.shared.doInit()
        _ = Alohomora.shared.registerURLProtocol()
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
