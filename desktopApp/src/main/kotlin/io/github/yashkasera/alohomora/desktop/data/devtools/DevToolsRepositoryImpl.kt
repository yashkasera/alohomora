package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.common.AuthFailureMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.AuthSuccessMessage
import io.github.yashkasera.alohomora.common.DatabaseSnapshotMessage
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.PrefsSnapshotMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseSchemaMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseTableMessage
import io.github.yashkasera.alohomora.common.RequestInitialStateMessage
import io.github.yashkasera.alohomora.common.RequestPrefValueMessage
import io.github.yashkasera.alohomora.common.StreamApiLogMessage
import io.github.yashkasera.alohomora.common.StreamEventMessage
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.data.local.ApiLogStore
import io.github.yashkasera.alohomora.desktop.data.local.BuildInfoStore
import io.github.yashkasera.alohomora.desktop.data.local.ChronicleStore
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseSnapshotStore
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.data.local.PrefsStore
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.ChronicleCommit
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.PrefsState
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DevToolsRepositoryImpl(
    private val remoteDataSource: DevToolsRemoteDataSource,
    private val eventStore: EventStore,
    private val apiLogStore: ApiLogStore,
    private val databaseStore: DatabaseSnapshotStore,
    private val prefsStore: PrefsStore,
    private val buildInfoStore: BuildInfoStore,
    private val chronicleStore: ChronicleStore,
) : DevToolsRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<DevToolsConnection>(DevToolsConnection.Disconnected)
    override val connectionState: StateFlow<DevToolsConnection> = _state.asStateFlow()
    private val _currentDeviceId = MutableStateFlow<String?>(null)
    override val currentDeviceId: StateFlow<String?> = _currentDeviceId.asStateFlow()
    private val _switching = MutableStateFlow(false)
    override val switching: StateFlow<Boolean> = _switching.asStateFlow()

    override val events: StateFlow<List<TelemetryEvent>> = eventStore.events
    override val apiLogs: StateFlow<List<TraceEntry>> = apiLogStore.logs
    override val databaseSnapshot: StateFlow<DatabaseSnapshot> = databaseStore.snapshot
    override val prefsState: StateFlow<PrefsState> = prefsStore.state
    override val buildInfo: StateFlow<BuildInfo?> = buildInfoStore.buildInfo
    override val chronicle: StateFlow<List<ChronicleCommit>> = chronicleStore.commits

    private var connectionJobActive = false
    private var connection: DevToolsSocket? = null
    private val writeMutex = Mutex()

    override fun connect(host: String, port: Int) {
        if (connectionJobActive) return
        connectionJobActive = true
        _switching.value = true
        _state.value = DevToolsConnection.Connecting(host, port)
        scope.launch {
            try {
                val socket = remoteDataSource.connect(host, port)
                connection = socket
                _state.value = DevToolsConnection.AwaitingAuth(host, port)
                _switching.value = false
                remoteDataSource.processConnection(socket, ::handleMessage)
            } catch (e: Exception) {
                _state.value = DevToolsConnection.Failed(e.message ?: "Connection failed")
                _switching.value = false
            } finally {
                connection?.close()
                connection = null
                if (_state.value is DevToolsConnection.Connecting ||
                    _state.value is DevToolsConnection.AwaitingAuth ||
                    _state.value is DevToolsConnection.Connected
                ) {
                    _state.value = DevToolsConnection.Disconnected
                }
                _switching.value = false
                connectionJobActive = false
            }
        }
    }

    override fun switchDevice(host: String, port: Int, deviceId: String?) {
        _currentDeviceId.value = deviceId
        clearAll()
        val current = _state.value
        val isSameTarget = current is DevToolsConnection.Connected &&
            current.host == host && current.port == port &&
            connection != null
        if (isSameTarget) {
            requestInitialState()
            return
        }
        disconnect()
        connect(host, port)
    }

    override fun disconnect() {
        connectionJobActive = false
        connection?.close()
        connection = null
        _state.value = DevToolsConnection.Disconnected
        _switching.value = false
        _currentDeviceId.value = null
        clearAll()
    }

    override fun requestDatabaseSchema(databaseName: String) {
        scope.launch { sendMessage(RequestDatabaseSchemaMessage(databaseName = databaseName)) }
    }

    override fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int) {
        scope.launch {
            sendMessage(RequestDatabaseTableMessage(databaseName = databaseName, tableName = tableName, limit = limit))
        }
    }

    override fun requestPrefValue(key: String) {
        scope.launch { sendMessage(RequestPrefValueMessage(key = key)) }
    }

    override fun requestInitialState() {
        scope.launch { sendMessage(RequestInitialStateMessage()) }
    }

    override fun submitOtp(otp: String) {
        scope.launch { sendMessage(AuthResponseMessage(otp = otp)) }
    }

    private suspend fun sendMessage(message: DevToolsMessage) {
        val frame = DevToolsProtocol.encodeEnvelope(message)
        writeMutex.withLock {
            connection?.write(frame)
        }
    }

    private fun handleMessage(message: DevToolsMessage) {
        when (message) {
            is AuthSuccessMessage -> {
                val current = _state.value
                if (current is DevToolsConnection.AwaitingAuth) {
                    _state.value = DevToolsConnection.Connected(current.host, current.port)
                    scope.launch { sendMessage(RequestInitialStateMessage()) }
                }
            }

            is AuthFailureMessage -> {
                _state.value = DevToolsConnection.Failed(message.reason)
            }

            is InitialStateMessage -> {
                val payload = message.payload
                eventStore.replace(payload.events)
                apiLogStore.replace(payload.apiLogs)
                databaseStore.replaceDatabases(
                    payload.databases.map { it.toDomain() },
                    payload.selectedDatabase,
                )
                databaseStore.replaceSchema(payload.databaseSchema.toDomain())
                prefsStore.replaceKeys(payload.preferenceKeys)
                buildInfoStore.replace(payload.buildInfo?.toDomain())
                chronicleStore.replace(payload.chronicle.map { it.toDomain() })
            }

            is StreamEventMessage -> eventStore.append(message.event)

            is StreamApiLogMessage -> apiLogStore.append(message.log)

            is DatabaseSnapshotMessage -> {
                databaseStore.applySnapshot(
                    message.payload.schema?.toDomain(),
                    message.payload.table?.toDomain(),
                )
            }

            is PrefsSnapshotMessage -> {
                prefsStore.applySnapshot(message.payload.keys, message.payload.values)
            }

            else -> Unit
        }
    }

    private fun clearAll() {
        eventStore.clear()
        apiLogStore.clear()
        databaseStore.clear()
        prefsStore.clear()
        buildInfoStore.clear()
        chronicleStore.clear()
    }
}
