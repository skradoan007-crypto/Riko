package com.rikodev.sync.nexmesh.core.protocol.model

import java.net.URI

data class QrPairingUri(
    val host: String,
    val port: Int,
    val publicKeyB64Url: String,
    val nodeId: String,
    val expiresAtEpochMs: Long? = null
) {
    fun toUriString(): String {
        val formattedHost = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
        val base = "riko-swarm://$formattedHost:$port?pub=$publicKeyB64Url&id=$nodeId"
        return if (expiresAtEpochMs != null) "$base&exp=$expiresAtEpochMs" else base
    }

    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        return expiresAtEpochMs != null && nowMs > expiresAtEpochMs
    }

    companion object {
        const val SCHEME = "riko-swarm"

        fun parse(raw: String, nowMs: Long = System.currentTimeMillis()): Result<QrPairingUri> {
            return runCatching {
                val trimmed = raw.trim()
                val uri = URI(trimmed)
                require(uri.scheme == SCHEME) { "Unsupported scheme: ${uri.scheme}" }

                val rawHost = uri.host ?: throw IllegalArgumentException("Missing host in QR URI")
                val cleanHost = rawHost.removePrefix("[").removeSuffix("]")
                require(cleanHost.isNotBlank()) { "Blank host in QR URI" }

                val port = uri.port
                require(port in 1..65535) { "Invalid port: $port" }

                val queryMap = (uri.rawQuery ?: "")
                    .split("&")
                    .filter { it.contains("=") }
                    .associate { pair ->
                        val idx = pair.indexOf('=')
                        pair.substring(0, idx) to pair.substring(idx + 1)
                    }

                val pub = queryMap["pub"]?.takeIf { it.length >= 32 }
                    ?: throw IllegalArgumentException("Missing or invalid 'pub' key parameter")
                val id = queryMap["id"]?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("Missing 'id' nodeId parameter")
                val exp = queryMap["exp"]?.toLongOrNull()

                val parsed = QrPairingUri(
                    host = cleanHost,
                    port = port,
                    publicKeyB64Url = pub,
                    nodeId = id,
                    expiresAtEpochMs = exp
                )
                require(!parsed.isExpired(nowMs)) { "QR pairing session has expired" }
                parsed
            }
        }
    }
}
