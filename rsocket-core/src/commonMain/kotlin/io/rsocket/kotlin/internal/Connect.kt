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
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.frame.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*

//TODO is it needed
@SharedImmutable
private val ignoreExceptionHandler = CoroutineExceptionHandler { _, _ -> }

@TransportApi
internal suspend fun Connection.connect(
    isServer: Boolean,
    interceptors: Interceptors,
    connectionConfig: ConnectionConfig,
    acceptor: ConnectionAcceptor
): RSocket {
    println("connect: ${currentThreadName()}")
    val keepAliveHandler = KeepAliveHandler(connectionConfig.keepAlive)
    val streamsPrioritizer = StreamsPrioritizer()
    val frameSender = FrameSender(streamsPrioritizer)
    val streamsStorage = StreamsStorage(isServer)
    val requestScope = CoroutineScope(SupervisorJob(job) + Dispatchers.Unconfined + ignoreExceptionHandler)
    val connectionScope = CoroutineScope(job + Dispatchers.Default + ignoreExceptionHandler)

    job.invokeOnCompletion {
        println("job.invokeOnCompletion: ${currentThreadName()}")
        streamsPrioritizer.close(it)
        streamsStorage.cleanup(it)
        connectionConfig.setupPayload.release()
    }

    val requester = interceptors.wrapRequester(
        RSocketRequester(job, requestScope, frameSender, streamsStorage)
    )

    val requestHandler = interceptors.wrapResponder(
        with(interceptors.wrapAcceptor(acceptor)) {
            ConnectionAcceptorContext(connectionConfig, requester).accept()
        }
    )

    // link completing of connection and requestHandler
    job.invokeOnCompletion {
        requestHandler.job.cancel("Connection closed", it)
    }
    requestHandler.job.invokeOnCompletion {
        println("requestHandler.job.invokeOnCompletion: ${currentThreadName()}")
        if (it != null) job.cancel("Request handler failed", it)
    }

    // start keepalive ticks
    connectionScope.launch() {
        while (true) {
            println("before KA tick: ${currentThreadName()}")
            if (keepAliveHandler.tick(job)) break
            frameSender.sendKeepAlive(true, 0, ByteReadPacket.Empty)
        }
    }

    // start sending frames to connection
    connectionScope.launch() {
        while (true) {
            println("before send: ${currentThreadName()}")
            sendFrame(streamsPrioritizer.receive())
            println("after send: ${currentThreadName()}")
        }
    }

    // start frame handling
    connectionScope.launch() {
        val rSocketResponder = RSocketResponder(frameSender, requestScope, requestHandler)
        while (true) {
            println("before receive: ${currentThreadName()}")
            receiveFrame().closeOnError { frame ->
                when (frame.streamId) {
                    0 -> when (frame) {
                        is MetadataPushFrame -> rSocketResponder.handleMetadataPush(frame.metadata)
                        is ErrorFrame        -> job.cancel("Error frame received on 0 stream", frame.throwable)
                        is KeepAliveFrame    -> {
                            keepAliveHandler.mark()
                            if (frame.respond) frameSender.sendKeepAlive(false, 0, frame.data) else Unit
                        }
                        is LeaseFrame        -> frame.release().also { error("lease isn't implemented") }
                        else                 -> frame.release()
                    }
                    else -> streamsStorage.handleFrame(frame, rSocketResponder)
                }
            }
            println("after receive: ${currentThreadName()}")
        }
    }

    return requester
}
