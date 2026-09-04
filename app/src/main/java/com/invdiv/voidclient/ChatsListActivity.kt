package com.invdiv.voidclient

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.invdiv.voidclient.settings.AboutScreen
import com.invdiv.voidclient.settings.DevicesScreen
import com.invdiv.voidclient.settings.EditProfileScreen
import com.invdiv.voidclient.settings.MainSettingsScreen
import com.invdiv.voidclient.settings.SecurityScreen
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long
import java.net.URL

class ChatsListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (SocketManager.socketState.value == 0) {
            val intent = Intent(this, MainActivity::class.java)

            this.startActivity(intent)
            finish()
        }

        val joinLink = intent.getStringExtra("link")

        println(intent.getStringExtra("navigateTo"))
        setContent {
            VoidclientTheme() {
                DrawLink(joinLink)
                UiChatsList()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawLink(link : String?) {
    var draw by remember { mutableStateOf(false) }
    val drawSheet = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(link) {
        if (link != null) {
            draw = true
        }
    }

    if (draw) {
        val fullLink = "https://max.ru/join/$link"

        var chat by remember { mutableStateOf(JsonObject(mapOf())) }

        LaunchedEffect(draw) {
            if (draw) {
                coroutineScope.launch {
                    SocketManager.sendPacket(opcode = OPCode.PREVIEW_JOINLINK, payload = JsonObject(mapOf(
                        "link" to JsonPrimitive(fullLink)
                    )), { packet ->
                        chat = packet.payload.jsonObject["chat"]!!.jsonObject
                    })
                }
            }
        }

        LaunchedEffect(chat) {
            if (chat.containsKey("title")) {
                val id = chat["id"]!!.jsonPrimitive.long

                if (ChatsManager.chatsList.value.contains(id)) {
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("chatID", id)

                    context.startActivity(intent)

                    draw = false
                }
            }
        }
        ModalBottomSheet(
            sheetState = drawSheet,
            onDismissRequest = {
                draw = false
            }
        ) {

            if (chat.containsKey("title")) {
                Text(chat["title"]!!.jsonPrimitive.content, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Utils.AvatarFromName(chat["title"]!!.jsonPrimitive.content, chat["baseRawIconUrl"]?.jsonPrimitive?.content, size = 100)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(onClick = {
                    coroutineScope.launch {
                        SocketManager.sendPacket(OPCode.JOIN_CHAT, JsonObject(mapOf(
                            "link" to JsonPrimitive(fullLink)
                        )), { packet ->
                            coroutineScope.launch {
                                ChatsManager.processChats(packet.payload.jsonObject["chat"]!!.jsonObject)
                            }
                        })
                    }

                    draw = false
                }, modifier = Modifier.fillMaxWidth()   ) {
                    Text("Войти", textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(onClick = {
                    draw = false
                }, modifier = Modifier.fillMaxWidth()   ) {
                    Text("Отмена", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UiChatsList() {
    val bottomEntires = mapOf(
        "Чаты" to Icons.AutoMirrored.Filled.Chat,
//        "Контакты" to Icons.Filled.Contacts,
        "Профиль" to Icons.Filled.Person,
    )

    val activity = LocalActivity.current
    val forwardChatID = activity?.intent?.getLongExtra("forwardChatID", 0L) ?: 0L
    val forwardMessageID = activity?.intent?.getLongExtra("forwardMessageID", 0L) ?: 0L

    val navigateTo = activity?.intent?.getStringExtra("navigateTo")

    val navController = rememberNavController()
    var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
    val state = SocketManager.socketState.collectAsState()

    val reallyExitFromAccount = remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val coroutineScope = rememberCoroutineScope()

    val text = if (forwardMessageID != 0L) {
        "Пересылка"
    } else {
        when (state.value) {
            1 -> "Подключение..."
            2 -> "Void Client"
            3 -> "Ожидание сети..."
            else -> ""
        }
    }

    val context = LocalContext.current

    if (reallyExitFromAccount.value) {
        AlertDialog(
            onDismissRequest = { reallyExitFromAccount.value = false},
            title = { Text(text = "Выйти из аккаунта") },
            text = {
                Text("Вы действительно хотите выйти из аккаунта?")
            },
            confirmButton = {
                Button({
                    reallyExitFromAccount.value = false
                    coroutineScope.launch {
                        SocketManager.sendPacket(OPCode.LOGOUT, JsonObject(mapOf()), { packet ->

                        })
                    }

                    AccountManager.clearDevices()
                    runBlocking {
                        try {
                            context.dataStore.edit { settings ->
                                settings[stringPreferencesKey("token")] = "null"
                            }
                        } catch (e: Exception) {
                            Log.e("AccountManager", "Error while trying to edit token: $e")
                        }
                    }

                    val intent = Intent(
                        context, MainActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

                    context.startActivity(intent)

                    (context as? Activity)?.finish()
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                Button({ reallyExitFromAccount.value = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    LaunchedEffect(currentRoute) {
        var index = 0
        for (route in bottomEntires) {
            if (route.key == currentRoute) {
                selectedDestination = index
                break
            }
            index++
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (currentRoute) {
                        "Чаты" -> {
                            AnimatedContent(
                                targetState = text,
                                transitionSpec = {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                            slideOutVertically { height -> -height } + fadeOut()
                                }, label = "animated content"
                            ) { text ->
                                Text(text, textAlign = TextAlign.Center)
                            }
                        }

                        "Профиль" -> {

                        }
                    }
                },

                navigationIcon = {
                    if (currentRoute == "Профиль") {
                        IconButton(
                            onClick = {
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = "MyQR"
                            )
                        }
                    }

                    if (currentRoute != "Чаты" && currentRoute != "Контакты" && currentRoute != "Профиль") {
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                },

                actions = {
                    when (currentRoute) {
                        "Чаты" -> {
                            IconButton({

                            }) { Icon(Icons.Filled.Add, contentDescription = "Создать группу") }

                            IconButton({

                            }) { Icon(Icons.Filled.Search, contentDescription = "Поиск") }
                        }

                        "Профиль" -> {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton({
                                expanded = true
                            }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    "",
                                    tint = colorScheme.primary
                                )

                                DropdownMenu(
                                    expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(colorScheme.secondaryContainer)
                                ) {
                                    DropdownMenuItem(
                                        text = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                "",
                                                tint = colorScheme.onSecondaryContainer
                                            )
                                            Text(text = "Редактировать", color = colorScheme.onSecondaryContainer)
                                        }},
                                        onClick = {
                                            navController.navigate("Редактирование профиля")
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Logout,
                                                "",
                                                tint = colorScheme.onSecondaryContainer
                                            )
                                            Text(text = "Выход", color = colorScheme.onSecondaryContainer)
                                        }},
                                        onClick = {
                                            reallyExitFromAccount.value = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (currentRoute != "Чаты" && currentRoute != "Контакты" && currentRoute != "Профиль") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(currentRoute ?: "", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleLarge, color = colorScheme.onBackground)
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (forwardMessageID == 0L && (currentRoute == "Чаты" || currentRoute == "Контакты" || currentRoute == "Профиль")) {
                NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                    bottomEntires.entries.forEachIndexed { index, entry ->
                        NavigationBarItem(
                            selected = selectedDestination == index,
                            onClick = {
                                navController.navigate(route = entry.key)
                                selectedDestination = index
                            },
                            icon = {
                                Icon(
                                    entry.value,
                                    contentDescription = entry.key
                                )
                            },
                            label = { Text(entry.key) }
                        )
                    }
                }
            }
        }
    ) {
        // TODO: Scrolling animation
        NavHost(navController, "Чаты", modifier = Modifier.padding(it)) {
            composable(route = "Чаты") {
                DrawChats(forwardChatID, forwardMessageID)
            }

            composable(route = "Контакты") {
                DrawContacts()
            }

            composable("Профиль") {
                MainSettingsScreen(navController)
            }

            composable("Безопасность") {
                SecurityScreen()
            }

            composable("Устройства") {
                DevicesScreen()
            }

            composable("Редактирование профиля") {
                EditProfileScreen()
            }

            composable("О приложении") {
                AboutScreen()
            }
        }
    }

    LaunchedEffect(navigateTo) {
        if (!navigateTo.isNullOrEmpty()) {
            navController.navigate(navigateTo)
        }
    }
}

@Composable
fun DrawContacts() {
    Text("Контакты")
}


@Composable
fun DrawChats(forwardChatID : Long, forwardMessageID : Long) {
    val chatsState = rememberLazyListState()
    val chats by ChatsManager.chatsList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val users by UsersManager.usersList.collectAsState()

    LaunchedEffect(chats) {
        coroutineScope.launch {
            chatsState.scrollToItem(index = chats.size)
        }
    }

    val sortedChats = remember(chats, users) {
        chats.entries
            .sortedBy { (_, value) ->
                value.messages?.values
                    ?.maxByOrNull { it.sendTime }
                    ?.sendTime
            }
    }

    LazyColumn(reverseLayout = true, state = chatsState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sortedChats, key = { entry ->
            entry.key
        }) { entry ->
            if (!entry.value.messages.isNullOrEmpty()) {
                DrawUser(entry.key, entry.value, forwardChatID, forwardMessageID)
            }
        }
    }
}

@Composable
fun DrawUser(chatID: Long, chat: Chat, forwardChatID : Long, forwardMessageID : Long) {
    val context = LocalContext.current
    val users by UsersManager.usersList.collectAsState()

    val sortedMessages = chat.messages?.entries?.toList()?.sortedBy { (_, value) -> value.sendTime }
    val lastMessage = sortedMessages?.last()?.value ?: Message()

    println(lastMessage)

    var lastUser = ""
    var chatIcon : String? = null
    var chatTitle : String
    val activity = LocalActivity.current

    UsersManager.checkForExisting(lastMessage?.senderID!!)
    if (chat.type == "CHAT") {
        if (lastMessage?.senderID == AccountManager.accountID) {
            lastUser = "Вы"
        } else {
            val user = users[lastMessage.senderID]
            lastUser = user?.firstName + " " + user?.lastName
        }
    }

    if (chat.type == "DIALOG" && chatID != 0L) {
        var secondUser = 0L
        for (i in chat.users.toList()) {
            if (i.first != AccountManager.accountID) {
                secondUser = i.first
                break
            }
        }
        UsersManager.checkForExisting(secondUser)

        val user = users[secondUser]

        chatTitle = user?.firstName + " " + user?.lastName
        chatIcon = user?.avatarUrl.toString()
    } else {
        chatTitle = chat.title
        chatIcon = chat.avatarUrl ?: chat.avatarUrl
    }

    Box(modifier = Modifier.padding(start = 8.dp, end = 8.dp).clickable {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("chatID", chatID)
        intent.putExtra("forwardChatID", forwardChatID)
        intent.putExtra("forwardMessageID", forwardMessageID)

        context.startActivity(intent)

        if (forwardMessageID != 0L) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity?.finish()
        }
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (chatID == 0L) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                ) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = "FavoriteIcon",
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.Center),
                        tint = colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Utils.AvatarFromName(chatTitle, chatIcon, 60)
            }

            Column() {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        chatTitle,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(0.45f)
                            .padding(end = 4.dp)
                    )

                    if (lastMessage?.message != null || lastMessage?.attaches != null) {
                        if (lastMessage.senderID == AccountManager.accountID) {
                            var read = false
                            for (user in chat.users) {
                                if (user.value >= lastMessage.sendTime && user.key != AccountManager.accountID) {
                                    read = true
                                    break
                                }
                            }

                            Icon(
                                imageVector = if (read) Icons.Filled.DoneAll else Icons.Filled.Check,
                                contentDescription = "Read",
                                // tint = colorScheme.primary,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        Text(
                            text = Utils.getTimeString(lastMessage.sendTime, false), modifier = Modifier.alpha(0.7f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (!lastMessage?.attaches?.jsonArray.isNullOrEmpty() && lastMessage.attaches.jsonArray.last().jsonObject.contains("event")) {
                        Text(
                            text = Utils.getEventString(lastMessage, chat.type),
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.7f),
                            // color = colorScheme.primary
                        )
                    } else {
                        // TODO: Remove repeat code
                        UsersManager.checkForExisting(lastMessage!!.senderID!!)
                        if (chat.type == "CHAT") {
                            lastUser += ": "
                        }

                        val clearedMessage = if (lastMessage?.link?.type == "FORWARD") {
                            lastMessage.link.message?.message?.replace("\n", " ")
                        } else {
                            lastMessage?.message?.replace("\n", " ")
                        }

                        val annotatedString = buildAnnotatedString {
                            append(lastUser)

                            if (lastMessage?.attaches?.jsonArray?.isNotEmpty() ?: false) {
                                lastMessage.attaches.jsonArray.forEachIndexed { index, jsonelement ->
                                    val type =
                                        jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                    if (type == "PHOTO") {
                                        val imageId = "image_$index"
                                        appendInlineContent(id = imageId)
                                        append(" ")
                                    }

                                    if (type == "VIDEO") {
                                        val imageId = "video_$index"
                                        appendInlineContent(id = imageId)
                                        append(" ")
                                    }
                                }
                            }

                            if (!lastMessage?.link?.message?.attaches?.jsonArray.isNullOrEmpty() && lastMessage.link.type == "FORWARD") {
                                lastMessage.link.message.attaches.jsonArray.forEachIndexed { index, jsonelement ->
                                    val type =
                                        jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                    if (type == "PHOTO") {
                                        val imageId = "image_$index"
                                        appendInlineContent(id = imageId)
                                        append(" ")
                                    }

                                    if (type == "VIDEO") {
                                        val imageId = "video_$index"
                                        appendInlineContent(id = imageId)
                                        append(" ")
                                    }
                                }
                            }

                            if (!clearedMessage.isNullOrEmpty()) {
                                append(clearedMessage)
                            } else {
                                if (lastMessage?.attaches?.jsonArray?.isNotEmpty() == true) {
                                    val lastAttach = lastMessage.attaches.jsonArray.last().jsonObject
                                    val type = lastAttach["_type"]!!.jsonPrimitive.content

                                    if (type == "STICKER") {
                                        appendInlineContent(id = "sticker")
                                        append("Стикер")
                                    } else if (type == "FILE") {
                                        appendInlineContent(id = "fileIcon")
                                        append(lastAttach["name"]!!.jsonPrimitive.content)
                                    } else {
                                        var text = ""
                                        var photoCount = 0
                                        var videoCount = 0

                                        for (attach in lastMessage?.attaches?.jsonArray!!) {
                                            if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "VIDEO") {
                                                videoCount++
                                            }

                                            if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "PHOTO") {
                                                photoCount++
                                            }
                                        }

                                        if (photoCount > 0) {
                                            text += if (photoCount == 1) "Фото" else "$photoCount фото"
                                        }

                                        if (videoCount > 0) {
                                            text += if (photoCount > 0) {
                                                if (videoCount == 1) " и видео" else " и $videoCount видео"
                                            } else {
                                                if (videoCount == 1) "Видео" else "$videoCount видео"
                                            }
                                        }

                                        append(text)
                                    }
                                }

                                if (lastMessage.link!!.type == "FORWARD" && !lastMessage?.link?.message?.attaches?.jsonArray.isNullOrEmpty()) {
                                    val lastAttach = lastMessage?.link?.message?.attaches?.jsonArray!!.last().jsonObject
                                    val type = lastAttach["_type"]!!.jsonPrimitive.content

                                    when (type) {
                                        "STICKER" -> {
                                            appendInlineContent(id = "sticker")
                                            append("Стикер")
                                        }
                                        "FILE" -> {
                                            appendInlineContent(id = "fileIcon")
                                            append(lastAttach["name"]!!.jsonPrimitive.content)
                                        }
                                        else -> {
                                            var text = ""
                                            var photoCount = 0
                                            var videoCount = 0

                                            for (attach in lastMessage?.link?.message?.attaches?.jsonArray!!) {
                                                if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "VIDEO") {
                                                    videoCount++
                                                }

                                                if (attach.jsonObject["_type"]!!.jsonPrimitive.content == "PHOTO") {
                                                    photoCount++
                                                }
                                            }

                                            if (photoCount > 0) {
                                                text += if (photoCount == 1) "Фото" else "$photoCount фото"
                                            }

                                            if (videoCount > 0) {
                                                text += if (photoCount > 0) {
                                                    if (videoCount == 1) " и видео" else " и $videoCount видео"
                                                } else {
                                                    if (videoCount == 1) "Видео" else "$videoCount видео"
                                                }
                                            }

                                            append(text)
                                        }
                                    }
                                }
                            }

                            if (lastMessage?.link?.type == "FORWARD") {
                                appendInlineContent(id = "forwardIcon")
                            }
                        }

                        val inlineContentMap = mutableMapOf<String, InlineTextContent>(

                        )

                        val placeholder = Placeholder(
                            width = 25.sp,
                            height = 25.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )

                        if (lastMessage?.attaches?.jsonArray?.isNotEmpty() == true) {
                            lastMessage.attaches.jsonArray?.forEachIndexed { index, jsonelement ->
                                val type = jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                if (type == "STICKER") {
                                    val photoUrl = jsonelement.jsonObject["url"]!!.jsonPrimitive.content
                                    val imageId = "sticker"

                                    inlineContentMap[imageId] =
                                        InlineTextContent(placeholder) { _ ->
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "Sticker",
                                                modifier = Modifier
                                                    .size(25.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                }

                                if (type == "FILE") {
                                    val imageId = "fileIcon"

                                    inlineContentMap[imageId] =
                                        InlineTextContent(placeholder) { _ ->
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                                contentDescription = "forwardIcon",
                                                tint = colorScheme.onBackground
                                            )
                                        }
                                }

                                if (type == "PHOTO") {
                                    val photoUrl = jsonelement.jsonObject["baseUrl"]!!.jsonPrimitive.content
                                    val imageId = "image_$index"

                                    inlineContentMap[imageId] =
                                    InlineTextContent(placeholder) { _ ->
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = "MessagePhoto",
                                            modifier = Modifier
                                                .size(25.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                if (type == "VIDEO") {
                                    val photoUrl = jsonelement.jsonObject["thumbnail"]!!.jsonPrimitive.content
                                    val videoId = "video_$index"

                                    inlineContentMap[videoId] =
                                    InlineTextContent(placeholder) { _ ->
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = "VideoThumb",
                                            modifier = Modifier
                                                .size(25.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        if (lastMessage.link!!.type == "FORWARD" && !lastMessage?.link?.message?.attaches?.jsonArray.isNullOrEmpty()) {
                            lastMessage?.link?.message?.attaches?.jsonArray?.forEachIndexed { index, jsonelement ->
                                val type = jsonelement.jsonObject["_type"]!!.jsonPrimitive.content

                                if (type == "STICKER") {
                                    val photoUrl = jsonelement.jsonObject["url"]!!.jsonPrimitive.content
                                    val imageId = "sticker"

                                    inlineContentMap[imageId] =
                                        InlineTextContent(placeholder) { _ ->
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "Sticker",
                                                modifier = Modifier
                                                    .size(25.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                }

                                if (type == "FILE") {
                                    val imageId = "fileIcon"

                                    inlineContentMap[imageId] =
                                        InlineTextContent(placeholder) { _ ->
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                                contentDescription = "forwardIcon",
                                                tint = colorScheme.onBackground
                                            )
                                        }
                                }

                                if (type == "PHOTO") {
                                    val photoUrl = jsonelement.jsonObject["baseUrl"]!!.jsonPrimitive.content
                                    val imageId = "image_$index"

                                    inlineContentMap[imageId] =
                                        InlineTextContent(placeholder) { _ ->
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "MessagePhoto",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                }

                                if (type == "VIDEO") {
                                    val photoUrl = jsonelement.jsonObject["thumbnail"]!!.jsonPrimitive.content
                                    val videoId = "video_$index"

                                    inlineContentMap[videoId] =
                                        InlineTextContent(placeholder) { _ ->
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "VideoThumb",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                }
                            }
                        }

                        if (lastMessage?.link?.type == "FORWARD") {
                            inlineContentMap["forwardIcon"] = InlineTextContent(placeholder) { _ ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "forwardIcon",
                                    tint = colorScheme.onBackground
                                )
                            }
                        }

                        Text(
                            text = annotatedString,
                            inlineContent = inlineContentMap,
                            modifier = Modifier.alpha(0.7f).weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}