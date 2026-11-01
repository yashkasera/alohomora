package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import kotlin.test.Test
import kotlin.test.assertEquals

class DeepLinkBuilderUrlTest {

    @Test
    fun `typed query values are percent-encoded`() {
        assertEquals("hello%20world", encodeQueryComponent("hello world", keepEquals = true))
        assertEquals("a%26b%23c", encodeQueryComponent("a&b#c", keepEquals = true))
        assertEquals("caf%C3%A9", encodeQueryComponent("café", keepEquals = true))
        assertEquals("100%25", encodeQueryComponent("100%", keepEquals = true))
    }

    @Test
    fun `already-encoded input is not encoded twice`() {
        assertEquals("hello%20world", encodeQueryComponent("hello%20world", keepEquals = true))
        assertEquals("a%2Fb%2fc", encodeQueryComponent("a%2Fb%2fc", keepEquals = true))
        assertEquals("a+b", encodeQueryComponent("a+b", keepEquals = true))
    }

    @Test
    fun `equals is kept in values and encoded in keys`() {
        assertEquals("b=c", encodeQueryComponent("b=c", keepEquals = true))
        assertEquals("k%3Dx", encodeQueryComponent("k=x", keepEquals = false))
    }

    @Test
    fun `buildUrl keeps every query param separate`() {
        val url = buildUrl(
            scheme = "myapp",
            host = "open",
            port = "",
            path = "/item",
            queryParams = listOf(QueryParam("q", "fish & chips"), QueryParam("page", "2")),
            fragment = "",
        )
        assertEquals("myapp://open/item?q=fish%20%26%20chips&page=2", url)
    }
}
