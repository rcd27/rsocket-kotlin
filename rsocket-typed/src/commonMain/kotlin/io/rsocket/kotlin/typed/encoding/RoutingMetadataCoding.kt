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
import kotlin.native.concurrent.*
import kotlin.reflect.*

@SharedImmutable
@ExperimentalMetadataApi
private val routingMetadataCoder = TypedMetadataCoder(RoutingMetadata, RoutingMetadataWriter) {
    coders(DefaultRoutingMetadataCoder)
}

@Suppress("FunctionName")
@ExperimentalMetadataApi
public fun RoutingMetadataCoder(): TypedMetadataCoder = routingMetadataCoder

@ExperimentalMetadataApi
public object RoutingMetadataWriter : MetadataWriter<MutableList<String>> {
    override fun create(): MutableList<String> = mutableListOf()
    override fun write(builder: MutableList<String>): ByteReadPacket = RoutingMetadata(builder.toList()).toPacket()
}

@ExperimentalMetadataApi
public object DefaultRoutingMetadataCoder : MetadataEntryEncoder<MutableList<String>>, MetadataEntryDecoder<RoutingMetadata> {
    @OptIn(ExperimentalStdlibApi::class)
    private val routingType = typeOf<RoutingMetadata>()

    override fun encode(type: KType, value: Any, metadataBuilder: MutableList<String>): Boolean {
        if (value !is RoutingMetadata) return false
        metadataBuilder += value.tags
        return true
    }

    override fun decode(type: KType, metadata: RoutingMetadata): List<Any>? {
        if (type != routingType) return null
        return listOf(metadata)
    }
}
