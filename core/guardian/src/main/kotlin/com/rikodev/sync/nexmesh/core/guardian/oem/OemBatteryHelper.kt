package com.rikodev.sync.nexmesh.core.guardian.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

data class OemSurvivalGuide(
    val manufacturer: String,
    val isBatteryOptimizationIgnored: Boolean,
    val manualSteps: List<String>
)

/**
 * Detects aggressive OEM power managers (Xiaomi, Realme, Oppo, Vivo, Samsung),
 * opens known OEM settings intents with safe fallback to standard Android Battery Optimization settings.
 */
class OemBatteryHelper(private val context: Context) {

    fun inspectGuide(): OemSurvivalGuide {
        val mfr = Build.MANUFACTURER.lowercase()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoring = pm.isIgnoringBatteryOptimizations(context.packageName)

        val steps = when {
            mfr.contains("xiaomi") || mfr.contains("redmi") || mfr.contains("poco") -> listOf(
                "Open Security / App Info -> Enable Autostart for Nex Mesh",
                "Battery Saver -> Select 'No restrictions'",
                "Lock Nex Mesh in Recents for long node sessions"
            )
            mfr.contains("realme") || mfr.contains("oppo") || mfr.contains("oneplus") -> listOf(
                "App Info -> Battery Usage -> Enable 'Allow background activity'",
                "Enable 'Allow auto-launch' so Node mode survives screen-off"
            )
            mfr.contains("vivo") || mfr.contains("iqoo") -> listOf(
                "iManager -> App Manager -> Autostart -> Enable Nex Mesh",
                "Battery -> High background power consumption -> Allow Nex Mesh"
            )
            mfr.contains("samsung") -> listOf(
                "App Info -> Battery -> Select 'Unrestricted'",
                "Device Care -> Battery -> Background usage limits -> Add Nex Mesh to 'Never auto sleeping apps'"
            )
            else -> listOf(
                "Open Battery Settings -> Select 'Unrestricted' for Nex Mesh",
                "Keep device plugged in for continuous Swarm Node hosting"
            )
        }

        return OemSurvivalGuide(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            isBatteryOptimizationIgnored = ignoring,
            manualSteps = steps
        )
    }

    fun openBestBatterySettings(): Boolean {
        val mfr = Build.MANUFACTURER.lowercase()
        val oemCandidates = when {
            mfr.contains("xiaomi") || mfr.contains("redmi") || mfr.contains("poco") -> listOf(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )
            mfr.contains("oppo") || mfr.contains("realme") -> listOf(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                ComponentName("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity")
            )
            mfr.contains("vivo") -> listOf(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            )
            mfr.contains("samsung") -> listOf(
                ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            )
            else -> emptyList()
        }

        for (comp in oemCandidates) {
            val intent = Intent().setComponent(comp).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tryStart(intent)) return true
        }

        // Standard Android Battery Optimization Settings fallback
        val standard = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(standard)) return true

        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStart(appDetails)
    }

    private fun tryStart(intent: Intent): Boolean {
        return runCatching {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
