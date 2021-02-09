package io.rsocket.kotlin.test.integration

import io.rsocket.kotlin.test.*
import kotlinx.atomicfu.*
import kotlin.random.*
import kotlin.test.*

abstract class IntegrationTest(
    private val serverImplementation: ServerImplementation,
    private val clientImplementation: ClientImplementation
) : SuspendTest {
    suspend fun integrator(config: ConfigurationBuilder.() -> Unit): Integrator =
        Integrator(serverImplementation, clientImplementation, config)

    @Test
    fun requestResponseNoFragmentation() = test {
        integrator { port = PORT.incrementAndGet() }.apply {
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
        }
    }

    @Test
    fun requestResponseClientFragmentation() = test {
        integrator {
            port = PORT.incrementAndGet()
            client.maxFragmentSize = 1000
        }.apply {
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
        }
    }

    @Test
    fun requestResponseServerFragmentation() = test {
        integrator {
            port = PORT.incrementAndGet()
            server.maxFragmentSize = 1000
        }.apply {
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
        }
    }

    @Test
    fun requestResponseBothFragmentation() = test {
        integrator {
            port = PORT.incrementAndGet()
            server.maxFragmentSize = 1000
            client.maxFragmentSize = 1000
        }.apply {
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
            checkRequestResponse(IntegrationPayload(ByteArray(16000) { it.toByte() }))
        }
    }

    @Test
    fun requestStreamNoFragmentation() = test {
        integrator { port = PORT.incrementAndGet() }.apply {
            listOf(10, 100, 1000).forEach {
                checkRequestStream(IntegrationPayload(ByteArray(16000) { it.toByte() }), it)
            }
        }
    }

    companion object {
        private val PORT = atomic(Random.nextInt(20, 90) * 100)
    }
}
