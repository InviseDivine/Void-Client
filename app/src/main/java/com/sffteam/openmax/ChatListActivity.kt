package com.sffteam.openmax

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.RectangleShape
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
import com.sffteam.openmax.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.Locale.getDefault
import kotlin.collections.contains
import kotlin.collections.get
import kotlin.collections.iterator

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

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun DrawChatList() {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }

    if (showPopup) {
        var inputText by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf("") }

        AlertDialog(
            title = {
                Text(text = "Войти в группу")
            },
            text = {
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

            },
            onDismissRequest = {
                showPopup = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val urlJoin = inputText.replace("https://max.ru/", "")
                        val packetSend = SocketManager.packPacket(OPCode.JOIN_CHAT.opcode, JsonObject(mapOf(
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
                                }
                            )
                        }


                    }
                ) {
                    Text("Войти", fontSize = 20.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPopup = false
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
                Row(modifier = Modifier.fillMaxWidth().clickable {
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
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
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

        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.surfaceDim,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            title = {
                Text(
                    "Open MAX",
                    fontSize = titleSize,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton({ }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Меню"
                    )
                }
            },
            actions = {
                IconButton({ showBottomSheet = true }) { Icon(Icons.Filled.Add, contentDescription = "Добавить чат") }
                IconButton({ }) { Icon(Icons.Filled.Search, contentDescription = "Поиск") }
            },
            modifier = Modifier.heightIn(max = 200.dp)

        )

        val chats by ChatManager.chatsList.collectAsState()

        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(chats) {
            coroutineScope.launch {
                listState.scrollToItem(index = chats.size)
            }
        }

        LazyColumn(reverseLayout = true, state = listState) {
            items(
                chats.entries.toList()
                    .sortedBy {  (_, value) ->
                        value.messages.entries.toList()
                            .maxByOrNull { (_, value) -> value.sendTime }!!.value.sendTime }, key = { entry ->
                                entry.key
                            }
            ) { entry ->
                println(entry)
                DrawUser(entry.key, entry.value, LocalContext.current, Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
fun DrawUser(chatID: Long, chat: Chat, context: Context, modifier : Modifier) {
    var chatTitle: String
    var chatIcon: String
    val users = UserManager.usersList.collectAsState()
    var secondUser = 0L
    val sortedMessages = chat.messages.entries.toList()
        .sortedBy { (_, value) -> value.sendTime }

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

    Button(
        shape = RectangleShape,
        onClick = {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("chatTitle", chatTitle)
            intent.putExtra("chatIcon", chatIcon)
            intent.putExtra("chatID", chatID)
            intent.putExtra("messageTime", chat.messages.toList().last().second.sendTime)
            intent.putExtra("chatType", chat.type)

            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        modifier = modifier
            .height(80.dp)
            .fillMaxWidth()
    ) {
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
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            if (chatIcon.isNotEmpty()) {
                AsyncImage(
                    model = chatIcon,
                    contentDescription = "ChatIcon",
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(CircleShape)
                )
            } else {
                val initial = chatTitle.split(" ").mapNotNull { it.firstOrNull() }
                    .take(2)
                    .joinToString("")
                    .uppercase(getDefault())

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient( // Create a vertical gradient
                                colors = listOf(
                                    Utils.getColorForAvatar(chatTitle).first,
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

            var lastMsgUser = ""

            if (chat.type == "CHAT") {
                if (sortedMessages.last().value.senderID == AccountManager.accountID) {
                    lastMsgUser = "Вы: "
                } else {
                    lastMsgUser = users.value[sortedMessages.last().value.senderID]?.firstName.toString()

                    if (users.value[sortedMessages.last().value.senderID]?.lastName?.isNotEmpty() ?: false
                    ) {
                        lastMsgUser += " " + users.value[sortedMessages.last().value.senderID]?.lastName
                    }

                    lastMsgUser += ": "
                }
            }
            val lastMSGCleared = if(sortedMessages.last().value.link.type.isNotEmpty()) {
                sortedMessages.last().value.link.msgForLink.message.replace("\n", " ")
            } else {
                sortedMessages.last().value.message.replace("\n", " ")
            }

            Column() {
                Text(
                    chatTitle,
                    fontSize = fontTitleSize,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val annotatedString = buildAnnotatedString {
                            append(lastMsgUser)

                        if (sortedMessages.last().value.attaches?.jsonArray?.isNotEmpty()
                                ?: false
                        ) {
                            sortedMessages.last().value.attaches?.jsonArray?.forEachIndexed { index, jsonelement ->
                                val type = jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                if (type == "PHOTO") {
                                    val imageId = "image_$index"
                                    appendInlineContent(id = imageId)
                                    append(" ")
                                }
                            }
                        }
                            append(lastMSGCleared)

                    }

                    val inlineContentMap = mutableMapOf<String, InlineTextContent>(

                    )
                    val placeholder = Placeholder(
                        width = 25.sp,
                        height = 25.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )

                    if (sortedMessages.last().value.attaches?.jsonArray?.isNotEmpty() ?: false) {
                        sortedMessages.last().value.attaches?.jsonArray?.forEachIndexed { index, jsonelement ->
                            val type = jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                            if (type == "PHOTO") {
                                val imageId = "image_$index"
                                inlineContentMap[imageId] = InlineTextContent(placeholder) { _ ->
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

                    Text(
                        text = annotatedString,
                        fontSize = fontSize,
                        inlineContent = inlineContentMap,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }
        }
    }
}