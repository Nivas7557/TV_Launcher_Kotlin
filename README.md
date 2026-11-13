# Shree TV Home — Glossy Glassmorphic Android TV Launcher

A lightweight, zero-dependency, glossy glassmorphic home launcher built purely with native Android programmatic UI components for Android TV, Google TV, and landscape Android devices.

## Screenshots


## Key Features
- **Glossy Glassmorphic UI**: Translucent dark-glass app cards with specular top highlights, radial focus spotlight, and breathing neon border glow.
- **Top Status & Action Bar**: Displays live date/time, custom header profile name, `Voice Search` trigger, `WI-FI` shortcut, `Customize (✦)`, `Quick Panel (⚙)`, and `System Settings (⧉)`.
- **20% Right-Side Slide Drawer**: Opens smoothly below the top bar without blocking header controls.
- **Custom Wallpaper Picker**: Set any local image from device or USB storage as your home background, or switch back to the default teal-blue glass gradient.
- **Favorites Management**: Long-press (`OK` / `Select` hold on TV remote or touch hold) on any app tile to pin or unpin it in the **Favorite Apps** row.
- **D-Pad & Touch Ready**: Automatic first-tile focus on startup, smooth scale animations, and full support for both D-pad remotes and touchscreens.

## Voice Search / Assistant Support
The **Voice Search** pill in the top bar automatically attempts to launch the device's default assistant (`ACTION_ASSIST`), Google Voice Web Search (`ACTION_WEB_SEARCH`), or `ACTION_VOICE_COMMAND`. *Note: A Google Assistant or voice search provider app must be installed on the TV/device for voice input to respond.*

## Installation
1. Download the latest `app-debug.apk` from the **Actions** tab artifacts (or **Releases**).
2. Enable **Install from Unknown Sources** on your Android TV or Android device.
3. Install the APK and press the **Home** button to select **Shree TV Home** as your default launcher.

## Project Structure
- `MainActivity.java` — Main landscape host activity and activity result handlers.
- `TopNavBar.java` — Glassmorphic header bar with clock, brand label, and quick action pills.
- `SideControlDrawer.java` — 20%-width right-side slide drawer for Customize, Wi-Fi, and Quick Settings.
- `GlowTileLayout.java` — Custom `FrameLayout` rendering frosted glass cards, radial spotlights, and animated glow borders.
- `AppTileFactory.java` — Builds individual app cards with icons, labels, and favorite star indicators.
- `AppGridManager.java` — Manages Favorite Apps and All Apps grid layout, column counts, and initial D-pad focus.
- `LauncherBackground.java` — Handles custom persisted wallpaper URIs and the default teal-blue gradient.
- `LauncherPreferences.java` & `LauncherUtils.java` — SharedPreferences storage and package query utilities.
