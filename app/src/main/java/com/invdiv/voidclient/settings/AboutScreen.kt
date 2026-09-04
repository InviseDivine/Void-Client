package com.invdiv.voidclient.settings

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.invdiv.voidclient.BuildConfig;

@Composable
fun AboutScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box(modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .background(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(start = 8.dp, end = 8.dp)
                .fillMaxWidth()
            ) {
                Column() {
                    Text("Версия приложения")
                    Text(BuildConfig.VERSION_NAME, color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }


        item {
            Box(modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .background(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(start = 8.dp, end = 8.dp)
                .fillMaxWidth()
            ) {
                Column() {
                    Text("Github и Telegram")

                    Text(buildAnnotatedString {withLink(LinkAnnotation.Url("https://github.com/InviseDivine/Void-Client",)) { append("https://github.com/InviseDivine/Void-Client") } },
                        color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)

                    Text(buildAnnotatedString {withLink(LinkAnnotation.Url("https://t.me/max_voidclient",)) { append("https://t.me/max_voidclient") } },
                        color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Box(modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .background(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(start = 8.dp, end = 8.dp)
                .fillMaxWidth()
            ) {
                Column() {
                    Text("Причастные к разработке")
                    Text("InviseDivine - погроммист (примерно всего приложения)", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("Kolyah35, Jaan, DeL - тестеры", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Box(modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .background(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(start = 8.dp, end = 8.dp)
                .fillMaxWidth()
            ) {
                Column() {
                    Text("Отдельное спасибо")
                    Text("PyMax", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("TeamKomet", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("FullHarmony", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("Irishka_Piper", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("a555lieva", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("njuyse", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("CITRIM", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("amamehanik", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                    Text("Андрей WinUpdate", color = colorScheme.onPrimaryContainer.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}