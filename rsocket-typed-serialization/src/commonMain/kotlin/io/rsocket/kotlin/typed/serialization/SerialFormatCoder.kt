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

package io.rsocket.kotlin.typed.serialization

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.typed.*
import io.rsocket.kotlin.typed.encoding.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

@ExperimentalSerializationApi
public class SerialFormatCoder(
    private val format: BinaryFormat
) : DataDecoder, DataEncoder {
    private val cache = SerializersCache(format.serializersModule)

    override fun decode(type: KType, metadata: TypedMetadata?, packet: ByteReadPacket): Any? {
        val serializer = cache.serializer(type) ?: return null
        return format.decodeFromByteArray(serializer, packet.readBytes())
    }

    override fun encode(type: KType, value: Any, metadata: TypedMetadata?, dataBuilder: BytePacketBuilder): Boolean {
        val serializer = cache.serializer(type) ?: return false
        dataBuilder.writeFully(format.encodeToByteArray(serializer, value))
        return true
    }
}

// TODO:
//  JVM    - concurrent map
//  JS     - simple map
//  Native - some freezable map
internal class SerializersCache(
    private val serializersModule: SerializersModule
) {
    private val cache = mutableMapOf<KType, KSerializer<Any?>>()

    fun serializer(type: KType): KSerializer<Any?>? {
        val cached = cache[type]
        return when {
            cached == null            -> {
                val serializer = serializersModule.serializerOrNull(type)
                cache[type] = serializer ?: NoopSerializer
                serializer
            }
            cached === NoopSerializer -> null
            else                      -> cached
        }
    }
}

// needed to fill cache with some value to distinguish nulls vs already checked types
@SharedImmutable
private val NoopSerializer = object : KSerializer<Any?> {
    override val descriptor: SerialDescriptor get() = error("never used")
    override fun deserialize(decoder: Decoder): Any = error("never used")
    override fun serialize(encoder: Encoder, value: Any?): Unit = error("never used")
}
