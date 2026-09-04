package com.invdiv.voidclient

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token")

class CodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val token = intent.getStringExtra("token").toString()
        val number = intent.getStringExtra("phone").toString()
        setContent {
            VoidclientTheme() {
                CodeUi(token, number)
            }
        }
    }
}

@Composable
fun CodeUi(token : String, number : String) {
    var tokenString by remember { mutableStateOf(token) }

    val errText = remember { mutableStateOf("") }
    val code = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var ticks by remember { mutableIntStateOf(60) }
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(launched) {
        if (ticks == 60 && !launched) {
            coroutineScope.launch {
                launched = true

                while(ticks > 0) {
                    delay(1.seconds)
                    ticks--
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(code.value) {
        if (code.value.length >= 6) {
            coroutineScope.launch {
                SocketManager.sendCode(code.value, tokenString, { packet ->
                    if (packet.payload is JsonObject) {
                        if ("error" in packet.payload) {
                            errText.value = packet.payload["localizedMessage"]!!.jsonPrimitive.content
                            code.value = ""
                        } else if ("tokenAttrs" in packet.payload) {
                            if ("REGISTER" in packet.payload["tokenAttrs"]!!.jsonObject) {
                                //TODO: Register screen
                            } else if ("passwordChallenge" in packet.payload) {
                                val intent = Intent(
                                    context, PasswordActivity::class.java
                                )

                                val trackId =
                                    packet.payload["passwordChallenge"]?.jsonObject["trackId"]?.jsonPrimitive?.content
                                val hint =
                                    packet.payload["passwordChallenge"]?.jsonObject["hint"]?.jsonPrimitive?.content
                                val email =
                                    packet.payload["passwordChallenge"]?.jsonObject["email"]?.jsonPrimitive?.content


                                intent.putExtra("trackId", trackId)
                                intent.putExtra("hint", hint)
                                intent.putExtra("email", email)

                                context.startActivity(intent)
                            } else {
                                val intent = Intent(
                                    context, ChatsListActivity::class.java
                                ).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }

                                runBlocking {
                                    context.dataStore.edit { settings ->
                                        val tokenNew =
                                            packet.payload["tokenAttrs"]!!.jsonObject["LOGIN"]!!.jsonObject["token"]!!.jsonPrimitive.content
                                        settings[stringPreferencesKey("token")] =
                                            tokenNew

                                        AccountManager.token = tokenNew
                                    }
                                }

                                GlobalScope.launch {
                                    AccountManager.loginToAccount(context)
                                }

                                context.startActivity(intent)

                                (context as? Activity)?.finish()
                            }
                        }
                    }
                })
            }
        }
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(colorScheme.background)
        .padding(top = 16.dp)
        .statusBarsPadding()
        .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
        Text("Код отправлен на номер $number", color = colorScheme.onBackground, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(color = colorScheme.onBackground.copy(0.6f))) { // , fontWeight = FontWeight.Bold
                append("Если не пришло SMS, проверьте чат ")
            }

            withStyle(style = SpanStyle(color = colorScheme.onBackground.copy(0.6f), fontWeight = FontWeight.Bold)) {
                append("\"Коды подтверждения\"")
            }

            withStyle(style = SpanStyle(color = colorScheme.onBackground.copy(0.6f))) { // , fontWeight = FontWeight.Bold
                append(" в MAX")
            }
        }, textAlign = TextAlign.Center)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box() {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0..5) {
                        val char = if (code.value.length > i) {
                            code.value[i]
                        } else {
                            null
                        }

                        Box(modifier = Modifier
                            .border(
                                width = 2.dp,
                                color = colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                            .widthIn(min = 30.dp)
                            .heightIn(min = MaterialTheme.typography.displaySmall.fontSize.value.dp + 8.dp)
                            .align(Alignment.CenterVertically)
                            .clickable {
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (char != null) {
                                println(char.toString())
                                Text(char.toString(), style = MaterialTheme.typography.displaySmall, color = colorScheme.onBackground, textAlign = TextAlign.Center)
                            } else if (i == code.value.length) {
                                Box(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp).background(colorScheme.primary).width(2.dp).height(MaterialTheme.typography.displaySmall.fontSize.value.dp)) {

                                }
                            }
                        }
                    }
                }

                BasicTextField(
                    value = code.value,
                    onValueChange = {
                        if (code.value.length < 6 || it.length < code.value.length) {
                            code.value = it
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .matchParentSize()
                        .alpha(0f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(Color.Transparent)
                )
            }

            Text(errText.value, color = colorScheme.error)

            if (ticks > 0) {
                Text("Получить новый код можно через ${ticks / 60}:${if (ticks % 60 == 0) "00" else if (ticks < 10) "0$ticks" else ticks}", color = colorScheme.onBackground.copy(0.6f))
            } else {
                Text("Получить новый код", color = colorScheme.onBackground, modifier = Modifier.clickable {
                    ticks = 60
                    launched = false

                    coroutineScope.launch {
                        SocketManager.resendPhoneNumber(number, { packet ->
                            if (packet.payload is JsonObject) {
                                if ("error" in packet.payload) {
                                    println(packet.payload)
                                    errText.value =
                                        packet.payload["localizedMessage"]?.jsonPrimitive?.content!!
                                } else if ("token" in packet.payload) {
                                    tokenString = packet.payload["token"]!!.jsonPrimitive.content
                                }
                            }
                        })
                    }
                })
            }
        }
    }
}