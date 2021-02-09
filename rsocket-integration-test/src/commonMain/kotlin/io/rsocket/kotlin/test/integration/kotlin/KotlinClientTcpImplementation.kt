package io.rsocket.kotlin.test.integration.kotlin

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.test.integration.*
import io.rsocket.kotlin.transport.ktor.*

object KotlinClientTcpImplementation : ClientImplementation {
    private val tcp = aSocket(SelectorManager()).tcp()

    override suspend fun connectClient(configuration: ClientConfiguration): Requester {
        val transport = tcp.clientTransport("0.0.0.0", configuration.port)
        val rSocket = RSocketConnector {
            maxFragmentSize = configuration.maxFragmentSize
//            loggerFactory = PrintLogger.withLevel(LoggingLevel.DEBUG)
        }.connect(transport)
        return KotlinRequester(rSocket)
    }
}
