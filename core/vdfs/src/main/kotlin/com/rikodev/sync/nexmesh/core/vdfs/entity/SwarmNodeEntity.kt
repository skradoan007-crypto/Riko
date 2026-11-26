package com.rikodev.sync.nexmesh.core.vdfs.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object SwarmNodeStatus {
    const val ONLINE = "ONLINE"
    const val THROTTLED = "THROTTLED"
    const val OFFLINE = "OFFLINE"
}

@Entity(tableName = "swarm_nodes")
data class SwarmNodeEntity(
    @PrimaryKey val nodeId: String,
    val name: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val batteryPct: Int,
    val tempCelsius: Float?, // Null when sensor unavailable — never fake temperature
    val status: String,
    val lastSeen: Long,
    val host: String = "",
    val port: Int = 47780,
    val isCharging: Boolean = false
)
