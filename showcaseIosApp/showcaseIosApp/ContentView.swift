import SwiftUI
import AlohomoraKit

// MARK: - Model

struct Post: Codable, Identifiable {
    let id: Int
    let userId: Int
    let title: String
    let body: String
}

// MARK: - Network

class PostsService {
    static let shared = PostsService()
    private init() {}

    private let endpoint = URL(string: "https://jsonplaceholder.typicode.com/posts")!

    // A traced session, NOT URLSession.shared.
    //
    // URLSession ignores globally registered NSURLProtocol classes, and URLSession.shared can
    // never be intercepted because its configuration is immutable — so registerURLProtocol()
    // alone captures nothing and the Traces screen stays empty while telemetry still works.
    // The protocol class must live in this session's own configuration.
    private let session = URLSession(
        configuration: Alohomora.shared.alohomoraURLSessionConfiguration()
    )

    func fetchPosts() async throws -> [Post] {
        let (data, _) = try await session.data(from: endpoint)
        return try JSONDecoder().decode([Post].self, from: data)
    }

    func fetchPost(id: Int) async throws -> Post {
        let url = URL(string: "https://jsonplaceholder.typicode.com/posts/\(id)")!
        let (data, _) = try await session.data(from: url)
        return try JSONDecoder().decode(Post.self, from: data)
    }
}

// MARK: - ViewModels

@MainActor
class PostsViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true
        errorMessage = nil
       Alohomora.shared.recordTelemetry(name: "posts_refresh_start", properties: nil)

        do {
            posts = try await PostsService.shared.fetchPosts()
           Alohomora.shared.recordTelemetry(
               name: "posts_refresh_success",
               properties: ["count": String(posts.count)]
           )
        } catch {
            errorMessage = error.localizedDescription
           Alohomora.shared.recordTelemetry(
               name: "posts_refresh_failure",
               properties: ["error": error.localizedDescription]
           )
        }

        isLoading = false
    }

    func onPostTapped(_ post: Post) {
       Alohomora.shared.recordTelemetry(
           name: "post_clicked",
           properties: ["postId": String(post.id)]
       )
        Task { try? await PostsService.shared.fetchPost(id: post.id) }
    }
}

class PreferencesViewModel: ObservableObject {
    private let defaults = UserDefaults.standard

    @Published var username: String {
        didSet {
            defaults.set(username, forKey: "username")
        }
    }

    @Published var autoRefresh: Bool {
        didSet {
            defaults.set(autoRefresh, forKey: "auto_refresh")
           Alohomora.shared.recordTelemetry(
               name: "auto_refresh_toggled",
               properties: ["enabled": autoRefresh ? "true" : "false"]
           )
        }
    }

    var lastRefreshDate: Date? {
        let epoch = defaults.double(forKey: "last_refresh_epoch_millis")
        guard epoch > 0 else { return nil }
        return Date(timeIntervalSince1970: epoch / 1000)
    }

    init() {
        username = defaults.string(forKey: "username") ?? ""
        autoRefresh = defaults.bool(forKey: "auto_refresh")
    }

    func commitUsername() {
        defaults.set(username, forKey: "username")
       Alohomora.shared.recordTelemetry(name: "username_updated", properties: nil)
    }

    func markRefreshed() {
        defaults.set(Date().timeIntervalSince1970 * 1000, forKey: "last_refresh_epoch_millis")
    }
}

// MARK: - Root view

struct ContentView: View {
    @StateObject private var postsVM = PostsViewModel()
    @StateObject private var prefsVM = PreferencesViewModel()
    @State private var showDevTools = false

    var body: some View {
        NavigationStack {
            List {
                preferencesSection
                postsSection
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Alohomora Showcase")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showDevTools = true
                    } label: {
                        Label("DevTools", systemImage: "wrench.and.screwdriver")
                    }
                }
            }
            .task {
                await postsVM.load()
            }
            .refreshable {
                prefsVM.markRefreshed()
                await postsVM.load()
            }
           .sheet(isPresented: $showDevTools) {
                DevToolsSheet(onClose: { showDevTools = false })
                    .ignoresSafeArea()
                    // Belt and braces. The console renders its own close button (wired to
                    // onClose), but the grab handle gives a second, native way out — Compose
                    // consumes the sheet's swipe-to-dismiss drag, so without either of these the
                    // sheet cannot be dismissed at all.
                    .presentationDragIndicator(.visible)
            }
        }
    }

    // MARK: Preferences section

    @ViewBuilder
    private var preferencesSection: some View {
        Section("Preferences") {
            HStack {
                Text("Username")
                Spacer()
                TextField("Enter username", text: $prefsVM.username)
                    .multilineTextAlignment(.trailing)
                    .foregroundStyle(.secondary)
                    .onSubmit { prefsVM.commitUsername() }
            }

            Toggle("Auto Refresh", isOn: $prefsVM.autoRefresh)

            if let date = prefsVM.lastRefreshDate {
                HStack {
                    Text("Last Refresh")
                    Spacer()
                    Text(date, style: .relative)
                        .foregroundStyle(.secondary)
                        .font(.subheadline)
                }
            }
        }
    }

    // MARK: Posts section

    @ViewBuilder
    private var postsSection: some View {
        Section {
            if let error = postsVM.errorMessage {
                Label(error, systemImage: "exclamationmark.triangle")
                    .foregroundStyle(.red)
            } else if postsVM.isLoading {
                HStack {
                    Spacer()
                    ProgressView()
                        .padding(.vertical, 8)
                    Spacer()
                }
                .listRowSeparator(.hidden)
            } else {
                ForEach(postsVM.posts) { post in
                    PostCard(post: post)
                        .contentShape(Rectangle())
                        .onTapGesture { postsVM.onPostTapped(post) }
                }
            }
        } header: {
            HStack {
                Text("Posts")
                Spacer()
                Button {
                    Task {
                        prefsVM.markRefreshed()
                        await postsVM.load()
                    }
                } label: {
                    Text(postsVM.isLoading ? "Refreshing…" : "Refresh")
                        .foregroundStyle(postsVM.isLoading ? .secondary : .quaternary)
                }
                .disabled(postsVM.isLoading)
            }
        }
    }
}

// MARK: - Post card

struct PostCard: View {
    let post: Post

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(post.title.capitalized)
                .font(.headline)
                .lineLimit(2)
            Text(post.body)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(3)
        }
        .padding(.vertical, 2)
    }
}

// MARK: - DevTools sheet

// Embeds the Alohomora Compose DevTools UI in a SwiftUI sheet.
struct DevToolsSheet: UIViewControllerRepresentable {
    /// Dismisses the sheet. Passed into the Compose console, which renders the close button.
    let onClose: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        MainKt.MainViewController(onClose: onClose)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
