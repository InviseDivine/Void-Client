package com.sffteam.voidclient.preferences

import android.content.ClipData
import android.content.Intent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.voidclient.AccountManager
import com.sffteam.voidclient.UserManager
import com.sffteam.voidclient.Utils
import com.sffteam.voidclient.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.Locale.getDefault

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surfaceContainer,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            title = {
                                Text("Настройки")
                            },
                            navigationIcon = {
                                IconButton({ finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBackIos,
                                        contentDescription = "Меню"
                                    )
                                }
                            },
                        )
                    }
                ) {
                    val context = LocalContext.current
                    val users by UserManager.usersList.collectAsState()

                    println("users $users")
                    val user = users[AccountManager.accountID]
                    val username = user?.firstName + if (user?.lastName?.isNotEmpty() == true) {
                        " " + user.lastName
                    } else {
                        ""
                    }
                    val coroutineScope = rememberCoroutineScope()
                    val clipboardManager = LocalClipboard.current
                    Box(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(top = 5.dp, start = 8.dp, end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Account
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (user?.avatarUrl?.isNotEmpty() == true) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = "ChatIcon",
                                            modifier = Modifier
                                                .width(100.dp)
                                                .height(100.dp)
                                                .clip(CircleShape)
                                                .align(Alignment.CenterHorizontally),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        val fullName = user?.firstName + user?.lastName
                                        val initial =
                                            fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                                                .uppercase(getDefault())

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .width(100.dp)
                                                .height(100.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Utils.getColorForAvatar(fullName).first,
                                                            Utils.getColorForAvatar(fullName).second
                                                        )
                                                    )
                                                )
                                                .align(Alignment.CenterHorizontally),
                                            ) {
                                            Text(
                                                text = initial,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontSize = 25.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = username,
                                        fontSize = 20.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clickable {
                                                coroutineScope.launch {
                                                    clipboardManager.setClipEntry(
                                                        ClipEntry(
                                                            ClipData.newPlainText(
                                                                username,
                                                                username
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                    )

                                    Text(
                                        text = "+${AccountManager.phone}",
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clickable {
                                                coroutineScope.launch {
                                                    clipboardManager.setClipEntry(
                                                        ClipEntry(
                                                            ClipData.newPlainText(
                                                                "+${AccountManager.phone}",
                                                                "+${AccountManager.phone}"
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                    )
                                    Text(
                                        text = "ID: ${AccountManager.accountID}",
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clickable {
                                                coroutineScope.launch {
                                                    clipboardManager.setClipEntry(
                                                        ClipEntry(
                                                            ClipData.newPlainText(
                                                                "${AccountManager.accountID}",
                                                                "${AccountManager.accountID}"
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                    )

                                    Text(
                                        text = "Нажмите на информацию, чтобы скопировать её",
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .alpha(0.7f)
                                    )
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent =
                                                Intent(context, ProfileSettingsActivity::class.java)

                                            context.startActivity(intent)
                                        }
                                ) {
                                    Column() {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.AccountCircle,
                                                "lol",
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .padding()
                                            )
                                            Text(
                                                "Мой аккаунт", fontSize = 20.sp,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }
                                    }
                                }
                            }

                            // Settings
                            item {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .fillMaxWidth()
                                ) {
                                    Column() {
                                        // For next update
//                                        Row(
//                                            modifier = Modifier
//                                                .padding(12.dp)
//                                                .clickable {
//                                                    val intent =
//                                                        Intent(
//                                                            context,
//                                                            ProfileSettingsActivity::class.java
//                                                        )
//
//                                                    context.startActivity(intent)
//                                                },
//                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
//                                        ) {
//                                            Icon(
//                                                Icons.Outlined.Settings,
//                                                "lol",
//                                                modifier = Modifier
//                                                    .size(25.dp)
//                                                    .padding()
//                                            )
//                                            Text(
//                                                "Настройки Open MAX", fontSize = 20.sp,
//                                                modifier = Modifier.align(Alignment.CenterVertically)
//                                            )
//                                        }
//                                        Row(
//                                            modifier = Modifier
//                                                .padding(12.dp)
//                                                .clickable {
//                                                    val intent =
//                                                        Intent(
//                                                            context,
//                                                            ChatSettingsActivity::class.java
//                                                        )
//
//                                                    context.startActivity(intent)
//                                                },
//                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
//                                        ) {
//                                            Icon(
//                                                Icons.Outlined.ChatBubble,
//                                                "lol",
//                                                modifier = Modifier
//                                                    .size(25.dp)
//                                                    .padding()
//                                            )
//                                            Text(
//                                                "Настройки чатов", fontSize = 20.sp,
//                                                modifier = Modifier.align(Alignment.CenterVertically)
//                                            )
//                                        }
//                                        Row(
//                                            modifier = Modifier
//                                                .padding(12.dp)
//                                                .clickable {
//                                                    val intent =
//                                                        Intent(
//                                                            context,
//                                                            SecurityActivity::class.java
//                                                        )
//
//                                                    context.startActivity(intent)
//                                                },
//                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
//                                        ) {
//                                            Icon(
//                                                Icons.Outlined.Lock,
//                                                "lol",
//                                                modifier = Modifier
//                                                    .size(25.dp)
//                                                    .padding()
//                                            )
//                                            Text(
//                                                "Безопасность", fontSize = 20.sp,
//                                                modifier = Modifier.align(Alignment.CenterVertically)
//                                            )
//                                        }

                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .clickable {
                                                    val intent =
                                                        Intent(context, DevicesActivity::class.java)

                                                    context.startActivity(intent)
                                                },
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Smartphone,
                                                "lol",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .padding()
                                            )
                                            Text(
                                                "Устройства", fontSize = 20.sp,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .clickable {
                                                    Toast.makeText(
                                                        context,
                                                        "Будет доступно в следующих обновлениях",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                },
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Notifications,
                                                "lol",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .padding()
                                                    .alpha(0.7f)
                                            )
                                            Text(
                                                "Уведомления", fontSize = 20.sp,
                                                modifier = Modifier
                                                    .align(Alignment.CenterVertically)
                                                    .alpha(0.7f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .clickable {
                                                    Toast.makeText(
                                                        context,
                                                        "Будет доступно в следующих обновлениях",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                },
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Folder,
                                                "lol",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .padding()
                                                    .alpha(0.7f)

                                            )
                                            Text(
                                                "Папки с чатами", fontSize = 20.sp,
                                                modifier = Modifier
                                                    .align(Alignment.CenterVertically)
                                                    .alpha(0.7f)

                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .fillMaxWidth()
                                ) {
                                    Column() {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .clickable {
                                                    val intent =
                                                        Intent(context, AboutActivity::class.java)

                                                    context.startActivity(intent)
                                                },
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Info,
                                                "lol",
                                                modifier = Modifier
                                                    .size(25.dp)
                                                    .padding()
                                            )
                                            Text(
                                                "О приложении", fontSize = 20.sp,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            "Void Client a1.0.0", fontSize = 14.sp, modifier = Modifier
                                .alpha(0.7f)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}