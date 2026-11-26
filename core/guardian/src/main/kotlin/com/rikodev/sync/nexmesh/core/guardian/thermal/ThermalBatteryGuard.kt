package com.rikodev.sync.nexmesh.core.guardian.thermal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ThermalReading {
    data class Available(val celsius: Float) : ThermalReading
    data object Unavailable : ThermalReading
}

enum class GuardAction {
    ALLOW,
    THROTTLE,
    PAUSE_BATTERY_LOW,
    PAUSE_OVERHEAT,
    STOP_CRITICAL_THERMAL
}

data class HardwareSafetySnapshot(
    val batteryPct: Int = -1,
    val isCharging: Boolean = false,
    val temperature: ThermalReading = ThermalReading.Unavailable,
    val systemThermalStatus: Int = 0,
    val action: GuardAction = GuardAction.ALLOW
) {
    val canRunHeavyTasks: Boolean
        get() = action == GuardAction.ALLOW || action == GuardAction.THROTTLE
}

/**
 * Monitors real device BatteryManager temperature, charge level, and API 29+ PowerManager thermal status.
 * Never fabricates fake temperature readings if sensor data is absent.
 */
class ThermalBatteryGuard(context: Context) {
    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _snapshot = MutableStateFlow(readCurrentSnapshot())
    val snapshot: StateFlow<HardwareSafetySnapshot> = _snapshot.asStateFlow()

    private var registered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent != null) {
                _snapshot.value = parseBatteryIntent(intent)
            }
        }
    }

    fun startMonitoring() {
        if (registered) return
        val sticky = appContext.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (sticky != null) {
            _snapshot.value = parseBatteryIntent(sticky)
        }
        registered = true
    }

    fun stopMonitoring() {
        if (!registered) return
        runCatching { appContext.unregisterReceiver(batteryReceiver) }
        registered = false
    }

    fun readCurrentSnapshot(): HardwareSafetySnapshot {
        val sticky = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (sticky != null) parseBatteryIntent(sticky) else HardwareSafetySnapshot()
    }

    private fun parseBatteryIntent(intent: Intent): HardwareSafetySnapshot {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) ((level * 100) / scale).coerceIn(0, 100) else -1

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val rawTempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempReading = if (rawTempTenths != Int.MIN_VALUE && rawTempTenths > 0) {
            ThermalReading.Available(rawTempTenths / 10.0f)
        } else {
            ThermalReading.Unavailable
        }

        val sysThermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            0
        }

        val action = evaluatePolicy(pct, isCharging, tempReading, sysThermal)
        return HardwareSafetySnapshot(
            batteryPct = pct,
            isCharging = isCharging,
            temperature = tempReading,
            systemThermalStatus = sysThermal,
            action = action
        )
    }

    companion object {
        const val MAX_SAFE_TEMP_CELSIUS = 42.0f
        const val MIN_UNPLUGGED_BATTERY_PCT = 20

        fun evaluatePolicy(
            batteryPct: Int,
            isCharging: Boolean,
            temperature: ThermalReading,
            systemThermalStatus: Int
        ): GuardAction {
            // API 29+ PowerManager.THERMAL_STATUS_CRITICAL = 4, EMERGENCY = 5, SHUTDOWN = 6
            if (systemThermalStatus >= 4) return GuardAction.STOP_CRITICAL_THERMAL
            if (temperature is ThermalReading.Available && temperature.celsius > MAX_SAFE_TEMP_CELSIUS) {
                return GuardAction.PAUSE_OVERHEAT
            }
            if (systemThermalStatus == 3) return GuardAction.THROTTLE // THERMAL_STATUS_SEVERE
            if (!isCharging && batteryPct in 0 until MIN_UNPLUGGED_BATTERY_PCT) {
                return GuardAction.PAUSE_BATTERY_LOW
            }
            return GuardAction.ALLOW
        }
    }
}
