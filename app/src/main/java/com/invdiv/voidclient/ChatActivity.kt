package com.invdiv.voidclient

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import java.io.File
import java.net.URLEncoder
import java.util.Date
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.DoneAll
import coil3.compose.rememberAsyncImagePainter
import kotlin.collections.iterator
import kotlin.time.Duration.Companion.seconds


class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val chatID = intent.getLongExtra("chatID", 0L)

            val chats by ChatsManager.chatsList.collectAsState()

            val chat = chats[chatID]

            val messageTime: Long = if (chat?.messages?.isNotEmpty() == true) {
                chat.messages.toList().last().second.sendTime
            } else {
                0L
            }

            LaunchedEffect(Unit) {
                if (chat?.needGetMessages == true && chat.messages?.size!! < 30) {
                    val payload = JsonObject(
                        mapOf(
                            "chatId" to JsonPrimitive(chatID),
                            "from" to JsonPrimitive(messageTime),
                            "forward" to JsonPrimitive(0),
                            "backward" to JsonPrimitive(30),
                            "getMessages" to JsonPrimitive(true)
                        )
                    )

                    SocketManager.sendPacket(
                        OPCode.CHAT_MESSAGES, payload, { packet ->
                            if (packet.payload is JsonObject) {
                                GlobalScope.launch {
                                    ChatsManager.processMessages(
                                        packet.payload["messages"]!!.jsonArray, chatID
                                    )
                                }
                            }
                        }
                    )
                }

            }

            VoidclientTheme() {
                UiChat(chatID)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiChat(chatID: Long) {
    val chats by ChatsManager.chatsList.collectAsState()
    val users by UsersManager.usersList.collectAsState()

    var chatIcon = ""
    var chatTitle = ""
    val chat = chats[chatID]
    val context = LocalContext.current
    var secondUser = 0L

    if (chat?.type == "DIALOG") {

        for (i in chat.users.toList()) {
            if (i.first != AccountManager.accountID) {
                secondUser = i.first
                break
            }
        }

        val user = users[secondUser]

        if (!user?.avatarUrl.isNullOrEmpty()) {
            chatIcon = user?.avatarUrl!!
        }

        chatTitle = if (chatID == 0L) {
            "Избранное"
        } else {
            Utils.getFullName(user!!)
        }
    } else {
        chatTitle = chat?.title ?: ""

        if (!chat?.avatarUrl.isNullOrEmpty()) {
            chatIcon = chat.avatarUrl
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(context, ChatInfoActivity::class.java)

                            if (chat?.type == "DIALOG" || chatID == 0L) {
                                var secondUser = 0L
                                if (chatID == 0L) {
                                    secondUser = AccountManager.accountID
                                } else {
                                    for (i in chat!!.users.toList()) {
                                        if (i.first != AccountManager.accountID) {
                                            secondUser = i.first
                                            break
                                        }
                                    }
                                }

                                intent.putExtra("userID", secondUser)
                            } else {
                                intent.putExtra("chatID", chatID)
                            }

                            context.startActivity(intent)
                        }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { (context as Activity).finish() },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Вернуться в меню"
                            )
                        }
                        if (chatID == 0L) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primaryContainer),
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = "FavoriteIcon",
                                    modifier = Modifier
                                        .size(25.dp)
                                        .align(Alignment.Center),
                                    tint = colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            Utils.AvatarFromName(chatTitle, chatIcon, 50)
                        }

                        // TODO: Good space receiving for big titles
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(chatTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)

                            var usersInfo = when (chat?.type) {
                                "DIALOG" -> if (users[secondUser]?.typing != 0L && users[secondUser]?.typing == chatID) "Печатает..." else Utils.getStatusString(users[secondUser]?.lastSeen ?: Pair(0L, 0))
                                // TODO: Declensions
                                "CHANNEL" -> "${chat.usersCount} ${Utils.formatMembersCount(chat.usersCount ?: 1, 1)}"
                                "CHAT" -> "${chat.usersCount} ${Utils.formatMembersCount(chat.usersCount ?: 1, 0)}"

                                else -> "undefined"
                            }
                            Text(usersInfo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = {
                            // TODO: ChatInfo
                        }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "ChatInfo"
                            )
                        }
                    }
                },
            )
        }
    ) {
        DrawColumn(chatID, Modifier.padding(top = it.calculateTopPadding()),
            chat?.type ?: "", bottom = it.calculateBottomPadding(), users)
    }
}

fun removeMessage(removeMessages : List<Long>, chatId : Long, removeForAll : Boolean) {
    val jsonRemove = mutableListOf<JsonPrimitive>()

    for (message in removeMessages) {
        jsonRemove += JsonPrimitive(message)
    }

    val payload = JsonObject(mapOf(
        "messageIds" to JsonArray(jsonRemove),
        "chatId" to JsonPrimitive(chatId),
        "forMe" to JsonPrimitive(!removeForAll),
        "itemType" to JsonPrimitive("REGULAR")
    ))

    GlobalScope.launch {
        SocketManager.sendPacket(OPCode.DELETE_MESSAGE, payload, { packet->
            if (packet.payload is JsonObject) {
                GlobalScope.launch {
                    ChatsManager.removeMessage(
                        packet.payload.jsonObject["chatId"]!!.jsonPrimitive.long, packet.payload.jsonObject["messageIds"]!!.jsonArray
                    )
                }
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawBottomSheet(sheetState : SheetState, onChange: (Boolean) -> Unit,
                    currentMessage : Pair<Long, Message>, setMessageEdit : (Long) -> Unit, setMessageReply : (Long) -> Unit, chatId : Long) {
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val reallyRemove = remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (reallyRemove.value) {
        val removeForAll = remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { reallyRemove.value = false},
            title = { Text(text = "Удалить это сообщение") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Вы действительно хотите удалить выбранное сообщение?")

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {removeForAll.value = !removeForAll.value}) {
                        Checkbox(
                            checked = removeForAll.value,
                            onCheckedChange = { removeForAll.value = it },
                            modifier = Modifier
                                .size(24.dp)
                                .clipToBounds()
                        )

                        Text("Удалить для всех?", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                   },
            confirmButton = {
                Button({
                    reallyRemove.value = false
                    onChange(false)
                    removeMessage(listOf(currentMessage.first), chatId, removeForAll.value)
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                Button({ reallyRemove.value = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            onChange(false)
        },
        sheetState = sheetState
    ) {
        TextButton(onClick = {
            setMessageEdit(0L)
            setMessageReply(currentMessage.first)
            onChange(false)
        },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = "scrollToBottom",
                tint = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.padding(3.dp))

            Text("Ответить", color = colorScheme.onBackground)
        }

        // TODO: photo copy
        if (!currentMessage.second.message.isNullOrEmpty()) {
            TextButton(onClick = {
                coroutineScope.launch {
                    clipboardManager.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(
                                currentMessage.second.message,
                                currentMessage.second.message
                            )
                        )
                    )

                    onChange(false)
                }
            },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "scrollToBottom",
                    tint = colorScheme.onBackground
                )

                Spacer(modifier = Modifier.padding(3.dp))

                Text("Копировать", color = colorScheme.onBackground)
            }
        }

        TextButton(onClick = {
            val intent = Intent(context, ChatsListActivity::class.java)

            intent.putExtra("forwardChatID", chatId)
            intent.putExtra("forwardMessageID", currentMessage.first)

            context.startActivity(intent)
        },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Forward,
                contentDescription = "scrollToBottom",
                tint = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.padding(3.dp))

            Text("Переслать", color = colorScheme.onBackground)
        }

        val currentMessageReply = currentMessage.second.link?.type == "FORWARD"
        if (currentMessage.second.senderID == AccountManager.accountID && !currentMessageReply) {
//            TextButton(onClick = {
//            },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Icon(
//                    Icons.Default.PushPin,
//                    contentDescription = "scrollToBottom",
//                    tint = colorScheme.onBackground
//                )
//
//                Spacer(modifier = Modifier.padding(3.dp))
//
//                Text("Закрепить", color = colorScheme.onBackground)
//            }

            TextButton(
                onClick = {
                    setMessageEdit(currentMessage.first)
                    setMessageReply(0L)
                    onChange(false)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "scrollToBottom",
                    tint = colorScheme.onBackground
                )

                Spacer(modifier = Modifier.padding(3.dp))

                Text("Изменить", color = colorScheme.onBackground)
            }
        }

        if (currentMessage.second.senderID == AccountManager.accountID) {
            TextButton(onClick = {
                reallyRemove.value = true
            },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "scrollToBottom",
                    tint = colorScheme.onBackground
                )

                Spacer(modifier = Modifier.padding(3.dp))

                Text("Удалить", color = colorScheme.onBackground)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawColumn(chatID: Long, modifier: Modifier, chatType: String, bottom : Dp, users : Map<Long, User>) {
    val listState = rememberLazyListState()
    val chats by ChatsManager.chatsList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentMessage by remember { mutableStateOf(Pair(0L, Message())) }
    var currentEditMessage by remember { mutableLongStateOf(0L) }
    var currentReplyMessage by remember { mutableLongStateOf(0L) }

    val messages = chats[chatID]?.messages

    if (showBottomSheet) {
        DrawBottomSheet(sheetState, { showBottomSheet = it}, currentMessage, { currentEditMessage = it }, { currentReplyMessage = it }, chatID)
    }

    val sortedMessages = remember(messages) {
        messages
            ?.toList()
            ?.sortedByDescending { (_, value) -> value.sendTime }
            ?: emptyList()
    }

    println("lastmessage: ${sortedMessages.last()}")
    val isAtBottom by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo

            visibleItems.isNotEmpty() && visibleItems.first().index < 5
        }
    }

    val isUserScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress
        }
    }

    val isNotBottom by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo

            visibleItems.isNotEmpty() && visibleItems.first().index > 5
        }
    }

    val hazeState = rememberHazeState()

    val prevSize = remember { mutableIntStateOf(0) }

    LaunchedEffect(chats) {
        if (isAtBottom && messages!!.size != prevSize.intValue) {
            listState.scrollToItem( 
                index = 0, scrollOffset = 0
            )
        }

        prevSize.intValue = messages?.size ?: 0
    }

    LaunchedEffect(isUserScrolling) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
            if (chats[chatID]?.messages?.entries?.isNotEmpty() == true && visibleItems.isNotEmpty()) {
                if (visibleItems.last().index >= sortedMessages.size - 5 && chats[chatID]?.needGetMessages == true && isUserScrolling && !chats[chatID]!!.gotMessages.contains(sortedMessages.last().second.sendTime)) {
                    val payload = JsonObject(
                            mapOf(
                                "chatId" to JsonPrimitive(chatID),
                                "from" to JsonPrimitive(
                                    sortedMessages.last().second.sendTime
                                ),
                                "forward" to JsonPrimitive(0),
                                "backward" to JsonPrimitive(30),
                                "getMessages" to JsonPrimitive(true)
                            )
                    )
                    chats[chatID]!!.gotMessages += sortedMessages.last().second.sendTime

                    try {
                        SocketManager.sendPacket(OPCode.CHAT_MESSAGES,payload, { packet ->
                            if (packet.payload is JsonObject) {
                                chats[chatID]!!.gotMessages -= sortedMessages.last().second.sendTime

                                GlobalScope.launch {
                                    if (packet.payload.contains("messages")) {
                                        ChatsManager.processMessages(
                                            packet.payload["messages"]!!.jsonArray, chatID
                                        )
                                    }
                                }
                            }
                        })
                    } catch (e: Exception) {
                        Log.e("ChatActivity", "Error while trying to get messages: $e")
                        chats[chatID]!!.gotMessages -= sortedMessages.last().second.sendTime
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier
            .padding(top = 6.dp, bottom = 6.dp)
            .fillMaxWidth()
            .hazeSource(state = hazeState),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(
                bottom = if (chatType == "DIALOG") {
                    var secondUser = 0L

                    for (i in chats[chatID]!!.users.toList()!!) {
                        if (i.first != AccountManager.accountID) {
                            secondUser = i.first
                            break
                        }
                    }

                    if (secondUser != 543835L) {
                        90.dp
                    } else {
                        20.dp
                    }
                } else {
                    90.dp
                }
            )
        ) {
            itemsIndexed(
                sortedMessages, key = { _, message ->
                    message.first
                }
            ) { index, message ->

                val horizontal: Alignment.Horizontal = if (!message.second.attaches?.jsonArray.isNullOrEmpty() && message.second.attaches?.jsonArray?.last()?.jsonObject?.contains("event") == true) {
                    Alignment.CenterHorizontally
                } else if (message.second.senderID == AccountManager.accountID) {
                    Alignment.End
                } else {
                    Alignment.Start
                }

                val prevMessage = if (index != sortedMessages?.size?.minus(1)) sortedMessages?.get(
                    index + 1
                )?.second ?: Message() else Message()

                val nextMessage = if (index > 0) sortedMessages?.get(index - 1)?.second
                    ?: Message() else Message()

                val timePrev =
                    Utils.getTime(prevMessage.sendTime)

                val timeCurr =
                    Utils.getTime(message.second.sendTime)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            showBottomSheet = !showBottomSheet
                            currentMessage = message
                        }),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, horizontal),
                ) {
                    DrawMessage(
                        message.second,
                        chatType,
                        prevMessage,
                        nextMessage,
                        chatID,
                        message.first,
                        users,
                    )
                }

                if (timePrev.date != timeCurr.date) {
                    val timeNow = Utils.getTime(Date().time)

                    val timeText = if (timeNow.date == timeCurr.date) {
                        "Сегодня"
                    } else if (timeNow.day - timeCurr.day == 1 && timeCurr.month == timeNow.month && timeCurr.year == timeNow.year) {
                        "Вчера"
                    } else {
                        Utils.getTimeString(message.second.sendTime, false, false)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
                            .background(
                                color = colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(timeText, textAlign = TextAlign.Center, modifier = Modifier.padding(start = 4.dp, end = 4.dp))
                        }
                    }
                }
            }
        }


        val curEditMessage = if (currentEditMessage == 0L) Message() else messages!![currentEditMessage]!!
        val curReplyMessage = if (currentReplyMessage == 0L) Message() else messages!![currentReplyMessage]!!

        if (chatType == "DIALOG") {
            var secondUser = 0L

            for (i in chats[chatID]!!.users.toList()!!) {
                if (i.first != AccountManager.accountID) {
                    secondUser = i.first
                    break
                }
            }

            if (secondUser != 543835L) {
                DrawBottom(Modifier.align(Alignment.BottomCenter), chats, chatType, chatID, hazeState, { currentEditMessage = it},
                    currentEditMessage, curEditMessage, {coroutineScope.launch { listState.animateScrollToItem(0, 0) } },
                    isNotBottom, bottom.value, currentReplyMessage, curReplyMessage, { currentReplyMessage = it})
            }
        } else {
            DrawBottom(Modifier.align(Alignment.BottomCenter), chats, chatType, chatID, hazeState, { currentEditMessage = it},
                currentEditMessage, curEditMessage, {coroutineScope.launch { listState.animateScrollToItem(0, 0) } },
                isNotBottom, bottom.value, currentReplyMessage, curReplyMessage, { currentReplyMessage = it})
        }
    }
}

