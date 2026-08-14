package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import io.github.yashkasera.alohomora.desktop.domain.model.ReplayState
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The write actions that don't go through NetworkRulesViewModel (replay, clear) — the mock/throttle
 * path is view-model-backed and disk-coupled, so it's covered by manual E2E, not here.
 */
class AlohomoraMcpWriteDataTest {

    @Test
    fun `replay refuses when the app registered no handler`() = runTest {
        val repo = FakeDevToolsRepository().apply {
            replayState.value = ReplayState(supported = false)
            traffic.value = listOf(entry("1"))
        }
        val result = AlohomoraMcpWriteData.replay(repo, "1", null, null, null, null, null)
        assertTrue(result is WriteResult.Error)
        assertTrue(repo.replayedRequests.isEmpty(), "nothing should be sent when replay is unsupported")
    }

    @Test
    fun `replay refuses an unknown id and a blocked request`() = runTest {
        val repo = FakeDevToolsRepository().apply {
            replayState.value = ReplayState(supported = true)
            traffic.value = listOf(entry("1").apply { requestBodyTruncated = true })
        }
        assertTrue(AlohomoraMcpWriteData.replay(repo, "missing", null, null, null, null, null) is WriteResult.Error)
        assertTrue(AlohomoraMcpWriteData.replay(repo, "1", null, null, null, null, null) is WriteResult.Error)
    }

    @Test
    fun `replay sends and reports the delivered result`() = runTest {
        val repo = FakeDevToolsRepository().apply {
            replayState.value = ReplayState(supported = true)
            traffic.value = listOf(entry("1"))
        }
        val deferred = async {
            AlohomoraMcpWriteData.replay(repo, "1", method = "post", url = null, headers = null, body = null, contentType = null)
        }
        runCurrent() // let replay send and suspend awaiting the device result
        assertEquals("POST", repo.replayedRequests.single().method, "override should be uppercased")
        repo.deliverReplaySuccess("1", entry("replayed").apply { replayOf = "1"; status = 200 })
        val result = deferred.await()
        assertTrue(result is WriteResult.Ok)
    }

    @Test
    fun `replay reports a device failure`() = runTest {
        val repo = FakeDevToolsRepository().apply {
            replayState.value = ReplayState(supported = true)
            traffic.value = listOf(entry("1"))
        }
        val deferred = async { AlohomoraMcpWriteData.replay(repo, "1", null, null, null, null, null) }
        runCurrent()
        repo.deliverReplayFailure("1", "connection refused")
        assertTrue(deferred.await() is WriteResult.Error)
    }

    @Test
    fun `replay times out when the device never answers`() = runTest {
        val repo = FakeDevToolsRepository().apply {
            replayState.value = ReplayState(supported = true)
            traffic.value = listOf(entry("1"))
        }
        // Never deliver; runTest advances virtual time past the timeout instantly.
        assertTrue(AlohomoraMcpWriteData.replay(repo, "1", null, null, null, null, null) is WriteResult.Error)
    }

    @Test
    fun `clear_captured requires the developer to confirm`() = runTest {
        val repo = FakeDevToolsRepository()
        val broker = McpConfirmationBroker()
        val deferred = async {
            AlohomoraMcpWriteData.clearCaptured(repo, broker, "dev", traffic = true, events = false, errors = false, spans = false)
        }
        runCurrent()
        broker.pending.value!!.resolve(false) // deny
        assertTrue(deferred.await() is WriteResult.Error)
        assertTrue(repo.clearCalls.isEmpty(), "a denied clear must not touch the device")
    }

    @Test
    fun `clear_captured maps traffic to the traces wire flag once approved`() = runTest {
        val repo = FakeDevToolsRepository()
        val broker = McpConfirmationBroker()
        val deferred = async {
            AlohomoraMcpWriteData.clearCaptured(repo, broker, "dev", traffic = true, events = false, errors = false, spans = true)
        }
        runCurrent()
        broker.pending.value!!.resolve(true)
        assertTrue(deferred.await() is WriteResult.Ok)
        val call = repo.clearCalls.single()
        assertTrue(call.traces, "agent 'traffic' must map to the wire 'traces' flag")
        assertTrue(call.spans)
        assertFalse(call.events)
    }

    @Test
    fun `clear_captured rejects an empty selection without prompting`() = runTest {
        val repo = FakeDevToolsRepository()
        val broker = McpConfirmationBroker()
        val result = AlohomoraMcpWriteData.clearCaptured(repo, broker, "dev", traffic = false, events = false, errors = false, spans = false)
        assertTrue(result is WriteResult.Error)
        assertEquals(null, broker.pending.value, "nothing selected: never opens a dialog")
    }

    private fun entry(id: String) = TrafficEntry(
        id = id,
        status = 500,
        method = "GET",
        url = "https://api.example.com/things",
        host = "api.example.com",
        path = "/things",
        time = id.hashCode().toLong(),
    )
}
