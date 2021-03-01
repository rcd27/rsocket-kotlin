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

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.typed.*
import kotlin.reflect.*

public fun interface DataDecoder {
    public fun decode(type: KType, metadata: TypedMetadata?, packet: ByteReadPacket): Any?
}

public fun interface DataEncoder {
    public fun encode(type: KType, value: Any, metadata: TypedMetadata?, dataBuilder: BytePacketBuilder): Boolean
}

internal fun List<DataDecoder>.decode(type: KType, metadata: TypedMetadata?, packet: ByteReadPacket): Any {
    forEach {
        val data = it.decode(type, metadata, packet)
        if (data != null) return data
    }
    throw IllegalArgumentException("No DataDecoder found for type '$type' and metadata '$metadata'")
}

internal fun List<DataEncoder>.encode(type: KType, value: Any, metadata: TypedMetadata?, dataBuilder: BytePacketBuilder) {
    forEach {
        if (it.encode(type, value, metadata, dataBuilder)) return
    }
    throw IllegalArgumentException("No DataEncoder found for type '$type', value class '${value::class}' and metadata '$metadata'")
}

internal fun List<DataEncoder>.encode(type: KType, value: Any, metadata: TypedMetadata?): ByteReadPacket = buildPacket {
    encode(type, value, metadata, this)
}

public interface TypedMetadataEncodingInterceptor {
    // to add smth to metadata, f.e. tracing on each call, or depending on data
    public fun intercept(dataType: KType, data: Any, metadata: TypedMetadata?): List<Any>
}
