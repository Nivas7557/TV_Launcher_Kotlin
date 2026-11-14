package com.shreenivas.tvhome

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

object AppGridManager {
    private var cachedApps: List<ResolveInfo>? = null
    private val iconCache = HashMap<String, Drawable>()
    private val labelCache = HashMap<String, String>()

    fun clearCache() {
        cachedApps = null
        iconCache.clear()
        labelCache.clear()
    }

    fun getAppLabel(pkg: String): String = labelCache[pkg] ?: pkg

    fun createHeader(
        ctx: Context,
        title: String,
        isFavorite: Boolean,
        lp: LauncherPreferences? = null,
        onSortToggle: (() -> Unit)? = null
    ): LinearLayout {
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 4), 0, dp(ctx, 4), dp(ctx, 8))
        }

        val titleTv = TextView(ctx).apply {
            text = title
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setTextColor(if (isFavorite) Color.rgb(120, 210, 255) else Color.argb(225, 225, 240, 255))
        }
        header.addView(titleTv)
        header.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))

        if (!isFavorite && lp != null && onSortToggle != null) {
            val sortBtn = TextView(ctx).apply {
                text = if (lp.isSortAscending) "⇅  A-Z ORDER" else "⇅  Z-A ORDER"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.05f
                setTextColor(Color.WHITE)
                setPadding(dp(ctx, 12), dp(ctx, 5), dp(ctx, 12), dp(ctx, 5))
                isFocusable = true
                isClickable = true
                background = chipBg(ctx, false, lp.accentColor)
                setOnFocusChangeListener { v, f ->
                    LauncherUtils.applyFocusSpring(v, f, 1.06f, 4f)
                    v.background = chipBg(ctx, f, lp.accentColor)
                }
                setOnClickListener {
                    lp.isSortAscending = !lp.isSortAscending
                    text = if (lp.isSortAscending) "⇅  A-Z ORDER" else "⇅  Z-A ORDER"
                    onSortToggle()
                }
            }
            header.addView(sortBtn, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(ctx, 10) })
        }

        val countTv = TextView(ctx).apply {
            tag = "count_tv"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(210, 230, 240, 255))
            setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4))
            background = chipBg(ctx, false, Color.WHITE)
        }
        header.addView(countTv)
        return header
    }

    fun populate(
        ctx: Context,
        lp: LauncherPreferences,
        favSection: LinearLayout,
        favRow: LinearLayout,
        favCount: TextView,
        appGrid: LinearLayout,
        allCount: TextView,
        shouldRequestFocus: Boolean,
        restorePkg: String?,
        actions: AppTileFactory.TileActions
    ) {
        appGrid.removeAllViews()
        favRow.removeAllViews()

        val pm = ctx.packageManager
        if (cachedApps == null) {
            val unique = LinkedHashMap<String, ResolveInfo>()
            LauncherUtils.addApps(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique)
            LauncherUtils.addApps(pm, Intent.CATEGORY_LAUNCHER, unique)
            val list = ArrayList(unique.values)
            for (app in list) {
                val pkg = app.activityInfo.packageName
                labelCache[pkg] = app.loadLabel(pm).toString()
                iconCache[pkg] = app.loadIcon(pm)
            }
            cachedApps = list
        }
