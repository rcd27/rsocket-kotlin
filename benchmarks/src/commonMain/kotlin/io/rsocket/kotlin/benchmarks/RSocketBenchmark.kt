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

package io.rsocket.kotlin.benchmarks

import kotlinx.benchmark.*

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1)
@Measurement(iterations = 3, time = 1)
@State(Scope.Benchmark)
abstract class RSocketBenchmark<Payload : Any, RSocket : Any> : AbstractRSocketBenchmark<Payload, RSocket>() {
    @Param("0", "64", "1024", "131072", "1048576", "15728640")
    override var payloadSize: Int = 0

    @Benchmark
    fun requestResponseBlocking(bh: Blackhole) = blocking(bh, ::requestResponse)

    @Benchmark
    fun requestResponseParallel(bh: Blackhole) = parallel(bh, 1000, ::requestResponse)


    @Benchmark
    fun requestStreamBlocking(bh: Blackhole) = blocking(bh, ::requestStream)

    @Benchmark
    fun requestStreamParallel(bh: Blackhole) = parallel(bh, 10, ::requestStream)


    //    @Benchmark
    fun requestChannelBlocking(bh: Blackhole) = blocking(bh, ::requestChannel)

    //    @Benchmark
    fun requestChannelParallel(bh: Blackhole) = parallel(bh, 10, ::requestChannel)

}
