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
import io.rsocket.kotlin.*
import io.rsocket.kotlin.metadata.*
import io.rsocket.kotlin.typed.*

public interface TypedMetadataCoder {
    public fun encode(metadata: TypedMetadata?): ByteReadPacket?
    public fun decode(metadata: ByteReadPacket?): TypedMetadata?
}

@ExperimentalMetadataApi
public fun <M : Metadata, B : Any> TypedMetadataCoder(
    reader: MetadataReader<M>,
    writer: MetadataWriter<B>,
    block: TypedMetadataCoderBuilder<M, B>.() -> Unit
): TypedMetadataCoder {
    val builder = TypedMetadataCoderBuilder<M, B>()
    builder.block()
    return builder.build(reader, writer)
}

@ExperimentalMetadataApi
public class TypedMetadataCoderBuilder<M : Metadata, B : Any> internal constructor() {
    private val allDecoders: MutableList<MetadataEntryDecoder<M>> = mutableListOf()
    private val allEncoders: MutableList<MetadataEntryEncoder<B>> = mutableListOf()

    public fun decoder(decoder: MetadataEntryDecoder<M>) {
        allDecoders += decoder
    }

    public fun decoders(vararg decoders: MetadataEntryDecoder<M>) {
        allDecoders += decoders
    }

    public fun decoders(decoders: List<MetadataEntryDecoder<M>>) {
        allDecoders += decoders
    }

    public fun encoder(encoder: MetadataEntryEncoder<B>) {
        allEncoders += encoder
    }

    public fun encoders(vararg encoders: MetadataEntryEncoder<B>) {
        allEncoders += encoders
    }

    public fun encoders(encoders: List<MetadataEntryEncoder<B>>) {
        allEncoders += encoders
    }

    public fun <T> coders(vararg coders: T) where T : MetadataEntryEncoder<B>, T : MetadataEntryDecoder<M> {
        allEncoders += coders
        allDecoders += coders
    }

    public fun <T> coders(coders: List<T>) where T : MetadataEntryEncoder<B>, T : MetadataEntryDecoder<M> {
        allEncoders += coders
        allDecoders += coders
    }

    internal fun build(reader: MetadataReader<M>, writer: MetadataWriter<B>): TypedMetadataCoder =
        TypedMetadataCoderFromBuilder(reader, writer, allDecoders.toList(), allEncoders.toList())
}

@ExperimentalMetadataApi
private class TypedMetadataCoderFromBuilder<M : Metadata, B : Any>(
    private val reader: MetadataReader<M>,
    private val writer: MetadataWriter<B>,
    private val decoders: List<MetadataEntryDecoder<M>>,
    private val encoders: List<MetadataEntryEncoder<B>>
) : TypedMetadataCoder {
    override fun encode(metadata: TypedMetadata?): ByteReadPacket? = when (metadata) {
        is TypedMetadataFromBuilder -> writer.build {
            metadata.entries.forEach { (type, value) ->
                encoders.encode(type, value, this)
            }
        }
        is TypedMetadataWrapper<*>  -> metadata.metadata.toPacket()
        null                        -> null
    }

    override fun decode(metadata: ByteReadPacket?): TypedMetadata? {
        return metadata?.read(reader)?.let { TypedMetadataWrapper(it, decoders) }
    }
}
