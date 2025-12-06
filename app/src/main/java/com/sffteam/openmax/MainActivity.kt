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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.sffteam.openmax.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.codec.binary.Hex
import java.math.BigInteger
fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Hex string must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val view = this.window.decorView;
        view.setBackgroundColor(resources.getColor(R.color.black, null))
        // val hexString = "0a03000300120000007e84a56572726f72b17665726966792e636f64652e77726f6e67a76d657373616765b54b65793a206572726f722e77726f6e672e636f6465b06c6f63616c697a65644d657373616765b7d09dd0b5d0b2d0b5d180d0bdd18bd0b920d0bad0bed0b4a57469746c65b7d09dd0b5d0b2d0b5d180d0bdd18bd0b920d0bad0bed0b4"
//        val jsonobj = "{\"error\":\"verify.code.wrong\",\"message\":\"Key: error.wrong.code\",\"localizedMessage\":\"Неверный код\",\"title\":\"Неверный код\"}"
//        println(SocketManager.unpackPacket(SocketManager.packPacket(18, Json.decodeFromString(jsonobj))))
        // Must be runBlocking because we need to wait for token check
        runBlocking {
            val exampleData = dataStore.data.first()
            AccountManager.token = exampleData[stringPreferencesKey("token")].toString()
        }

        if (AccountManager.token != "null") {
            val intent = Intent(this, ChatListActivity::class.java)

            this.startActivity(intent)
            finish()
        }

        setContent {
            AppTheme() {

                val phone = remember { mutableStateOf("") }
                val errorText = remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(errorText.value,
                        color = Color.White,
                        fontSize = 25.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround, // Distributes space horizontally
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = phone.value,
                            onValueChange = { newText ->
                                phone.value = newText
                            }, // Lambda to update the state when text changes
                            label = { Text("Введите номер телефона") }, // Optional label for the text field
                            textStyle = TextStyle(fontSize = 25.sp),
//                        keyboardOptions = KeyboardOptions(
//                        keyboardType = KeyboardType.Number,
//                        capitalization = KeyboardCapitalization.Sentences
//                    )
                        )
                    }

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            WebsocketManager.SendPacket(
                                OPCode.START_AUTH.opcode,
                                JsonObject(
                                    mapOf(
                                        "phone" to JsonPrimitive(phone.value.toString()),
                                        "type" to JsonPrimitive("START_AUTH"),
                                        "language" to JsonPrimitive("ru")
                                    )
                                ),
                                { packet ->
                                    println(packet.payload)
                                    if (packet.payload is JsonObject) {
                                        if ("error" in packet.payload) {
                                            errorText.value = packet.payload["localizedMessage"].toString()
                                        } else if ("token" in packet.payload) {
                                            val intent = Intent(context, CodeActivity::class.java)

                                            println("token " + packet.payload["token"])

                                            intent.putExtra("token", packet.payload["token"]!!.jsonPrimitive.content)
                                            context.startActivity(intent)
                                        } else {
                                            println("wtf")
                                        }
                                    }
                                }
                            )
                        }
                    ) {
                        Text("Продолжить", fontSize = 25.sp)
                    }
                }
            }
        }
    }
}