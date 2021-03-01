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

package io.rsocket.kotlin.typed.serialization

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS)
public annotation class RSocketRoute(vararg val tags: String)

@Target(AnnotationTarget.PROPERTY)
public annotation class RSocketRouteTag(val index: Int)

@Serializable
@RSocketRoute("route.{s}/{route}", "hello.tag.{route}")
public data class SomeRoute(
    val s: String,
    val v: Int,
    val route: Some2Route
)

@Serializable
@RSocketRoute("route.{s}")
public data class Some2Route(
    val s: String,
    val l: Int
)


@Serializable
@RSocketRoute("api/communities/{id}", "other/route/{id}")
public data class CommRoute(
    val id: String
)

@Serializable
@RSocketRoute("{comm}/shipments/{id}", "super/other/route.{id}")
public data class ShipmentsRoute(
    val id: String,
    val comm: CommRoute
)
