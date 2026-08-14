package io.github.yashkasera.alohomora.vpn

import android.net.VpnService
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

internal class TcpSessionManager(
    private val vpnService: VpnService,
    private val selector: Selector,
    private val writeTun: (ByteArray) -> Unit,
) {
    private val sessions = HashMap<SessionKey, TcpSession>()

    fun processPacket(
        ipHeader: IpHeader,
        tcpHeader: TcpHeader,
        packet: ByteBuffer,
        packetBytes: ByteArray,
    ) {
        val key = SessionKey(
            protocol = IP_PROTOCOL_TCP,
            sourceAddress = ipHeader.sourceAddress,
            sourcePort = tcpHeader.sourcePort,
            destinationAddress = ipHeader.destinationAddress,
            destinationPort = tcpHeader.destinationPort,
        )

        when {
            tcpHeader.isRst -> {
                closeSession(key)
            }

            tcpHeader.isSyn && !tcpHeader.isAck -> {
                handleSyn(key, ipHeader, tcpHeader)
            }

            else -> {
                val session = sessions[key] ?: return
                session.lastActive = System.currentTimeMillis()

                if (tcpHeader.isFin) {
                    handleFin(key, session, tcpHeader)
                    return
                }

                if (tcpHeader.isAck && session.state == TcpSessionState.SYN_ACK_SENT) {
                    session.state = TcpSessionState.ESTABLISHED
                }

                val payloadLength =
                    ipHeader.totalLength - ipHeader.headerLength - tcpHeader.dataOffset
                if (payloadLength > 0 && session.state == TcpSessionState.ESTABLISHED) {
                    handleData(key, session, ipHeader, tcpHeader, packet, payloadLength)
                }
            }
        }
    }

    private fun handleSyn(key: SessionKey, ipHeader: IpHeader, tcpHeader: TcpHeader) {
        closeSession(key)

        try {
            val channel = SocketChannel.open()
            vpnService.protect(channel.socket())
            channel.configureBlocking(false)
            channel.connect(InetSocketAddress(key.destinationAddress, key.destinationPort))

            val session = TcpSession(
                key = key,
                channel = channel,
                clientSeq = tcpHeader.sequenceNumber,
                serverSeq = 0L,
            )
            session.state = TcpSessionState.SYN_RECEIVED

            channel.register(selector, SelectionKey.OP_CONNECT or SelectionKey.OP_READ)
            channel.keyFor(selector)?.attach(session)

            sessions[key] = session
        } catch (e: Exception) {
            println("[Alohomora] VPN TCP connect failed to ${key.destinationAddress}:${key.destinationPort}: ${e.message}")
            sendRst(key, tcpHeader)
        }
    }

    fun handleConnectable(selectionKey: SelectionKey) {
        val session = selectionKey.attachment() as? TcpSession ?: return
        try {
            if (session.channel.finishConnect()) {
                selectionKey.interestOps(SelectionKey.OP_READ)

                session.serverSeq = 1000L
                val synAck = IpPacketParser.buildTcpPacket(
                    sourceAddress = session.key.destinationAddress,
                    destinationAddress = session.key.sourceAddress,
                    sourcePort = session.key.destinationPort,
                    destinationPort = session.key.sourcePort,
                    sequenceNumber = session.serverSeq,
                    acknowledgmentNumber = session.clientSeq + 1,
                    flags = TcpHeader.FLAG_SYN or TcpHeader.FLAG_ACK,
                )
                writeTun(synAck)
                session.serverSeq += 1
                session.clientSeq += 1
                session.state = TcpSessionState.SYN_ACK_SENT
            }
        } catch (e: Exception) {
            println("[Alohomora] VPN TCP finishConnect failed: ${e.message}")
            sendRst(session.key, null)
            closeSession(session.key)
        }
    }

    fun handleReadable(selectionKey: SelectionKey) {
        val session = selectionKey.attachment() as? TcpSession ?: return
        val buffer = ByteBuffer.allocate(AlohomoraVpnService.MTU - 40)
        try {
            val bytesRead = session.channel.read(buffer)
            if (bytesRead == -1) {
                sendFin(session)
                closeSession(session.key)
                return
            }
            if (bytesRead == 0) return

            buffer.flip()
            val payload = ByteArray(bytesRead)
            buffer.get(payload)

            val dataPacket = IpPacketParser.buildTcpPacket(
                sourceAddress = session.key.destinationAddress,
                destinationAddress = session.key.sourceAddress,
                sourcePort = session.key.destinationPort,
                destinationPort = session.key.sourcePort,
                sequenceNumber = session.serverSeq,
                acknowledgmentNumber = session.clientSeq,
                flags = TcpHeader.FLAG_ACK or TcpHeader.FLAG_PSH,
                payload = payload,
            )
            writeTun(dataPacket)
            session.serverSeq += bytesRead
            session.lastActive = System.currentTimeMillis()
        } catch (e: Exception) {
            println("[Alohomora] VPN TCP read failed: ${e.message}")
            closeSession(session.key)
        }
    }

    private fun handleData(
        key: SessionKey,
        session: TcpSession,
        ipHeader: IpHeader,
        tcpHeader: TcpHeader,
        packet: ByteBuffer,
        payloadLength: Int,
    ) {
        val payload = ByteArray(payloadLength)
        packet.position(tcpHeader.payloadOffset)
        packet.get(payload, 0, minOf(payloadLength, packet.remaining()))

        try {
            session.channel.write(ByteBuffer.wrap(payload))
            session.clientSeq += payloadLength

            val ack = IpPacketParser.buildTcpPacket(
                sourceAddress = key.destinationAddress,
                destinationAddress = key.sourceAddress,
                sourcePort = key.destinationPort,
                destinationPort = key.sourcePort,
                sequenceNumber = session.serverSeq,
                acknowledgmentNumber = session.clientSeq,
                flags = TcpHeader.FLAG_ACK,
            )
            writeTun(ack)
        } catch (e: Exception) {
            println("[Alohomora] VPN TCP write failed: ${e.message}")
            closeSession(key)
        }
    }

    private fun handleFin(key: SessionKey, session: TcpSession, tcpHeader: TcpHeader) {
        session.clientSeq += 1

        val finAck = IpPacketParser.buildTcpPacket(
            sourceAddress = key.destinationAddress,
            destinationAddress = key.sourceAddress,
            sourcePort = key.destinationPort,
            destinationPort = key.sourcePort,
            sequenceNumber = session.serverSeq,
            acknowledgmentNumber = session.clientSeq,
            flags = TcpHeader.FLAG_FIN or TcpHeader.FLAG_ACK,
        )
        writeTun(finAck)
        session.serverSeq += 1
        closeSession(key)
    }

    private fun sendFin(session: TcpSession) {
        val fin = IpPacketParser.buildTcpPacket(
            sourceAddress = session.key.destinationAddress,
            destinationAddress = session.key.sourceAddress,
            sourcePort = session.key.destinationPort,
            destinationPort = session.key.sourcePort,
            sequenceNumber = session.serverSeq,
            acknowledgmentNumber = session.clientSeq,
            flags = TcpHeader.FLAG_FIN or TcpHeader.FLAG_ACK,
        )
        writeTun(fin)
        session.serverSeq += 1
    }

    private fun sendRst(key: SessionKey, tcpHeader: TcpHeader?) {
        val rst = IpPacketParser.buildTcpPacket(
            sourceAddress = key.destinationAddress,
            destinationAddress = key.sourceAddress,
            sourcePort = key.destinationPort,
            destinationPort = key.sourcePort,
            sequenceNumber = 0,
            acknowledgmentNumber = (tcpHeader?.sequenceNumber ?: 0) + 1,
            flags = TcpHeader.FLAG_RST or TcpHeader.FLAG_ACK,
        )
        writeTun(rst)
    }

    fun reapIdle(now: Long, timeoutMs: Long = 60_000) {
        val expired = sessions.entries.filter { now - it.value.lastActive > timeoutMs }
        expired.forEach { (key, _) -> closeSession(key) }
    }

    fun closeAll() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }

    private fun closeSession(key: SessionKey) {
        sessions.remove(key)?.close()
    }
}

internal enum class TcpSessionState {
    SYN_RECEIVED,
    SYN_ACK_SENT,
    ESTABLISHED,
}

internal class TcpSession(
    val key: SessionKey,
    val channel: SocketChannel,
    var clientSeq: Long,
    var serverSeq: Long,
) {
    var state: TcpSessionState = TcpSessionState.SYN_RECEIVED
    var lastActive: Long = System.currentTimeMillis()

    fun close() {
        runCatching { channel.close() }
    }
}
