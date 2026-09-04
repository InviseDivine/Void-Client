package com.invdiv.voidclient

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.invdiv.voidclient.SocketManager.generateFingerprint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.msgpack.jackson.dataformat.MessagePackMapper

val userSettingsKeys = mapOf(
    "CHATS_PUSH_NOTIFICATION" to String,
    "PUSH_DETAILS" to Boolean,
    "PUSH_SOUND" to String,
    "PHONE_NUMBER_PRIVACY" to String,
    "INACTIVE_TTL" to String,
    "CHATS_QUICK_REPLY" to Boolean,
    "SHOW_READ_MARK" to Boolean,
    "AUDIO_TRANSCRIPTION_ENABLED" to Boolean,
    "CHATS_LED" to Long,
    "SEARCH_BY_PHONE" to String,
    "INCOMING_CALL" to String,
    "DOUBLE_TAP_REACTION_DISABLED" to Boolean,
    "SAFE_MODE_NO_PIN" to Boolean,
    "CHATS_PUSH_SOUND" to String,
    "COMMENTS_PUSH_NOTIFICATION" to String,
    "DOUBLE_TAP_REACTION_VALUE" to String,
    "FAMILY_PROTECTION" to String,
    "LED" to Long,
    "HIDDEN" to Boolean,
    "VIBR" to Boolean,
    "CHATS_INVITE" to String,
    "PUSH_NEW_CONTACTS" to Boolean,
    "UNSAFE_FILES" to Boolean,
    "DONT_DISTURB_UNTIL" to Int,
    "CHATS_VIBR" to Boolean,
    "CONTENT_LEVEL_ACCESS" to Boolean,
    "STICKERS_SUGGEST" to String,
    "SAFE_MODE" to Boolean,
    "M_CALL_PUSH_NOTIFICATION" to String,
    "QUICK_REPLY" to Boolean
)

@Serializable
data class LoginFirstAccount(
    val chatCacheFingerprint: ByteArray = byteArrayOf(),
    val exp : Map<String, ByteArray> = emptyMap(),
    val token: String = "",
    val presenceSync: Long = 0L,
    val interactive : Boolean = false
)

@Serializable
data class LoginAccount(
    val bannersSync: Long = 0L,
    val chatCacheFingerprint: ByteArray = byteArrayOf(),
    val callsSync: Long = 0L,
    val exp : Map<String, ByteArray> = emptyMap(),
    val token: String = "",
    val presenceSync: Long = -1L,
    val configHash: String = "",
    val interactive : Boolean = false,
    val lastLogin : Long = 0L
)

data class Device(
    val time : Long,
    val client : String,
    val info : String,
    val location : String,
    val current : Boolean = false
)
object AccountManager {
    private val _devicesList = MutableStateFlow<List<Device>>(emptyList())
    val devicesList = _devicesList.asStateFlow()

    var token = ""
    var accountID = 0L
    var phone = ""

    var deviceId = ""

    var sessionId = 0
    var mtInstanceId = ""
    var callsSeed = 0L

    var inviteLink = ""
    var configHash = "00000000-0000000000000000-00000000-0000000000000000-0000000000000000-0-0000000000000000-00000000"

    private val _userSettings = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userSettings = _userSettings.asStateFlow()

    var currentDeepLink : Pair<String, String> = Pair("", "")
//    var userSettings : MutableMap<String, Any> = mutableMapOf()

    fun clearDevices() {
        val list = buildList {
            for (device in _devicesList.value) {
                if (device.current) {
                    add(
                        device
                    )
                }
            }
        }

        _devicesList.update {
            list
        }
    }

    fun processDevices(devices : JsonArray) {
        val list = buildList {
            for (device in devices) {
                val time = device.jsonObject["time"]!!.jsonPrimitive.long
                val client = device.jsonObject["client"]!!.jsonPrimitive.content
                val info = device.jsonObject["info"]!!.jsonPrimitive.content
                val location = device.jsonObject["location"]!!.jsonPrimitive.content
                val current = device.jsonObject.contains("current")
                add(
                    Device(
                        time,
                        client,
                        info,
                        location,
                        current
                    )
                )
            }
        }

        _devicesList.update {
            list
        }
    }

    suspend fun processSecuritySettings(settings : JsonObject, context : Context) = coroutineScope {
        val settingsObject = settings["user"]!!.jsonObject

        configHash = settings["hash"]!!.jsonPrimitive.content

        run {
            context.dataStore.edit { settings ->
                settings[stringPreferencesKey("configHash")] = configHash
            }
        }

        for (setting in settingsObject.toList()) {
            if (userSettingsKeys.contains(setting.first)) {
                val primitive = setting.second.jsonPrimitive
                val mutableUser : MutableMap<String, Any> = mutableMapOf()

                mutableUser[setting.first] = when {
                    primitive.booleanOrNull != null -> {
                        primitive.boolean
                    }

                    primitive.isString -> {
                        primitive.content
                    }

                    primitive.longOrNull != null -> {
                        primitive.long
                    }

                    else -> {}
                }

                _userSettings.update {
                    it + mutableUser
                }

                runBlocking {
                    try {
                        val sett = userSettings.value[setting.first]

                        context.dataStore.edit { settings ->
                            if (sett is Boolean) {
                                settings[booleanPreferencesKey(setting.first)] = sett
                            }

                            if (sett is Long) {
                                settings[longPreferencesKey(setting.first)] = sett
                            }

                            if (sett is String) {
                                settings[stringPreferencesKey(setting.first)] = sett
                            }
                        }
                    } catch (e : Exception) {
                        Log.e("AccountManager", "Exception when trying to edit DataStore: $e")
                    }
                }
            }
        }
    }

