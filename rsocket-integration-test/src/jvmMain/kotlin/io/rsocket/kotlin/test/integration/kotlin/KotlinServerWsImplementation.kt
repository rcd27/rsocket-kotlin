package io.rsocket.kotlin.test.integration.kotlin

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.test.integration.*
import io.rsocket.kotlin.transport.ktor.server.*

class KotlinServerWsImplementation(
    private val serverEngine: ApplicationEngineFactory<*, *>
) : ServerImplementation {
    override suspend fun startServer(requestHandler: RequestHandler, configuration: ServerConfiguration) {
        embeddedServer(serverEngine, configuration.port) {
            install(WebSockets)
            install(RSocketSupport) {
                server = RSocketServer {
                    maxFragmentSize = configuration.maxFragmentSize
                }
            }
            routing {
                rSocket {
                    KotlinResponder(requestHandler)
                }
            }
        }.start()
    }

}
