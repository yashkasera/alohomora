package io.github.yashkasera.alohomora.devtools

import kotlin.test.Test
import kotlin.test.assertEquals

class DevToolsStreamAdapterTest {
    @Test
    fun filterNewReturnsOnlyNewItemsInOrder() {
        val adapter = DevToolsStreamAdapter<Pair<String, Long>> { it.second }
        val first = listOf("a" to 1L, "b" to 2L)
        val second = listOf("b" to 2L, "c" to 3L, "d" to 4L)

        val firstNew = adapter.filterNew(first)
        val secondNew = adapter.filterNew(second)

        assertEquals(listOf("a" to 1L, "b" to 2L), firstNew)
        assertEquals(listOf("c" to 3L, "d" to 4L), secondNew)
    }
}