    suspend fun loginToAccount(context: Context, firstLogin : Boolean = false) = coroutineScope {
        val mapper = MessagePackMapper()

        runBlocking {
            val data = context.dataStore.data.first()

            val hash = data[stringPreferencesKey("configHash")]

            configHash = hash ?: "00000000-0000000000000000-00000000-0000000000000000-0000000000000000-0-0000000000000000-00000000"

            if (configHash != "00000000-0000000000000000-00000000-0000000000000000-0000000000000000-0-0000000000000000-00000000") {
                for (key in userSettingsKeys) {
                    val settingData = when (key.value) {
                        Boolean -> {
                            data[booleanPreferencesKey(key.key)]
                        }
                        Long -> {
                            data[longPreferencesKey(key.key)]
                        }
                        else -> {
                            data[stringPreferencesKey(key.key)]
                        }
                    }

                    if (settingData == null) {
                        configHash = "00000000-0000000000000000-00000000-0000000000000000-0000000000000000-0-0000000000000000-00000000"
                        break
                    }
                }
            }
        }

        val payload = if (firstLogin) {
            LoginFirstAccount(
                generateFingerprint(callsSeed),
                mapOf("chatsCountGroups" to byteArrayOf(
                    0x0a.toByte(),
                    0x32.toByte()
                )),
                token,
                -1,
                true
            )
        } else {
            LoginAccount(
                -1,
                generateFingerprint(callsSeed),
                -1,
                mapOf("chatsCountGroups" to byteArrayOf(
                    0x0a.toByte(),
                    0x32.toByte()
                )),
                token,
                -1,
                configHash,
                false,
                -1
            )
        }

        val bytes = mapper.writeValueAsBytes(payload)

        SocketManager.sendPacket(
            OPCode.PROFILE_INFO, bytes, { packet ->
                if (packet.payload.jsonObject.containsKey("error")) {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    token = "null"

                    runBlocking {
                        try {
                            context.dataStore.edit { settings ->
                                settings[stringPreferencesKey("token")] = "null"
                            }
                        } catch (e: Exception) {
                            Log.e("AccountManager", "Error while trying to edit token: $e")
                        }
                    }
                    context.startActivity(intent)
                    (context as Activity).finish()
                } else {
                    try {
//                        val intent = Intent(
//                            context, ChatsListActivity::class.java
//                        )
                        if (packet.payload.jsonObject["config"]?.jsonObject?.isNotEmpty() == true) {
                            configHash = packet.payload.jsonObject["config"]!!.jsonObject["hash"]!!.jsonPrimitive.content

                            runBlocking {
                                context.dataStore.edit { settings ->
                                    settings[stringPreferencesKey("configHash")] = configHash
                                }
                            }

                            if (packet.payload.jsonObject["config"]!!.jsonObject.containsKey("user")) {
                                for (setting in packet.payload.jsonObject["config"]?.jsonObject["user"]!!.jsonObject.toList()) {
                                    if (userSettingsKeys.contains(setting.first)) {
                                        val primitive = setting.second.jsonPrimitive
                                        val mutableUser : MutableMap<String, Any> = mutableMapOf()

                                        mutableUser[setting.first] = when {
                                            primitive.booleanOrNull != null -> {
                                                primitive.boolean
                                            }

                                            primitive.isString -> {
                                                primitive.content
                                            }

                                            primitive.longOrNull != null -> {
                                                primitive.long
                                            }

                                            else -> {}
                                        }

                                        _userSettings.update {
                                            it + mutableUser
                                        }

                                        runBlocking {
                                            try {
                                                val sett = userSettings.value[setting.first]

                                                context.dataStore.edit { settings ->
                                                    if (sett is Boolean) {
                                                        settings[booleanPreferencesKey(setting.first)] = sett
                                                    }

                                                    if (sett is Long) {
                                                        settings[longPreferencesKey(setting.first)] = sett
                                                    }

                                                    if (sett is String) {
                                                        settings[stringPreferencesKey(setting.first)] = sett
                                                    }
                                                }
                                            } catch (e : Exception) {
                                                Log.e("AccountManager", "Error while trying to edit DataStore: $e")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        accountID =
                            packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["id"]!!.jsonPrimitive.long
                        phone =
                            packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["phone"]!!.jsonPrimitive.content
                        // AccountManager.processSettings(packet.payload.jsonObject["config"]!!.jsonObject["user"]!!.jsonObject)
                        val payload = JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(listOf(JsonPrimitive(accountID))),
                            )
                        )

                        GlobalScope.launch {
                            UsersManager.processUsers(JsonArray(listOf(packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject)))

                            SocketManager.sendPacket(
                                OPCode.CONTACTS_INFO, payload, { packet ->
                                    if (packet.payload is JsonObject) {
                                        GlobalScope.launch {
                                            UsersManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                        }
                                    }
                                }
                            )
                        }
                        GlobalScope.launch {
                            ChatsManager.processChats(packet.payload.jsonObject["chats"]!!.jsonArray)
                        }

//                        context.startActivity(intent)

                    } catch (e: Exception) {
                        Log.e("AccountManager", "Error while trying to get AccountInfo: $e")
                    }
                }
            }
        )
    }
}