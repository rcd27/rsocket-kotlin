package io.rsocket.kotlin.test.integration

fun IntegrationPayload(data: String, metadata: String? = null): IntegrationPayload =
    IntegrationPayload(data.encodeToByteArray(), metadata?.encodeToByteArray())

class IntegrationPayload(
    val data: ByteArray,
    val metadata: ByteArray? = null
)
