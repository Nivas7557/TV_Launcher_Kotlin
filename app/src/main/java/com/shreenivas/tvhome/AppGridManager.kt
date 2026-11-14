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
        val hiddenSet = lp.getHiddenApps()
        val visibleApps = (cachedApps ?: emptyList()).filter { !hiddenSet.contains(it.activityInfo.packageName) }

        val comparator = Comparator<ResolveInfo> { a, b ->
            val la = labelCache[a.activityInfo.packageName]?.lowercase(Locale.ROOT) ?: ""
            val lb = labelCache[b.activityInfo.packageName]?.lowercase(Locale.ROOT) ?: ""
            if (lp.isSortAscending) la.compareTo(lb) else lb.compareTo(la)
        }
        val allApps = visibleApps.sortedWith(comparator)

        // Build Favorites in exact user-defined custom order
        val orderedFavPkgs = lp.getOrderedFavorites()
        val appByPkg = visibleApps.associateBy { it.activityInfo.packageName }
        val favApps = orderedFavPkgs.mapNotNull { appByPkg[it] }

        val sizeFactor = when (lp.tileSize) { "small" -> 0.85f; "large" -> 1.15f; else -> 1.0f }
        val effectiveCols = when (lp.tileSize) {
            "small" -> (lp.columns + 1).coerceAtMost(8)
            "large" -> (lp.columns - 1).coerceAtLeast(4)
            else -> lp.columns
        }

        val edgePad = dp(ctx, 10)
        val availW = ctx.resources.displayMetrics.widthPixels - dp(ctx, 72) - (edgePad * 2)
        val gap = dp(ctx, 16)
        val tileW = (availW - (gap * (effectiveCols - 1))) / effectiveCols
        val tileH = (tileW * 0.74f * sizeFactor).toInt()
        var firstFocusTarget: View? = null

        favSection.visibility = View.VISIBLE
        if (favApps.isEmpty()) {
            favRow.visibility = View.GONE
            favCount.text = "Long-press any app to add"
        } else {
            favRow.visibility = View.VISIBLE
            favRow.setPadding(edgePad, edgePad / 2, edgePad, edgePad / 2)
            favCount.text = "${favApps.size} ${if (favApps.size == 1) "App" else "Apps"}"
            favApps.forEachIndexed { i, app ->
                val pkg = app.activityInfo.packageName
                val tile = AppTileFactory.create(
                    ctx, pm, app, labelCache[pkg], iconCache[pkg],
                    true, i, favApps.size, lp.accentColor, lp.badgeMode, lp.tileShape, lp.tileColor, lp.tileSize, tileH, actions
                )
                val tp = LinearLayout.LayoutParams(tileW, tileH).apply { if (i < favApps.lastIndex) rightMargin = gap }
                favRow.addView(tile, tp)
                if (i == 0) firstFocusTarget = tile
            }
        }

        allCount.text = "${allApps.size} ${if (allApps.size == 1) "Apps" else "Apps"}"
        for (start in allApps.indices step effectiveCols) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL; clipChildren = false; clipToPadding = false
                setPadding(edgePad, edgePad / 2, edgePad, edgePad / 2)
            }
            appGrid.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

            val end = minOf(start + effectiveCols, allApps.size)
            for (i in start until end) {
                val app = allApps[i]
                val pkg = app.activityInfo.packageName
                val favIdx = favApps.indexOfFirst { it.activityInfo.packageName == pkg }
                val tile = AppTileFactory.create(
                    ctx, pm, app, labelCache[pkg], iconCache[pkg],
                    favIdx >= 0, favIdx, favApps.size, lp.accentColor, lp.badgeMode, lp.tileShape, lp.tileColor, lp.tileSize, tileH, actions
                )
                val tp = LinearLayout.LayoutParams(tileW, tileH).apply { if (i < end - 1) rightMargin = gap }
                row.addView(tile, tp)
                if (restorePkg == pkg || (firstFocusTarget == null && start == 0 && i == 0)) {
                    firstFocusTarget = tile
                }
            }
        }

        if (shouldRequestFocus) firstFocusTarget?.post { firstFocusTarget?.requestFocus() }
    }

    private fun chipBg(ctx: Context, focused: Boolean, accent: Int) = GradientDrawable().apply {
        setColor(if (focused) Color.argb(90, 255, 255, 255) else Color.argb(36, 255, 255, 255))
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (focused) 2 else 1), if (focused) accent else Color.argb(55, 255, 255, 255))
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
