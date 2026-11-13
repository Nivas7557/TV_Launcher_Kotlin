package com.shreenivas.tvhome

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object WallpaperEditorPanel {

    fun build(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        onLiveUpdate: () -> Unit,
        onPickNewImage: () -> Unit,
        onDone: () -> Unit
    ) {
        addSection(ctx, panel, "SCALE MODE")
        addRowButtons(ctx, panel, lp, listOf("Fit Screen", "Fill Width", "Fill Height")) { idx ->
            val modes = listOf("fit", "width", "height")
            lp.prefs.edit().putString("wp_scale_mode", modes[idx]).apply()
            onLiveUpdate()
        }

        addSection(ctx, panel, "ZOOM")
        addRowButtons(ctx, panel, lp, listOf("Zoom -", "Reset", "Zoom +")) { idx ->
            val sp = lp.prefs
            var z = sp.getFloat("wp_zoom", 1.0f)
            z = when (idx) {
                0 -> maxOf(1.0f, z - 0.1f)
                1 -> 1.08f
                else -> minOf(2.2f, z + 0.1f)
            }
            sp.edit().putFloat("wp_zoom", z).apply()
            onLiveUpdate()
        }

        addSection(ctx, panel, "POSITION MOVE")
        addRowButtons(ctx, panel, lp, listOf("⬅ Left", "⬆ Up", "⬇ Down", "➡ Right")) { idx ->
            val sp = lp.prefs
            var ox = sp.getInt("wp_offset_x", 0)
            var oy = sp.getInt("wp_offset_y", 0)
            val step = LauncherUtils.dp(ctx, 24)
            when (idx) {
                0 -> ox -= step
                1 -> oy -= step
                2 -> oy += step
                else -> ox += step
            }
            sp.edit().putInt("wp_offset_x", ox).putInt("wp_offset_y", oy).apply()
            onLiveUpdate()
        }

        val changeBtn = makeBtn(ctx, lp, "🖼  Choose Another Image").apply {
            setOnClickListener { onPickNewImage() }
        }
        panel.addView(changeBtn, LinearLayout.LayoutParams(-1, LauncherUtils.dp(ctx, 38)).apply {
            topMargin = LauncherUtils.dp(ctx, 10)
        })

        val resetWpBtn = makeBtn(ctx, lp, "↺  Use Default Theme Gradient").apply {
            setOnClickListener {
                lp.prefs.edit().remove(LauncherPreferences.PREF_WALLPAPER).apply()
                onLiveUpdate()
                onDone()
            }
        }
        panel.addView(resetWpBtn, LinearLayout.LayoutParams(-1, LauncherUtils.dp(ctx, 38)).apply {
            topMargin = LauncherUtils.dp(ctx, 8)
        })

        val apply = makeBtn(ctx, lp, "✓  Apply & Save").apply {
            setOnClickListener { onDone() }
        }
        panel.addView(apply, LinearLayout.LayoutParams(-1, LauncherUtils.dp(ctx, 40)).apply {
            topMargin = LauncherUtils.dp(ctx, 10)
        })
    }

    private fun addRowButtons(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        labels: List<String>,
        onClick: (Int) -> Unit
    ) {
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        labels.forEachIndexed { i, text ->
            val b = makeBtn(ctx, lp, text).apply { setOnClickListener { onClick(i) } }
            val p = LinearLayout.LayoutParams(0, LauncherUtils.dp(ctx, 36), 1f).apply {
                if (i < labels.lastIndex) rightMargin = LauncherUtils.dp(ctx, 5)
            }
            row.addView(b, p)
        }
        panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = LauncherUtils.dp(ctx, 6) })
    }

    private fun makeBtn(ctx: Context, lp: LauncherPreferences, label: String) = Button(ctx).apply {
        text = label
        isAllCaps = false
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        setPadding(4, 0, 4, 0)
        isFocusable = true
        background = btnBg(ctx, false, lp.accentColor)
        setOnFocusChangeListener { v, f ->
            LauncherUtils.applyFocusSpring(v, f, 1.03f, 3f)
            v.background = btnBg(ctx, f, lp.accentColor)
        }
    }

    private fun btnBg(ctx: Context, focused: Boolean, accent: Int) = GradientDrawable().apply {
        setColor(if (focused) Color.argb(95, 255, 255, 255) else Color.argb(42, 255, 255, 255))
        cornerRadius = LauncherUtils.dpF(ctx, 10f)
        setStroke(LauncherUtils.dp(ctx, if (focused) 2 else 1), if (focused) accent else Color.argb(65, 255, 255, 255))
    }

    private fun addSection(ctx: Context, panel: LinearLayout, label: String) {
        val t = TextView(ctx).apply {
            text = label
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setTextColor(Color.rgb(110, 215, 255))
        }
        panel.addView(t, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = LauncherUtils.dp(ctx, 8)
            bottomMargin = LauncherUtils.dp(ctx, 5)
        })
    }
}
