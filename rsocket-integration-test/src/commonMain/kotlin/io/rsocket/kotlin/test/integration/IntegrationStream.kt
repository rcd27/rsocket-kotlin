package io.rsocket.kotlin.test.integration

interface IntegrationStream {
    fun request(n: Int)
    suspend fun next(): IntegrationPayload?
}
