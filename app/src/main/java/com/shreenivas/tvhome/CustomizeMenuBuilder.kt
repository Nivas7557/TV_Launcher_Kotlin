package com.shreenivas.tvhome

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.FocusFinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object CustomizeMenuBuilder {
    private var openSubMenu = ""
    private var lastFocusedTag = ""
    private var ignoreFocusTrigger = false
    private var isAnimating = false

    private var activePanelRef: LinearLayout? = null
    private var activeSubBoxRef: LinearLayout? = null
    private var activeCloseDrawerRef: (() -> Unit)? = null

    fun closeSubMenuWithAnimation(onClosedCallback: (() -> Unit)? = null) {
        if (openSubMenu.isEmpty()) {
            onClosedCallback?.invoke()
            return
        }
        val closedKey = openSubMenu
        val parentCard = activePanelRef?.findViewWithTag<View>("acc_$closedKey")
        val box = activeSubBoxRef

        openSubMenu = ""
        activeSubBoxRef = null
        lastFocusedTag = "acc_$closedKey"

        box?.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        parentCard?.requestFocus()
        onClosedCallback?.invoke()

        if (box != null && activePanelRef != null) {
            isAnimating = true
            val startH = maxOf(1, box.height)
            val lp = box.layoutParams as? ViewGroup.MarginLayoutParams
            val startMargin = lp?.bottomMargin ?: 0

            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 170L
                interpolator = LauncherUtils.SILKY_EASE
                addUpdateListener { anim ->
                    val f = anim.animatedValue as Float
                    box.alpha = f
                    lp?.let {
                        it.height = maxOf(1, (startH * f).toInt())
                        it.bottomMargin = (startMargin * f).toInt()
                        box.layoutParams = it
                    }
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (box.parent == activePanelRef) activePanelRef?.removeView(box)
                        isAnimating = false
                    }
                })
                start()
            }
        }
    }

    fun closeSubMenuAndNavigate(moveDown: Boolean) {
        val closedKey = openSubMenu
        closeSubMenuWithAnimation {
            val panel = activePanelRef ?: return@closeSubMenuWithAnimation
            val parentCard = panel.findViewWithTag<View>("acc_$closedKey") ?: return@closeSubMenuWithAnimation
            if (!moveDown) {
                parentCard.requestFocus()
            } else {
                val next = FocusFinder.getInstance().findNextFocus(panel, parentCard, View.FOCUS_DOWN)
                (next ?: parentCard).requestFocus()
            }
        }
    }

    fun build(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        onLiveRefresh: () -> Unit,
        onRebuildPanel: () -> Unit,
        onEditProfile: () -> Unit,
        onOpenWallpaper: () -> Unit,
        onBackToApps: () -> Unit
    ) {
        activePanelRef = panel
        activeCloseDrawerRef = onBackToApps
        activeSubBoxRef = null
        openSubMenu = ""
        isAnimating = false

        (panel.findFocus()?.tag as? String)?.let { lastFocusedTag = it }

        ignoreFocusTrigger = true
        panel.removeAllViews()
        val widthPct = lp.panelWidthPct
        val narrowMode = widthPct <= 25

        addHeader(ctx, panel, "USER PROFILE & GREETINGS")
        addWideCard(ctx, panel, "card_profile", "👤", "User Profile & Greetings",
            "\"${lp.userName}\" • Custom greeting", "Edit ›", lp.accentColor, onEditProfile)

        addHeader(ctx, panel, "DISPLAY PRESETS")
        addAccordionPair(ctx, panel, lp, narrowMode,
            "tile_size", "📐", "Tile Size", cap(lp.tileSize),
            "panel_width", "◫", "Panel Width", "Dock: $widthPct%", onLiveRefresh)

        addHeader(ctx, panel, "TILE & BADGE STYLING")
        addAccordionPair(ctx, panel, lp, narrowMode,
            "tile_shape", "▢", "Tile Shape", cap(lp.tileShape),
            "icon_badge", "♡", "Icon Badge", cap(lp.badgeMode), onLiveRefresh)

        addHeader(ctx, panel, "GRID & ACCENT")
        addAccordionPair(ctx, panel, lp, narrowMode,
            "cols", "⊞", "Apps Per Row", "${lp.columns} Columns",
            "accent", "✦", "Accent Aura", cap(lp.accentName), onLiveRefresh)

        addHeader(ctx, panel, "LAUNCHER THEMES (6 MODES)")
        val curTheme = lp.tileColor
        for (i in TileThemePalette.NAMES.indices step 2) {
            val row = LinearLayout(ctx).apply { orientation = if (narrowMode) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
            for (j in i until minOf(i + 2, TileThemePalette.NAMES.size)) {
                val key = TileThemePalette.KEYS[j]
                val card = makeThemeCard(ctx, TileThemePalette.NAMES[j], TileThemePalette.SUBTITLES[j], key == curTheme, lp.accentColor).apply {
                    tag = "theme_$key"
                    setOnClickListener {
                        lastFocusedTag = "theme_$key"
                        lp.tileColor = key
                        onLiveRefresh()
                        onRebuildPanel()
                    }
                }
                val cp = if (narrowMode) LinearLayout.LayoutParams(-1, dp(ctx, 48)).apply { if (j == i) bottomMargin = dp(ctx, 6) }
                else LinearLayout.LayoutParams(0, dp(ctx, 50), 1f).apply { if (j == i) rightMargin = dp(ctx, 6) }
                row.addView(card, cp)
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }

        addHeader(ctx, panel, "WALLPAPER & CANVAS")
        addWideCard(ctx, panel, "card_wallpaper", "🖼", "Custom Wallpaper Studio",
            "Scale, zoom & position", "Open ›", lp.accentColor, onOpenWallpaper)

        val backBtn = TextView(ctx).apply {
            tag = "btn_back_apps"
            text = "Back to Apps   ›"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            background = pillBg(ctx, sel = false, foc = false, accent = lp.accentColor)
            setOnFocusChangeListener { v, f ->
                LauncherUtils.applyFocusSpring(v, f, 1.03f, 3f)
                v.background = pillBg(ctx, false, f, lp.accentColor)
            }
            setOnClickListener { activeCloseDrawerRef?.invoke() }
        }
        panel.addView(backBtn, LinearLayout.LayoutParams(-1, dp(ctx, 40)).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 6) })

        if (lastFocusedTag.isNotEmpty()) panel.findViewWithTag<View>(lastFocusedTag)?.requestFocus()
        panel.postDelayed({ ignoreFocusTrigger = false }, 160L)
    }
    private fun addAccordionPair(
        ctx: Context, panel: LinearLayout, lp: LauncherPreferences, narrowMode: Boolean,
        k1: String, ic1: String, t1: String, v1: String,
        k2: String, ic2: String, t2: String, v2: String,
        onLiveRefresh: () -> Unit
    ) {
        val c1 = makeSettingCard(ctx, ic1, t1, v1, lp.accentColor).apply {
            tag = "acc_$k1"
            setOnClickListener { handleAccordionClick(ctx, panel, lp, k1, this, onLiveRefresh) }
        }
        val c2 = makeSettingCard(ctx, ic2, t2, v2, lp.accentColor).apply {
            tag = "acc_$k2"
            setOnClickListener { handleAccordionClick(ctx, panel, lp, k2, this, onLiveRefresh) }
        }
        if (narrowMode) {
            panel.addView(c1, LinearLayout.LayoutParams(-1, dp(ctx, 50)).apply { bottomMargin = dp(ctx, 6) })
            panel.addView(c2, LinearLayout.LayoutParams(-1, dp(ctx, 50)).apply { bottomMargin = dp(ctx, 6) })
        } else {
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(c1, LinearLayout.LayoutParams(0, dp(ctx, 54), 1f).apply { rightMargin = dp(ctx, 6) })
            row.addView(c2, LinearLayout.LayoutParams(0, dp(ctx, 54), 1f))
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }
    }

    private fun handleAccordionClick(
        ctx: Context, panel: LinearLayout, lp: LauncherPreferences,
        key: String, clickedCard: LinearLayout, onLiveRefresh: () -> Unit
    ) {
        if (isAnimating) return
        if (key == openSubMenu) { closeSubMenuWithAnimation(); return }
        if (openSubMenu.isNotEmpty() && activeSubBoxRef != null) {
            panel.removeView(activeSubBoxRef)
            activeSubBoxRef = null
            openSubMenu = ""
        }
        openSubMenu = key
        lastFocusedTag = "pill_${key}_sel"
        val rowOrCard = if (clickedCard.parent == panel) clickedCard else clickedCard.parent as View
        insertSubMenuAt(ctx, panel, lp, key, clickedCard, panel.indexOfChild(rowOrCard) + 1, onLiveRefresh)
    }

    private fun insertSubMenuAt(
        ctx: Context, panel: LinearLayout, lp: LauncherPreferences,
        key: String, parentCard: LinearLayout, index: Int, onLive: () -> Unit
    ) {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 8), dp(ctx, 8), dp(ctx, 8), dp(ctx, 8))
            background = GradientDrawable().apply {
                setColor(Color.argb(195, 16, 24, 46))
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
                setStroke(dp(ctx, 1), Color.argb(110, 255, 255, 255))
            }
        }
        activeSubBoxRef = box

        val (headerTxt, labels, vals) = when (key) {
            "tile_size" -> Triple("SELECT TILE SIZE", listOf("Small", "Medium", "Large"), listOf("small", "medium", "large"))
            "panel_width" -> Triple("PANEL FOOTPRINT", listOf("20%", "25%", "30%", "35%", "40%"), listOf("20", "25", "30", "35", "40"))
            "tile_shape" -> Triple("TILE SILHOUETTE", listOf("Squircle", "Circle", "Heart", "Classic"), listOf("squircle", "circle", "heart", "classic"))
            "icon_badge" -> Triple("CORNER EMBLEM", listOf("None", "Heart", "Blue", "Star"), listOf("none", "heart", "blue", "star"))
            "cols" -> Triple("APPS PER ROW", listOf("4", "5", "6", "7", "8"), listOf("4", "5", "6", "7", "8"))
            else -> Triple("ACCENT AURA", listOf("Royal", "Magenta", "Purple", "Emerald", "Sunset"), listOf("royal", "magenta", "purple", "emerald", "sunset"))
        }

        box.addView(TextView(ctx).apply {
            text = "$headerTxt (◀ / ▶)"
            textSize = 8f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(185, 190, 215, 235))
            setPadding(dp(ctx, 2), 0, 0, dp(ctx, 6))
        })

        val optRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val pills = ArrayList<TextView>()
        var initialSelPill: TextView? = null

        labels.forEachIndexed { i, lbl ->
            val v = vals[i]
            val isSel = isOptSelected(lp, key, v)
            val pill = TextView(ctx).apply {
                tag = if (isSel) "pill_${key}_sel" else "pill_${key}_$v"
                text = lbl
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(2, dp(ctx, 7), 2, dp(ctx, 7))
                isFocusable = true
                background = pillBg(ctx, isSel, isSel, lp.accentColor)
            }
            if (isSel) initialSelPill = pill

            val applySel = {
                if (!ignoreFocusTrigger && !isOptSelected(lp, key, v)) {
                    when (key) {
                        "tile_size" -> lp.tileSize = v
                        "panel_width" -> lp.panelWidthPct = v.toInt()
                        "tile_shape" -> lp.tileShape = v
                        "icon_badge" -> lp.badgeMode = v
                        "cols" -> lp.columns = v.toInt()
                        "accent" -> { lp.accentColor = lp.getAccentColorValue(v); lp.accentName = cap(v) }
                    }
                    pills.forEachIndexed { k, p ->
                        val s = vals[k] == v
                        p.tag = if (s) "pill_${key}_sel" else "pill_${key}_${vals[k]}"
                        p.background = pillBg(ctx, s, p.hasFocus(), lp.accentColor)
                    }
                    updateParentCardValue(parentCard, key, v, lp.accentColor)
                    onLive()
                }
            }

            pill.setOnFocusChangeListener { vw, f ->
                LauncherUtils.applyFocusSpring(vw, f, 1.05f, 2f)
                if (f) applySel() else vw.background = pillBg(ctx, isOptSelected(lp, key, v), false, lp.accentColor)
            }
            pill.setOnClickListener { applySel(); closeSubMenuWithAnimation() }
            pills.add(pill)
            optRow.addView(pill, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i < labels.lastIndex) rightMargin = dp(ctx, 3) })
        }

        box.addView(optRow)
        val bp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) }
        panel.addView(box, minOf(index, panel.childCount), bp)

        ignoreFocusTrigger = true
        (initialSelPill ?: pills.firstOrNull())?.requestFocus()
        box.postDelayed({ ignoreFocusTrigger = false }, 150L)

        box.measure(View.MeasureSpec.makeMeasureSpec(if (panel.width > 0) panel.width else dp(ctx, 260), View.MeasureSpec.AT_MOST), 0)
        val targetH = maxOf(1, box.measuredHeight)
        bp.height = 1; box.alpha = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200L; interpolator = LauncherUtils.SILKY_EASE
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                box.alpha = f; bp.height = maxOf(1, (targetH * f).toInt()); box.layoutParams = bp
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bp.height = ViewGroup.LayoutParams.WRAP_CONTENT; box.layoutParams = bp
                    (panel.parent as? ScrollView)?.let { sv ->
                        sv.post {
                            val diff = (box.bottom + dp(ctx, 12)) - (sv.scrollY + sv.height)
                            if (diff > 0) sv.smoothScrollBy(0, diff)
                        }
                    }
                }
            })
            start()
        }
    }

    private fun updateParentCardValue(card: LinearLayout, key: String, v: String, accent: Int) {
        val mid = card.getChildAt(1) as? LinearLayout ?: return
        val sub = mid.getChildAt(1) as? TextView ?: return
        sub.text = when (key) { "panel_width" -> "Dock: $v%"; "cols" -> "$v Columns"; else -> cap(v) }
        sub.setTextColor(accent)
    }

    private fun isOptSelected(lp: LauncherPreferences, key: String, v: String): Boolean = when (key) {
        "tile_size" -> lp.tileSize == v; "panel_width" -> lp.panelWidthPct == v.toInt()
        "tile_shape" -> lp.tileShape == v; "icon_badge" -> lp.badgeMode == v
        "cols" -> lp.columns == v.toInt(); else -> lp.accentColor == lp.getAccentColorValue(v)
    }

    private fun makeSettingCard(ctx: Context, icon: String, title: String, valTxt: String, accent: Int) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(ctx, 8), dp(ctx, 6), dp(ctx, 6), dp(ctx, 6)); isFocusable = true
        background = pillBg(ctx, false, false, accent)
        setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.02f, 3f); v.background = pillBg(ctx, false, f, accent) }
        addView(TextView(ctx).apply { text = icon; textSize = 12f; setTextColor(accent); setPadding(0, 0, dp(ctx, 6), 0) })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(TextView(ctx).apply { text = title; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); isSingleLine = true; ellipsize = TextUtils.TruncateAt.END })
        mid.addView(TextView(ctx).apply { text = valTxt; textSize = 8f; setTextColor(accent); isSingleLine = true })
        addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(ctx).apply { text = "›"; textSize = 12f; setTextColor(Color.argb(180, 255, 255, 255)) })
    }

    private fun makeThemeCard(ctx: Context, name: String, sub: String, sel: Boolean, accent: Int) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
      
