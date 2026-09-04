package com.invdiv.voidclient

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlin.collections.emptyMap

data class MessageLink(
    val message: Message? = null,
    val type: String = ""
)

data class Message(
    val message: String? = null,
    val sendTime: Long = 0L,
    val senderID: Long? = 0L,
    val attaches: JsonElement? = null,
    val status: String? = null,
    val link: MessageLink? = null,
    val reactions: Map<String, Int>? = emptyMap(),
    val myReaction: String? = null,
    val elements: JsonElement? = null,
    val messageType : String? = null,
    val channelIcon : String? = null,
    val channelName : String? = null
)

data class Chat(
    val avatarUrl: String? = null,
    val title: String = "",
    val messages: Map<Long, Message>? = emptyMap(),
    val type: String = "",
    val users: Map<Long, Long> = emptyMap(),
    val usersCount: Int? = 0,
    val needGetMessages: Boolean = true,
    val description: String? = "",
    val admins : List<Long> = emptyList(),
    val owner : Long = 0L,
    val inviteLink : String? = "",
    val pinned : Int? = 0,
    var gotMessages : List<Long> = emptyList()
)

object ChatsManager {
    private val _chatsList = MutableStateFlow<Map<Long, Chat>>(emptyMap())
    var chatsList = _chatsList.asStateFlow()

    fun processReactions(reactionInfo : JsonObject, messageID : Long, chatID : Long) {
        if (reactionInfo.isEmpty()) {
            try {
                val newMsg = _chatsList.value[chatID]!!.messages?.get(messageID)!!.copy(reactions = emptyMap(), myReaction = "")
                _chatsList.update { oldMap ->
                    oldMap + (chatID to Chat(
                        oldMap[chatID]?.avatarUrl ?: "",
                        oldMap[chatID]?.title ?: "",
                        oldMap[chatID]?.messages?.plus(mapOf(messageID to newMsg)) ?: emptyMap(),
                        oldMap[chatID]?.type ?: "",
                        oldMap[chatID]?.users ?: emptyMap(),
                        oldMap[chatID]?.usersCount ?: 0,
                        oldMap[chatID]?.needGetMessages ?: false,
                        oldMap[chatID]?.description ?: "",
                        oldMap[chatID]?.admins ?: emptyList(),
                        oldMap[chatID]?.owner ?: 0L,
                        oldMap[chatID]?.inviteLink ?: "",
                    ))
                }
            } catch (e: Exception) {
                Log.e("ChatsManager", "Error while trying to remove reactions: $e")
            }
        } else {
            val totalCount = reactionInfo["totalCount"]!!.jsonPrimitive.int

            val reactionArray = reactionInfo.jsonObject["counters"]!!.jsonArray
            var reactions = mutableMapOf<String, Int>()
            var myReaction = ""

            for (reaction in reactionArray) {
                reactions[reaction.jsonObject["reaction"]!!.jsonPrimitive.content] = reaction.jsonObject["count"]!!.jsonPrimitive.int
            }

            if (reactionInfo.jsonObject.contains("yourReaction")) {
                myReaction = reactionInfo.jsonObject["yourReaction"]!!.jsonPrimitive.content
            }

            val newMsg = _chatsList.value[chatID]!!.messages?.get(messageID)!!.copy(reactions =reactions, myReaction = myReaction)

            try {
                _chatsList.update { oldMap ->
                    oldMap + (chatID to Chat(
                        oldMap[chatID]?.avatarUrl ?: "",
                        oldMap[chatID]?.title ?: "",
                        oldMap[chatID]?.messages?.plus(mapOf(messageID to newMsg)) ?: emptyMap(),
                        oldMap[chatID]?.type ?: "",
                        oldMap[chatID]?.users ?: emptyMap(),
                        oldMap[chatID]?.usersCount ?: 0,
                        oldMap[chatID]?.needGetMessages ?: false,
                        oldMap[chatID]?.description ?: "",
                        oldMap[chatID]?.admins ?: emptyList(),
                        oldMap[chatID]?.owner ?: 0L,
                        oldMap[chatID]?.inviteLink ?: "",
                    ))
                }
            } catch (e: Exception) {
                Log.e("ChatsManager", "Error while trying to remove reactions: $e")
            }
        }
    }

