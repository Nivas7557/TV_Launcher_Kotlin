package com.shreenivas.tvhome

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.PathInterpolator
import kotlin.math.roundToInt

object LauncherUtils {
    val FLUID_SPRING = PathInterpolator(0.2f, 1.25f, 0.35f, 1f)
    val SILKY_EASE = PathInterpolator(0.16f, 1.0f, 0.30f, 1f)

    fun addApps(pm: PackageManager, category: String, out: MutableMap<String, ResolveInfo>) {
        val query = Intent(Intent.ACTION_MAIN).apply { addCategory(category) }
        val list = pm.queryIntentActivities(query, 0)
        for (app in list) {
            val pkg = app.activityInfo?.packageName ?: continue
            val name = app.activityInfo?.name ?: continue
            if (pkg == "com.shreenivas.tvhome") continue
            out["$pkg/$name"] = app
        }
    }

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

    fun dpF(context: Context, value: Float): Float {
        return value * context.resources.displayMetrics.density
    }

    fun createGlassDrawable(
        ctx: Context,
        bgColor: Int,
        strokeColor: Int,
        radiusDp: Float,
        strokeDp: Float = 1f
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = dpF(ctx, radiusDp)
            setStroke(dpF(ctx, strokeDp).roundToInt(), strokeColor)
        }
    }

    fun applyFocusSpring(view: View, focused: Boolean, scale: Float = 1.04f, elevationDp: Float = 6f) {
        view.animate().cancel()
        view.animate()
            .scaleX(if (focused) scale else 1f)
            .scaleY(if (focused) scale else 1f)
            .translationZ(if (focused) dpF(view.context, elevationDp) else 0f)
            .setDuration(if (focused) 180L else 130L)
            .setInterpolator(FLUID_SPRING)
            .start()
    }

    fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
