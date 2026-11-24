package com.rikodev.vyrox.core.common

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class HardwareTier { LOW_RAM, BALANCED, FLAGSHIP }

data class DeviceProfile(
    val totalRamMb: Long,
    val tier: HardwareTier,
    val maxRecommendedPipVideoLayers: Int,
    val shouldAutoProxy4K: Boolean
)

@Singleton
class DeviceCapabilityProbe @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun probe(): DeviceProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024L * 1024L)

        val tier = when {
            totalMb < 4200L -> HardwareTier.LOW_RAM
            totalMb < 7500L -> HardwareTier.BALANCED
            else -> HardwareTier.FLAGSHIP
        }

        return DeviceProfile(
            totalRamMb = totalMb,
            tier = tier,
            maxRecommendedPipVideoLayers = when (tier) {
                HardwareTier.LOW_RAM -> 1
                HardwareTier.BALANCED -> 2
                HardwareTier.FLAGSHIP -> 4
            },
            shouldAutoProxy4K = tier != HardwareTier.FLAGSHIP
        )
    }
}
