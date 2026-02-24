/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import app.lawnchair.preferences2.PreferenceManager2
import com.patrykmichalik.opto.core.firstBlocking

class LawnchairAccessibilityService : AccessibilityService() {

    private var lastLaunchTime = 0L
    private var lastForegroundPackage: String? = null

    // Recent packages on the cover screen, most recent first (excluding this launcher).
    private val recentPackages = ArrayDeque<String>()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                Log.d(TAG, "ACTION_USER_PRESENT received")
                launchIfCoverScreen()
            }
        }
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 0
            packageNames = null
        }
        lawnchairApp.accessibilityService = this

        registerReceiver(
            screenReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
        )
    }

    override fun onDestroy() {
        lawnchairApp.accessibilityService = null
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onInterrupt() {}

    /** Returns the most-recently-used packages seen on the cover screen, most recent first. */
    fun getRecentPackages(): List<String> = recentPackages.toList()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action != KeyEvent.ACTION_UP) return false
        if (!isCoverScreen()) return false

        val prefs = PreferenceManager2.getInstance(this) ?: return false
        if (!prefs.coverScreenAutoLaunch.firstBlocking()) return false

        val fg = lastForegroundPackage
        if (fg == null || fg == packageName) return false

        Log.d(TAG, "  -> Home key intercepted from $fg, launching Lawnchair directly")
        launchLawnchair()
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!isCoverScreen()) return

        val source = event.packageName?.toString()
        val className = event.className?.toString()
        Log.d(TAG, "[STATE_CHANGED] pkg=$source class=$className lastFg=$lastForegroundPackage")

        // RecentsActivity is not Samsung's home — don't kill it when it appears.
        val isSamsungRecents = source == SAMSUNG_LAUNCHER_PKG && className == SAMSUNG_RECENTS_CLASS
        val isCoverHome = !isSamsungRecents && (
            source == SAMSUNG_LAUNCHER_PKG ||
                className == SAMSUNG_COVER_HOME_CLASS ||
                source == "com.samsung.android.app.aodservice"
            )

        if (isCoverHome) {
            // Only auto-launch when the preference is enabled.
            val prefs = PreferenceManager2.getInstance(this) ?: return
            if (!prefs.coverScreenAutoLaunch.firstBlocking()) return

            if (lastForegroundPackage == packageName &&
                prefs.coverScreenSamsungHomeToggle.firstBlocking()) {
                Log.d(TAG, "  -> User left Lawnchair, staying on Samsung home")
            } else {
                Log.d(TAG, "  -> User left app ($lastForegroundPackage), launching Lawnchair")
                launchLawnchair()
            }
        } else {
            // Track foreground package for recents panel — regardless of autoLaunch pref.
            if (source != null && source != "com.android.systemui") {
                lastForegroundPackage = source
                if (source != packageName) {
                    addRecentPackage(source)
                }
            }
            Log.d(TAG, "  -> Tracking foreground: $source")
        }
    }

    private fun addRecentPackage(pkg: String) {
        recentPackages.remove(pkg)
        recentPackages.addFirst(pkg)
        while (recentPackages.size > MAX_RECENT_APPS) {
            recentPackages.removeLast()
        }
    }

    private fun launchIfCoverScreen() {
        if (!isCoverScreen()) return

        val prefs = PreferenceManager2.getInstance(this) ?: return
        if (!prefs.coverScreenAutoLaunch.firstBlocking()) return

        launchLawnchair()
    }

    private fun launchLawnchair() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastLaunchTime
        if (elapsed < LAUNCH_COOLDOWN_MS) {
            Log.d(TAG, "  -> COOLDOWN: ${elapsed}ms < ${LAUNCH_COOLDOWN_MS}ms, skipping")
            return
        }
        lastLaunchTime = now

        Log.d(TAG, ">>> LAUNCHING Lawnchair on cover screen!")
        val intent = Intent(this, LawnchairLauncher::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
        }
        // Explicitly target display 1 (cover screen) so the singleTask lookup
        // runs against that display and reuses the existing task rather than
        // creating a new one every press (which forces a full config-change redraw).
        val options = ActivityOptions.makeBasic().apply {
            launchDisplayId = COVER_DISPLAY_ID
        }
        startActivity(intent, options.toBundle())
    }

    private fun isCoverScreen(): Boolean {
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val mainDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return false
        return mainDisplay.state == Display.STATE_OFF
    }

    companion object {
        private const val TAG = "LawnchairCoverScreen"
        private const val LAUNCH_COOLDOWN_MS = 1000L
        private const val MAX_RECENT_APPS = 10
        private const val COVER_DISPLAY_ID = 1
        private const val SAMSUNG_LAUNCHER_PKG = "com.sec.android.app.launcher"
        private const val SAMSUNG_COVER_HOME_CLASS = "com.android.systemui.subscreen.SubHomeActivity"
        private const val SAMSUNG_RECENTS_CLASS = "com.android.quickstep.RecentsActivity"
    }
}
