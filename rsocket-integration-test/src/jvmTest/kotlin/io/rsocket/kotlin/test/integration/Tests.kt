package io.rsocket.kotlin.test.integration

import io.rsocket.kotlin.test.integration.java.*
import io.rsocket.kotlin.test.integration.kotlin.*
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.server.cio.CIO as ServerCIO

class KotlinServerJavaClientTcpTest : IntegrationTest(KotlinServerTcpImplementation, JavaTcpImplementation)
class KotlinClientJavaServerTcpTest : IntegrationTest(JavaTcpImplementation, KotlinClientTcpImplementation)

class KotlinServerJavaClientWsTest : IntegrationTest(KotlinServerWsImplementation(ServerCIO), JavaWsImplementation)
class KotlinClientJavaServerWsTest : IntegrationTest(JavaWsImplementation, KotlinClientWsImplementation(ClientCIO))
