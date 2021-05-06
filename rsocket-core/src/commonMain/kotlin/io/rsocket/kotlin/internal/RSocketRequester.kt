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

package io.rsocket.kotlin.internal

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.frame.*
import io.rsocket.kotlin.internal.handler.*
import io.rsocket.kotlin.payload.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

internal class RSocketRequester(
    override val job: Job,
    private val requestScope: CoroutineScope,
    private val connection: FrameSender,
    private val streamsStorage: StreamsStorage
) : RSocket {
    override suspend fun metadataPush(metadata: ByteReadPacket) {
        println("requester metadataPush: ${currentThreadName()}")
        ensureActiveOrRelease(metadata)

        metadata.closeOnError {
            connection.sendMetadataPush(metadata)
        }
    }

    override suspend fun fireAndForget(payload: Payload) {
        println("requester fireAndForget: ${currentThreadName()}")
        ensureActiveOrRelease(payload)

        val id = streamsStorage.nextId()

        try {
            connection.sendRequestPayload(FrameType.RequestFnF, id, payload)
        } catch (cause: Throwable) {
            payload.release()
            if (job.isActive) connection.sendCancel(id)
            throw cause
        }
    }

    override suspend fun requestResponse(payload: Payload): Payload {
        println("requester requestResponse: ${currentThreadName()}")
        ensureActiveOrRelease(payload)

        val id = streamsStorage.nextId()

        val deferred = CompletableDeferred<Payload>()
        val handler = RequesterRequestResponseFrameHandler(id, streamsStorage, deferred)
        streamsStorage.save(id, handler)

        return handler.receiveOrCancel(id, payload) {
            connection.sendRequestPayload(FrameType.RequestResponse, id, payload)
            deferred.await()
        }
    }

    @ExperimentalStreamsApi
    override fun requestStream(payload: Payload): Flow<Payload> = requestFlow { strategy, initialRequest ->
        println("requester requestStream: ${currentThreadName()}")
        ensureActiveOrRelease(payload)

        val id = streamsStorage.nextId()

        val channel = SafeChannel<Payload>(Channel.UNLIMITED)
        val handler = RequesterRequestStreamFrameHandler(id, streamsStorage, channel)
        streamsStorage.save(id, handler)

        handler.receiveOrCancel(id, payload) {
            connection.sendRequestPayload(FrameType.RequestStream, id, payload, initialRequest)
            emitAllWithRequestN(channel, strategy) { connection.sendRequestN(id, it) }
        }
    }

    @ExperimentalStreamsApi
    override fun requestChannel(initPayload: Payload, payloads: Flow<Payload>): Flow<Payload> = requestFlow { strategy, initialRequest ->
        println("requester requestChannel: ${currentThreadName()}")
        ensureActiveOrRelease(initPayload)

        val id = streamsStorage.nextId()

        val channel = SafeChannel<Payload>(Channel.UNLIMITED)
        val limiter = Limiter(0)
        val sender = Job(requestScope.coroutineContext[Job])
        val handler = RequesterRequestChannelFrameHandler(id, streamsStorage, limiter, sender, channel)
        streamsStorage.save(id, handler)

        handler.receiveOrCancel(id, initPayload) {
            connection.sendRequestPayload(FrameType.RequestChannel, id, initPayload, initialRequest)
            requestScope.launch(sender, start = CoroutineStart.UNDISPATCHED) {
                handler.sendOrFail(id) {
                    payloads.collectLimiting(limiter) { connection.sendNextPayload(id, it) }
                    connection.sendCompletePayload(id)
                }
            }
            emitAllWithRequestN(channel, strategy) { connection.sendRequestN(id, it) }
        }
    }


    private suspend inline fun SendFrameHandler.sendOrFail(
        id: Int,
        block: () -> Unit
    ) {
        try {
            block()
            onSendComplete()
        } catch (cause: Throwable) {
            val isFailed = onSendFailed(cause)
            if (job.isActive && isFailed) connection.sendError(id, cause)
            throw cause
        }
    }

    private suspend inline fun <T> ReceiveFrameHandler.receiveOrCancel(
        id: Int,
        payload: Payload,
        block: () -> T
    ): T {
        try {
            val result = block()
            onReceiveComplete()
            return result
        } catch (cause: Throwable) {
            payload.release()
            val isCancelled = onReceiveCancelled(cause)
            if (job.isActive && isCancelled) connection.sendCancel(id)
            throw cause
        }
    }

    private fun ensureActiveOrRelease(closeable: Closeable) {
        if (job.isActive) return
        closeable.close()
        job.ensureActive()
    }
}
