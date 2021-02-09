package io.rsocket.kotlin.test.integration.java

import io.rsocket.core.*
import io.rsocket.kotlin.test.integration.*
import io.rsocket.transport.*
import kotlinx.coroutines.reactive.*
import reactor.core.publisher.*

interface JavaImplementation : ServerImplementation, ClientImplementation {

    fun serverTransport(port: Int): ServerTransport<*>
    fun clientTransport(port: Int): ClientTransport

    override suspend fun startServer(requestHandler: RequestHandler, configuration: ServerConfiguration) {
        val transport = serverTransport(configuration.port)
        RSocketServer.create { _, _ -> Mono.just(JavaResponder(requestHandler)) }
            .fragment(configuration.maxFragmentSize)
            .bind(transport)
            .awaitFirst()
    }

    override suspend fun connectClient(configuration: ClientConfiguration): Requester {
        val transport = clientTransport(configuration.port)
        return RSocketConnector.create()
            .fragment(configuration.maxFragmentSize)
            .connect(transport)
            .block()!!
            .let(::JavaRequester)
    }
}
