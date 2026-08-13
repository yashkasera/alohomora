import SwiftUI
import AlohomoraKit
import SQLite3

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
        let start = DispatchTime.now()
        let (data, _) = try await session.data(from: endpoint)
        let posts = try JSONDecoder().decode([Post].self, from: data)
        let elapsed = Int64(DispatchTime.now().uptimeNanoseconds - start.uptimeNanoseconds)
        Alohomora.shared.recordSpan(
            name: "GET /posts",
            durationNanos: elapsed,
            attributes: ["http.method": "GET", "post.count": "\(posts.count)"]
        )
        return posts
    }

    func fetchPost(id: Int) async throws -> Post {
        let url = URL(string: "https://jsonplaceholder.typicode.com/posts/\(id)")!
        let start = DispatchTime.now()
        let (data, _) = try await session.data(from: url)
        let post = try JSONDecoder().decode(Post.self, from: data)
        let elapsed = Int64(DispatchTime.now().uptimeNanoseconds - start.uptimeNanoseconds)
        Alohomora.shared.recordSpan(
            name: "GET /posts/\(id)",
            durationNanos: elapsed,
            attributes: ["http.method": "GET", "post.id": "\(id)"]
        )
        return post
    }
}

// MARK: - Local database

// A real on-device SQLite database, so the DevTools Database inspector has something to show.
//
// Alohomora's iOS inspector scans the app sandbox (Documents / Library / Application Support) for
// .db / .sqlite / .sqlite3 files, so writing this into Documents is all it takes to appear there —
// the Android showcase does the same with a Room `posts` table.
final class PostsDatabase {
    static let shared = PostsDatabase()

    // SQLite copies the bound bytes immediately instead of holding the transient Swift String buffer.
    private let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

    private var db: OpaquePointer?
    let path: String

    private init() {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        path = documents.appendingPathComponent("ios_sample.db").path

        guard sqlite3_open(path, &db) == SQLITE_OK else {
            Alohomora.shared.recordError(
                reason: "SQLiteError: could not open \(path)",
                stackTrace: nil,
                place: "PostsDatabase.init"
            )
            return
        }
        sqlite3_exec(
            db,
            """
            CREATE TABLE IF NOT EXISTS posts (
                id INTEGER PRIMARY KEY NOT NULL,
                userId INTEGER NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            );
            """,
            nil, nil, nil
        )
    }

    func replaceAll(_ posts: [Post]) {
        guard let db else { return }
        sqlite3_exec(db, "BEGIN TRANSACTION;", nil, nil, nil)
        sqlite3_exec(db, "DELETE FROM posts;", nil, nil, nil)

        var statement: OpaquePointer?
        let sql = "INSERT OR REPLACE INTO posts (id, userId, title, body, updatedAtEpochMillis) VALUES (?, ?, ?, ?, ?);"
        if sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK {
            let nowMillis = Int64(Date().timeIntervalSince1970 * 1000)
            for post in posts {
                sqlite3_bind_int64(statement, 1, Int64(post.id))
                sqlite3_bind_int64(statement, 2, Int64(post.userId))
                sqlite3_bind_text(statement, 3, post.title, -1, SQLITE_TRANSIENT)
                sqlite3_bind_text(statement, 4, post.body, -1, SQLITE_TRANSIENT)
                sqlite3_bind_int64(statement, 5, nowMillis)
                sqlite3_step(statement)
                sqlite3_reset(statement)
            }
            sqlite3_finalize(statement)
        }

        sqlite3_exec(db, "COMMIT;", nil, nil, nil)
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
        Alohomora.shared.recordEvent(name: "posts_refresh_start", properties: nil)

        let start = DispatchTime.now()
        do {
            posts = try await PostsService.shared.fetchPosts()
            PostsDatabase.shared.replaceAll(posts)
            let elapsed = Int64(DispatchTime.now().uptimeNanoseconds - start.uptimeNanoseconds)
            Alohomora.shared.recordSpan(
                name: "posts.refresh",
                durationNanos: elapsed,
                attributes: ["post.count": "\(posts.count)"]
            )
            Alohomora.shared.recordEvent(
                name: "posts_refresh_success",
                properties: ["count": String(posts.count)]
            )
        } catch {
            errorMessage = error.localizedDescription
            Alohomora.shared.recordError(
                reason: "\(type(of: error)): \(error.localizedDescription)",
                stackTrace: Thread.callStackSymbols.joined(separator: "\n"),
                place: "PostsViewModel.load"
            )
            Alohomora.shared.recordEvent(
                name: "posts_refresh_failure",
                properties: ["error": error.localizedDescription]
            )
        }

        isLoading = false
    }

    func onPostTapped(_ post: Post) {
        Alohomora.shared.recordEvent(
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
            Alohomora.shared.recordEvent(
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
        Alohomora.shared.recordEvent(name: "username_updated", properties: nil)
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
            ZStack(alignment: .bottomTrailing) {
                List {
                    preferencesSection
                    postsSection
                }
                .listStyle(.insetGrouped)

                Button {
                    fatalError("Intentional crash to demo Alohomora error capture")
                } label: {
                    Text("Crash")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(width: 56, height: 56)
                        .background(.red, in: Circle())
                        .shadow(radius: 4, y: 2)
                }
                .padding()
            }
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
        MainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
