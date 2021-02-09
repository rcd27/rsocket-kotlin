package io.rsocket.kotlin.test.integration.java

import io.rsocket.transport.*
import io.rsocket.transport.netty.client.*
import io.rsocket.transport.netty.server.*

object JavaWsImplementation : JavaImplementation {
    override fun serverTransport(port: Int): ServerTransport<*> = WebsocketServerTransport.create(port)
    override fun clientTransport(port: Int): ClientTransport = WebsocketClientTransport.create(port)
}
