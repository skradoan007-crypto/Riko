package com.rikodev.sync.nexmesh.core.guardian

import com.rikodev.sync.nexmesh.core.guardian.thermal.GuardAction
import com.rikodev.sync.nexmesh.core.guardian.thermal.ThermalBatteryGuard
import com.rikodev.sync.nexmesh.core.guardian.thermal.ThermalReading
import org.junit.Assert.assertEquals
import org.junit.Test

class ThermalBatteryGuardTest {

    @Test
    fun pausesOverheatAbove42Celsius() {
        val action = ThermalBatteryGuard.evaluatePolicy(
            batteryPct = 80,
            isCharging = true,
            temperature = ThermalReading.Available(42.5f),
            systemThermalStatus = 1
        )
        assertEquals(GuardAction.PAUSE_OVERHEAT, action)
    }

    @Test
    fun pausesUnpluggedBelow20Percent() {
        val action = ThermalBatteryGuard.evaluatePolicy(
            batteryPct = 18,
            isCharging = false,
            temperature = ThermalReading.Available(35.0f),
            systemThermalStatus = 0
        )
        assertEquals(GuardAction.PAUSE_BATTERY_LOW, action)
    }

    @Test
    fun allowsPluggedInBelow20PercentWhenCool() {
        val action = ThermalBatteryGuard.evaluatePolicy(
            batteryPct = 15,
            isCharging = true,
            temperature = ThermalReading.Unavailable,
            systemThermalStatus = 0
        )
        assertEquals(GuardAction.ALLOW, action)
    }

    @Test
    fun stopsOnCriticalThermalStatus() {
        val action = ThermalBatteryGuard.evaluatePolicy(
            batteryPct = 95,
            isCharging = true,
            temperature = ThermalReading.Available(39.0f),
            systemThermalStatus = 4
        )
        assertEquals(GuardAction.STOP_CRITICAL_THERMAL, action)
    }
}
