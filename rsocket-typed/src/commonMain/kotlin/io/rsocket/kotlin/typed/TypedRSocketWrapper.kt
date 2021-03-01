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

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.transport.*
import io.rsocket.kotlin.typed.encoding.*
import kotlinx.coroutines.flow.*

public class TypedRSocketWrapper internal constructor(
    private val coder: TypedPayloadCoder
) : RSocketWrapper<TypedRSocket> {
    override fun wrapRequester(rSocket: RSocket): TypedRSocket = TypedRSocketRequester(rSocket, coder)
    override fun wrapResponder(rSocket: TypedRSocket): RSocket = TypedRSocketResponder(rSocket, coder)
}

@OptIn(ExperimentalMetadataApi::class)
private class TypedRSocketResponder(
    private val rSocket: TypedRSocket,
    private val coder: TypedPayloadCoder
) : RSocket, Cancellable by rSocket {
    override suspend fun metadataPush(metadata: ByteReadPacket) {
        val request = coder.metadataCoder.decode(metadata) ?: TypedMetadataFromBuilder()
        rSocket.metadataPush(request)
    }

    override suspend fun fireAndForget(payload: Payload) {
        val request = coder.decode(payload)
        rSocket.fireAndForget(request)
    }

    override suspend fun requestResponse(payload: Payload): Payload {
        val request = coder.decode(payload)
        val response = rSocket.requestResponse(request)
        return coder.encode(response)
    }

    override fun requestStream(payload: Payload): Flow<Payload> {
        val request = coder.decode(payload)
        val response = rSocket.requestStream(request)
        return response.map(coder::encode)
    }

    override fun requestChannel(initPayload: Payload, payloads: Flow<Payload>): Flow<Payload> {
        val initRequest = coder.decode(initPayload)
        val flowRequest = payloads.map(coder::decode)
        val response = rSocket.requestChannel(initRequest, flowRequest)
        return response.map(coder::encode)
    }
}

@OptIn(ExperimentalMetadataApi::class)
private class TypedRSocketRequester(
    private val rSocket: RSocket,
    private val coder: TypedPayloadCoder
) : TypedRSocket, Cancellable by rSocket {

    override suspend fun metadataPush(metadata: TypedMetadata) {
        val request = coder.metadataCoder.encode(metadata) ?: ByteReadPacket.Empty
        rSocket.metadataPush(request)
    }

    override suspend fun requestResponse(payload: TypedPayload): TypedPayload {
        val request = coder.encode(payload)
        val response = rSocket.requestResponse(request)
        return coder.decode(response)
    }

    override suspend fun fireAndForget(payload: TypedPayload) {
        val request = coder.encode(payload)
        rSocket.fireAndForget(request)
    }

    override fun requestStream(payload: TypedPayload): Flow<TypedPayload> {
        val request = coder.encode(payload)
        val response = rSocket.requestStream(request)
        return response.map(coder::decode)
    }

    override fun requestChannel(initPayload: TypedPayload, payloads: Flow<TypedPayload>): Flow<TypedPayload> {
        val initRequest = coder.encode(initPayload)
        val flowRequest = payloads.map(coder::encode)
        val response = rSocket.requestChannel(initRequest, flowRequest)
        return response.map(coder::decode)
    }
}

private suspend fun m() {
    lateinit var ct: ClientTransport
    lateinit var st: ServerTransport<Unit>

    fun responder(): TypedRSocket = TODO()

    val wrapper = TypedRSocketWrapper(
        TypedPayloadCoder(
            DataCoder(emptyList(), emptyList()),
            CompositeMetadataCoder()
        )
    )

    val connector = RSocketConnector {
        connectionConfig {
            payloadMimeType = PayloadMimeType(
                WellKnownMimeType.ApplicationProtoBuf,
                WellKnownMimeType.MessageRSocketCompositeMetadata
            )
        }
    }

    val r = connector.connect(wrapper, ct)

    val server = RSocketServer {

    }


    server.bind(wrapper, st) {

        responder()
    }
}
