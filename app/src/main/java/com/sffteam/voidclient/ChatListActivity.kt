package com.sffteam.voidclient

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.voidclient.preferences.SettingsActivity
import com.sffteam.voidclient.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Duration
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale.getDefault
import kotlin.collections.get
import kotlin.time.ExperimentalTime
import kotlin.time.Instant.Companion.fromEpochMilliseconds

class ChatListActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent @OptIn(ExperimentalMaterial3Api::class) {
            Utils.windowSize = calculateWindowSizeClass(this)
            AppTheme {
                DrawChatList()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun DrawChatList() {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        var inputText by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf("") }

        AlertDialog(title = {
            Text(text = "Войти в группу")
        }, text = {
            Column() {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Введите ссылку на группу") },
                    singleLine = true
                )

                Text(
                    errorText
                )
            }

        }, onDismissRequest = {
            showPopup = false
        }, confirmButton = {
            TextButton(
                onClick = {
                    val urlJoin = inputText.replace("https://max.ru/", "")
                    val packetSend = SocketManager.packPacket(
                        OPCode.JOIN_CHAT.opcode, JsonObject(
                            mapOf(
                                "link" to JsonPrimitive(urlJoin)
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(packetSend, callback = { packet ->
                            if (packet.payload is JsonObject) {
                                if ("error" in packet.payload) {
                                    errorText = packet.payload["localizedMessage"].toString()
                                } else {
                                    GlobalScope.launch {
                                        ChatManager.processChats(JsonArray(listOf(packet.payload["chat"]?.jsonObject) as List<JsonElement>))
                                    }

                                    showPopup = false
                                    showBottomSheet = false
                                }
                            }
                        })
                    }


                }) {
                Text("Войти", fontSize = 20.sp)
            }
        }, dismissButton = {
            TextButton(
                onClick = {
                    showPopup = false
                }) {
                Text("Отмена", fontSize = 20.sp)
            }
        })
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            }, sheetState = sheetState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPopup = true
                        }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "edit message",
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp)
                            .align(Alignment.CenterVertically)

                    )

                    Text(
                        text = "Войти в группу",
                        fontSize = 25.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }

    Column {
        val titleSize = when (Utils.windowSize.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 18.sp
            WindowWidthSizeClass.Medium -> 24.sp
            WindowWidthSizeClass.Expanded -> 28.sp
            else -> 24.sp
        }

        val chats by ChatManager.chatsList.collectAsState()

        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        val context = LocalContext.current
        LaunchedEffect(chats) {
            coroutineScope.launch {
                listState.scrollToItem(index = chats.size)
            }
        }

        Scaffold(topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surfaceContainer,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ), title = {
                    Text(
                        "Void Client",
                        fontSize = titleSize,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }, navigationIcon = {
                    IconButton({
                        val intent = Intent(context, SettingsActivity::class.java)

                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.Filled.Settings, contentDescription = "Меню"
                        )
                    }
                }, actions = {
                    IconButton({ showBottomSheet = true }) {
                        Icon(
                            Icons.Filled.Add, contentDescription = "Добавить чат"
                        )
                    }
                    IconButton({ }) { Icon(Icons.Filled.Search, contentDescription = "Поиск") }
                }, modifier = Modifier.heightIn(max = 200.dp)

            )
        }) {
            LazyColumn(reverseLayout = true, state = listState, modifier = Modifier.padding(it)) {
                items(chats.entries.toList().sortedBy { (_, value) ->
                    value.messages.entries.toList()
                        .maxByOrNull { (_, value) -> value.sendTime }!!.value.sendTime
                }, key = { entry ->
                    entry.key
                }) { entry ->
                    println(entry)
                    DrawUser(entry.key, entry.value, LocalContext.current, Modifier.weight(0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun DrawUser(chatID: Long, chat: Chat, context: Context, modifier: Modifier) {
    var chatTitle: String
    var chatIcon: String
    val users = UserManager.usersList.collectAsState()
    var secondUser = 0L
    val sortedMessages = chat.messages.entries.toList().sortedBy { (_, value) -> value.sendTime }

    val lastMessage = sortedMessages.last().value

    if (chat.type == "DIALOG" && chatID != 0L) {
        for (i in chat.users.toList()) {
            if (i.first != AccountManager.accountID) {
                secondUser = i.first
                break
            }
        }
        val user = users.value[secondUser]

        chatTitle = user?.firstName + " " + user?.lastName
        chatIcon = user?.avatarUrl.toString()
    } else {
        chatTitle = chat.title
        chatIcon = chat.avatarUrl
    }

    Box(
        modifier = modifier
            .height(80.dp)
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("chatTitle", chatTitle)
                intent.putExtra("chatIcon", chatIcon)
                intent.putExtra("chatID", chatID)
                intent.putExtra("messageTime", lastMessage.sendTime)
                intent.putExtra("chatType", chat.type)

                context.startActivity(intent)
            }
            .background(Color.Transparent)
            .padding(start = 12.dp, end = 16.dp),
        contentAlignment = Alignment.Center) {
        val fontTitleSize = when (Utils.windowSize.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 20.sp
            WindowWidthSizeClass.Medium -> 24.sp
            WindowWidthSizeClass.Expanded -> 30.sp
            else -> 24.sp
        }

        val fontSize = when (Utils.windowSize.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 18.sp
            WindowWidthSizeClass.Medium -> 24.sp
            WindowWidthSizeClass.Expanded -> 28.sp
            else -> 24.sp
        }

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            if (chatIcon.isNotEmpty()) {
                AsyncImage(
                    model = chatIcon,
                    contentDescription = "ChatIcon",
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.FillBounds
                )
            } else if (chatID == 0L) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                ) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = "edit message",
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.Center)
                    )
                }
            } else {
                val initial =
                    chatTitle.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                        .uppercase(getDefault())

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Utils.getColorForAvatar(chatTitle).first,
                                    Utils.getColorForAvatar(chatTitle).second
                                )
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    val lastMessageTime = lastMessage.sendTime
                    val currentTime = Date().time

                    val instantLast = fromEpochMilliseconds(lastMessageTime)

                    val duration = Duration.ofSeconds(currentTime / 1000 - lastMessageTime / 1000)

                    val localDateTime = instantLast.toLocalDateTime(TimeZone.currentSystemDefault())

                    val hours = if (localDateTime.hour < 10) {
                        "0${localDateTime.hour}"
                    } else {
                        localDateTime.hour
                    }

                    val minutes = if (localDateTime.minute < 10) {
                        "0${localDateTime.minute}"
                    } else {
                        localDateTime.minute
                    }

                    val dayOfWeek = localDateTime.dayOfWeek.toJavaDayOfWeek().getDisplayName(
                        TextStyle.SHORT, getDefault()
                    )

                    val time = if (duration.toHours() < 24) {
                        "${hours}:${minutes}"
                    } else if (duration.toHours() >= 24 && duration.toDays() < 7) {
                        dayOfWeek.toString().replaceFirstChar { it.titlecase(getDefault()) }
                    } else {
                        "${localDateTime.day}.${localDateTime.month.number}.${localDateTime.year}"
                    }

                    Text(
                        chatTitle,
                        fontSize = fontTitleSize,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(0.45f)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = time, fontSize = 16.sp, modifier = Modifier.alpha(0.7f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (!(lastMessage.attaches?.jsonArray?.isNotEmpty() == true && lastMessage.attaches.jsonArray?.last()?.jsonObject?.contains(
                            "event"
                        ) == true)
                    ) {
                        var lastMsgUser = ""

                        if (chat.type == "CHAT") {
                            if (lastMessage.senderID == AccountManager.accountID) {
                                lastMsgUser = "Вы: "
                            } else {
                                lastMsgUser =
                                    users.value[lastMessage.senderID]?.firstName.toString()

                                if (users.value[lastMessage.senderID]?.lastName?.isNotEmpty()
                                        ?: false
                                ) {
                                    lastMsgUser += " " + users.value[lastMessage.senderID]?.lastName
                                }

                                lastMsgUser += ": "
                            }
                        }

                        val lastMSGCleared = if (lastMessage.link.type == "FORWARD") {
                            lastMessage.link.msgForLink.message.replace("\n", " ")
                        } else {
                            lastMessage.message.replace("\n", " ")
                        }

                        val annotatedString = buildAnnotatedString {
                            append(lastMsgUser)

                            if (lastMessage.attaches?.jsonArray?.isNotEmpty() ?: false) {
                                lastMessage.attaches.jsonArray.forEachIndexed { index, jsonelement ->
                                    val type =
                                        jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                    if (type == "PHOTO") {
                                        val imageId = "image_$index"
                                        appendInlineContent(id = imageId)
                                        append(" ")
                                    }
                                }
                            }
                            if (lastMessage.link.msgForLink.attaches is JsonArray && lastMessage.link.type == "FORWARD") {
                                lastMessage.link.msgForLink.attaches.jsonArray.forEachIndexed { index, jsonelement ->
                                    val type =
                                        jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                    if (type == "PHOTO") {
                                        val imageId = "image_$index"
                                        appendInlineContent(id = imageId)
                                        append(" ")
                                    }
                                }
                            }

                            append(lastMSGCleared)

                            if (lastMessage.link.type == "FORWARD") {
                                appendInlineContent(id = "iconId")
                            }
                        }

                        val inlineContentMap = mutableMapOf<String, InlineTextContent>(

                        )
                        val placeholder = Placeholder(
                            width = 25.sp,
                            height = 25.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )

                        if (lastMessage.attaches?.jsonArray?.isNotEmpty() ?: false) {
                            lastMessage.attaches.jsonArray.forEachIndexed { index, jsonelement ->
                                val type = jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                if (type == "PHOTO") {
                                    val imageId = "image_$index"
                                    inlineContentMap[imageId] =
                                        InlineTextContent(placeholder) { _ ->
                                            AsyncImage(
                                                model = jsonelement.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                contentDescription = "ChatIcon",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                }
                            }
                        }

                        if (lastMessage.link.type == "FORWARD" && lastMessage.link.msgForLink.attaches is JsonArray) {
                            lastMessage.link.msgForLink.attaches.jsonArray.forEachIndexed { index, jsonelement ->
                                println("sh1t jsonelm $jsonelement")
                                val type = jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                if (type == "PHOTO") {
                                    val imageId = "image_$index"
                                    inlineContentMap[imageId] =
                                        InlineTextContent(placeholder) { _ ->
                                            AsyncImage(
                                                model = jsonelement.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                contentDescription = "ChatIcon",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                }
                            }
                        }

                        if (lastMessage.link.type == "FORWARD") {
                            inlineContentMap["iconId"] = InlineTextContent(placeholder) { _ ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "forwardIcon",
                                )
                            }
                        }
                        Text(
                            text = annotatedString,
                            fontSize = fontSize,
                            inlineContent = inlineContentMap,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.7f)
                        )
                    } else {
                        var text = ""
                        val attach = lastMessage.attaches.jsonArray.last()

                        val event = attach.jsonObject["event"]?.jsonPrimitive?.content

                        when (event) {
                            "remove" -> {
                                val peoplesRemoved =
                                    attach.jsonObject["userId"]?.jsonPrimitive?.long

                                UserManager.checkForExisting(peoplesRemoved!!)
                                UserManager.checkForExisting(lastMessage.senderID)

                                var whomAdded = users.value[peoplesRemoved]?.firstName.toString()

                                if (users.value[peoplesRemoved]?.lastName?.isNotEmpty() == true) {
                                    whomAdded += " " + users.value[peoplesRemoved]?.lastName
                                }
                                if (lastMessage.senderID == AccountManager.accountID) {
                                    text += "Вы удалили $whomAdded"
                                } else {
                                    var whoAdded =
                                        users.value[lastMessage.senderID]?.firstName.toString()

                                    if (users.value[lastMessage.senderID]?.firstName?.isNotEmpty() == true) {
                                        whoAdded += " " + users.value[lastMessage.senderID]?.lastName
                                    }

                                    text += "$whoAdded удалил(-а) $whomAdded"
                                }
                            }

                            "new" -> {
                                var usr = ""

                                UserManager.checkForExisting(lastMessage.senderID)
                                if (chat.type != "CHANNEL") {
                                    if (lastMessage.senderID == AccountManager.accountID) {
                                        usr = "Вы"

                                        text += "$usr создали чат"
                                    } else {
                                        usr =
                                            users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                                        if (users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                                            usr += " " + users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                                        }

                                        text += "$usr создал(-а) чат"
                                    }
                                } else {
                                    text += "Канал создан"
                                }
                            }

                            "add" -> {
                                val peoplesAdded = attach.jsonObject["userIds"]?.jsonArray

                                for (i in peoplesAdded!!) {
                                    if (attach.jsonObject["userIds"]?.jsonArray?.isNotEmpty() == true) {
                                        UserManager.checkForExisting(i.jsonPrimitive.long)
                                    }
                                }

                                UserManager.checkForExisting(lastMessage.senderID)

                                if (lastMessage.senderID == AccountManager.accountID) {
                                    text += "Вы добавили "
                                } else {
                                    var whoAdded =
                                        users.value[lastMessage.senderID]?.firstName.toString()

                                    if (users.value[lastMessage.senderID]?.firstName?.isNotEmpty() == true) {
                                        whoAdded += " " + users.value[lastMessage.senderID]?.lastName
                                    }

                                    text += "$whoAdded добавил(-а) "
                                }

                                for (i in peoplesAdded) {
                                    var whomAdded =
                                        users.value[i.jsonPrimitive.long]?.firstName.toString()

                                    if (users.value[i.jsonPrimitive.long]?.lastName?.isNotEmpty() == true) {
                                        whomAdded += " " + users.value[i.jsonPrimitive.long]?.lastName
                                    }

                                    text += whomAdded
                                    if (i != peoplesAdded.last()) {
                                        text += ", "
                                    }
                                }
                            }

                            "icon" -> {
                                var userName = ""

                                UserManager.checkForExisting(lastMessage.senderID)

                                if (lastMessage.senderID == AccountManager.accountID) {
                                    userName = "Вы"

                                    text += "$userName изменили фото чата"
                                } else {
                                    userName =
                                        users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                                    if (users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                                        userName += " " + users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                                    }

                                    text += "$userName изменил(-а) фото чата"
                                }
                            }

                            "title" -> {
                                var userName = ""
                                UserManager.checkForExisting(lastMessage.senderID)

                                if (lastMessage.senderID == AccountManager.accountID) {
                                    userName = "Вы"
                                    val newTitle =
                                        attach.jsonObject["title"]?.jsonPrimitive?.content

                                    text += "$userName изменили название чата на «$newTitle»"
                                } else {
                                    userName =
                                        users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                                    if (users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                                        userName += " " + users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                                    }

                                    val newTitle =
                                        attach.jsonObject["title"]?.jsonPrimitive?.content

                                    text += "$userName изменил(-а) название чата на «$newTitle»"
                                }
                            }

                            "leave" -> {
                                var userName = ""
                                UserManager.checkForExisting(lastMessage.senderID)

                                if (lastMessage.senderID == AccountManager.accountID) {
                                    text += "Вы покинули чат"
                                } else {
                                    userName =
                                        users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                                    if (users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                                        userName += " " + users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                                    }
                                    text += "$userName покинул(-а) чат"
                                }
                            }

                            "joinByLink" -> {
                                var userName = ""
                                UserManager.checkForExisting(attach.jsonObject["userId"]?.jsonPrimitive?.long!!)

                                if (lastMessage.senderID == AccountManager.accountID) {
                                    text += "Вы присоединились к чату"
                                } else {
                                    userName =
                                        users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                                    if (users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                                        userName += " " + users.value[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                                    }
                                    text += "$userName присоединился(-ась) к чату"
                                }
                            }

                            "system" -> {
                                text += attach.jsonObject["message"]?.jsonPrimitive?.content
                            }
                        }
                        Text(
                            text = text,
                            fontSize = fontSize,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.7f)
                        )
                    }
                }
            }
        }
    }
}