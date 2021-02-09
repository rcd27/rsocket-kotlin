package io.rsocket.kotlin.test.integration.kotlin

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.test.*
import io.rsocket.kotlin.test.integration.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

class KotlinResponder(val handler: RequestHandler) : RSocket {
    override val job: CompletableJob = Job()

    override suspend fun requestResponse(payload: Payload): Payload {
        return handler.requestResponseHandler()(payload.toIntegration()).fromIntegration()
    }

    override fun requestStream(payload: Payload): Flow<Payload> = flow<Payload> {
        val stream = handler.requestStreamHandler()(payload.toIntegration())
        var response: IntegrationPayload? = stream.next() ?: return@flow
        while (response != null) {
            emit(response.fromIntegration())
            response = stream.next()
        }
    }

}
