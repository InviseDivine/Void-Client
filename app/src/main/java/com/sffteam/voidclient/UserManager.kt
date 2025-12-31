package com.sffteam.voidclient

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class User(
    val avatarUrl: String, val firstName: String, val lastName: String, val lastSeen: Long, val description : String
)

object UserManager {
    private val _usersList = MutableStateFlow<Map<Long, User>>(emptyMap())
    var usersList = _usersList.asStateFlow()

    fun processMyProfile(profile : JsonObject) {
        println("myprofile $profile")
        var userID = 0L
        try {
            userID = profile.jsonObject["id"]!!.jsonPrimitive.long
        } catch (e: Exception) {
            println(e)
        }

        println(userID)

        try {
            var avatarUrl = ""
            var firstName = ""
            var lastName = ""
            var lastSeen = 0L
            var desc = ""
            try {
                avatarUrl = profile.jsonObject["baseUrl"]!!.jsonPrimitive.content
            } catch (e: Exception) {
                println(e)
                println("0msg")
            }

            try {
                firstName =
                    profile.jsonObject["names"]!!.jsonArray[0].jsonObject["firstName"]?.jsonPrimitive!!.content
            } catch (e: Exception) {
                println("1msg")
                println(e)
            }

            try {
                lastName =
                    profile.jsonObject["names"]!!.jsonArray[0].jsonObject["lastName"]?.jsonPrimitive!!.content
            } catch (e: Exception) {
                println("5msg")
                println(e)
            }

            val currentMap = mapOf(
                userID to User(
                    avatarUrl, firstName, lastName, 0L, desc
                )
            )

            _usersList.update {
                it.toMap() + currentMap
            }

        } catch (e: Exception) {
            println(e)
        }
    }

    // TODO: Process users presence
    fun processPresence(presences: JsonArray) {
        for (i in presences.jsonObject.toList()) {
            val prs = i.second.jsonObject["seen"]?.jsonPrimitive?.long

            _usersList.update { oldMap ->
                oldMap + (i.first.toLong() to User(
                    oldMap[i.first.toLong()]?.avatarUrl ?: "",
                    oldMap[i.first.toLong()]?.firstName ?: "",
                    oldMap[i.first.toLong()]?.lastName ?: "",
                    i.second.jsonObject["seen"]?.jsonPrimitive?.long ?: 0L,
                    oldMap[i.first.toLong()]?.description ?: ""
                ))
            }
        }
    }

    fun processUsers(contacts: JsonArray) {
        println("cool users $contacts")
        for (i in contacts) {
            var userID: Long = 0

            try {
                userID = i.jsonObject["id"]!!.jsonPrimitive.long
            } catch (e: Exception) {
                println(e)
            }

            println(userID)

            try {
                var avatarUrl = ""
                var firstName = ""
                var lastName = ""
                var lastSeen = 0L
                var desc = ""

                try {
                    avatarUrl = i.jsonObject["baseUrl"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println(e)
                    println("0msg")
                }

                try {
                    firstName =
                        i.jsonObject["names"]!!.jsonArray[0].jsonObject["firstName"]?.jsonPrimitive!!.content
                } catch (e: Exception) {
                    println("1msg")
                    println(e)
                }

                try {
                    lastName =
                        i.jsonObject["names"]!!.jsonArray[0].jsonObject["lastName"]?.jsonPrimitive!!.content
                } catch (e: Exception) {
                    println("5msg")
                    println(e)
                }

                try {
                    desc =
                        i.jsonObject["description"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println("5msg")
                    println(e)
                }


                val currentMap = mapOf(
                    userID to User(
                        avatarUrl, firstName, lastName, 0L, desc
                    )
                )

                _usersList.update {
                    it.toMap() + currentMap
                }

            } catch (e: Exception) {
                println(e)
            }
            println(_usersList.value.toMap())
            println("processing")
        }
    }

    fun checkForExisting(user: Long) {
        if (!usersList.value.containsKey(user)) {
            val packet = SocketManager.packPacket(
                OPCode.CONTACTS_INFO.opcode, JsonObject(
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
            )
            GlobalScope.launch {
                SocketManager.sendPacket(
                    packet, { packet ->
                        println(packet.payload)
                        if (packet.payload is JsonObject) {
                            GlobalScope.launch {
                                processUsers(packet.payload["contacts"]!!.jsonArray)
                            }
                        }
                    })
            }
        }
    }
}