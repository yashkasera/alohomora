package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.desktop.data.local.ApiLogStore
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseSnapshotStore
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.data.local.PrefsStore
import io.github.yashkasera.alohomora.devtools.ApiLogPayload
import io.github.yashkasera.alohomora.devtools.AppDatabaseInfo
import io.github.yashkasera.alohomora.devtools.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.devtools.DatabaseSnapshotPayload
import io.github.yashkasera.alohomora.devtools.DatabaseTableColumnPayload
import io.github.yashkasera.alohomora.devtools.DatabaseTableSchemaPayload
import io.github.yashkasera.alohomora.devtools.DevToolsEnvelope
import io.github.yashkasera.alohomora.devtools.DevToolsMessageType
import io.github.yashkasera.alohomora.devtools.DevToolsProtocol
import io.github.yashkasera.alohomora.devtools.EventPayload
import io.github.yashkasera.alohomora.devtools.InitialStatePayload
import io.github.yashkasera.alohomora.devtools.PrefsSnapshotPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class DevToolsRepositoryImplTest {
    @Test
    fun handlesInitialStateAndSnapshots() = runBlocking {
        val eventStore = EventStore()
        val apiLogStore = ApiLogStore()
        val databaseStore = DatabaseSnapshotStore()
        val prefsStore = PrefsStore()

        val initialPayload = InitialStatePayload(
            events = listOf(EventPayload(1, "launch", null, 100)),
            apiLogs = listOf(
                ApiLogPayload(
                    id = "1",
                    status = 200,
                    url = "https://example.com",
                    message = null,
                    method = "GET",
                    scheme = "https",
                    host = "example.com",
                    path = "/",
                    query = null,
                    request = null,
                    response = null,
                    time = 100,
                    duration = 10,
                    requestHeaders = null,
                    responseHeaders = null,
                    curl = null,
                    size = null,
                    isViewed = false
                )
            ),
            databaseSchema = DatabaseSchemaSnapshot(
                databaseName = "app.db",
                tables = listOf("Analytics"),
                schemas = listOf(
                    DatabaseTableSchemaPayload(
                        name = "Analytics",
                        columns = listOf(DatabaseTableColumnPayload("id", "INTEGER", true, true)),
                        primaryKey = "id",
                        indexes = emptyList()
                    )
                )
            ),
            databases = listOf(AppDatabaseInfo("app.db", "/data/data/com.example.app/databases/app.db")),
            selectedDatabase = "app.db",
            preferenceKeys = listOf("token"),
        )

        val envelopes = listOf(
            DevToolsEnvelope(
                type = DevToolsMessageType.REQUEST_INITIAL_STATE,
                sequence = 1,
                payload = DevToolsProtocol.encodePayload(initialPayload),
            ),
            DevToolsEnvelope(
                type = DevToolsMessageType.SNAPSHOT_PREFS,
                sequence = 2,
                payload = DevToolsProtocol.encodePayload(PrefsSnapshotPayload(values = mapOf("token" to "abc"))),
            ),
            DevToolsEnvelope(
                type = DevToolsMessageType.SNAPSHOT_DATABASE,
                sequence = 3,
                payload = DevToolsProtocol.encodePayload(DatabaseSnapshotPayload(table = null)),
            )
        )

        val repository = DevToolsRepositoryImpl(
            remoteDataSource = FakeRemoteDataSource(envelopes),
            eventStore = eventStore,
            apiLogStore = apiLogStore,
            databaseStore = databaseStore,
            prefsStore = prefsStore,
        )

        repository.connect("localhost", 1234)
        delay(50)
        repository.disconnect()

        assertEquals(1, repository.events.value.size)
        assertEquals("launch", repository.events.value.first().name)
        assertEquals(1, repository.apiLogs.value.size)
        assertEquals("token", repository.prefsState.value.keys.first())
        assertEquals("abc", repository.prefsState.value.values["token"])
        assertEquals("app.db", repository.databaseSnapshot.value.selectedDatabase?.name)
    }
}

private class FakeRemoteDataSource(
    private val envelopes: List<DevToolsEnvelope>,
) : DevToolsRemoteDataSource() {
    override fun connect(host: String, port: Int): DevToolsSocketConnection {
        return FakeSocketConnection()
    }

    override suspend fun processConnection(
        connection: DevToolsSocketConnection,
        onEnvelope: (DevToolsEnvelope) -> Unit,
    ) {
        envelopes.forEach { onEnvelope(it) }
    }
}

private class FakeSocketConnection : DevToolsSocketConnection {
    override fun readExact(byteCount: Int): ByteArray? = null

    override fun write(bytes: ByteArray) = Unit

    override fun close() = Unit
}
