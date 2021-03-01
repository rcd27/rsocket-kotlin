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

package io.rsocket.kotlin.typed

import io.rsocket.kotlin.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//todo sealed interface or class
public fun interface TypedRSocketRequestHandlerProducer {
    public operator fun invoke(parentJob: Job?): TypedRSocket
    public operator fun invoke(): TypedRSocket = invoke(null)
}

public class TypedRSocketRequestHandlerBuilder internal constructor() {
    private var metadataPush: (suspend TypedRSocket.(metadata: TypedMetadata) -> Unit)? = null
    private var fireAndForget: (suspend TypedRSocket.(payload: TypedPayload) -> Unit)? = null
    private var requestResponse: (suspend TypedRSocket.(payload: TypedPayload) -> TypedPayload)? = null
    private var requestStream: (TypedRSocket.(payload: TypedPayload) -> Flow<TypedPayload>)? = null
    private var requestChannel: (TypedRSocket.(initPayload: TypedPayload, payloads: Flow<TypedPayload>) -> Flow<TypedPayload>)? = null

    public fun metadataPush(block: (suspend TypedRSocket.(metadata: TypedMetadata) -> Unit)) {
        check(metadataPush == null) { "Metadata Push handler already configured" }
        metadataPush = block
    }

    public fun fireAndForget(block: (suspend TypedRSocket.(payload: TypedPayload) -> Unit)) {
        check(fireAndForget == null) { "Fire and Forget handler already configured" }
        fireAndForget = block
    }

    public fun requestResponse(block: (suspend TypedRSocket.(payload: TypedPayload) -> TypedPayload)) {
        check(requestResponse == null) { "Request Response handler already configured" }
        requestResponse = block
    }

    public fun requestStream(block: (TypedRSocket.(payload: TypedPayload) -> Flow<TypedPayload>)) {
        check(requestStream == null) { "Request Stream handler already configured" }
        requestStream = block
    }

    public fun requestChannel(block: (TypedRSocket.(initPayload: TypedPayload, payloads: Flow<TypedPayload>) -> Flow<TypedPayload>)) {
        check(requestChannel == null) { "Request Channel handler already configured" }
        requestChannel = block
    }

    internal fun build(job: CompletableJob): TypedRSocket =
        TypedRSocketRequestHandler(job, metadataPush, fireAndForget, requestResponse, requestStream, requestChannel)

    internal fun buildProducer(producerPparentJob: Job? = null): TypedRSocketRequestHandlerProducer {
        val metadataPush = this.metadataPush
        val fireAndForget = this.fireAndForget
        val requestResponse = this.requestResponse
        val requestStream = this.requestStream
        val requestChannel = this.requestChannel

        return TypedRSocketRequestHandlerProducer { parentJob ->
            val job = Job(parentJob ?: producerPparentJob)
            TypedRSocketRequestHandler(job, metadataPush, fireAndForget, requestResponse, requestStream, requestChannel)
        }
    }
}

public fun TypedRSocketRequestHandlerProducer(
    parentJob: Job? = null,
    configure: TypedRSocketRequestHandlerBuilder.() -> Unit
): TypedRSocketRequestHandlerProducer {
    val builder = TypedRSocketRequestHandlerBuilder()
    builder.configure()
    return builder.buildProducer(parentJob)
}

@Suppress("FunctionName")
public fun TypedRSocketRequestHandler(
    parentJob: Job? = null,
    configure: TypedRSocketRequestHandlerBuilder.() -> Unit
): TypedRSocket {
    val builder = TypedRSocketRequestHandlerBuilder()
    builder.configure()
    return builder.build(Job(parentJob))
}

private class TypedRSocketRequestHandler(
    override val job: CompletableJob,
    private val metadataPush: (suspend TypedRSocket.(metadata: TypedMetadata) -> Unit)? = null,
    private val fireAndForget: (suspend TypedRSocket.(payload: TypedPayload) -> Unit)? = null,
    private val requestResponse: (suspend TypedRSocket.(payload: TypedPayload) -> TypedPayload)? = null,
    private val requestStream: (TypedRSocket.(payload: TypedPayload) -> Flow<TypedPayload>)? = null,
    private val requestChannel: (TypedRSocket.(initPayload: TypedPayload, payloads: Flow<TypedPayload>) -> Flow<TypedPayload>)? = null,
) : TypedRSocket {
    override suspend fun metadataPush(metadata: TypedMetadata): Unit =
        metadataPush?.invoke(this, metadata) ?: super.metadataPush(metadata)

    override suspend fun fireAndForget(payload: TypedPayload): Unit =
        fireAndForget?.invoke(this, payload) ?: super.fireAndForget(payload)

    override suspend fun requestResponse(payload: TypedPayload): TypedPayload =
        requestResponse?.invoke(this, payload) ?: super.requestResponse(payload)

    override fun requestStream(payload: TypedPayload): Flow<TypedPayload> =
        requestStream?.invoke(this, payload) ?: super.requestStream(payload)

    override fun requestChannel(initPayload: TypedPayload, payloads: Flow<TypedPayload>): Flow<TypedPayload> =
        requestChannel?.invoke(this, initPayload, payloads) ?: super.requestChannel(initPayload, payloads)

}
