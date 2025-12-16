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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.openmax.ui.theme.AppTheme
import com.sffteam.openmax.ui.theme.primaryContainerDark
import com.sffteam.openmax.ui.theme.primaryDark
import io.ktor.http.websocket.websocketServerAccept
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
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
import sh.calvin.autolinktext.rememberAutoLinkText
import java.util.Locale.getDefault
import kotlin.time.ExperimentalTime
import kotlin.time.Instant.Companion.fromEpochMilliseconds

class ChatActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        println("joined to chat")
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
                val sortedChats = chats[chatID]?.messages?.toList()?.toList()
                    ?.sortedByDescending { (_, value) -> value.sendTime }
                val listState = rememberLazyListState()

                val coroutineScope = rememberCoroutineScope()

                var showBottomSheet by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState()

                var removeforall by remember { mutableStateOf(false) }

                var selectedMSGID by remember { mutableLongStateOf(0L) }
                var showPopup by remember { mutableStateOf(false) }

                val isUserScrolling by remember {
                    derivedStateOf {
                        listState.isScrollInProgress
                    }
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surfaceDim,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            title = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBackIos,
                                            contentDescription = "Вернуться в меню"
                                        )
                                    }

                                    if (chatUrl.isNotEmpty()) {
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
                                    DrawBottomDialog(chatID, listState, coroutineScope)
                                }
                            }
                        )
                    }
                ) {
                    LaunchedEffect(isUserScrolling) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                            .collect { visibleItems ->
                                val listSorted = chats[chatID]?.messages?.entries?.toList()
                                    ?.sortedByDescending { (_, value) -> value.sendTime }
                                println("size ${listSorted?.size}")
                                val totalItems = listState.layoutInfo.totalItemsCount
                                println("visItems ${visibleItems[0].index}")
                                println("visItems ${visibleItems.last().index}")

                                if (visibleItems.last().index >= listSorted!!.size - 5 && chats[chatID]?.messages?.size?.rem(
                                        30
                                    ) == 0 && isUserScrolling) {

                                    print("cool: ")
                                    println(listSorted)
                                    val packet = SocketManager.packPacket(
                                        OPCode.CHAT_MESSAGES.opcode,
                                        JsonObject(
                                            mapOf(
                                                "chatId" to JsonPrimitive(chatID),
                                                "from" to JsonPrimitive(
                                                    listSorted?.last()?.value?.sendTime
                                                ),
                                                "forward" to JsonPrimitive(0),
                                                "backward" to JsonPrimitive(31),
                                                "getMessages" to JsonPrimitive(true)
                                            )
                                        )
                                    )

                                    SocketManager.sendPacket(packet, { packet ->
                                        if (packet.payload is JsonObject) {
                                            ChatManager.processMessages(packet.payload["messages"]!!.jsonArray, chatID)
                                        }
                                    })
                                }
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
                                                mapOf(  "forMe" to JsonPrimitive(!removeforall),
                                                    "itemType" to JsonPrimitive("REGULAR"),
                                                    "chatId" to JsonPrimitive(chatID),
                                                    "messageIds" to JsonArray(
                                                        listOf(
                                                            Json.encodeToJsonElement(
                                                                Long.serializer(),
                                                                selectedMSGID
                                                            )
                                                        )
                                                    ),
                                                )
                                            )
                                        )
                                        GlobalScope.launch {
                                            SocketManager.sendPacket(packet,
                                                { packet ->
                                                    if (packet.payload is JsonObject) {
                                                        println(packet)
                                                        val packetID = packet.payload["chatId"]?.jsonPrimitive?.long

                                                        for (i in chats[packetID]?.messages!!) {
                                                            if (i.key.toLong() == selectedMSGID) {
                                                                println(i.key)
                                                                println(selectedMSGID)
                                                                ChatManager.removeMessage(
                                                                    chatID,
                                                                    selectedMSGID.toString()
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
                                if (chats[chatID]?.messages[selectedMSGID.toString()]?.senderID == AccountManager.accountID) {
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
                    }
                    var prevMsg : Message
                    LazyColumn(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        state = listState,
                        reverseLayout = true
                    ) {
                        itemsIndexed(
                            sortedChats ?: emptyList(), key = { index, message ->
                                message.first
                            }) { index, message ->
                            val horizontal: Alignment.Horizontal = if (message.second.senderID == AccountManager.accountID && !(message.second.attaches?.jsonArray?.isNotEmpty() == true && message.second.attaches?.jsonArray?.last()?.jsonObject?.contains(
                                    "event"
                                ) == true)) {
                                Alignment.End
                            } else if (message.second.attaches?.jsonArray?.isNotEmpty() == true && message.second.attaches?.jsonArray?.last()?.jsonObject?.contains(
                                    "event"
                                ) == true
                            )  {
                                Alignment.CenterHorizontally
                            } else {
                                Alignment.Start
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBottomSheet = true
                                        selectedMSGID = message.first.toLong()
                                    },
                                horizontalArrangement = Arrangement.spacedBy(
                                    16.dp,
                                    horizontal
                                ),
                            ) {
                                DrawMessage(message.second, type,
                                    if (index != sortedChats?.size?.minus(1)) sortedChats?.get(index + 1)?.second ?: Message() else Message(),
                                    if (index > 0) sortedChats?.get(index - 1)?.second ?: Message() else Message()
                                )
                            }
                        }
                    }
                    val isAtBottom by remember {
                        derivedStateOf {
                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                            val totalItems = listState.layoutInfo.totalItemsCount

                            visibleItems.isNotEmpty() &&
                                    visibleItems.first().index < 5
                        }
                    }

                    LaunchedEffect(chats) {
                        println("testttt")
                        val msgSize : Int = chats[chatID]!!.messages.size

                        if (isAtBottom) {
                            listState.scrollToItem(
                                index = 0,
                                scrollOffset = 0
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class)
@Composable
fun DrawMessage(message: Message, chatType : String, previousMessage : Message, nextMessage : Message) {
    val users by UserManager.usersList.collectAsState()

    var username = users[message.senderID]?.firstName

    if (users[message.senderID]?.lastName?.isNotEmpty() == true) {
        username += " " + users[message.senderID]?.lastName
    }
    if (!(message.attaches?.jsonArray?.isNotEmpty() == true && message.attaches.jsonArray.last().jsonObject.contains(
            "event"
        ))
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (chatType != "CHAT" || (message.senderID != AccountManager.accountID && nextMessage.senderID != message.senderID)) 0.dp else 49.dp,
            ), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start)
        ) {
            if (chatType == "CHAT" && message.senderID != AccountManager.accountID && nextMessage.senderID != message.senderID) {
                if (!users.containsKey(message.senderID)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            message.senderID
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }
                }

                if (users[message.senderID]?.avatarUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = users[message.senderID]?.avatarUrl,
                        contentDescription = "ChatIcon",
                        modifier = Modifier
                            .width(45.dp)
                            .height(45.dp)
                            .clip(CircleShape)
                            .align(Alignment.Bottom),
                        contentScale = ContentScale.Crop,

                        )
                } else {
                    val initial = username?.split(" ")?.mapNotNull { it.firstOrNull() }
                        ?.take(2)
                        ?.joinToString("")
                        ?.uppercase(getDefault())

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(45.dp)
                            .height(45.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Utils.getColorForAvatar(username.toString()).first,
                                        Utils.getColorForAvatar(username.toString()).second
                                    )
                                )
                            )
                            .align(Alignment.Bottom)
                    ) {
                        Text(
                            text = initial.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 25.sp
                        )
                    }
                }
            }
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp

            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 100.dp, maxWidth = screenWidth * 0.7f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color = if (message.senderID == AccountManager.accountID) colorScheme.primaryContainer else colorScheme.secondaryContainer)
                    .padding(start = 4.dp, end = 2.dp, top = 4.dp)
            ) {
                Column {
                    if (chatType == "CHAT" && message.senderID != AccountManager.accountID && previousMessage.senderID != message.senderID) {
                        Text(
                            username.toString(),    
                            fontSize = 17.sp,
                            modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                        )
                    }
                    if (message.link.type.isNotEmpty() && message.link.type == "FORWARD") {
                        if (!users.containsKey(message.link.msgForLink.senderID)) {
                            val packet = SocketManager.packPacket(
                                OPCode.CONTACTS_INFO.opcode,
                                JsonObject(
                                    mapOf(
                                        "contactIds" to JsonArray(
                                            listOf(
                                                Json.encodeToJsonElement(
                                                    Long.serializer(),
                                                    message.link.msgForLink.senderID
                                                )
                                            )
                                        ),
                                    )
                                )
                            )

                            GlobalScope.launch {
                                SocketManager.sendPacket(
                                    packet,
                                    { packet ->
                                        println(packet.payload)
                                        if (packet.payload is JsonObject) {
                                            UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                        }
                                    }
                                )
                            }
                        }
                        val fromUserForward =
                            users[message.link.msgForLink.senderID]?.firstName + " " + users[message.link.msgForLink.senderID]?.lastName

                        val annotatedString = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontSize = 15.sp)) {
                                append("Переслано от: ")
                            }

                            appendInlineContent(id = "avatar")

                            withStyle(style = SpanStyle(fontSize = 15.sp)) {
                                append(fromUserForward)
                            }
                        }
                        val inlineContentMap = mutableMapOf<String, InlineTextContent>(

                        )
                        val placeholder = Placeholder(
                            width = 30.sp,
                            height = 30.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )

                        inlineContentMap["avatar"] = InlineTextContent(placeholder) { _ ->
                            if (users[message.link.msgForLink.senderID]?.avatarUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = users[message.link.msgForLink.senderID]?.avatarUrl,
                                    contentDescription = "ChatIcon",
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(30.dp)
                                        .clip(CircleShape)
                                        .fillMaxSize(),
                                    alignment = Alignment.Center,
                                    contentScale = ContentScale.FillBounds
                                )
                            } else {
                                val initial =
                                    fromUserForward.split(" ").mapNotNull { it.firstOrNull() }
                                        .take(2)
                                        .joinToString("")
                                        .uppercase(getDefault())

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Utils.getColorForAvatar(fromUserForward).first,
                                                    Utils.getColorForAvatar(fromUserForward).second
                                                )
                                            )
                                        )
                                        .fillMaxSize()
                                        .padding(start = 2.dp, end = 2.dp)
                                ) {
                                    Text(
                                        text = initial,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Text(
                            annotatedString,
                            inlineContent = inlineContentMap,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 4.dp, end = 2.dp).heightIn(max = 50.dp)
                        )
                        if (message.link.msgForLink.attaches!!.jsonArray.isNotEmpty()) {
                            DrawImages(message.link.msgForLink.attaches.jsonArray)
                        }

                        Text(
                            message.link.msgForLink.message,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 16.dp)
                        )
                    } else {
                        if (message.link.type.isNotEmpty() && message.link.type == "REPLY") {
                            Box(modifier = Modifier
                                .background(color = if (message.senderID == AccountManager.accountID) colorScheme.onPrimary.copy(0.6f) else colorScheme.onSecondary.copy(0.6f)
                                    , shape = RoundedCornerShape(8.dp))
                                .padding(start = 2.dp)
                                .sizeIn(minWidth = 100.dp, maxWidth = screenWidth * 0.7f)
                            ) {
                                    var userName = users[message.link.msgForLink.senderID]?.firstName

                                    if (users[message.link.msgForLink.senderID]?.lastName?.isNotEmpty() == true) {
                                        userName += " " + users[message.link.msgForLink.senderID]?.lastName
                                    }

                                    if (!users.containsKey(message.link.msgForLink.senderID)) {
                                        val packet = SocketManager.packPacket(
                                            OPCode.CONTACTS_INFO.opcode,
                                            JsonObject(
                                                mapOf(
                                                    "contactIds" to JsonArray(
                                                        listOf(
                                                            Json.encodeToJsonElement(
                                                                Long.serializer(),
                                                                message.link.msgForLink.senderID
                                                            )
                                                        )
                                                    ),
                                                )
                                            )
                                        )

                                        GlobalScope.launch {
                                            SocketManager.sendPacket(
                                                packet,
                                                { packet ->
                                                    println(packet.payload)
                                                    if (packet.payload is JsonObject) {
                                                        UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    Column() {
                                        Text(
                                            userName.toString(),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                        if (message.link.msgForLink.attaches!!.jsonArray.isNotEmpty()) {
                                            DrawImages(message.link.msgForLink.attaches.jsonArray)
                                        }

                                        Text(message.link.msgForLink.message,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 2.dp))
                                    }
                            }
                        }
                        if (message.attaches!!.jsonArray.isNotEmpty()) {
                            DrawImages(message.attaches.jsonArray)
                        }

                        Text(
                            AnnotatedString.rememberAutoLinkText(
                                message.message
                            ),
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 10.sp,
                                maxFontSize = 16.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp, end = 6.dp, bottom = 16.dp)
                        )
                    }
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
    } else {
        val users by UserManager.usersList.collectAsState()
        val attach = message.attaches.jsonArray.last()
        val event = attach.jsonObject["event"]?.jsonPrimitive?.content
        var userName = ""

        Box(modifier = Modifier
            .background(colorScheme.secondaryContainer.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)),
            ) {
            var joinText = ""
            // shit code :roflan_ebalo:

            if (event == "joinByLink") {
                if (!users.containsKey(attach.jsonObject["userId"]?.jsonPrimitive?.long)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            attach.jsonObject["userId"]?.jsonPrimitive?.long!!
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }


                }
                if (message.senderID == AccountManager.accountID) {
                    joinText += "Вы присоединились к чату"
                } else {
                    userName = users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                    if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                        userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                    }
                    joinText += "$username присоединился(-ась) к чату"
                }
            } else if (event == "add") {
                val peoplesAdded = attach.jsonObject["userIds"]?.jsonArray

                if (!users.containsKey(peoplesAdded?.last()?.jsonPrimitive?.long) && attach.jsonObject["userIds"]?.jsonArray?.isNotEmpty() == true) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            attach.jsonObject["userIds"]?.jsonArray?.last()?.jsonPrimitive?.long!!
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }
                }

                if (!users.containsKey(message.senderID)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            message.senderID
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }
                }
                var whomAdded = users[peoplesAdded?.last()?.jsonPrimitive?.long]?.firstName.toString()

                if (users[peoplesAdded?.last()?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                    whomAdded += " " + users[peoplesAdded?.last()?.jsonPrimitive?.long]?.lastName
                }
                if (message.senderID == AccountManager.accountID) {
                    joinText += "Вы добавили $whomAdded"
                } else {
                    var whoAdded = users[message.senderID]?.firstName.toString()

                    if (users[message.senderID]?.firstName?.isNotEmpty() == true) {
                        whoAdded += " " + users[message.senderID]?.lastName
                    }

                    joinText += "$whoAdded добавил(-а) $whomAdded"
                }
            } else if (event == "leave") {
                if (!users.containsKey(message.senderID)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            message.senderID
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }
                }


                if (message.senderID == AccountManager.accountID) {
                    joinText += "Вы покинули чат"
                } else {
                    userName = users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                    if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                        userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                    }
                    joinText += "$username покинул(-а) чат"
                }
            } else if (event == "title") {
                var userName = ""
                if (!users.containsKey(message.senderID)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            message.senderID
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }
                }

                if (message.senderID == AccountManager.accountID) {
                    username = "Вы"
                    val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                    joinText += "$username изменили название чата на «$newTitle»"
                } else {
                    userName = users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                    if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                        userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                    }

                    val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                    joinText += "$username изменил(-а) название чата на «$newTitle»"
                }
            } else if (event == "icon") {
                var userName = ""
                if (!users.containsKey(message.senderID)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            message.senderID
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }
                }

                if (message.senderID == AccountManager.accountID) {
                    username = "Вы"
                    val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                    joinText += "$username изменили фото чата"
                } else {
                    userName = users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                    if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                        userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                    }

                    val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                    joinText += "$username изменил(-а) фото чата"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(joinText)
                    AsyncImage(
                        model = attach.jsonObject["url"]!!.jsonPrimitive.content,
                        contentDescription = "ChatIcon",
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(100.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (event == "new") {
                var userName = ""
                if (!users.containsKey(message.senderID)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode,
                        JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(),
                                            message.senderID
                                        )
                                    )
                                ),
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet,
                            { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                }
                            }
                        )
                    }
                }
                if (chatType != "CHANNEL") {
                    if (message.senderID == AccountManager.accountID) {
                        username = "Вы"

                        joinText += "$username создали чат"
                    } else {
                        userName = users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                        if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                            userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                        }

                        joinText += "$username создал(-а) чат"
                    }
                } else {
                    joinText += "Канал создан"
                }
            }
            if (event != "icon") {
                Text(joinText)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun DrawBottomDialog(chatID: Long, listState: LazyListState, coroutineScope: CoroutineScope) {
    var message by remember { mutableStateOf("") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Add, contentDescription = "add")
        }
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = message,
            onValueChange = { newText ->
                message = newText
            },
            placeholder = { Text("Сообщение")},
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