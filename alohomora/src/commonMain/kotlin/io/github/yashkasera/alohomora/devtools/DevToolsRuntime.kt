package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement

internal object DevToolsDefaults {
    const val DEFAULT_PORT: Int = 53999
    const val EVENT_SNAPSHOT_LIMIT: Int = 500
    const val API_LOG_SNAPSHOT_LIMIT: Int = 200
    const val STREAM_BUFFER_CAPACITY: Int = 1024
}

internal class DevToolsRuntime(
    private val eventRepository: EventRepository,
    private val networkRepository: NetworkRepository,
    private val database: AlohomoraDb,
    private val preferencesInspector: DevToolsPreferencesInspector,
    private val server: DevToolsTcpServer,
    private val appDatabaseProvider: DevToolsAppDatabaseProvider,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sequenceMutex = Mutex()
    private var nextSequence: Long = 1
    private var activeConnection: DevToolsConnection? = null
    private val databaseInspector = DevToolsDatabaseInspector(database, appDatabaseProvider)
    private var defaultDatabaseName: String? = null

    fun start(port: Int = DevToolsDefaults.DEFAULT_PORT) {
        if (!isDebugBuild) return
        server.start(port) { socket ->
            scope.launch {
                attachClient(socket)
            }
        }
    }

    fun stop() {
        activeConnection?.close()
        activeConnection = null
        server.stop()
    }

    private suspend fun attachClient(socket: DevToolsSocket) {
        activeConnection?.close()
        val connection = DevToolsConnection(socket)
        activeConnection = connection
        connection.start()
    }

    private suspend fun nextSequence(): Long {
        return sequenceMutex.withLock {
            nextSequence++
        }
    }

    private inner class DevToolsConnection(
        private val socket: DevToolsSocket,
    ) {
        private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val outbound = Channel<DevToolsEnvelope>(
            capacity = DevToolsDefaults.STREAM_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        private val eventAdapter = DevToolsStreamAdapter { event: EventPayload -> event.time }
        private val apiAdapter = DevToolsStreamAdapter { log: ApiLogPayload -> log.time }

        fun start() {
            connectionScope.launch { writerLoop() }
            connectionScope.launch { readerLoop() }
            connectionScope.launch { streamEvents() }
            connectionScope.launch { streamApiLogs() }
            connectionScope.launch { sendInitialState() }
        }

        fun close() {
            outbound.close()
            socket.close()
            connectionScope.coroutineContext.cancel()
            if (activeConnection === this) {
                activeConnection = null
            }
        }

        private suspend fun writerLoop() {
            for (envelope in outbound) {
                val frame = DevToolsProtocol.encodeEnvelope(envelope)
                socket.write(frame)
            }
        }

        private suspend fun readerLoop() {
            while (true) {
                val envelope = DevToolsProtocol.readEnvelope(socket) ?: break
                when (envelope.type) {
                    DevToolsMessageType.REQUEST_INITIAL_STATE -> sendInitialState()
                    DevToolsMessageType.REQUEST_DATABASE_SCHEMA -> handleDatabaseSchemaRequest(envelope.payload)
                    DevToolsMessageType.REQUEST_DATABASE_TABLE -> handleDatabaseRequest(envelope.payload)
                    DevToolsMessageType.REQUEST_PREF_VALUE -> handlePreferenceRequest(envelope.payload)
                    else -> Unit
                }
            }
            close()
        }

        private suspend fun sendInitialState() {
            val events = database.eventDao()
                .getLatest(DevToolsDefaults.EVENT_SNAPSHOT_LIMIT)
                .map { EventPayload.from(it) }
            val apiLogs = database.networkDao()
                .getLatest(DevToolsDefaults.API_LOG_SNAPSHOT_LIMIT)
                .map { ApiLogPayload.from(it) }
            val databases = databaseInspector.listDatabases()
            val selectedDatabase = databases.firstOrNull()?.name
            defaultDatabaseName = selectedDatabase
            val schema = if (selectedDatabase == null) {
                DatabaseSchemaSnapshot(
                    databaseName = null,
                    tables = emptyList(),
                    schemas = emptyList(),
                )
            } else {
                databaseInspector.loadSchema(selectedDatabase)
            }
            val preferenceKeys = preferencesInspector.getAllKeys()
            val payload = InitialStatePayload(
                events = events,
                apiLogs = apiLogs,
                databaseSchema = schema,
                databases = databases,
                selectedDatabase = selectedDatabase,
                preferenceKeys = preferenceKeys,
            )
            eventAdapter.seed(events)
            apiAdapter.seed(apiLogs)
            send(DevToolsMessageType.REQUEST_INITIAL_STATE, payload)
        }

        private suspend fun streamEvents() {
            eventRepository.getAllEvents().collect { events ->
                val payloads = events.map { EventPayload.from(it) }
                val newItems = eventAdapter.filterNew(payloads)
                newItems.forEach { item ->
                    send(DevToolsMessageType.STREAM_EVENT, item)
                }
            }
        }

        private suspend fun streamApiLogs() {
            networkRepository.getAllCalls().collect { logs ->
                val payloads = logs.map { ApiLogPayload.from(it) }
                val newItems = apiAdapter.filterNew(payloads)
                newItems.forEach { item ->
                    send(DevToolsMessageType.STREAM_API_LOG, item)
                }
            }
        }

        private suspend fun handleDatabaseRequest(payload: JsonElement?) {
            if (payload == null) return
            val request = DevToolsProtocol.decodePayload<RequestDatabaseTablePayload>(payload)
            val databaseName = request.databaseName ?: defaultDatabaseName ?: return
            val tableSnapshot = databaseInspector.loadTable(databaseName, request.tableName, request.limit)
            val snapshotPayload = DatabaseSnapshotPayload(
                databaseName = databaseName,
                table = tableSnapshot,
            )
            send(DevToolsMessageType.SNAPSHOT_DATABASE, snapshotPayload)
        }

        private suspend fun handleDatabaseSchemaRequest(payload: JsonElement?) {
            if (payload == null) return
            val request = DevToolsProtocol.decodePayload<RequestDatabaseSchemaPayload>(payload)
            defaultDatabaseName = request.databaseName
            val schema = databaseInspector.loadSchema(request.databaseName)
            val snapshotPayload = DatabaseSnapshotPayload(
                databaseName = request.databaseName,
                schema = schema,
            )
            send(DevToolsMessageType.SNAPSHOT_DATABASE, snapshotPayload)
        }

        private suspend fun handlePreferenceRequest(payload: JsonElement?) {
            if (payload == null) return
            val request = DevToolsProtocol.decodePayload<RequestPrefValuePayload>(payload)
            val value = preferencesInspector.getValue(request.key)
            val snapshotPayload = PrefsSnapshotPayload(values = mapOf(request.key to value))
            send(DevToolsMessageType.SNAPSHOT_PREFS, snapshotPayload)
        }

        private suspend inline fun <reified T> send(type: DevToolsMessageType, payload: T) {
            val envelope = DevToolsEnvelope(
                type = type,
                sequence = nextSequence(),
                payload = DevToolsProtocol.encodePayload(payload),
            )
            outbound.trySend(envelope)
        }
    }
}
