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
import kotlin.native.concurrent.*
import kotlin.reflect.*

@OptIn(ExperimentalStdlibApi::class)
private val stringType = typeOf<String>()

@OptIn(ExperimentalStdlibApi::class)
private val charSequenceType = typeOf<CharSequence>()

@OptIn(ExperimentalStdlibApi::class)
private val byteArrayType = typeOf<ByteArray>()

@OptIn(ExperimentalStdlibApi::class)
private val byteReadPacketType = typeOf<ByteReadPacket>()

@SharedImmutable
private val builtInDecoder = DataDecoder { type, _, packet ->
    when (type) {
        stringType, charSequenceType -> packet.readText()
        byteArrayType                -> packet.readBytes()
        byteReadPacketType           -> packet
        else                         -> null
    }
}

@SharedImmutable
private val builtInEncoder = DataEncoder { _, value, _, dataBuilder ->
    when (value) {
        is CharSequence   -> dataBuilder.writeText(value)
        is ByteArray      -> dataBuilder.writeFully(value)
        is ByteReadPacket -> dataBuilder.writePacket(value)
        else              -> return@DataEncoder false
    }
    true
}

public class DataCoder internal constructor(
    internal val decoders: List<DataDecoder>,
    internal val encoders: List<DataEncoder>,
)

public class DataCoderBuilder internal constructor() {

    public fun e() {
        encoder { type, value, metadata, dataBuilder ->
            if (value !is String) return@encoder false
            dataBuilder.writeText(value)
            true
        }

        @OptIn(ExperimentalStdlibApi::class)
        decoder { type, metadata, packet ->
            if (type != typeOf<String>()) return@decoder null
            packet.readText()
        }
    }

    public fun encoder(encoder: DataEncoder) {

    }

    public fun encoders(vararg encoders: DataEncoder) {

    }

    public fun encoders(encoders: List<DataEncoder>) {

    }

    public fun decoder(decoder: DataDecoder) {

    }

    public fun decoders(vararg decoders: DataDecoder) {

    }

    public fun decoders(decoders: List<DataDecoder>) {

    }

    public fun <T> coders(vararg coders: T) where T : DataEncoder, T : DataDecoder {

    }

    public fun <T> coders(coders: List<T>) where T : DataEncoder, T : DataDecoder {

    }

}
