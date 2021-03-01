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
import kotlinx.coroutines.flow.*
import kotlin.reflect.*

public interface RoutableRSocket {
    public suspend fun <S : Any> metadataPush(selectorType: KType, selector: S, metadata: TypedMetadata)
    public suspend fun <S : Any> fireAndForget(selectorType: KType, selector: S, payload: TypedPayload)
    public suspend fun <S : Any> requestResponse(selectorType: KType, selector: S, payload: TypedPayload): TypedPayload
    public fun <S : Any> requestStream(selectorType: KType, selector: S, payload: TypedPayload): Flow<TypedPayload>
    public fun <S : Any> requestChannel(
        selectorType: KType, selector: S,
        initPayload: TypedPayload, payloads: Flow<TypedPayload>
    ): Flow<TypedPayload>
}
