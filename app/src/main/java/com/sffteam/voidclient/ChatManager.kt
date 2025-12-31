package com.sffteam.voidclient

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class msgForLink(
    val message: String = "",
    val sendTime: Long = 0L,
    val senderID: Long = 0L,
    val attaches: JsonElement? = JsonArray(emptyList()),
    val status: String = "",
    val msgID: String = ""
)

data class MessageLink(
    val msgForLink: msgForLink = msgForLink(), val type: String = ""
)

data class Message(
    val message: String = "",
    val sendTime: Long = 0L,
    val senderID: Long = 0L,
    val attaches: JsonElement? = JsonArray(emptyList()),
    val status: String = "",
    val link: MessageLink = MessageLink()
)


data class Chat(
    val avatarUrl: String,
    val title: String,
    val messages: Map<String, Message>,
    val type: String,
    val users: Map<Long, Long>,
    val usersCount: Int,
    val needGetMessages: Boolean = true
)

object ChatManager {
    private val _chatsList = MutableStateFlow<Map<Long, Chat>>(emptyMap())
    var chatsList = _chatsList.asStateFlow()

    fun removeMessage(chatID: Long, messageID: String) {
        _chatsList.update { oldMap ->
            oldMap + (chatID to Chat(
                oldMap[chatID]?.avatarUrl ?: "",
                oldMap[chatID]?.title ?: "",
                oldMap[chatID]?.messages?.minus(messageID) ?: emptyMap(),
                oldMap[chatID]?.type ?: "",
                oldMap[chatID]?.users ?: emptyMap(),
                oldMap[chatID]?.usersCount ?: 0,
                oldMap[chatID]?.needGetMessages ?: false
            ))
        }
    }

