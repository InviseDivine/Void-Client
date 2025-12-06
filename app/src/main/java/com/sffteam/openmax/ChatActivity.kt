package com.sffteam.openmax

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.openmax.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.Locale.getDefault
import kotlin.time.ExperimentalTime
import kotlin.time.Instant.Companion.fromEpochMilliseconds

class ChatActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val chatTitle: String = intent.getStringExtra("chatTitle").toString()
        val chatUrl: String = intent.getStringExtra("chatIcon").toString()
        val chatID: Long = intent.getLongExtra("chatID", 0L)
        val messageTime: Long = intent.getLongExtra("messageTime", 0L)
        val type: String = intent.getStringExtra("chatType").toString()

        println(ChatManager.chatsList.value[chatID]?.messages?.size)
        if (ChatManager.chatsList.value[chatID]?.messages?.size == 1) {
            val packet = SocketManager.packPacket(
                OPCode.CHAT_MESSAGES.opcode,
                JsonObject(
                    mapOf(
                        "chatId" to JsonPrimitive(chatID),
                        "from" to JsonPrimitive(messageTime),
                        "forward" to JsonPrimitive(0),
                        "backward" to JsonPrimitive(30),
                        "getMessages" to JsonPrimitive(true)
                    )
                )
            )
            GlobalScope.launch {
                SocketManager.sendPacket(
                    packet,
                    { packet ->
                        println(packet)
                        if (packet.payload is JsonObject)
                            ChatManager.processMessages(packet.payload["messages"]!!.jsonArray, chatID)
                    }
                )
            }
        }

        setContent {
            AppTheme() {
                val chats by ChatManager.chatsList.collectAsState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBackIos,
                                            contentDescription = "Меню"
                                        )
                                    }

                                    if (!chatUrl.isEmpty() || type == "DIALOG") {
                                        AsyncImage(
                                            model = chatUrl,
                                            contentDescription = "ChatIcon",
                                            modifier = Modifier
                                                .width(50.dp)
                                                .height(50.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        val initial = chatTitle.split(" ")
                                            .mapNotNull { it.firstOrNull()?.toChar() }
                                            .take(2)
                                            .joinToString("")
                                            .uppercase(getDefault())
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .width(50.dp)
                                                .height(50.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.linearGradient( // Create a vertical gradient
                                                        colors = listOf(
                                                            Utils.getColorForAvatar(
                                                                chatTitle
                                                            ).first,
                                                            Utils.getColorForAvatar(chatTitle).second
                                                        ) // Define the colors for the gradient
                                                    )
                                                ),

                                            ) {
                                            Text(
                                                text = initial,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontSize = 25.sp
                                            )
                                        }
                                    }
                                    Column() {
                                        Text(text = chatTitle)
                                        var userDesc: String

                                        when (type) {
                                            "CHAT" -> {
                                                userDesc = when (chats[chatID]?.users?.size) {
                                                    1 -> {
                                                        "Тут только вы"
                                                    }

                                                    2, 3, 4 -> {
                                                        chats[chatID]?.users?.size.toString() + " участника"
                                                    }

                                                    else -> {
                                                        chats[chatID]?.users?.size.toString() + " участников"
                                                    }
                                                }

                                            }

                                            "CHANNEL" -> {
                                                // Should be changed to getQuantityString, but im lazy rn for it
                                                userDesc = when (chats[chatID]?.usersCount) {
                                                    1 -> {
                                                        chats[chatID]?.usersCount.toString() + " подписчик"
                                                    }

                                                    2, 3, 4 -> {
                                                        chats[chatID]?.usersCount.toString() + " подписчика"
                                                    }

                                                    else -> {
                                                        chats[chatID]?.usersCount.toString() + " подписчиков"
                                                    }
                                                }
                                            }

                                            else -> {
                                                userDesc = if (chatID == 0L) {
                                                    ""
                                                } else {
                                                    "Был(а) недавно"
                                                }
                                            }
                                        }

                                        Text(
                                            text = userDesc,
                                            fontSize = 16.sp,
                                            modifier = Modifier.alpha(0.85f)
                                        )
                                    }
                                }
                            },
                        )
                    },
                    bottomBar = {
                        BottomAppBar(
                            content = {
                                if (type == "CHANNEL") {
                                    DrawBottomChannel(chatID)
                                } else {
                                    DrawBottomDialog(chatID)
                                }
                            }
                        )
                    }
                ) {

                    val listState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()

                    var showBottomSheet by remember { mutableStateOf(false) }
                    val sheetState = rememberModalBottomSheetState()

                    var removeforall by remember { mutableStateOf(false) }

                    var selectedMSGID by remember { mutableStateOf("") }
                    var showPopup by remember { mutableStateOf(false) }

                    LaunchedEffect(chats) {
                        coroutineScope.launch {
                            listState.scrollToItem(index = chats[chatID]?.messages?.size ?: 0)

                            println(chats[chatID]!!.messages.toList().last())
                            println("scrolled2")
                        }
                    }
                    if (showPopup) {
                        AlertDialog(
                            title = {
                                Text(text = "Удалить сообщение")
                            },
                            text = {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(0.dp)
                                    ) {
                                        Checkbox(
                                            checked = removeforall,
                                            onCheckedChange = { isChecked ->
                                                removeforall = isChecked
                                            },
                                            modifier = Modifier.padding(0.dp),
                                        )
                                        Text(
                                            text = "Удалить у всех",
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(0.dp)
                                        )
                                    }
                                    Text(
                                        text = "Вы точно хотите удалить сообщение?",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )

                                }
                            },
                            onDismissRequest = {
                                showPopup = false
                                removeforall = false
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val packet = SocketManager.packPacket(
                                            OPCode.DELETE_MESSAGE.opcode,
                                            JsonObject(
                                                mapOf(
                                                    "chatId" to JsonPrimitive(chatID),
                                                    "messageIds" to JsonArray(
                                                        listOf(
                                                            Json.encodeToJsonElement(
                                                                String.serializer(),
                                                                selectedMSGID
                                                            )
                                                        )
                                                    ),
                                                    "forMe" to JsonPrimitive(!removeforall)
                                                )
                                            )
                                        )
                                        GlobalScope.launch {
                                            SocketManager.sendPacket(packet,
                                                { packet ->
                                                    if (packet.payload is JsonObject) {
                                                        println(packet)
                                                        val packetID =
                                                            packet.payload["chatId"]?.jsonPrimitive?.long


                                                        for (i in chats[packetID]?.messages!!) {
                                                            if (i.key == selectedMSGID) {
                                                                println(i.key)
                                                                println(selectedMSGID)
                                                                ChatManager.removeMessage(
                                                                    chatID,
                                                                    selectedMSGID
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        showBottomSheet = false
                                        showPopup = false
                                    }
                                ) {
                                    Text("Удалить", fontSize = 20.sp)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showPopup = false
                                        removeforall = false
                                    }
                                ) {
                                    Text("Отмена", fontSize = 20.sp)
                                }
                            }
                        )
                    }

                    if (showBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                showBottomSheet = false
                            },
                            sheetState = sheetState
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "edit message",
                                        modifier = Modifier
                                            .padding(end = 10.dp)
                                            .size(20.dp)
                                            .align(Alignment.CenterVertically)
                                    )

                                    Text(
                                        text = "Редактировать",
                                        fontSize = 25.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                    )
                                }
                                Row(modifier = Modifier
                                    .clickable {
                                        showPopup = true
                                    }
                                    .fillMaxWidth()) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "delete message",
                                        modifier = Modifier
                                            .padding(end = 10.dp)
                                            .size(20.dp)
                                            .align(Alignment.CenterVertically),
                                        tint = Color.Red
                                    )

                                    Text(
                                        text = "Удалить",
                                        fontSize = 25.sp,
                                        color = Color.Red,
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState
                    ) {
                        items(chats[chatID]?.messages?.toList() ?: emptyList()) { message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBottomSheet = true
                                        selectedMSGID = message.first
                                    },
                                horizontalArrangement = Arrangement.spacedBy(
                                    16.dp,
                                    if (message.second.senderID == AccountManager.accountID) Alignment.End else Alignment.Start
                                ),
                            ) {
                                DrawMessage(message.second)
                            }
                        }
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun DrawMessage(message: Message) {
    Box(
        modifier = Modifier
            .widthIn(max = 300.dp, min = 100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = Color.Red)
            .padding(start = 2.dp, end = 2.dp, top = 4.dp)
    ) {
        Column {
            if (message.attaches!!.jsonArray.isNotEmpty()) {
                DrawImages(message.attaches.jsonArray)
            }

            Text(
                message.message,
                fontSize = 24.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 16.dp)
            )
        }

        val instant = fromEpochMilliseconds(message.sendTime)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        var minutezero: String = "0"

        minutezero = if (localDateTime.minute < 10) {
            "0" + localDateTime.minute.toString()
        } else {
            localDateTime.minute.toString()
        }

        val time = "${localDateTime.hour}:${minutezero}"

        Row(
            modifier = Modifier
                .padding(top = 20.dp, end = 4.dp)
                .align(Alignment.BottomEnd)
        ) {
            if (message.status == "EDITED") {
                Icon(
                    Icons.Filled.Edit, contentDescription = "add", modifier = Modifier
                        .size(16.dp)
                        .align(
                            Alignment.Bottom
                        )
                        .alpha(0.8f)
                )
            }

            Text(
                time,
                modifier = Modifier
                    .align(
                        Alignment.Bottom
                    )
                    .alpha(0.8f),
            )
        }
    }
}

