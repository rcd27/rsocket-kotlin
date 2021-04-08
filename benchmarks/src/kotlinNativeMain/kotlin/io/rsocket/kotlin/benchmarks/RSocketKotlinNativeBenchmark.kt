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

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.util.network.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.transport.ktor.*
import io.rsocket.kotlin.transport.local.*
import kotlinx.benchmark.*
import kotlinx.coroutines.*

@OptIn(InternalAPI::class)
class RSocketKotlinNativeBenchmark : RSocketKotlinBenchmark() {

    private var server: Job? = null
    private var tcpServer: TcpServer? = null

    @Param("local", "tcp")
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
                val tcp = aSocket(SelectorManager()).tcp()
                val tcpServer2 = tcp.serverTransport(address)
                tcpServer = tcpServer2
                server = rSocketServer.bind(tcpServer2, acceptor)
                rSocketConnector.connect(tcp.clientTransport(address))
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
        }
    }

}

internal actual fun runBenchmark(block: suspend CoroutineScope.() -> Unit) {
    runBlocking(block = block)
}
