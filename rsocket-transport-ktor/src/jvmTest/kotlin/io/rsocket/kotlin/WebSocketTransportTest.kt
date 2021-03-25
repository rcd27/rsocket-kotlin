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

package io.rsocket.kotlin

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.rsocket.kotlin.test.*
import io.rsocket.kotlin.transport.ktor.client.*
import io.rsocket.kotlin.transport.ktor.server.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlin.random.*
import io.ktor.client.features.websocket.WebSockets as ClientWebSockets
import io.rsocket.kotlin.transport.ktor.client.RSocketSupport as ClientRSocketSupport

abstract class WebSocketTransportTest(
    clientEngine: HttpClientEngineFactory<*>,
    serverEngine: ApplicationEngineFactory<*, *>,
) : TransportTest() {

    private val httpClient = HttpClient(clientEngine) {
        install(ClientWebSockets)
        install(ClientRSocketSupport) { connector = CONNECTOR }
    }

    override suspend fun before() {
        client = httpClient.rSocket(port = 9000)
    }
}
