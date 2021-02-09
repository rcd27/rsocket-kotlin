package io.rsocket.kotlin.test.integration.kotlin

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.test.integration.*
import io.rsocket.kotlin.transport.ktor.*
import kotlinx.coroutines.*

object KotlinServerTcpImplementation : ServerImplementation {
    private val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    override suspend fun startServer(requestHandler: RequestHandler, configuration: ServerConfiguration) {
        val server = tcp.serverTransport("0.0.0.0", configuration.port)
        RSocketServer {
            maxFragmentSize = configuration.maxFragmentSize
        }.bind(server) { KotlinResponder(requestHandler) }
    }
}