@Composable
fun DrawBottomDialog(chatID: Long) {
    var message by remember { mutableStateOf("") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        IconButton(onClick = {
        }) {
            Icon(Icons.Filled.Add, contentDescription = "add")
        }
        OutlinedTextField(
            value = message,
            onValueChange = { newText ->
                message = newText
            },
            placeholder = { Text("Сообщение") },
        )

        IconButton(onClick = {
            println(message)
            if (message.isNotEmpty()) {
                val packet = SocketManager.packPacket(OPCode.SEND_MESSAGE.opcode,
                    JsonObject(
                        mapOf(
                            "chatId" to JsonPrimitive(chatID),
                            "message" to JsonObject(
                                mapOf(
                                    "text" to JsonPrimitive(message),
                                    "cid" to JsonPrimitive(System.currentTimeMillis()),
                                    "elements" to JsonArray(emptyList()),
                                    "attaches" to JsonArray(emptyList())
                                )
                            ),
                            "notify" to JsonPrimitive(true)
                        )
                    ))
                GlobalScope.launch {
                    SocketManager.sendPacket(packet,
                        { packet ->
                            println(packet.payload)
                            println("msg should be added")
                            if (packet.payload is JsonObject) {
                                println("msg should be added")
                                var msgID = ""
                                var msg = Message("", 0L, 0L, JsonArray(emptyList()), "")
                                try {
                                    var status = ""
                                    try {
                                        status = ""
                                    } catch (e: Exception) {

                                    }
                                    msg = Message(
                                        packet.payload["message"]?.jsonObject["text"]!!.jsonPrimitive.content,
                                        packet.payload["message"]?.jsonObject["time"]!!.jsonPrimitive.long,
                                        packet.payload["message"]?.jsonObject["sender"]!!.jsonPrimitive.long,
                                        packet.payload["message"]?.jsonObject["attaches"]!!.jsonArray,
                                        status,
                                    )

                                    msgID =
                                        packet.payload["message"]?.jsonObject["id"]!!.jsonPrimitive.content
                                } catch (e: Exception) {
                                    println(e)
                                }

                                println(msg)
                                ChatManager.addMessage(msgID, msg, chatID)
                            }
                        }
                    )
                }
            }

            message = ""
        }) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send")
        }
    }
}

