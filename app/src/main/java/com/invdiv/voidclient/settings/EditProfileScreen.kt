package com.invdiv.voidclient.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.invdiv.voidclient.AccountManager
import com.invdiv.voidclient.ChatsListActivity
import com.invdiv.voidclient.MainActivity
import com.invdiv.voidclient.OPCode
import com.invdiv.voidclient.SocketManager
import com.invdiv.voidclient.UsersManager
import com.invdiv.voidclient.Utils
import com.invdiv.voidclient.dataStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException

@Composable
fun EditProfileScreen() {
    val coroutineScope = rememberCoroutineScope()
    val users by UsersManager.usersList.collectAsState()
    val user = users[AccountManager.accountID]

    val context = LocalContext.current
    val firstName = remember { mutableStateOf(user?.firstName ?: "") }
    val secondName = remember { mutableStateOf(user?.lastName ?: "")}
    val desc = remember { mutableStateOf(user?.description ?: "") }
    val avatar = remember { mutableStateOf(user?.avatarUrl ?: "") }

    val density = LocalDensity.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            avatar.value = uri.toString()

            val payloadImage = JsonObject(mapOf(
                "profile" to JsonPrimitive(true),
                "count" to JsonPrimitive(1)
            ))

            coroutineScope.launch {
                SocketManager.sendPacket(OPCode.UPLOAD_IMAGE, payloadImage, { packet ->
                    if (packet.payload is JsonObject) {
                        var imageName = ""

                        val cursor =
                            context.contentResolver.query(uri, null, null, null, null)
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
                            context.contentResolver.openInputStream(uri)
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
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.use {
                                    val responseData = it.body?.string()
                                    val content =
                                        Json.parseToJsonElement(responseData!!).jsonObject
                                    val token =
                                        content["photos"]!!.jsonObject.values.last().jsonObject["token"]!!.jsonPrimitive.content

                                    val payload = JsonObject(mapOf(
                                        "lastName" to JsonPrimitive(secondName.value),
                                        "photoToken" to JsonPrimitive(token),
                                        "avatarType" to JsonPrimitive("USER_AVATAR"),
                                        "crop" to JsonObject(mapOf(
                                            "y1" to JsonPrimitive(0.0),
                                            "x1" to JsonPrimitive(0.0),
                                            "y2" to JsonPrimitive(1.0),
                                            "x2" to JsonPrimitive(1.0)
                                        )),
                                        "firstName" to JsonPrimitive("")
                                    ))

                                    coroutineScope.launch {
                                        SocketManager.sendPacket(OPCode.CHANGE_PROFILE, payload, { packet ->
                                            coroutineScope.launch {
                                                UsersManager.processUsers(JsonArray(listOf(packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject)))
                                            }
                                        })
                                    }
                                }
                            }
                        })
                    }
                })
            }
        }
    }

    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier) {
                Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box() {
                        Utils.AvatarFromName(Utils.getFullName(AccountManager.accountID), avatar.value, 110, modifier = Modifier.clickable {
                            launcher.launch(PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                            ))
                        })
                    }
                    OutlinedTextField(value = firstName.value, onValueChange = { if (firstName.value.length < 60)  firstName.value = it }, singleLine = true, label = { Text("Имя")}, maxLines = 1, modifier = Modifier.width(220.dp),
                        supportingText = {
                            Text(
                                text = "${60 - firstName.value.length}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                            )
                        }
                    )

                    OutlinedTextField(value = secondName.value, onValueChange = { if (secondName.value.length < 60)  secondName.value = it }, singleLine = true, label = { Text("Фамилия")}, maxLines = 1, modifier = Modifier.width(220.dp),
                        supportingText = {
                            Text(
                                text = "${60 - secondName.value.length}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                            )
                        }
                    )

                    OutlinedTextField(value = desc.value, onValueChange = { if (desc.value.length < 400) desc.value = it}, label = { Text("О себе")},
                        supportingText = {
                        Text(
                            text = "${400 - desc.value.length}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    }, modifier = Modifier.width(220.dp))

//                    Button(onClick = {
//                    }) {
//                        Text("Выйти из аккаунта")
//                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = firstName.value != user!!.firstName || secondName.value != user.lastName || desc.value != user.description || user.avatarUrl != avatar.value,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically { with(density) { -40.dp.roundToPx() } } + fadeOut()
            ) {
                Button(onClick = {
                    val payload = JsonObject(mapOf(
                        "description" to JsonPrimitive(desc.value),
                        "lastName" to JsonPrimitive(secondName.value),
                        "photoToken" to JsonPrimitive(""),
                        "avatarType" to JsonPrimitive("USER_AVATAR"),
                        "firstName" to JsonPrimitive(firstName.value)
                    ))

                    coroutineScope.launch {
                        SocketManager.sendPacket(OPCode.CHANGE_PROFILE, payload, { packet ->
                            coroutineScope.launch {
                                UsersManager.processUsers(JsonArray(listOf(packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject)))
                            }
                        })
                    }
                }) {
                    Text("Сохранить")
                }
            }
        }
    }
}