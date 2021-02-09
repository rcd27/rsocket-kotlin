package io.rsocket.kotlin.test.integration.kotlin

import io.rsocket.kotlin.*
import io.rsocket.kotlin.test.integration.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

class KotlinRequester(
    private val rSocket: RSocket
) : Requester {

    override suspend fun doMetadataPush(metadata: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun doFireAndForget(payload: IntegrationPayload) {
        TODO("Not yet implemented")
    }

    override suspend fun doRequestResponse(payload: IntegrationPayload): IntegrationPayload {
        return rSocket.requestResponse(payload.fromIntegration()).toIntegration()
    }

    override suspend fun doRequestStream(payload: IntegrationPayload): IntegrationStream {

        val requestNChannel = Channel<Int>(Channel.UNLIMITED)
        val responseChannel =
            rSocket
                .requestStream(payload.fromIntegration())
                .flowOn(ChannelStrategy(requestNChannel))
                .map { it.toIntegration() }
                .produceIn(GlobalScope)

        return object : IntegrationStream {
            override fun request(n: Int) {
                requestNChannel.offer(n)
            }

            override suspend fun next(): IntegrationPayload? = responseChannel.receiveOrNull()
        }
    }

    override suspend fun doRequestChannel(stream: IntegrationStream): IntegrationStream {
        TODO("Not yet implemented")
    }
}
