package io.rsocket.kotlin.test.integration.java

import io.ktor.util.*
import io.rsocket.*
import io.rsocket.kotlin.test.integration.*
import io.rsocket.util.*

fun Payload.toIntegration(): IntegrationPayload = IntegrationPayload(
    data.moveToByteArray(),
    if (hasMetadata()) metadata.moveToByteArray() else null
)

fun IntegrationPayload.fromIntegration(): Payload = DefaultPayload.create(data, metadata)
