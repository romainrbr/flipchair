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

package app.lawnchair.gestures.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lawnchair.LawnchairAccessibilityService
import app.lawnchair.LawnchairLauncher
import app.lawnchair.lawnchairApp
import app.lawnchair.views.ComposeBottomSheet
import com.android.launcher3.R

class RecentsGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: LawnchairLauncher) {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val onCoverScreen = displayManager
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?.state == Display.STATE_OFF

        if (onCoverScreen) {
            val service = launcher.lawnchairApp.accessibilityService as? LawnchairAccessibilityService
            val recentPkgs = service?.getRecentPackages() ?: emptyList()
            ComposeBottomSheet.show(launcher) {
                CoverRecentAppsSheet(
                    packages = recentPkgs,
                    onDismiss = { close(true) },
                )
            }
        } else {
            GestureWithAccessibilityHandler.onTrigger(
                launcher,
                R.string.recents_screen_a11y_hint,
                AccessibilityService.GLOBAL_ACTION_RECENTS,
            )
        }
    }
}

@Composable
private fun CoverRecentAppsSheet(
    packages: List<String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        Text(
            text = "Recent Apps",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (packages.isEmpty()) {
            Text(
                text = "No recent apps recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(packages) { pkg ->
                    val icon = remember(pkg) { getIconBitmap(pm, pkg) }
                    val label = remember(pkg) { getAppLabel(pm, pkg) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable {
                                val intent = pm.getLaunchIntentForPackage(pkg)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                if (intent != null) {
                                    context.startActivity(intent)
                                }
                                onDismiss()
                            },
                    ) {
                        if (icon != null) {
                            Image(
                                bitmap = icon.asImageBitmap(),
                                contentDescription = label,
                                modifier = Modifier.size(52.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(52.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun getIconBitmap(pm: PackageManager, packageName: String): Bitmap? {
    return try {
        drawableToBitmap(pm.getApplicationIcon(packageName))
    } catch (_: Exception) {
        null
    }
}

private fun getAppLabel(pm: PackageManager, packageName: String): String {
    return try {
        @Suppress("DEPRECATION")
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
        packageName
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
