# RSocket-kotlin typed/routing design notes

As rsocket-kotlin doesn't have some high-level abstraction (and not sure if something like this will appear soon), like spring-messaging for
rsocket-java, I was thinking about extending it in simple way on library level. Those thoughts come to me from spring, ktor and other
high-level frameworks.

Idea is mainly to support easy encoding/decoding of models to data, typed metadata handling and routing. I came to two things
called `TypedRSocket` and `RoutableRSocket`.

* `TypedRSocket` abstraction to support typed encoding and decoding of data based on kotlin's `KType` with main target
  for `kotlinx.serialization` (but can be easily extended for support of sending java files, streams or `okio` files)
* `RoutableRSocket` adds support for typed routing based on extracting data from `RoutingMetadata` or anything else

## TypedRSocket (rsocket-typed + rsocket-typed-serialization modules)

Provides several core classes: `TypedMetadata`, `TypedPayload` and `TypedRSocket`.

### TypedMetadata

Reference API interface:

```kotlin
//class
sealed class TypedMetadata : Closeable {
    abstract fun isEmpty(): Boolean
    abstract fun byType(type: KType): List<Any>
}

//type-safe accessors of metadata inside (main target implementation is CompositeMetadata)
inline fun <reified T : Any> TypedMetadata.byType(): List<T>
inline fun <reified T : Any> TypedMetadata.has(): Boolean
inline fun <reified T : Any> TypedMetadata.getOrNull(): T?
inline fun <reified T> TypedMetadata.get(): T

//type-safe accessors for destructuring declaration
inline operator fun <reified T> TypedMetadata.component1(): T
inline operator fun <reified T> TypedMetadata.component2(): T
inline operator fun <reified T> TypedMetadata.component3(): T
inline operator fun <reified T> TypedMetadata.component4(): T
inline operator fun <reified T> TypedMetadata.component5(): T

//builder for typed metadata
inline fun typedMetadata(block: TypedMetadataBuilder.() -> Unit): TypedMetadata
interface TypedMetadataBuilder : Closeable {
    fun add(type: KType, value: Any)
}

//type-safe adding components to metadata
inline fun <reified T : Any> TypedMetadataBuilder.add(value: T)
```

Use-site examples:

```kotlin
//building example
val metadata = typedMetadata {
    add(RoutingMetadata("route1", "tag2")) //extension routing("route1", "tag2") can also be provided if needed
    add(BearerAuthMetadata("token"))

    // or some not default object like json from kx.serialization
    // it's encoding will be covered by metadata coders/encoders that will be shortly showed latter
    add(buildJsonObject {
        put("key", "value")
    })

    // or even some own class, that can be encoded to some metadata
    add(SomeClass(1))
}

// access example
val routing = metadata.get<RoutingMetadata>()
// or
val routing: RoutingMetadata = metadata.get()
val hasAuth = metadata.has<BearerAuthMetadata>()
// or 
val auth = metadata.get<BearerAuthMetadata?>()
val hasAuth = auth != null
// or destructuring declaration
// here if there will be no auth metadata, auth will be null
val (routing: RoutingMetadata, auth: BearerAuthMetadata?) = metadata
```

### TypedPayload

Reference API interface:

```kotlin
//typed payload to get data in type-safe manner
sealed class TypedPayload(
    val metadata: TypedMetadata
) : Closeable {
    abstract fun data(type: KType): Any
}

//type-safe data accessor
inline fun <reified D> TypedPayload.data(): D = data(typeOf<D>()) as D

//type-safe accessors for destructuring declaration
inline operator fun <reified T> TypedPayload.component1(): T = data()
inline operator fun TypedPayload.component2(): TypedMetadata = metadata
```

Use-site example:

```kotlin
// model to serialize
@Serializable
data class SomeData(val s1: String, val v2: Int)

val payload = typedPayload(SomeData("hello", 322)) {
    // metadata builder as before
    add(RoutingMetadata("tag"))
}

// access

val data = payload.data<SomeData>() // will decode data to specified class using decoder
// or
val data: SomeData = payload.data()
// or
val (data: SomeData, metadata) = payload // such call necessity will be described in routing section 
```

### TypedRSocket

