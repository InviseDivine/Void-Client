package com.sffteam.voidclient

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sffteam.voidclient.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token")

class CodeActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val code = remember { mutableStateOf("") }
                val errorText = remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(colorScheme.background),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = code.value,
                        onValueChange = { newText -> code.value = newText },
                        label = { Text("Введите код из СМС") },
                        textStyle = TextStyle(fontSize = 25.sp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        )
                    )
                    Text(
                        errorText.value, color = Color.White, fontSize = 25.sp
                    )

                    val context = LocalContext.current
                    Button(
                        modifier = Modifier.padding(16.dp), onClick = {
                            val packet = SocketManager.packPacket(
                                OPCode.CHECK_CODE.opcode, JsonObject(
                                    mapOf(
                                        "token" to JsonPrimitive(
                                            intent.getStringExtra("token").toString()
                                        ),
                                        "verifyCode" to JsonPrimitive(code.value),
                                        "authTokenType" to JsonPrimitive("CHECK_CODE")
                                    )
                                )
                            )

                            GlobalScope.launch {
                                SocketManager.sendPacket(
                                    packet, { packet ->
                                        println(packet.payload)
                                        if (packet.payload is JsonObject) {
                                            if ("error" in packet.payload) {
                                                errorText.value =
                                                    packet.payload["localizedMessage"].toString()
                                            } else if ("tokenAttrs" in packet.payload) {
                                                if ("REGISTER" in packet.payload["tokenAttrs"]!!.jsonObject) {
                                                    val intent = Intent(
                                                        context, RegisterActivity::class.java
                                                    )

                                                    val token =
                                                        packet.payload["tokenAttrs"]!!.jsonObject["REGISTER"]!!.jsonObject["token"]!!.jsonPrimitive.content

                                                    intent.putExtra("token", token)

                                                    startActivity(intent)

                                                    finish()
                                                } else if ("passwordChallenge" in packet.payload) {
                                                    val intent = Intent(
                                                        context, PasswordCheckActivity::class.java
                                                    )
                                                    val trackId = packet.payload["passwordChallenge"]?.jsonObject["trackId"]?.jsonPrimitive?.content
                                                    val hint = packet.payload["passwordChallenge"]?.jsonObject["hint"]?.jsonPrimitive?.content
                                                    val email = packet.payload["passwordChallenge"]?.jsonObject["email"]?.jsonPrimitive?.content


                                                    intent.putExtra("trackId", trackId)
                                                    intent.putExtra("hint", hint)
                                                    intent.putExtra("email", email)

                                                    context.startActivity(intent)

                                                    finish()
                                                } else {
                                                    val intent = Intent(
                                                        context, ChatListActivity::class.java
                                                    )
                                                    runBlocking {
                                                        dataStore.edit { settings ->
                                                            // Nice sandwich lol
                                                            val token =
                                                                packet.payload["tokenAttrs"]!!.jsonObject["LOGIN"]!!.jsonObject["token"]!!.jsonPrimitive.content
                                                            settings[stringPreferencesKey("token")] =
                                                                token
                                                            AccountManager.token = token
                                                        }
                                                    }

                                                    GlobalScope.launch {
                                                        SocketManager.loginToAccount(context)
                                                    }

                                                    context.startActivity(intent)

                                                    finish()
                                                }
                                            } else {

                                            }
                                        }
                                    })
                            }
                        }) {
                        Text("Войти", fontSize = 25.sp)
                    }
                }
            }
        }
    }
}