package com.invdiv.voidclient.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCbrt
import com.invdiv.voidclient.AccountManager
import com.invdiv.voidclient.ChatsListActivity
import com.invdiv.voidclient.OPCode
import com.invdiv.voidclient.SocketManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

data class SecuritySetting(
    val title: String,
    val key: String,
    val values: Map<Any, String>,
    val label: String
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen() {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showInformationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var securityIndex by remember { mutableIntStateOf(0) }
    var informationIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val userSettings by AccountManager.userSettings.collectAsState()

    val securitySettings = listOf(
        SecuritySetting(
            title = "Найти меня по номеру",
            key = "SEARCH_BY_PHONE",
            values = mapOf(
                "ALL" to "Все",
                "CONTACTS" to "Контакты"
            ),
            label = "Кто может найти меня по номеру"
        ),
        SecuritySetting(
            title = "Позвонить",
            key = "INCOMING_CALL",
            values = mapOf(
                "ALL" to "Все",
                "CONTACTS" to "Контакты"
            ),
            label = "Кто может мне позвонить?"
        ),
        SecuritySetting(
            title = "Пригласить в чат",
            key = "CHATS_INVITE",
            values = mapOf(
                "ALL" to "Все",
                "CONTACTS" to "Контакты"
            ),
            label = "Кто может пригласить меня в чат"
        ),
        SecuritySetting(
            title = "Показывать контент",
            key = "CONTENT_LEVEL_ACCESS",
            values = mapOf(
                true to "Безопасный",
                false to "Весь"
            ),
            label = "Какой контент показывать?"
        )
    )

    val informationSettings = listOf(
        SecuritySetting(
            title = "Видеть статус \"в сети\"",
            key = "HIDDEN",
            values = mapOf(
                true to "Никто",
                false to "Контакты"
            ),
            label = "Кто видит мой статус \"в сети\""
        ),
        SecuritySetting(
            title = "Видеть мой номер",
            key = "PHONE_NUMBER_PRIVACY",
            values = mapOf(
                "ALL" to "Все",
                "CONTACTS" to "Контакты",
                "NOBODY" to "Никто"
            ),
            label = "Кто может видеть мой номер телефона"
        ),
    )

    val safeMode = userSettings["SAFE_MODE"] as? Boolean ?: false

    if (showBottomSheet || showInformationSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                showInformationSheet = false
            },
            sheetState = sheetState
        ) {
            if (showBottomSheet) {
                val securityIndexed = securitySettings[securityIndex]
                val label = securityIndexed.label

                Text(label!!, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                for ((key, value) in securityIndexed.values.entries) {
                    TextButton(onClick = {
                        val payload : MutableMap<String, JsonElement> = mutableMapOf()

                        if (key is Boolean) {
                            payload["settings"] = JsonObject(
                                mapOf("user" to JsonObject(mapOf(
                                    securityIndexed.key to JsonPrimitive(key)
                                )))
                            )
                        }

                        if (key is String) {
                            payload["settings"] = JsonObject(
                                mapOf("user" to JsonObject(mapOf(
                                    securityIndexed.key to JsonPrimitive(key)
                                )))
                            )
                        }

                        if (payload.isNotEmpty()) {
                            coroutineScope.launch {
                                SocketManager.sendPacket(OPCode.SETTINGS_CHANGE, JsonObject(payload), { packet ->
                                    println(packet)
                                    coroutineScope.launch {
                                        AccountManager.processSecuritySettings(packet.payload.jsonObject, context)
                                    }
                                })
                            }

                            showBottomSheet = false
                        }
                    },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(value, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                val informationIndexed = informationSettings[informationIndex]
                val label = informationIndexed.label

                Text(label!!, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                for ((key, value) in informationIndexed.values.entries) {
                    TextButton(onClick = {
                        val payload : MutableMap<String, JsonElement> = mutableMapOf()

                        if (key is Boolean) {
                            payload["settings"] = JsonObject(
                                mapOf("user" to JsonObject(mapOf(
                                    informationIndexed.key to JsonPrimitive(key)
                                )))
                            )
                        }

                        if (key is String) {
                            payload["settings"] = JsonObject(
                                mapOf("user" to JsonObject(mapOf(
                                    informationIndexed.key to JsonPrimitive(key)
                                )))
                            )
                        }

                        if (payload.isNotEmpty()) {
                            coroutineScope.launch {
                                SocketManager.sendPacket(OPCode.SETTINGS_CHANGE, JsonObject(payload), { packet ->
                                    println(packet)
                                    coroutineScope.launch {
                                        AccountManager.processSecuritySettings(packet.payload.jsonObject, context)
                                    }
                                })
                            }

                            showInformationSheet = false
                        }
                    },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(value, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(color = colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                .padding(start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    if (safeMode is Boolean) {
                        Icon(
                            Icons.Filled.Lock,
                            "",
                            tint = colorScheme.onPrimaryContainer
                        )
                        Text("Безопасный режим", style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.weight(1f))

                        Switch(
                            checked = safeMode,
                            onCheckedChange = {
                                val payload = if (!safeMode) {
                                    JsonObject(
                                        mapOf("settings" to JsonObject(
                                            mapOf("user" to JsonObject(mapOf(
                                            "INCOMING_CALL" to JsonPrimitive("CONTACTS"),
                                            "SEARCH_BY_PHONE" to JsonPrimitive("CONTACTS"),
                                            "SAFE_MODE_NO_PIN" to JsonPrimitive(true),
                                            "CONTENT_LEVEL_ACCESS" to JsonPrimitive(true),
                                            "UNSAFE_FILES" to JsonPrimitive(true),
                                            "CHATS_INVITE" to JsonPrimitive("CONTACTS"),
                                            "SAFE_MODE" to JsonPrimitive(true)
                                        )))))
                                    )
                                } else {
                                    JsonObject(
                                        mapOf("settings" to JsonObject(mapOf(
                                            "user" to JsonObject(mapOf(
                                                "SAFE_MODE_NO_PIN" to JsonPrimitive(false),
                                                "SAFE_MODE" to JsonPrimitive(false)
                                            ))
                                        )))
                                    )
                                }

                                coroutineScope.launch {
                                    SocketManager.sendPacket(OPCode.SETTINGS_CHANGE, JsonObject(payload), { packet ->
                                        println(packet)
                                        coroutineScope.launch {
                                            AccountManager.processSecuritySettings(packet.payload.jsonObject, context)
                                        }
                                    })
                                }
                            }
                        )
                    }
                }

                for ((index, setting) in securitySettings.withIndex()) {
                    val value = userSettings[setting.key]

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.clickable {
                        if (!safeMode) {
                            securityIndex = index
                            showBottomSheet = true
                        } else {
                            Toast.makeText(context, "Выключите безопасный режим для изменения!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(setting.title, style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.weight(1f))

                        Text(setting.values[value] ?: "", style = MaterialTheme.typography.titleMedium, color = colorScheme.onPrimaryContainer.copy(0.8f))

                        if (safeMode) {
                            Icon(
                                Icons.Filled.Lock,
                                "",
                                tint = colorScheme.onPrimaryContainer
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowRight,
                            contentDescription = "",
                            tint = colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        item {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(color = colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                .padding(start = 16.dp, end = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                for ((index, setting) in informationSettings.withIndex()) {
                    Row(modifier = Modifier.clickable {
                        informationIndex = index
                        showInformationSheet = true
                    }) {
                        val value = userSettings[setting.key]

                        Text(setting.title, style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.weight(1f))

                        Text(setting.values[value] ?: "", style = MaterialTheme.typography.titleMedium, color = colorScheme.onPrimaryContainer.copy(0.8f))

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowRight,
                            contentDescription = "",
                            tint = colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                }
            }
        }
    }
}