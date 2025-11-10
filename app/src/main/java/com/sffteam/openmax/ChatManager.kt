package com.sffteam.openmax

import android.R
import androidx.compose.animation.core.animateValueAsState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.jvm.Throws
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlin.collections.mutableMapOf
import kotlin.collections.listOf
data class Chat(
    var lastMessageTime : Long,
    var lastMessage : String,
    var avatarUrl : String,
    var title : String
)


fun Chat.toChatMapPair() : List<Pair<String, Any>> {
    return listOf(
        "lastMessageTime" to this.lastMessageTime,
        "lastMessage" to this.lastMessage,
        "avatarUrl" to this.avatarUrl,
        "title" to this.title
    )

}
object ChatManager {
    val chatsList = mutableMapOf<Long, Chat>()

    /* Function. */
    fun processChats(chats : JsonArray) : Boolean {
        for (i in chats) {
            var chatID : Long = 0

            try {
                chatID = i.jsonObject["id"]!!.jsonPrimitive.long
            } catch (e : Exception) {
                println(e)
            }
            println(chatID)
            try {
                var lastmsgtm = 0L
                var lastmsg = ""
                var avatarUrl = ""
                var title = ""

                try {
                     lastmsgtm = i.jsonObject["lastMessage"]!!.jsonObject["time"]!!.jsonPrimitive.long
                } catch (e : Exception) {
                    println(e)
                    println("0msg")

                }

                try {
                    lastmsg = i.jsonObject["lastMessage"]!!.jsonObject["text"]!!.jsonPrimitive.content
                } catch (e : Exception) {
                    println("1msg")
                    println(e)
                }

                if (i.jsonObject.contains("baseIconUrl")) {
                    try {
                        avatarUrl = i.jsonObject["baseIconUrl"]!!.jsonPrimitive.content
                    } catch (e : Exception) {
                        println(e)
                        println("2msg")
                    }
                }
                if (i.jsonObject.contains("title")) {
                    try {
                        title = i.jsonObject["title"]!!.jsonPrimitive.content
                    } catch (e : Exception) {
                        println(e)
                        println("3msg")
                    }
                }

                chatsList.set(chatID,
                    Chat(
                        lastmsgtm,
                        lastmsg,
                        avatarUrl,
                        title
                    )
                )
            } catch (e : Exception) {
                println(e)
            }


            println(chatsList)
            println("processing")

        }

        return true
    }

    fun getChatList() : Map<Long, Chat> {
        return chatsList
    }
}