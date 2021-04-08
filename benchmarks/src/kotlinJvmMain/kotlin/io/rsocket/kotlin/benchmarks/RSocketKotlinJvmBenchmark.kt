/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.kotlin.benchmarks

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.util.network.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.transport.ktor.*
import io.rsocket.kotlin.transport.ktor.client.*
import io.rsocket.kotlin.transport.ktor.server.*
import io.rsocket.kotlin.transport.local.*
import kotlinx.benchmark.*
import kotlinx.coroutines.*
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.features.websocket.WebSockets as ClientWebSockets
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.websocket.WebSockets as ServerWebSockets
import io.rsocket.kotlin.transport.ktor.client.RSocketSupport as ClientRSocketSupport
import io.rsocket.kotlin.transport.ktor.server.RSocketSupport as ServerRSocketSupport

class RSocketKotlinJvmBenchmark : RSocketKotlinBenchmark() {

    private var server: Job? = null
    private var tcpServer: TcpServer? = null
    private var httpServer: ApplicationEngine? = null

    @Param("local", "tcp", "ws")
    var transport: String = "local"

    override fun connect(rSocketServer: RSocketServer, rSocketConnector: RSocketConnector): RSocket = runBlocking {
        when (transport) {
            "local" -> {
                val localServer = LocalServer()
                server = rSocketServer.bind(localServer, acceptor)
                rSocketConnector.connect(localServer)
            }
            "tcp"   -> {
                val address = NetworkAddress("0.0.0.0", 9006)
                val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
                val tcpServer2 = tcp.serverTransport(address)
                tcpServer = tcpServer2
                server = rSocketServer.bind(tcpServer2, acceptor)
                rSocketConnector.connect(tcp.clientTransport(address))
            }
            "ws"    -> {
                val httpClient = HttpClient(ClientCIO) {
                    install(ClientWebSockets)
                    install(ClientRSocketSupport) {
                        connector = rSocketConnector
                    }
                }
                httpServer = embeddedServer(ServerCIO, 9006) {
                    install(ServerWebSockets)
                    install(ServerRSocketSupport) {
                        server = rSocketServer
                    }
                    install(Routing) { rSocket(acceptor = acceptor) }
                }
                httpServer?.start()
                trySeveralTimes { httpClient.rSocket(port = 9006) }
            }
            else    -> error("Wrong transport")
        }
    }

    override fun cleanup() {
        runBlocking {
            client.cancelAndJoin()
            server?.cancelAndJoin()
            tcpServer?.socket?.apply {
                socketContext.cancelChildren()
                close()
                awaitClosed()
            }
            httpServer?.stop(0, 0)
        }
    }

    private suspend inline fun <R> trySeveralTimes(block: () -> R): R {
        lateinit var error: Throwable
        repeat(10) {
            try {
                return block()
            } catch (e: Throwable) {
                error = e
                delay(500) //sometimes address isn't yet available (server isn't started)
            }
        }
        throw error
    }

}

internal actual fun runBenchmark(block: suspend CoroutineScope.() -> Unit) {
    runBlocking(block = block)
}
