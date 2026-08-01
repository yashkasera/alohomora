package io.github.yashkasera.alohomora.desktop.data.ios

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Minimal XML property-list codec, just enough for the usbmuxd handshake.
 *
 * usbmuxd accepts and — verified against `/var/run/usbmuxd` on macOS — *replies with* XML
 * plists, so there is no need for a binary-plist library or any third-party dependency. Only
 * the node types usbmuxd actually uses are handled: dict, array, string, integer, true/false
 * and data.
 */
internal object Plist {

    /** Serialises [value] as an XML plist document. */
    fun encode(value: Any?): ByteArray = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append(
            """<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" """ +
                """"http://www.apple.com/DTDs/PropertyList-1.0.dtd">""",
        )
        append("""<plist version="1.0">""")
        appendValue(value)
        append("</plist>")
    }.toByteArray()

    private fun StringBuilder.appendValue(value: Any?) {
        when (value) {
            null -> append("<string></string>")
            is Map<*, *> -> {
                append("<dict>")
                value.forEach { (k, v) ->
                    append("<key>").append(escape(k.toString())).append("</key>")
                    appendValue(v)
                }
                append("</dict>")
            }
            is List<*> -> {
                append("<array>")
                value.forEach { appendValue(it) }
                append("</array>")
            }
            is Boolean -> append(if (value) "<true/>" else "<false/>")
            is Int, is Long -> append("<integer>").append(value.toString()).append("</integer>")
            else -> append("<string>").append(escape(value.toString())).append("</string>")
        }
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    /**
     * Parses an XML plist into Kotlin types: dict → Map, array → List, integer → Long,
     * string → String, true/false → Boolean, data → base64 String.
     *
     * @return the root value, or null when the document cannot be parsed.
     */
    fun decode(bytes: ByteArray): Any? {
        val document = try {
            DocumentBuilderFactory.newInstance().apply {
                // usbmuxd emits a DOCTYPE referencing apple.com. Resolving it would make every
                // request depend on network access, so external entities are disabled — which
                // is also the correct hardening posture for parsing bytes off a socket.
                isNamespaceAware = false
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            return null
        }
        val plist = document.documentElement ?: return null
        val root = plist.childElements().firstOrNull() ?: return null
        return parseValue(root)
    }

    private fun parseValue(element: Element): Any? = when (element.tagName) {
        "dict" -> {
            val map = LinkedHashMap<String, Any?>()
            val children = element.childElements()
            var index = 0
            while (index + 1 < children.size) {
                val keyNode = children[index]
                if (keyNode.tagName == "key") {
                    map[keyNode.textContent] = parseValue(children[index + 1])
                    index += 2
                } else {
                    index += 1
                }
            }
            map
        }
        "array" -> element.childElements().map { parseValue(it) }
        "integer" -> element.textContent.trim().toLongOrNull()
        "real" -> element.textContent.trim().toDoubleOrNull()
        "true" -> true
        "false" -> false
        "string", "data" -> element.textContent
        else -> element.textContent
    }

    private fun Element.childElements(): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it) }
            .filter { it.nodeType == Node.ELEMENT_NODE }
            .map { it as Element }
    }
}
