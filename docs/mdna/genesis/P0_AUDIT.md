# MDNA Genesis P0 — source audit (2026-11-10)

Genesis phases are independent of the earlier experiment's docs/mdna/P0_LAB.md, P1.md and P2.md.

## Verified baseline
- ZIP: mdna-p2.zip, 809 entries, CRC validated. No Git history.
- Remote: skradoan007-crypto/Riko, branch new, a79695d412e6ff4f5a86378865a646777bfadd0b.
- Remote differs from ZIP only in CI, .gitignore, PCM raw tap wiring, new RawSpectrumTap,
  and removal of a sensitive session transcript. Preserve those upstream repairs. Never distribute the transcript.
- Single :app Android app, MVVM/Hilt; Kotlin 2.0.21, AGP 8.5.2, Gradle 8.7,
  Compose BOM 2024.09.03, Media3 1.4.1, Room 2.6.1. Application ID com.rikodev.music.android;
  debug suffix .debug. Min 26 / target and compile 35. No identity change.
- :benchmark is referenced but its sources are missing in both ZIP and remote. Include and
  depend on it only when supplied; this does NOT claim macrobenchmark coverage.

## Audio, sync, rendering
PlayerModule provides one ExoPlayer + PcmAudioProcessor through DefaultAudioSink.
SpectrumEngine owns FFT and 40 log bands; PCM is passed through unchanged.
ZIP's MdnaSyncHub imports a missing RawSpectrumTap; remote contains the repair.
Timestamped ring -> LatencyCompensatedSource -> BandFollower -> GLES renderer is reusable.
Existing callback timestamps are monotonic processing time, NOT audio presentation PTS.
120 ms base + user route offset is an estimate. Existing platform EQ/preamp runs downstream
of the PCM tap; exact post-EQ response is NOT observable from this tap.

GLES30 renderer has four legacy scenes, procedural gl_VertexID geometry, history texture,
bloom and quality downgrade. Problems: per-frame Pair allocation, wrong energy crossover
indices, no explicit release, stale context resource names, no explicit display callback pacing.
Genesis reuses this engine, not a second decoder or audio player. AGSL is a separate optional
API-33 Canvas atmosphere, never a substitute for GLES 3D geometry.

## Data / offline / app integration
AudioFileAnalyzer owns one incremental MediaExtractor/MediaCodec path with a Mutex,
channel/encoding-aware WaveformBuckets, cancellation, decoder timeout and cleanup.
Reuse/refactor in Genesis P2; no second decoder in P0.
RikoDatabase is v5 (8 entity classes), migrations 2->3->4->5; destructive fallback exists.
play_history has song_id, played_at, play_duration; later meaningful-listen queries must
filter duration. No v5 exported schema or database fixture is supplied. P3 is not validated.
Existing MusicPlaybackService owns MediaSession; PlayerManager owns queue and transport.
Existing SpectrumStyle.MDNA and MdnaVisualizerHost are retained. Frozen library UI untouched.
No WorkManager dependency found; decide P6 scheduling after background infrastructure audit.
41 test source files supplied (unit + device); legacy MDNA tests cover ring, follower,
triple buffer, governor and catalog. Historical test claims are not current-run evidence.

## Reference review
All nine references inspected: 02/05/08 emphasize luminous spherical silhouettes, layered
contours and fine particles; 01/03/04 terrain; 06/07/09 flowing particle matter. P0 selects
one translucent cyan-violet-magenta orb, with restrained highlights and continuous deformation.
No reference raster is copied into application resources.

## Implementation checklist
- [x] Reuse upstream tap repair; isolate consumer failure from playback.
- [x] Harden ring concurrency/reset and playback lifecycle handling.
- [x] Build one Genesis orb on the existing GLES engine.
- [x] Separate debug lab, LIVE/DEMO, freeze, quality, diagnostics and manual AV offset.
- [x] Optional API-33 AGSL atmosphere; GLES on API 26–32; report unsupported GPU.
- [x] Explicit frame pacing, cleanup and context-loss safety.
- [x] Unit/device test sources added; actual shader render evidence generated.
- [ ] Execute Android/JVM tests and physical-device acceptance (tooling unavailable).
- [x] Preserve release/debug signing; CI configured to compile both variants (not executed).
- [x] Prepare one local final P0 milestone commit; stop for visual review before P1.
- [ ] Remote push/CI (GitHub integration returned HTTP 403).
