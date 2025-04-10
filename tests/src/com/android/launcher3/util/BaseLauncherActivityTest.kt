/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.util

import android.content.Context
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherAppState
import com.android.launcher3.allapps.AllAppsStore.DEFER_UPDATES_TEST
import com.android.launcher3.integration.util.LauncherActivityScenarioRule
import com.android.launcher3.util.ModelTestExtensions.loadModelSync
import com.android.launcher3.util.Wait.atMost
import java.util.function.Function
import java.util.function.Predicate
import org.junit.Rule

/**
 * Base class for tests which use Launcher activity with some utility methods.
 *
 * This should instead be a rule, but is kept as a base class for easier migration from TAPL
 */
open class BaseLauncherActivityTest<LAUNCHER_TYPE : Launcher> {

    @get:Rule
    var launcherActivity =
        LauncherActivityScenarioRule<LAUNCHER_TYPE>(getInstrumentation().targetContext)

    val launcherInteractor = LauncherActivityInteractor(launcherActivity)

    @JvmField val uiDevice = UiDevice.getInstance(getInstrumentation())

    protected fun loadLauncherSync() {
        LauncherAppState.getInstance(targetContext()).model.loadModelSync()
        launcherActivity.initializeActivity()
    }

    protected fun targetContext(): Context = getInstrumentation().targetContext

    protected fun waitForLauncherCondition(message: String, condition: (LAUNCHER_TYPE) -> Boolean) =
        atMost(message, { launcherActivity.getFromLauncher(condition)!! })

    protected fun waitForLauncherCondition(
        message: String,
        condition: (LAUNCHER_TYPE) -> Boolean,
        timeout: Long,
    ) = atMost(message, { launcherActivity.getFromLauncher(condition)!! }, null, timeout)

    protected fun <T> getOnceNotNull(message: String, f: Function<LAUNCHER_TYPE, T?>): T? {
        var output: T? = null
        atMost(
            message,
            {
                val fromLauncher = launcherActivity.getFromLauncher<T> { f.apply(it) }
                output = fromLauncher
                fromLauncher != null
            },
        )
        return output
    }

    @JvmOverloads
    protected fun injectKeyEvent(keyCode: Int, actionDown: Boolean, metaState: Int = 0) {
        uiDevice.waitForIdle()
        val eventTime = SystemClock.uptimeMillis()
        val event =
            KeyEvent(
                eventTime,
                eventTime,
                if (actionDown) KeyEvent.ACTION_DOWN else MotionEvent.ACTION_UP,
                keyCode,
                /* repeat= */ 0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                /* scancode= */ 0,
                /* flags= */ 0,
                InputDevice.SOURCE_KEYBOARD,
            )
        launcherActivity.executeOnLauncher { it.dispatchKeyEvent(event) }
    }

    fun freezeAllApps() =
        launcherActivity.executeOnLauncher {
            it.appsView.appsStore.enableDeferUpdates(DEFER_UPDATES_TEST)
        }

    fun ViewGroup.searchView(filter: Predicate<View>): View? {
        if (filter.test(this)) return this
        for (child in children) {
            if (filter.test(child)) return child
            if (child is ViewGroup)
                child.searchView(filter)?.let {
                    return it
                }
        }
        return null
    }
}
