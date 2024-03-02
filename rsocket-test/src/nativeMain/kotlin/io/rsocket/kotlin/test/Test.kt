/*
 * Copyright 2015-2024 the original author or authors.
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

package io.rsocket.kotlin.test

import kotlinx.coroutines.*
import kotlin.experimental.*
import kotlin.native.*

actual annotation class IgnoreJs
actual annotation class IgnoreJvm
actual typealias IgnoreNative = kotlin.test.Ignore

actual val anotherDispatcher: CoroutineDispatcher get() = newSingleThreadContext("another")

@OptIn(ExperimentalNativeApi::class)
actual fun identityHashCode(instance: Any): Int = instance.identityHashCode()
