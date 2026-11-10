# RIKO liquid waveform timeline — implementation and verification

Status: source implementation complete; Android compile, device rendering, audio-output regression and frame-time validation are NOT verified in this environment. This is not a claim of a tested release APK or of 60 FPS.

## Scope and reference observations

The supplied archive is the source of truth. Five images were available in this request: three current Now Playing screenshots and two target collages. The other two stated current screenshots were not attached. No unrelated source checkout was used.

The current screenshots show a largely flat, densely notched cap, visually similar layers, a pale washed-out fill and a solid blue seek dot. The targets show broader rounded lobes, distinguishable translucent ribbons, a luminous thin rail, and a crisp outlined circle with localized bloom. The new rendering follows that visual language, using artwork swatches rather than reproducing the reference's fixed pink/orange palette. A heavily limited, constant-energy song can still have a fairly flat envelope: fake crests are deliberately not introduced.

Actual Android screenshots of the new implementation could not be produced here. Reference fidelity, exact opacity/height tuning and contrast under the existing animated aurora therefore remain device review items, not verified successes. Render tests capture the production composable when run in a configured Android build environment.

## Findings in the uploaded implementation

- AudioFileAnalyzer stored one peak/RMS pair per decoder buffer, then averaged by buffer index. Unequal buffer durations were not represented in time, peaks were averaged, and output format changes were ignored.
- It retained unbounded per-chunk lists and lacked cooperative cancellation checks. A cancelled track could continue decoding.
- WaveformRepository keyed persistent cache only by song ID, guessed the CSV format from even/odd length, used locale-dependent float serialization, had no memory/cache capacity limit, and could publish stale work.
- WaveformTimeline rebuilt three complete cubic paths and gradients during every draw. Its pause gain and oscillator changed the underlying displayed amplitude. The old unused Contour helper also contained random travelling packets.
- The sheet accepted any Ready result without checking the current song ID. Seeking used two scrubbing states, lacked a proper cancel callback/accessibility seek action, and could briefly return to stale position after release.
- The manifest and activity already declared/requested microphone access, and PlayerManager started the permission-dependent system Visualizer. This contradicted the requested policy.

## Final architecture

Local URI → MediaExtractor → raw PCM or MediaCodec decoder → WaveformBuckets → immutable WaveformData → existing WaveformRepository / Room cache → LiquidEnvelope → monotone LiquidCurve / cached LiquidRibbonPath → WaveformTimeline.

Existing Media3 playback, PcmAudioProcessor, SpectrumEngine, ColorIntelligenceEngine and ViewModel state ownership remain in place. No playback replacement, framework migration, AGSL, microphone capture, new library dependency or whole-screen redesign was introduced. Frozen presentation/library code is untouched.

### Audio analysis

- A single extra decoder, protected by a coroutine Mutex, operates on Dispatchers.IO at background thread priority; priority is restored on exit. Trace section: `RIKO.Waveform.Decode`.
- Timestamp = decoder presentationTimeUs minus the track origin plus frame/sampleRate. The negotiated channel count, sample rate and 16-bit/float encoding are checked. Raw WAV/PCM bypasses MediaCodec when already decoded.
- Channel energies are averaged, rather than phase-cancelling opposite stereo channels. Each bucket retains maximum peak and sample-weighted squared amplitude; RMS is computed from energy/count.
- The streaming workspace contains 4096 cells. Adjacent cells merge if necessary, including for unknown or long durations. Raw PCM and per-decoder-buffer collections are never retained for the song.
- Output normally has 1024 uniformly time-spaced peak/RMS pairs plus duration. Short codec-padding tails use the container timeline; large metadata differences retain observed duration.
- Cancellation is checked between codec operations and within large PCM buffers. Decoder/extractor release happens in finally, including cancellation. Timeouts, unsupported encodings and decoding failure produce no fake waveform and cannot stop playback.

### Cache and stale-result prevention

- Existing Room table/column names and DB schema are retained. `buckets_csv` now contains a versioned header and Base64 unsigned-16 peak/RMS payload. Old ambiguous CSV is a cache miss and is recomputed once.
- The fingerprint covers algorithm version, song ID, URI, duration, MediaStore size/mtime and file size/mtime where available. A source change during fresh analysis is rejected.
- At the default 1024 buckets the serialized data is approximately 5.5KB per track; this is the format size, not a measured heap benchmark.
- Access-ordered memory cache: 12 tracks. Persistent Room cache: 128 newest analyses. Cache read/write failure is nonfatal.
- Request generation and state publication share one lock. A cancelled provider/decode result cannot overwrite the newest request. Null track/ViewModel disposal cancels current work. The sheet additionally checks Ready.songId.
- Providers that expose neither size nor modification time must fall back to the supplied Song metadata; undetectable same-size/same-mtime content edits cannot be distinguished without hashing the entire source.

### Shape, drawing and colour

- Full-track audio is area-reduced, using peak maxima and RMS energy. Shared percentile-based gain is capped at 2x, with gentle logarithmic compression. Quiet audio stays quiet and digital silence stays zero.
- Body, deeper RMS bed and transient-derived highlight all use this same audio. Different positive smoothing windows provide overlap/depth without independent random lobes.
- Bounded monotone cubic Bezier controls eliminate overshoot. Full shapes never shift, regenerate randomly, or shrink when paused.
- Full paths are cached per data/size/density. Ordinary progress clips the cached prefix and updates only a short reused tail path, which tapers into the circle centreline. No coloured body is drawn to the right of the seek circle. Only the intentional small circle glow extends around it.
- Gradients, radial halo shader and stroke objects are cached with drawWithCache. The halo is defined at the origin and translated, avoiding a new moving radial shader each frame. No full-screen blur, saveLayer mask or shader stack is used.
- Existing primary/secondary artwork swatches are harmonized into a restrained family; the secondary hue is limited to a nearby range. Monochrome artwork stays neutral. Colour changes transition over 420ms; pause settles localized glow over 180ms, without mutating geometry.
- Palette changes can refresh brushes, but the separate path holder prevents full-geometry rebuilds during those colour transitions.

