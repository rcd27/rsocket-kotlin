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

import io.rsocket.*
import io.rsocket.core.*
import io.rsocket.frame.decoder.*
import io.rsocket.transport.local.*
import io.rsocket.transport.netty.client.*
import io.rsocket.transport.netty.server.*
import io.rsocket.util.*
import kotlinx.benchmark.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.*
import org.reactivestreams.*
import reactor.core.publisher.*

class RSocketJavaBenchmark : RSocketBenchmark<Payload, RSocket>() {
    lateinit var server: Closeable
    lateinit var payloadMono: Mono<Payload>
    lateinit var payloadsFlux: Flux<Payload>

    @Param("local", "tcp", "ws")
    var transport: String = "local"

    override fun connect(): RSocket {
        payloadMono = Mono.fromSupplier(this::copyPayload)
        payloadsFlux = Flux.range(0, 5000).map { copyPayload() }

        val (serverTransport, clientTransport) = when (transport) {
            "local" -> LocalServerTransport.create("server") to LocalClientTransport.create("server")
            "tcp"   -> TcpServerTransport.create(9000) to TcpClientTransport.create(9000)
            "ws"    -> WebsocketServerTransport.create(9000) to WebsocketClientTransport.create(9000)
            else    -> error("Wrong transport")
        }

        server = RSocketServer.create { _, _ ->
            Mono.just(
                object : RSocket {
                    override fun requestResponse(payload: Payload): Mono<Payload> {
                        payload.release()
                        return payloadMono
                    }

                    override fun requestStream(payload: Payload): Flux<Payload> {
                        payload.release()
                        return payloadsFlux
                    }

                    override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> = Flux.from(payloads)
                })
        }
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .bind(serverTransport)
            .block()!!

        return RSocketConnector.create()
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .connect(clientTransport)
            .block()!!
    }

    override fun cleanup() {
        client.dispose()
        server.dispose()
    }

    override fun emptyPayload(): Payload = EmptyPayload.INSTANCE
    override fun createPayload(data: ByteArray, metadata: ByteArray): Payload = ByteBufPayload.create(data, metadata)
    override fun releasePayload(payload: Payload) {
        payload.release()
    }

    override fun copyPayload(): Payload = payload.retain()

    override suspend fun RSocket.doRequestResponse(payload: Payload): Payload = requestResponse(payload).awaitSingle()

    override suspend fun RSocket.doRequestStream(payload: Payload): Flow<Payload> = requestStream(payload).asFlow()

    override suspend fun RSocket.doRequestChannel(payload: Payload, payloads: Flow<Payload>): Flow<Payload> =
        requestChannel(payloads.onStart { emit(payload) }.asPublisher()).asFlow()

}

internal actual fun runBenchmark(block: suspend CoroutineScope.() -> Unit) {
    runBlocking(block = block)
}
