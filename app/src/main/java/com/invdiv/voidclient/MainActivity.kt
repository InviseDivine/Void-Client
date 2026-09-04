package com.invdiv.voidclient

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = this

        runBlocking {
            val data = dataStore.data.first()

            AccountManager.token = data[stringPreferencesKey("token")].toString()
            AccountManager.sessionId = if (data[intPreferencesKey("sessionId")] == null) {
                0
            } else {
                data[intPreferencesKey("sessionId")]!!
            }

            context.dataStore.edit { settings ->
                settings[intPreferencesKey("sessionId")] = AccountManager.sessionId + 1
            }

            AccountManager.mtInstanceId = data[stringPreferencesKey("mtInstanceId")].toString()

            if (data[stringPreferencesKey("mtInstanceId")].toString() == "null") {
                AccountManager.mtInstanceId = UUID.randomUUID().toString()

                context.dataStore.edit { settings ->
                    settings[stringPreferencesKey("mtInstanceId")] = AccountManager.mtInstanceId
                }
            }
        }

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                SocketManager.connect(context)
            }
        }

        if (AccountManager.token != "null") {
            val intent = Intent(this, ChatsListActivity::class.java)

            if (AccountManager.currentDeepLink.first.isNotEmpty()) {
                intent.putExtra(AccountManager.currentDeepLink.first, AccountManager.currentDeepLink.second)
                AccountManager.currentDeepLink = Pair("", "")
            }

            this.startActivity(intent)
            finish()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoidclientTheme {
                Ui()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ui() {
    val context = LocalContext.current
    val phone = remember { mutableStateOf("") }
    val errText = remember { mutableStateOf("") }
    var choseCountry by remember { mutableStateOf("RU") }
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val choseCountryMap = Utils.countries[choseCountry]

    val sheetState = rememberModalBottomSheetState()
    var showCountries by remember { mutableStateOf(false) }

    if (showCountries) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                showCountries = false
            }
        ) {
            for (country in Utils.maxCodeBase) {
                val mapCountry = Utils.countries[country]

                Row(modifier = Modifier.fillMaxWidth().clickable {
                    choseCountry = country
                    showCountries = false
                }.padding(start = 8.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(mapCountry?.flag ?: "")
                    Text(mapCountry?.name ?: "")

                    Spacer(modifier = Modifier.weight(1f))

                    Text("+" + mapCountry?.phoneCode)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(colorScheme.background)
        .statusBarsPadding()
        .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            val packageManager = context.packageManager
            val appIconDrawable: Drawable =
                packageManager.getApplicationIcon("com.invdiv.voidclient")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    appIconDrawable.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(),
                    contentDescription = "Image",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                Text("Void Client", fontWeight = FontWeight.Bold, color = colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Введите номер телефона", color = colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text("Для регистрации или входа в аккаунт", color = colorScheme.onBackground.copy(0.8f))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(errText.value, color = colorScheme.onBackground)

                Row {
                    Box(modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .border(
                            width = 2.dp,
                            color = colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(start = 18.dp, end = 18.dp)
                        .clickable {
                            showCountries = true
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                            .width(screenWidth * 0.8f)
                            .heightIn(min = 60.dp)
                        ) {
                            Text(choseCountryMap?.flag ?: "", color = colorScheme.onBackground)

                            Spacer(modifier = Modifier.width(20.dp))

                            Text(choseCountryMap?.name ?: "", color = colorScheme.onBackground)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Box(modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .border(
                            width = 2.dp,
                            color = colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(("+" + choseCountryMap?.phoneCode) ?: "", color = colorScheme.onBackground)

                            Spacer(modifier = Modifier.width(20.dp))

                            Text("|", color = colorScheme.onBackground)
                            Spacer(modifier = Modifier.width(8.dp))

                            BasicTextField(
                                value = phone.value,
                                onValueChange = { if (phone.value.length < (choseCountryMap?.maxDigits ?: 1) || it.length < phone.value.length) phone.value = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                textStyle = LocalTextStyle.current.copy(
                                    color = colorScheme.onBackground
                                ),
                                modifier = Modifier.width(screenWidth * 0.8f).heightIn(min = 60.dp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.heightIn(min = 60.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (phone.value.isEmpty()) {
                                            Text(
                                                choseCountryMap?.example ?: "",
                                                modifier = Modifier.alpha(0.7f),
                                                color = colorScheme.onBackground
                                            )
                                        }

                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    if (phone.value.isNotEmpty()) {
                        coroutineScope.launch {
                            SocketManager.sendPhoneNumber(choseCountryMap!!.phoneCode + phone.value, { packet ->
                                if (packet.payload is JsonObject) {
                                    if ("error" in packet.payload) {
                                        println(packet.payload)
                                        errText.value =
                                            packet.payload["localizedMessage"]?.jsonPrimitive?.content!!
                                    } else if ("token" in packet.payload) {
                                        val intent =
                                            Intent(context, CodeActivity::class.java)
                                        intent.putExtra(
                                            "token",
                                            packet.payload["token"]!!.jsonPrimitive.content
                                        )

                                        intent.putExtra(
                                            "phone",
                                            "+" + choseCountryMap!!.phoneCode + phone.value
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                            })
                        }
                    }
                }) {
                    Text("Продолжить")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(buildAnnotatedString  {
                    withStyle(style = SpanStyle(color = colorScheme.onBackground.copy(0.6f))) {
                        append("Продолжая, вы принимаете ")
                    }

                    withLink(
                        LinkAnnotation.Url(
                            "https://legal.max.ru/pp",
                        )
                    ) {
                        append("политику конфиденциальности ")
                    }

                    withStyle(style = SpanStyle(color = colorScheme.onBackground.copy(0.6f))) {
                        append(", ")
                    }

                    withLink(
                        LinkAnnotation.Url(
                            "https://legal.max.ru/ps",
                        )
                    ) {
                        append("пользовательское соглашение ")
                    }

                    withStyle(style = SpanStyle(color = colorScheme.onBackground.copy(0.6f))) {
                        append("и ")
                    }

                    withLink(
                        LinkAnnotation.Url(
                            "https://legal.max.ru/recsysrules",
                        )
                    ) {
                        append("правила персональных рекомендаций MAX")
                    }
                }, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}