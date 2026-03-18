package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.AuthChallengeMessage
import io.github.yashkasera.alohomora.common.AuthFailureMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.AuthSuccessMessage
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseSnapshotMessage
import io.github.yashkasera.alohomora.common.DatabaseSnapshotPayload
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.InitialStatePayload
import io.github.yashkasera.alohomora.common.PrefsSnapshotMessage
import io.github.yashkasera.alohomora.common.PrefsSnapshotPayload
import io.github.yashkasera.alohomora.common.RequestDatabaseSchemaMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseTableMessage
import io.github.yashkasera.alohomora.common.RequestInitialStateMessage
import io.github.yashkasera.alohomora.common.RequestPrefValueMessage
import io.github.yashkasera.alohomora.common.StreamApiLogMessage
import io.github.yashkasera.alohomora.common.StreamEventMessage
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.AtomicLong

internal object DevToolsDefaults {
    const val DEFAULT_PORT: Int = 53999
    const val EVENT_SNAPSHOT_LIMIT: Int = 500
    const val API_LOG_SNAPSHOT_LIMIT: Int = 200
    const val STREAM_BUFFER_CAPACITY: Int = 1024
}

internal class DevToolsRuntime(
    private val telemetryRepository: TelemetryRepository,
    private val database: AlohomoraDb,
    private val preferencesInspector: DevToolsPreferencesInspector,
    private val server: DevToolsTcpServer,
    private val appDatabaseProvider: DevToolsAppDatabaseProvider,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sequenceCounter = AtomicLong(1)
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
        _serverState.value = _serverState.value.copy(hasClient = true, lastError = null, pendingOtp = null)
        connection.start()
    }

    private fun nextSequence(): Long = sequenceCounter.getAndIncrement()

    private inner class DevToolsConnection(
        private val socket: DevToolsSocket,
    ) {
        private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val outbound = Channel<DevToolsMessage>(
            capacity = DevToolsDefaults.STREAM_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        private val eventAdapter = DevToolsStreamAdapter { event: TelemetryEvent -> event.time }
        private val apiLogSignatures = linkedMapOf<String, Int>()
        private val otp = (1000..9999).random().toString()
        private var isAuthenticated = false

        fun start() {
            _serverState.value = _serverState.value.copy(pendingOtp = otp)
            outbound.trySend(AuthChallengeMessage(nextSequence()))
            connectionScope.launch { writerLoop() }
            connectionScope.launch { readerLoop() }
        }

        fun close() {
            outbound.close()
            socket.close()
            connectionScope.coroutineContext.cancel()
            if (activeConnection === this) {
                activeConnection = null
                _serverState.value = _serverState.value.copy(hasClient = false, pendingOtp = null)
            }
        }

        private suspend fun writerLoop() {
            for (message in outbound) {
                socket.write(DevToolsProtocol.encodeEnvelope(message))
            }
        }

        private suspend fun readerLoop() {
            while (true) {
                val message = DevToolsProtocol.readEnvelope(socket) ?: break
                if (!isAuthenticated) {
                    if (message is AuthResponseMessage) handleAuthResponse(message.otp)
                    continue
                }
                when (message) {
                    is RequestInitialStateMessage -> sendInitialState()
                    is RequestDatabaseSchemaMessage -> handleDatabaseSchemaRequest(message.databaseName)
                    is RequestDatabaseTableMessage -> handleDatabaseRequest(
                        message.databaseName,
                        message.tableName,
                        message.limit,
                    )
                    is RequestPrefValueMessage -> handlePreferenceRequest(message.key)
                    else -> Unit
                }
            }
            close()
        }

        private fun handleAuthResponse(otp: String) {
            if (otp == this.otp) {
                isAuthenticated = true
                _serverState.value = _serverState.value.copy(pendingOtp = null)
                send(AuthSuccessMessage(nextSequence()))
                connectionScope.launch { streamEvents() }
                connectionScope.launch { streamApiLogs() }
                connectionScope.launch { sendInitialState() }
            } else {
                send(AuthFailureMessage(nextSequence(), "Invalid OTP"))
                close()
            }
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
            seedApiLogSignatures(apiLogs)
            send(InitialStateMessage(nextSequence(), payload))
        }

        private suspend fun streamEvents() {
            telemetryRepository.list("", 0, DevToolsDefaults.EVENT_SNAPSHOT_LIMIT).collect { events ->
                val newItems = eventAdapter.filterNew(events)
                newItems.forEach { item ->
                    send(StreamEventMessage(nextSequence(), item))
                }
            }
        }

        private suspend fun streamApiLogs() {
            database.traceDao().observeLatest(DevToolsDefaults.API_LOG_SNAPSHOT_LIMIT).collect { logs ->
                val changedItems = changedApiLogs(logs)
                changedItems.forEach { item ->
                    send(StreamApiLogMessage(nextSequence(), item))
                }
            }
        }

        private fun seedApiLogSignatures(logs: List<TraceEntry>) {
            apiLogSignatures.clear()
            logs.forEach { log ->
                apiLogSignatures[log.id] = log.streamSignature()
            }
        }

        private fun changedApiLogs(logs: List<TraceEntry>): List<TraceEntry> {
            val visibleIds = logs.mapTo(mutableSetOf()) { it.id }
            apiLogSignatures.keys.retainAll(visibleIds)

            val changed = mutableListOf<TraceEntry>()
            logs.asReversed().forEach { log ->
                val signature = log.streamSignature()
                if (apiLogSignatures[log.id] != signature) {
                    apiLogSignatures[log.id] = signature
                    changed += log
                }
            }
            return changed
        }

        private fun TraceEntry.streamSignature(): Int =
            listOf(
                status,
                message,
                method,
                scheme,
                host,
                path,
                query,
                requestBody,
                responseBody,
                duration,
                requestHeaders,
                requestContentType,
                responseContentType,
                responseHeaders,
                requestSize,
                responseSize,
                time,
            ).hashCode()

        private suspend fun handleDatabaseRequest(databaseName: String?, tableName: String, limit: Int) {
            val resolvedName = databaseName ?: defaultDatabaseName ?: return
            val tableSnapshot = databaseInspector.loadTable(resolvedName, tableName, limit)
            send(DatabaseSnapshotMessage(
                nextSequence(),
                DatabaseSnapshotPayload(databaseName = resolvedName, table = tableSnapshot),
            ))
        }

        private suspend fun handleDatabaseSchemaRequest(databaseName: String) {
            defaultDatabaseName = databaseName
            val schema = databaseInspector.loadSchema(databaseName)
            send(DatabaseSnapshotMessage(
                nextSequence(),
                DatabaseSnapshotPayload(databaseName = databaseName, schema = schema),
            ))
        }

        private suspend fun handlePreferenceRequest(key: String) {
            val value = preferencesInspector.getValue(key)
            send(PrefsSnapshotMessage(
                nextSequence(),
                PrefsSnapshotPayload(values = mapOf(key to value)),
            ))
        }

        private fun send(message: DevToolsMessage) {
            val result = outbound.trySend(message)
            if (result.isFailure) {
                println("[Alohomora] DevTools outbound channel full, dropped ${message::class.simpleName}")
            }
        }
    }
}
