# CI Fix Pass 1 — Real Compile Errors from Actual Build

Source: your `logs_84710175635.zip` — the real `Build Debug APK` GitHub
Actions run, `compileDebugKotlin` FAILED, 3m29s. This is the first
actual compiler feedback this whole project has ever had.

## 1 bug was mine

`presentation/search/host/SearchHostDependenciesReal.kt` — I wrote
`musicRepository.getAllSongs().map { song -> ... }` without checking
that `getAllSongs()` returns `Flow<List<Song>>`, not `List<Song>`
directly. Fixed: `.getAllSongs().first().map { ... }` +
`import kotlinx.coroutines.flow.first`. Every other error reported in
this file (18 of them) was a downstream cascade of this one root cause
— once `song`'s type couldn't be inferred, every property access on it
failed too.

## 5 bugs were pre-existing in the original codebase (confirmed —
none of these files were touched before this fix pass, except
NowPlayingScreen.kt where my only prior edit was the unrelated wave-HUD
toggle)

- **`MainActivity.kt`** — `toArgb()` used without its own import
  (`Color` was imported, but the extension function needs a separate
  import). Added `import androidx.compose.ui.graphics.toArgb`.
- **`GlassBackground.kt`** — `animateFloatAsState` imported from the
  wrong package (`androidx.compose.animation` instead of
  `androidx.compose.animation.core`). This one wrong import caused all
  4 reported errors in the file via cascade (unresolved function →
  can't infer the animationSpec parameter type → the resulting
  `Float + Float` becomes an ambiguous `.plus()` overload). Fixed the
  one import line.
- **`NowPlayingScreen.kt`** — two missing imports:
  `androidx.compose.material3.Icon` and this app's own
  `core.designsystem.theme.RikoShapes` (both used repeatedly in the
  file, neither imported).
- **`TransportControlsV2.kt`** — called `RikoAnim.MediumSpring()`, but
  `RikoAnim` doesn't exist anywhere in the codebase. The real thing is
  `RikoMotion.MediumSpring()` (already defined, already used
  correctly elsewhere) — looks like a leftover from an earlier name for
  the same object. Fixed the call site + added the import.
- **`VisualOsDockRenderer.kt`** — `dockPainter` is already a resolved
  `Painter` (from `rememberAsyncImagePainter`), but the code passed it
  into Coil's `AsyncImage(painter = ...)`, which doesn't have that
  parameter — `AsyncImage` wants a `model:` (raw source), not a
  pre-built `Painter`. Changed to `Image(painter = dockPainter, ...)`
  (`androidx.compose.foundation.Image`, which does take `painter`
  directly) and added its import.

## Checked but not changed

Searched for the same mistake patterns elsewhere (wrong
`animateFloatAsState` import, `RikoAnim` usage, `AsyncImage(painter =
...)` misuse, missing `Icon` imports) across the rest of the codebase.
Found a few textually-similar matches
(`SongListItem.kt`, `NowPlayingScreen.kt:421`, `BubbleIcon.kt`) — all
checked individually and confirmed correct as written (either
`AsyncImage(model = ...)` used properly, or a different, unrelated
composable that legitimately takes a `painter` parameter, or a
function name that merely contains the substring "Icon("). Nothing
else changed on the basis of pattern-matching alone.

## What this doesn't tell us yet

This CI run failed at `compileDebugKotlin` — the compiler stops
reporting once it's collected this batch, so there is no guarantee
these are the *only* remaining errors. The honest expectation: push
again, and either it's green, or a new (likely shorter) error list
shows up — that's still real progress either way, not a failure of
this pass.

## CI Fix Pass 2 — down to 1 bug

Second real CI run: only 2 errors left (both the same root cause) —
`TransportControlsV2.kt` had `RikoMotion` imported twice. This was my
own mistake from Pass 1: the file already imported it correctly at
line 6; I didn't check for that before adding a second import when
fixing the separate `RikoAnim` typo. Removed the duplicate — one line,
nothing else changed. Checked every other file touched in Pass 1 for
the same mistake (duplicate imports) — none found.

Error count: 30+ → 2 → (pending this fix) 0 expected.
