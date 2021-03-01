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

package io.rsocket.kotlin.typed.routing

import io.rsocket.kotlin.metadata.*
import io.rsocket.kotlin.typed.*
import io.rsocket.kotlin.typed.metadata.*
import io.rsocket.kotlin.typed.payload.*
import kotlin.reflect.*

public object StringRouteCoder : RouteDecoder, RouteEncoder {
    override fun decode(type: KType, metadata: TypedMetadata): Any? {
        if (type != typeOf<String>()) return null
        return metadata.getOrNull<RoutingMetadata>()?.tags?.firstOrNull()
    }

    override fun encode(type: KType, route: Any, metadataBuilder: TypedMetadataBuilder): Boolean {
        if (route !is String) return false
        metadataBuilder.add(RoutingMetadata(route))
        return true
    }
}

public object RMRouteCoder : RouteDecoder, RouteEncoder {
    override fun decode(type: KType, metadata: TypedMetadata): Any? {
        if (type != typeOf<RoutingMetadata>()) return null
        return metadata.getOrNull<RoutingMetadata>()
    }

    override fun encode(type: KType, route: Any, metadataBuilder: TypedMetadataBuilder): Boolean {
        if (route !is RoutingMetadata) return false
        metadataBuilder.add(route)
        return true
    }
}

public interface RouteDecoder {
    public fun decode(type: KType, metadata: TypedMetadata): Any?
}

public interface RouteEncoder {
    public fun encode(type: KType, route: Any, metadataBuilder: TypedMetadataBuilder): Boolean
}

public interface RouteRegistry {
    public fun registerRequestResponse(routeType: KType, block: suspend (route: Any) -> Unit)
    public fun registerRequestResponse(routeType: KType, route: Any, block: suspend () -> Unit)

    public fun build(): Router
}

public interface Router {
    public suspend fun requestResponse(routeType: KType, route: Any)
}

public class MapRouteRegistry : RouteRegistry {
    private val map1 = mutableMapOf<KType, suspend (route: Any) -> Unit>()
    private val map2 = mutableMapOf<Pair<KType, Any>, suspend () -> Unit>()
    override fun registerRequestResponse(routeType: KType, block: suspend (route: Any) -> Unit) {
        map1[routeType] = block
    }

    override fun registerRequestResponse(routeType: KType, route: Any, block: suspend () -> Unit) {
        map2[routeType to route] = block
    }

    override suspend fun callRequestResponse(routeType: KType, route: Any) {
        map2[routeType to route]?.invoke() ?: map1[routeType]?.invoke(route) ?: error("No routes registered")
    }
}

public interface RequestResponseEndpoint

@OptIn(ExperimentalStdlibApi::class)
public suspend inline fun <reified S : Any> RoutableRSocket.requestResponse(selector: S, payload: TypedPayload): TypedPayload =
    requestResponse(typeOf<S>(), selector, payload)

@OptIn(ExperimentalStdlibApi::class)
public suspend inline fun <reified S : Any, reified D : Any> RoutableRSocket.requestResponse(
    selector: S,
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
): TypedPayload =
    requestResponse(typeOf<S>(), selector, typedPayload(data, block))

@OptIn(ExperimentalStdlibApi::class)
public inline fun <reified S : Any> RoutableRSocketRequestHandlerBuilder.requestResponse(
    noinline block: suspend (selector: S, payload: TypedPayload) -> TypedPayload
): Unit = requestResponse(typeOf<S>(), block)

private suspend fun m() {
    lateinit var b1: TypedRSocketRequestHandlerBuilder
    lateinit var b2: RoutableRSocketRequestHandlerBuilder
    lateinit var b3: RoutableRSocketRequestHandlerBuilder
    lateinit var client1: TypedRSocket
    lateinit var client2: RoutableRSocket

    b1.requestResponse { (data: SomeData, metadata) ->
        when (val route = metadata?.route()) {
            "selector" -> {
                val meta = metadata.get<SomeMetaData>()
                typedPayload(SomeData("hello", meta.v2 + 1)) {
                    add(SomeMetaData(data.v1, 322 + route.length))
                }
            }
            else       -> error("")
        }
    }

    b2.requestResponse { selector: String, (data: SomeData, metadata) ->
        val (meta: SomeMetaData?) = metadata
        typedPayload(SomeData("hello", meta.v2 + 1)) {
            add(SomeMetaData(data.v1, 322 + selector.length))
        }
    }

    b2.requestResponse("selector") { (data: SomeData, metadata) ->
        val (meta: SomeMetaData) = metadata
        typedPayload(SomeData("hello", meta.v2 + 1)) {
            add(SomeMetaData(data.v1, 322))
        }
    }

    b2.requestResponse<SomeRoute> { (selector, meta), (data: SomeData) ->
        typedPayload(SomeData("hello", meta.v2 + 1)) {
            add(SomeMetaData(data.v1, 322 + selector.length))
        }
    }

    client1.requestResponse(SomeData("Hello", 1)) {
        routing("selector")
        add(SomeMetaData("123", 2))
    }
    client2.requestResponse(SomeRoute("selector", SomeMetaData("123", 2)), SomeData("Hello", 1))

    client2.requestResponse("route.2", SomeData("123", 2)) {
        add(SomeMetaData("123", 2))
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun RoutableRSocketRequestHandlerBuilder.m() {
    requestResponse { selector: String, (data: Int) ->
        typedPayload(data + 1)
    }
    requestResponse("hello world") { (data: Int) ->
        typedPayload(data + 1)
    }
    requestResponse<SomeRoute> { (s), (data: Int) ->
        typedPayload(data + s.length)
    }
}

private data class SomeRoute(val selector: String, val value: SomeMetaData)

private data class SomeMetaData(val v1: String, val v2: Int)
private data class SomeData(val v1: String, val v2: Int)
