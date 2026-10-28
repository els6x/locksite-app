package com.locksite.app.dns

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exercises the forwarder against a real UDP socket on loopback.
 *
 * This is the closest thing to a network test that runs without a device: it proves the
 * send/receive path, the ID validation and the timeout handling.
 */
class UdpDnsForwarderTest {

    private val loopback: InetAddress = InetAddress.getByName("127.0.0.1")

    @Test
    fun `returns the upstream answer`() {
        val query = TestWire.query("example.com", id = 0x2222)
        val expected = responseFor(query)

        withServer(reply = { expected }) { port ->
            val result = UdpDnsForwarder(listOf(loopback), port = port, timeoutMillis = 2000)
                .forward(query)

            assertNotNull(result)
            assertArrayEquals(expected, result)
        }
    }

    @Test
    fun `ignores an answer with the wrong transaction id`() {
        val query = TestWire.query("example.com", id = 0x2222)
        val wrongId = responseFor(query).also { TestWire.putU16(it, 0, 0x9999) }

        withServer(reply = { wrongId }) { port ->
            val result = UdpDnsForwarder(listOf(loopback), port = port, timeoutMillis = 300)
                .forward(query)

            assertNull(result)
        }
    }

    @Test
    fun `ignores an answer that is not marked as a response`() {
        val query = TestWire.query("example.com", id = 0x2222)
        val notAResponse = responseFor(query).also { TestWire.putU16(it, 2, 0x0100) }

        withServer(reply = { notAResponse }) { port ->
            val result = UdpDnsForwarder(listOf(loopback), port = port, timeoutMillis = 300)
                .forward(query)

            assertNull(result)
        }
    }

    @Test
    fun `times out when the upstream stays silent`() {
        val query = TestWire.query("example.com")

        withServer(reply = null) { port ->
            val result = UdpDnsForwarder(listOf(loopback), port = port, timeoutMillis = 200)
                .forward(query)

            assertNull(result)
        }
    }

    @Test
    fun `returns null without any upstream`() {
        assertNull(UdpDnsForwarder(emptyList()).forward(TestWire.query("example.com")))
    }

    @Test
    fun `refuses a query shorter than a dns header`() {
        assertNull(UdpDnsForwarder(listOf(loopback)).forward(ByteArray(4)))
    }

    private fun responseFor(query: ByteArray): ByteArray {
        val answer = ByteArray(query.size + 16)
        query.copyInto(answer, 0, 0, query.size)
        TestWire.putU16(answer, 2, 0x8180)                  // QR + RD + RA + NOERROR
        TestWire.putU16(answer, 6, 1)                       // ANCOUNT

        val offset = query.size
        answer[offset] = 0xC0.toByte()                      // pointer to the question name
        answer[offset + 1] = 0x0C
        TestWire.putU16(answer, offset + 2, 1)              // TYPE A
        TestWire.putU16(answer, offset + 4, 1)              // CLASS IN
        TestWire.putU16(answer, offset + 10, 4)             // RDLENGTH
        answer[offset + 12] = 93.toByte()
        answer[offset + 13] = 184.toByte()
        answer[offset + 14] = 216.toByte()
        answer[offset + 15] = 34.toByte()
        return answer
    }

    /**
     * Runs [block] against a loopback UDP server. [reply] returning `null` means the
     * server deliberately never answers.
     */
    private fun withServer(reply: (() -> ByteArray)?, block: (Int) -> Unit) {
        val server = DatagramSocket(0, loopback)
        val worker = Thread {
            runCatching {
                val buffer = ByteArray(1024)
                val incoming = DatagramPacket(buffer, buffer.size)
                server.receive(incoming)
                val payload = reply?.invoke() ?: return@Thread
                server.send(
                    DatagramPacket(payload, payload.size, incoming.address, incoming.port),
                )
            }
        }
        worker.isDaemon = true
        worker.start()

        try {
            block(server.localPort)
        } finally {
            server.close()
            worker.join(2000)
        }
    }
}
