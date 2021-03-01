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
import kotlin.reflect.*

@ExperimentalMetadataApi
public fun interface MetadataEntryDecoder<M : Metadata> {
    public fun decode(type: KType, metadata: M): List<Any>?
}

@ExperimentalMetadataApi
public fun interface MetadataEntryEncoder<B : Any> {
    public fun encode(type: KType, value: Any, metadataBuilder: B): Boolean //true if encoded
}

@ExperimentalMetadataApi
public interface MetadataWriter<B : Any> {
    public fun create(): B
    public fun write(builder: B): ByteReadPacket
}

@ExperimentalMetadataApi
internal inline fun <B : Any> MetadataWriter<B>.build(block: B.() -> Unit): ByteReadPacket {
    val builder = create()
    try {
        builder.block()
        return write(builder)
    } catch (cause: Throwable) {
        if (builder is Closeable) builder.close()
        throw cause
    }
}

@ExperimentalMetadataApi
internal fun <M : Metadata> List<MetadataEntryDecoder<M>>.decode(type: KType, metadata: M): List<Any> {
    forEach {
        val data = it.decode(type, metadata)
        if (data != null) return data
    }
    throw IllegalArgumentException("No MetadataEntryDecoder found for type '$type' and metadata '$metadata'")
}

@ExperimentalMetadataApi
internal fun <B : Any> List<MetadataEntryEncoder<B>>.encode(type: KType, value: Any, metadataBuilder: B) {
    forEach {
        if (it.encode(type, value, metadataBuilder)) return
    }
    throw IllegalArgumentException("No MetadataEntryEncoder found for type '$type' and value class '${value::class}'")
}
