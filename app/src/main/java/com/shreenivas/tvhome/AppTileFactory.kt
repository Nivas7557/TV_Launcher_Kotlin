package com.shreenivas.tvhome

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object AppTileFactory {

    interface TileActions {
        fun onToggleFavorite(pkg: String, label: String)
        fun onMoveFavorite(pkg: String, delta: Int)
        fun onHideApp(pkg: String, label: String)
    }

    fun create(
        ctx: Context,
        pm: PackageManager,
        app: ResolveInfo,
        cachedLabel: String?,
        cachedIcon: Drawable?,
        isFav: Boolean,
        favIndex: Int,
        favTotal: Int,
        accentColor: Int,
        badgeMode: String,
        tileShape: String,
        tileColor: String,
        tileSize: String,
        tileHeight: Int,
        actions: TileActions
    ): GlowTileLayout {
        val tile = GlowTileLayout(ctx).apply {
            isFocusable = true
            isClickable = true
            setStyle(accentColor, tileShape, tileColor)
        }

        val scaleMult = when (tileSize) { "small" -> 0.85f; "large" -> 1.18f; else -> 1.0f }

        if (badgeMode != "none" || isFav) {
            val badge = TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * scaleMult)
                when (badgeMode) {
                    "heart" -> { text = if (isFav) "♥" else "♡"; setTextColor(if (isFav) Color.rgb(255, 95, 130) else Color.argb(130, 255, 255, 255)) }
                    "blue" -> { text = "●"; setTextColor(if (isFav) Color.rgb(92, 184, 255) else Color.argb(120, 92, 184, 255)) }
                    else -> { text = if (isFav) "★" else "☆"; setTextColor(if (isFav) Color.rgb(255, 200, 42) else Color.argb(115, 255, 255, 255)) }
                }
            }
            tile.addView(badge, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
                topMargin = LauncherUtils.dp(ctx, 8); rightMargin = LauncherUtils.dp(ctx, 10)
            })
        }

        val isHeart = tileShape == "heart"
        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            val sidePad = LauncherUtils.dp(ctx, if (isHeart) 20 else 8)
            val botPad = LauncherUtils.dp(ctx, if (isHeart) 18 else 8)
            setPadding(sidePad, LauncherUtils.dp(ctx, 12), sidePad, botPad)
        }

        val iconSize = (tileHeight * (if (isHeart) 0.36f else 0.44f)).toInt().coerceAtLeast(LauncherUtils.dp(ctx, 32))
        val icon = ImageView(ctx).apply {
            setImageDrawable(cachedIcon ?: app.loadIcon(pm))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        inner.addView(icon, LinearLayout.LayoutParams(iconSize, iconSize).apply { bottomMargin = LauncherUtils.dp(ctx, if (isHeart) 5 else 8) })

        val appTitle = cachedLabel ?: app.loadLabel(pm).toString()
        val finalTextSp = ((if (isHeart) 11.5f else 14f) * scaleMult).coerceIn(10.5f, 18.5f)
        val label = TextView(ctx).apply {
            text = appTitle; setTextSize(TypedValue.COMPLEX_UNIT_SP, finalTextSp); typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setShadowLayer(3f, 0f, 1.5f, Color.argb(170, 0, 0, 0))
            gravity = Gravity.CENTER; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(label, LinearLayout.LayoutParams(-1, -2))
        tile.addView(inner, FrameLayout.LayoutParams(-1, -1))

        val pkg = app.activityInfo.packageName
        tile.setOnClickListener {
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (launch != null) ctx.startActivity(launch)
        }

        tile.setOnLongClickListener {
            AppActionDialog.show(
                ctx, pkg, appTitle, isFav,
                canMoveLeft = isFav && favIndex > 0,
                canMoveRight = isFav && favIndex >= 0 && favIndex < favTotal - 1,
                accent = accentColor,
                onToggleFav = { actions.onToggleFavorite(pkg, appTitle) },
                onMoveFav = { delta -> actions.onMoveFavorite(pkg, delta) },
                onHideApp = { actions.onHideApp(pkg, appTitle) }
            )
            true
        }
        return tile
    }
}
