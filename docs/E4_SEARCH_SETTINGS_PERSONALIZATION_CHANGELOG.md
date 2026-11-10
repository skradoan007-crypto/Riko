# E4 — Search, Settings, Personalization — Real Wiring Changelog

Status: written, not compiled/run yet.

## Search

**New:** `SearchHostDependenciesReal.kt`, `SearchHistoryStore.kt`
(`presentation/search/host/`).

- Real: navigation-to-song (= play, since songs have no separate detail
  screen), play/add-to-queue (PlayerManager), search history persistence
  (DataStore, unencrypted — see file header), haptic/snackbar/log.
- **Important:** `RikoNavGraph.kt` does not route to this subsystem's
  screen at all — it still uses the older, simpler
  `presentation/search/SearchScreen.kt` (a small, real, already-working
  text search against `MusicRepository.searchSongs()`). Wiring this
  host/ tier's dependencies doesn't change what the user sees; swapping
  the nav graph to the newer architecture is a separate, real product
  decision, not made unilaterally here.
- **Honest gaps:** `getCatalog()`'s mood/genre/bpm/play-count fields are
  neutral placeholders — no audio-mood-analysis or play-tracking
  subsystem exists anywhere in this codebase yet, and fabricating
  plausible-looking numbers would be exactly the "fake reaction"
  problem this project is trying to eliminate. Basic text search works
  correctly on the real fields; mood/vibe matching doesn't discriminate
  yet. `startVoiceInput`/`cancelVoiceInput` need an Activity-level
  bridge (same category as Balance in E2) — logged, not faked.

## Settings

**New:** `SettingsHostDependenciesReal.kt`
(`presentation/settings_v2/host/`).

- Real: own dedicated DataStore (`riko_settings_v2`) for theme/motion/
  glass/color/mini-player/language preferences — didn't reuse the
  pre-existing `SettingsRepository` since its keys are for a different,
  older settings surface with separately-typed enums; guessing at type
  compatibility without compiling wasn't worth the risk.
- `clearAllData()` is honestly scoped to preferences only — does not
  touch the music library database. A blind full wipe is a real-data-
  loss risk not worth taking unverified.

## Personalization

**New:** `PersonalizationHostDependenciesReal.kt`
(`presentation/personalization/host/`).

- Real: theme persistence (DataStore), and a real HMAC-SHA256
  `computeSignature()` (standard `javax.crypto`, no new dependency).
- **Honest gap:** `exportBackup`/`importBackup` (passphrase-based backup
  encryption) stay unimplemented. Not because the crypto primitives are
  hard — AES-GCM + PBKDF2 are both plain `javax.crypto`, no new
  dependency needed — but because a correct implementation needs
  serializing `PersonalizationBackup`'s nested `SettingsSnapshot` /
  `AudioSnapshot` / `QueueSnapshot` types, none of which this pass has
  inspected. Security-sensitive code against unverified data shapes,
  this late in a very long session, deserves its own careful pass
  rather than being squeezed in.

## DI

`di/CoordinatorModule.kt` rewritten cleanly — all 6 subsystems (Queue,
Audio, Lyrics ×2, Search, Settings, Personalization) now bind to their
Real implementations. See the top-level session note in the delivery
message about why this file needed a clean rewrite rather than another
incremental edit.

## Next
E5 — the 7 release-blockers already self-identified in this project's
own `docs/FINAL_AUDIT_REPORT.md` (duplicate NowPlayingScreen, unbounded
blur/loops, DeviceTier wiring, splash identity, LeakCanary, QueueHost
sheet ownership) — then E6 (visual tuning), E7 (validation gates), E8
(tests/release).
