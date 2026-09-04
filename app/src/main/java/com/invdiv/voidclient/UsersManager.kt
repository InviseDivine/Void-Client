package com.invdiv.voidclient

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.collections.contains

data class User(
    val avatarUrl: String? = "",
    val firstName: String = "",
    val lastName: String? = "",
    val lastSeen: Pair<Long, Int>? = Pair(0L, 0),
    val description : String? = "",
    val typing : Long? = 0L
)

object UsersManager {
    private val _usersList = MutableStateFlow<Map<Long, User>>(emptyMap())
    val usersList = _usersList.asStateFlow()

    fun clearUsersList() {
        _usersList.update {
            emptyMap()
        }
    }

    suspend fun processUsers(contacts: JsonArray) {
        var usersId : MutableList<JsonPrimitive> = mutableListOf()

        val newUsers = buildMap {
            for (contact in contacts) {
                try {
                    val userID =
                        contact.jsonObject["id"]!!.jsonPrimitive.long
                    usersId += JsonPrimitive(userID)

                    val firstName =
                        contact.jsonObject["names"]!!
                            .jsonArray[0]
                            .jsonObject["firstName"]!!
                            .jsonPrimitive.content
                    val avatarUrl =
                        contact.jsonObject["baseUrl"]
                            ?.jsonPrimitive
                            ?.content ?: ""
                    val lastName =
                        contact.jsonObject["names"]!!
                            .jsonArray[0]
                            .jsonObject["lastName"]
                            ?.jsonPrimitive
                            ?.content ?: ""

                    val desc =
                        contact.jsonObject["description"]
                            ?.jsonPrimitive
                            ?.content ?: ""

                    put(
                        userID,
                        User(
                            avatarUrl,
                            firstName,
                            lastName,
                            _usersList.value[userID]?.lastSeen ?: Pair(0L, 0),
                            desc,
                            0L
                        )
                    )

                } catch (e: Exception) {
                    Log.e("UserManager", "Error: $e")
                }
            }
        }

        _usersList.update {
            it + newUsers
        }

        val payload = JsonObject(
            mapOf(
                "contactIds" to JsonArray(usersId),
            )
        )

        SocketManager.sendPacket(
            OPCode.LAST_SEEN, payload, { packet ->
                if (packet.payload is JsonObject) {
                    GlobalScope.launch {
                        if (!packet.payload.contains("userId") && !packet.payload.contains("error") && packet.payload.contains("presence")) {
                            processPresences(packet.payload["presence"]!!.jsonObject)
                        }
                    }
                }
            }
        )

        Log.i("UserManager", "${usersList.value}")
    }

    suspend fun processPresences(presence : JsonObject) {
        for (presence in presence) {
            val userID = presence.key.toLong()

            try {
                _usersList.update { oldMap ->
                    oldMap + (userID to User(
                        oldMap[userID]?.avatarUrl ?: "",
                        oldMap[userID]?.firstName ?: "",
                        oldMap[userID]?.lastName ?: "",
                        Pair(presence.value.jsonObject["seen"]?.jsonPrimitive?.long ?: 0L, presence.value.jsonObject["status"]?.jsonPrimitive?.int ?: 0),
                        oldMap[userID]?.description ?: "",
                        oldMap[userID]?.typing ?: 0L
                    ))
                }
            } catch (e: Exception) {
                Log.e("ChatsManager", "Error while trying to update presence message: $e")
            }
        }
    }

    suspend fun processPresences(userID : Long, presence : JsonObject) {
        try {
            _usersList.update { oldMap ->
                oldMap + (userID to User(
                    oldMap[userID]?.avatarUrl ?: "",
                    oldMap[userID]?.firstName ?: "",
                    oldMap[userID]?.lastName ?: "",
                    Pair(presence["seen"]?.jsonPrimitive?.long ?: 0L, presence["status"]?.jsonPrimitive?.int ?: 0),
                    oldMap[userID]?.description ?: "",
                    oldMap[userID]?.typing ?: 0L
                ))
            }
        } catch (e: Exception) {
            Log.e("ChatsManager", "Error while trying to remove message: $e")
        }
    }

    suspend fun setTyping(typingChat : Long, userID : Long) {
        try {
            _usersList.update { oldMap ->
                oldMap + (userID to User(
                    oldMap[userID]?.avatarUrl ?: "",
                    oldMap[userID]?.firstName ?: "",
                    oldMap[userID]?.lastName ?: "",
                    oldMap[userID]?.lastSeen ?: Pair(0L, 0),
                    oldMap[userID]?.description ?: "",
                    typingChat
                ))
            }
        } catch (e: Exception) {
            Log.e("ChatsManager", "Error while trying to remove message: $e")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun checkForExisting(user: Long) {
        if (!usersList.value.containsKey(user) && user != 0L) {
            val payload = JsonObject(
                mapOf(
                    "contactIds" to JsonArray(
                        listOf(
                            Json.encodeToJsonElement(
                                Long.serializer(), user
                            )
                        )
                    ),
                )
            )

            GlobalScope.launch {
                SocketManager.sendPacket(OPCode.CONTACTS_INFO, payload, { packet ->
                    if (packet.payload is JsonObject) {
                        GlobalScope.launch {
                            if (packet.payload["contacts"]?.jsonArray?.isNotEmpty() == true) {
                                processUsers(packet.payload["contacts"]!!.jsonArray)
                            }
                        }
                    }
                })
            }
        }
    }
}