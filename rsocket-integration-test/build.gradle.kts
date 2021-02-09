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

plugins {
    kotlin("multiplatform")
    id("kotlinx-atomicfu")
}

val ktorVersion: String by rootProject
val rsocketJavaVersion: String by rootProject
val kotlinxCoroutinesVersion: String by rootProject

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":rsocket-test"))
                implementation(project(":rsocket-transport-ktor-client"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":rsocket-transport-ktor-server"))

                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-server-cio:$ktorVersion")

                //rsocket-java + coroutines reactor adapter for it
                implementation("io.rsocket:rsocket-transport-netty:$rsocketJavaVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
            }
        }
    }
}
