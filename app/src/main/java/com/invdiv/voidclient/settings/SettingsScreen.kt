package com.invdiv.voidclient.settings

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.invdiv.voidclient.AccountManager
import com.invdiv.voidclient.MediaViewActivity
import com.invdiv.voidclient.UsersManager
import com.invdiv.voidclient.Utils
import kotlinx.coroutines.launch

data class SettingTab(
    val icon: ImageVector,
    val name: String,
    val desc : String
)
@Composable
fun MainSettingsScreen(navController : NavController) {
    val users by UsersManager.usersList.collectAsState()
    val context = LocalContext.current

    val myUser = users[AccountManager.accountID]

    val myName = Utils.getFullName(myUser)
    val myAvatar = myUser!!.avatarUrl
    val phone = "+${AccountManager.phone}"

    val clipboardManager = LocalClipboard.current

    val coroutineScope = rememberCoroutineScope()

    val tabs = mapOf(
        "Безопасность" to SettingTab(Icons.Filled.Lock, "Безопасность", "Безопасный режим, конфидециальность"),
        "Устройства" to SettingTab(Icons.Filled.Devices, "Устройства", "Сессии, вход по QR-коду"),
        "О приложении" to SettingTab(Icons.Filled.Info, "О приложении", "Разработчики, информация")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Utils.AvatarFromName(myName, myAvatar, 100, modifier = Modifier.align(Alignment.CenterHorizontally).clickable {
                    val intent = Intent(context, MediaViewActivity::class.java)

                    intent.putExtra("isSingleImage", true)
                    intent.putExtra("image", myAvatar)

                    context.startActivity(intent)
                })

                Text(myName, style = MaterialTheme.typography.titleLarge, color = colorScheme.onBackground, modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Row(modifier = Modifier.align(Alignment.CenterHorizontally).clickable {
                    coroutineScope.launch {
                        clipboardManager.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText(
                                    phone,
                                    phone
                                )
                            )
                        )
                    }
                }) {
                    Text(phone, style = MaterialTheme.typography.titleMedium, color = colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically), textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "",
                        tint = colorScheme.primary
                    )
                }
            }
        }

        item {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(color = colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    for (tab in tabs) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                            .clickable {
                                navController.navigate(tab.key)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.value.icon,
                                contentDescription = "",
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(tab.value.name, style = MaterialTheme.typography.titleMedium)
                                Text(tab.value.desc, style = MaterialTheme.typography.titleSmall, color = colorScheme.onPrimaryContainer.copy(0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}