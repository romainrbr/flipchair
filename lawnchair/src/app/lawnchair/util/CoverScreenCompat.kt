package app.lawnchair.util

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Manages Samsung's cover screen app allowlist (MultiStar).
 * Requires `WRITE_SECURE_SETTINGS` permission (granted via ADB or Shizuku).
 */
object CoverScreenCompat {

    private const val TAG = "CoverScreenCompat"
    private const val MULTISTAR_KEY = "multistar_setting_repository"

    /**
     * Ensures [packageName] is in Samsung's cover screen allowlist.
     * Returns true if the package was already present or successfully added.
     */
    fun ensureAllowlisted(context: Context, packageName: String): Boolean {
        if (!isSamsung) return true

        try {
            val current = Settings.Secure.getString(context.contentResolver, MULTISTAR_KEY)
                ?: return false

            val fields = current.split("/").toMutableList()
            if (fields.size < 25) return false

            // Field 19: semicolon-separated package names
            // Field 24: semicolon-separated "package,0" entries
            val allowlistField = 19
            val widgetField = 24

            val allowlist = fields[allowlistField].split(";").filter { it.isNotEmpty() }.toMutableList()
            if (packageName in allowlist) return true

            // Add to both fields
            allowlist.add(packageName)
            fields[allowlistField] = allowlist.joinToString(";")

            val widgetList = fields[widgetField].split(";").filter { it.isNotEmpty() }.toMutableList()
            if (widgetList.none { it.startsWith("$packageName,") }) {
                widgetList.add("$packageName,0")
                fields[widgetField] = widgetList.joinToString(";")
            }

            val newValue = fields.joinToString("/")
            Settings.Secure.putString(context.contentResolver, MULTISTAR_KEY, newValue)
            Log.d(TAG, "Added $packageName to cover screen allowlist")
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted, cannot modify cover screen allowlist", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update cover screen allowlist", e)
            return false
        }
    }
}
