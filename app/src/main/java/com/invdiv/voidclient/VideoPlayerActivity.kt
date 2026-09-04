package com.invdiv.voidclient

import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.invdiv.voidclient.ui.theme.VoidclientTheme

class VideoPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = intent.getStringExtra("url")

        setContent {
            VoidclientTheme() {
                VideoUI(url ?: "", window)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoUI(url : String, window : Window) {
    val context = LocalContext.current
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setUserAgent("OneExoPlayer/2.24.0 (Linux; Android ${Build.VERSION.RELEASE}) App:PackageName/ru.oneme.app App:Version/26.24.0 AndroidXMedia3/1.9.3")
            setAllowCrossProtocolRedirects(true)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(
            httpDataSourceFactory
        )

        ExoPlayer.Builder(context).apply {
            setMediaSourceFactory(
                mediaSourceFactory
            )
        }.build().apply {
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                exoPlayer.pause()
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier,
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )
    }
}