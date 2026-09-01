package io.github.yashkasera.alohomora.desktop.data.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdbParserMdnsTest {

    @Test
    fun `parses pairing and connect endpoints`() {
        val output = """
            List of discovered mdns services
            adb-39121FDJG000GV-ymZFvL	_adb-tls-pairing._tcp.	192.168.1.5:37155
            adb-39121FDJG000GV-ymZFvL	_adb-tls-connect._tcp.	192.168.1.5:42159
        """.trimIndent()

        val services = AdbParser.parseMdnsServices(output)

        assertEquals(2, services.size)
        val pairing = services.single { it.isPairing }
        val connect = services.single { it.isConnect }
        assertEquals("_adb-tls-pairing._tcp", pairing.type)
        assertEquals("192.168.1.5", pairing.host)
        assertEquals(37155, pairing.port)
        assertEquals("192.168.1.5", connect.host)
        assertEquals(42159, connect.port)
    }

    @Test
    fun `header-only or empty output yields no services`() {
        assertTrue(AdbParser.parseMdnsServices("List of discovered mdns services").isEmpty())
        assertTrue(AdbParser.parseMdnsServices("").isEmpty())
    }

    @Test
    fun `malformed lines are skipped`() {
        val output = """
            List of discovered mdns services
            garbage-without-address	_adb-tls-connect._tcp.
            adb-x	_adb-tls-connect._tcp.	10.0.0.9:5555
        """.trimIndent()

        val services = AdbParser.parseMdnsServices(output)

        assertEquals(1, services.size)
        assertEquals(5555, services.single().port)
    }
}
