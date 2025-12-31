package com.sffteam.voidclient

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.collections.plus

data class Session(
    val client: String = "",
    val location: String = "",
    val current: Boolean = false,
    val time: Long = 0L,
    val info : String = ""
)
object AccountManager {
    private val _sessionsList = MutableStateFlow<List<Session>>(emptyList())
    var sessionsList = _sessionsList.asStateFlow()

    var logined : Boolean = false
    var accountID: Long = 0L
    var token: String = ""
    var phone: String = ""

    fun processSession(sessions: JsonArray) {
        for (i in sessions) {
            try {
                var client: String = ""
                var location: String = ""
                var current: Boolean = false
                var time: Long = 0L
                var info : String = ""

                try {
                    client = i.jsonObject["client"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println(e)
                    println("0msg")
                }

                try {
                    location =
                        i.jsonObject["location"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println("1msg")
                    println(e)
                }

                try {
                    if (i.jsonObject.containsKey("current")) {
                        current = i.jsonObject["current"]!!.jsonPrimitive.boolean
                    }
                } catch (e: Exception) {
                    println("5msg")
                    println(e)
                }

                try {
                    time =
                        i.jsonObject["time"]!!.jsonPrimitive.long
                } catch (e: Exception) {
                    println("1msg")
                    println(e)
                }

                try {
                    info =
                        i.jsonObject["info"]!!.jsonPrimitive.content
                } catch (e: Exception) {
                    println("1msg")
                    println(e)
                }

                val currentList = listOf(
                    Session(client, location, current, time, info)
                )

                _sessionsList.update {
                    it.toList() + currentList
                }

            } catch (e: Exception) {
                println(e)
            }
        }
    }
}
