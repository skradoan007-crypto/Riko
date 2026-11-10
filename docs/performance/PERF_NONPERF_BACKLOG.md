# RIKO Non-Performance Correctness Backlog (H12)

> **Phase 18 (A) artifact** · honesty tags: `[M]` · `[R]` · `[V]` · `[N]`
> **Rule:** these are **product/UI bugs and design-system debt**, NOT performance work. They must NEVER be mixed silently into perf commits. They get their own phases (Campaign v3 re-entry, plan §6 row 31+) or the FULL REBUILD track (`.mimocode\plans\1791612818890-swift-comet.md`).

## 1. Source of truth
`.mimocode\plans\1791612818890-swift-comet.md` ("RIKO FULL REBUILD PLAN — Production-Grade Music Player", Kotlin/Compose, no framework change) `[R]`.

## 2. Register

| ID | Item | File / evidence | Priority | Status | Track |
|---|---|---|---|---|---|
| N1 | **NP title cut-off** | swift-comet §2.1 | P0 product | **FIXED** — verified `PlayerSheetFull.kt:606-619`: `RikoThemeV2.Type.npTitle`, `maxLines = 2`, `TextOverflow.Ellipsis`, marquee gated on reduceMotion | done |
| N2 | **Transport buttons 3-tier inconsistency** | swift-comet §2.2 | P1 product | **FIXED** — `CircularTransportButton` has glow ring (`shadow` 6dp/12dp); Queue already uses `CircularTransportButton` | done |
| N3 | **Export button was a no-op** | swift-comet §1.2 | P1 product | **FIXED** — export button captures lab screen via `View.draw` → `VisualizerExport.exportScreenshot` → `shareScreenshot` | done |
| N4 | **Visualizer Lab polish** — mode previews fake, hardcoded colors/shapes | swift-comet §2.3 | P1 product | open | FULL REBUILD Phase 2.2 |
| N5 | **80+ hardcoded dp/sp violations** — `PlayerSheetFull.kt` (30+), `AuroraSpectrum.kt` (50+); violates `RikoDimensions.kt:11-12` rule | swift-comet §3.2 | P1 debt | open | FULL REBUILD Phase 3.1 |
| N6 | **Three parallel type systems** — `RikoTypography`+`RikoSpacing`+`RikoShape` vs `RikoThemeV2.Type`+`Colors`+`Spacing` vs raw hardcoded | swift-comet §3.1 | P1 debt | open | FULL REBUILD Phase 3.1 |
| N7 | Songs-not-loading residual latency (300 ms debounce + `filterNotNull` first-emit wait) | `MusicRepository.kt:84,122` `[R]` | P0 product+perf | partially fixed | **perf wave C2** (latency part) + product verify (skeleton UX) |

## 3. Non-goals for this file
- Performance recomposition / frame-budget work → `PERF_HYPOTHESES.md` waves C1–C9.
- Dependency upgrades → post-campaign checkpoint (Campaign §26).
- Any edit under `presentation\library\**` (D-7 frozen) — out of scope for ALL tracks without explicit user waiver.

## 4. Rules for later execution
1. Each item above gets its own commit with its own message (e.g. `Phase 31 editing completed`), never bundled with perf waves.
2. UI/design changes must still pass the J10 (NP morph) + J12 (EQ) journeys so frame cost is recorded.
3. Honesty tags apply the same way.
