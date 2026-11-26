package com.rikodev.sync.nexmesh.core.protocol.identity

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.rikodev.sync.nexmesh.core.protocol.crypto.EcdhKeyExchange
import com.rikodev.sync.nexmesh.core.protocol.crypto.StreamingSha256
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

data class LocalNodeIdentity(
    val nodeId: String,
    val deviceName: String,
    val publicKey: PublicKey,
    val publicKeyB64Url: String
)

/**
 * Manages the device's persistent AndroidKeyStore-backed EC P-256 identity keypair.
 * Derives a deterministic, collision-resistant nodeId from the SHA-256 fingerprint of the public key.
 */
class NodeIdentityManager(context: Context) {
    private val appContext = context.applicationContext
    private val keyAlias = "nexmesh_node_identity_p256_v1"

    @Synchronized
    fun getOrCreateIdentity(): LocalNodeIdentity {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val publicKey: PublicKey = if (keyStore.containsAlias(keyAlias)) {
            keyStore.getCertificate(keyAlias).publicKey
        } else {
            generateIdentityKeyPair().public
        }

        val pubB64 = EcdhKeyExchange.encodePublicKeyB64Url(publicKey)
        val sha = StreamingSha256().apply { update(publicKey.encoded) }.digestHex()
        val nodeId = "nxm_${sha.take(16)}"
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()

        return LocalNodeIdentity(
            nodeId = nodeId,
            deviceName = deviceName,
            publicKey = publicKey,
            publicKeyB64Url = pubB64
        )
    }

    @Synchronized
    fun getPrivateKey(): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            generateIdentityKeyPair()
        }
        return keyStore.getKey(keyAlias, null) as PrivateKey
    }

    private fun generateIdentityKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        val purposes = KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        val spec = KeyGenParameterSpec.Builder(keyAlias, purposes)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
