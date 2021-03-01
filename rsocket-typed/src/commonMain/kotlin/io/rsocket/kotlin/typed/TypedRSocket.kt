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
import kotlinx.coroutines.flow.*

public interface TypedRSocket : Cancellable, RSocketMarker {
    public suspend fun metadataPush(metadata: TypedMetadata) {
        metadata.close()
        notImplemented("Metadata Push")
    }

    public suspend fun fireAndForget(payload: TypedPayload) {
        payload.close()
        notImplemented("Fire and Forget")
    }

    public suspend fun requestResponse(payload: TypedPayload): TypedPayload {
        payload.close()
        notImplemented("Request Response")
    }

    public fun requestStream(payload: TypedPayload): Flow<TypedPayload> {
        payload.close()
        notImplemented("Request Stream")
    }

    public fun requestChannel(initPayload: TypedPayload, payloads: Flow<TypedPayload>): Flow<TypedPayload> {
        initPayload.close()
        notImplemented("Request Channel")
    }
}

private fun notImplemented(operation: String): Nothing = throw NotImplementedError("$operation is not implemented.")

public suspend inline fun TypedRSocket.metadataPush(
    block: TypedMetadataBuilder.() -> Unit
): Unit = metadataPush(typedMetadata(block))

public suspend inline fun <reified D : Any> TypedRSocket.fireAndForget(
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
): Unit = fireAndForget(typedPayload(data, block))

public suspend inline fun <reified D : Any> TypedRSocket.requestResponse(
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
): TypedPayload = requestResponse(typedPayload(data, block))

public inline fun <reified D : Any> TypedRSocket.requestStream(
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
): Flow<TypedPayload> = requestStream(typedPayload(data, block))

public inline fun <reified D : Any> TypedRSocket.requestChannel(
    data: D,
    payloads: Flow<TypedPayload> = emptyFlow(),
    block: TypedMetadataBuilder.() -> Unit = {}
): Flow<TypedPayload> = requestChannel(typedPayload(data, block), payloads)


public suspend inline fun <reified D : Any> FlowCollector<TypedPayload>.emitPayload(data: D, block: TypedMetadataBuilder.() -> Unit = {}) {
    emit(typedPayload(data, block))
}

public inline fun <reified D : Any> Flow<TypedPayload>.extractPayloadData(): Flow<D> = map(TypedPayload::data)
