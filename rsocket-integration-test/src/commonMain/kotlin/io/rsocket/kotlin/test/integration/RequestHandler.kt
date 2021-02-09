package io.rsocket.kotlin.test.integration

interface RequestHandler {
    suspend fun metadataPushHandler(): MetadataPushHandler
    suspend fun fireAndForgetHandler(): FireAndForgetHandler
    suspend fun requestResponseHandler(): RequestResponseHandler
    suspend fun requestStreamHandler(): RequestStreamHandler
    suspend fun requestChannelHandler(): RequestChannelHandler
}
