package com.sffteam.openmax

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.*
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.subclass
import java.util.UUID

private const val API_VERSION = 11
private const val USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0 Safari/537.36"

private var Seq = 0

enum class OPCode(val opcode: Int) {
    PING(1),
    START(6), // Using that on open socket
    START_AUTH(17),
    CHECK_CODE(18), // Also can be LOGIN packet from server or WRONG_CODE from server
    PROFILE_INFO(19), // Server returns profile info with that opcode
    NOTIFICATION_SET(22), // Set notification for chat
    NEW_STICKER_SETS(26), // Idk, implement it later
    SYNC_EMOJI(27), // Also syncs ANIMOJI, REACTIONS, STICKERS, FAVORITE_STICKER
    ANIMOJI(28), // Idk
    CONTACTS_INFO(32), // Returns info about ids that your sent (if you sent ids that not your contacts, server return you just a empty array)
    LAST_SEEN(35), // Used for obtain last seen of contacts
    CHAT_INFO(48),
    CHAT_MESSAGES(49),
    CHAT_SUBSCRIBE(75), // Idk
    HISTORY(79), // Idk
    SESSIONS(96), // Used for obtain all sessions for account
    SYNC_FOLDER(272)
}

@Serializable
data class Packet(
    @SerialName("ver")
    val ver: Int = API_VERSION,
    @SerialName("cmd")
    val cmd: Int = 0,
    @SerialName("seq")
    val seq: Int = Seq,
    @SerialName("opcode")
    val opcode: OPCode,
    @SerialName("payload")
    val payload: JsonElement,
)

object WebsocketManager {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private const val url = "wss://ws-api.oneme.ru/websocket"

    private val subscribers = CopyOnWriteArrayList<(String) -> Unit>()

    fun Connect(onConnected: (() -> Unit)? = null, onError: ((Throwable) -> Unit)? = null) {
        val request = Request.Builder()
            .url(url)
            .addHeader("user_agent_header", USER_AGENT)
            .addHeader("Origin", "https://web.max.ru")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("opened")
                // Sending welcome package
                SendPacket(
                    OPCode.START,
                    JsonObject(
                        mapOf(
                            "userAgent" to JsonObject(
                                mapOf(
                                    "deviceType" to JsonPrimitive("WEB"),
                                    "locale" to JsonPrimitive("ru"),
                                    "deviceLocale" to JsonPrimitive("ru"),
                                    "osVersion" to JsonPrimitive("Linux"),
                                    "deviceName" to JsonPrimitive("Chrome"),
                                    "headerUserAgent" to JsonPrimitive(USER_AGENT),
                                    "appVersion" to JsonPrimitive("25.10.13"),
                                    "screen" to JsonPrimitive("1080x1920 1.0x"),
                                    "timezone" to JsonPrimitive("Europe/Moscow"),
                                )
                            ),
                            "deviceId" to JsonPrimitive(UUID.randomUUID().toString())
                        )
                    )
                )
                println("opened")

                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text);
                Log.d("SOCKET", "receive $text");
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                println(ByteString)
                notifySubscribers(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError?.invoke(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket closed: $code / $reason")
            }
        })
    }

    fun SendPing() {

    }

    fun SendPacket(opcode: OPCode, payload: JsonElement) {
        val packet = Packet(opcode = opcode, payload = payload)

        val json = Json {
            encodeDefaults = true
            prettyPrint = true
        }

        val stringPacket = json.encodeToString(packet)

        println(stringPacket)

        webSocket?.send(stringPacket)

        Seq += 1
    }

    fun subscribe(callback: (String) -> Unit) {
        subscribers += callback
    }

    fun unsubscribe(callback: (String) -> Unit) {
        subscribers -= callback
    }

    fun close(code: Int = 1000, reason: String = "Closing") {
        webSocket?.close(code, reason)
    }

    private fun notifySubscribers(message: String) {
        for (subscriber in subscribers) {
            subscriber(message)
        }
    }
}