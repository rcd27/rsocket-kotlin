package io.rsocket.kotlin.test.integration

interface Requester {
    suspend fun doMetadataPush(metadata: ByteArray)
    suspend fun doFireAndForget(payload: IntegrationPayload)
    suspend fun doRequestResponse(payload: IntegrationPayload): IntegrationPayload
    suspend fun doRequestStream(payload: IntegrationPayload): IntegrationStream
    suspend fun doRequestChannel(stream: IntegrationStream): IntegrationStream
}
