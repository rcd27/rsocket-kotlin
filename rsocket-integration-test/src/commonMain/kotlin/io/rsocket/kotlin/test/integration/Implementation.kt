package io.rsocket.kotlin.test.integration

import kotlinx.coroutines.*

interface ServerImplementation {
    suspend fun startServer(requestHandler: RequestHandler, configuration: ServerConfiguration)
}

interface ClientImplementation {
    suspend fun connectClient(configuration: ClientConfiguration): Requester
}

internal suspend inline fun <R> trySeveralTimes(block: () -> R): R {
    lateinit var error: Throwable
    repeat(10) {
        try {
            return block()
        } catch (e: Throwable) {
            error = e
            delay(500) //sometimes address isn't yet available (server isn't started)
        }
    }
    throw error
}
