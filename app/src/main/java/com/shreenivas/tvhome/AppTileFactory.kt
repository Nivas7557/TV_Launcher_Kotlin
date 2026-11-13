package com.shreenivas.tvhome

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object AppTileFactory {

    fun create(
        ctx: Context,
        pm: PackageManager,
        app: ResolveInfo,
        cachedLabel: String?,
        cachedIcon: Drawable?,
        isFav: Boolean,
        accentColor: Int,
        badgeMode: String,
        tileShape: String,
        tileColor: String,
        tileHeight: Int,
        onToggleFavorite: (packageName: String, appLabel: String) -> Unit
    ): GlowTileLayout {
        val tile = GlowTileLayout(ctx).apply {
            isFocusable = true
            isClickable = true
            setStyle(accentColor, tileShape, tileColor)
        }

        if (badgeMode != "none" || isFav) {
            val badge = TextView(ctx).apply {
                textSize = 13.5f
                when (badgeMode) {
                    "heart" -> {
                        text = if (isFav) "♥" else "♡"
                        setTextColor(if (isFav) Color.rgb(255, 95, 130) else Color.argb(130, 255, 255, 255))
                    }
                    "blue" -> {
                        text = "●"
                        setTextColor(if (isFav) Color.rgb(92, 184, 255) else Color.argb(120, 92, 184, 255))
                    }
                    else -> {
                        text = if (isFav) "★" else "☆"
                        setTextColor(if (isFav) Color.rgb(255, 200, 42) else Color.argb(115, 255, 255, 255))
                    }
                }
            }
            val bp = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
                topMargin = LauncherUtils.dp(ctx, 9)
                rightMargin = LauncherUtils.dp(ctx, 11)
            }
            tile.addView(badge, bp)
        }

        val isHeart = tileShape == "heart"
        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val sidePad = LauncherUtils.dp(ctx, if (isHeart) 22 else 10)
            val botPad = LauncherUtils.dp(ctx, if (isHeart) 20 else 10)
            setPadding(sidePad, LauncherUtils.dp(ctx, 14), sidePad, botPad)
        }

        val iconSize = minOf(
            LauncherUtils.dp(ctx, if (isHeart) 40 else 48),
            (tileHeight * (if (isHeart) 0.34f else 0.42f)).toInt()
        )
        val icon = ImageView(ctx).apply {
            setImageDrawable(cachedIcon ?: app.loadIcon(pm))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        inner.addView(icon, LinearLayout.LayoutParams(iconSize, iconSize).apply {
            bottomMargin = LauncherUtils.dp(ctx, if (isHeart) 6 else 10)
        })

        val appTitle = cachedLabel ?: app.loadLabel(pm).toString()
        val label = TextView(ctx).apply {
            text = appTitle
            textSize = if (isHeart) 10.5f else 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(label, LinearLayout.LayoutParams(-1, -2))
        tile.addView(inner, FrameLayout.LayoutParams(-1, -1))

        val pkg = app.activityInfo.packageName
        tile.setOnClickListener {
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (launch != null) ctx.startActivity(launch)
        }

        tile.setOnLongClickListener {
            AppActionDialog.show(ctx, pkg, appTitle, isFav, accentColor) {
                onToggleFavorite(pkg, appTitle)
            }
            true
        }

        return tile
    }
}
