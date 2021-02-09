package io.rsocket.kotlin.test.integration

interface HandlerStorage {
    fun saveMetadataPushHandler(handler: MetadataPushHandler)
    fun saveFireAndForgetHandler(handler: FireAndForgetHandler)
    fun saveRequestResponseHandler(handler: RequestResponseHandler)
    fun saveRequestStreamHandler(handler: RequestStreamHandler)
    fun saveRequestChannelHandler(handler: RequestChannelHandler)
}
