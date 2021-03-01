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

package io.rsocket.kotlin.typed

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.metadata.*
import io.rsocket.kotlin.typed.encoding.*
import kotlin.reflect.*

//TODO sealed interface
public sealed class TypedMetadata : Closeable {
    @UnsafeTypedRSocketApi
    public abstract fun byType(type: KType): List<Any>
}

//TODO sealed interface
public interface TypedMetadataBuilder : Closeable {
    @UnsafeTypedRSocketApi
    public fun add(type: KType, value: Any)
}

public inline fun typedMetadata(block: TypedMetadataBuilder.() -> Unit): TypedMetadata {
    val builder = createTypedMetadataBuilder()
    try {
        builder.block()
        return builder
    } catch (cause: Throwable) {
        (builder as TypedMetadataBuilder).close()
        throw cause
    }
}

@OptIn(UnsafeTypedRSocketApi::class, ExperimentalStdlibApi::class)
public inline fun <reified T : Any> TypedMetadataBuilder.add(value: T) {
    add(typeOf<T>(), value)
}

@OptIn(UnsafeTypedRSocketApi::class, ExperimentalStdlibApi::class)
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : Any> TypedMetadata.byType(): List<T> = byType(typeOf<T>()) as List<T>
public inline fun <reified T : Any> TypedMetadata.has(): Boolean = byType<T>().isNotEmpty()
public inline fun <reified T : Any> TypedMetadata.getOrNull(): T? = byType<T>().firstOrNull()
public inline fun <reified T> TypedMetadata.get(): T = internalGet()

public inline operator fun <reified T> TypedMetadata.component1(): T = get()
public inline operator fun <reified T> TypedMetadata.component2(): T = get()
public inline operator fun <reified T> TypedMetadata.component3(): T = get()
public inline operator fun <reified T> TypedMetadata.component4(): T = get()
public inline operator fun <reified T> TypedMetadata.component5(): T = get()

// ---------- implementation details ----------

@PublishedApi
@OptIn(UnsafeTypedRSocketApi::class, ExperimentalStdlibApi::class)
internal inline fun <reified T> TypedMetadata.internalGet(): T = internalGet(typeOf<T>())

@PublishedApi
@UnsafeTypedRSocketApi
@Suppress("UNCHECKED_CAST")
internal fun <T> TypedMetadata.internalGet(type: KType): T {
    val list = byType(type)
    if (list.isNotEmpty()) return list[0] as T
    if (type.isMarkedNullable) return null as T
    throw NoSuchElementException("No metadata with type '$type' found")
}

@PublishedApi
internal fun createTypedMetadataBuilder(): TypedMetadataFromBuilder = TypedMetadataFromBuilder()

internal class TypedMetadataFromBuilder : TypedMetadata(), TypedMetadataBuilder {
    private val _entries = mutableListOf<Pair<KType, Any>>()
    val entries: List<Pair<KType, Any>> get() = _entries

    @UnsafeTypedRSocketApi
    override fun add(type: KType, value: Any) {
        _entries += type to value
    }

    @UnsafeTypedRSocketApi
    override fun byType(type: KType): List<Any> {
        return _entries.filter { it.first == type }
    }

    override fun close() {
        _entries.forEach { (_, entry) ->
            if (entry is Closeable) entry.close()
        }
    }
}

@ExperimentalMetadataApi
internal class TypedMetadataWrapper<M : Metadata>(
    val metadata: M,
    private val decoders: List<MetadataEntryDecoder<M>>
) : TypedMetadata() {
    @UnsafeTypedRSocketApi
    override fun byType(type: KType): List<Any> {
        return decoders.decode(type, metadata)
    }

    override fun close() {
        metadata.close()
    }
}
