package com.shreenivas.tvhome

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class LauncherPreferences(context: Context) {

    companion object {
        const val PREFS = "launcher_preferences"
        const val PREF_ACCENT = "accent_color"
        const val PREF_ACCENT_NAME = "accent_name"
        const val PREF_TILE_SIZE = "tile_size"
        const val PREF_TILE_SHAPE = "tile_shape"
        const val PREF_TILE_COLOR = "tile_color"
        const val PREF_BADGE = "icon_badge"
        const val PREF_COLUMNS = "grid_columns"
        const val PREF_PANEL_WIDTH = "panel_width_pct"
        const val PREF_WALLPAPER = "wallpaper_uri"
        const val PREF_FAVORITES = "favorites"
        const val PREF_USER_NAME = "launcher_user_name"
        const val PREF_GREETING_PHRASE = "launcher_greeting_phrase"
        const val PREF_SORT_ASC = "sort_ascending"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var accentColor: Int
        get() = prefs.getInt(PREF_ACCENT, Color.rgb(92, 168, 255))
        set(value) = prefs.edit().putInt(PREF_ACCENT, value).apply()

    var accentName: String
        get() = prefs.getString(PREF_ACCENT_NAME, "Royal") ?: "Royal"
        set(value) = prefs.edit().putString(PREF_ACCENT_NAME, value).apply()

    var tileSize: String
        get() = prefs.getString(PREF_TILE_SIZE, "medium") ?: "medium"
        set(value) = prefs.edit().putString(PREF_TILE_SIZE, value).apply()

    var tileShape: String
        get() = prefs.getString(PREF_TILE_SHAPE, "squircle") ?: "squircle"
        set(value) = prefs.edit().putString(PREF_TILE_SHAPE, value).apply()

    var tileColor: String
        get() = prefs.getString(PREF_TILE_COLOR, "frost") ?: "frost"
        set(value) = prefs.edit().putString(PREF_TILE_COLOR, value).apply()

    var badgeMode: String
        get() = prefs.getString(PREF_BADGE, "star") ?: "star"
        set(value) = prefs.edit().putString(PREF_BADGE, value).apply()

    var columns: Int
        get() = prefs.getInt(PREF_COLUMNS, 6).coerceIn(4, 8)
        set(value) = prefs.edit().putInt(PREF_COLUMNS, value.coerceIn(4, 8)).apply()

    var panelWidthPct: Int
        get() = prefs.getInt(PREF_PANEL_WIDTH, 30).coerceIn(20, 40)
        set(value) = prefs.edit().putInt(PREF_PANEL_WIDTH, value.coerceIn(20, 40)).apply()

    var userName: String
        get() = prefs.getString(PREF_USER_NAME, "Shree") ?: "Shree"
        set(value) = prefs.edit().putString(PREF_USER_NAME, value.ifBlank { "Shree" }).apply()

    var greetingPhrase: String
        get() = prefs.getString(PREF_GREETING_PHRASE, "") ?: ""
        set(value) = prefs.edit().putString(PREF_GREETING_PHRASE, value).apply()

    var isSortAscending: Boolean
        get() = prefs.getBoolean(PREF_SORT_ASC, true)
        set(value) = prefs.edit().putBoolean(PREF_SORT_ASC, value).apply()

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(PREF_FAVORITES, emptySet()) ?: emptySet()
    }

    fun setFavorites(favs: Set<String>) {
        prefs.edit().putStringSet(PREF_FAVORITES, HashSet(favs)).apply()
    }

    fun getAccentColorValue(name: String?): Int {
        return when (name?.lowercase()) {
            "magenta" -> Color.rgb(232, 72, 174)
            "purple" -> Color.rgb(165, 105, 255)
            "emerald" -> Color.rgb(46, 190, 132)
            "sunset" -> Color.rgb(255, 145, 75)
            else -> Color.rgb(92, 168, 255)
        }
    }
}
