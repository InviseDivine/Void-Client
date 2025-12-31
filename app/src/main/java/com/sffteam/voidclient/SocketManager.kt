package com.sffteam.voidclient

import android.content.Context
import android.content.Intent
import android.icu.util.TimeZone
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import net.jpountz.lz4.LZ4Factory
import net.jpountz.lz4.LZ4FastDecompressor
import org.msgpack.jackson.dataformat.MessagePackFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds

const val host = "api.oneme.ru"
const val port = 443
const val API_VERSION = 10 // lol
var Seq = 1

enum class OPCode(val opcode: Int) {
    PING(1), START(6), // Using that on open socket
    CHANGE_PROFILE(16), START_AUTH(17), CHECK_CODE(18), // Also can be LOGIN packet from server or WRONG_CODE from server
    PROFILE_INFO(19), // Server returns profile info with that opcode
    LOGOUT(20), NOTIFICATION_SET(22), // Set notification for chat
    NEW_STICKER_SETS(26), // Idk, implement it later
    SYNC_EMOJI(27), // Also syncs ANIMOJI, REACTIONS, STICKERS, FAVORITE_STICKER
    ANIMOJI(28), // Idk
    CONTACTS_INFO(32), // Returns info about ids that your sent (if you sent ids that not your contacts, server return you just a empty array)
    LAST_SEEN(35), // Used for obtain last seen of contacts
    CHAT_INFO(48), CHAT_MESSAGES(49), JOIN_CHAT(57), SEND_MESSAGE(64), DELETE_MESSAGE(66), EDIT_MESSAGE(
        67
    ),
    CHAT_SUBSCRIBE(75), // Idk
    WHO_CAN_SEE(76), // Used for disable or enable status online
    HISTORY(79), // Idk
    UPLOAD_IMAGE(80), SESSIONS(96), // Used for obtain all sessions for account
    SESSIONS_EXIT(97), PASSWORD_CHECK(115), SYNC_FOLDER(272), QR_CODE(290)
}

@Serializable
data class Packet(
    @SerialName("ver") val ver: Int = API_VERSION,
    @SerialName("cmd") val cmd: Int = 0,
    @SerialName("seq") val seq: Int = Seq,
    @SerialName("opcode") val opcode: Int,
    @SerialName("payload") @Contextual val payload: JsonElement,
)

data class PacketCallback(val seq: Int, val callback: (Packet) -> Unit)

fun Short.toByteArrayBigEndian(): ByteArray {
    return ByteBuffer.allocate(Short.SIZE_BYTES).putShort(this).array()
}

fun Int.toByteArrayBigEndian(): ByteArray {
    return byteArrayOf(
        (this ushr 24).toByte(), (this ushr 16).toByte(), (this ushr 8).toByte(), this.toByte()
    )
}

fun messagePackToJson(bytes: ByteArray): String {
    val msgpackMapper = ObjectMapper(MessagePackFactory())
    val jsonMapper = ObjectMapper()

    val node = msgpackMapper.readTree(bytes)
    return jsonMapper.writeValueAsString(node)
}

fun jsonToMessagePack(json: String): ByteArray {
    val jsonMapper = ObjectMapper()
    val msgPackMapper = ObjectMapper(MessagePackFactory())

    val tree = jsonMapper.readTree(json)
    return msgPackMapper.writeValueAsBytes(tree)
}

object SocketManager {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private lateinit var socket: Socket

    private val subscribers = CopyOnWriteArrayList<(String) -> Unit>()

    private var packetCallbacks = mutableListOf<PacketCallback>()

    fun packPacket(opcode: Int, payload: JsonElement): ByteArray {
        // Thanks to https://github.com/ink-developer/PyMax/blob/main/src/pymax/mixins/socket.py#L75 again :D
        val apiVer = API_VERSION.toByte()
        val cmd = 0.toByte()
        val seq = Seq.toShort().toByteArrayBigEndian()
        val opcode = opcode.toShort().toByteArrayBigEndian()
        println("string ${payload.toString()}")
        val payload = jsonToMessagePack(payload.toString())
        val payloadLen = payload.size and 0xFFFFFF

        return byteArrayOf(
            apiVer, cmd, *seq, *opcode, *payloadLen.toByteArrayBigEndian(), *payload
        )
    }

