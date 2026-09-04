package com.invdiv.voidclient

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import net.jpountz.lz4.LZ4Factory
import net.jpountz.lz4.LZ4SafeDecompressor
import org.msgpack.jackson.dataformat.MessagePackFactory
import org.msgpack.jackson.dataformat.MessagePackMapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.seconds
import okio.Buffer
import okio.ByteString.Companion.toByteString
import com.squareup.zstd.okio.zstdDecompress

// BIG Thanks to https://github.com/MaxApiTeam/PyMax/blob/main/src/pymax/_data/apk_fingerprints.json !!
const val certificate_meta_sha256 = "1684414033eb263e2c615f8b7df5ed8793850a07656304997fbf07e9e21e1e93"
const val dex_meta_sha256 = "73e8434d6524c3b6d7a90b63598befcf21f2d3b304e9ef3da153ac5bcbebdd99"
val so_meta_sha256 = mapOf(
    "arm64-v8a" to "e7871f948c8507284b09642372c40324498acdf2180a55643eca1bd66755375f",
    "armeabi-v7a" to "b4818fc95cd46d9bf0700adcd97dd49db67a4fcfed88d2ff181c16fd69f5d3db",
    "x86" to "b3c699608d9aadc3bf5360b99048ad41507c2ba23d5bfad6d8cfb19ef435efd3",
    "x86_64" to "8c6bad639bd1db5814200d75bbdf508bb0bb577b60dce5b4ca2961b63544b608"
)

const val host = "api2.oneme.ru"
const val port = 443
const val API_VERSION = 10 // lol
var Seq = 1

@Serializable
data class PhoneNumber(
    val mode: ByteArray,
    val type: String,
    val phone: String
)

enum class OPCode(val opcode: Int) {
    PING(1),
    START(6), // Using that on open socket
    CHANGE_PROFILE(16),
    START_AUTH(17),
    CHECK_CODE(18), // Also can be LOGIN packet from server or WRONG_CODE from server
    PROFILE_INFO(19), // Server returns profile info with that opcode
    LOGOUT(20),
    SETTINGS_CHANGE(22),
    NEW_STICKER_SETS(26), // Idk, will implement it later
    SYNC_EMOJI(27), // Also syncs ANIMOJI, REACTIONS, STICKERS, FAVORITE_STICKER
    ANIMOJI(28), // Idk
    CONTACTS_INFO(32), // Returns info about ids that your sent (if you sent ids that not your contacts, server return you just a empty array)
    LAST_SEEN(35), // Used for obtain last seen of contacts
    CHAT_INFO(48),
    CHAT_MESSAGES(49),
    MEDIAS_FROM_CHAT(51),
    EDIT_CHAT_INFO(55),
    JOIN_CHAT(57),
    LEAVE_CHAT(58),
    SEND_MESSAGE(64),
    DELETE_MESSAGE(66),
    EDIT_MESSAGE(67),
    CHAT_SUBSCRIBE(75), // Idk
    WHO_CAN_SEE(76), // Used for disable or enable status online
    EDIT_ADMIN_PERMISSION(77),
    HISTORY(79), // Idk
    UPLOAD_IMAGE(80),
    UPLOAD_VIDEO(82),
    GET_VIDEO(83),
    UPLOAD_FILE(87),
    GET_FILE(88),
    PREVIEW_JOINLINK(89),
    SESSIONS(96), // Used for obtain all sessions for account
    SESSIONS_EXIT(97),
    REQUEST_RESET_PASSWORD(109),
    RESET_PASSWORD_CHECK(110),
    PASSWORD_CHECK(115),
    BOT_CALLBACK(118),
    NEW_MESSAGE(128),
    TYPING(129),
    UPDATE_MESSAGE_READ(130),
    PRECENSE_UPDATE(132),
    SETTINGS_UPDATE(134),
    CHAT_UPDATE_INFO(142),
    REACTIONS_CHANGED(156),
    ADD_EMOJI(178),
    REMOVE_REACTION(179),
    GET_DETAILED_REACTIONS(181),
    SYNC_FOLDER(272),
    QR_CODE(290)
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
data class FileCallback(val fileId: Long, val callback: () -> Unit)

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
    private var packetCallbacks = mutableListOf<PacketCallback>()
    private var filesUploadCallbacks = mutableListOf<FileCallback>()

    private val _socketState = MutableStateFlow<Int>(0)
    var socketState = _socketState.asStateFlow()

    private fun packPacket(opcode: Int, payload: JsonElement): ByteArray {
        // Thanks to https://github.com/ink-developer/PyMax/blob/main/src/pymax/mixins/socket.py#L75 again :D
        val apiVer = API_VERSION.toByte()
        val cmd = 0.toByte()
        val seq = Seq.toShort().toByteArrayBigEndian()
        val opcode = opcode.toShort().toByteArrayBigEndian()
        val payload = jsonToMessagePack(payload.toString())
        val payloadLen = payload.size and 0xFFFFFF

        return byteArrayOf(
            apiVer, cmd, *seq, *opcode, *payloadLen.toByteArrayBigEndian(), *payload
        )
    }

