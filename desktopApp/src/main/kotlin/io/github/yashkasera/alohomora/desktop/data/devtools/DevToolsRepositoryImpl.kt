package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.desktop.data.local.ApiLogStore
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseSnapshotStore
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.data.local.PrefsStore
import io.github.yashkasera.alohomora.desktop.domain.model.ApiLog
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.Event
import io.github.yashkasera.alohomora.desktop.domain.model.PrefsState
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.devtools.ApiLogPayload
import io.github.yashkasera.alohomora.devtools.DatabaseSnapshotPayload
import io.github.yashkasera.alohomora.devtools.DevToolsEnvelope
import io.github.yashkasera.alohomora.devtools.DevToolsMessageType
import io.github.yashkasera.alohomora.devtools.DevToolsProtocol
import io.github.yashkasera.alohomora.devtools.EventPayload
import io.github.yashkasera.alohomora.devtools.InitialStatePayload
import io.github.yashkasera.alohomora.devtools.PrefsSnapshotPayload
import io.github.yashkasera.alohomora.devtools.RequestDatabaseSchemaPayload
import io.github.yashkasera.alohomora.devtools.RequestDatabaseTablePayload
import io.github.yashkasera.alohomora.devtools.RequestPrefValuePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DevToolsRepositoryImpl(
    private val remoteDataSource: DevToolsRemoteDataSource = DevToolsRemoteDataSource(),
    private val eventStore: EventStore = EventStore(),
    private val apiLogStore: ApiLogStore = ApiLogStore(),
    private val databaseStore: DatabaseSnapshotStore = DatabaseSnapshotStore(),
    private val prefsStore: PrefsStore = PrefsStore(),
) : DevToolsRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<DevToolsConnection>(DevToolsConnection.Disconnected)
    override val connectionState: StateFlow<DevToolsConnection> = _state.asStateFlow()
    private val _currentDeviceId = MutableStateFlow<String?>(null)
    override val currentDeviceId: StateFlow<String?> = _currentDeviceId.asStateFlow()
    private val _switching = MutableStateFlow(false)
    override val switching: StateFlow<Boolean> = _switching.asStateFlow()

    override val events: StateFlow<List<Event>> = eventStore.events
    override val apiLogs: StateFlow<List<ApiLog>> = apiLogStore.logs
    override val databaseSnapshot: StateFlow<DatabaseSnapshot> = databaseStore.snapshot
    override val prefsState: StateFlow<PrefsState> = prefsStore.state

    private var connectionJobActive = false
    private var connection: DevToolsSocketConnection? = null
    private val writeMutex = Mutex()

    override fun connect(host: String, port: Int) {
        if (connectionJobActive) return
        connectionJobActive = true
        _switching.value = true
        _state.value = DevToolsConnection.Connecting(host, port)
        scope.launch {
            while (connectionJobActive) {
                try {
                    val socket = remoteDataSource.connect(host, port)
                    connection = socket
                    _state.value = DevToolsConnection.Connected(host, port)
                    _switching.value = false
                    remoteDataSource.processConnection(socket, ::handleEnvelope)
                } catch (e: Exception) {
                    _state.value = DevToolsConnection.Failed(e.message ?: "Connection failed")
                    _switching.value = false
                } finally {
                    connection?.close()
                    connection = null
                    if (connectionJobActive) {
                        _state.value = DevToolsConnection.Connecting(host, port)
                        delay(2000)
                    }
                }
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
        scope.launch {
            sendRequest(
                DevToolsMessageType.REQUEST_DATABASE_SCHEMA,
                RequestDatabaseSchemaPayload(databaseName)
            )
        }
    }

    override fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int) {
        scope.launch {
            sendRequest(
                DevToolsMessageType.REQUEST_DATABASE_TABLE,
                RequestDatabaseTablePayload(databaseName, tableName, limit)
            )
        }
    }

    override fun requestPrefValue(key: String) {
        scope.launch {
            sendRequest(
                DevToolsMessageType.REQUEST_PREF_VALUE,
                RequestPrefValuePayload(key)
            )
        }
    }

    override fun requestInitialState() {
        scope.launch {
            sendRequest(DevToolsMessageType.REQUEST_INITIAL_STATE)
        }
    }

    private suspend inline fun <reified T> sendRequest(type: DevToolsMessageType, payload: T) {
        val envelope = DevToolsEnvelope(
            type = type,
            sequence = 0,
            payload = DevToolsProtocol.encodePayload(payload),
        )
        val frame = DevToolsProtocol.encodeEnvelope(envelope)
        writeMutex.withLock {
            connection?.write(frame)
        }
    }

    private suspend fun sendRequest(type: DevToolsMessageType) {
        val envelope = DevToolsEnvelope(
            type = type,
            sequence = 0,
            payload = null,
        )
        val frame = DevToolsProtocol.encodeEnvelope(envelope)
        writeMutex.withLock {
            connection?.write(frame)
        }
    }

    private fun handleEnvelope(envelope: DevToolsEnvelope) {
        when (envelope.type) {
            DevToolsMessageType.REQUEST_INITIAL_STATE -> {
                envelope.payload?.let {
                    val payload = DevToolsProtocol.decodePayload<InitialStatePayload>(it)
                    eventStore.replace(payload.events.map { event -> event.toDomain() })
                    apiLogStore.replace(payload.apiLogs.map { log -> log.toDomain() })
                    databaseStore.replaceDatabases(
                        payload.databases.map { it.toDomain() },
                        payload.selectedDatabase
                    )
                    databaseStore.replaceSchema(payload.databaseSchema.toDomain())
                    prefsStore.replaceKeys(payload.preferenceKeys)
                }
            }
            DevToolsMessageType.STREAM_EVENT -> {
                envelope.payload?.let {
                    val payload = DevToolsProtocol.decodePayload<EventPayload>(it)
                    eventStore.append(payload.toDomain())
                }
            }
            DevToolsMessageType.STREAM_API_LOG -> {
                envelope.payload?.let {
                    val payload = DevToolsProtocol.decodePayload<ApiLogPayload>(it)
                    apiLogStore.append(payload.toDomain())
                }
            }
            DevToolsMessageType.SNAPSHOT_DATABASE -> {
                envelope.payload?.let {
                    val payload = DevToolsProtocol.decodePayload<DatabaseSnapshotPayload>(it)
                    databaseStore.applySnapshot(
                        payload.schema?.toDomain(),
                        payload.table?.toDomain()
                    )
                }
            }
            DevToolsMessageType.SNAPSHOT_PREFS -> {
                envelope.payload?.let {
                    val payload = DevToolsProtocol.decodePayload<PrefsSnapshotPayload>(it)
                    prefsStore.applySnapshot(payload.keys, payload.values)
                }
            }
            else -> Unit
        }
    }

    private fun clearAll() {
        eventStore.clear()
        apiLogStore.clear()
        databaseStore.clear()
        prefsStore.clear()
    }
}
