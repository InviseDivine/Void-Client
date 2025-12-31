package com.sffteam.voidclient

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sffteam.voidclient.ui.theme.AppTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PasswordCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_password_check2)

        val trackId = intent.getStringExtra("trackId")

        setContent {
            val password = remember { mutableStateOf("") }
            val errorText = remember { mutableStateOf("") }

            val coroutineScope = rememberCoroutineScope()
            AppTheme {
                val context = LocalContext.current

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { newText -> password.value = newText },
                        label = { Text("Введите облачный пароль") },
                        textStyle = TextStyle(fontSize = 25.sp),
                    )
                    Text(
                        errorText.value, color = Color.White, fontSize = 25.sp
                    )

                    Button(onClick = {
                        val packet = SocketManager.packPacket(
                            OPCode.PASSWORD_CHECK.opcode, JsonObject(
                                mapOf(
                                    "password" to JsonPrimitive(password.value),
                                    "trackId" to JsonPrimitive(trackId)
                                )
                            )
                        )

                        coroutineScope.launch {
                            SocketManager.sendPacket(packet, { packet ->
                                if (packet.payload is JsonObject) {
                                    if (packet.payload.containsKey("error")) {
                                        errorText.value =
                                            packet.payload["message"]!!.jsonPrimitive.content
                                    } else {
                                        val intent = Intent(
                                            context, ChatListActivity::class.java
                                        )
                                        runBlocking {
                                            dataStore.edit { settings ->
                                                // Nice sandwich lol
                                                val token =
                                                    packet.payload["tokenAttrs"]!!.jsonObject["LOGIN"]!!.jsonObject["token"]!!.jsonPrimitive.content
                                                settings[stringPreferencesKey("token")] = token
                                                AccountManager.token = token
                                            }
                                        }

                                        GlobalScope.launch {
                                            SocketManager.loginToAccount(context)
                                        }

                                        context.startActivity(intent)

                                        finish()
                                    }
                                }
                            })
                        }
                    }) {
                        Text("Войти")
                    }
                }
            }
        }
    }
}