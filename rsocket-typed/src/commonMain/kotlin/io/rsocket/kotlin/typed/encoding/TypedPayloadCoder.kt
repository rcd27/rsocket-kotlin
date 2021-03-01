/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.kotlin.typed.encoding

import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.typed.*

public class TypedPayloadCoder(
    public val dataCoder: DataCoder,
    public val metadataCoder: TypedMetadataCoder
) {

    public fun encode(payload: TypedPayload): Payload {
        val metadata = metadataCoder.encode(payload.metadata)
        val data = when (payload) {
            is DefaultTypedPayload -> dataCoder.encoders.encode(payload.type, payload.value, payload.metadata)
            is TypedPayloadWrapper -> payload.packet
        }
        return Payload(data, metadata)
    }

    public fun decode(payload: Payload): TypedPayload {
        val metadata = metadataCoder.decode(payload.metadata)
        return TypedPayloadWrapper(payload.data, metadata, dataCoder.decoders)
    }
}
