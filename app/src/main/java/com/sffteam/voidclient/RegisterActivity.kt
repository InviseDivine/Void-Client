package com.sffteam.voidclient

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sffteam.voidclient.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

class RegisterActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val token = intent.getStringExtra("token")

        setContent {
            AppTheme {
                val firstName = remember { mutableStateOf("") }
                val lastName = remember { mutableStateOf("") }
                val errorText = remember { mutableStateOf("") }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = firstName.value,
                        onValueChange = { newText -> firstName.value = newText },
                        label = { Text("Имя") },
                        textStyle = TextStyle(fontSize = 25.sp),
                        modifier = Modifier.padding(bottom = 15.dp)
                    )

                    OutlinedTextField(
                        value = lastName.value,
                        onValueChange = { newText -> lastName.value = newText },
                        label = { Text("Фамилия (необязательно)") },
                        textStyle = TextStyle(fontSize = 25.sp),
                    )

                    Text(
                        errorText.value, color = Color.White, fontSize = 25.sp
                    )
                    val context = LocalContext.current

                    Button(onClick = {
                        println("fff${firstName.value}fff")

                        if (firstName.value.isEmpty()) {
                            errorText.value = "Имя не может быть пустым!"
                        } else {
                            val payload = mutableMapOf(
                                "firstName" to JsonPrimitive(firstName.value),
                            )
                            if (lastName.value.isNotEmpty()) {
                                payload["lastName"] = JsonPrimitive(lastName.value)
                            }

                            payload["tokenType"] = JsonPrimitive("REGISTER")
                            payload["token"] = JsonPrimitive(token)

                            val packet = SocketManager.packPacket(23, JsonObject(payload))

                            GlobalScope.launch {
                                SocketManager.sendPacket(packet, { packet ->
                                    if (packet.payload is JsonObject) {
                                        if ("error" in packet.payload) {
                                            errorText.value =
                                                packet.payload["localizedMessage"]!!.jsonPrimitive.content
                                        } else if ("token" in packet.payload) {
                                            val intent =
                                                Intent(context, ChatListActivity::class.java)

                                            runBlocking {
                                                dataStore.edit { settings ->
                                                    // Nice sandwich lol
                                                    val tokenSettings =
                                                        packet.payload["token"]!!.jsonPrimitive.content
                                                    settings[stringPreferencesKey("token")] =
                                                        tokenSettings
                                                    AccountManager.token = tokenSettings
                                                }

                                            }

                                            GlobalScope.launch {
                                                SocketManager.loginToAccount(context)
                                            }

                                            context.startActivity(intent)

                                            finish()
                                        } else {
                                            println("wtf")
                                        }
                                    }
                                })
                            }
                        }
                    }) {
                        Text("Продолжить", fontSize = 25.sp)
                    }
                }
            }
        }
    }
}