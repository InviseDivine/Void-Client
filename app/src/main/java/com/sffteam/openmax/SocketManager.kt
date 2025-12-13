package com.sffteam.openmax

import android.content.Context
import android.icu.util.TimeZone
import android.os.Build
import android.provider.Settings
import com.daveanthonythomas.moshipack.Format
import com.daveanthonythomas.moshipack.MoshiPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.fasterxml.jackson.databind.ObjectMapper
import com.sffteam.openmax.WebsocketManager.SendPacket
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import net.jpountz.lz4.LZ4DecompressorWithLength
import net.jpountz.lz4.LZ4Factory
import net.jpountz.lz4.LZ4FastDecompressor
import org.apache.commons.codec.binary.Hex
import org.msgpack.jackson.dataformat.MessagePackFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

const val host = "api.oneme.ru"
const val port = 443
const val API_VERSION = 10 // lol
var Seq = 1

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
    SEND_MESSAGE(64),
    DELETE_MESSAGE(66),
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
    val opcode: Int,
    @SerialName("payload")
    @Contextual
    val payload: JsonElement,
)

fun Short.toByteArrayBigEndian(): ByteArray {
    return ByteBuffer.allocate(Short.SIZE_BYTES)
        .putShort(this)
        .array() //
}

fun Int.toByteArrayBigEndian(): ByteArray {
    return byteArrayOf(
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
}
fun messagePackToJson(bytes: ByteArray): String {
    val msgpackMapper = ObjectMapper(MessagePackFactory())
    val jsonMapper = ObjectMapper()

    val node = msgpackMapper.readTree(bytes)
    return jsonMapper.writeValueAsString(node)
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
        val payload = MoshiPack().jsonToMsgpack(payload.toString()).readByteArray()
        val payloadLen = payload.size and 0xFFFFFF

        return byteArrayOf(
            apiVer,
            cmd,
            *seq,
            *opcode,
            *payloadLen.toByteArrayBigEndian(),
            *payload
        )
    }

    fun unpackPacket(data: ByteArray): Packet {
        // Thanks to https://github.com/ink-developer/PyMax/blob/main/src/pymax/mixins/socket.py#L42
        val factory = LZ4Factory.fastestInstance()
        val decompressor: LZ4FastDecompressor = factory.fastDecompressor()
        println(Hex.encodeHex(data))

        val apiVer = data[0].toInt() and 0xFF
        println(apiVer)

        val cmd = data[1].toInt() and 0xFF

        println(cmd)

        val seqSigned = ByteBuffer.wrap(data, 2, 2).order(ByteOrder.BIG_ENDIAN).short
        val seq = seqSigned.toInt() and 0xFFFF

        println(seq)

        val opcodeSigned = ByteBuffer.wrap(data, 4, 2).order(ByteOrder.BIG_ENDIAN).short
        val opcode = opcodeSigned.toInt() and 0xFFFF

        println(opcode)
        val packedLen =
            ByteBuffer.wrap(data, 6, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL

        val compFlag = (packedLen shr 24).toInt()
        val payloadLength = (packedLen and 0xFFFFFF).toInt()
        println(data.size)
        println(payloadLength)
        println("test3")

        val payloadBytes = data.sliceArray(10 until (10 + payloadLength))
        var payload: String = ""
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
            apiVer,
            cmd,
            seq,
            opcode,
            jsonPayload
        )
    }

    suspend fun connect(context: Context) = coroutineScope {
        println("trying to connect")
        socket = aSocket(selectorManager)
            .tcp()
            .connect(host, port)
            .tls(coroutineContext = currentCoroutineContext())

        sendPacket(
            packPacket(
                6, JsonObject(
                    mapOf(
                        "clientSessionId" to JsonPrimitive(192L),
                        "userAgent" to JsonObject(
                            mapOf(
                                "deviceType" to JsonPrimitive("ANDROID"),
                                "appVersion" to JsonPrimitive("25.12.1"),
                                "osVersion" to JsonPrimitive("Android ${Build.VERSION.RELEASE}"),
                                "timezone" to JsonPrimitive(TimeZone.getDefault().id),
                                "screen" to JsonPrimitive("382dpi 382dpi 1080x2243"),
                                "pushDeviceType" to JsonPrimitive("GCM"),
                                "locale" to JsonPrimitive("ru"),
                                "buildNumber" to JsonPrimitive(6420),
                                "deviceName" to JsonPrimitive(Build.MANUFACTURER + " " + Build.MODEL),
                                "deviceLocale" to JsonPrimitive(Locale.getDefault().language.toString()),
                            )
                        ),
                        "deviceId" to JsonPrimitive(Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID))
                    )
                )
            ),
            { packet ->
                println("response")
                println(packet.payload)
            }
        )

        if (AccountManager.token != "null") {
            loginToAccount()
            AccountManager.logined = true
        }
        async {
            sendPing()
        }

        getPackets()
    }

    suspend fun loginToAccount() = coroutineScope {
        val packet = packPacket(
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
            )
        )

        sendPacket(
            packet,
            { packet ->
                println("processin1g")
                println(packet)
                try {
                    AccountManager.accountID =
                        packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["id"]!!.jsonPrimitive.long
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

                runBlocking {
                    ChatManager.processChats(packet.payload.jsonObject["chats"]!!.jsonArray)
                }
                println("processi2ng")
            }
        )
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
        println("trying to get")
        val receiveChannel = socket.openReadChannel()
        try {
            var entirePacket = ByteArray(131072)
            var pos = 0
            while (socket.isActive) {
                println("trying 2get")

                val buffer = ByteArray(8192)
                val bytesRead = receiveChannel.readAvailable(buffer, 0, 8192)
                if (bytesRead < 0) {
                    socket.close()
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
                        var textForwarded : String = ""
                        var senderForwarded : Long = 0L
                        var msgForwardedID : String = ""
                        var forwardedAttaches: JsonElement? = JsonNull
                        var forwardedType : String = ""


                        if (packet.payload.jsonObject["message"]!!.jsonObject.contains("link")) {

                        }
                        ChatManager.addMessage(packet.payload.jsonObject["message"]?.jsonObject["id"]!!.jsonPrimitive.content, Message(
                        packet.payload.jsonObject["message"]?.jsonObject["text"]!!.jsonPrimitive.content,
                        packet.payload.jsonObject["message"]?.jsonObject["time"]!!.jsonPrimitive.long,
                        packet.payload.jsonObject["message"]?.jsonObject["sender"]!!.jsonPrimitive.long,
                        if (packet.payload.jsonObject["message"]?.jsonObject?.contains("attaches") == true) packet.payload.jsonObject["message"]?.jsonObject["attaches"]!!.jsonArray else JsonArray(emptyList()),
                            if (packet.payload.jsonObject["message"]?.jsonObject?.contains("status") == true) packet.payload.jsonObject["message"]?.jsonObject["status"]!!.jsonPrimitive.content else "",
                            MessageLink(
                                type = forwardedType,
                                msgForLink = msgForLink(
                                    message = textForwarded,
                                    senderID = senderForwarded,
                                    attaches = forwardedAttaches,
                                    msgID = msgForwardedID
                                )
                            )
                        ), packet.payload.jsonObject["chatId"]?.jsonPrimitive?.long ?: 0L
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
            println("Reading error: ${e.message}")

        }
    }

    suspend fun sendPing() {
        val packet = packPacket(
            OPCode.PING.opcode,
            JsonObject(
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
