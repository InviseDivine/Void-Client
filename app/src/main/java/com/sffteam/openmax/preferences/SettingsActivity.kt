package com.sffteam.openmax.preferences

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sffteam.openmax.AccountManager
import com.sffteam.openmax.ChatActivity
import com.sffteam.openmax.UserManager
import com.sffteam.openmax.ui.theme.AppTheme
import kotlinx.coroutines.launch

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
                    val user = users[AccountManager.accountID]
                    val username = user?.firstName + if (user?.lastName?.isNotEmpty() == true) {
                        " " + user.lastName
                    } else {
                        ""
                    }

                    Column(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth()
                            .padding(top = 5.dp, start = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val coroutineScope = rememberCoroutineScope()
                        val clipboardManager = LocalClipboard.current

                        // Account
                        Column(modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AsyncImage(
                                model = user?.avatarUrl,
                                contentDescription = "ChatIcon",
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(100.dp)
                                    .clip(CircleShape)
                                    .align(Alignment.CenterHorizontally),
                                contentScale = ContentScale.Crop,
                            )

                            Text(
                                text = username,
                                fontSize = 20.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                                text = "Нажмите, чтобы скопировать информацию",
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                    .alpha(0.7f)
                            )
                        }

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

                        // Settings
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
                                    modifier = Modifier.padding(12.dp)
                                        .clickable {
                                            val intent =
                                                Intent(context, ProfileSettingsActivity::class.java)

                                            context.startActivity(intent)
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        "lol",
                                        modifier = Modifier
                                            .size(25.dp)
                                            .padding()
                                    )
                                    Text(
                                        "Настройки OpenMAX", fontSize = 20.sp,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(12.dp)
                                        .clickable {
                                            val intent =
                                                Intent(context, ProfileSettingsActivity::class.java)

                                            context.startActivity(intent)
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ChatBubble,
                                        "lol",
                                        modifier = Modifier
                                            .size(25.dp)
                                            .padding()
                                    )
                                    Text(
                                        "Настройки чатов", fontSize = 20.sp,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                                Row(
                                    modifier = Modifier.padding(12.dp)
                                        .clickable {
                                            val intent =
                                                Intent(context, SecurityActivity::class.java)

                                            context.startActivity(intent)
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        "lol",
                                        modifier = Modifier
                                            .size(25.dp)
                                            .padding()
                                    )
                                    Text(
                                        "Безопасность", fontSize = 20.sp,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(12.dp)
                                        .clickable {
                                            Toast.makeText(context, "Будет доступно в следующих обновлениях", Toast.LENGTH_LONG).show()
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
                                        modifier = Modifier.align(Alignment.CenterVertically).alpha(0.7f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(12.dp)
                                        .clickable {
                                            Toast.makeText(context, "Будет доступно в следующих обновлениях", Toast.LENGTH_LONG).show()
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
                                        modifier = Modifier.align(Alignment.CenterVertically).alpha(0.7f)

                                    )
                                }
                            }
                        }


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
                                    modifier = Modifier.padding(12.dp)
                                        .clickable {
                                            val intent =
                                                Intent(context, ProfileSettingsActivity::class.java)

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
            }
        }
    }
}