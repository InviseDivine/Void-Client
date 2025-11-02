package com.sffteam.openmax

import android.os.Bundle
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

class CodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val phone = intent.getStringExtra("number")

        val view = this.window.decorView;
        view.setBackgroundColor(resources.getColor(R.color.black, null))

        setContent {
            val code = remember{mutableStateOf("")}

            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = code.value,
                    onValueChange = {
                        newText -> code.value = newText }, // Lambda to update the state when text changes
                    label = { Text("Введите код из СМС") }, // Optional label for the text field
                    textStyle = TextStyle(fontSize = 25.sp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    )
                )

                val context = LocalContext.current
                Button(
                    onClick = {

                    }
                ) {
                    Text("Войти", fontSize = 25.sp)
                }
            }

        }
    }
}