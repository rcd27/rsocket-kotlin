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

package io.rsocket.kotlin.transport.darwin

import io.rsocket.kotlin.test.*
import io.rsocket.kotlin.transport.*
import platform.Foundation.*
import platform.darwin.*

class WebSocketTransportTest : TransportTest() {
    override suspend fun before(): Unit {
        val url = "ws://0.0.0.0:9000"
        val socketEndpoint = NSURL.URLWithString(url)!!
        val urlSession = NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration(),
            delegate = object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didOpenWithProtocol: String?
                ) {
                    println("open")
                }

                override fun URLSession(
                    session: NSURLSession,
                    webSocketTask: NSURLSessionWebSocketTask,
                    didCloseWithCode: NSURLSessionWebSocketCloseCode,
                    reason: NSData?
                ) {
                    println("close: $reason")
                }
            },
            delegateQueue = NSOperationQueue.currentQueue()
        )
        val webSocket = urlSession.webSocketTaskWithURL(socketEndpoint)
        val connection = WebSocketConnection(webSocket)
        webSocket.resume()
        client = CONNECTOR.connect(ClientTransport { connection })
    }
}