```kotlin
//core interface for interactions
interface TypedRSocket {
    suspend fun metadataPush(metadata: TypedMetadata)
    suspend fun fireAndForget(payload: TypedPayload)
    suspend fun requestResponse(payload: TypedPayload): TypedPayload
    fun requestStream(payload: TypedPayload): Flow<TypedPayload>
    fun requestChannel(initPayload: TypedPayload, payloads: Flow<TypedPayload>): Flow<TypedPayload>
}

//simplified interaction calls to easily do requests
suspend inline fun TypedRSocket.metadataPush(
    block: TypedMetadataBuilder.() -> Unit
)

suspend inline fun <reified D : Any> TypedRSocket.fireAndForget(
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
)

suspend inline fun <reified D : Any> TypedRSocket.requestResponse(
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
): TypedPayload

inline fun <reified D : Any> TypedRSocket.requestStream(
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
): Flow<TypedPayload>

inline fun <reified D : Any> TypedRSocket.requestChannel(
    data: D,
    payloads: Flow<TypedPayload> = emptyFlow(),
    block: TypedMetadataBuilder.() -> Unit = {}
): Flow<TypedPayload>

//simple emitting to flow 
suspend inline fun <reified D : Any> FlowCollector<TypedPayload>.emitPayload(data: D, block: TypedMetadataBuilder.() -> Unit = {})
```

Use-site example:

```kotlin
val rSocket: TypedRSocket = TODO()

@Serializable
data class SomeData(val s1: String, val v2: Int)

@Serializable
data class SomeRequest(val s1: String, val v2: Int)

@Serializable
data class SomeResponse(val s1: String, val v2: Int)

// full 
val data = rSocket.requestResponse(typedPayload(SomeRequest("1", 1)) {
    add(RoutingMetadata("hello"))
}).data<SomeResponse>()

// or using shortcuts

val (data: SomeResponse) = rSocket.requestResponse(SomeRequest("1", 1)) {
    routing("hello")
}

// request channel
val flow = flow {
    repeat(10) {
        emitPayload(SomeData("data")) {
            // some metadata
        }
    }
}

val response = rSocket.requestChannel(SomeRequest("1", 1), flow) {
    routing("hello.world")
}
//collect using decoding
response.collect { payload ->
    val data = payload.data<SomeData>()
    println(data)
}
// or here we use typed destructuring declaration to extract data of specified type
response.collect { (data: SomeData, metadata) ->
    println(data)
}
```

> All those examples have under the hood encoders/decoders using kx.serialization and some metadata coders not explained in this note

For building responder there is `TypedRSocketRequestHandlerBuilder` which can be used like this:

```kotlin
val responder = TypedRSocketRequestHandler {
    requestResponse { payload: TypedPayload ->
        when (val route = payload.metadata.get<RoutingMetadata>().tags.first()) {
            "route1" -> {
                val data = payload.data<String>()
                typedPayload("Hello world: $data")
            }
            "route2" -> {
                val data = payload.data<SomeData>()
                typedPayload("Hello world: $data")
            }
        }
    }
    // or with decoding in place
    requestStream { (data: JsonObject, metadata) ->
        val (routing: RoutingMetadata, other: String) = metadata
        TODO()
    }
}
```

## RoutableRSocket (rsocket-routing + rsocket-routing-serialization modules)

rsocket-routing contains: `RoutableRSocket` and `RoutableRSocketRequestHandlerBuilder`
rsocket-routing-serialization contains: `RSocketRoute` annotation to decode route dynamically to object

### RoutableRSocket

Reference API implementation: RR and RC for example. Here `R` is dynamic type of route, how to interpret it will be specified in route
coders. It can be both supported just with `String` route, and also with more complex usages like using serialization later.

```kotlin
interface RoutableRSocket {
    suspend fun <R : Any> requestResponse(routeType: KType, route: R, payload: TypedPayload): TypedPayload
    fun <R : Any> requestChannel(routeType: KType, route: R, initPayload: TypedPayload, payloads: Flow<TypedPayload>): Flow<TypedPayload>
}

//simplified typed accessors for routes for
suspend inline fun <reified R : Any> RoutableRSocket.requestResponse(route: R, payload: TypedPayload): TypedPayload

suspend inline fun <reified R : Any, reified D : Any> RoutableRSocket.requestResponse(
    selector: R,
    data: D,
    block: TypedMetadataBuilder.() -> Unit = {}
): TypedPayload

suspend inline fun <reified R : Any> RoutableRSocket.requestChannel(
    route: R,
    initPayload: TypedPayload,
    payloads: Flow<TypedPayload>
): Flow<TypedPayload>

suspend inline fun <reified R : Any, reified D : Any> RoutableRSocket.requestChannel(
    selector: R,
    data: D,
    payloads: Flow<TypedPayload>,
    block: TypedMetadataBuilder.() -> Unit = {}
): TypedPayload

```

Use-site example:

```kotlin
val rSocket: RoutableRSocket = TODO()

// one line to serialize route, request and deserialize response
val (response: SomeResponse, metadata) = rSocket.requestResponse("some.route", SomeRequest("123", 1)) {
    // add tracing or auth here
}
// or typed route (will be better described in serialization topic later
val response = rSocket.requestResponse(SomeRoute("dynamic_id_inside_route"), SomeRequest("123", 1))
```

