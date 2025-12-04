package com.sffteam.openmax

import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.nio.ByteBuffer
import net.jpountz.lz4.LZ4Factory
import net.jpountz.lz4.LZ4FastDecompressor
import org.apache.commons.codec.binary.Hex
import java.nio.ByteOrder
import kotlin.collections.emptyList

val host = "api.oneme.ru"
val port = 443
val API_VERSION = 10 // lol
var Seq = 0
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

@Serializable
data class Payload(
    @SerialName("payload")
    @Contextual
    val payload: Any,
)

object SocketManager {
//    val sslContext = SSLContext.getInstance("TLS")
//    val sslSocketFactory = sslContext.socketFactory
//
//    var socket = Socket()

    fun packPacket() {

    }

    fun unpackPacket(data : ByteArray): Packet {
        // Thanks to https://github.com/ink-developer/PyMax/blob/main/src/pymax/mixins/socket.py#L42
        val factory = LZ4Factory.fastestInstance()
        val decompressor: LZ4FastDecompressor = factory.fastDecompressor()
        println(Hex.encodeHex(data))
        // not working..
        val apiVerSigned: Short = ByteBuffer.wrap(data, 0, 2).order(ByteOrder.LITTLE_ENDIAN).short
        val apiVer = apiVerSigned.toInt() and 0xFFFF
        println(apiVer)

        val cmdSigned = ByteBuffer.wrap(data, 1, 2).order(ByteOrder.LITTLE_ENDIAN).short
        val cmd = cmdSigned.toInt() and 0xFFFF

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
        payloadLength = payloadLength - 1
        val payloadBytes = data.sliceArray(10 .. 10 + payloadLength)
        var payload = Payload(0)

        if (compFlag != 0) {
            val compressedData = payloadBytes
            val decompressedBytes = ByteArray(99999)

            try {
                decompressor.decompress(compressedData, 0, decompressedBytes, 0, decompressedBytes.size)
            } catch (e : Exception) {
                println(e)
            }

            payload = MsgPack.decodeFromByteArray(
                Payload.serializer(),
                decompressedBytes
            )
        } else {
            println(payloadBytes)
            payload = MsgPack.decodeFromByteArray(
                Payload.serializer(),
                payloadBytes
            )
        }

        print(payload)
        return Packet(ver = apiVer, cmd = cmd, seq = seq, opcode = opcode, payload = JsonObject(emptyMap()))
    }

    fun connect() {
//        socket = sslSocketFactory.createSocket(host, port) as SSLSocket

        getPackets()
    }

    fun getPackets() {
        while (true) {
//            val input = socket.getInputStream()
//            val buffer = ByteArray(4096)
//            val bytesRead = input.read(buffer)
//
//            if (bytesRead != -1) {
//                val receivedData = buffer.copyOfRange(0, bytesRead)
//
//
//            }
        }
    }
}