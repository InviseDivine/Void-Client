package com.invdiv.voidclient

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil3.compose.AsyncImage
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import java.net.URL

class MediaViewActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isSingleImage = intent.getBooleanExtra("isSingleImage", false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        var chatId = 0L
        var url = ""
        var pickedPhoto = ""

        if (isSingleImage) {
            url = intent.getStringExtra("image").toString()
        } else {
            chatId = intent.getLongExtra("chatId", 0L)
            pickedPhoto = intent.getStringExtra("pickedPhoto").toString()
        }

        setContent {
            VoidclientTheme() {
                MediaUi(pickedPhoto, isSingleImage, chatId, window, url)
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun MediaUi(pickedPhoto : String, isSingleImage : Boolean, chatId : Long, window : Window, url : String) {
    val context = LocalContext.current

    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

    var scrolled by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var images: MutableList<Pair<String, Boolean>> = mutableListOf()
    var pagerState = rememberPagerState { images.size }

    var isTopBar by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }

    if (isTopBar) {
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    } else {
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    val metrics = context.resources.displayMetrics

    if (!isSingleImage) {
        val chats by ChatsManager.chatsList.collectAsState()
        val viewedChat = remember { mutableStateOf(chats[chatId]) }
        val messages = remember { mutableStateOf(viewedChat!!.value!!.messages!!.toList().sortedByDescending { it.second.sendTime }) }
        for (message in messages.value) {
            val attaches = message.second.attaches!!.jsonArray

            if (attaches.isNotEmpty()) {
                for (attach in attaches) {
                    if (attach.jsonObject["_type"]?.jsonPrimitive?.content == "PHOTO") {
                        images.add(attach.jsonObject["baseUrl"]!!.jsonPrimitive.content to false)

                        val photoToken = attach.jsonObject["photoToken"]!!.jsonPrimitive.content

                        if (pickedPhoto == photoToken && !scrolled) {
                            coroutineScope.launch {
                                for (img in 0..images.size) {
                                    if (images[img].first == attach.jsonObject["baseUrl"]!!.jsonPrimitive.content && !images[img].second) {
                                        pagerState.scrollToPage(img)
                                        scrolled = true
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (message.second.link!!.message?.attaches is JsonArray
                && message.second.link!!.message?.attaches!!.jsonArray.isNotEmpty()) {
                val attaches = message.second.link?.message?.attaches!!.jsonArray

                for (attach in attaches) {
                    if (attach.jsonObject["_type"]?.jsonPrimitive?.content == "PHOTO") {
                        images.add(attach.jsonObject["baseUrl"]!!.jsonPrimitive.content to true)

                        val photoToken = attach.jsonObject["photoToken"]!!.jsonPrimitive.content

                        // i think its cringe code :(
                        if (pickedPhoto == photoToken && !scrolled) {
                            coroutineScope.launch {
                                for (img in 0..images.size) {
                                    if (images[img].first == attach.jsonObject["baseUrl"]!!.jsonPrimitive.content && images[img].second) {
                                        pagerState.scrollToPage(img)
                                        scrolled = true

                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column (modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .clickable(indication = null, interactionSource = interactionSource) {
            isTopBar = !isTopBar
        }
    ) {
        if (isSingleImage) {
            Box(modifier = Modifier.fillMaxHeight()) {
                AsyncImage(
                    url,
                    "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .zoomable(rememberZoomState()),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                AsyncImage(
                    images[page].first,
                    "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .zoomable(rememberZoomState()),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
    AnimatedVisibility(visible = isTopBar, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.background(Color.Black.copy(
            0.6f
        ))) {
            Column(modifier = Modifier. padding(top = 30.dp, start = 8.dp, end = 8.dp)) {
                Row() {
                    IconButton({
                        (context as Activity).finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBackIos,
                            "",
                            modifier = Modifier.size(25.dp),
                            tint = colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton({
                        expanded = true
                    }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            "",
                            modifier = Modifier.size(25.dp),
                            tint = colorScheme.primary
                        )

                        DropdownMenu(
                            expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(colorScheme.secondaryContainer)
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = "Сохранить изображение", color = colorScheme.onSecondaryContainer)},
                                onClick = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            expanded = false
                                            val filename = "IMG_${System.currentTimeMillis()}.jpg"
                                            val downloadUrl = if (isSingleImage) {
                                                url
                                            } else {
                                                images[pagerState.currentPage].first
                                            }
                                            val imageByteArray = URL(downloadUrl).openStream().readBytes()
                                            val bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)

                                            val contentValues = ContentValues().apply {
                                                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                                                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VoidClient")
                                            }

                                            val resolver = context.contentResolver
                                            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                                            val uri = resolver.insert(collection, contentValues)

                                            uri?.let { imageUri ->
                                                resolver.openOutputStream(imageUri)?.use { outputStream ->
                                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                                }
                                                contentValues.clear()
                                                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                                                resolver.update(imageUri, contentValues, null, null)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Фотография сохранена в галерею", Toast.LENGTH_SHORT).show()
                                                }
                                            } ?: run {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Сохранение отменено", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (!isSingleImage) {
                    Text("${pagerState.currentPage + 1} из ${images.size}",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}