### RoutableRSocketRequestHandlerBuilder

Reference API implementation: RR and RC for example.

```kotlin
interface RoutableRSocketRequestHandlerBuilder {
    fun <R : Any> requestResponse(route: R, block: suspend (payload: TypedPayload) -> TypedPayload)
    fun <R : Any> requestResponse(routeType: KType, block: suspend (route: R, payload: TypedPayload) -> TypedPayload)

    fun <R : Any> requestChannel(route: R, block: (initPayload: TypedPayload, payloads: Flow<TypedPayload>) -> Flow<TypedPayload>)
    fun <R : Any> requestChannel(
        routeType: KType,
        block: (route: R, initPayload: TypedPayload, payloads: Flow<TypedPayload>) -> Flow<TypedPayload>
    )
}

// type-safe with `KType`
inline fun <reified S : Any> RoutableRSocketRequestHandlerBuilder.requestResponse(
    block: suspend (route: S, payload: TypedPayload) -> TypedPayload
)

inline fun <reified S : Any> RoutableRSocketRequestHandlerBuilder.requestChannel(
    block: (initPayload: TypedPayload, payloads: Flow<TypedPayload>) -> Flow<TypedPayload>
)
```

Use-site examples:

```kotlin
fun RoutableRSocketRequestHandlerBuilder.build() {
    // simple string routing
    requestResponse("route.selector") { payload: TypedPayload ->
        TODO()
    }

    // string routing and decoding
    requestResponse("route.selector") { (data: SomeData) ->
        TODO()
    }

    // typed dynamic routing + decoding
    requestResponse { route: SomeRoute, (data: SomeData) ->
        TODO()
    }

    // or for RC
    requestChannel { route: SomeRoute, (data: SomeData), payloads ->
        TODO()
    }
}
```

Feature symmetric example:

```kotlin
// requester
val (data: SomeResponse) = rSocket.requestResponse(SomeRoute("dynamic_id_inside_route"), SomeRequest("123", 1))

// responder
requestResponse { route: SomeRoute, (data: SomeRequest) ->
    typedPayload(SomeResponse("hello", 123))
}
// now kotlin frontend will fail to resolve next example, 
// but it will be possible to omit typed payload constructor and encode Any passing value
requestResponse { route: SomeRoute, (data: SomeRequest) ->
    SomeResponse("hello", 123)
}
// or for type-safe check of resulting value
requestResponse<SomeRoute, SomeResponse> { route, (data: SomeRequest) ->
    SomeResponse("hello", 123)
}
```

### RSocketRoute

Final topic about type-safe routing. Introducing annotation + custom format for kx.serialization:

```kotlin
// contains tags which will be mapped to routing metadata tags
annotation class RSocketRoute(vararg val tags: String)
```

Example of usage:

```kotlin
// simple route `id` will be decoded from route and passed to `id` field of class
@Serializable
@RSocketRoute("api/communities/{id}")
data class CommunitiesRoute(
    val id: String
)

// nested route, first, it will encode nested part from nested route
// so the full path of match will be `api/communities/com_id/shipments/some_id
// route will match this route and construct object: ShipmentsRoute("some_id", CommunitiesRoute("com_id"))  
@Serializable
@RSocketRoute("{communities}/shipments/{id}")
data class ShipmentsRoute(
    val id: String,
    val communities: CommunitiesRoute
)


//and using it the same as before
//requester
rSocket.requestRespons(ShipmentsRoute("some_id", CommunitiesRoute("com_id")), "some data")
//responder
requestResponse { route: ShipmentsRoute, (data: String) ->
    route.id //id
    route.communities.id //community id
    //do smth

    typedPayload("response")
}
```

## Encoders and Decoders

It's out of scope of current note but some thoughts:

```kotlin

interface DataEncoder {
    // as multiple encoders can be specified we need to select one
    // metadata is provided here for case like `PerStreamDataMimeTypeMetadata` when we have several possible ways to encode same data  
    fun canEncode(type: KType, metadata: TypedMetadata): Boolean
    fun encode(type: KType, metadata: TypedMetadata, value: Any): ByteReadPacket
}

interface DataDecoder {
    // same for decoding of data with `PerStreamDataMimeTypeMetadata`
    fun canDecode(type: KType, metadata: TypedMetadata): Boolean
    fun decode(type: KType, metadata: TypedMetadata, packet: ByteReadPacket): Any
}
```

Similar interfaces for metadata coding still in progress... They will support encoding of specific entries inside metadata, and the
container for it: `CompositeMetadata`, `RoutingMetadata` (for super simple cases) or any other custom format. 
