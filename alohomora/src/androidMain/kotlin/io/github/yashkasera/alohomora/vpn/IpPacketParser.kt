package io.github.yashkasera.alohomora.vpn

import java.net.InetAddress
import java.nio.ByteBuffer

internal const val IP_PROTOCOL_TCP = 6
internal const val IP_PROTOCOL_UDP = 17

internal data class IpHeader(
    val version: Int,
    val headerLength: Int,
    val totalLength: Int,
    val protocol: Int,
    val sourceAddress: InetAddress,
    val destinationAddress: InetAddress,
)

internal data class TcpHeader(
    val sourcePort: Int,
    val destinationPort: Int,
    val sequenceNumber: Long,
    val acknowledgmentNumber: Long,
    val dataOffset: Int,
    val flags: Int,
    val windowSize: Int,
    val payloadOffset: Int,
) {
    val isSyn: Boolean get() = flags and FLAG_SYN != 0
    val isAck: Boolean get() = flags and FLAG_ACK != 0
    val isFin: Boolean get() = flags and FLAG_FIN != 0
    val isRst: Boolean get() = flags and FLAG_RST != 0

    companion object {
        const val FLAG_FIN = 0x01
        const val FLAG_SYN = 0x02
        const val FLAG_RST = 0x04
        const val FLAG_PSH = 0x08
        const val FLAG_ACK = 0x10
    }
}

internal data class UdpHeader(
    val sourcePort: Int,
    val destinationPort: Int,
    val length: Int,
    val payloadOffset: Int,
)

internal data class SessionKey(
    val protocol: Int,
    val sourceAddress: InetAddress,
    val sourcePort: Int,
    val destinationAddress: InetAddress,
    val destinationPort: Int,
)

internal object IpPacketParser {

    fun parseIpHeader(buffer: ByteBuffer): IpHeader? {
        if (buffer.remaining() < 20) return null
        val start = buffer.position()

        val versionAndIhl = buffer.get().toInt() and 0xFF
        val version = versionAndIhl ushr 4
        if (version != 4) return null

        val ihl = versionAndIhl and 0x0F
        val headerLength = ihl * 4
        if (buffer.remaining() + 1 < headerLength) return null

        buffer.position(start + 2)
        val totalLength = buffer.short.toInt() and 0xFFFF

        buffer.position(start + 9)
        val protocol = buffer.get().toInt() and 0xFF

        buffer.position(start + 12)
        val srcBytes = ByteArray(4)
        buffer.get(srcBytes)
        val dstBytes = ByteArray(4)
        buffer.get(dstBytes)

        buffer.position(start + headerLength)

        return IpHeader(
            version = version,
            headerLength = headerLength,
            totalLength = totalLength,
            protocol = protocol,
            sourceAddress = InetAddress.getByAddress(srcBytes),
            destinationAddress = InetAddress.getByAddress(dstBytes),
        )
    }

    fun parseTcpHeader(buffer: ByteBuffer, ipHeader: IpHeader): TcpHeader? {
        val tcpStart = buffer.position()
        if (buffer.remaining() < 20) return null

        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destinationPort = buffer.short.toInt() and 0xFFFF
        val sequenceNumber = buffer.int.toLong() and 0xFFFFFFFFL
        val acknowledgmentNumber = buffer.int.toLong() and 0xFFFFFFFFL
        val dataOffsetAndFlags = buffer.short.toInt() and 0xFFFF
        val dataOffset = (dataOffsetAndFlags ushr 12) * 4
        val flags = dataOffsetAndFlags and 0x3F
        val windowSize = buffer.short.toInt() and 0xFFFF

        val payloadOffset = tcpStart + dataOffset

        return TcpHeader(
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            sequenceNumber = sequenceNumber,
            acknowledgmentNumber = acknowledgmentNumber,
            dataOffset = dataOffset,
            flags = flags,
            windowSize = windowSize,
            payloadOffset = payloadOffset,
        )
    }

    fun parseUdpHeader(buffer: ByteBuffer): UdpHeader? {
        val udpStart = buffer.position()
        if (buffer.remaining() < 8) return null

        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destinationPort = buffer.short.toInt() and 0xFFFF
        val length = buffer.short.toInt() and 0xFFFF

        return UdpHeader(
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            length = length,
            payloadOffset = udpStart + 8,
        )
    }

