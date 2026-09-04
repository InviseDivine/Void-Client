package com.invdiv.voidclient

import android.app.Activity
import android.content.Intent
import android.graphics.fonts.FontStyle
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val trackId = intent.getStringExtra("trackId")
        val hint = intent.getStringExtra("hint")
        val email = intent.getStringExtra("email")

        setContent {
            VoidclientTheme() {
                UiPassword(trackId.toString(), hint, email)
            }
        }
    }
}

@Composable
fun UiPassword(trackId : String, hint : String?, email : String?) {
    val password = remember { mutableStateOf("") }
    val errText = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(colorScheme.background)
        .padding(top = 32.dp)
        .statusBarsPadding()
        .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = {
            (context as Activity).finish()
        }, modifier = Modifier.align(Alignment.Start)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "",
                tint = colorScheme.onBackground
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(Icons.Filled.Lock,
                "",
                tint = colorScheme.onPrimary
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Двухфакторная аутенфикация", color = colorScheme.onBackground, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("Введите пароль для входа в аккаунт", color = colorScheme.onBackground.copy(0.6f), textAlign = TextAlign.Center)

            if (!hint.isNullOrEmpty()) {
                Text("Подсказка к паролю: $hint", color = colorScheme.onBackground.copy(0.6f), textAlign = TextAlign.Center)
            }

            Text(errText.value, color = colorScheme.error)
        }


        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            visualTransformation = if (!showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            label = { Text("Пароль") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            trailingIcon = {
                IconButton(onClick = {
                    showPassword = !showPassword
                }) {
                    Icon(
                        imageVector = if (!showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "passwordVisibility",
                        tint = colorScheme.onBackground
                    )
                }
            }
        )

        Button(onClick = {
            if (password.value.isNotEmpty()) {
                val payload = JsonObject(
                    mapOf(
                        "password" to JsonPrimitive(password.value),
                        "trackId" to JsonPrimitive(trackId)
                    )
                )

                coroutineScope.launch {
                    SocketManager.sendPacket(OPCode.PASSWORD_CHECK, payload, { packet ->
                        if (packet.payload is JsonObject) {
                            if (packet.payload.containsKey("error")) {
                                errText.value =
                                    packet.payload["message"]!!.jsonPrimitive.content
                            } else {
                                val intent = Intent(
                                    context, ChatsListActivity::class.java
                                ).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }

                                runBlocking {
                                    context.dataStore.edit { settings ->
                                        // Nice sandwich lol
                                        val token = packet.payload["tokenAttrs"]!!.jsonObject["LOGIN"]!!.jsonObject["token"]!!.jsonPrimitive.content
                                        settings[stringPreferencesKey("token")] = token
                                        AccountManager.token = token
                                    }
                                }

                                GlobalScope.launch {
                                    AccountManager.loginToAccount(context)
                                }

                                context.startActivity(intent)

                                (context as? Activity)?.finish()
                            }
                        }
                    })
                }
            }
        }) {
            Text("Войти")
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text("Забыли пароль?", color = colorScheme.primary, modifier = Modifier.clickable {
            Toast.makeText(context, "Будет доступно в следующих обновлениях!", Toast.LENGTH_SHORT).show()
        })
    }
}