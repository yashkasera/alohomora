package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseSnapshotPayload
import io.github.yashkasera.alohomora.common.DevToolsEnvelope
import io.github.yashkasera.alohomora.common.DevToolsMessageType
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.InitialStatePayload
import io.github.yashkasera.alohomora.common.PrefsSnapshotPayload
import io.github.yashkasera.alohomora.common.RequestDatabaseSchemaPayload
import io.github.yashkasera.alohomora.common.RequestDatabaseTablePayload
import io.github.yashkasera.alohomora.common.RequestPrefValuePayload
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.TelemetryRepository
import io.github.yashkasera.alohomora.domain.repository.TraceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val telemetryRepository: TelemetryRepository,
    private val traceRepository: TraceRepository,
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
    private val _serverState = MutableStateFlow(DevToolsServerState())
    val serverState: StateFlow<DevToolsServerState> = _serverState.asStateFlow()

    fun start(port: Int = DevToolsDefaults.DEFAULT_PORT): Boolean {
        if (!isDebugBuild) return false
        val started = server.start(port) { socket ->
            scope.launch {
                attachClient(socket)
            }
        }
        _serverState.value = _serverState.value.copy(
            isRunning = started,
            port = if (started) port else null,
            lastError = if (started) null else "Failed to start server",
        )
        return started
    }

    fun stop() {
        activeConnection?.close()
        activeConnection = null
        server.stop()
        _serverState.value = _serverState.value.copy(
            isRunning = false,
            hasClient = false,
            port = null,
        )
    }

    private suspend fun attachClient(socket: DevToolsSocket) {
        activeConnection?.close()
        val connection = DevToolsConnection(socket)
        activeConnection = connection
        _serverState.value = _serverState.value.copy(hasClient = true, lastError = null)
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
        private val eventAdapter = DevToolsStreamAdapter { event: TelemetryEvent -> event.time }
        private val apiAdapter = DevToolsStreamAdapter { log: TraceEntry -> log.time ?: 0L }

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
                _serverState.value = _serverState.value.copy(hasClient = false)
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
                    DevToolsMessageType.REQUEST_DATABASE_SCHEMA -> handleDatabaseSchemaRequest(
                        envelope.payload,
                    )

                    DevToolsMessageType.REQUEST_DATABASE_TABLE -> handleDatabaseRequest(envelope.payload)
                    DevToolsMessageType.REQUEST_PREF_VALUE -> handlePreferenceRequest(envelope.payload)
                    else -> Unit
                }
            }
            close()
        }

        private suspend fun sendInitialState() {
            val events = database.telemetryDao()
                .getLatest(DevToolsDefaults.EVENT_SNAPSHOT_LIMIT)
            val apiLogs = database.traceDao()
                .getLatest(DevToolsDefaults.API_LOG_SNAPSHOT_LIMIT)
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
                buildInfo = Alohomora.config?.toBuildInfoPayload(),
                chronicle = Alohomora.config?.commits?.map { it.toChronicleCommitPayload() }.orEmpty(),
            )
            eventAdapter.seed(events)
            apiAdapter.seed(apiLogs)
            send(DevToolsMessageType.REQUEST_INITIAL_STATE, payload)
        }

        private suspend fun streamEvents() {
            telemetryRepository.list("", 0, DevToolsDefaults.EVENT_SNAPSHOT_LIMIT).collect { events ->
                val newItems = eventAdapter.filterNew(events)
                newItems.forEach { item ->
                    send(DevToolsMessageType.STREAM_EVENT, item)
                }
            }
        }

        private suspend fun streamApiLogs() {
            traceRepository.list("", "", 0, DevToolsDefaults.API_LOG_SNAPSHOT_LIMIT).collect { logs ->
                val newItems = apiAdapter.filterNew(logs)
                newItems.forEach { item ->
                    send(DevToolsMessageType.STREAM_API_LOG, item)
                }
            }
        }

        private suspend fun handleDatabaseRequest(payload: JsonElement?) {
            if (payload == null) return
            val request = DevToolsProtocol.decodePayload<RequestDatabaseTablePayload>(payload)
            val databaseName = request.databaseName ?: defaultDatabaseName ?: return
            val tableSnapshot =
                databaseInspector.loadTable(databaseName, request.tableName, request.limit)
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
