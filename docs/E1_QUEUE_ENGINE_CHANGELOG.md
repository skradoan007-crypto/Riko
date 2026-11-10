# E1 — Queue Engine — Real Wiring Changelog

Status: **Code written, NOT yet compiled or run on a device.** This codebase
has never had a verified green build (per FINAL_AUDIT_REPORT.md). Treat
everything below as "should be correct by static reading," not "confirmed
working" — push to CI (`build-debug-apk.yml`) for the real signal.

## What was broken

`QueueHostDependenciesStub` (di/stubs/) wired every Queue action to an
empty no-op body. Tapping play-at, reorder, remove, add-next, add-to-end,
clear, repeat, shuffle, favorite, share — none of it reached PlayerManager
or any repository. Separately, `QueueViewModelBridge`'s state only ever
changed from UI-originated events — nothing fed PlayerManager's real,
ExoPlayer-backed queue into Queue's own state, so even a perfectly-wired
outgoing path would still render a stale/empty screen.

## What changed

**New files:**
- `presentation/queue/host/QueuePlaybackSync.kt` — pure mapper,
  PlayerManager's `PlayerState` → Queue's own `QueueStateSnapshot`. This is
  the incoming-sync half that didn't exist at all before.
- `presentation/queue/host/QueueHostDependenciesReal.kt` — real
  implementation of every `QueueHostDependencies` method, replacing the
  stub. Outgoing-command half.

**Modified files:**
- `presentation/queue/host/QueueViewModelBridge.kt` — added a
  `PlayerManager`-driven collector (distinct-until-changed on the mapped
  snapshot, so the ~250ms position ticker doesn't spam re-syncs or slam
  the queue sheet closed); rewrote `restoreState()`'s comment with the
  actual reasoning instead of a silent stub.
- `di/CoordinatorModule.kt` — `bindQueueHostDependencies` now binds
  `QueueHostDependenciesReal` (was `QueueHostDependenciesStub`). The other
  5 stub bindings (Audio, Lyrics, Personalization, Search, Settings) are
  untouched — those are separate subsystem passes (E2-E4).

## Honest, labeled gaps (not hidden)

1. **`navigateToAlbum` / `navigateToArtist`** show a real Toast ("Album/
   Artist view isn't built yet") instead of crashing on a NavGraph route
   that doesn't exist — `RikoDestinations.kt` has no Album/Artist Detail
   destination at all. Building real navigation for these needs those
   Focus-layer screens to exist first (separate subsystem).
2. **`saveStateToSavedStateHandle`** is a real no-op — see the reasoning
   in `QueueViewModelBridge.restoreState()`. PlayerManager's own
   `@Singleton` lifetime + the new live-sync collector already cover the
   common "backgrounded, not fully killed" case; true Bundle persistence
   would require making `QueueState`/`QueueItem` Parcelable (touching the
   frozen `queue/contract` tier) for a case that's already handled.
3. **Per-item `energy` / `mood` / `source` / `addedAtMs`** on synced items
   are neutral placeholders (0.5 / null / AUTO_QUEUE / sync-time) —
   PlayerManager's domain model doesn't track these per slot. Real values
   need the (separate, not-yet-wired) Audio Energy / Live Analysis engine.
4. **`queueId` is position-derived** (`q_<songId>_<index>`), not a truly
   stable per-slot id — PlayerManager's own `QueueItem` only exposes an
   index. Correct for this app's single-user, one-event-at-a-time flow;
   a genuinely stable id needs a slot-id field added to PlayerManager's
   `QueueItem` (out of Queue Engine scope).

## Next

E2 — Audio FX Engine (`AudioHostDependenciesStub` → real `AudioFxManager`
wiring), continuing straight through per the approved E0–E8 plan.
