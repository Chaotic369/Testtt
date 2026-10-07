package com.example.edgering.overlay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/**
 * SPIKE STUB (M01): lists the first installed launcher apps alphabetically so the overlay has
 * something real to launch. Replaced by the cached LauncherApps index in M03.
 */
object SpikeAppSource {
    fun load(context: Context, limit: Int): List<OverlayItem> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = try {
            queryActivities(pm, query)
        } catch (e: RuntimeException) {
            return emptyList()
        }
        return infos
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                if (activity.packageName == context.packageName) return@mapNotNull null
                Triple(info.loadLabel(pm).toString(), activity.packageName, activity.name) to info
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first.first })
            .take(limit)
            .map { (key, info) ->
                OverlayItem(
                    label = key.first,
                    component = ComponentName(key.second, key.third),
                    icon = try {
                        info.loadIcon(pm)
                    } catch (e: RuntimeException) {
                        null
                    },
                )
            }
    }

    private fun queryActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            queryActivitiesLegacy(pm, intent)
        }

    @Suppress("DEPRECATION")
    private fun queryActivitiesLegacy(pm: PackageManager, intent: Intent): List<ResolveInfo> =
        pm.queryIntentActivities(intent, 0)
}
