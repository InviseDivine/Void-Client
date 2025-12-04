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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
//        WebsocketManager.Connect()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val view = this.window.decorView;
        view.setBackgroundColor(resources.getColor(R.color.black, null))
        val hexString = "0A03000300120000007E84A56572726F72B17665726966792E636F64652E77726F6E67A76D657373616765B54B65793A206572726F722E77726F6E672E636F6465B06C6F63616C697A65644D657373616765B7D09DD0B5D0B2D0B5D180D0BDD18BD0B920D0BAD0BED0B4A57469746C65B7D09DD0B5D0B2D0B5D180D0BDD18BD0B920D0BAD0BED0B4"
        println(hexString.decodeHex())
        println(SocketManager.unpackPacket(hexString.decodeHex()))
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