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

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.payload.*
import kotlinx.benchmark.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalStreamsApi::class)
abstract class RSocketKotlinBenchmark : RSocketBenchmark<Payload, RSocket>() {

    @Param("default", "perf")
    var configuration: String = "default"

    private val requestStrategy = PrefetchStrategy(64, 0)

    protected val acceptor = ConnectionAcceptor {
        RSocketRequestHandler {
            requestResponse {
                it.release()
                copyPayload()
            }
            requestStream {
                it.release()
                payloadsFlow
            }
            requestChannel { init, payloads ->
                init.release()
                payloads.flowOn(requestStrategy)
            }
        }
    }

    abstract fun connect(
        rSocketServer: RSocketServer,
        rSocketConnector: RSocketConnector
    ): RSocket

    override fun connect(): RSocket = connect(
        rSocketServer = RSocketServer {
            if (configuration == "perf") {
                connectionBuffer = Int.MAX_VALUE
                requestDispatcher = Dispatchers.Unconfined
            }
        },
        rSocketConnector = RSocketConnector {
            if (configuration == "perf") {
                connectionBuffer = Int.MAX_VALUE
                requestDispatcher = Dispatchers.Unconfined
            }
        }
    )

    override fun cleanup() {
        client.job.cancel()
    }

    override fun emptyPayload(): Payload = Payload.Empty
    override fun createPayload(data: ByteArray, metadata: ByteArray): Payload = Payload(ByteReadPacket(data), ByteReadPacket(metadata))
    override fun releasePayload(payload: Payload): Unit = payload.release()
    override fun copyPayload(): Payload = payload.copy()

    override suspend fun RSocket.doRequestResponse(payload: Payload): Payload = requestResponse(payload)

    override suspend fun RSocket.doRequestStream(payload: Payload): Flow<Payload> = requestStream(payload).flowOn(requestStrategy)

    override suspend fun RSocket.doRequestChannel(payload: Payload, payloads: Flow<Payload>): Flow<Payload> =
        requestChannel(payload, payloads).flowOn(requestStrategy)

}
