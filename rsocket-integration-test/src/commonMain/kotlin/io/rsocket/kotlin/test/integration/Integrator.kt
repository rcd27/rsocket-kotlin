package io.rsocket.kotlin.test.integration

import io.rsocket.kotlin.test.*
import kotlin.test.*
import kotlin.time.*

typealias MetadataPushHandler = (metadata: ByteArray) -> Unit
typealias FireAndForgetHandler = (payload: IntegrationPayload) -> Unit
typealias RequestResponseHandler = (payload: IntegrationPayload) -> IntegrationPayload
typealias RequestStreamHandler = (payload: IntegrationPayload) -> IntegrationStream
typealias RequestChannelHandler = (stream: IntegrationStream) -> IntegrationStream

suspend fun Integrator(
    server: ServerImplementation,
    client: ClientImplementation,
    block: ConfigurationBuilder.() -> Unit = {}
): Integrator {
    val configuration = ConfigurationBuilder().apply(block).build()
    val ch = ChannelStorage()
    server.startServer(ch, configuration.server)
    val c = trySeveralTimes { client.connectClient(configuration.client) }
    return Integrator(c, ch)
}


class Integrator internal constructor(
    private val requester: Requester,
    private val handlerStorage: HandlerStorage
) {

    suspend fun metadataPush(metadata: ByteArray, handler: MetadataPushHandler) {
        handlerStorage.saveMetadataPushHandler(handler)
        requester.doMetadataPush(metadata)
    }

    suspend fun fireAndForget(payload: IntegrationPayload, handler: FireAndForgetHandler) {
        handlerStorage.saveFireAndForgetHandler(handler)
        requester.doFireAndForget(payload)
    }

    suspend fun requestResponse(payload: IntegrationPayload, handler: RequestResponseHandler): IntegrationPayload {
        handlerStorage.saveRequestResponseHandler(handler)
        return requester.doRequestResponse(payload)
    }

    suspend fun requestStream(payload: IntegrationPayload, handler: RequestStreamHandler): IntegrationStream {
        handlerStorage.saveRequestStreamHandler(handler)
        return requester.doRequestStream(payload)
    }

    suspend fun requestChannel(stream: IntegrationStream, handler: RequestChannelHandler): IntegrationStream {
        handlerStorage.saveRequestChannelHandler(handler)
        return requester.doRequestChannel(stream)
    }
}


suspend fun Integrator.checkRequestResponse(request: IntegrationPayload) {
    val mark = TimeSource.Monotonic.markNow()
    println("requestResponse[${mark.elapsedNow().inMilliseconds}]: initiate")
    val response = requestResponse(request) {
        println("requestResponse[${mark.elapsedNow().inMilliseconds}]: handler called")
        assertBytesEquals(request.data, it.data)
        assertBytesEquals(request.metadata, it.metadata)
        request
    }
    println("requestResponse[${mark.elapsedNow().inMilliseconds}]: response received")
    assertBytesEquals(request.data, response.data)
    assertBytesEquals(request.metadata, response.metadata)
}

suspend fun Integrator.checkRequestStream(request: IntegrationPayload, amount: Int) {
    val mark = TimeSource.Monotonic.markNow()
    println("requestStream[${mark.elapsedNow().inMilliseconds}]: initiate")
    val stream = requestStream(request) {
        println("requestStream[${mark.elapsedNow().inMilliseconds}]: handler called")
        assertBytesEquals(request.data, it.data)
        assertBytesEquals(request.metadata, it.metadata)
        var left = amount
        object : IntegrationStream {
            override fun request(n: Int) {
                //TODO not needed
            }

            override suspend fun next(): IntegrationPayload? {
                if (left <= 0) return null

                left -= 1
                return request
            }
        }
    }
    stream.request(amount)
    repeat(amount) {
        val response = assertNotNull(stream.next())
        println("requestStream[${mark.elapsedNow().inMilliseconds}]: response received")
        assertBytesEquals(request.data, response.data)
        assertBytesEquals(request.metadata, response.metadata)
    }
    assertNull(stream.next())
    println("requestStream[${mark.elapsedNow().inMilliseconds}]: response completed")
}
