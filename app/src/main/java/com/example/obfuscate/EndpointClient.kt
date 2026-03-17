package com.example.obfuscate

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL

private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 15_000
private const val TCP_IDLE_TIMEOUT_MS = 1_000
private val HELLO_BYTES = "Hello".toByteArray(Charsets.UTF_8)

enum class EndpointProtocol {
    TCP,
    UDP,
    HTTP
}

data class Endpoint(
    val protocol: EndpointProtocol,
    val host: String,
    val port: Int
) {
    private fun bracketedHost(): String = if (host.contains(':')) "[$host]" else host

    fun toDisplayString(): String = "${protocol.name.lowercase()}:${bracketedHost()}:$port"

    fun toHttpHost(): String = bracketedHost()
}

object EndpointClient {
    fun parseEndpoint(spec: String): Endpoint {
        val trimmed = spec.trim()
        val firstSeparator = trimmed.indexOf(':')
        val lastSeparator = trimmed.lastIndexOf(':')

        if (firstSeparator <= 0 || lastSeparator <= firstSeparator || lastSeparator >= trimmed.lastIndex) {
            throw IllegalArgumentException("Endpoint must use protocol:host:port format.")
        }

        val protocol = when (trimmed.substring(0, firstSeparator).lowercase()) {
            "tcp" -> EndpointProtocol.TCP
            "udp" -> EndpointProtocol.UDP
            "http" -> EndpointProtocol.HTTP
            else -> throw IllegalArgumentException("Unsupported protocol. Use tcp, udp, or http.")
        }

        val host = trimmed.substring(firstSeparator + 1, lastSeparator).trim().removeSurrounding("[", "]")
        if (host.isBlank()) {
            throw IllegalArgumentException("Endpoint host cannot be blank.")
        }

        val port = trimmed.substring(lastSeparator + 1).trim().toIntOrNull()
            ?.takeIf { it in 1..65_535 }
            ?: throw IllegalArgumentException("Endpoint port must be between 1 and 65535.")

        return Endpoint(protocol = protocol, host = host, port = port)
    }

    fun requestJson(endpoint: Endpoint): String {
        val rawResponse = when (endpoint.protocol) {
            EndpointProtocol.TCP -> requestTcp(endpoint)
            EndpointProtocol.UDP -> requestUdp(endpoint)
            EndpointProtocol.HTTP -> requestHttp(endpoint)
        }

        return formatJson(rawResponse)
    }

    private fun requestTcp(endpoint: Endpoint): String {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(endpoint.host, endpoint.port), CONNECT_TIMEOUT_MS)
            socket.soTimeout = READ_TIMEOUT_MS

            val output = socket.getOutputStream()
            output.write(HELLO_BYTES)
            output.flush()
            socket.shutdownOutput()

            return readTcpResponse(socket)
        }
    }

    private fun requestUdp(endpoint: Endpoint): String {
        DatagramSocket().use { socket ->
            socket.soTimeout = READ_TIMEOUT_MS

            val address = InetAddress.getByName(endpoint.host)
            val request = DatagramPacket(HELLO_BYTES, HELLO_BYTES.size, address, endpoint.port)
            socket.send(request)

            val buffer = ByteArray(65_507)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)

            return String(response.data, response.offset, response.length, Charsets.UTF_8)
        }
    }

    private fun requestHttp(endpoint: Endpoint): String {
        val connection = (URL("http://${endpoint.toHttpHost()}:${endpoint.port}/").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "text/plain; charset=utf-8")
        }

        try {
            connection.outputStream.use { output ->
                output.write(HELLO_BYTES)
                output.flush()
            }

            val stream = if (connection.responseCode >= 400) {
                connection.errorStream ?: throw IOException("HTTP ${connection.responseCode} with empty response body.")
            } else {
                connection.inputStream
            }

            val response = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (response.isBlank()) {
                throw IOException("HTTP response body was empty.")
            }

            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun readTcpResponse(socket: Socket): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4_096)
        var receivedAnyBytes = false

        while (true) {
            try {
                val read = socket.getInputStream().read(buffer)
                if (read == -1) {
                    break
                }

                output.write(buffer, 0, read)
                receivedAnyBytes = true
                socket.soTimeout = TCP_IDLE_TIMEOUT_MS
            } catch (exception: SocketTimeoutException) {
                if (receivedAnyBytes) {
                    break
                }

                throw IOException("Timed out waiting for the first TCP response byte.", exception)
            }
        }

        if (!receivedAnyBytes) {
            throw IOException("No TCP response received.")
        }

        return String(output.toByteArray(), Charsets.UTF_8)
    }

    private fun formatJson(rawResponse: String): String {
        val trimmed = rawResponse.trim()
        if (trimmed.isEmpty()) {
            throw IOException("Response body was empty.")
        }

        return try {
            when (val value = JSONTokener(trimmed).nextValue()) {
                is JSONObject -> value.toString(2)
                is JSONArray -> value.toString(2)
                JSONObject.NULL -> "null"
                is String -> JSONObject.quote(value)
                else -> value.toString()
            }
        } catch (exception: JSONException) {
            throw IOException("Response was not valid JSON.", exception)
        }
    }
}
