package com.sffteam.voidclient.preferences

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.sffteam.voidclient.R
import com.sffteam.voidclient.ui.theme.AppTheme
import sh.calvin.autolinktext.rememberAutoLinkText

class AboutActivity : ComponentActivity() {
    val specialThanks = listOf(
        "Kolyah35",
        "CITRIM",
        "DeL",
        "FullHarmony",
        "danilka22ah",
        "njuyse",
        "TeamKomet",
        "a555lieva",
        "Irishka_Piper",
    )

    val developers = mapOf(
        "InviseDivine" to "Разработчик, дизайнер, основатель проекта",
        "Jaan" to "Помощь с разработкой SocketManager'а"
    )

    val infoText = "Void Client - самописный клиент для MAX'а с открытым исходным кодом"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)

        setContent {
            AppTheme() {
                val context = LocalContext.current

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
                                Text("О приложении")
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
                    }) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val packageManager = context.packageManager
                            val appIconDrawable: Drawable =
                                packageManager.getApplicationIcon("com.sffteam.voidclient")

                            Image(
                                appIconDrawable.toBitmap(config = Bitmap.Config.ARGB_8888)
                                    .asImageBitmap(),
                                contentDescription = "Image", modifier = Modifier
                                    .size(100.dp)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .background(
                                        colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 4.dp, end = 4.dp)
                            ) {
                                Column() {
                                    Text(
                                        "О приложении",
                                        fontSize = 24.sp,
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )

                                    Column() {
                                        Text(
                                            infoText,
                                            fontSize = 22.sp,
                                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                        )

                                        Text(
                                            AnnotatedString.rememberAutoLinkText(
                                                "Наши ссылки: \n" +
                                                        "Github - https://github.com/InviseDivine/Void-Client \n" +
                                                        "Telegram - t.me/max_voidclient",
                                            ),
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)

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
                                    .padding(start = 4.dp, top = 4.dp, end = 4.dp)
                            ) {
                                Column() {
                                    Text(
                                        "Разработчики",
                                        fontSize = 24.sp,
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )

                                    for (dev in developers.toList()) {
                                        DrawDevelopers(dev.first, dev.second)
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
                                    .padding(start = 4.dp, top = 4.dp, end = 4.dp)
                            ) {
                                Column() {
                                    Text(
                                        "Отдельная благодарность",
                                        fontSize = 24.sp,
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )

                                    for (people in specialThanks) {
                                        DrawSpecialThanks(people)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawDevelopers(people: String, desc: String) {
    // TODO : Avatars
    Column() {
        Text(
            people,
            fontSize = 22.sp,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 2.dp)
        )

        Text(
            desc,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 4.dp)
                .alpha(0.7f)
        )
    }
}

@Composable
fun DrawSpecialThanks(people: String) {
    Text(
        people,
        fontSize = 20.sp,
        modifier = Modifier
            .padding(start = 4.dp, bottom = 4.dp)
            .alpha(0.8f),
    )
}