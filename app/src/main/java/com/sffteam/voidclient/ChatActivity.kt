package com.sffteam.voidclient

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.voidclient.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import sh.calvin.autolinktext.rememberAutoLinkText
import java.time.Duration
import java.util.Date
import java.util.Locale.getDefault
import kotlin.time.ExperimentalTime
import kotlin.time.Instant.Companion.fromEpochMilliseconds
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.collections.listOf
import kotlin.collections.set
import androidx.compose.foundation.lazy.items

class ChatActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnrememberedMutableState")
    @OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class, ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val chatTitle: String = intent.getStringExtra("chatTitle").toString()
        val chatUrl: String = intent.getStringExtra("chatIcon").toString()
        val chatID: Long = intent.getLongExtra("chatID", 0L)
        val messageTime: Long = intent.getLongExtra("messageTime", 0L)
        val type: String = intent.getStringExtra("chatType").toString()

        println(ChatManager.chatsList.value[chatID]?.messages?.size)

        // TODO : fix this sh1t code
        if (ChatManager.chatsList.value[chatID]?.messages?.size!! < 30 && ChatManager.chatsList.value[chatID]?.needGetMessages == true) {
            val packet = SocketManager.packPacket(
                OPCode.CHAT_MESSAGES.opcode, JsonObject(
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
                    packet, { packet ->
                        println(packet)
                        if (packet.payload is JsonObject) ChatManager.processMessages(
                            packet.payload["messages"]!!.jsonArray, chatID
                        )
                    })
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

                var selectedMSGEdit by remember { mutableLongStateOf(0L) }
                var selectedMSGReply by remember { mutableLongStateOf(0L) }
                var selectedMSGID by remember { mutableLongStateOf(0L) }
                var showPopup by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }
                val isUserScrolling by remember {
                    derivedStateOf {
                        listState.isScrollInProgress
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surfaceContainer,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White,
                            ),
                            title = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { finish() },
                                    ) {
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
                                                .clip(CircleShape),
                                            contentScale = ContentScale.FillBounds
                                        )
                                    } else if (chatID == 0L) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .width(50.dp)
                                                .height(50.dp)
                                                .clip(CircleShape)
                                                .background(colorScheme.primaryContainer)
                                        ) {
                                            Icon(
                                                Icons.Filled.Bookmark,
                                                contentDescription = "Bookmark",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    } else {
                                        val initial = chatTitle.split(" ")
                                            .mapNotNull { it.firstOrNull()?.toChar() }.take(2)
                                            .joinToString("").uppercase(getDefault())
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
                                                )
                                        ) {
                                            Text(
                                                text = initial,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontSize = 25.sp,
                                            )
                                        }
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = chatTitle,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        var userDesc: String

                                        when (type) {
                                            "CHAT" -> {
                                                val sizeString =
                                                    chats[chatID]?.users?.size.toString()
                                                val sizeStringLast = sizeString.last().code
                                                userDesc = when (sizeStringLast) {
                                                    1 -> {
                                                        "Тут только вы"
                                                    }

                                                    else -> {
                                                        if (sizeStringLast == 2 || sizeStringLast == 3 || sizeStringLast == 4) {
                                                            "$sizeString участника"
                                                        } else if (sizeStringLast == 1 && sizeString != "11") {
                                                            "$sizeString участник"
                                                        } else {
                                                            "$sizeString участников"
                                                        }
                                                    }
                                                }

                                            }

                                            "CHANNEL" -> {
                                                userDesc = if (chats[chatID]?.usersCount.toString()
                                                        .last().code == 2 || chats[chatID]?.usersCount.toString()
                                                        .last().code == 3 || chats[chatID]?.usersCount.toString()
                                                        .last().code == 4
                                                ) {
                                                    println(
                                                        "code ${
                                                            chats[chatID]?.usersCount.toString()
                                                                .last().code
                                                        }"
                                                    )
                                                    chats[chatID]?.usersCount?.toString() + " подписчика"
                                                } else if (chats[chatID]?.usersCount.toString()
                                                        .last().code == 1 && chats[chatID]?.usersCount.toString() != "11"
                                                ) {
                                                    chats[chatID]?.usersCount?.toString() + " подписчик"
                                                } else {
                                                    chats[chatID]?.usersCount?.toString() + " подписчиков"
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

                                        if (userDesc.isNotEmpty()) {
                                            Text(
                                                text = userDesc,
                                                fontSize = 16.sp,
                                                modifier = Modifier.alpha(0.85f)
                                            )
                                        }
                                    }
                                }
                            },

                            )
                    },
                    bottomBar = {
                        if (type == "CHANNEL") {
                            DrawBottomChannel(chatID)
                        } else {
                            DrawBottomDialog(
                                chatID,
                                selectedMSGReply,
                                onValChange = { newVal -> selectedMSGReply = newVal },
                                type,
                                onEditChange = { newVal -> selectedMSGEdit = newVal },
                                selectedMessageEdit = selectedMSGEdit
                            )
                        }
                    },
                ) {
                    LaunchedEffect(isUserScrolling) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
                            val listSorted = chats[chatID]?.messages?.entries?.toList()
                                ?.sortedByDescending { (_, value) -> value.sendTime }
                            println("size ${listSorted?.size}")
                            println("visItems ${visibleItems[0].index}")
                            println("visItems ${visibleItems.last().index}")

                            if (visibleItems.last().index >= listSorted!!.size - 5 && chats[chatID]?.needGetMessages == true && isUserScrolling) {
                                print("cool: ")
                                println(listSorted)
                                val packet = SocketManager.packPacket(
                                    OPCode.CHAT_MESSAGES.opcode, JsonObject(
                                        mapOf(
                                            "chatId" to JsonPrimitive(chatID),
                                            "from" to JsonPrimitive(
                                                listSorted.last().value?.sendTime
                                            ),
                                            "forward" to JsonPrimitive(0),
                                            "backward" to JsonPrimitive(30),
                                            "getMessages" to JsonPrimitive(true)
                                        )
                                    )
                                )

                                SocketManager.sendPacket(packet, { packet ->
                                    if (packet.payload is JsonObject) {
                                        ChatManager.processMessages(
                                            packet.payload["messages"]!!.jsonArray, chatID
                                        )
                                    }
                                })
                            }
                        }
                    }
                    if (showPopup) {
                        AlertDialog(title = {
                            Text(text = "Удалить сообщение")
                        }, text = {
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
                        }, onDismissRequest = {
                            showPopup = false
                            removeforall = false
                        }, confirmButton = {
                            TextButton(
                                onClick = {
                                    val packet = SocketManager.packPacket(
                                        OPCode.DELETE_MESSAGE.opcode, JsonObject(
                                            mapOf(
                                                "messageIds" to JsonArray(
                                                    listOf(
                                                        JsonPrimitive(selectedMSGID)
                                                    )
                                                ),
                                                "chatId" to JsonPrimitive(chatID),
                                                "forMe" to JsonPrimitive(!removeforall),
                                                "itemType" to JsonPrimitive("REGULAR"),
                                            )
                                        )
                                    )
                                    GlobalScope.launch {
                                        SocketManager.sendPacket(
                                            packet, { packet ->
                                                if (packet.payload is JsonObject) {
                                                    println(packet)
                                                    val packetID =
                                                        packet.payload["chatId"]?.jsonPrimitive?.long

                                                    for (i in chats[packetID]?.messages!!) {
                                                        if (i.key.toLong() == selectedMSGID) {
                                                            println(i.key)
                                                            println(selectedMSGID)
                                                            ChatManager.removeMessage(
                                                                chatID, selectedMSGID.toString()
                                                            )
                                                        }
                                                    }
                                                }
                                            })
                                    }

                                    showBottomSheet = false
                                    showPopup = false
                                    removeforall = false
                                }) {
                                Text("Удалить", fontSize = 20.sp)
                            }
                        }, dismissButton = {
                            TextButton(
                                onClick = {
                                    showPopup = false
                                    removeforall = false
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
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedMSGReply = selectedMSGID
                                            selectedMSGEdit = 0L
                                            showBottomSheet = false
                                        }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "reply on message",
                                        modifier = Modifier
                                            .padding(end = 10.dp)
                                            .size(20.dp)
                                            .align(Alignment.CenterVertically)
                                    )

                                    Text(
                                        text = "Ответить",
                                        fontSize = 25.sp,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }

                                if (chats[chatID]?.messages[selectedMSGID.toString()]?.senderID == AccountManager.accountID) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMSGEdit = selectedMSGID
                                                showBottomSheet = false
                                                selectedMSGReply = 0L
                                            }) {
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
                                            modifier = Modifier.align(Alignment.CenterVertically)
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
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                            val horizontal: Alignment.Horizontal =
                                if (message.second.senderID == AccountManager.accountID && !(message.second.attaches?.jsonArray?.isNotEmpty() == true && message.second.attaches?.jsonArray?.last()?.jsonObject?.contains(
                                        "event"
                                    ) == true)
                                ) {
                                    Alignment.End
                                } else if (message.second.attaches?.jsonArray?.isNotEmpty() == true && message.second.attaches?.jsonArray?.last()?.jsonObject?.contains(
                                        "event"
                                    ) == true
                                ) {
                                    Alignment.CenterHorizontally
                                } else {
                                    Alignment.Start
                                }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val prevMsg =
                                    if (index != sortedChats?.size?.minus(1)) sortedChats?.get(index + 1)?.second else Message()
                                val duration = Duration.ofSeconds(
                                    message.second.sendTime / 1000 - (prevMsg?.sendTime?.div(
                                        1000
                                    ) ?: 0)
                                )

                                val instantLast = fromEpochMilliseconds(message.second.sendTime)
                                val instantPrev = fromEpochMilliseconds(prevMsg?.sendTime!!)

                                val localDateTimeLastPrev =
                                    instantPrev.toLocalDateTime(TimeZone.currentSystemDefault())
                                val localDateTimeLast =
                                    instantLast.toLocalDateTime(TimeZone.currentSystemDefault())


                                println("${duration.toHours()} sh1t")
                                if (localDateTimeLastPrev.date != localDateTimeLast.date) {
                                    val currentTime = Date().time

                                    val durCool =
                                        Duration.ofSeconds(currentTime / 1000 - message.second.sendTime / 1000)
                                    val instantLast = fromEpochMilliseconds(message.second.sendTime)

                                    val text = if (durCool.toHours() < 24) {
                                        "Сегодня"
                                    } else if (durCool.toHours() in 24..<48) {
                                        "Вчера"
                                    } else {
                                        "${localDateTimeLast.day}.${localDateTimeLast.month.number}.${localDateTimeLast.year}"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .background(
                                                colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(14.dp)
                                            ),
                                    ) {
                                        Text(
                                            text, modifier = Modifier.padding(
                                                bottom = 3.dp, start = 6.dp, end = 6.dp
                                            )
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            indication = null, interactionSource = interactionSource
                                        ) {
                                            if (message.second.attaches is JsonArray && (message.second.attaches!!.jsonArray.isEmpty() || !message.second.attaches!!.jsonArray.last().jsonObject.containsKey(
                                                    "event"
                                                ))
                                            ) {
                                                showBottomSheet = true
                                                selectedMSGID = message.first.toLong()
                                            }
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(
                                        16.dp, horizontal
                                    ),
                                ) {
                                    DrawMessage(
                                        message.second,
                                        type,
                                        if (index != sortedChats?.size?.minus(1)) sortedChats?.get(
                                            index + 1
                                        )?.second ?: Message() else Message(),
                                        if (index > 0) sortedChats?.get(index - 1)?.second
                                            ?: Message() else Message()
                                    )
                                }
                            }
                        }
                    }
                    val isBottom by remember {
                        derivedStateOf {
                            val visibleItems = listState.layoutInfo.visibleItemsInfo

                            visibleItems.isNotEmpty() && visibleItems.first().index > 10
                        }
                    }
                    val density = LocalDensity.current
                    AnimatedVisibility(visible = isBottom, enter = slideInVertically {
                        with(density) { 50.dp.roundToPx() }
                    }, exit = slideOutVertically {
                        with(density) { +70.dp.roundToPx() }
                    }) {
                        println("butotn")
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .background(Color.Transparent)
                                .fillMaxSize()
                                .padding(end = 8.dp, bottom = 8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0, 0)
                                    }
                                },
                                modifier = Modifier
                                    .padding(it)
                                    .background(colorScheme.surfaceContainer, CircleShape)
                                    .size(50.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "scrollToBottom"
                                )
                            }
                        }
                    }
                    val isAtBottom by remember {
                        derivedStateOf {
                            val visibleItems = listState.layoutInfo.visibleItemsInfo

                            visibleItems.isNotEmpty() && visibleItems.first().index < 5
                        }
                    }

                    LaunchedEffect(chats) {
                        println("testttt")
                        if (isAtBottom) {
                            listState.scrollToItem(
                                index = 0, scrollOffset = 0
                            )
                        }
                    }
                    val reply = selectedMSGReply != 0L
                    val edit = selectedMSGEdit != 0L

                    // TODO: rewrite edit and reply message box
                    Box(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .fillMaxSize()
                            .padding(it),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        // TODO: fix null user on exit animation
                        AnimatedVisibility(visible = edit, enter = slideInVertically {
                            with(density) { 50.dp.roundToPx() }
                        }, exit = slideOutVertically {
                            with(density) { +70.dp.roundToPx() }
                        }) {
                            Box(
                                modifier = Modifier
                                    .background(colorScheme.surfaceContainer)
                                    .fillMaxWidth()
                                    .padding(start = 10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "edit icon",
                                        modifier = Modifier
                                            .size(25.dp)
                                            .padding(end = 8.dp)
                                    )

                                    val users by UserManager.usersList.collectAsState()
                                    val selectedMsg =
                                        chats[chatID]?.messages[selectedMSGEdit.toString()]

                                    Column() {
                                        val user = users[selectedMsg?.senderID]
                                        val configuration = LocalConfiguration.current
                                        val screenWidth = configuration.screenWidthDp.dp

                                        val userName = if (user?.lastName?.isNotEmpty() == true) {
                                            user.firstName + " " + user.lastName
                                        } else {
                                            user?.firstName
                                        }

                                        Text("Редактирование", color = colorScheme.primary)

                                        val annotatedString = buildAnnotatedString {
                                            if (selectedMsg?.attaches?.jsonArray?.isNotEmpty() == true) {
                                                appendInlineContent(id = "lastImg")
                                                append(" ")
                                            }

                                            if (selectedMsg?.message?.isNotEmpty() == true) {
                                                append(selectedMsg.message)
                                            } else {
                                                append("Фотография")
                                            }
                                        }

                                        val inlineContentMap =
                                            mutableMapOf<String, InlineTextContent>()

                                        val placeholder = Placeholder(
                                            width = 25.sp,
                                            height = 25.sp,
                                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                        )

                                        inlineContentMap["lastImg"] =
                                            InlineTextContent(placeholder) { _ ->
                                                AsyncImage(
                                                    model = selectedMsg?.attaches?.jsonArray!!.last().jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                    contentDescription = "ChatIcon",
                                                    modifier = Modifier
                                                        .size(25.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                        Text(
                                            annotatedString,
                                            inlineContent = inlineContentMap,
                                            modifier = Modifier.sizeIn(maxWidth = screenWidth * 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                    }

                                    Spacer(modifier = Modifier.weight(0.5f))

                                    IconButton(
                                        onClick = {
                                            selectedMSGEdit = 0L
                                        },
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "close")
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .fillMaxSize()
                            .padding(it),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        // TODO fix null user on exit animation
                        AnimatedVisibility(visible = reply, enter = slideInVertically {
                            with(density) { 50.dp.roundToPx() }
                        }, exit = slideOutVertically {
                            with(density) { +70.dp.roundToPx() }
                        }) {
                            Box(
                                modifier = Modifier
                                    .background(colorScheme.surfaceContainer)
                                    .fillMaxWidth()
                                    .padding(start = 10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "reply icon",
                                        modifier = Modifier
                                            .size(25.dp)
                                            .padding(end = 8.dp)
                                    )

                                    val users by UserManager.usersList.collectAsState()
                                    val selectedMsg =
                                        chats[chatID]?.messages[selectedMSGReply.toString()]

                                    Column() {
                                        val user = users[selectedMsg?.senderID]
                                        val configuration = LocalConfiguration.current
                                        val screenWidth = configuration.screenWidthDp.dp

                                        val userName = if (user?.lastName?.isNotEmpty() == true) {
                                            user.firstName + " " + user.lastName
                                        } else {
                                            user?.firstName
                                        }

                                        Text("В ответ $userName", color = colorScheme.primary)

                                        val annotatedString = buildAnnotatedString {
                                            if (selectedMsg?.attaches?.jsonArray?.isNotEmpty() == true) {
                                                appendInlineContent(id = "lastImg")
                                                append(" ")
                                            }

                                            if (selectedMsg?.message?.isNotEmpty() == true) {
                                                append(selectedMsg.message)
                                            } else {
                                                append("Фотография")
                                            }
                                        }

                                        val inlineContentMap =
                                            mutableMapOf<String, InlineTextContent>()

                                        val placeholder = Placeholder(
                                            width = 25.sp,
                                            height = 25.sp,
                                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                        )

                                        inlineContentMap["lastImg"] =
                                            InlineTextContent(placeholder) { _ ->
                                                AsyncImage(
                                                    model = selectedMsg?.attaches?.jsonArray!!.last().jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                    contentDescription = "ChatIcon",
                                                    modifier = Modifier
                                                        .size(25.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                        Text(
                                            annotatedString,
                                            inlineContent = inlineContentMap,
                                            modifier = Modifier.sizeIn(maxWidth = screenWidth * 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                    }

                                    Spacer(modifier = Modifier.weight(0.5f))

                                    IconButton(
                                        onClick = {
                                            selectedMSGReply = 0L
                                        },
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "close")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class)
@Composable
fun DrawMessage(
    message: Message, chatType: String, previousMessage: Message, nextMessage: Message
) {
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
                start = if (chatType != "CHAT" || (message.senderID != AccountManager.accountID && nextMessage.senderID != message.senderID)) 6.dp else 55.dp,
                end = 6.dp
            ), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start)
        ) {
            if (chatType == "CHAT" && message.senderID != AccountManager.accountID && nextMessage.senderID != message.senderID) {
                if (!users.containsKey(message.senderID)) {
                    val packet = SocketManager.packPacket(
                        OPCode.CONTACTS_INFO.opcode, JsonObject(
                            mapOf(
                                "contactIds" to JsonArray(
                                    listOf(
                                        Json.encodeToJsonElement(
                                            Long.serializer(), message.senderID
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
                                        UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                    }
                                }
                            })
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
                    val initial = username?.split(" ")?.mapNotNull { it.firstOrNull() }?.take(2)
                        ?.joinToString("")?.uppercase(getDefault())

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
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp)
            ) {
                Column {
                    if (chatType == "CHAT" && message.senderID != AccountManager.accountID && ((previousMessage.attaches?.jsonArray?.isNotEmpty() == true && previousMessage.attaches.jsonArray.last().jsonObject.containsKey(
                            "event"
                        )) || previousMessage.senderID != message.senderID)
                    ) {
                        Text(
                            username.toString(),
                            color = Utils.getColorForNickname(username.toString()),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                        )
                    } else {
                        println("prevmsg $previousMessage")
                    }
                    if (message.link.type.isNotEmpty() && message.link.type == "FORWARD") {
                        if (!users.containsKey(message.link.msgForLink.senderID)) {
                            val packet = SocketManager.packPacket(
                                OPCode.CONTACTS_INFO.opcode, JsonObject(
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
                                    packet, { packet ->
                                        println(packet.payload)
                                        if (packet.payload is JsonObject) {
                                            GlobalScope.launch {
                                                UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                            }
                                        }
                                    })
                            }
                        }
                        val fromUserForward =
                            users[message.link.msgForLink.senderID]?.firstName + " " + users[message.link.msgForLink.senderID]?.lastName

                        val annotatedString = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontSize = 15.sp)) {
                                append("Переслано от: ")
                            }

                            appendInlineContent(id = "avatar")
                            append(" ")
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
                                        .take(2).joinToString("").uppercase(getDefault())

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
                            modifier = Modifier
                                .padding(start = 2.dp, end = 2.dp)
                                .heightIn(max = 50.dp)
                        )
                        if (message.link.msgForLink.attaches is JsonArray) {
                            DrawImages(message.link.msgForLink.attaches.jsonArray)
                        }

                        Text(
                            message.link.msgForLink.message,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 16.dp)
                        )
                    } else {
                        if (message.link.type.isNotEmpty() && message.link.type == "REPLY") {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (message.senderID == AccountManager.accountID) colorScheme.onPrimary.copy(
                                            0.6f
                                        ) else colorScheme.onSecondary.copy(0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(start = 2.dp, end = 2.dp)
                                    .sizeIn(minWidth = 100.dp, maxWidth = screenWidth * 0.7f)
                            ) {
                                var userName = users[message.link.msgForLink.senderID]?.firstName

                                if (users[message.link.msgForLink.senderID]?.lastName?.isNotEmpty() == true) {
                                    userName += " " + users[message.link.msgForLink.senderID]?.lastName
                                }

                                if (!users.containsKey(message.link.msgForLink.senderID)) {
                                    val packet = SocketManager.packPacket(
                                        OPCode.CONTACTS_INFO.opcode, JsonObject(
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
                                            packet, { packet ->
                                                println(packet.payload)
                                                if (packet.payload is JsonObject) {
                                                    GlobalScope.launch {
                                                        UserManager.processUsers(packet.payload["contacts"]!!.jsonArray)
                                                    }
                                                }
                                            })
                                    }
                                }

                                Column() {
                                    Text(
                                        userName.toString(),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 2.dp),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        color = colorScheme.primary
                                    )

                                    val annotatedString = buildAnnotatedString {
                                        if (message.link.msgForLink.attaches?.jsonArray?.isNotEmpty() == true) {
                                            appendInlineContent(id = "lastImg")
                                            append(" ")
                                        }

                                        if (message.link.msgForLink.message.isNotEmpty()) {
                                            append(message.link.msgForLink.message)
                                        } else {
                                            append("Фотография")
                                        }
                                    }

                                    val inlineContentMap = mutableMapOf<String, InlineTextContent>()

                                    val placeholder = Placeholder(
                                        width = 25.sp,
                                        height = 25.sp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                    )

                                    inlineContentMap["lastImg"] =
                                        InlineTextContent(placeholder) { _ ->
                                            AsyncImage(
                                                model = message.link.msgForLink.attaches?.jsonArray!!.last().jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                contentDescription = "ChatIcon",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                    Text(
                                        text = annotatedString,
                                        inlineContent = inlineContentMap,
                                        fontSize = 12.sp,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        modifier = Modifier.padding(start = 1.dp, bottom = 4.dp)
                                    )
                                }
                            }
                        }
                        if (message.attaches!!.jsonArray.isNotEmpty()) {
                            DrawImages(message.attaches.jsonArray)
                        }

                        Text(
                            AnnotatedString.rememberAutoLinkText(
                                message.message
                            ), autoSize = TextAutoSize.StepBased(
                                minFontSize = 10.sp, maxFontSize = 16.sp
                            ), modifier = Modifier.padding(start = 2.dp, end = 6.dp, bottom = 16.dp)
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
                        .align(Alignment.BottomEnd),
                ) {
                    if (message.status == "EDITED") {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "add",
                            modifier = Modifier
                                .size(15.dp)
                                .align(
                                    Alignment.CenterVertically
                                )
                                .alpha(0.8f)
                                .padding(end = 2.dp)
                        )
                    }

                    Text(
                        time, modifier = Modifier
                            .align(
                                Alignment.CenterVertically
                            )
                            .alpha(0.8f), fontSize = 14.sp
                    )
                }
            }
        }
    } else {
        val users by UserManager.usersList.collectAsState()
        val attach = message.attaches.jsonArray.last()
        val event = attach.jsonObject["event"]?.jsonPrimitive?.content
        var userName = ""

        Box(
            modifier = Modifier
                .background(
                    colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(bottom = 3.dp, start = 6.dp, end = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            var joinText = ""
            // shit code :roflan_ebalo:

            if (event == "joinByLink") {
                UserManager.checkForExisting(attach.jsonObject["userId"]?.jsonPrimitive?.long!!)

                if (message.senderID == AccountManager.accountID) {
                    joinText += "Вы присоединились к чату"
                } else {
                    userName =
                        users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                    if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                        userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                    }
                    joinText += "$username присоединился(-ась) к чату"
                }
            } else if (event == "add") {
                val peoplesAdded = attach.jsonObject["userIds"]?.jsonArray

                for (i in peoplesAdded!!) {
                    if (attach.jsonObject["userIds"]?.jsonArray?.isNotEmpty() == true) {
                        UserManager.checkForExisting(i.jsonPrimitive.long)
                    }
                }

                UserManager.checkForExisting(message.senderID)

                if (message.senderID == AccountManager.accountID) {
                    joinText += "Вы добавили "
                } else {
                    var whoAdded = users[message.senderID]?.firstName.toString()

                    if (users[message.senderID]?.firstName?.isNotEmpty() == true) {
                        whoAdded += " " + users[message.senderID]?.lastName
                    }

                    joinText += "$whoAdded добавил(-а) "
                }

                for (i in peoplesAdded) {
                    var whomAdded = users[i.jsonPrimitive.long]?.firstName.toString()

                    if (users[i.jsonPrimitive.long]?.lastName?.isNotEmpty() == true) {
                        whomAdded += " " + users[i.jsonPrimitive.long]?.lastName
                    }

                    joinText += whomAdded
                    if (i != peoplesAdded.last()) {
                        joinText += ", "
                    }
                }
            } else if (event == "leave") {
                UserManager.checkForExisting(message.senderID)

                if (message.senderID == AccountManager.accountID) {
                    joinText += "Вы покинули чат"
                } else {
                    userName =
                        users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                    if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                        userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                    }
                    joinText += "$username покинул(-а) чат"
                }
            } else if (event == "title") {
                var userName = ""
                UserManager.checkForExisting(message.senderID)

                if (message.senderID == AccountManager.accountID) {
                    username = "Вы"
                    val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                    joinText += "$username изменили название чата на «$newTitle»"
                } else {
                    userName =
                        users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                    if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                        userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                    }

                    val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                    joinText += "$username изменил(-а) название чата на «$newTitle»"
                }
            } else if (event == "icon") {
                var userName = ""

                UserManager.checkForExisting(message.senderID)

                if (chatType == "CHANNEL") {
                    joinText += "Изменено фото канала"
                } else {
                    if (message.senderID == AccountManager.accountID) {
                        username = "Вы"
                        val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                        joinText += "$username изменили фото чата"
                    } else {
                        userName =
                            users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                        if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                            userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                        }

                        val newTitle = attach.jsonObject["title"]?.jsonPrimitive?.content

                        joinText += "$username изменил(-а) фото чата"
                    }
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

                UserManager.checkForExisting(message.senderID)

                if (chatType != "CHANNEL") {
                    if (message.senderID == AccountManager.accountID) {
                        username = "Вы"

                        joinText += "$username создали чат"
                    } else {
                        userName =
                            users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.firstName.toString()

                        if (users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName?.isNotEmpty() == true) {
                            userName += " " + users[attach.jsonObject["userId"]?.jsonPrimitive?.long]?.lastName
                        }

                        joinText += "$username создал(-а) чат"
                    }
                } else {
                    joinText += "Канал создан"
                }
            } else if (event == "remove") {
                val peoplesRemoved = attach.jsonObject["userId"]?.jsonPrimitive?.long

                UserManager.checkForExisting(peoplesRemoved!!)
                UserManager.checkForExisting(message.senderID)

                var whomAdded = users[peoplesRemoved]?.firstName.toString()

                if (users[peoplesRemoved]?.lastName?.isNotEmpty() == true) {
                    whomAdded += " " + users[peoplesRemoved]?.lastName
                }
                if (message.senderID == AccountManager.accountID) {
                    joinText += "Вы удалили $whomAdded"
                } else {
                    var whoAdded = users[message.senderID]?.firstName.toString()

                    if (users[message.senderID]?.firstName?.isNotEmpty() == true) {
                        whoAdded += " " + users[message.senderID]?.lastName
                    }

                    joinText += "$whoAdded удалил(-а) $whomAdded"
                }
            } else if (event == "system") {
                joinText += attach.jsonObject["message"]?.jsonPrimitive?.content
            }

            if (event != "icon") {
                Text(
                    joinText, modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun DrawBottomDialog(
    chatID: Long,
    selectedMessage: Long,
    onValChange: (Long) -> Unit,
    chatType: String,
    selectedMessageEdit: Long,
    onEditChange: (Long) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var selectedImages by remember {
        mutableStateOf<List<Uri?>>(emptyList())
    }

    var uploadedImages = remember {
        mutableListOf<Pair<Uri, JsonObject>>()
    }

    val context = LocalContext.current
    Column() {
        // Can message be with id == 0? :thinking:
        val chats by ChatManager.chatsList.collectAsState()
        if (selectedMessageEdit != 0L) {
            val selectedEdit = chats[chatID]?.messages[selectedMessageEdit.toString()]

            message = selectedEdit?.message!!
        } else {
            message = ""
        }

        LazyRow() {
            items(selectedImages) { image ->
                Box() {
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        modifier = Modifier.size(75.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        BottomAppBar {
            val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
                onResult = { uris ->
                    if (uris.isNotEmpty()) {
                        for (i in uris) {
                            selectedImages = uris

                            var imageType = ""
                            var imageName = ""

                            val cursor = context.contentResolver.query(i, null, null, null, null)
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                                    imageName = it.getString(nameIndex)
                                    imageType = context.contentResolver.getType(i).toString()
                                }
                            }

                            val packet = SocketManager.packPacket(
                                OPCode.UPLOAD_IMAGE.opcode, JsonObject(
                                    mapOf(
                                        "count" to JsonPrimitive(1)
                                    )
                                )
                            )
                            val client = HttpClient(CIO)

                            runBlocking {
                                val imageBytes = try {
                                    context.contentResolver.openInputStream(uris.last())
                                        ?.use { inputStream ->
                                            inputStream.readBytes()
                                        }
                                } catch (e: Exception) {
                                    null
                                }

                                SocketManager.sendPacket(packet, { packet ->
                                    if (packet.payload is JsonObject) {
                                        runBlocking {
                                            try {
                                                val response: HttpResponse =
                                                    client.post(packet.payload["url"]?.jsonPrimitive?.content.toString()) {
                                                        method = HttpMethod.Post

                                                        headers {
                                                            append(
                                                                HttpHeaders.UserAgent,
                                                                "OKMessages/25.12.1 (Android 14; oneplus CPH2465; 382dpi 2300x1023)"
                                                            )
                                                            append(
                                                                HttpHeaders.ContentType,
                                                                "application/octet-stream"
                                                            )
                                                            append(
                                                                HttpHeaders.ContentDisposition,
                                                                "attachment; filename=${imageName}"
                                                            )
                                                            append(
                                                                "X-Uploading-Mode", "parallel"
                                                            )
                                                            append(
                                                                "Content-Range",
                                                                "bytes 0-${imageBytes!!.size - 1}/${imageBytes.size}"
                                                            )
                                                            append(
                                                                HttpHeaders.Connection, "keep-alive"
                                                            )
                                                            append(
                                                                HttpHeaders.AcceptEncoding, "gzip"
                                                            )
                                                        }

                                                        setBody(imageBytes)
                                                    }

                                                println(response.request.content)
                                                println("Upload response status: ${response.status}")
                                                val content =
                                                    Json.parseToJsonElement(response.bodyAsText())

                                                uploadedImages += Pair(i, content.jsonObject["photos"]!!.jsonObject)

                                                print(content)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            } finally {
                                                client.close()
                                            }
                                        }
                                    }
                                })
                            }
                        }
                    }
                })

            IconButton(onClick = {
                multiplePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = "add")
            }


            OutlinedTextField(
                value = message,
                onValueChange = { newText ->
                    message = newText
                },
                placeholder = { Text("Сообщение") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                if (selectedMessageEdit == 0L) {
                    if (message.isNotEmpty() || (uploadedImages.isNotEmpty() && uploadedImages.size == selectedImages.size)) {
                        println(uploadedImages)
                        println(selectedImages)
                        var messageObject = mutableMapOf(
                            "isLive" to JsonPrimitive(false),
                            "detectShare" to JsonPrimitive(true),
                            "elements" to JsonArray(emptyList()),
                            "cid" to JsonPrimitive(System.currentTimeMillis()),
                        )

                        var secondUser = 1L

                        if (message.isNotEmpty()) {
                            messageObject["text"] = JsonPrimitive(message)
                        }

                        if (uploadedImages.isNotEmpty()) {
                            var coolJson = mutableListOf<JsonObject>()
                            for (i in uploadedImages) {
                                println("elm $i")
                                println("secon ${i.second.toList().last().second.jsonObject["token"]}")
                                coolJson += JsonObject(mapOf(
                                    "photoToken" to JsonPrimitive(i.second.toList().last().second.jsonObject["token"]!!.jsonPrimitive.content),
                                    "_type" to JsonPrimitive("PHOTO")
                                )
                                )
                                println("cool jsn2 $coolJson")
                            }

                            println("cool jsn $coolJson")
                            messageObject["attaches"] = JsonArray(coolJson)
                        }

                        if (chatType == "DIALOG") {
                            for (i in chats[chatID]?.users?.toList()!!) {
                                if (i.first != AccountManager.accountID) {
                                    secondUser = i.first
                                    break
                                }
                            }
                        }

                        if (selectedMessage != 0L) {
                            messageObject["link"] = JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("REPLY"),
                                    "chatId" to JsonPrimitive(chatID),
                                    "messageId" to JsonPrimitive(selectedMessage),
                                )
                            )
                        }

                        val packetJson = JsonObject(
                            mapOf(
                                if (chatType == "CHAT") "chatId" to JsonPrimitive(chatID) else if (chatID == 0L) "userId" to JsonPrimitive(
                                    AccountManager.accountID
                                ) else "userId" to JsonPrimitive(secondUser),
                                "message" to JsonObject(
                                    messageObject
                                )
                            )
                        )

                        val packet =
                            SocketManager.packPacket(OPCode.SEND_MESSAGE.opcode, packetJson)
                        GlobalScope.launch {
                            SocketManager.sendPacket(
                                packet, { packet ->
                                    println(packet.payload)
                                    println("msg should be added")
                                    if (packet.payload is JsonObject) {
                                        println("msg should be added")
                                        var msgID = ""
                                        var msg = Message("", 0L, 0L, JsonArray(emptyList()), "")

                                        try {
                                            var status = ""
                                            if (packet.payload.jsonObject["message"]?.jsonObject?.containsKey(
                                                    "status"
                                                ) == true
                                            ) {
                                                try {
                                                    status =
                                                        packet.payload.jsonObject["message"]?.jsonObject["status"]!!.jsonPrimitive.content
                                                } catch (e: Exception) {
                                                    println(e)
                                                }
                                            }
                                            var textForwarded: String = ""
                                            var senderForwarded: Long = 0L
                                            var msgForwardedID: String = ""
                                            var forwardedAttaches: JsonElement? = JsonNull
                                            var forwardedType: String = ""

                                            if (packet.payload.jsonObject["message"]!!.jsonObject.contains(
                                                    "link"
                                                )
                                            ) {
                                                val messageLinked =
                                                    packet.payload.jsonObject["message"]?.jsonObject["link"]?.jsonObject["message"]

                                                textForwarded =
                                                    messageLinked?.jsonObject["text"]?.jsonPrimitive?.content.toString()
                                                senderForwarded =
                                                    messageLinked?.jsonObject["sender"]?.jsonPrimitive!!.long
                                                msgForwardedID =
                                                    messageLinked.jsonObject["id"]?.jsonPrimitive!!.long.toString()
                                                forwardedType =
                                                    packet.payload.jsonObject["message"]?.jsonObject["link"]?.jsonObject["type"]?.jsonPrimitive?.content.toString()
                                            }

                                            msg = Message(
                                                packet.payload["message"]?.jsonObject["text"]!!.jsonPrimitive.content,
                                                packet.payload["message"]?.jsonObject["time"]!!.jsonPrimitive.long,
                                                packet.payload["message"]?.jsonObject["sender"]!!.jsonPrimitive.long,
                                                packet.payload["message"]?.jsonObject["attaches"]!!.jsonArray,
                                                status,
                                                MessageLink(
                                                    type = forwardedType, msgForLink = msgForLink(
                                                        textForwarded,
                                                        senderID = senderForwarded,
                                                        msgID = msgForwardedID
                                                    )
                                                )
                                            )

                                            msgID =
                                                packet.payload["message"]?.jsonObject["id"]!!.jsonPrimitive.content
                                        } catch (e: Exception) {
                                            println(e)
                                        }

                                        println(msg)
                                        ChatManager.addMessage(msgID, msg, chatID)

                                    }
                                })
                            onValChange(0L)
                            uploadedImages = mutableListOf<Pair<Uri, JsonObject>>()
                            selectedImages = mutableListOf<Uri>()
                        }
                    }
                } else {
                    val selectedEdit = chats[chatID]?.messages[selectedMessageEdit.toString()]

                    val packet = SocketManager.packPacket(
                        OPCode.EDIT_MESSAGE.opcode, JsonObject(
                            mapOf(
                                "chatId" to JsonPrimitive(chatID),
                                "messageId" to JsonPrimitive(selectedMessageEdit),
                                "text" to JsonPrimitive(message),
                                "elements" to JsonArray(emptyList()),
                                "attachments" to selectedEdit?.attaches!!
                            )
                        )
                    )

                    GlobalScope.launch {
                        SocketManager.sendPacket(
                            packet, { packet ->
                                println(packet.payload)
                                if (packet.payload is JsonObject) {
                                    var msgID = ""
                                    var msg = Message("", 0L, 0L, JsonArray(emptyList()), "")

                                    try {
                                        var status = ""

                                        if (packet.payload.jsonObject["message"]?.jsonObject?.containsKey(
                                                "status"
                                            ) == true
                                        ) {
                                            try {
                                                status =
                                                    packet.payload.jsonObject["message"]?.jsonObject["status"]!!.jsonPrimitive.content
                                            } catch (e: Exception) {
                                                println(e)
                                            }
                                        }

                                        var textForwarded: String = ""
                                        var senderForwarded: Long = 0L
                                        var msgForwardedID: String = ""
                                        var forwardedAttaches: JsonElement? = JsonNull
                                        var forwardedType: String = ""

                                        if (packet.payload.jsonObject["message"]!!.jsonObject.contains(
                                                "link"
                                            )
                                        ) {
                                            val messageLinked =
                                                packet.payload.jsonObject["message"]?.jsonObject["link"]?.jsonObject["message"]

                                            textForwarded =
                                                messageLinked?.jsonObject["text"]?.jsonPrimitive?.content.toString()
                                            senderForwarded =
                                                messageLinked?.jsonObject["sender"]?.jsonPrimitive!!.long
                                            msgForwardedID =
                                                messageLinked.jsonObject["id"]?.jsonPrimitive!!.long.toString()
                                            forwardedType =
                                                packet.payload.jsonObject["message"]?.jsonObject["link"]?.jsonObject["type"]?.jsonPrimitive?.content.toString()
                                        }

                                        msg = Message(
                                            packet.payload["message"]?.jsonObject["text"]!!.jsonPrimitive.content,
                                            packet.payload["message"]?.jsonObject["time"]!!.jsonPrimitive.long,
                                            packet.payload["message"]?.jsonObject["sender"]!!.jsonPrimitive.long,
                                            packet.payload["message"]?.jsonObject["attaches"]!!.jsonArray,
                                            status,
                                            MessageLink(
                                                type = forwardedType, msgForLink = msgForLink(
                                                    textForwarded,
                                                    senderID = senderForwarded,
                                                    msgID = msgForwardedID
                                                )
                                            )
                                        )

                                        msgID =
                                            packet.payload["message"]?.jsonObject["id"]!!.jsonPrimitive.content
                                    } catch (e: Exception) {
                                        println(e)
                                    }

                                    println(msg)
                                    ChatManager.addMessage(msgID, msg, chatID)
                                }
                            })
                        onValChange(0L)
                        onEditChange(0L)
                    }
                }

                message = ""
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send")
            }
        }
    }
}

@Composable
fun DrawBottomChannel(chatID: Long) {
    BottomAppBar() {
        TextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Отключить уведомления", fontSize = 25.sp)
        }
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