# E3 — Lyrics Engine — Real Wiring Changelog

Status: written, not compiled/run yet.

## What changed

**New files** (`presentation/lyrics/host/`):
- `Id3UsltReader.kt` — new: a minimal ID3v2.3/2.4 USLT frame reader
  (nothing like it existed anywhere in the codebase). Scope is
  intentionally bounded — see its own header for exactly what it does
  and doesn't support (no ID3v2.2, no SYLT).
- `LyricsHostDependenciesReal.kt` — real song lookups (MusicRepository),
  real seek (PlayerManager), real share/open-url (Android intents), real
  haptic/toast/log, real online/offline check.
- `LyricsFetcherCallbacksReal.kt` — real local `.lrc` file read, real
  embedded-lyrics read (via Id3UsltReader), and an intentionally empty
  provider list (see below — not a gap).

**Modified:**
- `di/CoordinatorModule.kt` — both Lyrics bindings point to the real
  classes.
- `AndroidManifest.xml` — added `ACCESS_NETWORK_STATE` (normal
  permission, no runtime prompt) — needed for the real offline check.

## Honest notes

- **`getProviders() = emptyList()` is correct, not a stub.** RIKO is
  explicitly offline-first. `LyricsFetcher` already treats an empty list
  as normal, falling through to a clean "no lyrics found" state.
- **Local `.lrc` reads may fail on some Android versions** — scoped
  storage can block raw file access to a non-MediaStore-indexed sibling
  file. Never a crash: returns null, `LyricsFetcher`'s fallback chain
  handles it.
- **Id3UsltReader is new, byte-level parsing code** — defensively
  bounds-checked throughout, every call site wraps it in try/catch, but
  it's the one file in E1-E3 most worth real device/file testing.

## Next
E4 — Search, Settings, Personalization.
