package com.locksite.app.dns

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Lets the VPN exclude a socket from its own tunnel. Implemented by the service with
 * `VpnService.protect`; absent in unit tests.
 */
fun interface SocketProtector {
    fun protect(socket: DatagramSocket): Boolean
}

/**
 * Forwards one DNS query to an upstream resolver over UDP and returns its answer.
 *
 * A fresh socket is used per query. That costs one file descriptor and a few
 * microseconds, and in exchange there is no shared mutable state, no lock contention and
 * no chance of one slow lookup corrupting another. Query volume on a phone is tiny.
 *
 * IPv4 literals only: the configured upstreams are fixed addresses, so no resolution is
 * needed and nothing here can recurse into the filter itself.
 */
class UdpDnsForwarder(
    private val upstreams: List<InetAddress>,
    private val protector: SocketProtector? = null,
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MS,
    private val port: Int = DEFAULT_DNS_PORT,
) {

    /** @return the raw response, or `null` when every upstream failed or timed out. */
    fun forward(query: ByteArray): ByteArray? {
        if (query.size < DnsMessages.HEADER_SIZE) return null
        for (upstream in upstreams) {
            val answer = exchange(upstream, query)
            if (answer != null) return answer
        }
        return null
    }

    private fun exchange(upstream: InetAddress, query: ByteArray): ByteArray? {
        val socket = DatagramSocket()
        try {
            // Keep the socket outside our own tunnel, otherwise the query would be
            // captured by the interface we are trying to answer from.
            protector?.protect(socket)
            // Connecting makes the kernel discard datagrams from anyone but this peer,
            // which removes the need to validate the sender ourselves.
            socket.connect(upstream, port)
            socket.send(DatagramPacket(query, query.size))

            val buffer = ByteArray(MAX_RESPONSE_BYTES)
            val queryId = readU16(query, 0)
            val deadline = System.nanoTime() + timeoutMillis * NANOS_PER_MILLI

            while (true) {
                val remainingMillis = (deadline - System.nanoTime()) / NANOS_PER_MILLI
                if (remainingMillis <= 0) return null
                socket.soTimeout = remainingMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

                val reply = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(reply)
                } catch (timeout: SocketTimeoutException) {
                    return null
                }

                // Ignore stray datagrams: too short, wrong transaction ID, or not a reply.
                if (reply.length < DnsMessages.HEADER_SIZE) continue
                if (readU16(buffer, 0) != queryId) continue
                if ((readU16(buffer, 2) and DnsMessages.FLAG_QR) == 0) continue

                return buffer.copyOf(reply.length)
            }
        } catch (io: IOException) {
            return null
        } finally {
            socket.close()
        }
    }

    companion object {
        const val DEFAULT_DNS_PORT = 53
        const val DEFAULT_TIMEOUT_MS = 3000

        /** Large enough for any EDNS0 answer a phone will ask for. */
        private const val MAX_RESPONSE_BYTES = 4096
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
