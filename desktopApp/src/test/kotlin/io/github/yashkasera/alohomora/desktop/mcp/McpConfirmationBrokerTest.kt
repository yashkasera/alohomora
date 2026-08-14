package io.github.yashkasera.alohomora.desktop.mcp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class McpConfirmationBrokerTest {

    @Test
    fun `confirm suspends until resolved and clears the pending slot`() = runTest {
        val broker = McpConfirmationBroker()
        val deferred = async { broker.confirm("Clear?", "traffic on dev") }
        runCurrent()

        val pending = broker.pending.value
        assertNotNull(pending, "confirm should publish a pending request")
        pending.resolve(true)

        assertTrue(deferred.await())
        assertNull(broker.pending.value, "resolving clears the slot")
    }

    @Test
    fun `deny returns false`() = runTest {
        val broker = McpConfirmationBroker()
        val deferred = async { broker.confirm("Clear?", "everything") }
        runCurrent()
        broker.pending.value!!.resolve(false)
        assertFalse(deferred.await())
    }

    @Test
    fun `a second confirmation while one is pending is refused`() = runTest {
        val broker = McpConfirmationBroker()
        val first = async { broker.confirm("first", "…") }
        runCurrent()
        // Second overlaps the first -> denied outright rather than queued.
        assertFalse(broker.confirm("second", "…"))
        broker.pending.value!!.resolve(true)
        assertTrue(first.await())
    }
}
