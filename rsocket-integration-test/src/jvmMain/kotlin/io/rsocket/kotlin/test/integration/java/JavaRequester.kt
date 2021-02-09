package io.rsocket.kotlin.test.integration.java

import io.rsocket.*
import io.rsocket.kotlin.test.integration.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.reactive.*
import org.reactivestreams.*

class JavaRequester(
    private val rSocket: RSocket
) : Requester {

    override suspend fun doMetadataPush(metadata: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun doFireAndForget(payload: IntegrationPayload) {
        TODO("Not yet implemented")
    }

    override suspend fun doRequestResponse(payload: IntegrationPayload): IntegrationPayload {
        return rSocket.requestResponse(payload.fromIntegration()).awaitFirst().toIntegration()
    }

    override suspend fun doRequestStream(payload: IntegrationPayload): IntegrationStream {
        val responseChannel = Channel<IntegrationPayload>(Channel.UNLIMITED)
        var sub: Subscription? = null
        rSocket.requestStream(payload.fromIntegration()).subscribe(object : Subscriber<Payload> {
            override fun onSubscribe(s: Subscription?) {
                sub = s
            }

            override fun onNext(t: Payload) {
                responseChannel.offer(t.toIntegration())
            }

            override fun onError(t: Throwable) {
                responseChannel.close(t)
                responseChannel.cancel()
            }

            override fun onComplete() {
                responseChannel.close()
            }
        })
        return object : IntegrationStream {
            override fun request(n: Int) {
                sub?.request(n.toLong())
            }

            override suspend fun next(): IntegrationPayload? = responseChannel.receiveOrNull()
        }
    }

    override suspend fun doRequestChannel(stream: IntegrationStream): IntegrationStream {
        TODO("Not yet implemented")
    }
}