fun editMessage(message : String, chatID : Long, chat : Chat, chatType : String, uploadedImages : List<Pair<Uri, JsonObject>>, currentMessageEdit: Long) {
    val payload = mutableMapOf(
        "messageId" to JsonPrimitive(currentMessageEdit),
        "chatId" to JsonPrimitive(chatID),
        "elements" to JsonArray(emptyList()),
    )

    if (message.isNotEmpty()) {
        payload["text"] = JsonPrimitive(message)
    }

    if (uploadedImages.isNotEmpty()) {
        var attaches = mutableListOf<JsonObject>()
        for (i in uploadedImages) {
            attaches += JsonObject(
                mapOf(
                    "photoToken" to JsonPrimitive(
                        i.second.toList()
                            .last().second.jsonObject["token"]!!.jsonPrimitive.content
                    ), "_type" to JsonPrimitive("PHOTO")
                )
            )
        }

        payload["attaches"] = JsonArray(attaches)
    }

    GlobalScope.launch {
        SocketManager.sendPacket(OPCode.EDIT_MESSAGE, JsonObject(payload), { packet ->
            if (packet.payload is JsonObject) {
                GlobalScope.launch {
                    ChatsManager.addMessage(
                        packet.payload.jsonObject["message"]!!.jsonObject, chatID
                    )
                }
            }
        })
    }
}
fun sendMessage(message : String, chatID : Long, chat : Chat, chatType : String, uploadedImages : List<Pair<Uri, JsonObject>>,
                replyMessage : Long, fileToken : String, uploadedVideos : List<String>, forwardChatID : Long = 0L, forwardMessageID : Long = 0L) {
    var secondUser = 0L

    if (uploadedImages.isEmpty() && message.isEmpty() && fileToken.isEmpty() && uploadedVideos.isEmpty() && forwardMessageID == 0L) {
        return
    }

    var attaches = mutableListOf<JsonObject>()

    val messageObject = mutableMapOf(
        "isLive" to JsonPrimitive(false),
        "detectShare" to JsonPrimitive(true),
        "elements" to JsonArray(emptyList()),
        "cid" to JsonPrimitive(System.currentTimeMillis()),
    )

    if (message.isNotEmpty()) {
        messageObject["text"] = JsonPrimitive(message)
    }

    if (replyMessage != 0L) {
        messageObject["link"] = JsonObject(mapOf(
            "type" to JsonPrimitive("REPLY"),
            "chatId" to JsonPrimitive(chatID),
            "messageId" to JsonPrimitive(replyMessage)
        ))
    }

    if (uploadedImages.isNotEmpty()) {
        for (i in uploadedImages) {
            attaches += JsonObject(
                mapOf(
                    "photoToken" to JsonPrimitive(
                        i.second.toList()
                            .last().second.jsonObject["token"]!!.jsonPrimitive.content
                    ), "_type" to JsonPrimitive("PHOTO")
                )
            )
        }

    }

    if (uploadedVideos.isNotEmpty()) {
        for (i in uploadedVideos) {
            attaches += JsonObject(
                mapOf(
                    "videoType" to JsonPrimitive(0),
                    "_type" to JsonPrimitive("VIDEO"),
                    "token" to JsonPrimitive(i)
                )
            )
        }
    }

    if (attaches.isNotEmpty()) {
        messageObject["attaches"] = JsonArray(attaches)
    }

    if (fileToken.isNotEmpty()) {
        messageObject["attaches"] = JsonArray(
            listOf(
                JsonObject(mapOf(
                    "_type" to JsonPrimitive("FILE"),
                    "token" to JsonPrimitive(fileToken)
                ))
            )
        )
    }

    if (forwardMessageID != 0L) {
        messageObject["link"] = JsonObject(mapOf(
            "type" to JsonPrimitive("FORWARD"),
            "chatId" to JsonPrimitive(forwardChatID),
            "messageId" to JsonPrimitive(forwardMessageID)
        ))
    }

    if (chatType == "DIALOG") {
        for (i in chat.users.toList()!!) {
            if (i.first != AccountManager.accountID) {
                secondUser = i.first
                break
            }
        }
    }

    val payload = JsonObject(
        mapOf(
            if (chatType == "CHAT") "chatId" to JsonPrimitive(chatID) else if (chatID == 0L) "userId" to JsonPrimitive(
                AccountManager.accountID
            ) else "userId" to JsonPrimitive(secondUser),
            "message" to JsonObject(
                messageObject
            )
        )
    )

    println(payload)

    GlobalScope.launch {
        SocketManager.sendPacket(OPCode.SEND_MESSAGE, payload, { packet->
            if (packet.payload is JsonObject) {
                GlobalScope.launch {
                    ChatsManager.addMessage(
                        packet.payload.jsonObject["message"]!!.jsonObject, chatID
                    )
                }
            }
        })
    }
}
@Composable
fun DrawBottomChannel() {
    TextButton(onClick = {

    }, modifier = Modifier.fillMaxWidth()) {
        Text("Выкл. уведомления")
    }
}

fun uploadFile(uris : List<Uri>, context : Context, chatID : Long, chat : Chat, chatType : String) {
    for (uri in uris) {
        val payload = JsonObject(mapOf(
            "count" to JsonPrimitive(1)
        ))

        GlobalScope.launch {
            SocketManager.sendPacket(OPCode.UPLOAD_FILE, payload, { packet ->
                if (packet.payload is JsonObject) {

                    val item = packet.payload["info"]!!.jsonArray.last().jsonObject
                    val url = item["url"]!!.jsonPrimitive.content
                    val token = item["token"]!!.jsonPrimitive.content
                    val fileId = item["fileId"]!!.jsonPrimitive.long

                    var fileName = ""

                    val cursor =
                        context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex =
                                it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                            fileName = it.getString(nameIndex)
                        }
                    }
                    val encodedFilename = URLEncoder
                        .encode(fileName, "UTF-8")
                        .replace("+", "%20")
                    val client = OkHttpClient()

                    val imageBytes = try {
                        context.contentResolver.openInputStream(uri)
                            ?.use { inputStream ->
                                inputStream.readBytes()
                            }
                    } catch (e: Exception) {
                        null
                    }

                    val metrics = context.resources.displayMetrics

                    val requestBody = imageBytes!!.toRequestBody(
                        "application/x-binary; charset=x-user-defined".toMediaType()
                    )

                    SocketManager.addFileCallback(fileId, { sendMessage("", chatID, chat, chatType, emptyList(), 0L, token, emptyList())})
                    val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                    val request = Request.Builder()
                        .url(
                            url.toHttpUrl().newBuilder()
                                .build()
                        )
                        .post(requestBody)
                        .addHeader(
                            "User-Agent",
                            "OKMessages/26.24.0 (Android ${Build.VERSION.RELEASE}; $deviceName ; ${metrics.densityDpi}dpi ${metrics.densityDpi}dpi ${metrics.heightPixels}x${metrics.widthPixels})"
                        )
                        .addHeader(
                            "Content-Disposition",
                            "attachment; filename=$encodedFilename"
                        )
                        .addHeader(
                            "Content-Range",
                            "bytes 0-${imageBytes!!.size - 1}/${imageBytes.size}"
                        )
                        .addHeader(
                            "HttpHeaders.Connection",
                            "keep-alive"
                        )
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            e.printStackTrace()
                            return
                        }

                        override fun onResponse(call: Call, response: Response) {
                            response.use {
//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(context, "Файл загружен", Toast.LENGTH_SHORT).show()
//                                }
                            }
                        }
                    })
                }
            })
        }
    }
}

