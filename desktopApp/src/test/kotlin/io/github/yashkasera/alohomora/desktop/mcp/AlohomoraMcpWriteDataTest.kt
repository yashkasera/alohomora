package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import io.github.yashkasera.alohomora.desktop.data.adb.AdbCommandResult
import io.github.yashkasera.alohomora.desktop.data.adb.AdbCommandRunner
import io.github.yashkasera.alohomora.desktop.domain.model.ReplayState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

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
        assertTrue(
            repo.replayedRequests.isEmpty(),
            "nothing should be sent when replay is unsupported",
        )
    }

    @Test
    fun `replay refuses an unknown id and a blocked request`() = runTest {
        val repo = FakeDevToolsRepository().apply {
            replayState.value = ReplayState(supported = true)
            traffic.value = listOf(entry("1").apply { requestBodyTruncated = true })
        }
        assertTrue(
            AlohomoraMcpWriteData.replay(
                repo,
                "missing",
                null,
                null,
                null,
                null,
                null,
            ) is WriteResult.Error,
        )
        assertTrue(
            AlohomoraMcpWriteData.replay(
                repo,
                "1",
                null,
                null,
                null,
                null,
                null,
            ) is WriteResult.Error,
        )
    }

    @Test
    fun `replay sends and reports the delivered result`() = runTest {
        val repo = FakeDevToolsRepository().apply {
            replayState.value = ReplayState(supported = true)
            traffic.value = listOf(entry("1"))
        }
        val deferred = async {
            AlohomoraMcpWriteData.replay(
                repo,
                "1",
                method = "post",
                url = null,
                headers = null,
                body = null,
                contentType = null,
            )
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
        val deferred =
            async { AlohomoraMcpWriteData.replay(repo, "1", null, null, null, null, null) }
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
        assertTrue(
            AlohomoraMcpWriteData.replay(
                repo,
                "1",
                null,
                null,
                null,
                null,
                null,
            ) is WriteResult.Error,
        )
    }

    @Test
    fun `clear_captured requires the developer to confirm`() = runTest {
        val repo = FakeDevToolsRepository()
        val broker = McpConfirmationBroker()
        val deferred = async {
            AlohomoraMcpWriteData.clearCaptured(
                repo,
                broker,
                "dev",
                traffic = true,
                events = false,
                errors = false,
                spans = false,
            )
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
            AlohomoraMcpWriteData.clearCaptured(
                repo,
                broker,
                "dev",
                traffic = true,
                events = false,
                errors = false,
                spans = true,
            )
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
        val result = AlohomoraMcpWriteData.clearCaptured(
            repo,
            broker,
            "dev",
            traffic = false,
            events = false,
            errors = false,
            spans = false,
        )
        assertTrue(result is WriteResult.Error)
        assertEquals(null, broker.pending.value, "nothing selected: never opens a dialog")
    }

    @Test
    fun `create_mock_from_traffic creates a rule from a captured entry`() {
        val repo = FakeDevToolsRepository().apply {
            traffic.value = listOf(
                TrafficEntry(
                    id = "1",
                    status = 200,
                    method = "GET",
                    url = "https://api.example.com/users",
                    host = "api.example.com",
                    path = "/users",
                    responseBody = """{"name":"Alice"}""",
                    responseContentType = "application/json; charset=utf-8",
                ),
            )
        }
        val vm = testViewModel(supported = true)
        val result = AlohomoraMcpWriteData.createMockFromTraffic(repo, vm, "1", "mock users")
        assertTrue(result is WriteResult.Ok)
        val rule = Json.decodeFromJsonElement(MockRule.serializer(), result.json)
        assertEquals("/users", rule.urlPattern)
        assertEquals("GET", rule.method)
        assertEquals(200, rule.statusCode)
        assertEquals("""{"name":"Alice"}""", rule.responseBody)
        assertEquals("application/json", rule.contentType)
        assertEquals("mock users", rule.name)
        assertTrue(rule.id.isNotBlank(), "id must be auto-generated")
    }

    @Test
    fun `create_mock_from_traffic refuses unknown id`() {
        val repo = FakeDevToolsRepository()
        val vm = testViewModel(supported = true)
        assertTrue(
            AlohomoraMcpWriteData.createMockFromTraffic(
                repo,
                vm,
                "missing",
                null,
            ) is WriteResult.Error,
        )
    }

    @Test
    fun `create_mock_from_traffic refuses when mocks are unsupported`() {
        val repo = FakeDevToolsRepository().apply { traffic.value = listOf(entry("1")) }
        val vm = testViewModel(supported = false)
        assertTrue(
            AlohomoraMcpWriteData.createMockFromTraffic(
                repo,
                vm,
                "1",
                null,
            ) is WriteResult.Error,
        )
    }

    @Test
    fun `adb allowlist permits shell am and blocks install`() {
        assertEquals(AdbCommandCheck.Allowed, check("shell am force-stop com.example"))
        assertEquals(AdbCommandCheck.Allowed, check("shell dumpsys wifi"))
        assertEquals(AdbCommandCheck.Allowed, check("shell svc wifi disable"))
        assertEquals(AdbCommandCheck.Allowed, check("logcat -d"))
        assertTrue(check("install -r app.apk") is AdbCommandCheck.Rejected)
        assertTrue(check("uninstall com.example") is AdbCommandCheck.Rejected)
        assertTrue(check("push local /sdcard/remote") is AdbCommandCheck.Rejected)
        assertTrue(check("pull /sdcard/remote local") is AdbCommandCheck.Rejected)
        assertTrue(check("reboot") is AdbCommandCheck.Rejected)
        assertTrue(check("root") is AdbCommandCheck.Rejected)
        assertTrue(check("shell rm -rf /") is AdbCommandCheck.Rejected)
        assertTrue(check("shell su") is AdbCommandCheck.Rejected)
        assertTrue(check("shell") is AdbCommandCheck.Rejected)
    }

    @Test
    fun `adb allowlist matches whole arguments rather than a joined prefix`() {
        assertTrue(check("shell amfoo") is AdbCommandCheck.Rejected)
        assertTrue(check("shell svc power reboot") is AdbCommandCheck.Rejected)
        assertTrue(check("shell -t am start") is AdbCommandCheck.Rejected)
    }

    @Test
    fun `adb allowlist rejects shell metacharacters in any argument`() {
        listOf(
            "shell am; reboot",
            "shell am start -d x;reboot",
            "shell am start -d https://a.example/?a=1&b=2",
            "shell dumpsys | sh",
            "shell am start -d `reboot`",
            "shell am start -d \$(reboot)",
            "shell getprop > /sdcard/out",
            "shell am start -d 'x'",
            "shell am start -d \"x\"",
            "shell input text a\\b",
        ).forEach { command ->
            assertTrue(check(command) is AdbCommandCheck.Rejected, "should reject: $command")
        }
    }

    @Test
    fun `logcat filterspecs keep their glob since adb escapes logcat args itself`() {
        assertEquals(AdbCommandCheck.Allowed, check("logcat -d *:E"))
    }

    @Test
    fun `adb pm and cmd need the developer to confirm`() {
        assertEquals(AdbCommandCheck.NeedsConfirmation, check("shell pm clear com.example"))
        assertEquals(AdbCommandCheck.NeedsConfirmation, check("shell cmd package uninstall com.example"))
    }

    @Test
    fun `a denied pm command never reaches adb`() = runTest {
        val runner = RecordingAdbRunner()
        val broker = McpConfirmationBroker()
        val deferred = async {
            AlohomoraMcpWriteData.runAdbCommand("dev", "shell pm clear com.example", broker, runner)
        }
        runCurrent()
        assertTrue(broker.pending.value!!.message.contains("shell pm clear com.example"))
        broker.pending.value!!.resolve(false)
        assertTrue(deferred.await() is WriteResult.Error)
        assertTrue(runner.calls.isEmpty(), "a denied command must not run")
    }

    @Test
    fun `an approved pm command runs against the session device`() = runTest {
        val runner = RecordingAdbRunner()
        val broker = McpConfirmationBroker()
        val deferred = async {
            AlohomoraMcpWriteData.runAdbCommand("dev", "shell pm clear com.example", broker, runner)
        }
        runCurrent()
        broker.pending.value!!.resolve(true)
        assertTrue(deferred.await() is WriteResult.Ok)
        assertEquals(
            listOf(listOf("-s", "dev", "shell", "pm", "clear", "com.example")),
            runner.calls,
        )
    }

    @Test
    fun `an unconfirmed program runs without prompting`() = runTest {
        val runner = RecordingAdbRunner()
        val broker = McpConfirmationBroker()
        val result =
            AlohomoraMcpWriteData.runAdbCommand("dev", "shell am force-stop com.example", broker, runner)
        assertTrue(result is WriteResult.Ok)
        assertEquals(null, broker.pending.value)
        assertEquals(1, runner.calls.size)
    }

    @Test
    fun `a rejected command neither prompts nor runs`() = runTest {
        val runner = RecordingAdbRunner()
        val broker = McpConfirmationBroker()
        val result = AlohomoraMcpWriteData.runAdbCommand("dev", "shell pm clear x;reboot", broker, runner)
        assertTrue(result is WriteResult.Error)
        assertEquals(null, broker.pending.value)
        assertTrue(runner.calls.isEmpty())
    }

    private fun check(command: String) = checkAdbCommand(command.split(" "))

    private class RecordingAdbRunner : AdbCommandRunner {
        val calls = mutableListOf<List<String>>()

        override fun run(args: List<String>): AdbCommandResult {
            calls += args
            return AdbCommandResult(0, "", "")
        }

        override fun runDetached(args: List<String>): String? = null
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

    private fun testViewModel(supported: Boolean): NetworkRulesViewModel {
        val vm = NetworkRulesViewModel(
            FakeDevToolsRepository().apply { networkRulesSupported.value = supported },
        )
        // Kill the init coroutines immediately — they hit the real filesystem via MockSessionStore
        // and leak onto Dispatchers.IO, which runTest picks up as UncaughtExceptionsBeforeTest.
        // The tests here only need addRule() and networkRulesSupported, both scope-independent.
        vm.close()
        return vm
    }
}
