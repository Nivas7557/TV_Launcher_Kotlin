package com.shreenivas.tvhome

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class SideControlDrawer(
    private val context: Context,
    private val drawerLayout: LinearLayout,
    private val scrim: View,
    private val lp: LauncherPreferences,
    private val onLiveRefresh: () -> Unit,
    private val onWallpaperRefresh: () -> Unit,
    private val onPickWallpaper: () -> Unit
) {
    companion object {
        const val MODE_CUSTOMIZE = "customize"
        const val MODE_QUICK = "quick"
        const val MODE_WIFI = "wifi"
        const val MODE_WALLPAPER_EDIT = "wp_edit"
    }

    var isOpen = false
        private set
    private var currentMode = ""
    private var activeScroll: ScrollView? = null
    private var activePanel: LinearLayout? = null

    fun toggle(mode: String) {
        if (isOpen && mode == currentMode) close() else open(mode)
    }

    fun open(mode: String) {
        val wasOpen = isOpen
        currentMode = mode
        isOpen = true

        drawerLayout.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.argb(244, 18, 28, 52), Color.argb(248, 12, 18, 36))
        ).apply {
            cornerRadius = LauncherUtils.dpF(context, 22f)
            setStroke(dp(1.5f), Color.argb(95, 255, 255, 255))
        }
        drawerLayout.elevation = dp(18f).toFloat()

        scrim.visibility = View.VISIBLE
        drawerLayout.visibility = View.VISIBLE

        setupDrawerSkeleton()
        populateActivePanel(true)

        if (!wasOpen) {
            scrim.alpha = 0f
            scrim.animate().alpha(1f).setDuration(240L).start()

            val startX = drawerLayout.layoutParams.width + dp(36f)
            drawerLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            drawerLayout.translationX = startX.toFloat()
            drawerLayout.alpha = 0.4f
            drawerLayout.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300L)
                .setInterpolator(LauncherUtils.SILKY_EASE)
                .withEndAction { drawerLayout.setLayerType(View.LAYER_TYPE_NONE, null) }
                .start()
        }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        currentMode = ""
        val out = maxOf(dp(340f), drawerLayout.width + dp(36f))

        scrim.animate().alpha(0f).setDuration(200L).start()
        drawerLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        drawerLayout.animate()
            .translationX(out.toFloat())
            .alpha(0f)
            .setDuration(250L)
            .setInterpolator(LauncherUtils.SILKY_EASE)
            .withEndAction {
                drawerLayout.setLayerType(View.LAYER_TYPE_NONE, null)
                scrim.visibility = View.GONE
                drawerLayout.visibility = View.GONE
            }
            .start()
    }

    private fun setupDrawerSkeleton() {
        drawerLayout.removeAllViews()
        drawerLayout.setPadding(dp(14f), dp(14f), dp(14f), dp(14f))

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4f), dp(2f), dp(2f), dp(10f))
        }

        val (icon, title) = when (currentMode) {
            MODE_CUSTOMIZE -> "✦  " to "Customize Launcher"
            MODE_WIFI -> "📶  " to "Network & Internet"
            MODE_WALLPAPER_EDIT -> "🖼  " to "Wallpaper Studio"
            else -> "⚡  " to "Control Center"
        }
        topRow.addView(text("$icon$title", 15f, Color.WHITE, true))
        topRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))

        val closeX = TextView(context).apply {
            text = "✕"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            isFocusable = true; isClickable = true
            background = xBtnBg(false)
            setOnFocusChangeListener { v, f ->
                LauncherUtils.applyFocusSpring(v, f, 1.12f, 4f)
                v.background = xBtnBg(f)
            }
            setOnClickListener { close() }
        }
        topRow.addView(closeX, LinearLayout.LayoutParams(dp(30f), dp(30f)))
        drawerLayout.addView(topRow)

        val divider = View(context).apply { setBackgroundColor(Color.argb(35, 255, 255, 255)) }
        drawerLayout.addView(divider, LinearLayout.LayoutParams(-1, dp(1f)).apply { bottomMargin = dp(8f) })

        activeScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        activePanel = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        drawerLayout.addView(activeScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        activeScroll?.addView(activePanel)
    }
        private fun populateActivePanel(isInitialOpen: Boolean) {
        val panel = activePanel ?: return
        panel.removeAllViews()

        when (currentMode) {
            MODE_CUSTOMIZE -> CustomizeMenuBuilder.build(
                context, panel, lp, onLiveRefresh, { populateActivePanel(false) },
                ::showUserNameDialog,
                {
                    if (lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null) != null) open(MODE_WALLPAPER_EDIT)
                    else onPickWallpaper()
                },
                ::close
            )
            MODE_WIFI -> WifiPanelBuilder.build(context, panel, lp) { populateActivePanel(false) }
            MODE_WALLPAPER_EDIT -> WallpaperEditorPanel.build(context, panel, lp, onWallpaperRefresh, onPickWallpaper) { open(MODE_CUSTOMIZE) }
            else -> buildControlCenter(panel)
        }

        if (isInitialOpen) {
            activeScroll?.apply {
                translationY = dp(10f).toFloat()
                alpha = 0f
                animate().translationY(0f).alpha(1f).setDuration(220L).setInterpolator(LauncherUtils.SILKY_EASE).start()
            }
            panel.post { panel.requestFocus() }
        }
    }

    private fun buildControlCenter(panel: LinearLayout) {
        val sysRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sysRow.addView(text("MEDIA & DISPLAY", 9.5f, lp.accentColor, true))
        sysRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        val sysBtn = text("⚙ System", 10f, Color.WHITE, true).apply {
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            isFocusable = true
            background = btnBg(false)
            setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.05f, 3f); v.background = btnBg(f) }
            setOnClickListener { launch(Settings.ACTION_SETTINGS) }
        }
        sysRow.addView(sysBtn)
        panel.addView(sysRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8f) })

        // 2x2 Quick Hardware Grid (Video Style: TV Input, Picture, Audio Out, Screen Art)
        val gridRow1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        gridRow1.addView(makeGridTile("📺", "TV Input", "HDMI 1") { launch(Settings.ACTION_INPUT_METHOD_SETTINGS) },
            LinearLayout.LayoutParams(0, dp(56f), 1f).apply { rightMargin = dp(6f) })
        gridRow1.addView(makeGridTile("🎨", "Picture", "Vivid HDR") { launch(Settings.ACTION_DISPLAY_SETTINGS) },
            LinearLayout.LayoutParams(0, dp(56f), 1f))
        panel.addView(gridRow1, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6f) })

        val gridRow2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        gridRow2.addView(makeGridTile("🔊", "Audio Out", "TV Speaker") { launch(Settings.ACTION_SOUND_SETTINGS) },
            LinearLayout.LayoutParams(0, dp(56f), 1f).apply { rightMargin = dp(6f) })
        gridRow2.addView(makeGridTile("🖼", "Screen Art", "Ambient") { open(MODE_WALLPAPER_EDIT) },
            LinearLayout.LayoutParams(0, dp(56f), 1f))
        panel.addView(gridRow2, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8f) })

        addLabel(panel, "DISPLAY & SOUND LEVELS")
        addSliderCard(panel, "☀  Display Brightness", 85) { launch(Settings.ACTION_DISPLAY_SETTINGS) }

        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val curVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        val volPct = ((curVol * 100f) / maxOf(1, maxVol)).toInt()
        addSliderCard(panel, "🔊  TV Audio Volume", volPct) { p ->
            am?.setStreamVolume(AudioManager.STREAM_MUSIC, ((p / 100f) * maxVol).toInt(), 0)
        }

        addLabel(panel, "QUICK ACTIONS")
        addActionButton(panel, "📶", "Wi-Fi & Network", "Manage connection") { open(MODE_WIFI) }
        addActionButton(panel, "✦", "Customize Home", "Themes, grid & wallpaper") { open(MODE_CUSTOMIZE) }
        addActionButton(panel, "📱", "Installed Apps", "Storage & permissions") { launch(Settings.ACTION_APPLICATION_SETTINGS) }
    }

    private fun makeGridTile(icon: String, title: String, sub: String, onClick: () -> Unit) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10f), dp(6f), dp(10f), dp(6f)); isFocusable = true; background = btnBg(false)
        setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.03f, 4f); v.background = btnBg(f) }
        setOnClickListener { onClick() }
        addView(text("$icon  $title", 10f, Color.WHITE, true))
        addView(text(sub, 8.5f, lp.accentColor, false).apply { setPadding(0, dp(2f), 0, 0) })
    }

    private fun addSliderCard(panel: LinearLayout, label: String, initialPct: Int, onChange: (Int) -> Unit) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12f), dp(9f), dp(12f), dp(9f)); background = btnBg(false)
        }
        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val pct = text("$initialPct%", 10.5f, lp.accentColor, true)
        top.addView(text(label, 10.5f, Color.WHITE, true))
        top.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(pct)
        card.addView(top)

        val sb = SeekBar(context).apply {
            max = 100; progress = initialPct; isFocusable = true
            setOnFocusChangeListener { _, f -> card.background = btnBg(f) }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { pct.text = "$p%"; if (user) onChange(p) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        card.addView(sb, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5f) })
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7f) })
    }

    private fun showUserNameDialog() {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(8f)) }
        val nameInput = EditText(context).apply {
            isSingleLine = true; hint = "Your Name (e.g. Shree)"; setText(lp.userName)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val phraseInput = EditText(context).apply {
            isSingleLine = true; hint = "Custom Greeting (Empty = Auto Time)"; setText(lp.greetingPhrase)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        box.addView(nameInput)
        box.addView(phraseInput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })

        AlertDialog.Builder(context)
            .setTitle("Profile & Greeting")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                lp.userName = nameInput.text.toString().trim()
                lp.greetingPhrase = phraseInput.text.toString().trim()
                onLiveRefresh()
                populateActivePanel(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addActionButton(panel: LinearLayout, icon: String, title: String, sub: String, act: () -> Unit) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(9f), dp(12f), dp(9f)); isFocusable = true; background = btnBg(false)
            setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.02f, 4f); v.background = btnBg(f) }
            setOnClickListener { act() }
        }
        card.addView(text(icon, 14f, lp.accentColor, true).apply { setPadding(0, 0, dp(10f), 0) })
        val mid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(text(title, 10.5f, Color.WHITE, true))
        mid.addView(text(sub, 8.5f, Color.argb(170, 200, 225, 240), false))
        card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(text("›", 15f, Color.argb(160, 255, 255, 255), true))
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6f) })
    }

    private fun launch(act: String) = try { context.startActivity(Intent(act)) } catch (_: Exception) { Toast.makeText(context, "Unavailable", Toast.LENGTH_SHORT).show() }
    private fun xBtnBg(foc: Boolean) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (foc) Color.argb(150, 255, 75, 85) else Color.argb(45, 255, 255, 255)); setStroke(dp(1f), if (foc) Color.WHITE else Color.argb(70, 255, 255, 255)) }
    private fun btnBg(foc: Boolean) = GradientDrawable().apply { setColor(if (foc) Color.argb(85, 255, 255, 255) else Color.argb(36, 255, 255, 255)); cornerRadius = LauncherUtils.dpF(context, 14f); setStroke(dp(if (foc) 1.8f else 1f), if (foc) lp.accentColor else Color.argb(45, 255, 255, 255)) }
    private fun text(v: String, sz: Float, col: Int, bld: Boolean) = TextView(context).apply { text = v; textSize = sz; setTextColor(col); if (bld) typeface = Typeface.DEFAULT_BOLD }
    private fun addLabel(panel: LinearLayout, lbl: String) = panel.addView(text(lbl.uppercase(Locale.ROOT), 9f, lp.accentColor, true).apply { letterSpacing = 0.09f }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f); bottomMargin = dp(5f) })
    private fun dp(v: Float) = LauncherUtils.dpF(context, v).toInt()
}
