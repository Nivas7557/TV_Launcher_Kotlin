package com.shreenivas.tvhome

import android.Manifest
import android.animation.LayoutTransition
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        private const val REQ_PICK_WALLPAPER = 1001
    }

    private lateinit var rootLayout: FrameLayout
    private lateinit var mainPage: LinearLayout
    private lateinit var topNavBar: TopNavBar
    private lateinit var favSection: LinearLayout
    private lateinit var favRow: LinearLayout
    private lateinit var appGrid: LinearLayout
    private lateinit var drawerLayout: LinearLayout
    private lateinit var favCount: TextView
    private lateinit var allCount: TextView
    private lateinit var drawerScrim: View
    private lateinit var lp: LauncherPreferences
    private lateinit var sideDrawer: SideControlDrawer

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppGridManager.clearCache()
            refreshApps(!sideDrawer.isOpen)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = LauncherUtils.calculateAdaptiveScale(newBase)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lp = LauncherPreferences(this)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        buildScreen()
        NightNudgeReceiver.scheduleNextNudge(this)
        rootLayout.post { checkPermissionsWithExplanation() }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pkgReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pkgReceiver, filter)
        }
    }

    private fun checkPermissionsWithExplanation() {
        val needLoc = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        val needOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)
        if (!needLoc && !needOverlay) return

        val msg = buildString {
            append("To give you the best TV experience, Shree TV Home needs:\n\n")
            if (needLoc) append("• Location Access: Required by Android TV to display your connected Wi-Fi network name.\n\n")
            if (needOverlay) append("• Display Over Other Apps: Shows gentle bedtime reminder bubbles at night while watching videos.")
        }

        AlertDialog.Builder(this)
            .setTitle("Optional Permissions")
            .setMessage(msg.trim())
            .setPositiveButton("Allow") { _, _ ->
                if (needLoc) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1002)
                }
                if (needOverlay) {
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    } catch (_: Exception) {
                    }
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pkgReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appGrid.isInitialized) {
            refreshApps(!sideDrawer.isOpen)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sideDrawer.isOpen) {
            sideDrawer.close()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (sideDrawer.isOpen && DrawerFocusTrap.handleKeyEvent(event, true, drawerLayout, topNavBar)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(req, res, data)
        if (req == REQ_PICK_WALLPAPER && res == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }

            lp.prefs.edit()
                .putString(LauncherPreferences.PREF_WALLPAPER, uri.toString())
                .putString("wp_scale_mode", "fit")
                .putFloat("wp_zoom", 1.08f)
                .putInt("wp_offset_x", 0)
                .putInt("wp_offset_y", 0)
                .apply()

            applyWallpaperOnly()
            sideDrawer.open(SideControlDrawer.MODE_WALLPAPER_EDIT)
        }
    }

    private fun openWallpaperPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_PICK_WALLPAPER)
        } catch (_: Exception) {
            Toast.makeText(this, "No image picker found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildScreen() {
        rootLayout = FrameLayout(this)
        LauncherBackground.apply(this, rootLayout, lp)

        mainPage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(14), dp(32), dp(12))
        }
        rootLayout.addView(mainPage, FrameLayout.LayoutParams(-1, -1))

        topNavBar = TopNavBar(this, lp, object : TopNavBar.NavCallbacks {
            override fun onVoiceSearch() {
                val actions = listOf(
                    Intent.ACTION_ASSIST,
                    RecognizerIntent.ACTION_WEB_SEARCH,
                    Intent.ACTION_VOICE_COMMAND
                )
                for (act in actions) {
                    try {
                        startActivity(Intent(act))
                        return
                    } catch (_: Exception) {
                    }
                }
                Toast.makeText(this@MainActivity, "Voice Assistant not installed", Toast.LENGTH_SHORT).show()
            }

            override fun onWifiClick() = sideDrawer.toggle(SideControlDrawer.MODE_WIFI)
            override fun onAlertsClick() = sideDrawer.toggle(SideControlDrawer.MODE_ALERTS)
            override fun onCustomizeClick() = sideDrawer.toggle(SideControlDrawer.MODE_CUSTOMIZE)
            override fun onQuickSettingsClick() = sideDrawer.toggle(SideControlDrawer.MODE_QUICK)
            override fun onPowerClick() = sideDrawer.toggle(SideControlDrawer.MODE_POWER)
        })
        mainPage.addView(topNavBar)

        val contentArea = FrameLayout(this)
        mainPage.addView(contentArea, LinearLayout.LayoutParams(-1, 0, 1f))

        val mainBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, 0)
        }
        contentArea.addView(mainBody, FrameLayout.LayoutParams(-1, -1))

        favSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val favHeader = AppGridManager.createHeader(this, "♥  FAVORITES & QUICK ACCESS", true)
        favCount = favHeader.findViewWithTag("count_tv")
        favSection.addView(favHeader)

        val favHScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            clipChildren = false
        }
        favRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
        }
        favHScroll.addView(favRow)
        favSection.addView(favHScroll)
        mainBody.addView(favSection)

        val allHeader = AppGridManager.createHeader(this, "⊞  ALL APPS", false, lp) {
            refreshApps(true)
        }
        allCount = allHeader.findViewWithTag("count_tv")
        mainBody.addView(allHeader, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isSmoothScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            setPadding(0, dp(4), 0, dp(24))
            viewTreeObserver.addOnScrollChangedListener { LauncherBackground.onScrollParallax(scrollY) }
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutTransition = LayoutTransition().apply {
                setDuration(200L)
                disableTransitionType(LayoutTransition.APPEARING)
                disableTransitionType(LayoutTransition.DISAPPEARING)
            }
        }
        appGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
        }
        scrollContent.addView(appGrid)
        scroll.addView(scrollContent)
        mainBody.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setupRightDrawer(contentArea)
                val nudgeView = topNavBar.createFloatingNudgeView()
        rootLayout.addView(
            nudgeView,
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
                topMargin = dp(52)
                leftMargin = dp(235)
            }
        )
        
        setContentView(rootLayout)
        refreshApps(true)
    }

    private fun setupRightDrawer(contentArea: FrameLayout) {
        drawerScrim = View(this).apply {
            setBackgroundColor(0x55000000)
            visibility = View.GONE
            isClickable = true
            setOnClickListener { sideDrawer.close() }
        }
        contentArea.addView(drawerScrim, FrameLayout.LayoutParams(-1, -1))

        val drawerW = maxOf(dp(230), (resources.displayMetrics.widthPixels * (lp.panelWidthPct / 100f)).toInt())
        drawerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            translationX = (drawerW + dp(30)).toFloat()
        }
        val dpParams = FrameLayout.LayoutParams(drawerW, -1, Gravity.END).apply {
            topMargin = dp(6)
            bottomMargin = dp(6)
        }
        contentArea.addView(drawerLayout, dpParams)

        sideDrawer = SideControlDrawer(
            this, drawerLayout, drawerScrim, lp,
            ::applyLiveVisuals, ::applyWallpaperOnly, ::openWallpaperPicker
        )
    }

    private fun applyLiveVisuals() {
        topNavBar.refreshGreeting()
        LauncherBackground.updateThemeAndAccent(rootLayout, lp)
        val newW = maxOf(dp(230), (resources.displayMetrics.widthPixels * (lp.panelWidthPct / 100f)).toInt())
        if (drawerLayout.layoutParams.width != newW) {
            drawerLayout.layoutParams.width = newW
            drawerLayout.requestLayout()
        }
        refreshApps(!sideDrawer.isOpen)
    }

    private fun applyWallpaperOnly() {
        rootLayout.removeAllViews()
        rootLayout.addView(mainPage)
        LauncherBackground.apply(this, rootLayout, lp)
    }

    private fun refreshApps(shouldRequestFocus: Boolean, restorePkg: String? = null) {
        AppGridManager.populate(
            this, lp, favSection, favRow, favCount, appGrid, allCount,
            shouldRequestFocus, restorePkg,
            object : AppTileFactory.TileActions {
                override fun onToggleFavorite(pkg: String, label: String) {
                    val list = lp.getOrderedFavorites()
                    val added = !list.contains(pkg)
                    if (added) list.add(pkg) else list.remove(pkg)
                    lp.setFavorites(list)
                    Toast.makeText(this@MainActivity, if (added) "$label added to Favorites" else "$label removed", Toast.LENGTH_SHORT).show()
                    refreshApps(true, pkg)
                }

                override fun onMoveFavorite(pkg: String, delta: Int) {
                    if (lp.moveFavorite(pkg, delta)) refreshApps(true, pkg)
                }

                override fun onHideApp(pkg: String, label: String) {
                    val hidden = lp.getHiddenApps().apply { add(pkg) }
                    lp.setHiddenApps(hidden)
                    Toast.makeText(this@MainActivity, "$label hidden (Restore in Customize)", Toast.LENGTH_SHORT).show()
                    refreshApps(true)
                }
            }
        )
    }

    private fun dp(value: Int) = LauncherUtils.dp(this, value)
}
