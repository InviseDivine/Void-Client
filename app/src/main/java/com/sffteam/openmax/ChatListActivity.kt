package com.sffteam.openmax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.sffteam.openmax.ChatManager
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ChatListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Shitcode, but currently i don't know how to fix that :(
        while(!WebsocketManager.IsConnected()) {
        }

        if (WebsocketManager.IsConnected()) {
            WebsocketManager.SendPacket(
                OPCode.PROFILE_INFO.opcode,
                JsonObject(
                    mapOf(
                        "interactive" to JsonPrimitive(true),
                        "token" to JsonPrimitive(intent.getStringExtra("token")),
                        "chatsCount" to JsonPrimitive(40),
                        "chatsSync" to JsonPrimitive(0),
                        "contactsSync" to JsonPrimitive(0),
                        "presenceSync" to JsonPrimitive(0),
                        "draftsSync" to JsonPrimitive(0),
                    )
                ),
                { packet ->
                    println("processin1g")
                    try {
                        val test = packet.payload.jsonObject["chats"]!!.jsonArray
                        println(test)
                    } catch (e: Exception) {
                        println(e)
                    }
                    println()
                    while (!ChatManager.processChats(packet.payload.jsonObject["chats"]!!.jsonArray)) {
                        println("test1")
                    }
                    println("processi2ng")

                }
            )
        }

        setContent  @OptIn(ExperimentalMaterial3Api::class) {
            Column() {
                CenterAlignedTopAppBar(title= {
                        Text("Open MAX", fontSize = 22.sp, textAlign = TextAlign.Center,)
                    },
                )
                LazyColumn() {
                    var llist : List<MutableMap.MutableEntry<Long, Chat>> = emptyList()
                    try {
                        llist = ChatManager.chatsList.entries.toList()
                        println(llist)
                        println("TEST")
                    } catch (e : Exception) {
                        println(e)
                    }

                    items(llist) { entry ->
                        println(entry)
                        DrawUser(entry.value)
                    }
                }
            }
        }
    }
}

@Composable
fun DrawUser(chat : Chat) {
    Button(
        shape = RectangleShape,
        onClick = {
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.Start
        ) {
            AsyncImage(
                model = chat.avatarUrl,
                contentDescription = "Image",
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp)
                    .clip(CircleShape)
            )

            Column {
                Text(chat.title, fontSize = 35.sp, textAlign = TextAlign.Start)

                Text(chat.lastMessage, fontSize = 20.sp, textAlign = TextAlign.Start)
            }
        }
    }
}
