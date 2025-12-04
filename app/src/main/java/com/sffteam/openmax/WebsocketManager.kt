package com.sffteam.openmax

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds

//private const val API_VERSION = 11
private const val USER_AGENT = "Mozilla/5.0 (Linux; U; Linux x86_64) Gecko/20100101 Firefox/73.6"

//private var Seq = 0

//enum class OPCode(val opcode: Int) {
//    PING(1),
//    START(6), // Using that on open socket
//    START_AUTH(17),
//    CHECK_CODE(18), // Also can be LOGIN packet from server or WRONG_CODE from server
//    PROFILE_INFO(19), // Server returns profile info with that opcode
//    NOTIFICATION_SET(22), // Set notification for chat
//    NEW_STICKER_SETS(26), // Idk, implement it later
//    SYNC_EMOJI(27), // Also syncs ANIMOJI, REACTIONS, STICKERS, FAVORITE_STICKER
//    ANIMOJI(28), // Idk
//    CONTACTS_INFO(32), // Returns info about ids that your sent (if you sent ids that not your contacts, server return you just a empty array)
//    LAST_SEEN(35), // Used for obtain last seen of contacts
//    CHAT_INFO(48),
//    CHAT_MESSAGES(49),
//    SEND_MESSAGE(64),
//    DELETE_MESSAGE(66),
//    CHAT_SUBSCRIBE(75), // Idk
//    HISTORY(79), // Idk
//    SESSIONS(96), // Used for obtain all sessions for account
//    SYNC_FOLDER(272)
//}

//@Serializable
//data class Packet(
//    @SerialName("ver")
//    val ver: Int = API_VERSION,
//    @SerialName("cmd")
//    val cmd: Int = 0,
//    @SerialName("seq")
//    val seq: Int = Seq,
//    @SerialName("opcode")
//    val opcode: Int,
//    @SerialName("payload")
//    val payload: JsonElement,
//)

data class PacketCallback(val seq: Int, val callback: (Packet) -> Unit)

object WebsocketManager {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private const val url = "wss://ws-api.oneme.ru/websocket"

    private val subscribers = CopyOnWriteArrayList<(String) -> Unit>()

    private var packetCallbacks = mutableListOf<PacketCallback>()

    private var isConnected: Boolean = false
    fun Connect(onConnected: (() -> Unit)? = null, onError: ((Throwable) -> Unit)? = null) {
        val request = Request.Builder()
            .url(url)
            .addHeader("user_agent_header", USER_AGENT)
            .addHeader("Origin", "https://web.max.ru")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Seq = 0
                println("opened")
                // Sending welcome package
                SendPacket(
                    OPCode.START.opcode,
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
                                    "appVersion" to JsonPrimitive("25.11.7"),
                                    "screen" to JsonPrimitive("1080x1920 1.0x"),
                                    "timezone" to JsonPrimitive("Europe/Kaliningrad"),
                                )
                            ),
                            "deviceId" to JsonPrimitive(UUID.randomUUID().toString())
                        )
                    ),
                    { packet ->
                        println("response!")
                        println(packet.payload)

                    }
                )
                println("opened")
                isConnected = true
                loginToAccount()


                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text);
                Log.d("SOCKET", "receive $text");

                val packet = Json.decodeFromString<Packet>(text);
//                if (packet.opcode == 128) {
//                    println(packet)
//                    ChatManager.addMessage(Message(
//                        packet.payload.jsonObject["message"]?.jsonObject["text"]!!.jsonPrimitive.content,
//                        packet.payload.jsonObject["message"]?.jsonObject["time"]!!.jsonPrimitive.long,
//                        packet.payload.jsonObject["message"]?.jsonObject["sender"]!!.jsonPrimitive.long,
//                        packet.payload.jsonObject["message"]?.jsonObject["attaches"]!!.jsonArray,
//                        packet.payload.jsonObject["message"]?.jsonObject["status"]!!.jsonPrimitive.content,
//                        packet.payload.jsonObject["message"]?.jsonObject["id"]!!.jsonPrimitive.content,
//                        ), packet.payload.jsonObject["chatId"]?.jsonPrimitive?.long ?: 0L
//                    )
//                    println("msg added")
//                }
                run loop@{
                    packetCallbacks.forEachIndexed { i, cb ->
                        if (cb.seq == packet.seq) {
                            cb.callback(packet)
                            packetCallbacks.removeAt(i)
                            return@loop
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                println(ByteString)
                notifySubscribers(bytes.utf8())
                Log.d("SOCKET", "received binary data")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("response ${response?.message}")
                println("throw ${t.message}")
                onError?.invoke(t)
                Connect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket closed: $code / $reason")
                Connect()

            }

        })
    }
    fun loginToAccount() {
        SendPacket(
            OPCode.PROFILE_INFO.opcode,
            JsonObject(
                mapOf(
                    "interactive" to JsonPrimitive(true),
                    "token" to JsonPrimitive(AccountManager.token),
                    "chatsCount" to JsonPrimitive(40),
                    "chatsSync" to JsonPrimitive(0),
                    "contactsSync" to JsonPrimitive(0),
                    "presenceSync" to JsonPrimitive(0),
                    "draftsSync" to JsonPrimitive(0),
                )
            ),
            { packet ->
                println("processin1g")
                try {
                    AccountManager.accountID = packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["id"]!!.jsonPrimitive.long
                } catch (e : Exception) {
                    println(e)
                }
                try {
                    val test = packet.payload.jsonObject["chats"]!!.jsonArray
                    println(test)
                } catch (e: Exception) {
                    println(e)
                }
                println()

                while (!ChatManager.processChats(packet.payload.jsonObject["chats"]!!.jsonArray)) {
                    println("test1")
                }
                println("processi2ng")
            }
        )
    }
    suspend fun sendPing() {
        while (true) {
            delay(25.seconds)
            SendPacket(
                OPCode.PING.opcode,
                JsonObject(
                    mapOf(
                        "interactive" to JsonPrimitive(true),
                    )
                ),
                {}
            )
            println("ping!")
        }
    }

    fun SendPacket(opcode: Int, payload: JsonElement, callback: (Packet) -> Unit) {
        val packet = Packet(opcode = opcode, payload = payload)

        val json = Json {
            encodeDefaults = true
            prettyPrint = true
        }

        val stringPacket = json.encodeToString(packet)

        println(stringPacket)

        webSocket?.send(stringPacket)

        packetCallbacks.add(PacketCallback(Seq, callback))

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