    suspend fun removeMessage(chatID: Long, messageIDs: JsonArray) {
        for (message in messageIDs) {
            try {
                _chatsList.update { oldMap ->
                    oldMap + (chatID to Chat(
                        oldMap[chatID]?.avatarUrl ?: "",
                        oldMap[chatID]?.title ?: "",
                        oldMap[chatID]?.messages?.minus(message.jsonPrimitive.long) ?: emptyMap(),
                        oldMap[chatID]?.type ?: "",
                        oldMap[chatID]?.users ?: emptyMap(),
                        oldMap[chatID]?.usersCount ?: 0,
                        oldMap[chatID]?.needGetMessages ?: false,
                        oldMap[chatID]?.description ?: "",
                        oldMap[chatID]?.admins ?: emptyList(),
                        oldMap[chatID]?.owner ?: 0L,
                        oldMap[chatID]?.inviteLink ?: "",
                    ))
                }
            } catch (e: Exception) {
                Log.e("ChatsManager", "Error while trying to remove message: $e")
            }
        }
    }

    suspend fun removeMessage(chatID: Long, messageID: Long) {
        try {
            _chatsList.update { oldMap ->
                oldMap + (chatID to Chat(
                    oldMap[chatID]?.avatarUrl ?: "",
                    oldMap[chatID]?.title ?: "",
                    oldMap[chatID]?.messages?.minus(messageID) ?: emptyMap(),
                    oldMap[chatID]?.type ?: "",
                    oldMap[chatID]?.users ?: emptyMap(),
                    oldMap[chatID]?.usersCount ?: 0,
                    oldMap[chatID]?.needGetMessages ?: false,
                    oldMap[chatID]?.description ?: "",
                    oldMap[chatID]?.admins ?: emptyList(),
                    oldMap[chatID]?.owner ?: 0L,
                    oldMap[chatID]?.inviteLink ?: "",
                ))
            }
        } catch (e: Exception) {
            Log.e("ChatsManager", "Error while trying to remove message: $e")
        }
    }

    suspend fun addMessage(message: JsonObject, chatID: Long) {
        try {
            val msg = extractMessage(message)

            _chatsList.update { oldMap ->
                oldMap + (chatID to Chat(
                    oldMap[chatID]?.avatarUrl ?: "",
                    oldMap[chatID]?.title ?: "",
                    oldMap[chatID]?.messages?.plus(mapOf(msg.first to msg.second)) ?: emptyMap(),
                    oldMap[chatID]?.type ?: "",
                    oldMap[chatID]?.users ?: emptyMap(),
                    oldMap[chatID]?.usersCount ?: 0,
                    oldMap[chatID]?.needGetMessages ?: false,
                    oldMap[chatID]?.description ?: "",
                    oldMap[chatID]?.admins ?: emptyList(),
                    oldMap[chatID]?.owner ?: 0L,
                    oldMap[chatID]?.inviteLink ?: "",
                ))
            }
        } catch (e: Exception) {
            Log.e("ChatsManager", "Error while trying to add message: $e")
        }
    }

    suspend fun processMessages(messages: JsonArray, chatID: Long) {
        val msgList: MutableMap<Long, Message> = mutableMapOf()

        try {
            for (message in messages) {
                val msg = extractMessage(message)
                msgList[msg.first] = msg.second
            }

            _chatsList.update { oldMap ->
                oldMap + (chatID to Chat(
                    oldMap[chatID]?.avatarUrl ?: "",
                    oldMap[chatID]?.title ?: "",
                    oldMap[chatID]?.messages?.plus(msgList) ?: emptyMap(),
                    oldMap[chatID]?.type ?: "",
                    oldMap[chatID]?.users ?: emptyMap(),
                    oldMap[chatID]?.usersCount ?: 0,
                    msgList.size == 30,
                    oldMap[chatID]?.description ?: "",
                    oldMap[chatID]?.admins ?: emptyList(),
                    oldMap[chatID]?.owner ?: 0L,
                    oldMap[chatID]?.inviteLink ?: "",
                ))
            }

            Log.i("ChatsManager", "Chat ${_chatsList.value[chatID]?.title} was updated")
        } catch (e: Exception) {
            Log.e("ChatsManager", "Error while trying to process messages for chat $chatID: $e")
        }
    }

