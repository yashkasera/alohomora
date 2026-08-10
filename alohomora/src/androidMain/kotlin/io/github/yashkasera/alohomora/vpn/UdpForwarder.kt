package io.github.yashkasera.alohomora.vpn

import android.net.VpnService
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

internal class UdpForwarder(
    private val vpnService: VpnService,
    private val selector: Selector,
    private val writeTun: (ByteArray) -> Unit,
) {
    private val sessions = HashMap<SessionKey, UdpSession>()

    fun processPacket(
        ipHeader: IpHeader,
        udpHeader: UdpHeader,
        packet: ByteBuffer,
        packetBytes: ByteArray,
    ) {
        val key = SessionKey(
            protocol = IP_PROTOCOL_UDP,
            sourceAddress = ipHeader.sourceAddress,
            sourcePort = udpHeader.sourcePort,
            destinationAddress = ipHeader.destinationAddress,
            destinationPort = udpHeader.destinationPort,
        )

        val session = sessions[key] ?: (openSession(key)?.also { sessions[key] = it }) ?: return

        val payloadSize = udpHeader.length - 8
        if (payloadSize <= 0) return

        val payload = ByteArray(payloadSize)
        packet.position(udpHeader.payloadOffset)
        packet.get(payload, 0, minOf(payloadSize, packet.remaining()))

        try {
            session.channel.write(ByteBuffer.wrap(payload))
            session.lastActive = System.currentTimeMillis()
        } catch (e: Exception) {
            println("[Alohomora] VPN UDP send failed: ${e.message}")
            closeSession(key)
        }
    }

    fun handleReadable(selectionKey: SelectionKey) {
        val session = selectionKey.attachment() as? UdpSession ?: return
        val buffer = ByteBuffer.allocate(AlohomoraVpnService.MTU - 28)
        try {
            val bytesRead = session.channel.read(buffer)
            if (bytesRead <= 0) return

            buffer.flip()
            val payload = ByteArray(bytesRead)
            buffer.get(payload)

            val responsePacket = IpPacketParser.buildUdpPacket(
                sourceAddress = session.key.destinationAddress,
                destinationAddress = session.key.sourceAddress,
                sourcePort = session.key.destinationPort,
                destinationPort = session.key.sourcePort,
                payload = payload,
            )
            writeTun(responsePacket)
            session.lastActive = System.currentTimeMillis()
        } catch (e: Exception) {
            println("[Alohomora] VPN UDP read failed: ${e.message}")
            closeSession(session.key)
        }
    }

    fun reapIdle(now: Long, timeoutMs: Long = 30_000) {
        val expired = sessions.entries.filter { now - it.value.lastActive > timeoutMs }
        expired.forEach { (key, session) ->
            session.close()
            sessions.remove(key)
        }
    }

    fun closeAll() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }

    private fun openSession(key: SessionKey): UdpSession? {
        return try {
            val channel = DatagramChannel.open()
            vpnService.protect(channel.socket())
            channel.configureBlocking(false)
            channel.connect(InetSocketAddress(key.destinationAddress, key.destinationPort))
            channel.register(selector, SelectionKey.OP_READ)

            val session = UdpSession(key, channel)
            channel.keyFor(selector)?.attach(session)
            session
        } catch (e: Exception) {
            println("[Alohomora] VPN UDP open failed to ${key.destinationAddress}:${key.destinationPort}: ${e.message}")
            null
        }
    }

    private fun closeSession(key: SessionKey) {
        sessions.remove(key)?.close()
    }
}

internal class UdpSession(
    val key: SessionKey,
    val channel: DatagramChannel,
) {
    var lastActive: Long = System.currentTimeMillis()

    fun close() {
        runCatching { channel.close() }
    }
}
