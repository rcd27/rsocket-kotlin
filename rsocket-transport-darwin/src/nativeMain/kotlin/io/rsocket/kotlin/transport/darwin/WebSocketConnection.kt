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

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.*
import kotlin.coroutines.*

@SharedImmutable
private val completionHandler: (NSError?) -> Unit = {
    println(it)
}

internal fun ByteReadPacket.toNSData(): NSData = NSMutableData().apply {
    if (isEmpty) return@apply
    val bytes = readBytes()
    bytes.usePinned {
        appendBytes(it.addressOf(0), bytes.size.convert())
    }
}

internal fun NSData.toPacket(): ByteReadPacket {
    println(length.toString())
    println(length.toString().toInt())
    return buildPacket {
        writeFully(bytes!!.reinterpret(), length.toString().toInt())
    }
}

@TransportApi
public class WebSocketConnection(private val webSocket: NSURLSessionWebSocketTask) : Connection {
    override val job: CompletableJob = Job()

    private val channel = Channel<ByteReadPacket>(Channel.UNLIMITED)

    init {
        listen()
    }

    override suspend fun send(packet: ByteReadPacket) {
        val message = NSURLSessionWebSocketMessage(packet.toNSData())
        webSocket.sendMessage(message, completionHandler)
    }

    override suspend fun receive(): ByteReadPacket = channel.receive()

    private fun listen() {
        webSocket.receiveMessageWithCompletionHandler { message, nsError ->
            println(message)
            println(nsError)
            when {
                nsError != null -> job.completeExceptionally(Throwable(nsError.description))
                message != null -> channel.offer(message.data!!.toPacket())
            }
            listen()
        }
    }
}

fun test() {
    val url = ""
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
}
