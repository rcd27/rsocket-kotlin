package io.rsocket.kotlin.test.integration.java

import io.rsocket.transport.*
import io.rsocket.transport.netty.client.*
import io.rsocket.transport.netty.server.*

object JavaTcpImplementation : JavaImplementation {
    override fun serverTransport(port: Int): ServerTransport<*> = TcpServerTransport.create(port)
    override fun clientTransport(port: Int): ClientTransport = TcpClientTransport.create(port)
}
