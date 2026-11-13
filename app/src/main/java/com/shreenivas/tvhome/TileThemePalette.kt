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
        val aTop = if (focused) 205 else 145
        val aBot = if (focused) 235 else 175
        return when (key) {
            "sapphire" -> intArrayOf(Color.argb(aTop, 36, 118, 196), Color.argb(aBot, 18, 56, 112))
            "emerald" -> intArrayOf(Color.argb(aTop, 32, 148, 114), Color.argb(aBot, 14, 74, 60))
            "rose" -> intArrayOf(Color.argb(aTop, 188, 68, 112), Color.argb(aBot, 104, 32, 64))
            "amethyst" -> intArrayOf(Color.argb(aTop, 118, 72, 198), Color.argb(aBot, 60, 34, 112))
            "gold" -> intArrayOf(Color.argb(aTop, 58, 66, 84), Color.argb(aBot, 32, 38, 50))
            else -> intArrayOf(Color.argb(aTop, 74, 88, 148), Color.argb(aBot, 44, 52, 98))
        }
    }

    fun getWallpaperGradient(key: String): IntArray {
        return when (key) {
            "sapphire" -> intArrayOf(Color.rgb(14, 48, 92), Color.rgb(22, 74, 128), Color.rgb(10, 28, 58))
            "emerald" -> intArrayOf(Color.rgb(12, 58, 52), Color.rgb(20, 86, 72), Color.rgb(8, 34, 30))
            "rose" -> intArrayOf(Color.rgb(66, 22, 52), Color.rgb(94, 30, 68), Color.rgb(36, 12, 30))
            "amethyst" -> intArrayOf(Color.rgb(38, 26, 84), Color.rgb(58, 38, 118), Color.rgb(22, 16, 52))
            "gold" -> intArrayOf(Color.rgb(24, 28, 38), Color.rgb(36, 42, 56), Color.rgb(16, 18, 26))
            else -> intArrayOf(Color.rgb(34, 42, 92), Color.rgb(48, 56, 116), Color.rgb(22, 28, 66))
        }
    }
}
