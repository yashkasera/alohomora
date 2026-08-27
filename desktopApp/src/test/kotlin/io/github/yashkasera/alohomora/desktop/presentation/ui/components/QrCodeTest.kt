package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import kotlin.test.Test
import kotlin.test.assertTrue

class QrCodeTest {

    @Test
    fun `encodes a non-empty square module grid`() {
        val modules = encodeQrModules("WIFI:T:ADB;S:alohomora-abc;P:secret123;;")

        assertTrue(modules.isNotEmpty(), "expected a module grid")
        assertTrue(modules.all { it.size == modules.size }, "grid must be square")
        assertTrue(modules.any { row -> row.any { it } }, "expected some dark modules")
    }

    @Test
    fun `different content yields a different grid`() {
        val a = encodeQrModules("WIFI:T:ADB;S:one;P:aaaa;;")
        val b = encodeQrModules("WIFI:T:ADB;S:two;P:bbbb;;")

        val differs = a.size != b.size ||
            a.indices.any { y -> !a[y].contentEquals(b[y]) }
        assertTrue(differs, "distinct payloads should not encode identically")
    }
}
