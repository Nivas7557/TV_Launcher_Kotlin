package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
        val shift = minOf(28f, scrollY * 0.10f)
        activeWallpaperView?.translationY = baseOffsetY - shift
        activeAuroraView?.setParallaxShift(shift)
    }

    private class AuroraSkyView(
        ctx: Context,
        private var themeKey: String,
        private var accentColor: Int
    ) : View(ctx) {

        private val skyPaint = Paint(Paint.DITHER_FLAG)
        private val auroraGlow1 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val auroraGlow2 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 252, 235) }
        private val moonCutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        private val wavePath = Path()
        private var flowAnim: ValueAnimator? = null
        private var phase = 0f
        private var parallaxY = 0f

        private data class Star(val xRatio: Float, val yRatio: Float, val radiusPx: Float, val offset: Float, val tint: Int)

        private val stars = List(65) { i ->
            val rnd = Random(i * 1013 + 19)
            val d = resources.displayMetrics.density
            val tint = when (i % 4) {
                0 -> Color.rgb(255, 240, 200)
                1 -> Color.rgb(185, 225, 255)
                else -> Color.WHITE
            }
            Star(
                xRatio = rnd.nextFloat(),
                yRatio = rnd.nextFloat() * 0.72f,
                radiusPx = (0.7f + rnd.nextFloat() * 1.3f) * d,
                offset = rnd.nextFloat() * 6.28f,
                tint = tint
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
                duration = 24000L
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

            skyPaint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.rgb(6, 10, 32), baseColors[0], baseColors[2]),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )

            // Vibrant Multi-Color Aurora Borealis Glows (Emerald + Cyan + Royal Violet)
            auroraGlow1.shader = RadialGradient(
                w * 0.30f, h * 0.28f, w * 0.52f,
                intArrayOf(
                    Color.argb(115, 20, 235, 165),
                    Color.argb(65, 35, 175, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP
            )

            auroraGlow2.shader = RadialGradient(
                w * 0.68f, h * 0.34f, w * 0.50f,
                intArrayOf(
                    Color.argb(105, 155, 75, 255),
                    Color.argb(55, 235, 85, 175),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )

            ribbonPaint.shader = LinearGradient(
                0f, h * 0.14f, w.toFloat(), h * 0.52f,
                intArrayOf(
                    Color.argb(0, 35, 235, 175),
                    Color.argb(95, 35, 240, 175),
                    Color.argb(95, 75, 170, 255),
                    Color.argb(85, 175, 85, 255),
                    Color.argb(0, 175, 85, 255)
                ),
                floatArrayOf(0f, 0.25f, 0.52f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            // 1. Rich Midnight Cosmos Sky
            canvas.drawRect(0f, 0f, w, h, skyPaint)

            // 2. Colorful Aurora Ambient Clouds
            canvas.drawRect(0f, 0f, w, h * 0.85f, auroraGlow1)
            canvas.drawRect(0f, 0f, w, h * 0.85f, auroraGlow2)

            // 3. Flowing Aurora Ribbon Curtain
            wavePath.reset()
            val baseY = h * 0.22f - parallaxY * 0.45f
            val steps = 12
            val stepW = w / steps
            wavePath.moveTo(0f, baseY)
            for (i in 0..steps) {
                val x = i * stepW
                val y = baseY + sin(phase + i * 0.5f) * (h * 0.045f) + cos(phase * 0.6f + i * 0.3f) * (h * 0.02f)
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, baseY + h * 0.28f)
            wavePath.lineTo(0f, baseY + h * 0.28f)
            wavePath.close()
            canvas.drawPath(wavePath, ribbonPaint)

            // 4. Twinkling Multi-Tone Stars
            for (s in stars) {
                val twinkle = 0.40f + 0.60f * sin(phase * 2.2f + s.offset)
                starPaint.color = LauncherUtils.withAlpha(s.tint, (twinkle * 235).toInt())
                canvas.drawCircle(s.xRatio * w, s.yRatio * h - parallaxY * 0.3f, s.radiusPx, starPaint)
            }

            // 5. Crescent Moon Positioned Cleanly Below Top Bar (Right Sky) with Silver Moonlight Halo
            val d = resources.displayMetrics.density
            val moonX = w * 0.925f
            val moonY = h * 0.31f - parallaxY * 0.35f
            val moonR = 17f * d

            // Soft Silvery-Blue Moonlight Spread
            moonHaloPaint.shader = RadialGradient(
                moonX, moonY, moonR * 6.5f,
                intArrayOf(
                    Color.argb(120, 235, 245, 255),
                    Color.argb(48, 175, 210, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.38f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(moonX, moonY, moonR * 6.5f, moonHaloPaint)

            // True Transparent Crescent Cutout (No Black Circle!)
            val layerId = canvas.saveLayer(moonX - moonR * 2, moonY - moonR * 2, moonX + moonR * 2, moonY + moonR * 2, null)
            canvas.drawCircle(moonX, moonY, moonR, moonPaint)
            canvas.drawCircle(moonX - moonR * 0.44f, moonY - moonR * 0.20f, moonR * 0.85f, moonCutPaint)
            canvas.restoreToCount(layerId)
        }
        override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
            flowAnim?.cancel()
        }
    }
}
    