    fun unpackPacket(data: ByteArray): Packet {
        // Thanks to https://github.com/ink-developer/PyMax/blob/main/src/pymax/mixins/socket.py#L42
        val factory = LZ4Factory.fastestInstance()
        val decompressor: LZ4FastDecompressor = factory.fastDecompressor()

        val apiVer = data[0].toInt() and 0xFF
        val cmd = data[1].toInt() and 0xFF
        val seqSigned = ByteBuffer.wrap(data, 2, 2).order(ByteOrder.BIG_ENDIAN).short
        val seq = seqSigned.toInt() and 0xFFFF


        val opcodeSigned = ByteBuffer.wrap(data, 4, 2).order(ByteOrder.BIG_ENDIAN).short
        val opcode = opcodeSigned.toInt() and 0xFFFF

        val packedLen =
            ByteBuffer.wrap(data, 6, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL

        val compFlag = (packedLen shr 24).toInt()
        val payloadLength = (packedLen and 0xFFFFFF).toInt()

        val payloadBytes = data.sliceArray(10 until (10 + payloadLength))
        var payload = ""

        if (payloadBytes.isNotEmpty()) {
            if (compFlag != 0) {
                var decompressedBytes = ByteArray(131072)
                println("test1")
                try {
                    decompressor.decompress(payloadBytes, decompressedBytes)
                } catch (e: Exception) {
                    println("decomp err ${e}")
                }

                println("test2")

                try {
                    payload = messagePackToJson(decompressedBytes)
                } catch (e: Exception) {
                    println(e)
                }
            } else {
                println(payloadBytes)
                payload = messagePackToJson(payloadBytes)
            }
        }

        println("payload! ${payload}")
        var jsonPayload = JsonObject(emptyMap())

        if (payload.isNotEmpty()) {
            jsonPayload = Json.decodeFromString(payload)
        }
        return Packet(
            apiVer, cmd, seq, opcode, jsonPayload
        )
    }

    suspend fun sendStartPacket(context: Context): Boolean {
        sendPacket(
            packPacket(
                6, JsonObject(
                    mapOf(
                        "clientSessionId" to JsonPrimitive(192L), "userAgent" to JsonObject(
                            mapOf(
                                "deviceType" to JsonPrimitive("ANDROID"),
                                "appVersion" to JsonPrimitive("25.21.0"),
                                "osVersion" to JsonPrimitive("Android ${Build.VERSION.RELEASE}"),
                                "timezone" to JsonPrimitive(TimeZone.getDefault().id),
                                "screen" to JsonPrimitive("382dpi 382dpi 1080x2243"),
                                "pushDeviceType" to JsonPrimitive("GCM"),
                                "locale" to JsonPrimitive("ru"),
                                "buildNumber" to JsonPrimitive(6420),
                                "deviceName" to JsonPrimitive(Build.MANUFACTURER + " " + Build.MODEL),
                                "deviceLocale" to JsonPrimitive(Locale.getDefault().language.toString()),
                            )
                        ), "deviceId" to JsonPrimitive(
                            Settings.Secure.getString(
                                context.contentResolver, Settings.Secure.ANDROID_ID
                            )
                        )
                    )
                )
            ), { packet ->
                println("response")
                println(packet.payload)
            })

        return true
    }

    suspend fun connect(context: Context) = coroutineScope {
        println("trying to connect")

        while (true) {
            try {
                socket = aSocket(selectorManager).tcp().connect(host, port)
                    .tls(coroutineContext = currentCoroutineContext())

                val result = sendStartPacket(context)

                if (result) {
                    if (AccountManager.token != "null") {
                        loginToAccount(context)
                        AccountManager.logined = true
                    }
                    async {
                        sendPing()
                    }

                    getPackets()
                }
            } catch (e: Exception) {
                println(e)
            }

            delay(50)
        }
    }

    suspend fun loginToAccount(context: Context) = coroutineScope {
        val packet = packPacket(
            OPCode.PROFILE_INFO.opcode, JsonObject(
                mapOf(
                    "interactive" to JsonPrimitive(true),
                    "token" to JsonPrimitive(AccountManager.token),
                    "chatsCount" to JsonPrimitive(40),
                    "chatsSync" to JsonPrimitive(0),
                    "contactsSync" to JsonPrimitive(0),
                    "presenceSync" to JsonPrimitive(0),
                    "draftsSync" to JsonPrimitive(0),
                )
            )
        )

        sendPacket(
            packet, { packet ->
                if (packet.payload.jsonObject.containsKey("error")) {
                    val intent: Intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    AccountManager.token = "null"

                    println(AccountManager.token)

                    runBlocking {
                        try {
                            context.dataStore.edit { settings ->
                                settings[stringPreferencesKey("token")] = "null"
                            }
                        } catch (e: Exception) {
                            println(e)
                        }
                    }
                    context.startActivity(intent)
                } else {
                    println(packet)
                    try {
                        AccountManager.accountID =
                            packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["id"]!!.jsonPrimitive.long
                        AccountManager.phone =
                            packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["phone"]!!.jsonPrimitive.content

                        UserManager.processMyProfile(packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject)

                        val packet = SocketManager.packPacket(
                            OPCode.CONTACTS_INFO.opcode, JsonObject(
                                mapOf(
                                    "contactIds" to JsonArray(listOf(JsonPrimitive(AccountManager.accountID))),
                                )
                            )
                        )

                        GlobalScope.launch {
                            sendPacket(
                                packet, { packet ->
                                    println(packet.payload)
                                    if (packet.payload is JsonObject) {
                                        GlobalScope.launch {
                                            UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                        }
                                    }
                                })
                        }
                    } catch (e: Exception) {
                        println(e)
                    }
                    try {
                        val test = packet.payload.jsonObject["chats"]!!.jsonArray
                        println(test)
                    } catch (e: Exception) {
                        println(e)
                    }
                    println()

                    GlobalScope.launch {
                        ChatManager.processChats(packet.payload.jsonObject["chats"]!!.jsonArray)
                    }
                }
            })
    }

    suspend fun sendPacket(packet: ByteArray, callback: (Packet) -> Unit) {
        val sendChannel = socket.openWriteChannel(autoFlush = true)

        println(unpackPacket(packet))
        sendChannel.writeFully(packet)
        sendChannel.flush()

        packetCallbacks.add(PacketCallback(Seq, callback))

        Seq += 1
    }

    suspend fun getPackets() {
        val receiveChannel = socket.openReadChannel()
        try {
            var entirePacket = ByteArray(131072)
            var pos = 0
            while (socket.isActive) {
                val buffer = ByteArray(8192)
                val bytesRead = receiveChannel.readAvailable(buffer, 0, 8192)

                if (bytesRead == -1) {
                    break
                }

                println(bytesRead)
                println(buffer.size)

                if (bytesRead > 0) {
                    if (bytesRead == 8192) { // tmp solution
                        entirePacket = buffer.copyInto(entirePacket, pos)
                        pos += 8192
                        continue
                    }
                    entirePacket = buffer.copyInto(entirePacket, pos, 0, bytesRead)
                    pos += bytesRead
                    println("Total packet length: ${pos}")
                    val packet = unpackPacket(entirePacket.sliceArray(0..<pos))
                    pos = 0

                    if (packet.opcode == 128) {
                        var textForwarded: String = ""
                        var senderForwarded: Long = 0L
                        var msgForwardedID: String = ""
                        var forwardedAttaches: JsonElement? = JsonNull
                        var forwardedType: String = ""

                        if (packet.payload.jsonObject["message"]!!.jsonObject.contains("link")) {
                            val messageLinked =
                                packet.payload.jsonObject["message"]?.jsonObject["link"]?.jsonObject["message"]

                            textForwarded =
                                messageLinked?.jsonObject["text"]?.jsonPrimitive?.content.toString()
                            senderForwarded =
                                messageLinked?.jsonObject["sender"]?.jsonPrimitive!!.long
                            msgForwardedID =
                                messageLinked.jsonObject["id"]?.jsonPrimitive!!.long.toString()
                            forwardedType =
                                packet.payload.jsonObject["message"]?.jsonObject["link"]?.jsonObject["type"]?.jsonPrimitive?.content.toString()
                        }

                        ChatManager.addMessage(
                            packet.payload.jsonObject["message"]?.jsonObject["id"]!!.jsonPrimitive.content,
                            Message(
                                packet.payload.jsonObject["message"]?.jsonObject["text"]!!.jsonPrimitive.content,
                                packet.payload.jsonObject["message"]?.jsonObject["time"]!!.jsonPrimitive.long,
                                packet.payload.jsonObject["message"]?.jsonObject["sender"]!!.jsonPrimitive.long,
                                if (packet.payload.jsonObject["message"]?.jsonObject?.contains("attaches") == true) packet.payload.jsonObject["message"]?.jsonObject["attaches"]!!.jsonArray else JsonArray(
                                    emptyList()
                                ),
                                if (packet.payload.jsonObject["message"]?.jsonObject?.contains("status") == true) packet.payload.jsonObject["message"]?.jsonObject["status"]!!.jsonPrimitive.content else "",
                                MessageLink(
                                    type = forwardedType, msgForLink = msgForLink(
                                        message = textForwarded,
                                        senderID = senderForwarded,
                                        attaches = forwardedAttaches,
                                        msgID = msgForwardedID
                                    )
                                )
                            ),
                            packet.payload.jsonObject["chatId"]?.jsonPrimitive?.long ?: 0L
                        )
                    }
                    run loop@{
                        SocketManager.packetCallbacks.forEachIndexed { i, cb ->
                            if (cb.seq == packet.seq) {
                                cb.callback(packet)
                                SocketManager.packetCallbacks.removeAt(i)
                                return@loop
                            }
                        }
                    }

                    println(packet)
                    println()
                }
            }
        } catch (e: Exception) {
            println(e)
        } finally {
            receiveChannel.cancel()
            socket.close()
        }
    }

    suspend fun sendPing() {
        val packet = packPacket(
            OPCode.PING.opcode, JsonObject(
                mapOf(
                    "interactive" to JsonPrimitive(false),
                )
            )
        )

        while (true) {
            delay(20.seconds)
            sendPacket(packet, {})
            println("ping!")
        }
    }
}
