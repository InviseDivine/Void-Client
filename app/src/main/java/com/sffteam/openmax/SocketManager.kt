package com.sffteam.openmax

import com.daveanthonythomas.moshipack.MoshiPack
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.jpountz.lz4.LZ4Factory
import net.jpountz.lz4.LZ4FastDecompressor
import org.apache.commons.codec.binary.Hex
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
object SocketManager {
    val selectorManager = SelectorManager(Dispatchers.IO)
    lateinit var socket : Socket
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

    fun unpackPacket(data : ByteArray): Packet {
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
        val packedLen = ByteBuffer.wrap(data.sliceArray(6 .. 10)).int

        val compFlag = packedLen shr 24
        var payloadLength = packedLen and 0xFFFFFF
        payloadLength -= 1
        val payloadBytes = data.sliceArray(10 .. 10 + payloadLength)
        var payload: JsonObject

        if (compFlag != 0) {
            val decompressedBytes = ByteArray(99999)

            try {
                decompressor.decompress(payloadBytes, 0, decompressedBytes, 0, decompressedBytes.size)
            } catch (e : Exception) {
                println(e)
            }

            payload = Json.decodeFromString(MoshiPack.msgpackToJson(decompressedBytes))
        } else {
            println(payloadBytes)
            payload =  Json.decodeFromString(MoshiPack.msgpackToJson(payloadBytes))
        }

        println(payload)
        return Packet(ver = apiVer, cmd = cmd, seq = seq, opcode = opcode, payload = JsonObject(payload))
    }

    suspend fun connect() {
        println("trying to connect")
        socket = aSocket(selectorManager)
            .tcp()
            .connect(host, port)
            .tls(coroutineContext = currentCoroutineContext())

        sendPacket(packPacket(6, JsonObject(
            mapOf(
                "clientSessionId" to JsonPrimitive(192L),
                "userAgent" to JsonObject(
                    mapOf(
                        "deviceType" to JsonPrimitive("ANDROID"),
                        "appVersion" to JsonPrimitive("25.12.1"),
                        "osVersion" to JsonPrimitive("Android 14"),
                        "timezone" to JsonPrimitive("Europe/Kaliningrad"),
                        "screen" to JsonPrimitive("382dpi 382dpi 1080x2243"),
                        "pushDeviceType" to JsonPrimitive("GCM"),
                        "locale" to JsonPrimitive("ru"),
                        "buildNumber" to JsonPrimitive(6420),
                        "deviceName" to JsonPrimitive("oneplus CPH2465"),
                        "deviceLocale" to JsonPrimitive("ru"),
                        )
                ),
                "deviceId" to JsonPrimitive("018a9d9a35d8de67")
            )
        )))
        getPackets()
    }

    suspend fun sendPacket(packet : ByteArray) {
        val sendChannel = socket.openWriteChannel(autoFlush = true)

        println()
        println(unpackPacket(packet))
        println("trying to send")
        sendChannel.writeFully(packet)
        sendChannel.flush()
        println("sent")
    }
    suspend fun getPackets() {
            println("trying to get")
            val receiveChannel = socket.openReadChannel()
            try {
                while (socket.isActive) {
                    println("trying 2get")
                    val buffer = ByteArray(8192)
                    val bytesRead = receiveChannel.readAvailable(buffer)

                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        println(data)
                        println(unpackPacket(data))
                    }
                }
            } catch (e: Exception) {
                println("Reading error: ${e.message}")
            }
        }
    }
