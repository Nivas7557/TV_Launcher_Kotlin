package com.shreenivas.tvhome

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object WifiPanelBuilder {

    fun build(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onRefresh: () -> Unit) {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiOn = try { wm?.isWifiEnabled ?: true } catch (_: Exception) { true }

        // 1. Top Wi-Fi ON/OFF Toggle Row
        val toggleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
            isFocusable = true
            isClickable = true
            background = glassCardBg(ctx, false, lp.accentColor)
            setOnFocusChangeListener { v, f ->
                LauncherUtils.applyFocusSpring(v, f, 1.02f, 4f)
                v.background = glassCardBg(ctx, f, lp.accentColor)
            }
            
            setOnClickListener {
                @Suppress("DEPRECATION")
                val changed = try {
                    wm?.setWifiEnabled(!isWifiOn) ?: false
                } catch (_: Exception) {
                    false
                }
                if (changed) {
                    panel.postDelayed({ onRefresh() }, 500L)
                } else {
                    openSystemWifi(ctx)
                }
            }
        }
        toggleRow.addView(makeText(ctx, "Wi-Fi", 13.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))

        val switchTrack = LinearLayout(ctx).apply {
            gravity = if (isWifiOn) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 3), dp(ctx, 3), dp(ctx, 3), dp(ctx, 3))
            background = GradientDrawable().apply {
                setColor(if (isWifiOn) lp.accentColor else Color.argb(80, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
            }
        }
        val switchThumb = View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
        }
        switchTrack.addView(switchThumb, LinearLayout.LayoutParams(dp(ctx, 18), dp(ctx, 18)))
        toggleRow.addView(switchTrack, LinearLayout.LayoutParams(dp(ctx, 44), dp(ctx, 24)))
        panel.addView(toggleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 12) })

        // 2. Connected Network Section
        val connectedSsid = getConnectedSsid(ctx, wm)
        if (isWifiOn && connectedSsid != null) {
            panel.addView(
                makeText(ctx, "Connected network", 10f, Color.argb(190, 210, 230, 245), true),
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6); leftMargin = dp(ctx, 4) }
            )

            @Suppress("DEPRECATION")
            val info = wm?.connectionInfo
            val speed = info?.linkSpeed ?: -1
            val freq = info?.frequency ?: 0
            val bandTxt = if (freq > 4900) "5 GHz" else if (freq > 2400) "2.4 GHz" else "Wi-Fi"
            val statusSub = if (speed > 0) "Connected • $bandTxt • $speed Mbps" else "Connected • $bandTxt"

            val connCard = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
                isFocusable = true
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.argb(85, 34, 155, 105), Color.argb(55, 20, 95, 65))
                ).apply {
                    cornerRadius = LauncherUtils.dpF(ctx, 14f)
                    setStroke(dp(ctx, 1), Color.rgb(46, 232, 138))
                }
                setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.02f, 4f) }
                setOnClickListener { openSystemWifi(ctx) }
            }
            connCard.addView(makeText(ctx, "✓", 14f, Color.rgb(46, 232, 138), true).apply { setPadding(0, 0, dp(ctx, 10), 0) })
            val cMid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            cMid.addView(makeText(ctx, connectedSsid, 11.5f, Color.WHITE, true))
            cMid.addView(makeText(ctx, statusSub, 8.5f, Color.rgb(160, 245, 205), false))
            connCard.addView(cMid, LinearLayout.LayoutParams(0, -2, 1f))
            connCard.addView(makeText(ctx, "📶", 13f, Color.rgb(46, 232, 138), false))
            panel.addView(connCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 14) })
        }

        // 3. Other Networks Header + Refresh Button
        if (isWifiOn) {
            val headRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 4), 0, dp(ctx, 4), dp(ctx, 8))
            }
            headRow.addView(makeText(ctx, "Other networks", 10.5f, Color.argb(200, 210, 225, 245), true))
            headRow.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
            val refBtn = makeText(ctx, "Refresh", 10.5f, lp.accentColor, true).apply {
                isFocusable = true
                setPadding(dp(ctx, 8), dp(ctx, 4), dp(ctx, 8), dp(ctx, 4))
                background = glassCardBg(ctx, false, lp.accentColor)
                setOnFocusChangeListener { v, f ->
                    LauncherUtils.applyFocusSpring(v, f, 1.05f, 3f)
                    v.background = glassCardBg(ctx, f, lp.accentColor)
                }
                setOnClickListener {
                    Toast.makeText(ctx, "Scanning networks...", Toast.LENGTH_SHORT).show()
                    onRefresh()
                }
            }
            headRow.addView(refBtn)
            panel.addView(headRow)

            loadRealWifi(ctx, panel, wm, connectedSsid, lp.accentColor)
        } else {
            val offMsg = makeText(ctx, "Turn on Wi-Fi to view available networks", 10f, Color.argb(170, 200, 220, 240), false).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(ctx, 24), 0, dp(ctx, 24))
            }
            panel.addView(offMsg, LinearLayout.LayoutParams(-1, -2))
        }

        // 4. Bottom "More settings" Action Link
        val moreBtn = makeText(ctx, "More settings", 11.5f, lp.accentColor, true).apply {
            gravity = Gravity.CENTER
            isFocusable = true
            setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12))
            background = glassCardBg(ctx, false, lp.accentColor)
            setOnFocusChangeListener { v, f ->
                LauncherUtils.applyFocusSpring(v, f, 1.03f, 4f)
                v.background = glassCardBg(ctx, f, lp.accentColor)
            }
            setOnClickListener { openSystemWifi(ctx) }
        }
        panel.addView(moreBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 12); bottomMargin = dp(ctx, 6) })
    }
    @Suppress("DEPRECATION")
    fun getConnectedSsid(ctx: Context, wm: WifiManager?): String? {
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork ?: return null
            val caps = cm.getNetworkCapabilities(net) ?: return null
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            ) return null

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Wired Ethernet"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = caps.transportInfo as? WifiInfo
                val ssidQ = wifiInfo?.ssid?.replace("\"", "")
                if (!ssidQ.isNullOrEmpty() && ssidQ != "<unknown ssid>") return ssidQ
            }

            val legacySsid = wm?.connectionInfo?.ssid?.replace("\"", "")
            if (!legacySsid.isNullOrEmpty() && legacySsid != "<unknown ssid>") return legacySsid

            return "Wi-Fi Connected"
        } catch (_: Exception) {
            return null
        }
    }

    @Suppress("DEPRECATION")
    private fun loadRealWifi(
        ctx: Context, panel: LinearLayout, wm: WifiManager?,
        connectedSsid: String?, accent: Int
    ) {
        try {
            wm?.startScan()
            val results: List<ScanResult> = wm?.scanResults ?: emptyList()
            val seen = HashSet<String>()
            if (connectedSsid != null) seen.add(connectedSsid)

            var count = 0
            for (sr in results) {
                val ssid = sr.SSID ?: continue
                if (ssid.isEmpty() || !seen.add(ssid)) continue
                val band = if (sr.frequency > 4900) "5 GHz" else "2.4 GHz"
                val locked = sr.capabilities?.contains("WPA") == true || sr.capabilities?.contains("WEP") == true
                val level = WifiManager.calculateSignalLevel(sr.level, 5)
                addWifiRow(ctx, panel, ssid, "Signal $level/4 • $band", locked, accent)
                if (++count >= 6) break
            }

            if (count == 0) {
                val emptyHint = makeText(
                    ctx,
                    "Tap Refresh or More settings to connect",
                    9.5f,
                    Color.argb(175, 200, 220, 240),
                    false
                ).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(ctx, 14), 0, dp(ctx, 14))
                }
                panel.addView(emptyHint, LinearLayout.LayoutParams(-1, -2))
            }
        } catch (_: SecurityException) {
            addWifiRow(ctx, panel, "Location permission needed", "Allow Location to scan nearby Wi-Fi", false, accent)
        }
    }

    private fun addWifiRow(ctx: Context, panel: LinearLayout, ssid: String, sub: String, locked: Boolean, accent: Int) {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            isFocusable = true
            background = glassCardBg(ctx, false, accent)
            setOnFocusChangeListener { v, f ->
                LauncherUtils.applyFocusSpring(v, f, 1.02f, 4f)
                v.background = glassCardBg(ctx, f, accent)
            }
            setOnClickListener { openSystemWifi(ctx) }
        }
        card.addView(makeText(ctx, "📶", 13f, accent, true).apply { setPadding(0, 0, dp(ctx, 10), 0) })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(makeText(ctx, ssid, 11f, Color.WHITE, true))
        mid.addView(makeText(ctx, sub, 8.5f, Color.argb(185, 210, 230, 245), false))
        card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        if (locked) card.addView(makeText(ctx, "🔒", 11f, Color.WHITE, false))
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
    }

    private fun openSystemWifi(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ctx.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                return
            } catch (_: Exception) {
            }
        }
        try {
            ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (_: Exception) {
        }
    }

    private fun glassCardBg(ctx: Context, foc: Boolean, accent: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (foc) intArrayOf(Color.argb(120, 255, 255, 255), Color.argb(80, 255, 255, 255))
        else intArrayOf(Color.argb(54, 255, 255, 255), Color.argb(28, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (foc) 2 else 1), if (foc) accent else Color.argb(65, 255, 255, 255))
    }

    private fun makeText(ctx: Context, txt: String, sz: Float, col: Int, bold: Boolean) = TextView(ctx).apply {
        text = txt
        textSize = sz
        setTextColor(col)
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
