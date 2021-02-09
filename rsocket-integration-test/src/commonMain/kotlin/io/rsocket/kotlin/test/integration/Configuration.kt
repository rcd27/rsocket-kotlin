package io.rsocket.kotlin.test.integration

class ConfigurationBuilder {
    var port: Int = 8000
    val client = Client()
    val server = Server()

    class Client {
        var maxFragmentSize: Int = 0
    }

    class Server {
        var maxFragmentSize: Int = 0
    }

    fun build(): Configuration = Configuration(
        ServerConfiguration(port, client.maxFragmentSize),
        ClientConfiguration(port, server.maxFragmentSize)
    )
}

class Configuration(
    val server: ServerConfiguration,
    val client: ClientConfiguration
)

class ServerConfiguration(
    val port: Int,
    val maxFragmentSize: Int
)

class ClientConfiguration(
    val port: Int,
    val maxFragmentSize: Int
)
