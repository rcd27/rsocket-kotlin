package io.rsocket.kotlin.test.integration.kotlin

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.test.integration.*

fun Payload.toIntegration(): IntegrationPayload = IntegrationPayload(data.readBytes(), metadata?.readBytes())

fun IntegrationPayload.fromIntegration(): Payload = buildPayload {
    data(data)
    if (metadata != null) metadata(metadata)
}