    fun addMessage(messageID: String, message: Message, chatID: Long) {
        try {
            println("msg $message")
            _chatsList.update { oldMap ->
                oldMap + (chatID to Chat(
                    oldMap[chatID]?.avatarUrl ?: "",
                    oldMap[chatID]?.title ?: "",
                    oldMap[chatID]?.messages?.plus(mapOf(messageID to message)) ?: emptyMap(),
                    oldMap[chatID]?.type ?: "",
                    oldMap[chatID]?.users ?: emptyMap(),
                    oldMap[chatID]?.usersCount ?: 0,
                    oldMap[chatID]?.needGetMessages ?: false
                ))
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    fun processMessages(messages: JsonArray, chatID: Long) {
        val msgList: MutableMap<String, Message> = mutableMapOf()

        for (i in messages) {
            var message = Message()
            var msg = ""
            var sendtime = 0L
            var senderID = 0L
            var attachs = JsonArray(emptyList())
            var status = ""
            var msgID = ""

            var textForwarded: String = ""
            var senderForwarded: Long = 0L
            var msgForwardedID: String = ""
            var forwardedAttaches: JsonElement? = JsonNull
            var forwardedType: String = ""

            try {
                msg = i.jsonObject["text"]!!.jsonPrimitive.content
            } catch (e: Exception) {
                println(e)
            }

            try {
                sendtime = i.jsonObject["time"]!!.jsonPrimitive.long
            } catch (e: Exception) {
                println(e)
            }

            try {
                senderID = i.jsonObject["sender"]!!.jsonPrimitive.long
            } catch (e: Exception) {
                println(e)
            }

            try {
                attachs = i.jsonObject["attaches"]!!.jsonArray
            } catch (e: Exception) {
                println(e)
            }

            try {
                status = i.jsonObject["status"]!!.jsonPrimitive.content
            } catch (e: Exception) {
                println(e)
            }

            try {
                msgID = i.jsonObject["id"]!!.jsonPrimitive.content
            } catch (e: Exception) {
                println(e)
            }

            if (i.jsonObject.contains("link")) {
                try {
                    val msgLink = i.jsonObject["link"]
                    forwardedType = msgLink!!.jsonObject["type"]?.jsonPrimitive!!.content
                    textForwarded =
                        msgLink.jsonObject["message"]!!.jsonObject["text"]!!.jsonPrimitive.content
                    senderForwarded =
                        msgLink.jsonObject["message"]!!.jsonObject["sender"]!!.jsonPrimitive.long
                    msgForwardedID =
                        msgLink.jsonObject["message"]!!.jsonObject["id"]!!.jsonPrimitive.content
                    forwardedAttaches = msgLink.jsonObject["message"]!!.jsonObject["attaches"]
                } catch (e: Exception) {
                    println(e)
                }
            }

            message = message.copy(
                message = msg,
                sendTime = sendtime,
                senderID = senderID,
                attaches = attachs,
                status = status,
                link = MessageLink(
                    type = forwardedType, msgForLink = msgForLink(
                        message = textForwarded,
                        senderID = senderForwarded,
                        attaches = forwardedAttaches,
                        msgID = msgForwardedID,
                    )
                )
            )

            println("coolmsg $message")
            msgList[msgID] = message
        }

        println("notcool size=${msgList.size}")
        _chatsList.update { oldMap ->
            oldMap + (chatID to Chat(
                oldMap[chatID]?.avatarUrl ?: "",
                oldMap[chatID]?.title ?: "",
                oldMap[chatID]?.messages?.plus(msgList) ?: emptyMap(),
                oldMap[chatID]?.type ?: "",
                oldMap[chatID]?.users ?: emptyMap(),
                oldMap[chatID]?.usersCount ?: 0,
                if (msgList.size == 30) true else false
            ))
        }
        println(_chatsList.value[chatID]?.messages?.size)
    }

    /* Function. */
    suspend fun processChats(chats: JsonArray): Boolean {
        for (i in chats) {
            var chatID: Long = 0

            try {
                chatID = i.jsonObject["id"]!!.jsonPrimitive.long
            } catch (e: Exception) {
                println(e)
            }

            println(chatID)

            try {
                var lastmsgtm = 0L
                var msgID = ""
                var lastmsg = ""
                var avatarUrl = ""
                var status = ""
                var title = ""
                var senderID = 0L
                var type = ""
                var users = mutableMapOf<Long, Long>()
                var attaches: JsonElement? = JsonNull
                var usersCount = 0

                var textForwarded: String = ""
                var senderForwarded: Long = 0L
                var msgForwardedID: String = ""
                var forwardedAttaches: JsonElement? = JsonNull
                var forwardedType: String = ""

                try {
                    lastmsgtm =
                        i.jsonObject["lastMessage"]!!.jsonObject["time"]!!.jsonPrimitive.long
                } catch (e: Exception) {
                    println(e)
                    println("0msg")
                }

                try {
                    lastmsg =
                        i.jsonObject["lastMessage"]!!.jsonObject["text"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println("1msg")
                    println(e)
                }

                try {
                    senderID =
                        i.jsonObject["lastMessage"]!!.jsonObject["sender"]!!.jsonPrimitive.long
                } catch (e: Exception) {
                    println("5msg")
                    println(e)
                }

                try {
                    status =
                        i.jsonObject["lastMessage"]!!.jsonObject["status"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println("5msg")
                    println(e)
                }

                try {
                    msgID = i.jsonObject["lastMessage"]!!.jsonObject["id"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println("5msg")
                    println(e)
                }


                if (i.jsonObject["lastMessage"]!!.jsonObject.contains("attaches")) {
                    println("supercool attach $i.jsonObject[\"lastMessage\"]!!.jsonObject.contains(\"attaches\")")
                    try {
                        attaches = i.jsonObject["lastMessage"]!!.jsonObject["attaches"]
                    } catch (e: Exception) {
                        println(e)
                    }
                } else {
                    attaches = JsonArray(emptyList())
                }

                if (i.jsonObject["lastMessage"]!!.jsonObject.contains("link")) {
                    try {
                        val msgLink = i.jsonObject["lastMessage"]!!.jsonObject["link"]
                        forwardedType = msgLink!!.jsonObject["type"]?.jsonPrimitive!!.content
                        textForwarded =
                            msgLink.jsonObject["message"]!!.jsonObject["text"]!!.jsonPrimitive.content
                        senderForwarded =
                            msgLink.jsonObject["message"]!!.jsonObject["sender"]!!.jsonPrimitive.long
                        msgForwardedID =
                            msgLink.jsonObject["message"]!!.jsonObject["id"]!!.jsonPrimitive.content
                        forwardedAttaches = msgLink.jsonObject["message"]!!.jsonObject["attaches"]
                    } catch (e: Exception) {
                        println(e)
                    }
                }

                if (i.jsonObject.contains("baseIconUrl")) {
                    try {
                        avatarUrl = i.jsonObject["baseIconUrl"]!!.jsonPrimitive.content
                    } catch (e: Exception) {
                        println(e)
                        println("2msg")
                    }
                }

                if (i.jsonObject.contains("title")) {
                    try {
                        title = i.jsonObject["title"]!!.jsonPrimitive.content
                    } catch (e: Exception) {
                        println(e)
                        println("3msg")
                    }
                } else {
                    if (chatID == 0L) {
                        title = "Избранное"
                    }
                }

                try {
                    type = i.jsonObject["type"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println(e)
                }

                try {
                    for (i in i.jsonObject["participants"]?.jsonObject?.toList()!!) {
                        users[i.first.toLong()] = i.second.jsonPrimitive.long
                    }
                } catch (e: Exception) {
                    println(e)
                }

                if (i.jsonObject.contains("participantsCount")) {
                    try {
                        usersCount = i.jsonObject["participantsCount"]!!.jsonPrimitive.int
                    } catch (e: Exception) {
                        println(e)
                    }
                }

                val messages: Map<String, Message> = mapOf(
                    msgID to Message(
                        lastmsg, lastmsgtm, senderID, attaches, status, link = MessageLink(
                            type = forwardedType, msgForLink = msgForLink(
                                textForwarded,
                                senderID = senderForwarded,
                                attaches = forwardedAttaches,
                                msgID = msgForwardedID
                            )
                        )
                    )
                )
                val currentMap = mapOf(
                    chatID to Chat(
                        avatarUrl,
                        title,
                        messages,
                        type,
                        users,
                        usersCount,
                    )
                )

                _chatsList.update {
                    it.toMap() + currentMap
                }
            } catch (e: Exception) {
                println(e)
            }
        }

        val userIds = mutableListOf<JsonElement>()
        for (i in _chatsList.value.toMap()) {
            if (i.value.type == "DIALOG") {
                for (y in i.value.users) {
                    if (y.key != AccountManager.accountID) {
                        userIds += Json.encodeToJsonElement(Long.serializer(), y.key)
                    }
                }
            } else if (i.value.type == "CHAT") {
                userIds += Json.encodeToJsonElement(
                    Long.serializer(), i.value.messages.toList().last().second.senderID
                )
            }
        }
        val packet = SocketManager.packPacket(
            OPCode.CONTACTS_INFO.opcode, JsonObject(
                mapOf(
                    "contactIds" to JsonArray(userIds),
                )
            )
        )

        SocketManager.sendPacket(
            packet, { packet ->
                println(packet.payload)
                if (packet.payload is JsonObject) {
                    GlobalScope.launch {
                        UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                    }
                }
            }
        )

        return true
    }

}