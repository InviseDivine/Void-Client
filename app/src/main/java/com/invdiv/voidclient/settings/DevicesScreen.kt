package com.invdiv.voidclient.settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.invdiv.voidclient.AccountManager
import com.invdiv.voidclient.OPCode
import com.invdiv.voidclient.SocketManager
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@Composable
fun DevicesScreen() {
    val devices by AccountManager.devicesList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val reallyExit = remember { mutableStateOf(false) }

    val scanQrCodeLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
        if (result is QRResult.QRSuccess) {
            val payload = JsonObject(
                mapOf(
                    "qrLink" to JsonPrimitive(result.content.rawValue.toString())
                )
            )

            coroutineScope.launch {
                SocketManager.sendPacket(OPCode.QR_CODE, payload, {
                })
            }
        }
    }

    LaunchedEffect(Unit) {
        SocketManager.sendPacket(OPCode.SESSIONS, JsonObject(mapOf()), { packet ->
            AccountManager.processDevices(packet.payload.jsonObject["sessions"]!!.jsonArray)
        })
    }

    if (reallyExit.value) {
        AlertDialog(
            onDismissRequest = { reallyExit.value = false},
            title = { Text(text = "Выйти со всех аккаунтов") },
            text = {
                Text("Вы действительно хотите завершить все сессии?")
            },
            confirmButton = {
                Button({
                    reallyExit.value = false
                    coroutineScope.launch {
                        SocketManager.sendPacket(OPCode.SESSIONS_EXIT, JsonObject(mapOf()), { packet ->

                        })
                    }

                    AccountManager.clearDevices()
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                Button({ reallyExit.value = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item() {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = {
                            scanQrCodeLauncher.launch(null) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Войти по QR-коду")
                        }

                        Button(onClick = {
                            reallyExit.value = true
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Завершить все сессии, кроме текущей")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .background(
                            color = colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (device in devices.sortedByDescending { it.time }) {
                        Row(modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
                            Column() {
                                val deviceText = device.client + if (device.current) {
                                    " (Текущая)"
                                } else {
                                    ""
                                }

                                Text(deviceText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(device.info, style = MaterialTheme.typography.titleSmall, color = colorScheme.onPrimaryContainer.copy(0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(device.location, style = MaterialTheme.typography.titleSmall, color = colorScheme.onPrimaryContainer.copy(0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}