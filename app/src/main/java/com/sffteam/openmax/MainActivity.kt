package com.sffteam.openmax

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.sffteam.openmax.WebsocketManager
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.sffteam.openmax.CodeActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val view = this.window.decorView;
        view.setBackgroundColor(resources.getColor(R.color.black, null))

        WebsocketManager.Connect()

        setContent {
            val phone = remember{mutableStateOf("")}

            Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextField(
                    value = phone.value,
                    onValueChange = { newText -> phone.value = newText }, // Lambda to update the state when text changes
                    label = { Text("Введите номер телефона") }, // Optional label for the text field
                    textStyle = TextStyle(fontSize = 25.sp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        capitalization = KeyboardCapitalization.Sentences
                    )

                    )

                val context = LocalContext.current
                Button(
                    onClick = {
                        val intent = Intent(context, CodeActivity::class.java)

                        WebsocketManager.SendPacket(
                            OPCode.START_AUTH.opcode,
                            JsonObject(
                                mapOf(
                                    "phone" to JsonPrimitive(phone.value.toString()),
                                    "type" to JsonPrimitive("START_AUTH"),
                                    "language" to JsonPrimitive("ru")
                                )
                            )
                        )

                        context.startActivity(intent)
                    }
                ) {
                    Text("Продолжить", fontSize = 25.sp)
                }
            }
        }
    }
}