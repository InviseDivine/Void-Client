package com.sffteam.openmax

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.openmax.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale.getDefault

class ChatListActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent @OptIn(ExperimentalMaterial3Api::class) {
            AppTheme {
                DrawChatList()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawChatList() {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Open MAX",
                    fontSize = 22.sp,
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
                IconButton({ }) { Icon(Icons.Filled.Add, contentDescription = "Добавить чат") }
                IconButton({ }) { Icon(Icons.Filled.Search, contentDescription = "Поиск") }
            }
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
                DrawUser(entry.key, entry.value, LocalContext.current)
            }
        }
    }
}

@Composable
fun DrawUser(chatID: Long, chat: Chat, context: Context) {
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
            containerColor = Color.DarkGray,
            contentColor = Color.White
        ),
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chatTitle,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val annotatedString = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontSize = 20.sp)) {
                            append(lastMsgUser)
                        }
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
                        withStyle(style = SpanStyle(fontSize = 20.sp)) {
                            append(lastMSGCleared)
                        }
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
                        inlineContent = inlineContentMap,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}