@Composable
fun DrawBottom(modifier : Modifier, chats : Map<Long, Chat>, chatType : String, chatID : Long, hazeState: HazeState, setMessageEdit: (Long) -> Unit, currentMessageEdit : Long, currentEdit : Message,
    scrollToBottom : () -> Unit, isNotBottom : Boolean, bottom : Float, currentMessageReply : Long, currentReply : Message, setReply: (Long) -> Unit) {
    val message = remember { mutableStateOf("") }
    val chat = remember { mutableStateOf(chats[chatID]) }
    val users by UsersManager.usersList.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val showPickFiles = remember { mutableStateOf(false) }
    var selectedImages by remember {
        mutableStateOf<List<Uri?>>(emptyList())
    }

    val activity = LocalActivity.current

    var selectedVideos by remember {
        mutableStateOf<List<Uri?>>(emptyList())
    }

    var selectedFiles by remember {
        mutableStateOf<List<Uri?>>(emptyList())
    }
    val intent = activity?.intent!!

    val forwardChatID =  intent.getLongExtra("forwardChatID", 0L)
    val forwardMessageID = intent.getLongExtra("forwardMessageID", 0L)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)) { uris ->
        if (uris != null) {
            for (uri in uris) {
                val mimeType = context.contentResolver.getType(uri)

                when {
                    mimeType?.startsWith("image/") == true -> {
                        selectedImages += uri
                    }
                    mimeType?.startsWith("video/") == true -> {
                        selectedVideos += uri
                    }
                    else -> {
                    }
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uploadFile(uris, context, chatID, chat.value!!, chatType)
        }
    }

    var uploadedImages = remember {
        mutableListOf<Pair<Uri, JsonObject>>()
    }

    var uploadedVideos = remember {
        mutableListOf<String>()
    }

    LaunchedEffect(currentMessageEdit) {
        if (currentMessageEdit != 0L) {
            message.value = if (!currentEdit.message.isNullOrEmpty()) currentEdit.message else ""
        } else {
            message.value = ""
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = isNotBottom,
                enter = slideInVertically { with(density) { 50.dp.roundToPx() } },
                exit = slideOutVertically { with(density) { +50.dp.roundToPx() } }
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, bottom = 4.dp)
                ) {
                    IconButton(
                        onClick = {
                            scrollToBottom()
                        },
                        modifier = Modifier
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


            AnimatedVisibility(
                visible = showPickFiles.value,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(modifier = Modifier
                        .padding(bottom = 16.dp, end = 20.dp)
                        .background(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(IntrinsicSize.Max)) {
                            Row(modifier = Modifier
                                .clickable {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                    showPickFiles.value = false
                                }
                                .fillMaxWidth()) {
                                Icon(
                                    Icons.Filled.AttachFile,
                                    contentDescription = "File",
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 8.dp)
                                        .align(Alignment.CenterVertically)
                                )

                                Text("Файл", style = MaterialTheme.typography.titleMedium)
                            }

                            Row(modifier = Modifier
                                .clickable {
                                    launcher.launch(
                                        PickVisualMediaRequest(
                                            mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                        )
                                    )

                                    showPickFiles.value = false
                                }
                                .fillMaxWidth()) {
                                Icon(
                                    Icons.Filled.Photo,
                                    contentDescription = "Photo",
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 8.dp)
                                        .align(Alignment.CenterVertically)
                                )

                                Text("Фото или видео", style = MaterialTheme.typography.titleMedium)
                            }

                            Row(modifier = Modifier
                                .clickable {
                                    Toast.makeText(
                                        context,
                                        "Будет доступно в следующих обновлениях!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showPickFiles.value = false
                                }
                                .fillMaxWidth()) {
                                Icon(
                                    Icons.Filled.Poll,
                                    contentDescription = "Poll",
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 8.dp)
                                        .align(Alignment.CenterVertically)
                                )

                                Text("Опрос", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            if (selectedImages.isNotEmpty() || selectedVideos.isNotEmpty()) {
                LazyRow(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(selectedImages) { image ->
                        Box() {
                            AsyncImage(
                                model = image,
                                contentDescription = "ChatIcon",
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            4.dp
                                        )
                                    )
                                    .size(80.dp),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(onClick = {
                                selectedImages -= image
                            },
                                modifier = Modifier
                                    .size(30.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "RemoveImage",
                                    modifier = Modifier
                                        .background(colorScheme.background, CircleShape)
                                )
                            }
                        }
                    }

                    items(selectedVideos) { video ->
                        Box() {
                            val model = ImageRequest.Builder(context)
                                .data(video)
                                .videoFrameMillis(10000)
                                .decoderFactory { result, options, _ ->
                                    VideoFrameDecoder(
                                        result.source,
                                        options
                                    )
                                }
                                .build()

                            AsyncImage(
                                model = model,
                                contentDescription = "ChatIcon",
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            4.dp
                                        )
                                    )
                                    .size(80.dp),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(onClick = {
                                selectedVideos -= video
                            },
                                modifier = Modifier
                                    .size(30.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "RemoveImage",
                                    modifier = Modifier
                                        .background(colorScheme.background, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            val topPadding = if (currentMessageEdit == 0L && currentMessageReply == 0L && forwardMessageID == 0L) 16.dp else 0.dp

            Box(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .hazeEffect(state = hazeState) {
                    blurEffect {
                        blurRadius = 20.dp
                        colorEffects = listOf(HazeColorEffect.tint(Color.Black.copy(alpha = 0.3f)))
                    }
                }
                .fillMaxWidth()
            ) {
                Column() {
                    AnimatedVisibility(
                        visible = currentMessageEdit != 0L || currentMessageReply != 0L || forwardMessageID != 0L,
                        enter = slideInVertically { with(density) { 50.dp.roundToPx() } },
                        exit = slideOutVertically { with(density) { +50.dp.roundToPx() } }
                    ) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp)
                            .background(
                                color = colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = if (currentMessageReply != 0L) Icons.AutoMirrored.Filled.Reply else if (currentMessageEdit != 0L) Icons.Filled.Edit
                            else if (forwardMessageID != 0L) Icons.AutoMirrored.Filled.Forward else Icons.Filled.ArrowDropDown
                            Row(horizontalArrangement = Arrangement.Center) {
                                Icon(
                                    icon,
                                    contentDescription = "Editing...",
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 8.dp)
                                        .align(Alignment.CenterVertically)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    val forwardChat by remember { mutableStateOf(chats[forwardChatID]) }
                                    val forwardMsg by remember { mutableStateOf(if (forwardMessageID != 0L) forwardChat?.messages?.get(forwardMessageID) else Message()) }

                                    val user = if (currentMessageReply != 0L) {
                                        UsersManager.checkForExisting(currentReply.senderID!!)
                                    } else {
                                        User()
                                    }
                                    val name = if (currentMessageReply != 0L) {
                                        Utils.getFullName(currentReply.senderID!!)
                                    } else if (forwardMessageID != 0L) {
                                        if (forwardChat!!.type == "CHANNEL") {
                                            forwardChat!!.title
                                        } else {
                                            Utils.getFullName(forwardMsg!!.senderID!!)
                                        }
                                    } else {
                                        ""
                                    }

                                    val text = if (currentMessageReply != 0L) {
                                        "В ответ $name"
                                    } else if (currentMessageEdit != 0L) {
                                        "Редактирование"
                                    } else if (forwardMessageID != 0L) {
                                        "Переслать сообщение"
                                    } else {
                                        ""
                                    }

                                    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                    val messageText = if (currentMessageEdit != 0L) {
                                        if (!currentEdit.message.isNullOrEmpty()) {
                                            currentEdit.message
                                        } else {
                                            ""
                                        }
                                    } else if (currentMessageReply != 0L) {
                                        if (!currentReply.message.isNullOrEmpty()) {
                                            currentReply.message
                                        } else {
                                            ""
                                        }
                                    } else {
                                        "От $name"
                                    }

                                    Text(messageText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                IconButton(onClick = {
                                    if (currentMessageEdit != 0L) setMessageEdit(0L) else if (currentMessageReply != 0L) setReply(0L) else {
                                        intent.removeExtra("forwardChatID")
                                        intent.removeExtra("forwardMessageID")
                                    }
                                }, modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close edit message"
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 8.dp, end = 8.dp)
                        .background(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(
                                bottomEnd = 16.dp,
                                bottomStart = 16.dp,
                                topStart = topPadding,
                                topEnd = topPadding
                            )
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chatType == "CHANNEL") {
                            DrawBottomChannel()
                        } else {
                            IconButton(onClick = {}) {
                                Icon(
                                    Icons.Filled.EmojiEmotions,
                                    contentDescription = "AddEmoji"
                                )
                            }

                            TextField(value = message.value, onValueChange = { if (message.value.length < 4000) message.value = it },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 10,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Сообщение... ", modifier = Modifier.alpha(0.7f)) }
                            )

                            Row(horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = {
                                    showPickFiles.value = !showPickFiles.value
                                }) {
                                    Icon(
                                        Icons.Filled.AttachFile,
                                        contentDescription = "AddFile"
                                    )
                                }

                                IconButton(onClick = {
                                    if (forwardMessageID != 0L) {
                                        sendMessage("", chatID, chat.value!!, chatType, emptyList(), 0L, "", emptyList(), forwardChatID, forwardMessageID)

                                        intent.removeExtra("forwardChatID")
                                        intent.removeExtra("forwardMessageID")
                                    }

                                    if (message.value.isNotEmpty() || selectedImages.isNotEmpty() || selectedVideos.isNotEmpty()) {
                                        val result = CompletableDeferred<List<Pair<Uri, JsonObject>>>()
                                        val videoResult = CompletableDeferred<List<String>>()

                                        if (selectedImages.isNotEmpty()) {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    for (i in selectedImages) {
                                                        delay(1.seconds)
                                                        SocketManager.sendPacket(OPCode.UPLOAD_IMAGE, JsonObject(
                                                            mapOf(
                                                                "profile" to JsonPrimitive(false),
                                                                "count" to JsonPrimitive(1)
                                                            )
                                                        ), { packet ->
                                                            if (packet.payload is JsonObject) {
                                                                var imageName = ""

                                                                val cursor =
                                                                    context.contentResolver.query(i!!, null, null, null, null)
                                                                cursor?.use {
                                                                    if (it.moveToFirst()) {
                                                                        val nameIndex =
                                                                            it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                                                                        imageName = it.getString(nameIndex)
                                                                    }
                                                                }

                                                                val client = OkHttpClient()
                                                                val url = packet.payload["url"]?.jsonPrimitive?.content.toString()

                                                                val imageBytes = try {
                                                                    context.contentResolver.openInputStream(i)
                                                                        ?.use { inputStream ->
                                                                            inputStream.readBytes()
                                                                        }
                                                                } catch (e: Exception) {
                                                                    null
                                                                }

                                                                val metrics = context.resources.displayMetrics

                                                                val requestBody = imageBytes!!.toRequestBody(
                                                                    "application/octet-stream".toMediaType()
                                                                )

                                                                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                                                                val request = Request.Builder()
                                                                    .url(
                                                                        url.toHttpUrl().newBuilder()
                                                                            .build()
                                                                    )
                                                                    .post(requestBody)
                                                                    .addHeader(
                                                                        "User-Agent",
                                                                        "OKMessages/26.24.0 (Android ${Build.VERSION.RELEASE}; $deviceName ; ${metrics.densityDpi}dpi ${metrics.densityDpi}dpi ${metrics.heightPixels}x${metrics.widthPixels})"
                                                                    )
                                                                    .addHeader(
                                                                        "Content-Disposition",
                                                                        "attachment; filename=$imageName"
                                                                    )
                                                                    .addHeader(
                                                                        "X-Uploading-Mode",
                                                                        "parallel"
                                                                    )
                                                                    .addHeader(
                                                                        "Content-Range",
                                                                        "bytes 0-${imageBytes!!.size - 1}/${imageBytes.size}"
                                                                    )
                                                                    .addHeader(
                                                                        "HttpHeaders.Connection",
                                                                        "keep-alive"
                                                                    )
                                                                    .addHeader(
                                                                        "HttpHeaders.AcceptEncoding",
                                                                        "gzip"
                                                                    )
                                                                    .build()

                                                                client.newCall(request).enqueue(object : Callback {
                                                                    override fun onFailure(call: Call, e: IOException) {
                                                                        e.printStackTrace()
                                                                        result.complete(emptyList())
                                                                        uploadedImages.clear()
                                                                        selectedImages = emptyList()
                                                                    }

                                                                    override fun onResponse(call: Call, response: Response) {
                                                                        response.use {
                                                                            val responseData = it.body?.string()
                                                                            val content =
                                                                                Json.parseToJsonElement(responseData!!).jsonObject
                                                                            uploadedImages += Pair(
                                                                                i,
                                                                                content["photos"]!!.jsonObject
                                                                            )

                                                                            if (uploadedImages.size == selectedImages.size) {
                                                                                result.complete(uploadedImages)
                                                                            }
                                                                        }
                                                                    }
                                                                })
                                                            }
                                                        }
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            result.complete(emptyList())
                                            uploadedImages.clear()
                                            selectedImages = emptyList()
                                        }

                                        if (selectedVideos.isNotEmpty()) {
                                            for (i in selectedVideos) {
                                                coroutineScope.launch {
                                                    SocketManager.sendPacket(OPCode.UPLOAD_VIDEO, JsonObject(
                                                        mapOf(
                                                            "uploaderType" to JsonPrimitive(0),
                                                            "type" to JsonPrimitive(0),
                                                            "count" to JsonPrimitive(1),
                                                        )
                                                    ), { packet ->
                                                        if (packet.payload is JsonObject) {
                                                            var imageName = ""

                                                            val cursor =
                                                                context.contentResolver.query(i!!, null, null, null, null)
                                                            cursor?.use {
                                                                if (it.moveToFirst()) {
                                                                    val nameIndex =
                                                                        it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                                                                    imageName = it.getString(nameIndex)
                                                                }
                                                            }

                                                            val client = OkHttpClient()

                                                            val videoObject = packet.payload["info"]!!.jsonArray.last().jsonObject

                                                            val url = videoObject["url"]?.jsonPrimitive?.content.toString()
                                                            val token = videoObject["token"]?.jsonPrimitive?.content.toString()

                                                            val videoBytes = try {
                                                                context.contentResolver.openInputStream(i)
                                                                    ?.use { inputStream ->
                                                                        inputStream.readBytes()
                                                                    }
                                                            } catch (e: Exception) {
                                                                null
                                                            }

                                                            val metrics = context.resources.displayMetrics

                                                            val requestBody = videoBytes!!.toRequestBody(
                                                                "application/x-binary; charset=x-user-defined".toMediaType()
                                                            )

                                                            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                                                            val request = Request.Builder()
                                                                .url(
                                                                    url.toHttpUrl().newBuilder()
                                                                        .build()
                                                                )
                                                                .post(requestBody)
                                                                .addHeader(
                                                                    "Content-Disposition",
                                                                    "attachment; filename=$imageName"
                                                                )
                                                                .addHeader(
                                                                    "Connection",
                                                                    "keep-alive"
                                                                )
                                                                .addHeader(
                                                                    "Content-Range",
                                                                    "bytes 0-${videoBytes.size - 1}/${videoBytes.size}"
                                                                )
                                                                .addHeader(
                                                                    "Content-Length",
                                                                    "${videoBytes.size}"
                                                                )
                                                                .build()

                                                            client.newCall(request).enqueue(object : Callback {
                                                                override fun onFailure(call: Call, e: IOException) {
                                                                    e.printStackTrace()
                                                                    result.complete(emptyList())
                                                                    uploadedVideos.clear()
                                                                    selectedVideos = emptyList()
                                                                }

                                                                override fun onResponse(call: Call, response: Response) {
                                                                    response.use {
                                                                        uploadedVideos += token

                                                                        if (uploadedVideos.size == selectedVideos.size) {
                                                                            videoResult.complete(uploadedVideos)
                                                                        }
                                                                    }
                                                                }
                                                            })
                                                        }
                                                    }
                                                    )
                                                }
                                            }
                                        } else {
                                            videoResult.complete(emptyList())
                                            uploadedVideos.clear()
                                            selectedVideos = emptyList()
                                        }

                                        coroutineScope.launch {
                                            val photo = result.await()
                                            val videos = videoResult.await()

                                            if (currentMessageEdit == 0L) {
                                                sendMessage(message.value, chatID, chat.value!!, chatType, uploadedImages, currentMessageReply, "", uploadedVideos)
                                                setReply(0L)
                                            } else {
                                                editMessage(message.value, chatID, chat.value!!, chatType, uploadedImages, currentMessageEdit)
                                                setMessageEdit(0L)
                                            }

                                            message.value = ""
                                            uploadedImages.clear()
                                            selectedImages = emptyList()
                                            uploadedVideos.clear()
                                            selectedVideos = emptyList()
                                        }
                                    }
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// TODO: Fix lags
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun DrawMessage(message: Message, chatType: String, previousMessage: Message, nextMessage: Message, chatId : Long, messageId : Long, users : Map<Long, User>) {
    if (chatType == "CHAT") {
        UsersManager.checkForExisting(message.senderID!!)
    }
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val user = users[message.senderID]
    val name = Utils.getFullName(user)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    if (!message.attaches?.jsonArray.isNullOrEmpty() && message.attaches.jsonArray.last().jsonObject.contains("event")) {
        val lastAttach = message.attaches.jsonArray.last().jsonObject
        if (lastAttach.contains("event")) {
            // Event
            val event = Utils.getEventString(message, chatType)

            Column() {
                Box(modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp)
                    .background(
                        color = colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Text(event, textAlign = TextAlign.Center,
                        color = colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }

                if (lastAttach.jsonObject["event"]!!.jsonPrimitive.content == "icon") {
                    val url = lastAttach.jsonObject["fullUrl"]!!.jsonPrimitive.content

                    AsyncImage(
                        model = url,
                        contentDescription = "ChangedIcon",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .clickable {
                                val intent = Intent(context, MediaViewActivity::class.java)

                                intent.putExtra("isSingleImage", true)
                                intent.putExtra("image", url)

                                context.startActivity(intent)
                            }
                            .padding(top = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    } else {
        var shouldAddPadding = message.senderID != AccountManager.accountID && chatType == "CHAT" && nextMessage.senderID == message.senderID
                && Utils.getTime(message.sendTime).date == Utils.getTime(nextMessage.sendTime).date
//                || (previousMessage.attaches?.jsonArray?.isNotEmpty() == true && !previousMessage.attaches.jsonArray.last().jsonObject.contains("event") && chatType == "CHAT" && message.senderID != AccountManager.accountID)
        Row(modifier = Modifier.padding(
            start = if (shouldAddPadding) 55.dp else 6.dp,
            end = 6.dp
        ), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start)) {
            // Avatar
            val shouldShowAvatar = message.senderID != AccountManager.accountID && chatType == "CHAT" && nextMessage.senderID != message.senderID
                    || (Utils.getTime(message.sendTime).date != Utils.getTime(nextMessage.sendTime).date && message.senderID != AccountManager.accountID && chatType == "CHAT")
                    || (previousMessage.attaches?.jsonArray?.isNotEmpty() == true && previousMessage.attaches.jsonArray.last().jsonObject.contains("event") && chatType == "CHAT"
                    && message.senderID != AccountManager.accountID && previousMessage.senderID == AccountManager.accountID)
//                     && !(nextMessage.attaches?.jsonArray?.isNotEmpty() == true && nextMessage.attaches?.jsonArray?.last()?.jsonObject?.contains("event") == true)
            if (shouldShowAvatar) {
                Utils.AvatarFromName(name, user?.avatarUrl, 45, Modifier
                    .align(Alignment.Bottom)
                    .clickable {
                        val userChatId = message.senderID
                        val intent = Intent(context, ChatInfoActivity::class.java)

                        intent.putExtra("userID", userChatId)

                        context.startActivity(intent)
                    })
            }
            val shouldShowName = (chatType == "CHAT" && message.senderID != AccountManager.accountID && previousMessage.senderID != message.senderID)
                    || (previousMessage.attaches?.jsonArray?.isNotEmpty() == true && previousMessage.attaches.jsonArray.last().jsonObject.contains("event") && chatType == "CHAT" && message.senderID != AccountManager.accountID)
                    || (Utils.getTime(message.sendTime).date != Utils.getTime(previousMessage.sendTime).date && chatType == "CHAT" && message.senderID != AccountManager.accountID)
            var width by remember { mutableIntStateOf(0) }

            if (!message.attaches?.jsonArray.isNullOrEmpty()
                && message.attaches!!.jsonArray.last().jsonObject.contains("_type")
                && message.attaches.jsonArray.last().jsonObject["_type"]!!.jsonPrimitive.content == "STICKER"
                ||
                !message.link?.message?.attaches?.jsonArray.isNullOrEmpty()
                && message.link.message.attaches!!.jsonArray.last().jsonObject.contains("_type")
                && message.link.message.attaches.jsonArray.last().jsonObject["_type"]!!.jsonPrimitive.content == "STICKER")
            {
                val lastAttach =
                    if (!message.attaches?.jsonArray.isNullOrEmpty()) message.attaches.jsonArray.last().jsonObject
                    else if (!message.link?.message?.attaches?.jsonArray.isNullOrEmpty()) message.link.message.attaches.jsonArray.last().jsonObject
                    else JsonObject(mapOf())
                // message.attaches.jsonArray.last().jsonObject
                val stickerType = lastAttach["stickerType"]!!.jsonPrimitive.content
                val width = lastAttach["width"]!!.jsonPrimitive.int.dp
                val height = lastAttach["height"]!!.jsonPrimitive.int.dp
                Box() {
                    Column() {
                        if (message.link?.type == "REPLY") {
                            Box(modifier = Modifier.background(
                                color = Color.Black,
                                shape = RoundedCornerShape(8.dp)
                            ).align(Alignment.End)
                            ) {
                                if (chatType != "CHANNEL") {
                                    UsersManager.checkForExisting(message.link.message?.senderID!!)
                                }

                                if (message.link.message?.status == "REMOVED") {
                                    Text("Удалённое сообщение", color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(start = 4.dp, end = 4.dp), fontStyle = FontStyle.Italic)
                                } else {
                                    val userReply = if (chatType == "CHANNEL") {
                                        User()
                                    } else {
                                        users[message.link.message?.senderID]
                                    }
                                    val replyName = if (chatType == "CHANNEL") {
                                        ChatsManager.chatsList.collectAsState().value[chatId]!!.title
                                    } else {
                                        Utils.getFullName(userReply)
                                    }

                                    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                        Text(replyName, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.alpha(0.8f), overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (!message.link.message?.attaches?.jsonArray.isNullOrEmpty()) {
                                                for (attach in message.link.message.attaches?.jsonArray!!) {
                                                    if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "PHOTO") {
                                                        AsyncImage(
                                                            model = attach.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                            contentDescription = "ChatIcon",
                                                            modifier = Modifier
                                                                .clip(
                                                                    RoundedCornerShape(
                                                                        4.dp
                                                                    )
                                                                )
                                                                .size(32.dp),
                                                            contentScale = ContentScale.Crop
                                                        )

                                                        break
                                                    }
                                                }
                                            }

                                            if (!message.link.message?.message.isNullOrEmpty()) {
                                                Text(message.link.message.message, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            } else if (!message.link.message?.attaches?.jsonArray.isNullOrEmpty()) {
                                                val text = when (message.link.message.attaches?.jsonArray[0]!!.jsonObject["_type"]!!.jsonPrimitive.content) {
                                                    "PHOTO" -> "Фото"
                                                    "FILE" -> "Файл"
                                                    "VIDEO" -> "Видео"
                                                    "STICKER" -> "Стикер"
                                                    else -> "undefined"
                                                }

                                                Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (message.link?.type == "FORWARD") {
                            Box(modifier = Modifier
                                .background(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .align(Alignment.End)
                            ) {
                                val messageType = message.link.message?.messageType

                                val idForward = message.link.message?.senderID
                                val userForward =  if (messageType == "CHANNEL") {
                                    User()
                                } else {
                                    users[idForward]
                                }

                                if (messageType != "CHANNEL") {
                                    UsersManager.checkForExisting(idForward!!)
                                }

                                val forwardName = if (messageType == "CHANNEL") {
                                    message.link.message.channelName!!
                                } else {
                                    Utils.getFullName(userForward)
                                }

                                val forwardIcon = if (messageType == "CHANNEL") {
                                    message.link.message.channelIcon
                                } else {
                                    userForward?.avatarUrl
                                }

                                val sentFrom = buildAnnotatedString {
                                    append("Переслано от: \n")

                                    appendInlineContent(id = "avatar")
                                    append(" ")
                                    withStyle(style = SpanStyle(color = Color.White)) {
                                        append(forwardName)
                                    }
                                }

                                val inlineContentMap = mutableMapOf<String, InlineTextContent>(

                                )

                                val placeholder = Placeholder(
                                    width = 30.sp,
                                    height = 30.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )

                                inlineContentMap["avatar"] =
                                    InlineTextContent(placeholder) { _ ->
                                        Utils.AvatarFromName(forwardName, forwardIcon, 30)
                                    }

                                Text(
                                    sentFrom,
                                    inlineContent = inlineContentMap,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable {
                                        if (messageType != "CHANNEL") {
                                            val userChatId = message.senderID
                                            val intent = Intent(context, ChatInfoActivity::class.java)

                                            intent.putExtra("userID", idForward)

                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }

                        when (stickerType) {
                            "STATIC" -> {
                                val url = lastAttach["url"]!!.jsonPrimitive.content

                                AsyncImage(
                                    model = url,
                                    contentDescription = "ChatIcon",
                                    modifier = Modifier
                                        .width(width)
                                        .height(height),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            "LOTTIE" -> {
                                val lottieUrl = lastAttach["lottieUrl"]!!.jsonPrimitive.content

                                val lottieCompositionSpec = LottieCompositionSpec.Url(lottieUrl)
                                val lottieComposition = rememberLottieComposition(
                                    spec = lottieCompositionSpec,
                                )

                                if (lottieComposition.isLoading) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .width(width)
                                            .height(height)
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (lottieComposition.isSuccess) {
                                    LottieAnimation(
                                        composition = lottieComposition.value,
                                        modifier = Modifier
                                            .width(width)
                                            .height(height),
                                        iterations = LottieConstants.IterateForever,
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier
                            .padding(end = 4.dp)
                            .background(
                                color = Color.Black.copy(0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(2.dp)
                            .align(Alignment.End)
                        ) {
                            Row() {
                                Text(Utils.getTimeString(message.sendTime), color = Color.White, style = MaterialTheme.typography.bodyMedium)

                                if (message.senderID == AccountManager.accountID) {
                                    val chats by ChatsManager.chatsList.collectAsState()
                                    val chat = chats[chatId]

                                    var read = false
                                    for (user in chat?.users ?: emptyMap()) {
                                        if (user.value >= message.sendTime && user.key != AccountManager.accountID) {
                                            read = true
                                            break
                                        }
                                    }

                                    Icon(
                                        imageVector = if (read) Icons.Filled.DoneAll else Icons.Filled.Check,
                                        contentDescription = "Read",
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .size((MaterialTheme.typography.bodyMedium.fontSize.value + 4).dp)
                                            .align(Alignment.CenterVertically),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                // pizdec
            } else if (!message.elements?.jsonArray.isNullOrEmpty()
                && message.elements.jsonArray.last().jsonObject.contains("type")
                && message.elements.jsonArray.last().jsonObject["type"]!!.jsonPrimitive.content == "ANIMOJI"
                && message.message?.length == 2
                ||
                !message.link?.message?.elements?.jsonArray.isNullOrEmpty()
                && message.link.message.elements.jsonArray.last().jsonObject.contains("type")
                && message.link.message.elements.jsonArray.last().jsonObject["type"]!!.jsonPrimitive.content == "ANIMOJI"
                && message.link.message.message?.length == 2) {
                val lastElement =
                    if (!message.elements?.jsonArray.isNullOrEmpty()) message.elements.jsonArray.last().jsonObject
                    else if (!message.link?.message?.elements?.jsonArray.isNullOrEmpty()) message.link.message?.elements?.jsonArray!!.last().jsonObject
                    else JsonObject(mapOf())

                val lottieUrl = lastElement["attributes"]!!.jsonObject["animojiLottieUrl"]!!.jsonPrimitive.content

                val lottieCompositionSpec = LottieCompositionSpec.Url(lottieUrl)
                val lottieComposition = rememberLottieComposition(
                    spec = lottieCompositionSpec,
                )

                Box() {
                    Column() {
                        if (message.link?.type == "REPLY") {
                            Box(modifier = Modifier.background(
                                color = Color.Black,
                                shape = RoundedCornerShape(8.dp)
                            ).align(Alignment.End)) {
                                val sentMessage = if (message.link.message?.link?.type == "FORWARD") {
                                    message.link.message?.link?.message
                                } else {
                                    message.link.message
                                }

                                if (chatType != "CHANNEL") {
                                    UsersManager.checkForExisting(message.link.message?.senderID!!)
                                }

                                if (message.link.message?.status == "REMOVED") {
                                    Text("Удалённое сообщение", color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(start = 4.dp, end = 4.dp), fontStyle = FontStyle.Italic)
                                } else {
                                    val userReply = if (chatType == "CHANNEL") {
                                        User()
                                    } else {
                                        users[message.link.message?.senderID]
                                    }
                                    val replyName = if (chatType == "CHANNEL") {
                                        ChatsManager.chatsList.collectAsState().value[chatId]!!.title
                                    } else {
                                        Utils.getFullName(userReply)
                                    }

                                    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                        Text(replyName, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.alpha(0.8f), overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (!sentMessage?.attaches?.jsonArray.isNullOrEmpty()) {
                                                for (attach in sentMessage.attaches?.jsonArray!!) {
                                                    if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "PHOTO") {
                                                        AsyncImage(
                                                            model = attach.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                            contentDescription = "ChatIcon",
                                                            modifier = Modifier
                                                                .clip(
                                                                    RoundedCornerShape(
                                                                        4.dp
                                                                    )
                                                                )
                                                                .size(32.dp),
                                                            contentScale = ContentScale.Crop
                                                        )

                                                        break
                                                    }
                                                }
                                            }

                                            if (!sentMessage?.message.isNullOrEmpty()) {
                                                Text(sentMessage.message, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            } else if (!sentMessage?.attaches?.jsonArray.isNullOrEmpty()) {
                                                val text = when (sentMessage.attaches?.jsonArray[0]!!.jsonObject["_type"]!!.jsonPrimitive.content) {
                                                    "PHOTO" -> "Фото"
                                                    "FILE" -> "Файл"
                                                    "VIDEO" -> "Видео"
                                                    "STICKER" -> "Стикер"
                                                    else -> "undefined"
                                                }

                                                Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (message.link?.type == "FORWARD") {
                            Box(modifier = Modifier
                                .background(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .align(Alignment.End)
                            ) {
                                val messageType = message.link.message?.messageType

                                val idForward = message.link.message?.senderID
                                val userForward =  if (messageType == "CHANNEL") {
                                    User()
                                } else {
                                    users[idForward]
                                }

                                if (messageType != "CHANNEL") {
                                    UsersManager.checkForExisting(idForward!!)
                                }

                                val forwardName = if (messageType == "CHANNEL") {
                                    message.link.message.channelName!!
                                } else {
                                    Utils.getFullName(userForward)
                                }

                                val forwardIcon = if (messageType == "CHANNEL") {
                                    message.link.message.channelIcon
                                } else {
                                    userForward?.avatarUrl
                                }

                                val sentFrom = buildAnnotatedString {
                                    append("Переслано от: \n")

                                    appendInlineContent(id = "avatar")
                                    append(" ")
                                    withStyle(style = SpanStyle(color = Color.White)) {
                                        append(forwardName)
                                    }
                                }

                                val inlineContentMap = mutableMapOf<String, InlineTextContent>(

                                )

                                val placeholder = Placeholder(
                                    width = 30.sp,
                                    height = 30.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )

                                inlineContentMap["avatar"] =
                                    InlineTextContent(placeholder) { _ ->
                                        Utils.AvatarFromName(forwardName, forwardIcon, 30)
                                    }

                                Text(
                                    sentFrom,
                                    inlineContent = inlineContentMap,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable {
                                        if (messageType != "CHANNEL") {
                                            val userChatId = message.senderID
                                            val intent = Intent(context, ChatInfoActivity::class.java)

                                            intent.putExtra("userID", idForward)

                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }

                        if (lottieComposition.isLoading) {
                            Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (lottieComposition.isSuccess) {
                            LottieAnimation(
                                composition = lottieComposition.value,
                                modifier = Modifier
                                    .size(80.dp),
                                iterations = LottieConstants.IterateForever,
                            )
                        }

                        Box(modifier = Modifier
                            .padding(end = 4.dp)
                            .background(
                                color = Color.Black.copy(0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(2.dp)
                            .align(Alignment.End)
                        ) {
                            Row() {
                                Text(Utils.getTimeString(message.sendTime), color = Color.White, style = MaterialTheme.typography.bodyMedium)

                                if (message.senderID == AccountManager.accountID) {
                                    val chats by ChatsManager.chatsList.collectAsState()
                                    val chat = chats[chatId]

                                    var read = false
                                    for (user in chat?.users ?: emptyMap()) {
                                        if (user.value >= message.sendTime && user.key != AccountManager.accountID) {
                                            read = true
                                            break
                                        }
                                    }

                                    Icon(
                                        imageVector = if (read) Icons.Filled.DoneAll else Icons.Filled.Check,
                                        contentDescription = "Read",
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .size((MaterialTheme.typography.bodyMedium.fontSize.value + 4).dp)
                                            .align(Alignment.CenterVertically),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column() {
                    Box(modifier = Modifier
                        .background(
                            color = if (message.senderID == AccountManager.accountID) colorScheme.primary else colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .sizeIn(minWidth = 100.dp, maxWidth = screenWidth * 0.8f, minHeight = 45.dp)
                        .width(IntrinsicSize.Max)
                        .onSizeChanged {
                            width = it.width
                        }
                        .padding(top = 4.dp)
                    ) {
                        val textColor =
                            if (message.senderID == AccountManager.accountID) colorScheme.onPrimaryFixed else colorScheme.onBackground

                        Column(modifier = Modifier
                            .padding(start = 4.dp, bottom = 2.dp, end = 4.dp)
//                            .sizeIn(minWidth = 100.dp)
                            .fillMaxWidth()
                        ) {
                            // Message
                            if (shouldShowName) {
                                val nameColor = Utils.getColorFoName(name)
                                Text(name, color = nameColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.clickable {
                                    val userChatId = message.senderID
                                    val intent = Intent(context, ChatInfoActivity::class.java)

                                    intent.putExtra("userID", userChatId)

                                    context.startActivity(intent)
                                })
                            }

                            if (!message.link?.type.isNullOrEmpty() && message.link.type == "REPLY") {
                                val boxReply = if (message.senderID == AccountManager.accountID) colorScheme.inversePrimary else colorScheme.primaryContainer
                                val textReply = if (message.senderID == AccountManager.accountID) colorScheme.onSurface else colorScheme.onPrimaryContainer

                                Box(modifier = Modifier
                                    .background(
                                        color = boxReply,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .sizeIn(
                                        minHeight = 45.dp
                                    )
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                ) {
                                    val sentMessage = if (message.link.message?.link?.type == "FORWARD") {
                                        message.link.message?.link?.message
                                    } else {
                                        message.link.message
                                    }

                                    if (chatType != "CHANNEL") {
                                        UsersManager.checkForExisting(message.link.message?.senderID!!)
                                    }

                                    if (message.link.message?.status == "REMOVED") {
                                        Text("Удалённое сообщение", color = textReply, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(start = 4.dp, end = 4.dp), fontStyle = FontStyle.Italic)
                                    } else {
                                        val userReply = if (chatType == "CHANNEL") {
                                            User()
                                        } else {
                                            users[message.link.message?.senderID]
                                        }
                                        val replyName = if (chatType == "CHANNEL") {
                                            ChatsManager.chatsList.collectAsState().value[chatId]!!.title
                                        } else {
                                            Utils.getFullName(userReply)
                                        }

                                        Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                            Text(replyName, color = textReply, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.alpha(0.8f), overflow = TextOverflow.Ellipsis)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (!sentMessage?.attaches?.jsonArray.isNullOrEmpty()) {
                                                    for (attach in sentMessage.attaches?.jsonArray!!) {
                                                        if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "PHOTO") {
                                                            AsyncImage(
                                                                model = attach.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                                                                contentDescription = "ChatIcon",
                                                                modifier = Modifier
                                                                    .clip(
                                                                        RoundedCornerShape(
                                                                            4.dp
                                                                        )
                                                                    )
                                                                    .size(32.dp),
                                                                contentScale = ContentScale.Crop
                                                            )

                                                            break
                                                        }
                                                    }
                                                }

                                                if (!sentMessage?.message.isNullOrEmpty()) {
                                                    Text(sentMessage.message, color = textReply, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                } else if (!sentMessage?.attaches?.jsonArray.isNullOrEmpty()) {
                                                    val text = when (sentMessage.attaches?.jsonArray[0]!!.jsonObject["_type"]!!.jsonPrimitive.content) {
                                                        "PHOTO" -> "Фото"
                                                        "FILE" -> "Файл"
                                                        "VIDEO" -> "Видео"
                                                        else -> "undefined"
                                                    }

                                                    Text(text, color = textReply, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!message.message.isNullOrEmpty() || !message.attaches?.jsonArray.isNullOrEmpty()) {
                                if (!message.attaches?.jsonArray.isNullOrEmpty()) {
                                    val lastAttach = message.attaches.jsonArray.last().jsonObject
                                    val type = lastAttach["_type"]!!.jsonPrimitive.content

                                    when(type) {
                                        "FILE" -> {
                                            val fileName = lastAttach["name"]!!.jsonPrimitive.content
                                            val size = Utils.getSizeFromBytes(lastAttach["size"]!!.jsonPrimitive.long)
                                            val fileId = lastAttach["fileId"]!!.jsonPrimitive.long

                                            Row(modifier = Modifier.clickable {
                                                val payload = JsonObject(
                                                    mapOf(
                                                        "fileId" to JsonPrimitive(fileId),
                                                        "chatId" to JsonPrimitive(chatId),
                                                        "messageId" to JsonPrimitive(messageId)
                                                    )
                                                )
                                                var url = ""
                                                coroutineScope.launch {
                                                    SocketManager.sendPacket(OPCode.GET_FILE, payload, { packet ->
                                                        if (packet.payload is JsonObject) {
                                                            url =
                                                                packet.payload["url"]?.jsonPrimitive?.content.toString()
                                                            val downloadManager =
                                                                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                                                            val request =
                                                                DownloadManager.Request(url.toUri())

                                                            val metrics = context.resources.displayMetrics

                                                            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                                                            request.addRequestHeader(
                                                                "User-Agent",
                                                                "OKMessages/26.24.0 (Android ${Build.VERSION.RELEASE}; $deviceName ; ${metrics.densityDpi}dpi ${metrics.densityDpi}dpi ${metrics.heightPixels}x${metrics.widthPixels})"
                                                            )
                                                            request.setTitle(fileName)
                                                            request.setNotificationVisibility(
                                                                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                                            )
                                                            val extension = File(fileName).extension

                                                            request.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))
                                                            request.setDestinationInExternalPublicDir(
                                                                Environment.DIRECTORY_DOWNLOADS,
                                                                fileName
                                                            )
                                                            downloadManager.enqueue(request)
                                                        }
                                                    });
                                                }

                                            }) {
                                                Icon(
                                                    imageVector = Utils.getIconForFile(fileName),
                                                    contentDescription = "NotRead",
                                                    // tint = colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(start = 2.dp)
                                                        .size((MaterialTheme.typography.headlineLarge.fontSize.value + 4).dp)
                                                        .align(Alignment.CenterVertically),
                                                    tint = textColor
                                                )
                                                Column(verticalArrangement = Arrangement.Center) {
                                                    Text(fileName, color = textColor)
                                                    Text(size, color = textColor)
                                                }
                                            }
                                        }

                                        "PHOTO" -> DrawImages(message.attaches.jsonArray, LocalContext.current, chatId, messageId, width, message.message)
                                        "VIDEO" -> DrawImages(message.attaches.jsonArray, LocalContext.current, chatId, messageId, width, message.message)
                                    }
                                }

                                if (!message.message.isNullOrEmpty()) {
                                    if (!message.elements!!.jsonArray.isNullOrEmpty()) {
                                        Text(text = buildAnnotatedString {
                                            append(message.message)

                                            for (element in message.elements.jsonArray) {
                                                val type = element.jsonObject["type"]!!.jsonPrimitive.content
                                                val start = if (element.jsonObject.contains("from")) {
                                                    element.jsonObject["from"]!!.jsonPrimitive.int
                                                } else {
                                                    0
                                                }
                                                val length = start + element.jsonObject["length"]!!.jsonPrimitive.int

                                                when (type) {
                                                    "UNDERLINE" -> {
                                                        addStyle(
                                                            style = SpanStyle(
                                                                textDecoration = TextDecoration.Underline
                                                            ),
                                                            start = start,
                                                            end = length
                                                        )
                                                    }

                                                    "STRIKETHROUGH" -> {
                                                        addStyle(
                                                            style = SpanStyle(
                                                                textDecoration = TextDecoration.LineThrough
                                                            ),
                                                            start = start,
                                                            end = length
                                                        )
                                                    }

                                                    "STRONG" -> {
                                                        addStyle(
                                                            style = SpanStyle(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            start = start,
                                                            end = length
                                                        )
                                                    }

                                                    "LINK" -> {
                                                        val link = element.jsonObject["attributes"]!!.jsonObject["url"]!!.jsonPrimitive.content
                                                        val formattedLink = if (!link.startsWith("https://") && !link.startsWith("http://")) {
                                                            "https://$link"
                                                        } else {
                                                            link
                                                        }

                                                        addLink(
                                                            url = LinkAnnotation.Url(url = formattedLink),
                                                            start = start,
                                                            end = length
                                                        )

                                                        addStyle(
                                                            style = SpanStyle(
                                                                color = textColor
                                                            ),
                                                            start = start,
                                                            end = length
                                                        )
                                                    }

                                                    "HEADING" -> {
                                                        addStyle(
                                                            style = MaterialTheme.typography.headlineLarge.toSpanStyle(),
                                                            start = start,
                                                            end = length
                                                        )
                                                    }

                                                    "ANIMOJI" -> {
                                                        // :\\
                                                    }
                                                }
                                            }
                                        }, color = textColor, style = MaterialTheme.typography.bodyLarge)

                                    } else {
                                        Text(message.message, color = textColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            } else if (!message.link?.type.isNullOrEmpty() && message.link.type == "FORWARD") {
                                // Forwarded message
                                val messageType = message.link.message?.messageType

                                val idForward = message.link.message?.senderID
                                val userForward =  if (messageType == "CHANNEL") {
                                    User()
                                } else {
                                    users[idForward]
                                }

                                if (messageType != "CHANNEL") {
                                    UsersManager.checkForExisting(idForward!!)
                                }

                                val forwardName = if (messageType == "CHANNEL") {
                                    message.link.message.channelName!!
                                } else {
                                    Utils.getFullName(userForward)
                                }

                                val forwardIcon = if (messageType == "CHANNEL") {
                                    message.link.message.channelIcon
                                } else {
                                    userForward?.avatarUrl
                                }

                                val sentFrom = buildAnnotatedString {
                                    append("Переслано от: ")

                                    appendInlineContent(id = "avatar")
                                    append(" ")
                                    withStyle(style = SpanStyle(color = textColor)) {
                                        append(forwardName)
                                    }
                                }

                                val inlineContentMap = mutableMapOf<String, InlineTextContent>(

                                )

                                val placeholder = Placeholder(
                                    width = 30.sp,
                                    height = 30.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )

                                inlineContentMap["avatar"] =
                                    InlineTextContent(placeholder) { _ ->
                                        Utils.AvatarFromName(forwardName, forwardIcon, 30)
                                    }

                                Text(
                                    sentFrom,
                                    inlineContent = inlineContentMap,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.clickable {
                                        if (messageType != "CHANNEL") {
                                            val userChatId = message.senderID
                                            val intent = Intent(context, ChatInfoActivity::class.java)

                                            intent.putExtra("userID", idForward)

                                            context.startActivity(intent)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(4.dp))

//                                var animojiCount = 0

                                if (!message.link.message.attaches?.jsonArray.isNullOrEmpty()) {
                                    val lastAttach = message.link.message.attaches?.jsonArray!!.last().jsonObject
                                    val type = lastAttach["_type"]!!.jsonPrimitive.content

                                    when(type) {
                                        "FILE" -> {
                                            val fileName = lastAttach["name"]!!.jsonPrimitive.content
                                            val size = Utils.getSizeFromBytes(lastAttach["size"]!!.jsonPrimitive.long)
                                            val fileId = lastAttach["fileId"]!!.jsonPrimitive.long

                                            Row(modifier = Modifier.clickable {
                                                val payload = JsonObject(
                                                    mapOf(
                                                        "fileId" to JsonPrimitive(fileId),
                                                        "chatId" to JsonPrimitive(chatId),
                                                        "messageId" to JsonPrimitive(messageId)
                                                    )
                                                )
                                                var url = ""
                                                coroutineScope.launch {
                                                    SocketManager.sendPacket(OPCode.GET_FILE, payload, { packet ->
                                                        if (packet.payload is JsonObject) {
                                                            url =
                                                                packet.payload["url"]?.jsonPrimitive?.content.toString()

                                                            val downloadManager =
                                                                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                                                            val request =
                                                                DownloadManager.Request(url.toUri())

                                                            val metrics = context.resources.displayMetrics

                                                            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                                                            request.addRequestHeader(
                                                                "User-Agent",
                                                                "OKMessages/26.24.0 (Android ${Build.VERSION.RELEASE}; $deviceName ; ${metrics.densityDpi}dpi ${metrics.densityDpi}dpi ${metrics.heightPixels}x${metrics.widthPixels})"
                                                            )
                                                            request.setTitle(fileName)

                                                            val extension = File(fileName).extension
                                                            request.setNotificationVisibility(
                                                                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                                            )

                                                            request.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))
                                                            request.setDestinationInExternalPublicDir(
                                                                Environment.DIRECTORY_DOWNLOADS,
                                                                fileName
                                                            )
                                                            downloadManager.enqueue(request)
                                                        }
                                                    });
                                                }

                                            }) {
                                                Icon(
                                                    imageVector = Utils.getIconForFile(fileName),
                                                    contentDescription = "NotRead",
                                                    // tint = colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(start = 2.dp)
                                                        .size((MaterialTheme.typography.headlineLarge.fontSize.value + 4).dp)
                                                        .align(Alignment.CenterVertically),
                                                    tint = textColor
                                                )
                                                Column(verticalArrangement = Arrangement.Center) {
                                                    Text(fileName, color = textColor)
                                                    Text(size, color = textColor)
                                                }
                                            }
                                        }

                                        "PHOTO" -> DrawImages(message.link.message.attaches.jsonArray, LocalContext.current, chatId, messageId, width, message.link.message.message)
                                        "VIDEO" -> DrawImages(message.link.message.attaches.jsonArray, LocalContext.current, chatId, messageId, width, message.link.message.message)
                                    }
                                }

                                if (!message.link.message.elements!!.jsonArray.isEmpty()) {
                                    Text(text = buildAnnotatedString {
                                        append(message.link.message.message!!)

                                        for (element in message.link.message.elements!!.jsonArray) {
                                            val type = element.jsonObject["type"]!!.jsonPrimitive.content
                                            val start = if (element.jsonObject.contains("from")) {
                                                element.jsonObject["from"]!!.jsonPrimitive.int
                                            } else {
                                                0
                                            }
                                            val length = start + element.jsonObject["length"]!!.jsonPrimitive.int

                                            when (type) {
                                                "UNDERLINE" -> {
                                                    addStyle(
                                                        style = SpanStyle(
                                                            textDecoration = TextDecoration.Underline
                                                        ),
                                                        start = start,
                                                        end = length
                                                    )
                                                }

                                                "STRIKETHROUGH" -> {
                                                    addStyle(
                                                        style = SpanStyle(
                                                            textDecoration = TextDecoration.LineThrough
                                                        ),
                                                        start = start,
                                                        end = length
                                                    )
                                                }

                                                "STRONG" -> {
                                                    addStyle(
                                                        style = SpanStyle(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        start = start,
                                                        end = length
                                                    )
                                                }

                                                "LINK" -> {
                                                    val link = element.jsonObject["attributes"]!!.jsonObject["url"]!!.jsonPrimitive.content
                                                    val formattedLink = if (!link.startsWith("https://") && !link.startsWith("http://")) {
                                                        "https://$link"
                                                    } else {
                                                        link
                                                    }

                                                    addLink(
                                                        url = LinkAnnotation.Url(url = formattedLink),
                                                        start = start,
                                                        end = length
                                                    )

                                                    addStyle(
                                                        style = SpanStyle(
                                                            color = textColor
                                                        ),
                                                        start = start,
                                                        end = length
                                                    )
                                                }

                                                "HEADING" -> {
                                                    addStyle(
                                                        style = MaterialTheme.typography.headlineLarge.toSpanStyle(),
                                                        start = start,
                                                        end = length
                                                    )
                                                }

                                                "ANIMOJI" -> {
//                                                    replace(start, length,  "\uFFFC")
//                                                    addStringAnnotation(
//                                                        tag = "androidx.compose.foundation.text.inlineContent",
//                                                        annotation = "animoji$animojiCount",
//                                                        start = start,
//                                                        end = length
//                                                    )
                                                }
                                            }
                                        }
                                    }, color = textColor, style = MaterialTheme.typography.bodyLarge)

                                } else {
                                    Text(message.link.message.message!!, color = textColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            if (!message.reactions.isNullOrEmpty()) {
                                FlowRow(modifier = Modifier
                                    .padding(top = 4.dp)
                                    .widthIn(max = with(LocalDensity.current) { width.toDp() }),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (reaction in message.reactions) {
                                        val containerColor = if (message.senderID == AccountManager.accountID) {
                                            if (reaction.key == message.myReaction) colorScheme.inverseSurface else colorScheme.secondaryContainer
                                        } else {
                                            if (reaction.key == message.myReaction) colorScheme.secondary else colorScheme.primaryContainer
                                        }

                                        val textColor = if (message.senderID == AccountManager.accountID) {
                                            if (reaction.key == message.myReaction) colorScheme.inverseOnSurface else colorScheme.onSecondaryContainer
                                        } else {
                                            if (reaction.key == message.myReaction) colorScheme.onSecondary else colorScheme.onPrimaryContainer
                                        }

                                        Box(modifier = Modifier
                                            .clickable(onClick = {
                                                if (reaction.key == message.myReaction) {
                                                    val payload = JsonObject(
                                                        mapOf(
                                                            "chatId" to JsonPrimitive(chatId),
                                                            "messageId" to JsonPrimitive(messageId)
                                                        )
                                                    )

                                                    coroutineScope.launch {
                                                        SocketManager.sendPacket(
                                                            OPCode.REMOVE_REACTION,
                                                            payload,
                                                            { packet ->
                                                                ChatsManager.processReactions(
                                                                    packet.payload.jsonObject["reactionInfo"]!!.jsonObject,
                                                                    messageId,
                                                                    chatId
                                                                )
                                                            })
                                                    }
                                                } else {
                                                    val payload = JsonObject(
                                                        mapOf(
                                                            "messageId" to JsonPrimitive(messageId),
                                                            "chatId" to JsonPrimitive(chatId),
                                                            "reaction" to JsonObject(
                                                                mapOf(
                                                                    "reactionType" to JsonPrimitive(
                                                                        "EMOJI"
                                                                    ),
                                                                    "id" to JsonPrimitive(reaction.key)
                                                                )
                                                            )
                                                        )
                                                    )

                                                    coroutineScope.launch {
                                                        SocketManager.sendPacket(
                                                            OPCode.ADD_EMOJI,
                                                            payload,
                                                            { packet ->
                                                                ChatsManager.processReactions(
                                                                    packet.payload.jsonObject["reactionInfo"]!!.jsonObject,
                                                                    messageId,
                                                                    chatId
                                                                )
                                                            })
                                                    }
                                                }
                                            })
                                            .background(
                                                color = containerColor,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(4.dp)
                                        ) {
                                            Text("${reaction.key}  ${reaction.value}", modifier = Modifier.padding(0.dp), style = MaterialTheme.typography.titleMedium, color = textColor)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)) {
                                if (message.status == "EDITED") {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edited",
                                        // tint = colorScheme.primary,
                                        modifier = Modifier
                                            .padding(end = 2.dp)
                                            .size((MaterialTheme.typography.bodyMedium.fontSize.value + 4).dp)
                                            .align(Alignment.CenterVertically),
                                        tint = textColor
                                    )
                                }

                                Text(Utils.getTimeString(message.sendTime), color = textColor, style = MaterialTheme.typography.bodyMedium)

                                if (message.senderID == AccountManager.accountID) {
                                    val chats by ChatsManager.chatsList.collectAsState()
                                    val chat = chats[chatId]

                                    var read = false
                                    for (user in chat?.users ?: emptyMap()) {
                                        if (user.value >= message.sendTime && user.key != AccountManager.accountID) {
                                            read = true
                                            break
                                        }
                                    }

                                    Icon(
                                        imageVector = if (read) Icons.Filled.DoneAll else Icons.Filled.Check,
                                        contentDescription = "Read",
                                        // tint = colorScheme.primary,
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .size((MaterialTheme.typography.bodyMedium.fontSize.value + 4).dp)
                                            .align(Alignment.CenterVertically),
                                        tint = textColor
                                    )
                                }
                            }
                        }
                    }

                    if (!message.attaches!!.jsonArray.isNullOrEmpty()) {
                        for (attach in message.attaches.jsonArray) {
                            if (attach.jsonObject.contains("keyboard")) {
                                val buttons = attach.jsonObject["keyboard"]!!.jsonObject["buttons"]!!.jsonArray
                                val callbackID = attach.jsonObject["callbackId"]!!.jsonPrimitive.content
                                var index = 0

                                for (button in buttons) {
                                    val buttonArr = button.jsonArray

                                    for (btn in buttonArr) {
                                        val type = btn.jsonObject["type"]!!.jsonPrimitive.content
                                        val text = btn.jsonObject["text"]!!.jsonPrimitive.content
                                        val clipboardManager = LocalClipboard.current

                                        val bottomPadding = if (index == buttons.size - 1) 16.dp else 8.dp
                                        Box(modifier = Modifier
                                            .padding(top = 4.dp)
                                            .background(
                                                color = colorScheme.secondaryContainer.copy(
                                                    alpha = 0.8f
                                                ),
                                                shape = RoundedCornerShape(
                                                    topStart = 8.dp,
                                                    topEnd = 8.dp,
                                                    bottomStart = bottomPadding,
                                                    bottomEnd = bottomPadding
                                                )
                                            )
                                            .width(with(LocalDensity.current) { width.toDp() })
                                            .heightIn(min = 40.dp)
                                            .padding(start = 4.dp, end = 4.dp)
                                            .clickable {
                                                when (type) {
                                                    "LINK" -> {
                                                        val link =
                                                            btn.jsonObject["url"]!!.jsonPrimitive.content

                                                        if (link.startsWith("https://max.ru/")) {
                                                            coroutineScope.launch {
                                                                parseUri(link.toUri(), context)
                                                            }
                                                        } else {
                                                            val urlIntent = Intent(
                                                                Intent.ACTION_VIEW,
                                                                link.toUri()
                                                            )
                                                            context.startActivity(urlIntent)
                                                        }
                                                    }

                                                    "CLIPBOARD" -> {
                                                        val payload =
                                                            btn.jsonObject["payload"]!!.jsonPrimitive.content

                                                        coroutineScope.launch {
                                                            clipboardManager.setClipEntry(
                                                                ClipEntry(
                                                                    ClipData.newPlainText(
                                                                        payload,
                                                                        payload
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }

                                                    "CALLBACK" -> {
                                                        val payload =
                                                            btn.jsonObject["payload"]!!.jsonPrimitive.content
                                                        val packetPayload = JsonObject(
                                                            mapOf(
                                                                "payload" to JsonPrimitive(payload),
                                                                "type" to JsonPrimitive("CALLBACK"),
                                                                "timestamp" to JsonPrimitive(System.currentTimeMillis()),
                                                                "callbackId" to JsonPrimitive(
                                                                    callbackID
                                                                )
                                                            )
                                                        )
                                                        coroutineScope.launch {
                                                            SocketManager.sendPacket(
                                                                OPCode.BOT_CALLBACK,
                                                                packetPayload,
                                                                {

                                                                })
                                                        }
                                                    }
                                                }
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text, color = colorScheme.onSecondaryContainer)
                                        }
                                    }

                                    index++
                                }

                                break
                            }
                        }
                    }
                }
            }
        }
    }
}


// TODO: Remove repeat code
@Composable
fun DrawImages(attaches: JsonArray, context : Context, chatId : Long, messageId : Long, messageWidth : Int, message : String?) {
    var attachmentSize = 0
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    var width by remember { mutableIntStateOf(screenWidth) }

    for (attach in attaches) {
        if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "PHOTO" || attach.jsonObject["_type"]!!.jsonPrimitive.content == "VIDEO") {
            attachmentSize++
        }
    }

    if (attachmentSize % 2 == 0) {
        Column {
            FlowRow(
                maxItemsInEachRow = 2,
                modifier = Modifier
                    .heightIn(max = 1000.dp)
                    .width((screenWidth * 0.8f).dp)
                    .onSizeChanged() {
                        width = it.width
                    }
//                    .padding(top = 4.dp)
                    ,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (index in 0..attachmentSize - 1) {
                    val photo = attaches[index]
                    val type = photo.jsonObject["_type"]!!.jsonPrimitive.content

                    val bytes: ByteArray? = if (photo.jsonObject.contains("previewData")) {
                        Base64.decode(photo.jsonObject["previewData"]!!.jsonPrimitive.content, Base64.NO_WRAP)
                    } else {
                        null
                    }
                    val placeholder =  if (bytes != null) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes?.size ?: 0)
                    } else {
                        null
                    }

                    val topStart = if (index == 0) 8.dp else 0.dp
                    val topEnd = if (index == 1) 8.dp else 0.dp
                    val bottomStart = if (index == attachmentSize - 2) 8.dp else 0.dp
                    val bottomEnd = if (index == attachmentSize - 1) 8.dp else 0.dp

                    val messageWidthDp = with(LocalDensity.current) {
                        width.toDp()
                    }

                    val itemWidth = (messageWidthDp - 4.dp) / 2

                    if (type == "PHOTO") {
                        AsyncImage(
                            model = photo.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                            contentDescription = "ChatIcon",
                            placeholder = rememberAsyncImagePainter(placeholder),
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = topStart,
                                        topEnd = topEnd,
                                        bottomStart = bottomStart,
                                        bottomEnd = bottomEnd
                                    )
                                )
                                .size(itemWidth)
                                .clickable {
                                    val intent = Intent(context, MediaViewActivity::class.java)

                                    intent.putExtra("isSingleImage", false)
                                    intent.putExtra("chatId", chatId)
                                    intent.putExtra(
                                        "pickedPhoto",
                                        photo.jsonObject["photoToken"]!!.jsonPrimitive.content
                                    )

                                    context.startActivity(intent)
                                },
                            contentScale = ContentScale.Crop,
                        )
                    }

                    if (type == "VIDEO") {
                        Box() {
                            AsyncImage(
                                model = photo.jsonObject["thumbnail"]!!.jsonPrimitive.content,
                                contentDescription = "ChatIcon",
                                placeholder = rememberAsyncImagePainter(placeholder),
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = topStart,
                                            topEnd = topEnd,
                                            bottomStart = bottomStart,
                                            bottomEnd = bottomEnd
                                        )
                                    )
                                    .size(itemWidth)
                                    .clickable {
                                        val token =
                                            photo.jsonObject["token"]!!.jsonPrimitive.content
                                        val videoId =
                                            photo.jsonObject["videoId"]!!.jsonPrimitive.long

                                        val payload = JsonObject(
                                            mapOf(
                                                "messageId" to JsonPrimitive(messageId),
                                                "chatId" to JsonPrimitive(chatId),
                                                "token" to JsonPrimitive(token),
                                                "videoId" to JsonPrimitive(videoId)
                                            )
                                        )

                                        GlobalScope.launch {
                                            SocketManager.sendPacket(
                                                OPCode.GET_VIDEO,
                                                payload,
                                                { packet ->
                                                    if (packet.payload is JsonObject) {
                                                        // TODO: Quality change
                                                        if (packet.payload.contains("MP4_1080")) {
                                                            val url =
                                                                packet.payload["MP4_1080"]!!.jsonPrimitive.content

                                                            val intent = Intent(
                                                                context,
                                                                VideoPlayerActivity::class.java
                                                            )

                                                            intent.putExtra("url", url)

                                                            context.startActivity(intent)
                                                        } else if (packet.payload.contains("MP4_720")) {
                                                            val url =
                                                                packet.payload["MP4_720"]!!.jsonPrimitive.content

                                                            val intent = Intent(
                                                                context,
                                                                VideoPlayerActivity::class.java
                                                            )

                                                            intent.putExtra("url", url)

                                                            context.startActivity(intent)
                                                        } else if (packet.payload.contains("MP4_480")) {
                                                            val url =
                                                                packet.payload["MP4_480"]!!.jsonPrimitive.content

                                                            val intent = Intent(
                                                                context,
                                                                VideoPlayerActivity::class.java
                                                            )

                                                            intent.putExtra("url", url)

                                                            context.startActivity(intent)
                                                        } else if (packet.payload.contains("MP4_360")) {
                                                            val url =
                                                                packet.payload["MP4_360"]!!.jsonPrimitive.content

                                                            val intent = Intent(
                                                                context,
                                                                VideoPlayerActivity::class.java
                                                            )

                                                            intent.putExtra("url", url)

                                                            context.startActivity(intent)
                                                        } else if (packet.payload.contains("MP4_240")) {
                                                            val url =
                                                                packet.payload["MP4_240"]!!.jsonPrimitive.content

                                                            val intent = Intent(
                                                                context,
                                                                VideoPlayerActivity::class.java
                                                            )

                                                            intent.putExtra("url", url)

                                                            context.startActivity(intent)
                                                        } else if (packet.payload.contains("MP4_144")) {
                                                            val url =
                                                                packet.payload["MP4_144"]!!.jsonPrimitive.content

                                                            val intent = Intent(
                                                                context,
                                                                VideoPlayerActivity::class.java
                                                            )

                                                            intent.putExtra("url", url)

                                                            context.startActivity(intent)
                                                        }
                                                    }
                                                })
                                        }
                                    },
                                contentScale = ContentScale.Crop,
                            )

                            Box(modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp, top = 6.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                val time = Utils.getTimeFromMillis(attaches[index].jsonObject["duration"]!!.jsonPrimitive.long)
                                Text(time, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(2.dp)) // , modifier = Modifier.padding(end = 4.dp, top = 4.dp)
                            }
                        }
                    }
                }
            }
        }
    } else if (attachmentSize == 1) {
        val type = attaches.last().jsonObject["_type"]!!.jsonPrimitive.content

        val modifier = if (!message.isNullOrEmpty()) {
            Modifier.width(messageWidth.dp)
        } else {
            Modifier
        }
        val bytes: ByteArray? = if (attaches.last().jsonObject.contains("previewData")) {
            Base64.decode(attaches.last().jsonObject["previewData"]!!.jsonPrimitive.content, Base64.NO_WRAP)
        } else {
            null
        }
        val placeholder =  if (bytes != null) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes?.size ?: 0)
        } else {
            null
        }

        if (type == "PHOTO") {
            AsyncImage(
                model = attaches.last().jsonObject["baseUrl"]!!.jsonPrimitive.content,
                contentDescription = "ChatIcon",
                placeholder = rememberAsyncImagePainter(placeholder),
                modifier = modifier
//                    .padding(top = 4.dp)
                    .clip(
                        RoundedCornerShape(
                            8.dp
                        )
                    )
                    .clickable {
                        val intent = Intent(context, MediaViewActivity::class.java)

                        intent.putExtra("isSingleImage", false)
                        intent.putExtra("chatId", chatId)
                        intent.putExtra(
                            "pickedPhoto",
                            attaches.last().jsonObject["photoToken"]!!.jsonPrimitive.content
                        )

                        context.startActivity(intent)
                    },

                contentScale = ContentScale.Fit
            )
        }

        if (type == "VIDEO") {
            Box() {
                AsyncImage(
                    model = attaches.last().jsonObject["thumbnail"]!!.jsonPrimitive.content,
                    contentDescription = "ChatIcon",
                    placeholder = rememberAsyncImagePainter(placeholder),
                    modifier = modifier
//                        .padding(top = 4.dp)
                        .clip(
                            RoundedCornerShape(
                                8.dp
                            )
                        )
                        .clickable {
                            val token = attaches.last().jsonObject["token"]!!.jsonPrimitive.content
                            val videoId = attaches.last().jsonObject["videoId"]!!.jsonPrimitive.long

                            val payload = JsonObject(
                                mapOf(
                                    "messageId" to JsonPrimitive(messageId),
                                    "chatId" to JsonPrimitive(chatId),
                                    "token" to JsonPrimitive(token),
                                    "videoId" to JsonPrimitive(videoId)
                                )
                            )

                            GlobalScope.launch {
                                SocketManager.sendPacket(OPCode.GET_VIDEO, payload, { packet ->
                                    if (packet.payload is JsonObject) {
                                        if (packet.payload.contains("MP4_1080")) {
                                            val url =
                                                packet.payload["MP4_1080"]!!.jsonPrimitive.content

                                            val intent =
                                                Intent(context, VideoPlayerActivity::class.java)

                                            intent.putExtra("url", url)

                                            context.startActivity(intent)
                                        } else if (packet.payload.contains("MP4_720")) {
                                            val url =
                                                packet.payload["MP4_720"]!!.jsonPrimitive.content

                                            val intent =
                                                Intent(context, VideoPlayerActivity::class.java)

                                            intent.putExtra("url", url)

                                            context.startActivity(intent)
                                        } else if (packet.payload.contains("MP4_480")) {
                                            val url =
                                                packet.payload["MP4_480"]!!.jsonPrimitive.content

                                            val intent =
                                                Intent(context, VideoPlayerActivity::class.java)

                                            intent.putExtra("url", url)

                                            context.startActivity(intent)
                                        } else if (packet.payload.contains("MP4_360")) {
                                            val url =
                                                packet.payload["MP4_360"]!!.jsonPrimitive.content

                                            val intent =
                                                Intent(context, VideoPlayerActivity::class.java)

                                            intent.putExtra("url", url)

                                            context.startActivity(intent)
                                        } else if (packet.payload.contains("MP4_240")) {
                                            val url =
                                                packet.payload["MP4_240"]!!.jsonPrimitive.content

                                            val intent =
                                                Intent(context, VideoPlayerActivity::class.java)

                                            intent.putExtra("url", url)

                                            context.startActivity(intent)
                                        } else if (packet.payload.contains("MP4_144")) {
                                            val url =
                                                packet.payload["MP4_144"]!!.jsonPrimitive.content

                                            val intent =
                                                Intent(context, VideoPlayerActivity::class.java)

                                            intent.putExtra("url", url)

                                            context.startActivity(intent)
                                        }
                                    }
                                })
                            }
                        },
                    contentScale = ContentScale.Fit,
                )

                Box(modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 6.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                ) {
                    val time = Utils.getTimeFromMillis(attaches.last().jsonObject["duration"]!!.jsonPrimitive.long)
                        Text(time, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(2.dp)) // , modifier = Modifier.padding(end = 4.dp, top = 4.dp)
                }
            }
        }
    } else {
        Column {
            FlowRow(
                maxItemsInEachRow = 2,
                modifier = Modifier
                    .heightIn(max = 1000.dp)
                    .width((screenWidth * 0.8f).dp)
                    .onSizeChanged() {
                        width = it.width
                    }
//                    .padding(top = 4.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (index in 0..attachmentSize - 2) {

                    val photo = attaches[index]
                    val type = photo.jsonObject["_type"]!!.jsonPrimitive.content

                    val bytes: ByteArray? = if (photo.jsonObject.contains("previewData")) {
                        Base64.decode(photo.jsonObject["previewData"]!!.jsonPrimitive.content, Base64.NO_WRAP)
                    } else {
                        null
                    }
                    val placeholder =  if (bytes != null) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes?.size ?: 0)
                    } else {
                        null
                    }

                    val topStart = if (index == 0) 8.dp else 0.dp
                    val topEnd = if (index == 1) 8.dp else 0.dp

                    val messageWidthDp = with(LocalDensity.current) {
                        width.toDp()
                    }

                    val itemWidth = (messageWidthDp - 4.dp) / 2
                    if (type == "PHOTO") {
                        AsyncImage(
                            model = photo.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                            contentDescription = "ChatIcon",
                            placeholder = rememberAsyncImagePainter(placeholder),
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = topStart,
                                        topEnd = topEnd
                                    )
                                )
                                .size(itemWidth)
                                .clickable {
                                    val intent = Intent(context, MediaViewActivity::class.java)

                                    intent.putExtra("isSingleImage", false)
                                    intent.putExtra("chatId", chatId)
                                    intent.putExtra(
                                        "pickedPhoto",
                                        photo.jsonObject["photoToken"]!!.jsonPrimitive.content
                                    )

                                    context.startActivity(intent)
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (type == "VIDEO") {
                        AsyncImage(
                            model = photo.jsonObject["thumbnail"]!!.jsonPrimitive.content,
                            contentDescription = "ChatIcon",
                            placeholder = rememberAsyncImagePainter(placeholder),
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = topStart,
                                        topEnd = topEnd,
                                    )
                                )
                                .size(itemWidth)
                                .clickable {
                                    val token = photo.jsonObject["token"]!!.jsonPrimitive.content
                                    val videoId = photo.jsonObject["videoId"]!!.jsonPrimitive.long

                                    val payload = JsonObject(
                                        mapOf(
                                            "messageId" to JsonPrimitive(messageId),
                                            "chatId" to JsonPrimitive(chatId),
                                            "token" to JsonPrimitive(token),
                                            "videoId" to JsonPrimitive(videoId)
                                        )
                                    )

                                    GlobalScope.launch {
                                        SocketManager.sendPacket(
                                            OPCode.GET_VIDEO,
                                            payload,
                                            { packet ->
                                                if (packet.payload is JsonObject) {
                                                    if (packet.payload.contains("MP4_360")) {
                                                        val url =
                                                            packet.payload["MP4_360"]!!.jsonPrimitive.content

                                                        val intent = Intent(
                                                            context,
                                                            VideoPlayerActivity::class.java
                                                        )

                                                        intent.putExtra("url", url)

                                                        context.startActivity(intent)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            val lastAttach = attaches.last()
            val type = lastAttach.jsonObject["_type"]!!.jsonPrimitive.content

            val bytes: ByteArray? = if (lastAttach.jsonObject.contains("previewData")) {
                Base64.decode(lastAttach.jsonObject["previewData"]!!.jsonPrimitive.content, Base64.NO_WRAP)
            } else {
                null
            }
            val placeholder =  if (bytes != null) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes?.size ?: 0)
            } else {
                null
            }

            if (type == "PHOTO") {
                AsyncImage(
                    model = lastAttach.jsonObject["baseUrl"]!!.jsonPrimitive.content,
                    contentDescription = "ChatIcon",
                    placeholder = rememberAsyncImagePainter(placeholder),
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                bottomStart = 8.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(context, MediaViewActivity::class.java)

                            intent.putExtra("isSingleImage", false)
                            intent.putExtra("chatId", chatId)
                            intent.putExtra(
                                "pickedPhoto",
                                lastAttach.jsonObject["photoToken"]!!.jsonPrimitive.content
                            )

                            context.startActivity(intent)
                        },
                    contentScale = ContentScale.Crop
                )
            }

            if (type == "VIDEO") {
                Box() {
                    AsyncImage(
                        model = lastAttach.jsonObject["thumbnail"]!!.jsonPrimitive.content,
                        contentDescription = "ChatIcon",
                        placeholder = rememberAsyncImagePainter(placeholder),
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 8.dp,
                                    bottomEnd = 8.dp
                                )
                            )
                            .padding(top = 4.dp)
                            .clickable {
                                val token = lastAttach.jsonObject["token"]!!.jsonPrimitive.content
                                val videoId = lastAttach.jsonObject["videoId"]!!.jsonPrimitive.long

                                val payload = JsonObject(
                                    mapOf(
                                        "messageId" to JsonPrimitive(messageId),
                                        "chatId" to JsonPrimitive(chatId),
                                        "token" to JsonPrimitive(token),
                                        "videoId" to JsonPrimitive(videoId)
                                    )
                                )

                                GlobalScope.launch {
                                    SocketManager.sendPacket(
                                        OPCode.GET_VIDEO,
                                        payload,
                                        { packet ->
                                            if (packet.payload is JsonObject) {
                                                if (packet.payload.contains("MP4_1080")) {
                                                    val url =
                                                        packet.payload["MP4_1080"]!!.jsonPrimitive.content

                                                    val intent = Intent(
                                                        context,
                                                        VideoPlayerActivity::class.java
                                                    )

                                                    intent.putExtra("url", url)

                                                    context.startActivity(intent)
                                                } else if (packet.payload.contains("MP4_720")) {
                                                    val url =
                                                        packet.payload["MP4_720"]!!.jsonPrimitive.content

                                                    val intent = Intent(
                                                        context,
                                                        VideoPlayerActivity::class.java
                                                    )

                                                    intent.putExtra("url", url)

                                                    context.startActivity(intent)
                                                } else if (packet.payload.contains("MP4_480")) {
                                                    val url =
                                                        packet.payload["MP4_480"]!!.jsonPrimitive.content

                                                    val intent = Intent(
                                                        context,
                                                        VideoPlayerActivity::class.java
                                                    )

                                                    intent.putExtra("url", url)

                                                    context.startActivity(intent)
                                                } else if (packet.payload.contains("MP4_360")) {
                                                    val url =
                                                        packet.payload["MP4_360"]!!.jsonPrimitive.content

                                                    val intent = Intent(
                                                        context,
                                                        VideoPlayerActivity::class.java
                                                    )

                                                    intent.putExtra("url", url)

                                                    context.startActivity(intent)
                                                } else if (packet.payload.contains("MP4_240")) {
                                                    val url =
                                                        packet.payload["MP4_240"]!!.jsonPrimitive.content

                                                    val intent = Intent(
                                                        context,
                                                        VideoPlayerActivity::class.java
                                                    )

                                                    intent.putExtra("url", url)

                                                    context.startActivity(intent)
                                                } else if (packet.payload.contains("MP4_144")) {
                                                    val url =
                                                        packet.payload["MP4_144"]!!.jsonPrimitive.content

                                                    val intent = Intent(
                                                        context,
                                                        VideoPlayerActivity::class.java
                                                    )

                                                    intent.putExtra("url", url)

                                                    context.startActivity(intent)
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                        contentScale = ContentScale.Crop,
                    )

                    Box(modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 6.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                    ) {
                        val time = Utils.getTimeFromMillis(attaches.last().jsonObject["duration"]!!.jsonPrimitive.long)
                        Text(time, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(2.dp)) // , modifier = Modifier.padding(end = 4.dp, top = 4.dp)
                    }
                }
            }
        }
    }
}