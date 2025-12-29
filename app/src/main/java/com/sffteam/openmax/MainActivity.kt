package com.sffteam.openmax

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sffteam.openmax.ui.theme.AppTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val view = this.window.decorView
        view.setBackgroundColor(resources.getColor(R.color.black, null))
        // Must be runBlocking because we need to wait for token check
        runBlocking {
            val exampleData = dataStore.data.first()
            AccountManager.token = exampleData[stringPreferencesKey("token")].toString()
        }

        val context = this

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                SocketManager.connect(context)
            }
        }

        if (AccountManager.token != "null") {
            val intent = Intent(this, ChatListActivity::class.java)

            this.startActivity(intent)
            finish()
        }

        setContent {
            Utils.windowSize = calculateWindowSizeClass(this)

            AppTheme {
                val phone = remember { mutableStateOf("") }
                val errorText = remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Добро пожаловать в Open MAX!",
                        fontSize = 25.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    Text(
                        "Введите свой номер телефона, чтобы войти или зарегистрироваться",
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Text(
                        errorText.value, color = Color.White, fontSize = 18.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = phone.value,
                            onValueChange = { newText ->
                                phone.value = newText
                            },
                            label = { Text("Номер телефона") },
                            textStyle = TextStyle(fontSize = 25.sp),
                            modifier = Modifier.padding(bottom = 3.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            )
                        )
                    }

                    val context = LocalContext.current
                    Button(
                        modifier = Modifier.padding(16.dp), onClick = {
                            val packet = SocketManager.packPacket(
                                OPCode.START_AUTH.opcode, JsonObject(
                                    mapOf(
                                        "phone" to JsonPrimitive(phone.value),
                                        "type" to JsonPrimitive("START_AUTH"),
                                        "language" to JsonPrimitive("ru")
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
                                            } else if ("token" in packet.payload) {
                                                val intent =
                                                    Intent(context, CodeActivity::class.java)

                                                println("token " + packet.payload["token"])

                                                intent.putExtra(
                                                    "token",
                                                    packet.payload["token"]!!.jsonPrimitive.content
                                                )
                                                context.startActivity(intent)
                                            } else {
                                                println("wtf")
                                            }
                                        }
                                    })
                            }
                        }) {
                        Text("Продолжить", fontSize = 25.sp)
                    }
                }
            }
        }
    }
}