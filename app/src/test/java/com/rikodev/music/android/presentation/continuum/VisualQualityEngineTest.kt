package com.rikodev.music.android.presentation.continuum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualQualityEngineTest {

    @Test
    fun `default quality tier is always ULTRA`() {
        assertEquals(VisualQualityTier.ULTRA, VisualQualityTier.DEFAULT)
        assertEquals(VisualQualityTier.ULTRA, VisualQualityTier.fromPersisted(null))
        assertEquals(VisualQualityTier.ULTRA, VisualQualityTier.fromPersisted("INVALID"))
    }

    @Test
    fun `ULTRA on Android 13+ enables AGSL shaders and RenderEffect blur`() {
        val caps = DeviceRenderCapabilities(sdkInt = 33, supportsAgslShader = true, supportsRenderEffectBlur = true)
        val profile = VisualQualityResolver.resolve(VisualQualityTier.ULTRA, caps)
        assertEquals(VisualQualityTier.ULTRA, profile.selectedTier)
        assertTrue(profile.useAgslShaders)
        assertTrue(profile.useRenderEffectBlur)
        assertTrue(profile.useOrganicBezier)
        assertTrue(profile.useMdnaReactivity)
    }

    @Test
    fun `ULTRA on Android 10 keeps ULTRA selected while falling back safely from AGSL and RenderEffect`() {
        val caps = DeviceRenderCapabilities(sdkInt = 29, supportsAgslShader = false, supportsRenderEffectBlur = false)
        val profile = VisualQualityResolver.resolve(VisualQualityTier.ULTRA, caps)
        assertEquals(VisualQualityTier.ULTRA, profile.selectedTier)
        assertFalse(profile.useAgslShaders)
        assertFalse(profile.useRenderEffectBlur)
        assertTrue(profile.useOrganicBezier)
        assertTrue(profile.useDimScrubOverlay)
    }

    @Test
    fun `repeated shader failures disable AGSL without altering saved ULTRA preference`() {
        val caps = DeviceRenderCapabilities(sdkInt = 34, supportsAgslShader = true, supportsRenderEffectBlur = true)
        val profile = VisualQualityResolver.resolve(
            selectedTier = VisualQualityTier.ULTRA,
            capabilities = caps,
            shaderFailureCount = VisualQualityResolver.MAX_SHADER_FAILURES_BEFORE_FALLBACK
        )
        assertEquals(VisualQualityTier.ULTRA, profile.selectedTier)
        assertFalse(profile.useAgslShaders)
        assertTrue(profile.shaderFailureFallbackActive)
    }

    @Test
    fun `LITE mode disables expensive blur and shaders and uses dim overlay`() {
        val caps = DeviceRenderCapabilities(sdkInt = 35, supportsAgslShader = true, supportsRenderEffectBlur = true)
        val profile = VisualQualityResolver.resolve(VisualQualityTier.LITE, caps)
        assertFalse(profile.useAgslShaders)
        assertFalse(profile.useRenderEffectBlur)
        assertFalse(profile.useOrganicBezier)
        assertTrue(profile.useDimScrubOverlay)
    }
}