    private fun packPacket(opcode: Int, payload: ByteArray): ByteArray {
        // Thanks to https://github.com/ink-developer/PyMax/blob/main/src/pymax/mixins/socket.py#L75 again :D
        val apiVer = API_VERSION.toByte()
        val cmd = 0.toByte()
        val seq = Seq.toShort().toByteArrayBigEndian()
        val opcode = opcode.toShort().toByteArrayBigEndian()
        val payloadLen = payload.size and 0xFFFFFF

        return byteArrayOf(
            apiVer, cmd, *seq, *opcode, *payloadLen.toByteArrayBigEndian(), *payload
        )
    }

    private fun unpackPacket(data: ByteArray): Packet {
        // Thanks to https://github.com/ink-developer/PyMax/blob/main/src/pymax/mixins/socket.py#L42
        val factory = LZ4Factory.safeInstance()
        val decompressor: LZ4SafeDecompressor = factory.safeDecompressor()

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
                if (compFlag == 255) {
                    val source = Buffer().apply {
                        write(payloadBytes)
                    }

                    val output = Buffer()

                    source
                        .zstdDecompress()
                        .use { zstdSource ->
                            output.writeAll(zstdSource)
                        }

                    payload = messagePackToJson(output.readByteArray())
                } else {
                    var decompressedBytes = ByteArray(payloadLength * compFlag)
                    try {
                        decompressor.decompress(payloadBytes, decompressedBytes)
                    } catch (e: Exception) {
                        Log.e("Packet", "LZ4 error: $e")
                    }

                    try {
                        payload = messagePackToJson(decompressedBytes)
                    } catch (e: Exception) {
                        Log.e("Packet", "MSGPack error: $e")
                    }
                }
            } else {
                payload = messagePackToJson(payloadBytes)
            }
        }

        var jsonPayload = JsonObject(emptyMap())

        if (payload.isNotEmpty()) {
            jsonPayload = Json.decodeFromString(payload)
        }

        return Packet(
            apiVer, cmd, seq, opcode, jsonPayload
        )
    }

    @SuppressLint("HardwareIds")
    private suspend fun sendStartPacket(context: Context): Boolean {
        val metrics = context.resources.displayMetrics

        AccountManager.deviceId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )

        val payload = JsonObject(
            mapOf(
                "mt_instanceid" to JsonPrimitive(AccountManager.mtInstanceId),
                "userAgent" to JsonObject(
                    mapOf(
                        "timezone"       to JsonPrimitive(TimeZone.getDefault().id),
                        "deviceLocale"   to JsonPrimitive(Locale.getDefault().language.toString()),
                        "locale"         to JsonPrimitive("ru"),
                        "screen"         to JsonPrimitive("${metrics.densityDpi}dpi ${metrics.densityDpi}dpi ${metrics.heightPixels}x${metrics.widthPixels}"),
                        "arch"           to JsonPrimitive(Build.SUPPORTED_ABIS[0]),
                        "deviceName"     to JsonPrimitive(Build.MANUFACTURER + " " + Build.MODEL),
                        "deviceType"     to JsonPrimitive("ANDROID"),
                        "appVersion"     to JsonPrimitive("26.24.0"),
                        "pushDeviceType" to JsonPrimitive("HUAWEI"),
                        "osVersion"      to JsonPrimitive("Android ${Build.VERSION.RELEASE}"),
                        "buildNumber"    to JsonPrimitive(6784),
                    )
                ),
                "clientSessionId" to JsonPrimitive(AccountManager.sessionId),
                "deviceId" to JsonPrimitive(AccountManager.deviceId)
            )
        )

        sendPacket(OPCode.START, payload, { packet ->
            AccountManager.callsSeed = packet.payload.jsonObject["callsSeed"]!!.jsonPrimitive.long

            for (country in packet.payload.jsonObject["reg-country-code"]!!.jsonArray) {
                Utils.maxCodeBase += country.jsonPrimitive.content
            }
        })

        return true
    }

    fun addFileCallback(fileId : Long, callback: () -> Unit) {
        val fileCallback = FileCallback(fileId, callback)

        filesUploadCallbacks.add(fileCallback)
    }
    suspend fun connect(context: Context) = coroutineScope {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        Log.i("SocketManager", "Trying to connect...")
        _socketState.update { 1 }

        while (true) {
            try {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                val isOnline = capabilities?.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) == true

                if (isOnline) {
                    _socketState.update { 1 }

                    socket = aSocket(selectorManager).tcp().connect(host, port)
                        .tls(coroutineContext = currentCoroutineContext())
                    val result = sendStartPacket(context)
                    if (result) {
                        _socketState.update { 2 }

                        if (AccountManager.token != "null") {
                            AccountManager.loginToAccount(context)
                        }

                        async {
                            sendPing()
                        }

                        getPackets()
                    }
                } else {
                    _socketState.update { 3 }
                }
            } catch (e: Exception) {
                Log.e("SocketManager", "Socket exception: $e")
                _socketState.update { 1 }
            }

            delay(2000)
            Log.i("SocketManager", "Reconnecting...")
            Seq = 1
            packetCallbacks.clear()
        }
    }

    suspend fun sendPacket(opcode: OPCode, payload: JsonElement, callback: (Packet) -> Unit) {
        val packet = packPacket(opcode.opcode, payload)
        val sendChannel = socket.openWriteChannel(autoFlush = true)

        sendChannel.writeFully(packet)
        sendChannel.flush()

//        Log.i("SocketManager", "Sent packet with seq $Seq")
//        Log.i("SocketManager", "Payload of sent packet: $payload")

        packetCallbacks.add(PacketCallback(Seq, callback))

        Seq += 1
    }

    suspend fun sendPacket(opcode: OPCode, payload : ByteArray, callback: (Packet) -> Unit) {
        val sendChannel = socket.openWriteChannel(autoFlush = true)
        val packet = packPacket(opcode.opcode, payload)

        sendChannel.writeFully(packet)
        sendChannel.flush()

//        Log.i("SocketManager", "Sent packet with seq $Seq")
//        Log.i("SocketManager", "Payload of sent packet: $payload")

        packetCallbacks.add(PacketCallback(Seq, callback))

        Seq += 1
    }

    private suspend fun getPackets() {
        val receiveChannel = socket.openReadChannel()
        try {
            var entirePacket = ByteArray(131720)
            var totalRead = 0

            while (socket.isActive) {
                var buffer = ByteArray(8192)
                val bytesRead = receiveChannel.readAvailable(buffer, 0, 8192)

                if (bytesRead == -1) {
                    break
                }

                if (bytesRead > 0) {
                    totalRead += bytesRead
                    val packedLen =
                        ByteBuffer.wrap(buffer, 6, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
                    val payloadLength = (packedLen and 0xFFFFFF).toInt()
                    val totalLength = payloadLength + 10

                    buffer.copyInto(entirePacket, 0, 0, bytesRead)

                    while (totalLength > totalRead) {
                        val newBytes = receiveChannel.readAvailable(buffer, 0, 8192)
                        buffer.copyInto(entirePacket, totalRead)
                        totalRead += newBytes
                    }

//                    Log.i("SocketManager", "Got packet with size $totalRead")
                    val packet = unpackPacket(entirePacket.sliceArray(0..totalRead))
//                    Log.i("SocketManager", "Seq ${packet.seq}; Payload: ${packet.payload}")

                    totalRead = 0


                    if (packet.opcode == OPCode.REACTIONS_CHANGED.opcode) {
                        ChatsManager.processReactions(packet.payload.jsonObject["reactionInfo"]!!.jsonObject, packet.payload.jsonObject["messageId"]!!.jsonPrimitive.long, packet.payload.jsonObject["chatId"]!!.jsonPrimitive.long)
                    }

                    if (packet.opcode == OPCode.CHAT_UPDATE_INFO.opcode) {
                        if (packet.payload.jsonObject.contains("messageIds")) {
                            ChatsManager.removeMessage(packet.payload.jsonObject["chat"]!!.jsonObject["id"]!!.jsonPrimitive.long, packet.payload.jsonObject["messageIds"]!!.jsonArray)
                        }

                        ChatsManager.processChats(packet.payload.jsonObject["chat"]!!.jsonObject)
                    }

                    if (packet.opcode == 136) {
                        if (packet.payload.jsonObject.contains("fileId")) {
                            val fileId = packet.payload.jsonObject["fileId"]!!.jsonPrimitive.long

                            for (callback in filesUploadCallbacks) {
                                if (callback.fileId == fileId) {
                                    callback.callback()
                                    filesUploadCallbacks.remove(callback)
                                    break
                                }
                            }
                        }
                    }

                    if (packet.opcode == OPCode.UPDATE_MESSAGE_READ.opcode) {
                        val chatID = packet.payload.jsonObject["chatId"]!!.jsonPrimitive.long
                        val userID = packet.payload.jsonObject["userId"]!!.jsonPrimitive.long
                        val mark = packet.payload.jsonObject["mark"]!!.jsonPrimitive.long

                        ChatsManager.updateReadMark(chatID, userID, mark)
                    }

                    if (packet.opcode == 135) {
                        if (packet.payload.jsonObject["chat"]?.jsonObject["status"]?.jsonPrimitive?.content == "REMOVED") {
                            ChatsManager.removeChat(packet.payload.jsonObject["chat"]?.jsonObject["id"]?.jsonPrimitive?.long!!)
                        } else {
                            ChatsManager.processChats(JsonArray(listOf(packet.payload.jsonObject["chat"]!!.jsonObject)))
                        }
                    }

                    if (packet.opcode == OPCode.NEW_MESSAGE.opcode) {
                        if (packet.payload.jsonObject.contains("chat")) {
                            GlobalScope.launch {
                                ChatsManager.processChats(JsonArray(listOf(packet.payload.jsonObject["chat"]!!.jsonObject)))
                            }
                        }

                        ChatsManager.addMessage(packet.payload.jsonObject["message"]?.jsonObject!!, packet.payload.jsonObject["chatId"]?.jsonPrimitive?.long!!)
                    }
//                    if (packet.opcode == OPCode.SETTINGS_UPDATE.opcode) {
//                        AccountManager.processSettings(packet.payload.jsonObject["config"]!!.jsonObject["user"]!!.jsonObject)
//                    }

                    if (packet.opcode == OPCode.PRECENSE_UPDATE.opcode) {
                        UsersManager.processPresences(packet.payload.jsonObject["userId"]!!.jsonPrimitive.long, packet.payload.jsonObject["presence"]!!.jsonObject)
                    }

                    if (packet.opcode == OPCode.TYPING.opcode) {
                        UsersManager.setTyping(packet.payload.jsonObject["chatId"]!!.jsonPrimitive.long, packet.payload.jsonObject["userId"]!!.jsonPrimitive.long)
                    }

                    if (packet.opcode == OPCode.CONTACTS_INFO.opcode) {
                        GlobalScope.launch {
                            UsersManager.processUsers(packet.payload.jsonObject["contacts"]!!.jsonArray)
                        }
                    }

                    run loop@{
                        SocketManager.packetCallbacks.forEachIndexed { i, cb ->
                            if (cb.seq == packet.seq && packet.cmd == 1) {
                                cb.callback(packet)
                                SocketManager.packetCallbacks.removeAt(i)
                                return@loop
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SocketManager", "Error while trying to get packets: $e")
        } finally {
            receiveChannel.cancel()
            withContext(Dispatchers.IO) {
                socket.close()
            }
        }
    }

    fun generateFingerprint(callsSeed: Long): ByteArray {
        val seedBytes = ByteBuffer
            .allocate(8)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(callsSeed)
            .array()

        val deviceBytes = AccountManager.deviceId.toByteArray(StandardCharsets.UTF_8)

        fun sha256(vararg parts: ByteArray): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")

            parts.forEach { digest.update(it) }

            return digest.digest()
        }

        val h1 = sha256(
            certificate_meta_sha256.hexToByteArray(),
            seedBytes,
            deviceBytes
        )

        val h2 = sha256(
            dex_meta_sha256.hexToByteArray(),
            seedBytes,
            deviceBytes
        )

        val h3 = sha256(
            so_meta_sha256[Build.SUPPORTED_ABIS[0]]!!.hexToByteArray(),
            seedBytes,
            deviceBytes
        )

        return h1 + h2 + h3
    }

    suspend fun sendPhoneNumber(phone : String, callback: (Packet) -> Unit) {
        val payload = PhoneNumber(
            mode = generateFingerprint(AccountManager.callsSeed),
            type = "START_AUTH",
            phone = phone
        )
        val mapper = MessagePackMapper()

        val bytes = mapper.writeValueAsBytes(payload)

        sendPacket(OPCode.START_AUTH, bytes, callback)
    }

    suspend fun resendPhoneNumber(phone : String, callback: (Packet) -> Unit) {
        val payload = PhoneNumber(
            mode = generateFingerprint(AccountManager.callsSeed),
            type = "RESEND",
            phone = phone
        )
        val mapper = MessagePackMapper()

        val bytes = mapper.writeValueAsBytes(payload)

        sendPacket(OPCode.START_AUTH, bytes, callback)
    }

    suspend fun sendCode(code : String, token : String, callback: (Packet) -> Unit) {
        val payload = JsonObject(
            mapOf(
                "token" to JsonPrimitive(token),
                "verifyCode" to JsonPrimitive(code),
                "authTokenType" to JsonPrimitive("CHECK_CODE")
            )
        )

        sendPacket(OPCode.CHECK_CODE, payload, callback)
    }

    private suspend fun sendPing() {
        while (true) {
            if (_socketState.value == 2) {
                delay(20.seconds)
                sendPacket(OPCode.PING, JsonObject(emptyMap()), {})
                Log.i("SocketManager", "Ping!")
            }
        }
    }
}
