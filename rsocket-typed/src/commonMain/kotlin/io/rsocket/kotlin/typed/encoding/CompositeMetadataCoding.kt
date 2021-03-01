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
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.metadata.*
import io.rsocket.kotlin.metadata.security.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

@ExperimentalMetadataApi
@OptIn(ExperimentalStdlibApi::class)
private val builtInReaders = buildMap {
    add(RoutingMetadata)
    add(ZipkinTracingMetadata)
    add(PerStreamDataMimeTypeMetadata)
    add(PerStreamAcceptableDataMimeTypesMetadata)
    add(SimpleAuthMetadata)
    add(BearerAuthMetadata)
}

@SharedImmutable
@ExperimentalMetadataApi
private val builtInEntryDecoder = CompositeMetadataEntryDecoder {}

@Suppress("FunctionName")
@ExperimentalMetadataApi
public fun CompositeMetadataCoder(block: TypedMetadataCoderBuilder<CompositeMetadata, CompositeMetadataBuilder>.() -> Unit = {}): TypedMetadataCoder =
    TypedMetadataCoder(CompositeMetadata, CompositeMetadataWriter) {
        decoders(builtInEntryDecoder)
        encoders(CompositeMetadataEntryEncoder)
        block()
    }

@ExperimentalMetadataApi
public object CompositeMetadataWriter : MetadataWriter<CompositeMetadataBuilder> {
    public override fun create(): CompositeMetadataBuilder = CompositeMetadataFromBuilder()
    public override fun write(builder: CompositeMetadataBuilder): ByteReadPacket = builder.build().toPacket()
}

@ExperimentalMetadataApi
public object CompositeMetadataEntryEncoder : MetadataEntryEncoder<CompositeMetadataBuilder> {
    override fun encode(type: KType, value: Any, metadataBuilder: CompositeMetadataBuilder): Boolean {
        if (value !is Metadata) return false
        metadataBuilder.add(value)
        return true
    }
}

@ExperimentalMetadataApi
public class CompositeMetadataEntryDecoder internal constructor(
    private val readers: Map<KType, MetadataReader<*>>
) : MetadataEntryDecoder<CompositeMetadata> {
    override fun decode(type: KType, metadata: CompositeMetadata): List<Any>? = readers[type]?.let { metadata.list(it) }
}

@ExperimentalMetadataApi
public fun CompositeMetadataEntryDecoder(block: CompositeMetadataEntryDecoderBuilder.() -> Unit): CompositeMetadataEntryDecoder {
    val builder = CompositeMetadataEntryDecoderBuilder()
    builder.block()
    return builder.build()
}

@ExperimentalMetadataApi
public class CompositeMetadataEntryDecoderBuilder internal constructor() {
    private val readers = mutableMapOf<KType, MetadataReader<*>>()

    @OptIn(ExperimentalStdlibApi::class)
    public inline fun <reified M : Metadata> add(reader: MetadataReader<M>) {
        add(typeOf<M>(), reader)
    }

    @PublishedApi
    internal fun add(type: KType, reader: MetadataReader<*>) {
        readers[type] = reader
    }

    internal fun build(): CompositeMetadataEntryDecoder = CompositeMetadataEntryDecoder(builtInReaders + readers)
}

@ExperimentalMetadataApi
@OptIn(ExperimentalStdlibApi::class)
private inline fun <reified M : Metadata> MutableMap<KType, MetadataReader<*>>.add(reader: MetadataReader<M>) {
    put(typeOf<M>(), reader)
}

@ExperimentalMetadataApi
private class CompositeMetadataFromBuilder : CompositeMetadataBuilder, CompositeMetadata {
    private val _entries = mutableListOf<CompositeMetadata.Entry>()

    override val entries: List<CompositeMetadata.Entry> get() = _entries

    override fun add(mimeType: MimeType, metadata: ByteReadPacket) {
        _entries += CompositeMetadata.Entry(mimeType, metadata)
    }

    override fun add(metadata: Metadata) {
        _entries += CompositeMetadata.Entry(metadata)
    }

    override fun build(): CompositeMetadata = this
}
