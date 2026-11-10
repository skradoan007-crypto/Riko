package com.rikodev.music.android.di

import com.rikodev.music.android.audiofx.AudioNormalizer
import com.rikodev.music.android.audiofx.AudioSmoother
import com.rikodev.music.android.audiofx.ExperienceDataHub
import com.rikodev.music.android.core.experience.ColorIntelligenceEngine
import com.rikodev.music.android.core.experience.PaletteAnalyzer
import com.rikodev.music.android.core.experience.wave.EnergySolver
import com.rikodev.music.android.core.experience.wave.HighlightSolver
import com.rikodev.music.android.core.experience.wave.MotionSolver
import com.rikodev.music.android.core.experience.wave.OrganicPathGenerator
import com.rikodev.music.android.core.experience.wave.RibbonPhysicsSolver
import com.rikodev.music.android.core.experience.wave.WaveEngine
import com.rikodev.music.android.core.experience.wave.render.WaveQualityController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EXPERIENCE MODULE
//
// Hilt DI module for the entire Experience OS.
//
// All components are @Singleton with @Inject constructor().
// Hilt resolves them automatically — this module provides only what
// cannot be constructor-injected (CoroutineScope for WaveEngine).
//
// DEPENDENCY GRAPH:
//
//   App
//     └── ExperienceModule
//           │
//           ├── ExperienceDataHub       (Audio Hub — single writer)
//           │     ├── PlayerManager      (from PlayerModule)
//           │     ├── PcmAudioProcessor (from PlayerModule) ✅ @Singleton @Inject
//           │     ├── VisualizerManager (from PlayerModule) ✅ @Singleton @Inject
//           │     ├── AudioNormalizer    ✅ @Singleton @Inject
//           │     └── AudioSmoother     ✅ @Singleton @Inject
//           │
//           ├── ColorIntelligenceEngine (Color Brain) ✅ @Singleton @Inject
//           │     ├── ExperienceDataHub
//           │     └── PaletteAnalyzer    ✅ @Singleton @Inject
//           │
//           └── WaveEngine              (Wave Conductor) ✅ @Singleton @Inject
//                 ├── EnergySolver      ✅ @Singleton @Inject
//                 ├── RibbonPhysicsSolver ✅ @Singleton @Inject
//                 ├── MotionSolver      ✅ @Singleton @Inject
//                 ├── OrganicPathGenerator ✅ @Singleton @Inject
//                 ├── HighlightSolver   ✅ @Singleton @Inject
//                 └── CoroutineScope    ← provided here
//
// NO manual instantiation anywhere.
// NO `new EnergySolver()` anywhere.
// Everything goes through Hilt.
//
// CONSUMERS:
//   PlayerViewModel: injects ExperienceDataHub + ColorIntelligenceEngine + WaveEngine
//   Future ViewModels: inject WaveEngine directly
//   Canvas Composables: receive StateFlow<WaveState> from WaveEngine
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object ExperienceModule {

    // ═══════════════════════════════════════════════════════════════════════
    // WAVE ENGINE SCOPE — Application-level CoroutineScope
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Application-level CoroutineScope for the WaveEngine.
     *
     * WaveEngine is a @Singleton that lives as long as the app process.
     * It collects from AudioState and ColorState flows continuously.
     * Using viewModelScope would be wrong — ViewModel is destroyed on
     * configuration changes, but WaveEngine must persist.
     *
     * SupervisorJob ensures child coroutine failures don't cancel siblings.
     * Dispatchers.Default for CPU-bound solver computations.
     */
    @Provides
    @Singleton
    fun provideWaveEngineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUTO-RESOLVED COMPONENTS (documented for reference)
    // ═══════════════════════════════════════════════════════════════════════
    //
    // The following components have @Singleton + @Inject constructor().
    // Hilt resolves them AUTOMATICALLY — no explicit binding needed:
    //
    // AUDIO HUB:
    //   ExperienceDataHub(PlayerManager, PcmAudioProcessor, VisualizerManager)
    //   AudioNormalizer()
    //   AudioSmoother()
    //
    // COLOR BRAIN:
    //   PaletteAnalyzer()
    //   ColorIntelligenceEngine(ExperienceDataHub, PaletteAnalyzer)
    //
    // WAVE ENGINE:
    //   EnergySolver()
    //   RibbonPhysicsSolver()
    //   MotionSolver()
    //   OrganicPathGenerator()
    //   HighlightSolver()
    //   WaveEngine(5 solvers + CoroutineScope)
    //
    // RENDER LAYER (Phase 3.4):
    //   WaveQualityController(@ApplicationContext Context) — auto-detected device quality
}
