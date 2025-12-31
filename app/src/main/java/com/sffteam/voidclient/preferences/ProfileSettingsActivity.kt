package com.sffteam.voidclient.preferences

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.voidclient.AccountManager
import com.sffteam.voidclient.OPCode
import com.sffteam.voidclient.SocketManager
import com.sffteam.voidclient.UserManager
import com.sffteam.voidclient.Utils
import com.sffteam.voidclient.ui.theme.AppTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.Locale.getDefault

class ProfileSettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surfaceContainer,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            title = {
                                Text("Профиль")
                            },
                            navigationIcon = {
                                IconButton({ finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBackIos,
                                        contentDescription = "Меню"
                                    )
                                }
                            },
                        )
                    }) {
                    Column(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        val users by UserManager.usersList.collectAsState()
                        val you = users[AccountManager.accountID]
                        val firstName = remember { mutableStateOf(you!!.firstName) }
                        val lastName = remember { mutableStateOf(you?.lastName ?: "") }
                        val desc = remember { mutableStateOf(you?.description) }
                        val context = LocalContext.current
                        var selectedImages by remember {
                            mutableStateOf<List<Uri?>>(emptyList())
                        }

                        val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.PickVisualMedia(),
                            onResult = { uri ->
                                println("uris $uri")
                                selectedImages = listOf(uri)
                            })

                        Box(
                            modifier = Modifier.clickable {
                                    singlePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }, contentAlignment = Alignment.Center
                        ) {
                            if (you?.avatarUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = you.avatarUrl,
                                    contentDescription = "ChatIcon",
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(100.dp)
                                        .clip(CircleShape)
                                        .align(Alignment.Center),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                val fullName = you!!.firstName + you.lastName
                                val initial =
                                    fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2)
                                        .joinToString("").uppercase(getDefault())

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(100.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Utils.getColorForAvatar(fullName).first,
                                                    Utils.getColorForAvatar(fullName).second
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

                            Box(
                                modifier = Modifier
                                    .background(
                                        colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = "Меню",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = firstName.value,
                            onValueChange = { newText ->
                                if (newText.length <= 59) {
                                    firstName.value = newText
                                }
                            },
                            label = { Text("Имя") },
                            textStyle = TextStyle(fontSize = 25.sp),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        OutlinedTextField(
                            value = lastName.value,
                            onValueChange = { newText ->
                                if (newText.length <= 59) {
                                    lastName.value = newText
                                }
                            },
                            label = { Text("Фамилия") },
                            textStyle = TextStyle(fontSize = 25.sp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        OutlinedTextField(
                            value = desc.value.toString(),
                            onValueChange = { newText ->
                                if (newText.length <= 400) {
                                    desc.value = newText
                                }
                            },
                            label = { Text("О себе") },
                            textStyle = TextStyle(fontSize = 25.sp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Button(
                            onClick = {
                                if (selectedImages.isNotEmpty()) {
                                    println("mr")
                                    var uploadedImages = mapOf<String, JsonElement>()

                                    var imageType = ""
                                    var imageName = ""
                                    val cursor = context.contentResolver.query(
                                        selectedImages.last()!!, null, null, null, null
                                    )
                                    cursor?.use {
                                        if (it.moveToFirst()) {
                                            val nameIndex =
                                                it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                                            imageName = it.getString(nameIndex)
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
                                        println("pen")
                                        val imageBytes = try {
                                            context.contentResolver.openInputStream(selectedImages.last()!!)
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
                                                                        "X-Uploading-Mode",
                                                                        "parallel"
                                                                    )
                                                                    append(
                                                                        "Content-Range",
                                                                        "bytes 0-${imageBytes!!.size - 1}/${imageBytes.size}"
                                                                    )
                                                                    append(
                                                                        HttpHeaders.Connection,
                                                                        "keep-alive"
                                                                    )
                                                                    append(
                                                                        HttpHeaders.AcceptEncoding,
                                                                        "gzip"
                                                                    )
                                                                }

                                                                setBody(imageBytes)
                                                            }

                                                        println(response.request.content)
                                                        println("Upload response status: ${response.status}")
                                                        val content =
                                                            Json.parseToJsonElement(response.bodyAsText())

                                                        uploadedImages =
                                                            content.jsonObject["photos"]!!.jsonObject

                                                        print(content)


                                                        println("is")
                                                        var packetJson = mutableMapOf(
                                                            "firstName" to JsonPrimitive(firstName.value),
                                                            "lastName" to JsonPrimitive(lastName.value),
                                                        )
                                                        packetJson["description"] =
                                                            JsonPrimitive(desc.value)

                                                        packetJson["avatarType"] =
                                                            JsonPrimitive("USER_AVATAR")
                                                        packetJson["photoToken"] = JsonPrimitive(
                                                            uploadedImages.toList()
                                                                .last().second.jsonObject["token"]!!.jsonPrimitive.content
                                                        )

                                                        val packet = SocketManager.packPacket(
                                                            OPCode.CHANGE_PROFILE.opcode,
                                                            JsonObject(packetJson)
                                                        )

                                                        println("gay")
                                                        GlobalScope.launch {
                                                            SocketManager.sendPacket(
                                                                packet, { packet ->
                                                                    if (packet.payload is JsonObject) {
                                                                        AccountManager.accountID =
                                                                            packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["id"]!!.jsonPrimitive.long

                                                                        AccountManager.phone =
                                                                            packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["phone"]!!.jsonPrimitive.content
                                                                    }
                                                                })
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    } finally {
                                                        client.close()
                                                    }
                                                }
                                            }
                                        })
                                    }
                                } else {
                                    var packetJson = mutableMapOf(
                                        "firstName" to JsonPrimitive(firstName.value),
                                        "lastName" to JsonPrimitive(lastName.value),
                                    )
                                    packetJson["description"] = JsonPrimitive(desc.value)

                                    val packet = SocketManager.packPacket(
                                        OPCode.CHANGE_PROFILE.opcode, JsonObject(packetJson)
                                    )

                                    GlobalScope.launch {
                                        SocketManager.sendPacket(packet, { packet ->
                                            if (packet.payload is JsonObject) {
                                                AccountManager.accountID =
                                                    packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["id"]!!.jsonPrimitive.long

                                                AccountManager.phone =
                                                    packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["phone"]!!.jsonPrimitive.content
                                            }
                                        })
                                    }
                                }
                            }) {
                            Text("Сохранить", fontSize = 18.sp)
                        }
                        Button(
                            onClick = {
                                // TODO: Clear chats and users
                                val packet = SocketManager.packPacket(
                                    OPCode.LOGOUT.opcode, JsonObject(mapOf())
                                )

                                coroutineScope.launch {
                                    SocketManager.sendPacket(packet, {

                                    })
                                }

                            }) {
                            Text("Выйти из профиля", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}