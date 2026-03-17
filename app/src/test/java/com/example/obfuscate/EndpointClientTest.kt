package com.example.obfuscate

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class EndpointClientTest {
    @Test
    fun parseTcpEndpoint() {
        val endpoint = EndpointClient.parseEndpoint("tcp:example.com:9000")

        assertEquals(EndpointProtocol.TCP, endpoint.protocol)
        assertEquals("example.com", endpoint.host)
        assertEquals(9000, endpoint.port)
        assertEquals("tcp:example.com:9000", endpoint.toDisplayString())
    }

    @Test
    fun parseIpv6HttpEndpoint() {
        val endpoint = EndpointClient.parseEndpoint("http:[::1]:8080")

        assertEquals(EndpointProtocol.HTTP, endpoint.protocol)
        assertEquals("::1", endpoint.host)
        assertEquals(8080, endpoint.port)
        assertEquals("http:[::1]:8080", endpoint.toDisplayString())
        assertEquals("[::1]", endpoint.toHttpHost())
    }

    @Test
    fun rejectUnsupportedProtocol() {
        try {
            EndpointClient.parseEndpoint("https:example.com:443")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Unsupported protocol. Use tcp, udp, or http.", exception.message)
        }
    }

    @Test
    fun rejectMissingPort() {
        try {
            EndpointClient.parseEndpoint("tcp:example.com")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Endpoint must use protocol:host:port format.", exception.message)
        }
    }
}
