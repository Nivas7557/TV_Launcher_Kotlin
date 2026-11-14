package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object LauncherBackground {
    private var activeWallpaperView: ImageView? = null
    private var activeAuroraView: AuroraSkyView? = null
    private var baseZoom = 1.08f
    private var baseOffsetY = 0

    fun apply(ctx: Context, root: FrameLayout, lp: LauncherPreferences) {
        val sp = lp.prefs
        val wallpaper = sp.getString(LauncherPreferences.PREF_WALLPAPER, null)
        activeWallpaperView = null
        activeAuroraView = null

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
        } else {
            root.background = null
            val aurora = AuroraSkyView(ctx, lp.tileColor, lp.accentColor)
            root.addView(aurora, 0, FrameLayout.LayoutParams(-1, -1))
            activeAuroraView = aurora
        }
    }

    fun updateThemeAndAccent(root: FrameLayout, lp: LauncherPreferences) {
        if (lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null) == null) {
            activeAuroraView?.updateStyle(lp.tileColor, lp.accentColor)
        }
    }

    fun onScrollParallax(scrollY: Int) {
        val shift = minOf(30f, scrollY * 0.10f)
        activeWallpaperView?.translationY = baseOffsetY - shift
        activeAuroraView?.setParallaxShift(shift)
    }

    private class AuroraSkyView(
        ctx: Context,
        private var themeKey: String,
        private var accentColor: Int
    ) : View(ctx) {

        private val skyPaint = Paint(Paint.DITHER_FLAG)
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 250, 232) }
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(8, 13, 32) }

        private val wavePath = Path()
        private var flowAnim: ValueAnimator? = null
        private var phase = 0f
        private var parallaxY = 0f

        private data class Star(val xRatio: Float, val yRatio: Float, val radiusPx: Float, val offset: Float)

        private val stars = List(45) { i ->
            val rnd = Random(i * 1013 + 7)
            val d = resources.displayMetrics.density
            Star(
                xRatio = rnd.nextFloat(),
                yRatio = rnd.nextFloat() * 0.65f,
                radiusPx = (0.7f + rnd.nextFloat() * 1.1f) * d,
                offset = rnd.nextFloat() * 6.28f
            )
        }

        fun updateStyle(newTheme: String, newAccent: Int) {
            themeKey = newTheme
            accentColor = newAccent
            buildShaders(width, height)
            invalidate()
        }

        fun setParallaxShift(shift: Float) {
            if (parallaxY != shift) {
                parallaxY = shift
                invalidate()
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0 || h <= 0) return
            buildShaders(w, h)

            flowAnim?.cancel()
            flowAnim = ValueAnimator.ofFloat(0f, 6.28318f).apply {
                duration = 28000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        private fun buildShaders(w: Int, h: Int) {
            if (w <= 0 || h <= 0) return
            val baseColors = TileThemePalette.getWallpaperGradient(themeKey)
            shadowPaint.color = Color.rgb(6, 10, 26)

            skyPaint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.rgb(5, 8, 22), baseColors[0], baseColors[2]),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )

            // Soft GPU-accelerated Aurora Glow (Zero CPU Blur cost!)
            glowPaint.shader = RadialGradient(
                w * 0.45f, h * 0.30f, w * 0.65f,
                intArrayOf(
                    Color.argb(62, 42, 225, 168),
                    Color.argb(42, 85, 165, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )

            ribbonPaint.shader = LinearGradient(
                0f, h * 0.18f, 0f, h * 0.56f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(48, 48, 232, 172),
                    Color.argb(38, 120, 110, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.38f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            // 1. Deep Night Sky
            canvas.drawRect(0f, 0f, w, h, skyPaint)

            // 2. Soft Ambient Aurora Glow
            canvas.drawRect(0f, 0f, w, h * 0.75f, glowPaint)

            // 3. Gentle Flowing Aurora Curtain
            wavePath.reset()
            val baseY = h * 0.24f - parallaxY * 0.5f
            val steps = 10
            val stepW = w / steps
            wavePath.moveTo(0f, baseY)
            for (i in 0..steps) {
                val x = i * stepW
                val y = baseY + sin(phase + i * 0.5f) * (h * 0.04f)
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, baseY + h * 0.30f)
            wavePath.lineTo(0f, baseY + h * 0.30f)
            wavePath.close()
            canvas.drawPath(wavePath, ribbonPaint)

            // 4. Twinkling Stars
            for (s in stars) {
                val twinkle = 0.45f + 0.55f * sin(phase * 2f + s.offset)
                starPaint.color = Color.argb((twinkle * 210).toInt(), 225, 240, 255)
                canvas.drawCircle(s.xRatio * w, s.yRatio * h - parallaxY * 0.3f, s.radiusPx, starPaint)
            }

            // 5. Fast GPU Crescent Moon (No saveLayer overhead!)
            val d = resources.displayMetrics.density
            val moonX = w * 0.85f
            val moonY = h * 0.16f - parallaxY * 0.4f
            val moonR = 18f * d
            moonHaloPaint.shader = RadialGradient(
                moonX, moonY, moonR * 3.5f,
                Color.argb(55, 255, 248, 215), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(moonX, moonY, moonR * 3.5f, moonHaloPaint)
            canvas.drawCircle(moonX, moonY, moonR, moonPaint)
            canvas.drawCircle(moonX + moonR * 0.42f, moonY - moonR * 0.20f, moonR * 0.86f, shadowPaint)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            flowAnim?.cancel()
        }
    }
}
