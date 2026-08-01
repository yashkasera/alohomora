package io.github.yashkasera.alohomora.desktop.data.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlistTest {

    @Test
    fun `encodes the usbmux ListDevices request`() {
        val xml = Plist.encode(
            mapOf(
                "MessageType" to "ListDevices",
                "ProgName" to "Alohomora",
            ),
        ).decodeToString()

        assertTrue(xml.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(xml.contains("<key>MessageType</key><string>ListDevices</string>"))
        assertTrue(xml.contains("<key>ProgName</key><string>Alohomora</string>"))
    }

    @Test
    fun `round trips dict array integer and boolean`() {
        val original = mapOf(
            "name" to "device",
            "id" to 42,
            "usb" to true,
            "network" to false,
            "ports" to listOf(1, 2, 3),
        )
        @Suppress("UNCHECKED_CAST")
        val decoded = Plist.decode(Plist.encode(original)) as Map<String, Any?>

        assertEquals("device", decoded["name"])
        assertEquals(42L, decoded["id"])
        assertEquals(true, decoded["usb"])
        assertEquals(false, decoded["network"])
        assertEquals(listOf(1L, 2L, 3L), decoded["ports"])
    }

    @Test
    fun `parses a real usbmuxd ListDevices reply`() {
        // Captured verbatim from /var/run/usbmuxd, including the apple.com DOCTYPE.
        val reply = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
              <key>DeviceList</key>
              <array>
                <dict>
                  <key>DeviceID</key><integer>7</integer>
                  <key>MessageType</key><string>Attached</string>
                  <key>Properties</key>
                  <dict>
                    <key>ConnectionType</key><string>USB</string>
                    <key>DeviceID</key><integer>7</integer>
                    <key>SerialNumber</key><string>00008140-000E4DA60AC0801C</string>
                  </dict>
                </dict>
              </array>
            </dict>
            </plist>
        """.trimIndent().toByteArray()

        val decoded = Plist.decode(reply) as Map<*, *>
        val devices = decoded["DeviceList"] as List<*>
        val properties = (devices.single() as Map<*, *>)["Properties"] as Map<*, *>

        assertEquals(7L, properties["DeviceID"])
        assertEquals("USB", properties["ConnectionType"])
        assertEquals("00008140-000E4DA60AC0801C", properties["SerialNumber"])
    }

    @Test
    fun `escapes characters that would break the document`() {
        val decoded = Plist.decode(Plist.encode(mapOf("k" to "a & b < c > d"))) as Map<*, *>
        assertEquals("a & b < c > d", decoded["k"])
    }

    @Test
    fun `returns null on malformed input instead of throwing`() {
        // Parsed straight off a socket, so garbage must degrade rather than propagate.
        assertNull(Plist.decode("not a plist at all".toByteArray()))
        assertNull(Plist.decode(ByteArray(0)))
    }

    @Test
    fun `does not resolve the external DTD`() {
        // The DOCTYPE points at apple.com. If external entities were enabled, parsing would
        // depend on network access and be a fetch-on-parse vector.
        val started = System.nanoTime()
        val decoded = Plist.decode(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0"><dict><key>a</key><string>b</string></dict></plist>
            """.trimIndent().toByteArray(),
        ) as Map<*, *>
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000

        assertEquals("b", decoded["a"])
        assertTrue(elapsedMillis < 2_000, "parse took ${elapsedMillis}ms; DTD may be fetched")
    }
}
