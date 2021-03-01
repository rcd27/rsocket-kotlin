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
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.metadata.*
import io.rsocket.kotlin.typed.metadata.*
import io.rsocket.kotlin.typed.payload.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.*

public fun TypedMetadataBuilder.routing(vararg tags: String): Unit = add(RoutingMetadata(*tags))
public fun TypedMetadataBuilder.routing(tags: List<String>): Unit = add(RoutingMetadata(tags))

public fun TypedMetadata.routing(): List<String> = get<RoutingMetadata>().tags
public fun TypedMetadata.route(): String = routing().first()

private suspend fun m() {
    typedPayload(ASD("")) {
        add("")
        add(123)
        add(123)
        routing("123", "222")
        zipkinTracing(ZipkinTracingMetadata.Kind.Debug)
        bearerAuth("")
        simpleAuth("")
        rawAuth("")
        perStreamDataMimeType(WellKnownMimeType.ApplicationCapnProto)
        add(RoutingMetadata("123"))
        add(RoutingMetadata("123"))
        add(ZipkinTracingMetadata(ZipkinTracingMetadata.Kind.Debug))
    }
    lateinit var client: TypedRSocket

    val (data1: String) = client.requestResponse("123")
    val data2 = client.requestResponse("123").data<String>()

    client.requestResponse(typedPayload(""))
    client.requestResponse("") {
        routing("123")
    }

    client.requestStream(typedPayload("")).collect { (data: String) ->

    }

    flow {
        emitPayload(123)
        emitPayload("123")
    }

    flow<String> {

    }.map(::typedPayload)
}

