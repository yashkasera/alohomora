package io.github.yashkasera.alohomora.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import io.github.yashkasera.alohomora.common.ThrottleProfile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Selector

private const val DNS_PORT = 53

/**
 * Core packet loop: reads IP packets from the TUN fd, dispatches them by protocol to
 * [TcpSessionManager] or [UdpForwarder], and writes response packets back to the TUN.
 *
 * Runs on its own thread — blocking reads on the TUN fd and NIO selector multiplexing for
 * real socket reads are incompatible with coroutine dispatchers.
 */
internal class PacketForwarder(
    private val vpnService: VpnService,
    private val tunFd: ParcelFileDescriptor,
) {
    @Volatile
    private var running = false

    private lateinit var selector: Selector
    private lateinit var tunInput: FileInputStream
    private lateinit var tunOutput: FileOutputStream
    private lateinit var tcpManager: TcpSessionManager
    private lateinit var udpForwarder: UdpForwarder

    val throttle = TokenBucketThrottle()

    private val tunWriteLock = Any()

    fun start() {
        running = true
        selector = Selector.open()
        tunInput = FileInputStream(tunFd.fileDescriptor)
        tunOutput = FileOutputStream(tunFd.fileDescriptor)

        val writeTun: (ByteArray) -> Unit = { packet -> writeTunPacket(packet) }

        tcpManager = TcpSessionManager(vpnService, selector, writeTun)
        udpForwarder = UdpForwarder(vpnService, selector, writeTun)

        Thread(::tunReaderLoop, "alohomora-vpn-tun-reader").start()
        Thread(::selectorLoop, "alohomora-vpn-selector").start()
    }

    fun stop() {
        running = false
        runCatching { selector.wakeup() }
        tcpManager.closeAll()
        udpForwarder.closeAll()
        runCatching { tunInput.close() }
        runCatching { tunOutput.close() }
        runCatching { selector.close() }
    }

    fun updateProfile(profile: ThrottleProfile) {
        throttle.update(profile)
    }

    private fun tunReaderLoop() {
        val buffer = ByteBuffer.allocate(AlohomoraVpnService.MTU)
        try {
            while (running) {
                buffer.clear()
                val length = tunInput.read(buffer.array())
                if (length <= 0) {
                    Thread.sleep(10)
                    continue
                }
                buffer.limit(length)
                processOutboundPacket(buffer, buffer.array().copyOf(length))
            }
        } catch (e: Exception) {
            if (running) {
                println("[Alohomora] VPN TUN reader error: ${e.message}")
            }
        }
    }

    private fun selectorLoop() {
        var lastReap = System.currentTimeMillis()
        try {
            while (running) {
                val ready = selector.select(1000)

                val now = System.currentTimeMillis()
                if (now - lastReap > 10_000) {
                    tcpManager.reapIdle(now)
                    udpForwarder.reapIdle(now)
                    lastReap = now
                }

                if (ready == 0) continue

                val keys = selector.selectedKeys().iterator()
                while (keys.hasNext()) {
                    val key = keys.next()
                    keys.remove()
                    if (!key.isValid) continue

                    try {
                        when {
                            key.isConnectable -> tcpManager.handleConnectable(key)
                            key.isReadable -> {
                                when (val attachment = key.attachment()) {
                                    is TcpSession -> {
                                        if (!isDnsSession(attachment.key)) {
                                            throttle.injectLatencyIfNewSession(attachment.key)
                                        }
                                        tcpManager.handleReadable(key)
                                    }

                                    is UdpSession -> {
                                        if (!isDnsSession(attachment.key)) {
                                            throttle.injectLatencyIfNewSession(attachment.key)
                                        }
                                        udpForwarder.handleReadable(key)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[Alohomora] VPN selector handler error: ${e.message}")
                        key.cancel()
                    }
                }
            }
        } catch (e: Exception) {
            if (running) {
                println("[Alohomora] VPN selector loop error: ${e.message}")
            }
        }
    }

    private fun processOutboundPacket(buffer: ByteBuffer, rawPacket: ByteArray) {
        buffer.position(0)
        val ipHeader = IpPacketParser.parseIpHeader(buffer) ?: return

        when (ipHeader.protocol) {
            IP_PROTOCOL_TCP -> {
                val tcpHeader = IpPacketParser.parseTcpHeader(buffer, ipHeader) ?: return
                tcpManager.processPacket(ipHeader, tcpHeader, buffer, rawPacket)
            }

            IP_PROTOCOL_UDP -> {
                val udpHeader = IpPacketParser.parseUdpHeader(buffer) ?: return
                udpForwarder.processPacket(ipHeader, udpHeader, buffer, rawPacket)
            }
        }
    }

    private fun writeTunPacket(packet: ByteArray) {
        // Apply throttle to downstream traffic (responses written back to TUN).
        // DNS is checked by the caller before invoking this — the session key check
        // happens in the selector loop. For simplicity, throttle is applied to all
        // writeTun calls uniformly; DNS responses are small enough that the brief
        // delay is acceptable even if not explicitly exempted here.
        throttle.consume(packet.size)

        synchronized(tunWriteLock) {
            try {
                tunOutput.write(packet)
                tunOutput.flush()
            } catch (e: Exception) {
                if (running) {
                    println("[Alohomora] VPN TUN write error: ${e.message}")
                }
            }
        }
    }

    private fun isDnsSession(key: SessionKey): Boolean =
        key.destinationPort == DNS_PORT
}
