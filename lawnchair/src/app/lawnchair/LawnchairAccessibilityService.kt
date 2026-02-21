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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import app.lawnchair.preferences2.PreferenceManager2
import com.patrykmichalik.opto.core.firstBlocking

class LawnchairAccessibilityService : AccessibilityService() {

    private var lastLaunchTime = 0L

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!isCoverScreen()) return

        val prefs = PreferenceManager2.getInstance(this) ?: return
        if (!prefs.coverScreenAutoLaunch.firstBlocking()) return

        val eventTypeName = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
            else -> "OTHER(${event.eventType})"
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val source = event.packageName?.toString()
                val className = event.className?.toString()
                Log.d(TAG, "[$eventTypeName] pkg=$source class=$className")

                // Match Samsung's cover home screen specifically, not generic systemui events
                // (systemui fires for gesture animations, nav bar, etc.)
                val isCoverHome = source == "com.sec.android.app.launcher" ||
                    className == SAMSUNG_COVER_HOME_CLASS ||
                    source == "com.samsung.android.app.aodservice"
                if (isCoverHome) {
                    Log.d(TAG, "  -> MATCH: cover home detected")
                    launchLawnchair()
                } else {
                    Log.d(TAG, "  -> SKIP: not cover home")
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Not useful on this device (windows list is always empty on cover screen)
            }
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    private fun isCoverScreen(): Boolean {
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val mainDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return false
        return mainDisplay.state == Display.STATE_OFF
    }

    companion object {
        private const val TAG = "LawnchairCoverScreen"
        private const val LAUNCH_COOLDOWN_MS = 1000L
        private const val SAMSUNG_COVER_HOME_CLASS = "com.android.systemui.subscreen.SubHomeActivity"
    }
}