@Composable
fun DrawBottomChannel(chatID: Long) {
    TextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Text("Отключить уведомления", fontSize = 25.sp)
    }
}

@Composable
fun DrawImages(messages: JsonArray) {
    if (messages.size % 2 == 0) {
        Column {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.heightIn(max = 1000.dp),
                userScrollEnabled = false,
            ) {
                items(messages.size) { index ->

                    val photo = messages[index]
                    val type = photo.jsonObject["_type"]!!.jsonPrimitive.content

                    var topstart = 0.dp
                    var topend = 0.dp

                    if (index == 0) topstart = 16.dp
                    if (index == 1) topend = 16.dp

                    if (type == "PHOTO") {
                        println(photo)
                        AsyncImage(
                            model = photo.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                            contentDescription = "ChatIcon",
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = topstart,
                                        topEnd = topend,
                                    )
                                )
                                .fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    } else if (messages.size == 1) {
        if (messages.last().jsonObject["_type"]!!.jsonPrimitive.content == "PHOTO") {
            AsyncImage(
                model = messages.last().jsonObject["baseUrl"]!!.jsonPrimitive.content,
                contentDescription = "ChatIcon",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                        )
                    ),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Column {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.heightIn(max = 1000.dp),
                userScrollEnabled = false,
            ) {
                items(messages.size - 1) { index ->

                    val photo = messages[index]
                    val type = photo.jsonObject["_type"]!!.jsonPrimitive.content

                    if (type == "PHOTO") {
                        println(photo)
                        var topstart = 0.dp
                        var topend = 0.dp

                        if (index == 0) topstart = 16.dp
                        if (index == 1) topend = 16.dp
                        AsyncImage(
                            model = photo.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                            contentDescription = "ChatIcon",
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = topstart,
                                        topEnd = topend,
                                    )
                                )
                                .fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            AsyncImage(
                model = messages.last().jsonObject["baseUrl"]!!.jsonPrimitive.content,
                contentDescription = "ChatIcon",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
    }
}