    fun buildTcpPacket(
        sourceAddress: InetAddress,
        destinationAddress: InetAddress,
        sourcePort: Int,
        destinationPort: Int,
        sequenceNumber: Long,
        acknowledgmentNumber: Long,
        flags: Int,
        payload: ByteArray = ByteArray(0),
    ): ByteArray {
        val ipHeaderLen = 20
        val tcpHeaderLen = 20
        val totalLength = ipHeaderLen + tcpHeaderLen + payload.size

        val packet = ByteBuffer.allocate(totalLength)

        // IP header
        packet.put((0x45).toByte()) // version=4, IHL=5
        packet.put(0.toByte()) // DSCP/ECN
        packet.putShort(totalLength.toShort())
        packet.putShort(0) // identification
        packet.putShort(0x4000.toShort()) // flags: Don't Fragment
        packet.put(64.toByte()) // TTL
        packet.put(IP_PROTOCOL_TCP.toByte())
        packet.putShort(0) // checksum placeholder
        packet.put(sourceAddress.address)
        packet.put(destinationAddress.address)

        // TCP header
        val tcpStart = packet.position()
        packet.putShort(sourcePort.toShort())
        packet.putShort(destinationPort.toShort())
        packet.putInt(sequenceNumber.toInt())
        packet.putInt(acknowledgmentNumber.toInt())
        val dataOffsetAndFlags = (5 shl 12) or flags
        packet.putShort(dataOffsetAndFlags.toShort())
        packet.putShort(65535.toShort()) // window size
        packet.putShort(0) // checksum placeholder
        packet.putShort(0) // urgent pointer

        if (payload.isNotEmpty()) {
            packet.put(payload)
        }

        // TCP checksum
        val tcpChecksum = computeTcpChecksum(
            packet.array(), sourceAddress, destinationAddress,
            tcpStart, tcpHeaderLen + payload.size,
        )
        packet.putShort(tcpStart + 16, tcpChecksum)

        // IP checksum
        val ipChecksum = computeIpChecksum(packet.array(), 0, ipHeaderLen)
        packet.putShort(10, ipChecksum)

        return packet.array()
    }

    fun buildUdpPacket(
        sourceAddress: InetAddress,
        destinationAddress: InetAddress,
        sourcePort: Int,
        destinationPort: Int,
        payload: ByteArray,
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val udpLength = udpHeaderLen + payload.size
        val totalLength = ipHeaderLen + udpLength

        val packet = ByteBuffer.allocate(totalLength)

        // IP header
        packet.put((0x45).toByte())
        packet.put(0.toByte())
        packet.putShort(totalLength.toShort())
        packet.putShort(0)
        packet.putShort(0x4000.toShort())
        packet.put(64.toByte())
        packet.put(IP_PROTOCOL_UDP.toByte())
        packet.putShort(0)
        packet.put(sourceAddress.address)
        packet.put(destinationAddress.address)

        // UDP header
        val udpStart = packet.position()
        packet.putShort(sourcePort.toShort())
        packet.putShort(destinationPort.toShort())
        packet.putShort(udpLength.toShort())
        packet.putShort(0) // checksum placeholder

        packet.put(payload)

        // UDP checksum
        val udpChecksum = computeUdpChecksum(
            packet.array(), sourceAddress, destinationAddress,
            udpStart, udpLength,
        )
        packet.putShort(udpStart + 6, udpChecksum)

        // IP checksum
        val ipChecksum = computeIpChecksum(packet.array(), 0, ipHeaderLen)
        packet.putShort(10, ipChecksum)

        return packet.array()
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Short {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (length % 2 != 0) {
            sum += (data[end - 1].toInt() and 0xFF) shl 8
        }
        while (sum ushr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }

    private fun computeTcpChecksum(
        data: ByteArray,
        src: InetAddress,
        dst: InetAddress,
        tcpOffset: Int,
        tcpLength: Int,
    ): Short = computeTransportChecksum(data, src, dst, IP_PROTOCOL_TCP, tcpOffset, tcpLength)

    private fun computeUdpChecksum(
        data: ByteArray,
        src: InetAddress,
        dst: InetAddress,
        udpOffset: Int,
        udpLength: Int,
    ): Short = computeTransportChecksum(data, src, dst, IP_PROTOCOL_UDP, udpOffset, udpLength)

    private fun computeTransportChecksum(
        data: ByteArray,
        src: InetAddress,
        dst: InetAddress,
        protocol: Int,
        offset: Int,
        length: Int,
    ): Short {
        var sum = 0L
        // Pseudo-header
        val srcAddr = src.address
        val dstAddr = dst.address
        sum += ((srcAddr[0].toInt() and 0xFF) shl 8) or (srcAddr[1].toInt() and 0xFF)
        sum += ((srcAddr[2].toInt() and 0xFF) shl 8) or (srcAddr[3].toInt() and 0xFF)
        sum += ((dstAddr[0].toInt() and 0xFF) shl 8) or (dstAddr[1].toInt() and 0xFF)
        sum += ((dstAddr[2].toInt() and 0xFF) shl 8) or (dstAddr[3].toInt() and 0xFF)
        sum += protocol.toLong()
        sum += length.toLong()

        // Transport header + payload
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (length % 2 != 0) {
            sum += (data[end - 1].toInt() and 0xFF) shl 8
        }
        while (sum ushr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }
}
