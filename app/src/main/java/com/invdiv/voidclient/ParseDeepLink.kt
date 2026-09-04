package com.invdiv.voidclient

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.invdiv.voidclient.ui.theme.VoidclientTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class ParseDeepLink : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var progress by remember { mutableFloatStateOf(0f) }
            val coroutineScope = rememberCoroutineScope()
            val animatedProgress by
            animateFloatAsState(
                targetValue = progress,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessVeryLow,
                        visibilityThreshold = 1 / 1000f,
                    ),
            )

            val context = LocalContext.current
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    parseUri(intent?.data, context)
                }
            }

            VoidclientTheme() {
                Box(modifier = Modifier.fillMaxSize().background(color = colorScheme.background), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator ()
                }
            }
        }
    }
}

suspend fun parseUri(uri: Uri?, context: Context) {
    if (uri != null) {
        val path = uri.path

        val strs = path?.split("/")?.toList()

        if (strs != null) {
            when(strs[1]) {
                "join" -> {
                    val intent = Intent(context, ChatsListActivity::class.java)

                    intent.putExtra("link", strs[2])
                    AccountManager.currentDeepLink = Pair("link", strs[2])
                    (context as Activity).startActivity(intent)
                }

                ":settings" -> {
                    val navigateMap = when (strs[2]) {
                        "devices" -> "Устройства"
                        else -> ""
                    }
                    val intent = Intent(context, ChatsListActivity::class.java)

                    AccountManager.currentDeepLink = Pair("navigateTo", navigateMap)

                    intent.putExtra("navigateTo", navigateMap)

                    (context as Activity).startActivity(intent)
                }

                else -> {
                    val intent = Intent(context, ChatsListActivity::class.java)
                    (context as Activity).startActivity(intent)
                }
            }

            context.finish()
        }
    }
}