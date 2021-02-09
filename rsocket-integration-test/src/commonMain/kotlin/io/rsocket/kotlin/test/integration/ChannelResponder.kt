package io.rsocket.kotlin.test.integration

import kotlinx.coroutines.channels.*

class ChannelStorage : HandlerStorage, RequestHandler {
    private val mp = Channel<MetadataPushHandler>(Channel.UNLIMITED)
    private val faf = Channel<FireAndForgetHandler>(Channel.UNLIMITED)
    private val rr = Channel<RequestResponseHandler>(Channel.UNLIMITED)
    private val rs = Channel<RequestStreamHandler>(Channel.UNLIMITED)
    private val rc = Channel<RequestChannelHandler>(Channel.UNLIMITED)

    override fun saveMetadataPushHandler(handler: MetadataPushHandler) {
        mp.offer(handler)
    }

    override fun saveFireAndForgetHandler(handler: FireAndForgetHandler) {
        faf.offer(handler)
    }

    override fun saveRequestResponseHandler(handler: RequestResponseHandler) {
        rr.offer(handler)
    }

    override fun saveRequestStreamHandler(handler: RequestStreamHandler) {
        rs.offer(handler)
    }

    override fun saveRequestChannelHandler(handler: RequestChannelHandler) {
        rc.offer(handler)
    }

    override suspend fun metadataPushHandler(): MetadataPushHandler = mp.receive()
    override suspend fun fireAndForgetHandler(): FireAndForgetHandler = faf.receive()
    override suspend fun requestResponseHandler(): RequestResponseHandler = rr.receive()
    override suspend fun requestStreamHandler(): RequestStreamHandler = rs.receive()
    override suspend fun requestChannelHandler(): RequestChannelHandler = rc.receive()
}
