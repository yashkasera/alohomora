package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SideSheetHostState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SideSheetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The controller behind every desktop overlay. Two invariants matter and neither is exercisable
 * from a Compose test: Escape must dismiss in *real open order* (the old hardcoded `when` used a
 * fixed priority and got master/detail sequences wrong), and the singleton id set must round-trip
 * so menu items, shortcuts, and the command palette all agree on what is open.
 */
class SideSheetHostStateTest {

    @Test
    fun `dismissTop fires the most recently registered handler`() {
        val host = SideSheetHostState()
        val dismissed = mutableListOf<String>()
        val tokenA = Any()
        val tokenB = Any()

        host.register(tokenA) { dismissed += "A" }
        host.register(tokenB) { dismissed += "B" }

        assertTrue(host.dismissTop())
        assertEquals(listOf("B"), dismissed)

        assertTrue(host.dismissTop())
        assertEquals(listOf("B", "A"), dismissed)
    }

    @Test
    fun `unregister removes a sheet from the stack without dismissing it`() {
        val host = SideSheetHostState()
        val dismissed = mutableListOf<String>()
        val tokenA = Any()
        val tokenB = Any()

        host.register(tokenA) { dismissed += "A" }
        host.register(tokenB) { dismissed += "B" }
        host.unregister(tokenB)

        assertTrue(host.dismissTop())
        assertEquals(listOf("A"), dismissed)
    }

    @Test
    fun `re-registering the same token keeps a single entry and moves it to the top`() {
        val host = SideSheetHostState()
        val dismissed = mutableListOf<String>()
        val tokenA = Any()
        val tokenB = Any()

        host.register(tokenA) { dismissed += "A" }
        host.register(tokenB) { dismissed += "B" }
        // A recomposition re-registers A with a fresh lambda; it must not duplicate A on the stack.
        host.register(tokenA) { dismissed += "A2" }

        assertTrue(host.dismissTop())
        assertEquals(listOf("A2"), dismissed)
        assertTrue(host.dismissTop())
        assertEquals(listOf("A2", "B"), dismissed)
        assertFalse(host.dismissTop())
    }

    @Test
    fun `dismissTop on an empty stack returns false`() {
        assertFalse(SideSheetHostState().dismissTop())
    }

    @Test
    fun `isAnyOpen reflects the stack`() {
        val host = SideSheetHostState()
        val token = Any()
        assertFalse(host.isAnyOpen)
        host.register(token) {}
        assertTrue(host.isAnyOpen)
        host.unregister(token)
        assertFalse(host.isAnyOpen)
    }

    @Test
    fun `singleton open close isOpen round-trips and is idempotent`() {
        val host = SideSheetHostState()
        assertFalse(host.isOpen(SideSheetId.MockRules))

        host.open(SideSheetId.MockRules)
        host.open(SideSheetId.MockRules) // idempotent, no duplicate state
        assertTrue(host.isOpen(SideSheetId.MockRules))
        assertFalse(host.isOpen(SideSheetId.Journeys))

        host.close(SideSheetId.MockRules)
        assertFalse(host.isOpen(SideSheetId.MockRules))
    }
}