    suspend fun removeChat(chatID : Long) {
        try {
            val updatedChatsList = _chatsList.value.toMutableMap()
            updatedChatsList.remove(chatID)

            _chatsList.update {
                updatedChatsList
            }
        } catch (e: Exception) {
            Log.e("ChatsManager", "Error while trying to remove chat $chatID: $e")
        }
    }

    fun updateReadMark(chatID : Long, userID : Long, mark : Long) {
        _chatsList.update { oldMap ->
            oldMap + (chatID to Chat(
                oldMap[chatID]?.avatarUrl ?: "",
                oldMap[chatID]?.title ?: "",
                oldMap[chatID]?.messages ?: emptyMap(),
                oldMap[chatID]?.type ?: "",
                oldMap[chatID]?.users?.plus(Pair(userID, mark)) ?: emptyMap(),
                oldMap[chatID]?.usersCount ?: 0,
                oldMap[chatID]?.needGetMessages ?: false,
                oldMap[chatID]?.description ?: "",
                oldMap[chatID]?.admins ?: emptyList(),
                oldMap[chatID]?.owner ?: 0L,
                oldMap[chatID]?.inviteLink ?: "",
            ))
        }
    }
    suspend fun processChats(chat: JsonObject) {
        val usersId = mutableListOf<JsonElement>()
        val chatID = chat.jsonObject["id"]!!.jsonPrimitive.long

        var avatarUrl = ""
        var title = ""
        var desc = ""
        var inviteLink = ""
        val type = chat.jsonObject["type"]!!.jsonPrimitive.content

        var lastMessage = Pair(0L, Message())

        var usersCount = 0
        var owner = 0L

        val admins : MutableList<Long> = mutableListOf()
        val users = mutableMapOf<Long, Long>()

        for (i in chat.jsonObject["participants"]?.jsonObject?.toList()!!) {
            val idEncoded = Json.encodeToJsonElement(Long.serializer(), i.first.toLong())

            if (!usersId.contains(idEncoded)) {
                usersId += idEncoded
            }

            users[i.first.toLong()] = i.second.jsonPrimitive.long
        }

        if (chat.jsonObject.contains("lastMessage")) {
            lastMessage = extractMessage(chat.jsonObject["lastMessage"]!!)

            val lastId = JsonPrimitive(lastMessage.second.senderID)

            if (!usersId.contains(lastId)) {
                usersId += lastId
            }
        }

        if (chat.jsonObject.contains("baseIconUrl")) {
            avatarUrl = chat.jsonObject["baseIconUrl"]!!.jsonPrimitive.content
        }

        if (chat.jsonObject.contains("title")) {
            title = chat.jsonObject["title"]!!.jsonPrimitive.content
        } else if (chatID == 0L) {
            title = "Избранное"
        }

        if (chat.jsonObject.contains("participantsCount")) {
            usersCount = chat.jsonObject["participantsCount"]!!.jsonPrimitive.int
        }

        if (chat.jsonObject.contains("description")) {
            desc = chat.jsonObject["description"]?.jsonPrimitive?.content!!
        }

        if (chat.jsonObject.contains("owner")) {
            owner = chat.jsonObject["owner"]?.jsonPrimitive?.long!!
        }

        if (chat.jsonObject.contains("admins")) {
            for (admin in chat.jsonObject["admins"]?.jsonArray?.toList()!!) {
                admins += admin.jsonPrimitive.long
            }
        }

        if (chat.jsonObject.contains("link")) {
            inviteLink = chat.jsonObject["link"]!!.jsonPrimitive.content
        }

        val messages: MutableMap<Long, Message> = mutableMapOf()

        if (lastMessage.first != 0L) messages[lastMessage.first] = lastMessage.second

        _chatsList.update { oldMap ->
            oldMap.toMap() + (chatID to Chat(
                avatarUrl,
                title,
                if (oldMap[chatID]?.messages?.isNotEmpty() == true) oldMap[chatID]?.messages?.plus(messages)!! else messages,
                type,
                users,
                usersCount,
                oldMap[chatID]?.needGetMessages ?: true,
                desc,
                admins,
                owner,
                inviteLink,
            ))
        }

        val payload = JsonObject(
            mapOf(
                "contactIds" to JsonArray(usersId),
            )
        )

        SocketManager.sendPacket(
            OPCode.CONTACTS_INFO, payload, { packet ->
                if (packet.payload is JsonObject) {
                    GlobalScope.launch {
                        // UsersManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                    }
                }
            }
        )
    }

    // TODO: Optimize like processUsers
    suspend fun processChats(chats: JsonArray): Boolean {
        val usersId = mutableListOf<JsonElement>()
        try {
            for (chat in chats) {
                val chatID = chat.jsonObject["id"]!!.jsonPrimitive.long

                var avatarUrl = ""
                var title = ""
                var desc = ""
                var inviteLink = ""
                val type = chat.jsonObject["type"]!!.jsonPrimitive.content

                var lastMessage = Pair(0L, Message())

                var usersCount = 0
                var owner = 0L

                val admins : MutableList<Long> = mutableListOf()
                val users = mutableMapOf<Long, Long>()

                for (i in chat.jsonObject["participants"]?.jsonObject?.toList()!!) {
                    val idEncoded = Json.encodeToJsonElement(Long.serializer(), i.first.toLong())
                    println(idEncoded)

                    if (!usersId.contains(idEncoded)) {
                        usersId += idEncoded
                    }

                    users[i.first.toLong()] = i.second.jsonPrimitive.long
                }

                if (chat.jsonObject.contains("lastMessage")) {
                    lastMessage = extractMessage(chat.jsonObject["lastMessage"]!!)
                }

                if (chat.jsonObject.contains("baseIconUrl")) {
                    avatarUrl = chat.jsonObject["baseIconUrl"]!!.jsonPrimitive.content
                }

                if (chat.jsonObject.contains("title")) {
                    title = chat.jsonObject["title"]!!.jsonPrimitive.content
                } else if (chatID == 0L) {
                    title = "Избранное"
                }

                if (chat.jsonObject.contains("participantsCount")) {
                    usersCount = chat.jsonObject["participantsCount"]!!.jsonPrimitive.int
                }

                if (chat.jsonObject.contains("description")) {
                    desc = chat.jsonObject["description"]?.jsonPrimitive?.content!!
                }

                if (chat.jsonObject.contains("owner")) {
                    owner = chat.jsonObject["owner"]?.jsonPrimitive?.long!!
                }

                if (chat.jsonObject.contains("admins")) {
                    for (admin in chat.jsonObject["admins"]?.jsonArray?.toList()!!) {
                        admins += admin.jsonPrimitive.long
                    }
                }

                if (chat.jsonObject.contains("link")) {
                    inviteLink = chat.jsonObject["link"]!!.jsonPrimitive.content
                }

                val messages: MutableMap<Long, Message> = mutableMapOf()

                if (lastMessage.first != 0L) messages[lastMessage.first] = lastMessage.second

                _chatsList.update { oldMap ->
                    oldMap.toMap() + (chatID to Chat(
                        avatarUrl,
                        title,
                        if (oldMap[chatID]?.messages?.isNotEmpty() == true) oldMap[chatID]?.messages?.plus(messages)!! else messages,
                        type,
                        users,
                        usersCount,
                        oldMap[chatID]?.needGetMessages ?: true,
                        desc,
                        admins,
                        owner,
                        inviteLink,
                    ))
                }
            }

            val payload = JsonObject(
                mapOf(
                    "contactIds" to JsonArray(usersId),
                )
            )

            SocketManager.sendPacket(
                OPCode.CONTACTS_INFO, payload, { packet ->
                    if (packet.payload is JsonObject) {
                        println("Contacts packet: ${packet.payload["contacts"]!!.jsonArray}")
                        GlobalScope.launch {
                            // UsersManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("ChatsManager", "Error while trying to process chats: $e")
        }
        return true
    }

    fun extractMessage(message: JsonElement): Pair<Long, Message> {
        var id = 0L
        var sendTime = 0L
        var senderID = 0L
        var messageText = ""
        var status = ""
        var attaches = JsonArray(emptyList())
        var elements = JsonArray(emptyList())
        var messageLink = MessageLink()
        var reactions = mutableMapOf<String, Int>()
        var myReaction = ""

        try {
            if (message.jsonObject.contains("id")) {
                id = message.jsonObject["id"]!!.jsonPrimitive.long
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get id: $e")
        }

        try {
            sendTime = message.jsonObject["time"]!!.jsonPrimitive.long
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get time: $e")
        }

        try {
            if (message.jsonObject.contains("sender")) {
                senderID = message.jsonObject["sender"]?.jsonPrimitive?.long ?: 0L
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get sender: $e")
        }

        try {
            if (message.jsonObject.contains("status")) {
                status = message.jsonObject["status"]?.jsonPrimitive?.content ?: ""
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get status of message: $e")
        }


        try {
            if (message.jsonObject.contains("text")) {
                messageText = message.jsonObject["text"]?.jsonPrimitive?.content ?: ""
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get text of message: $e")
        }

        try {
            if (message.jsonObject.contains("attaches")) {
                attaches = message.jsonObject["attaches"]?.jsonArray ?: JsonArray(emptyList())
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get attaches of message: $e")
        }

        try {
            if (message.jsonObject.contains("elements")) {
                elements = message.jsonObject["elements"]?.jsonArray ?: JsonArray(emptyList())
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get elements of message: $e")
        }

        try {
            if (message.jsonObject.contains("link")) {
                val linkedMsg = message.jsonObject["link"]

                val type = linkedMsg?.jsonObject["type"]?.jsonPrimitive?.content.toString()
                val linkText = linkedMsg?.jsonObject["message"]?.jsonObject["text"]?.jsonPrimitive?.content
                val linkSender = linkedMsg?.jsonObject["message"]?.jsonObject["sender"]?.jsonPrimitive?.long
                val linkAttaches = linkedMsg?.jsonObject["message"]?.jsonObject["attaches"]
                val status = if (linkedMsg?.jsonObject["message"]?.jsonObject?.contains("status") == true) {
                    linkedMsg.jsonObject["message"]!!.jsonObject["status"]!!.jsonPrimitive.content
                } else {
                    ""
                }

                val elements = if (linkedMsg?.jsonObject["message"]?.jsonObject?.contains("elements") == true) {
                    linkedMsg.jsonObject["message"]!!.jsonObject["elements"]
                } else {
                    JsonArray(emptyList())
                }

                val messageType = if (linkedMsg?.jsonObject["message"]?.jsonObject?.containsKey("type") == true) {
                    linkedMsg.jsonObject["message"]!!.jsonObject["type"]!!.jsonPrimitive.content
                } else {
                    ""
                }

                val channelName = if (linkedMsg?.jsonObject?.containsKey("chatName") == true) {
                    linkedMsg.jsonObject["chatName"]!!.jsonPrimitive.content
                } else {
                    ""
                }


                val channelIcon = if (linkedMsg?.jsonObject?.containsKey("chatIconUrl") == true) {
                    linkedMsg.jsonObject["chatIconUrl"]!!.jsonPrimitive.content
                } else {
                    ""
                }

                val msgLink = MessageLink(Message(linkText, 0L, linkSender, linkAttaches, status, null, null, null, elements, messageType, channelIcon, channelName), type)

                messageLink = msgLink
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get link of message: $e")
        }

        try {
            if (message.jsonObject["reactionInfo"]?.jsonObject?.containsKey("counters") == true) {
                val reactionArray = message.jsonObject["reactionInfo"]!!.jsonObject["counters"]!!.jsonArray

                for (reaction in reactionArray) {
                    reactions[reaction.jsonObject["reaction"]!!.jsonPrimitive.content] = reaction.jsonObject["count"]!!.jsonPrimitive.int
                }

                if (message.jsonObject["reactionInfo"]!!.jsonObject.contains("yourReaction")) {
                    myReaction = message.jsonObject["reactionInfo"]!!.jsonObject["yourReaction"]!!.jsonPrimitive.content
                }
            }
        } catch (e : Exception) {
            Log.e("ChatsManager", "Error when trying to get reactionInfo of message: $e")
        }

        return Pair(id, Message(messageText, sendTime, senderID, attaches, status, messageLink, reactions, myReaction, elements))
    }
}