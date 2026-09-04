package com.invdiv.voidclient

import android.app.Activity
import android.content.ClipData
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

class ChatInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val chatID = intent.getLongExtra("chatID", 0L)
        val userID = intent.getLongExtra("userID", 0L)

        setContent {
            VoidclientTheme() {
                UiChatInfo(chatID, userID)
            }
        }
    }
}

@Composable
fun UiChatInfo(chatID : Long, userID : Long) {
    val chats by ChatsManager.chatsList.collectAsState()
    val currentChat by remember { mutableStateOf(chats[chatID]) }

    val users by UsersManager.usersList.collectAsState()
    val user = if (userID != 0L) {
        UsersManager.checkForExisting(userID)

        users[userID]
    } else {
        User()
    }

    val chatType = currentChat!!.type

    val chatTitle = if (userID != 0L) {
        Utils.getFullName(user)
    } else {
        currentChat?.title.toString()
    }

    val chatIcon = if (userID != 0L) {
        user?.avatarUrl ?: ""
    } else {
        currentChat?.avatarUrl.toString()
    }

    val context = LocalContext.current

    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .background(colorScheme.background)
        .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { (context as Activity).finish() },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Вернуться в меню",
                        tint = colorScheme.onBackground
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Utils.AvatarFromName(chatTitle, chatIcon, 100, modifier = Modifier.align(Alignment.CenterHorizontally).clickable {
                    if (!chatIcon.isEmpty()) {
                        val intent = Intent(context, MediaViewActivity::class.java)

                        intent.putExtra("isSingleImage", true)
                        intent.putExtra("image", chatIcon)

                        context.startActivity(intent)
                    }
                })

                Text(chatTitle, style = MaterialTheme.typography.titleLarge, color = colorScheme.onBackground, modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                if (userID != 0L) {
                    Text(Utils.getStatusString(user?.lastSeen ?: Pair(0L, 0)), style = MaterialTheme.typography.titleMedium, color = colorScheme.onBackground, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Text(if (chatType == "CHANNEL") "${currentChat!!.usersCount} ${
                        Utils.formatMembersCount(
                            currentChat!!.usersCount ?: 1,
                            1
                        )
                    }" else "${currentChat!!.usersCount} ${
                        Utils.formatMembersCount(
                            currentChat!!.usersCount ?: 1,
                            0
                        )
                    }" , style = MaterialTheme.typography.titleMedium, color = colorScheme.onBackground, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                if (userID != 0L) {
                    Box(modifier = Modifier
                        .background(color = colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp))
                        .width(80.dp)
                        .padding(top = 4.dp)
                        .clickable {
                            val userChatId = AccountManager.accountID xor userID

                            val payload = JsonObject(mapOf(
                                "chatIds" to JsonArray(listOf(JsonPrimitive(userChatId))
                            )))

                            runBlocking {
                                SocketManager.sendPacket(OPCode.CHAT_INFO, payload, { packet ->
                                    if (packet.payload is JsonObject) {
                                        GlobalScope.launch {
                                            ChatsManager.processChats(
                                                packet.payload["chats"]!!.jsonArray
                                            )

                                            val intent = Intent(context, ChatActivity::class.java)
                                            intent.putExtra("chatID", userChatId)

                                            context.startActivity(intent)
                                            (context as Activity).finish()
                                        }
                                    }
                                })
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Goto chat",
                                tint = colorScheme.onSecondaryContainer
                            )

                            Text("Чат", color = colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }

        // InviteLink
        if (userID == 0L && !currentChat?.inviteLink.isNullOrEmpty()) {
            item {
                val inviteLink = currentChat?.inviteLink!!

                Column(modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .background(color = colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                    .clickable {
                        coroutineScope.launch {
                            clipboardManager.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        inviteLink,
                                        inviteLink
                                    )
                                )
                            )
                        }
                    }
                ) {
                    Text("Ссылка-приглашение", style = MaterialTheme.typography.titleMedium, color = colorScheme.onPrimaryContainer.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp))
                    Text(inviteLink, style = MaterialTheme.typography.titleMedium, color = colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // Description
        item {
            val desc = if (userID != 0L) {
                if (!user?.description.isNullOrEmpty()) {
                    user.description
                } else {
                    ""
                }
            } else {
                if (!currentChat?.description.isNullOrEmpty()) {
                    currentChat?.description
                } else {
                    ""
                }
            }

            if (desc?.isEmpty() == false) {
                Column(modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .background(color = colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                ) {
                    Text(if (currentChat!!.type == "DIALOG") "О себе" else "Описание", style = MaterialTheme.typography.titleMedium, color = colorScheme.onPrimaryContainer.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp))
                    Text(desc, style = MaterialTheme.typography.titleMedium, color = colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // Users
        if (userID == 0L && currentChat?.type == "CHAT") {
            item {
                Column(modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .background(color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val usersMap = if (!currentChat?.users.isNullOrEmpty()) {
                        currentChat?.users!!
                    } else {
                        emptyMap()
                    }

                    Text("Участники", color = colorScheme.onPrimaryContainer.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp))

                    for (user in usersMap) {
                        UsersManager.checkForExisting(user.key)

                        val userFromMap = users[user.key]
                        val name = Utils.getFullName(userFromMap)
                        val avatarUrl = userFromMap?.avatarUrl

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                            .padding(start = 4.dp, end = 4.dp)
                            .fillMaxWidth()
                            .clickable {
                                val userChatId = user.key

                                val intent = Intent(context, ChatInfoActivity::class.java)

                                intent.putExtra("userID", userChatId)

                                context.startActivity(intent)
                            }
                        ) {
                            Utils.AvatarFromName(name, avatarUrl, 50)

                            Column() {
                                Text(name, color = colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                Text(Utils.getStatusString(userFromMap?.lastSeen ?: Pair(0L, 0)), color = colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}