package com.sffteam.voidclient

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sffteam.voidclient.ui.theme.AppTheme
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
    @OptIn(
        DelicateCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class,
        ExperimentalMaterial3Api::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Must be runBlocking because we need to wait for token check
        runBlocking {
            val exampleData = dataStore.data.first()
            AccountManager.token = exampleData[stringPreferencesKey("token")].toString()
        }

        val context = this

        val codes = mapOf(
            "Россия" to "+7",
            "Беларусь" to "+375"
        )
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
            AppTheme {
                val phone = remember { mutableStateOf("") }
                val errorText = remember { mutableStateOf("") }
                var selectedCodeStr = remember { mutableStateOf("Россия") }
                var selectedCode = remember { mutableStateOf("+7") }
                val expanded by remember { mutableStateOf(false) }
                Utils.windowSize = calculateWindowSizeClass(this)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(colorScheme.background),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val packageManager = context.packageManager
                    val appIconDrawable: Drawable =
                        packageManager.getApplicationIcon("com.sffteam.voidclient")
                    var expanded by remember { mutableStateOf(false) }

                    Image(
                        appIconDrawable.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(),
                        contentDescription = "Image", modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )

                    Text(
                        "Добро пожаловать в Void Client!",
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

                    if (errorText.value.isNotEmpty()) {
                        Text(
                            "Ошибка: ${errorText.value}", color = Color.White, fontSize = 18.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TODO: Rewrite
                        ExposedDropdownMenuBox(
                            expanded = expanded, onExpandedChange = { expanded = it },
                            modifier = Modifier.width(180.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedCodeStr.value + " (${selectedCode.value})",
                                onValueChange = {},
                                readOnly = true,
                                maxLines = 1,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .heightIn(max = 60.dp)
                                    .padding(top = 3.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                codes.toList().forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.first,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        },
                                        onClick = {
                                            selectedCodeStr.value = option.first
                                            selectedCode.value = option.second
                                            expanded = false
                                        },
                                    )
                                }
                            }

                        }

                        OutlinedTextField(
                            value = phone.value,
                            onValueChange = { newText ->
                                phone.value = newText
                            },
                            label = { Text("Номер телефона") },
                            textStyle = TextStyle(fontSize = 25.sp),
                            modifier = Modifier.padding(bottom = 3.dp).width(200.dp),
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
                                        "phone" to JsonPrimitive(selectedCode.value + phone.value),
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
                                                    packet.payload["localizedMessage"]?.jsonPrimitive?.content!!
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
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Продолжить", fontSize = 25.sp)
                    }
                }
            }
        }
    }
}