package com.shreenivas.tvhome

import android.graphics.Color

object TileThemePalette {
    val NAMES = listOf(
        "Glossy Glass", "Ocean Breeze", "Cyber Neon",
        "Sunset Glow", "Emerald Forest", "Slate Dark"
    )
    val SUBTITLES = listOf(
        "Frosted & Clear", "Cyan & Deep Sea", "Electric & Cyber",
        "Crimson & Amber", "Deep Jungle & Mint", "Graphite & Matte"
    )
    val KEYS = listOf(
        "frost", "sapphire", "amethyst", "rose", "emerald", "gold"
    )

    fun getTopBottomColors(key: String, focused: Boolean): IntArray {
        val aTop = if (focused) 225 else 155
        val aBot = if (focused) 245 else 185
        return when (key) {
            "sapphire" -> intArrayOf(Color.argb(aTop, 42, 132, 224), Color.argb(aBot, 22, 68, 138))
            "emerald" -> intArrayOf(Color.argb(aTop, 36, 165, 128), Color.argb(aBot, 16, 86, 68))
            "rose" -> intArrayOf(Color.argb(aTop, 205, 76, 124), Color.argb(aBot, 118, 36, 72))
            "amethyst" -> intArrayOf(Color.argb(aTop, 132, 82, 218), Color.argb(aBot, 68, 38, 128))
            "gold" -> intArrayOf(Color.argb(aTop, 64, 74, 96), Color.argb(aBot, 36, 42, 58))
            else -> intArrayOf(Color.argb(aTop, 68, 96, 178), Color.argb(aBot, 42, 62, 124))
        }
    }

    fun getWallpaperGradient(key: String): IntArray {
        return when (key) {
            "sapphire" -> intArrayOf(Color.rgb(16, 56, 112), Color.rgb(24, 86, 148), Color.rgb(12, 34, 72))
            "emerald" -> intArrayOf(Color.rgb(14, 66, 58), Color.rgb(22, 96, 80), Color.rgb(10, 40, 36))
            "rose" -> intArrayOf(Color.rgb(74, 24, 58), Color.rgb(106, 34, 76), Color.rgb(42, 14, 34))
            "amethyst" -> intArrayOf(Color.rgb(42, 28, 96), Color.rgb(66, 42, 134), Color.rgb(24, 18, 58))
            "gold" -> intArrayOf(Color.rgb(26, 30, 42), Color.rgb(40, 46, 62), Color.rgb(18, 20, 30))
            else -> intArrayOf(Color.rgb(28, 48, 118), Color.rgb(44, 72, 158), Color.rgb(18, 32, 84))
        }
    }
}
