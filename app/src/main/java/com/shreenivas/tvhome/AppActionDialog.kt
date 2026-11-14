package com.shreenivas.tvhome

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object AppActionDialog {

    fun show(
        ctx: Context,
        pkg: String,
        appName: String,
        isFav: Boolean,
        canMoveLeft: Boolean,
        canMoveRight: Boolean,
        accent: Int,
        onToggleFav: () -> Unit,
        onMoveFav: (Int) -> Unit,
        onHideApp: () -> Unit
    ) {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = LauncherUtils.dp(ctx, 20)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(245, 22, 36, 62), Color.argb(248, 12, 20, 38))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 22f)
                setStroke(LauncherUtils.dp(ctx, 1), Color.argb(105, 255, 255, 255))
            }
        }

        val title = TextView(ctx).apply {
            text = appName
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(LauncherUtils.dp(ctx, 4), 0, 0, LauncherUtils.dp(ctx, 14))
        }
        box.addView(title)

        val dialog = AlertDialog.Builder(ctx).setView(box).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        if (isFav && (canMoveLeft || canMoveRight)) {
            val moveRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            if (canMoveLeft) {
                val leftBtn = makeOptionBtn(ctx, "⬅  Move Left", accent) { dialog.dismiss(); onMoveFav(-1) }
                moveRow.addView(leftBtn, LinearLayout.LayoutParams(0, LauncherUtils.dp(ctx, 42), 1f).apply {
                    if (canMoveRight) rightMargin = LauncherUtils.dp(ctx, 8)
                })
            }
            if (canMoveRight) {
                val rightBtn = makeOptionBtn(ctx, "Move Right  ➡", accent) { dialog.dismiss(); onMoveFav(1) }
                moveRow.addView(rightBtn, LinearLayout.LayoutParams(0, LauncherUtils.dp(ctx, 42), 1f))
            }
            box.addView(moveRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = LauncherUtils.dp(ctx, 9) })
        }

        addOption(ctx, box, if (isFav) "★  Remove from Favorites" else "★  Add to Favorites", accent) {
            dialog.dismiss(); onToggleFav()
        }

        addOption(ctx, box, "👁  Hide from Home", Color.rgb(175, 195, 235)) {
            dialog.dismiss(); onHideApp()
        }

        addOption(ctx, box, "ℹ  App Info & Storage", accent) {
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
            if (intent.resolveActivity(ctx.packageManager) != null) ctx.startActivity(intent)
        }

        addOption(ctx, box, "🗑  Uninstall App", Color.rgb(255, 95, 105)) {
            dialog.dismiss()
            val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null))
            if (intent.resolveActivity(ctx.packageManager) != null) ctx.startActivity(intent)
        }

        box.scaleX = 0.90f; box.scaleY = 0.90f; box.alpha = 0f
        dialog.show()
        box.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(220L).setInterpolator(LauncherUtils.FLUID_SPRING).start()
    }

    private fun makeOptionBtn(ctx: Context, label: String, accent: Int, action: () -> Unit) = Button(ctx).apply {
        text = label; isAllCaps = false; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setPadding(LauncherUtils.dp(ctx, 10), 0, LauncherUtils.dp(ctx, 10), 0); isFocusable = true
        background = pillBg(ctx, false, accent)
        setOnFocusChangeListener { v, foc -> LauncherUtils.applyFocusSpring(v, foc, 1.03f, 4f); v.background = pillBg(ctx, foc, accent) }
        setOnClickListener { action() }
    }

    private fun addOption(ctx: Context, box: LinearLayout, label: String, accent: Int, action: () -> Unit) {
        val btn = makeOptionBtn(ctx, label, accent, action).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(LauncherUtils.dp(ctx, 16), 0, LauncherUtils.dp(ctx, 16), 0)
        }
        box.addView(btn, LinearLayout.LayoutParams(-1, LauncherUtils.dp(ctx, 44)).apply { bottomMargin = LauncherUtils.dp(ctx, 9) })
    }

    private fun pillBg(ctx: Context, focused: Boolean, accent: Int) = GradientDrawable().apply {
        setColor(if (focused) Color.argb(95, 255, 255, 255) else Color.argb(36, 255, 255, 255))
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(LauncherUtils.dp(ctx, if (focused) 2 else 1), if (focused) accent else Color.argb(55, 255, 255, 255))
    }
}
