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

import io.rsocket.kotlin.typed.*
import kotlin.experimental.*
import kotlin.reflect.*

public interface RoutableRSocketRequestHandlerBuilder {
    public fun <S : Any> requestResponse(selectorType: KType, selector: S, block: suspend (payload: TypedPayload) -> TypedPayload)
    public fun <S : Any> requestResponse(selectorType: KType, block: suspend (selector: S, payload: TypedPayload) -> TypedPayload)
}

//@OptIn(ExperimentalTypeInference::class)
//@OverloadResolutionByLambdaReturnType
//public inline fun <reified S : Any> RoutableRSocketRequestHandlerBuilder.requestResponse(
//    noinline block: (route: S, payload: TypedPayload) -> TypedPayload
//): Unit = TODO()

//@OptIn(ExperimentalStdlibApitalTypeInference::class)
//@OverloadResolutionByLambdaReturnType
//public inline fun <reified S : Any, reified R : Any> RoutableRSocketRequestHandlerBuilder.requestResponse(
//    noinline block: (route: S, payload: TypedPayload) -> R
//): Unit = TODO()

private fun RoutableRSocketRequestHandlerBuilder.b() {
    requestResponse { route: String, (data: String) ->
        123
    }
}
