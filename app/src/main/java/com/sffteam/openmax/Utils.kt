package com.sffteam.openmax

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

object Utils {
    lateinit var windowSize : WindowSizeClass
    fun getColorForAvatar(avatar: String): Pair<Color, Color> {
        val colors = listOf(
            Pair(Color(0xFFFF0026), Color(0xFFFF00BB)),
            Pair(Color(0xFFFFC004), Color(0xFFFFE59F)),
            Pair(Color(0xFF0A5BC2), Color(0xFF3B8FFF)),
            Pair(Color(0xFF04C715), Color(0xFF6AFC78)),
            Pair(Color(0xFFA308C4), Color(0xFFE071FC)),
        )

        val index = (avatar.hashCode().absoluteValue) % colors.size

        return colors[index]
    }
}