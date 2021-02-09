package io.rsocket.kotlin.test.integration.java

import io.ktor.util.*
import io.ktor.utils.io.core.*
import io.rsocket.*
import io.rsocket.kotlin.test.integration.*
import io.rsocket.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.*
import reactor.core.publisher.*

class JavaResponder(val handler: RequestHandler) : RSocket {
    override fun requestResponse(payload: Payload): Mono<Payload> = mono {
        handler.requestResponseHandler()(payload.toIntegration()).fromIntegration()
    }

    override fun requestStream(payload: Payload): Flux<Payload> = flux {
        val stream = handler.requestStreamHandler()(payload.toIntegration())
        var response: IntegrationPayload? = stream.next() ?: return@flux
        while (response != null) {
            send(response.fromIntegration())
            response = stream.next()
        }
    }
}
