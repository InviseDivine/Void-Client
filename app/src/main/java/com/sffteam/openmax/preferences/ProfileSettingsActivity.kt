package com.sffteam.openmax.preferences

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.compose.AsyncImage
import com.sffteam.openmax.AccountManager
import com.sffteam.openmax.OPCode
import com.sffteam.openmax.R
import com.sffteam.openmax.SocketManager
import com.sffteam.openmax.UserManager
import com.sffteam.openmax.ui.theme.AppTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class ProfileSettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val users by UserManager.usersList.collectAsState()
                        val you = users[AccountManager.accountID]
                        val firstName = remember { mutableStateOf(you!!.firstName) }
                        val lastName = remember { mutableStateOf(you?.lastName ?: "") }
                        val desc = remember { mutableStateOf(AccountManager.desc) }

                        OutlinedTextField(
                            value = firstName.value,
                            onValueChange = { newText ->
                                if (newText.length <= 59) {
                                    firstName.value = newText
                                }
                            },
                            label = { Text("Имя") },
                            textStyle = TextStyle(fontSize = 25.sp),
                            modifier = Modifier
                                .padding(bottom = 15.dp)
                                .align(Alignment.CenterHorizontally),
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
                            modifier = Modifier
                                .padding(bottom = 15.dp)
                                .align(Alignment.CenterHorizontally)
                        )

                        OutlinedTextField(
                            value = desc.value,
                            onValueChange = { newText ->
                                if (newText.length <= 400) {
                                    desc.value = newText
                                }
                            },
                            label = { Text("О себе") },
                            textStyle = TextStyle(fontSize = 25.sp),
                            modifier = Modifier
                                .padding(bottom = 15.dp)
                                .align(Alignment.CenterHorizontally)
                        )

                        Button(
                            onClick = {
                                var packetJson = mutableMapOf(
                                    "firstName" to JsonPrimitive(firstName.value),
                                    "lastName" to JsonPrimitive(lastName.value),
                                )
                                packetJson["description"] = JsonPrimitive(desc.value)

                                val packet = SocketManager.packPacket(
                                    OPCode.CHANGE_PROFILE.opcode,
                                    JsonObject(packetJson)
                                )

                                GlobalScope.launch {
                                        SocketManager.sendPacket(packet, { packet ->
                                            if (packet.payload is JsonObject) {
                                                AccountManager.accountID =
                                                    packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["id"]!!.jsonPrimitive.long

                                                AccountManager.phone = packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["phone"]!!.jsonPrimitive.content
                                                AccountManager.desc = packet.payload.jsonObject["profile"]!!.jsonObject["contact"]!!.jsonObject["description"]!!.jsonPrimitive.content
                                            }
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Сохранить", fontSize = 18.sp)
                        }
                        Button(
                            onClick = {

                            }
                        ) {
                            Text("Выйти из профиля", fontSize = 18.sp)
                        }

                    }
                }
            }
        }
    }
}