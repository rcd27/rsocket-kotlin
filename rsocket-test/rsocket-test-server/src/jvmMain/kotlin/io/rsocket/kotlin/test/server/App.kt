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

package io.rsocket.kotlin.test.server

import io.ktor.application.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.logging.*
import io.rsocket.kotlin.test.*
import io.rsocket.kotlin.transport.ktor.*
import io.rsocket.kotlin.transport.ktor.server.*
import kotlinx.coroutines.*

fun main() {
    val rSocketServer = RSocketServer {
        loggerFactory = PrintLogger.withLevel(LoggingLevel.DEBUG)
    }
    val acceptor = ConnectionAcceptor { TestRSocket() }
    //start TCP server
    val tcpTransport = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().serverTransport(port = 8000)
    rSocketServer.bind(tcpTransport, acceptor)

    //start WS server
    embeddedServer(CIO, port = 9000) {
        install(WebSockets)
        install(RSocketSupport) { server = rSocketServer }

        routing {
            rSocket(acceptor = acceptor)
        }
    }.start(true)
}
