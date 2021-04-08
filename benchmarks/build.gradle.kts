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

import org.jetbrains.kotlin.gradle.targets.jvm.*

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark")
    kotlin("plugin.allopen")
}

val ktorVersion: String by rootProject
val rsocketJavaVersion: String by rootProject
val kotlinxCoroutinesVersion: String by rootProject
val kotlinxBenchmarkVersion: String by rootProject

kotlin {
    fun KotlinJvmTarget.ir() {
        compilations.all { kotlinOptions.useIR = true }
    }

    jvm("javaJvm").ir()
    jvm("kotlinJvm").ir()
    js("kotlinJs", LEGACY) { nodejs() }
    macosX64("kotlinNative")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:$kotlinxBenchmarkVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
            }
        }

        val kotlinCommonMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":rsocket-core"))
                implementation(project(":rsocket-transport-local"))
                implementation(project(":rsocket-transport-ktor"))
            }
        }

        val kotlinJsMain by getting {
            dependsOn(kotlinCommonMain)
        }

        val kotlinNativeMain by getting {
            dependsOn(kotlinCommonMain)
        }

        val kotlinJvmMain by getting {
            dependsOn(kotlinCommonMain)
            dependencies {
                // WS support
                implementation(project(":rsocket-transport-ktor-client"))
                implementation(project(":rsocket-transport-ktor-server"))
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-server-cio:$ktorVersion")
            }
        }

        val javaJvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
                implementation("io.rsocket:rsocket-core:$rsocketJavaVersion")
                implementation("io.rsocket:rsocket-transport-local:$rsocketJavaVersion")
                implementation("io.rsocket:rsocket-transport-netty:$rsocketJavaVersion")
            }
        }
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("javaJvm")
        register("kotlinJvm")
        register("kotlinJs")
        register("kotlinNative")
    }
}

//tasks.register<JavaExec>("jmhProfilers") {
//    group = "benchmark"
//    description = "Lists the available JMH profilers"
//    classpath = (kotlin.targets["jvm"].compilations["main"] as KotlinJvmCompilation).runtimeDependencyFiles
//    main = "org.openjdk.jmh.Main"
//    args("-lprof")
//}

//fun registerJmhGCTask(target: String): TaskProvider<*> = tasks.register<JavaExec>("${target}BenchmarkGC") {
//    group = "benchmark"
//
//    val buildFolder = buildDir.resolve("benchmarks").resolve(target)
//    val compilation = (kotlin.targets[target].compilations["main"] as KotlinJvmCompilation)
//    classpath(
//        file(buildFolder.resolve("classes")),
//        file(buildFolder.resolve("resources")),
//        compilation.runtimeDependencyFiles,
//        compilation.output.allOutputs
//    )
//    main = "org.openjdk.jmh.Main"
//
//    dependsOn("${target}BenchmarkCompile")
//    args("-prof", "gc")
//    args("-jvmArgsPrepend", "-Xmx3072m")
//    args("-jvmArgsPrepend", "-Xms3072m")
//    args("-foe", "true") //fail-on-error
//    args("-v", "NORMAL") //verbosity [SILENT, NORMAL, EXTRA]
//    args("-rf", "json")
//    args("-rff", project.file("build/reports/benchmarks/$target/result.json").also { it.parentFile.mkdirs() })
//}

//val t1 = registerJmhGCTask("java")
//val t2 = registerJmhGCTask("kotlin")

//tasks.register("benchmarkGC") {
//    group = "benchmark"
//    dependsOn(t1.get())
//    dependsOn(t2.get())
//}