### Progress and seek

- The visible playing timeline samples the existing ExoPlayer currentPosition on its application/Main thread at display vsync. It does not ease/extrapolate the progress circle or increase the global player StateFlow update rate. The timer text retains its existing low-frequency updates.
- Frame sampling stops when paused, hidden, backgrounded or dragging. Only drawing state changes at high frequency.
- Tap and drag use the same inset coordinate mapping. Drag follows local touch immediately. There is one player seek per completed gesture; cancellation does not commit a seek.
- A pending target is shown until player position acknowledges it, with a one-second reconciliation bound. New gestures and track changes interrupt this work.
- Unknown/zero duration disables seeking. Values clamp to the rail. Visible circle remains small, with a minimum 48dp-high touch region and SetProgress accessibility semantics.

### Permission policy

RECORD_AUDIO was removed from the app manifest and startup request list. The system audiofx.Visualizer is no longer initialized on STATE_READY. Existing playback PCM → SpectrumEngine remains the live visualization source; full-song analysis is independent. The legacy manager class/diagnostics remain in source, inactive; a permission-dependent fallback is deliberately no longer started. Device regression of Visualizer Studio and any PCM-unsupported audio path remains required.

## Verification record

Actually completed here:

- Compared the uploaded archive against the edited tree: no original source files deleted, no frozen library changes, and no changes to dependencies, Gradle build configuration, PcmAudioProcessor or PlayerModule.
- Checked manifest XML and absence of a microphone declaration.
- Checked Kotlin lexical delimiter balance and diff whitespace. These are source-integrity checks, NOT Kotlin compilation/type checking.
- Generated four-second opposite-phase stereo WAV/AAC fixtures. ffprobe verified AAC, 16kHz, two channels, 4.000 seconds. The actual PCM fixture's per-second RMS values are approximately 0.53031, 0, 0.02828, 0.24748.
- Verified the deliverable ZIP structure/integrity during packaging.

Blocked here:

- Gradle wrapper bootstrap attempted `https://services.gradle.org/distributions/gradle-8.7-bin.zip` and failed with `java.net.SocketException: Network is unreachable`.
- There is no cached Gradle distribution, Kotlin compiler, Android SDK, emulator or connected Android device available here. JDK 17 is available. No Gradle build, JVM test suite or instrumentation test has passed here.
- No Android before/after screenshots, actual audio-output comparison, frame timing, dropped-frame count, memory allocation trace, thermal measurement or low-end-device benchmark has been captured. No FPS or zero-jank claim is made.
- GitHub delivery: the prepared changes are applied to the latest riko-perf-fixes head as a normal follow-up commit. Unrelated branch changes are preserved; CI validation is pending the user's build result.

## Tests added/updated (prepared, not executed here)

- WaveformBucketsTest: timestamp placement, unequal chunks, RMS vs peak, inverted stereo, 16-bit offsets, long unknown duration, cooperative cancellation.
- WaveformCacheCodecTest: compact locale-independent round trip, version/source mismatch, malformed/legacy cache, immutable published data.
- FluidTimelineGeometryTest: touch mapping, bounded interpolation, silence/quiet/peak distinctions, deterministic timestamp geometry and audio-related layers.
- FluidTimelinePaletteTest: artwork hue families, restrained saturation, brightened dark-cover swatches and neutral artwork.
- FluidTimelineRenderTest: production composable at API 28/34, 0/25/50/75/100% clipping, persistent paused shape, loading rail, track change, tap/drag and accessibility seek, disabled unknown-duration interaction. Writes PNGs under app/build/reports/fluid-timeline.
- AudioFileAnalyzerDeviceTest: actual Android WAV/AAC decoding, real section amplitudes, determinism, corrupt audio, no microphone permission.
- WaveformRepositoryDeviceTest: uncooperative old cache request during rapid switching, memory hits and current file-metadata invalidation.

## Required Android validation commands

```sh
./gradlew :app:testDebugUnitTest --tests '*Waveform*' --tests '*FluidTimeline*'
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.audiofx.AudioFileAnalyzerDeviceTest,com.rikodev.music.android.audiofx.WaveformRepositoryDeviceTest
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Then compare actual Now Playing screenshots against the provided references at similar sizes/progress. Check real energetic/quiet/limited music, silence, sudden peaks, long files, corrupt/unsupported media, repeated seeking and rapid track changes. Test API 26 and a low-end device, and test buffering, pause/resume, screen collapse/background, playback speed changes and normal controls/audio output.

Use a release-like build and Perfetto/Android Studio profiler to compare the same interaction/audio/device conditions before/after. Attribute decoder CPU using `RIKO.Waveform.Decode`. Record frame duration percentiles, slow/dropped frames, main-thread work and allocation rate. The code's bounded caches and reduced curve work are implementation properties, not substitutes for these measurements.

## API references used

- Android MediaCodec: https://developer.android.com/reference/android/media/MediaCodec
- MediaFormat PCM negotiation: https://developer.android.com/reference/android/media/MediaFormat#KEY_PCM_ENCODING
- Compose graphics caching: https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- Compose phase isolation: https://developer.android.com/develop/ui/compose/performance/phases

## Changed and added files

See the accompanying `docs/validation/LIQUID_TIMELINE_FILES.txt` for the exact archive-relative list. Test assets are androidTest-only and are not packaged in the normal app APK.
