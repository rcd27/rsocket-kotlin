package io.rsocket.kotlin.test.integration.kotlin

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.features.websocket.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.test.integration.*
import io.rsocket.kotlin.transport.ktor.client.*

class KotlinClientWsImplementation(
    private val clientEngine: HttpClientEngineFactory<*>
) : ClientImplementation {

    override suspend fun connectClient(configuration: ClientConfiguration): Requester {
        val client = HttpClient(clientEngine) {
            install(WebSockets)
            install(RSocketSupport) {
                connector = RSocketConnector {
                    maxFragmentSize = configuration.maxFragmentSize
                }
            }
        }
        val rSocket = client.rSocket(port = configuration.port)
        return KotlinRequester(rSocket)
    }
}
