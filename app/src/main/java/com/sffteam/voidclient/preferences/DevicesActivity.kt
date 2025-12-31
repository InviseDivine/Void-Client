package com.sffteam.voidclient.preferences

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sffteam.voidclient.AccountManager
import com.sffteam.voidclient.OPCode
import com.sffteam.voidclient.Session
import com.sffteam.voidclient.SocketManager
import com.sffteam.voidclient.dataStore
import com.sffteam.voidclient.ui.theme.AppTheme
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.util.Date
import kotlin.time.ExperimentalTime
import kotlin.time.Instant.Companion.fromEpochMilliseconds

class DevicesActivity : ComponentActivity() {
    val scanQrCodeLauncher = registerForActivityResult(ScanQRCode()) { result ->
        if (result is QRResult.QRSuccess) {
            val packet = SocketManager.packPacket(OPCode.QR_CODE.opcode, JsonObject(
                mapOf(
                    "qrLink" to JsonPrimitive(result.content.rawValue.toString())
                )
            ))

            runBlocking {
                SocketManager.sendPacket(packet, { packet ->
                    if (packet.payload is JsonObject) {
                        println(packet.payload)
                    }
                })
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            if (AccountManager.sessionsList.collectAsState().value.isEmpty()) {
                val packet =
                    SocketManager.packPacket(OPCode.SESSIONS.opcode, JsonObject(emptyMap()))
                coroutineScope.launch {
                    SocketManager.sendPacket(packet, { packet ->
                        if (packet.payload is JsonObject) {
                            AccountManager.processSession(packet.payload["sessions"]!!.jsonArray)
                        }
                    })
                }
            }

            AppTheme {
                val context = LocalContext.current

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
                                Text("Устройства")
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

                    LazyColumn(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item() {
                            Button(onClick = {
                                scanQrCodeLauncher.launch(null)
                            }) {
                                Text("Войти по QR коду", fontSize = 18.sp)
                            }
                        }

                        item() {
                            Button(onClick = {
                                val packet = SocketManager.packPacket(OPCode.SESSIONS_EXIT.opcode,
                                    JsonObject(emptyMap()))

                                coroutineScope.launch {
                                    SocketManager.sendPacket(packet, { packet ->
                                        if (packet.payload is JsonObject) {
                                            println(packet.payload)
                                            try {
                                                runBlocking {
                                                    context.dataStore.edit { settings ->
                                                        settings[stringPreferencesKey("token")] = packet.payload["token"]!!.jsonPrimitive.content
                                                        SocketManager.loginToAccount(context)
                                                    }
                                                }
                                            } catch (e : Exception) {
                                                println(e)
                                            }
                                        }
                                    })

                                }
                            }) {
                                Text("Завершить все сеансы", fontSize = 18.sp, color = Color.Red)
                            }
                        }

                        item() {
                            Box(
                                modifier = Modifier
                                    .background(
                                        colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 4.dp, end = 4.dp)
                            ) {
                                Column() {
                                    val sessions by AccountManager.sessionsList.collectAsState()

                                    Column(modifier = Modifier) {
                                        Text(
                                            "Активные сеансы",
                                            fontSize = 22.sp,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                        )

                                        for (session in sessions.sortedByDescending { value -> value.time }) {
                                            DrawSessions(session)
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
}

@OptIn(ExperimentalTime::class)
@Composable
fun DrawSessions(session: Session) {
    Box(modifier = Modifier.padding(bottom = 8.dp)) {
        Row() {
            val lastMessageTime = session.time
            val currentTime = Date().time

            val instantLast = fromEpochMilliseconds(lastMessageTime)

            val duration = Duration.ofSeconds(currentTime / 1000 - lastMessageTime / 1000)

            val localDateTime = instantLast.toLocalDateTime(TimeZone.currentSystemDefault())

            val hours = if (localDateTime.hour < 10) {
                "0${localDateTime.hour}"
            } else {
                localDateTime.hour
            }

            val minutes = if (localDateTime.minute < 10) {
                "0${localDateTime.minute}"
            } else {
                localDateTime.minute
            }

            val time = if (duration.toHours() < 24) {
                "${hours}:${minutes}"
            } else {
                "${hours}:${minutes} ${localDateTime.date}"
            }

            val icon = if (session.client == "MAX WEB") {
                Icons.Outlined.Web
            } else if (session.client == "MAX Android") {
                Icons.Outlined.Android
            } else {
                // TODO : Change to IOS icon
                Icons.Outlined.PhoneIphone
            }
            Icon(
                icon,
                "lol",
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp)
            )

            Column(modifier = Modifier.weight(0.8f)) {
                val client = if (session.current) {
                    session.client + " (Текущая)"
                } else {
                    session.client
                }
                Text(text = client, fontSize = 18.sp, fontWeight = Bold)
                Text(text = session.info, fontSize = 16.sp, modifier = Modifier.alpha(0.7f))
                Text(text = session.location, fontSize = 16.sp, modifier = Modifier.alpha(0.7f))
            }

            Text(text = time, fontSize = 16.sp)
        }
    }
}