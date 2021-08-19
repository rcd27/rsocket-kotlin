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
import io.ktor.utils.io.core.internal.*
import io.ktor.utils.io.pool.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.frame.*
import io.rsocket.kotlin.internal.handler.*
import io.rsocket.kotlin.payload.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalStreamsApi::class)
internal class RSocketRequester(
    connectionJob: Job,
    private val sender: FrameSender,
    private val streamsStorage: StreamsStorage,
    private val requestScope: CoroutineScope,
    private val pool: ObjectPool<ChunkBuffer>
) : RSocket {
    override val job: Job = connectionJob

    override suspend fun metadataPush(metadata: ByteReadPacket) {
        ensureActiveOrRelease(metadata)
        metadata.closeOnError {
            sender.sendMetadataPush(metadata)
        }
    }

    override suspend fun fireAndForget(payload: Payload) {
        ensureActiveOrRelease(payload)

        val id = streamsStorage.nextId()
        try {
            sender.sendRequestPayload(FrameType.RequestFnF, id, payload)
        } catch (cause: Throwable) {
            payload.release()
            if (job.isActive) sender.sendCancel(id) //if cancelled during fragmentation
            throw cause
        }
    }

    override suspend fun requestResponse(payload: Payload): Payload {
        ensureActiveOrRelease(payload)

        val id = streamsStorage.nextId()

        val deferred = CompletableDeferred<Payload>()
        val handler = RequesterRequestResponseFrameHandler(id, streamsStorage, deferred, pool)
        streamsStorage.save(id, handler)

        return handler.receiveOrCancel(id, payload) {
            sender.sendRequestPayload(FrameType.RequestResponse, id, payload)
            deferred.await()
        }
    }

    override fun requestStream(payload: Payload): Flow<Payload> = requestFlow { strategy, initialRequest ->
        ensureActiveOrRelease(payload)

        val id = streamsStorage.nextId()

        val channel = SafeChannel<Payload>(Channel.UNLIMITED)
        val handler = RequesterRequestStreamFrameHandler(id, streamsStorage, channel, pool)
        streamsStorage.save(id, handler)

        handler.receiveOrCancel(id, payload) {
            sender.sendRequestPayload(FrameType.RequestStream, id, payload, initialRequest)
            emitAllWithRequestN(channel, strategy) { sender.sendRequestN(id, it) }
        }
    }

    override fun requestChannel(initPayload: Payload, payloads: Flow<Payload>): Flow<Payload> = requestFlow { strategy, initialRequest ->
        ensureActiveOrRelease(initPayload)

        val id = streamsStorage.nextId()

        val channel = SafeChannel<Payload>(Channel.UNLIMITED)
        val limiter = Limiter(0)
        val payloadsJob = Job(requestScope.coroutineContext.job)
        val handler = RequesterRequestChannelFrameHandler(id, streamsStorage, limiter, payloadsJob, channel, pool)
        streamsStorage.save(id, handler)

        handler.receiveOrCancel(id, initPayload) {
            sender.sendRequestPayload(FrameType.RequestChannel, id, initPayload, initialRequest)
            //TODO lazy?
            requestScope.launch(payloadsJob) {
                handler.sendOrFail(id) {
                    payloads.collectLimiting(limiter) { sender.sendNextPayload(id, it) }
                    sender.sendCompletePayload(id)
                }
            }
            emitAllWithRequestN(channel, strategy) { sender.sendRequestN(id, it) }
        }
    }

    private suspend inline fun SendFrameHandler.sendOrFail(id: Int, block: () -> Unit) {
        try {
            block()
            onSendComplete()
        } catch (cause: Throwable) {
            val isFailed = onSendFailed(cause)
            if (job.isActive && isFailed) sender.sendError(id, cause)
            throw cause
        }
    }

    private suspend inline fun <T> ReceiveFrameHandler.receiveOrCancel(id: Int, payload: Payload, block: () -> T): T {
        try {
            val result = block()
            onReceiveComplete()
            return result
        } catch (cause: Throwable) {
            payload.release()
            val isCancelled = onReceiveCancelled(cause)
            if (job.isActive && isCancelled) sender.sendCancel(id)
            throw cause
        }
    }

    private fun ensureActiveOrRelease(closeable: Closeable) {
        if (job.isActive) return
        closeable.close()
        job.ensureActive()
    }
}
