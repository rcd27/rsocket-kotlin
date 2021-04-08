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

import kotlinx.benchmark.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.*

abstract class AbstractRSocketBenchmark<Payload : Any, RSocket : Any> {
    lateinit var client: RSocket
    lateinit var payload: Payload
    abstract val payloadSize: Int
    val payloadsFlow: Flow<Payload> = flow { repeat(5000) { emit(copyPayload()) } }

    abstract fun connect(): RSocket
    abstract fun cleanup()

    abstract fun emptyPayload(): Payload
    abstract fun createPayload(data: ByteArray, metadata: ByteArray): Payload
    abstract fun releasePayload(payload: Payload)
    abstract fun copyPayload(): Payload

    abstract suspend fun RSocket.doRequestResponse(payload: Payload): Payload
    abstract suspend fun RSocket.doRequestStream(payload: Payload): Flow<Payload>
    abstract suspend fun RSocket.doRequestChannel(payload: Payload, payloads: Flow<Payload>): Flow<Payload>

    @Setup
    fun setupInternal() {
        payload = if (payloadSize == 0) emptyPayload() else createPayload(
            ByteArray(payloadSize / 2).also { Random.nextBytes(it) },
            ByteArray(payloadSize / 2).also { Random.nextBytes(it) }
        )
        client = connect()
    }

    @TearDown
    fun cleanupInternal() {
        cleanup()
    }

    protected suspend fun requestResponse(bh: Blackhole) {
        client.doRequestResponse(copyPayload()).also {
            releasePayload(it)
            bh.consume(it)
        }
    }

    protected suspend fun requestStream(bh: Blackhole) {
        client.doRequestStream(copyPayload()).collect {
            releasePayload(it)
            bh.consume(it)
        }
    }

    protected suspend fun requestChannel(bh: Blackhole) {
        client.doRequestChannel(copyPayload(), payloadsFlow).collect {
            releasePayload(it)
            bh.consume(it)
        }
    }
}

internal expect fun runBenchmark(block: suspend CoroutineScope.() -> Unit)


//plain blocking
internal inline fun blocking(bh: Blackhole, crossinline block: suspend (bh: Blackhole) -> Unit): Unit = runBenchmark {
    block(bh)
}

//Run every request in separate coroutine which will be dispatched on Default dispatcher (threads amount = cores amount)
internal inline fun parallel(bh: Blackhole, p: Int, crossinline block: suspend (bh: Blackhole) -> Unit): Unit = runBenchmark {
    (0..p).map {
        GlobalScope.async { block(bh) }
    }.awaitAll()
}
