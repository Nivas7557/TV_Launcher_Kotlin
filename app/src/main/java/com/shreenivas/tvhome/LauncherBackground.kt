package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView

object LauncherBackground {
    private var activeWallpaperView: ImageView? = null
    private var activeAirflowView: AirflowOverlayView? = null
    private var baseZoom = 1.08f
    private var baseOffsetY = 0

    fun apply(ctx: Context, root: FrameLayout, lp: LauncherPreferences) {
        val sp = lp.prefs
        val wallpaper = sp.getString(LauncherPreferences.PREF_WALLPAPER, null)
        activeWallpaperView = null

        if (wallpaper != null) {
            root.background = null
            val image = ImageView(ctx).apply {
                scaleType = when (sp.getString("wp_scale_mode", "fit")) {
                    "width" -> ImageView.ScaleType.FIT_XY
                    "height" -> ImageView.ScaleType.FIT_CENTER
                    else -> ImageView.ScaleType.CENTER_CROP
                }
                baseZoom = maxOf(1.08f, sp.getFloat("wp_zoom", 1.0f))
                baseOffsetY = sp.getInt("wp_offset_y", 0)
                scaleX = baseZoom
                scaleY = baseZoom
                translationX = sp.getInt("wp_offset_x", 0).toFloat()
                translationY = baseOffsetY.toFloat()
                try {
                    setImageURI(Uri.parse(wallpaper))
                } catch (_: Exception) {
                }
            }
            root.addView(image, 0, FrameLayout.LayoutParams(-1, -1))
            activeWallpaperView = image

            val airflow = AirflowOverlayView(ctx, lp.accentColor)
            root.addView(airflow, 1, FrameLayout.LayoutParams(-1, -1))
            activeAirflowView = airflow
        } else {
            val gradColors = TileThemePalette.getWallpaperGradient(lp.tileColor)
            root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, gradColors)

            val airflow = AirflowOverlayView(ctx, lp.accentColor)
            root.addView(airflow, 0, FrameLayout.LayoutParams(-1, -1))
            activeAirflowView = airflow
        }
    }

    fun updateThemeAndAccent(root: FrameLayout, lp: LauncherPreferences) {
        if (lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null) == null) {
            val gradColors = TileThemePalette.getWallpaperGradient(lp.tileColor)
            root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, gradColors)
        }
        activeAirflowView?.setAccent(lp.accentColor)
    }

    fun onScrollParallax(scrollY: Int) {
        activeWallpaperView?.let {
            val shift = minOf(45f, scrollY * 0.14f)
            it.translationY = baseOffsetY - shift
        }
    }

    private class AirflowOverlayView(ctx: Context, private var accent: Int) : View(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val matrix = Matrix()
        private var shader: LinearGradient? = null
        private var flowAnim: ValueAnimator? = null
        private var shift = 0f

        fun setAccent(newAccent: Int) {
            accent = newAccent
            buildShader(width, height)
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0 || h <= 0) return
            buildShader(w, h)
            flowAnim?.cancel()
            flowAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 9500L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    shift = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        private fun buildShader(w: Int, h: Int) {
            if (w <= 0 || h <= 0) return
            val softAccent = LauncherUtils.withAlpha(accent, 42)
            val softCyan = Color.argb(28, 120, 210, 255)
            val darkEdge = Color.argb(72, 8, 12, 28)
            shader = LinearGradient(
                0f, 0f, w * 1.3f, h * 1.1f,
                intArrayOf(darkEdge, softAccent, softCyan, darkEdge),
                floatArrayOf(0f, 0.38f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
        }

        override fun onDraw(canvas: Canvas) {
            shader?.let {
                val dx = (shift - 0.5f) * (width * 0.22f)
                val dy = (shift - 0.5f) * (height * 0.14f)
                matrix.setTranslate(dx, dy)
                it.setLocalMatrix(matrix)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            flowAnim?.cancel()
        }
    }
}
