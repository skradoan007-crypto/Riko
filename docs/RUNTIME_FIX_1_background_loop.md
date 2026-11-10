# Runtime Fix 1 — Background Animation Loop (real crash/lag report)

Root cause of "ultimate lag" / "Riko Music keeps stopping": found and
fixed with real evidence, not a guess.

## What was wrong

`VisualOsCoordinator.startBackgroundAnimation()` launched a
`while (true) { ...; delay(16) }` loop (a state update every ~16ms,
forever) directly on a `CoroutineScope`, NOT wrapped in Compose's
`LaunchedEffect` (unlike every other loop in this codebase — checked
all 7 other `while(true)` loops in the project, all correctly scoped
inside `LaunchedEffect`). Three compounding problems:

1. The loop's `Job` was never stored anywhere. `dispose()` only set the
   `coroutineScope` field to `null` — which does nothing to a coroutine
   that's already running on that scope. Nothing in the codebase could
   ever actually cancel this loop once started.
2. No lifecycle awareness at all — it kept running at full rate even
   with the app fully backgrounded (screen off, user in another app).
3. No DeviceTier awareness — same ~60fps rate on every device, unlike
   the blur radius in NowPlayingScreen (which already correctly scales
   65dp/25dp/0dp by tier).

Net effect: once `ProvideVisualOs` initializes (once per app session,
at MainActivity's root), this loop runs a StateFlow emission ~60
times/second for the rest of the process lifetime, foreground or
background — real, continuous CPU/battery cost. That's a very plausible
match for "severe lag," the app dragging down other apps, and Android's
background-execution limits eventually killing the process ("keeps
stopping").

## The fix (`VisualOsCoordinator.kt` + `VisualOsWiring.kt`)

- The loop's `Job` is now stored and properly cancelled — in
  `pauseBackgroundAnimation()` (real now, was a no-op comment before)
  and in `dispose()`.
- `startBackgroundAnimation()` is idempotent — cancels any existing job
  before starting a new one, so it can never run twice concurrently.
- Update rate now scales with real `DeviceTier`: 16ms (TIER_HIGH) /
  32ms (TIER_MEDIUM) / 66ms (TIER_LOW) instead of a flat 16ms for every
  device.
- `ProvideVisualOs` now observes the real Activity lifecycle (standard
  `LocalLifecycleOwner` + `LifecycleEventObserver`, already-available
  dependency, nothing new added) and calls
  `pauseBackgroundAnimation()` on `ON_STOP`, `resumeBackgroundAnimation()`
  on `ON_START`. This is the actual fix for "runs forever even
  backgrounded."

## What I couldn't verify from here

This is a very strong, evidence-based candidate for the crash — it
matches every symptom described (severe lag, dragging other apps down,
process getting killed) and it's a genuine, confirmed bug (not
speculative). But without a real logcat from the crash, I can't rule
out a second contributing cause. If it happens again after this fix,
the logcat becomes much more valuable — it'll be a different, narrower
signal.

## Also checked, not changed

Every other `while(true)` loop in the codebase (`WaveformTimeline.kt`,
`NowPlayingEffects.kt`, `NowPlayingPerformance.kt`, `DockMotion.kt`, and
two in debug/preview-only files) — all correctly wrapped in
`LaunchedEffect`, all correctly cancelled by Compose when their
composable leaves the tree or their key changes.
`WaveformTimeline.kt` in particular already does exactly the right
thing for the wave's "freeze when paused" requirement.

---

# Runtime Fix 2 — GlassSurface Oversized Blur Ovals (real screenshot evidence)

Root cause of the huge blur blobs covering Settings text in your
screenshot: found with certainty this time, not just a strong
hypothesis.

## What was wrong

`GlassSurface.kt`'s `drawSpecularHighlight()` and `drawGroundingShadow()`
(Layer 4 / Grounding Shadow of the Glass Engine) drew ovals sized
purely as a proportion of the container — 60% width / 50% height —
with no upper bound. That's a tasteful, subtle highlight on a small
button or pill (what this was clearly designed and tested against).
On a *tall* container — like a Settings section card stacking several
preset rows, easily 500dp+ — the exact same math produces an oval
hundreds of dp tall, sitting directly on top of the text. A real light
reflection doesn't grow just because the glass pane is taller; the
code never accounted for that.

## The fix

Both functions now cap their oval size to a sane absolute maximum
(130×90dp for the specular highlight, 140×90dp for the grounding
shadow) instead of scaling without bound. Still proportional (and
correctly small) on small elements; now visually contained on large
ones too.

This is a shared component (`GlassSurface`/`GlassCard`/`GlassPill`/
`GlassCircle`) used in 30 files across the app — this one fix applies
everywhere it's used, not just Settings.

## Next
The Settings screen's actual *content* (icon badges, pill buttons,
segmented controls, radio glow rings) still needs building — the
current code is minimal (plain Text/RadioButton/TextButton), nowhere
near the reference image. That's the very next piece of work.

---

# UI Rebuild — Now Playing (toward reference Image 1)

Restructured the transport controls to match the reference layout:

- **Hero row** (was Shuffle/Prev/Play/Next/Repeat): now Lyrics /
  Equalizer / [Play] / Timer / More — matching the reference exactly.
- **New pill row**: Shuffle / Previous / Next / Repeat, in its own
  `GlassSurface` chip container below the hero row.
- **New Lossless Quality strip**: real classification from the song's
  actual `bitrate`/`sampleRate` fields — never a hardcoded "Lossless"
  claim (most local files are lossy; only genuinely high-bitrate files
  get that label). "Full smooth, real cause" rule applied here too.
- **Lyrics is now really wired**: added `onOpenLyrics`, and — since
  `LyricsScreenWiring` already had real dependencies from E3 — gave it
  an actual bottom sheet in `RikoNavGraph.kt`, mirroring the existing
  Equalizer sheet pattern exactly. Not a placeholder; tapping Lyrics
  now opens the real lyrics subsystem.
- **`onMore`**: added as a parameter, currently a no-op default — no
  context-menu infrastructure exists yet to wire it to honestly.

**Note:** `onOpenQueue` is no longer called from this row (the
reference design doesn't show a Queue icon here either) — Queue still
needs to be reachable from somewhere in the real layout; flagging this
rather than silently dropping it.

**Not yet done:** Album art background glow color-accuracy wasn't
re-verified this pass (existing `colorState.isFromArtwork`-driven code
looked right on read, but "confirmed correct" needs a real render to
check). The full Now Playing chrome (top bar icons, title/artist
layout) wasn't touched — only the transport section. Settings'
remaining sections (Accessibility/Language/Diagnostics/Developer) and
the coil/braid FFT-driven waveform are both still ahead.

---

# UI Rebuild — Now Playing title block

- Title/artist switched from center-aligned to left-aligned, matching
  the reference layout.
- Artist line now shows "Artist • Album" (was artist-only before).
- **Restored Favorite's reachability**: the earlier transport-row
  restructure moved Favorite out of the layout without giving it a new
  home, making `viewModel.toggleFavoriteCurrentSong()` real but
  unreachable from the UI. Now sits next to the title as a heart icon,
  matching the reference. Added a "More" icon next to it too (wired to
  the same not-yet-built menu as the transport row's More).
- Deliberately did NOT add the reference's 3-dot page indicator under
  "PLAYING NOW" — that implies swipeable pages (Now Playing/Lyrics/
  Queue), which don't exist; this app currently uses bottom sheets for
  those. Decorative dots with no real paging behind them would be
  exactly the kind of fake-looking chrome to avoid. Building real
  swipeable pages is a separate, larger decision than a visual tweak.
- Also deliberately kept the top-right icon as Info (not swapped to a
  sparkle/AI glyph) — a sparkle strongly implies AI functionality;
  changing only the icon while the panel behind it is a plain info
  view would visually overclaim what it does.

---

# CI Fix Pass 3 — UI rebuild's first real compiler feedback

11 errors, all fixed, all genuine (no guessing):

1. **`GlowRadioButton.kt` / `PillSegmentedControl.kt` / `PillToggle.kt`
   (6 errors)** — `by animateColorAsState(...)` needs
   `import androidx.compose.runtime.getValue` for the `by` delegate to
   resolve. Forgot it in all 3 new files (same mistake, repeated —
   checked every other file from this session for the same pattern,
   found no other instances).
2. **`PillSegmentedControl.kt` (1 error)** — had explicitly imported
   `androidx.compose.foundation.layout.weight`, which doesn't exist as
   a free top-level import (it's `RowScope.weight()`, resolved
   automatically via the Row's implicit receiver, no import needed at
   all) — that wrong import was shadowing correct resolution. Removed.
3. **`NowPlayingScreen.kt` (3 errors)** — `GraphicEq` isn't under
   `Icons.AutoMirrored.Filled`, only plain `Icons.Filled`; `MoreVert`
   and `Modifier.width()` were used without their import lines (this
   codebase imports icons individually, not via wildcard).
4. **`SettingsRendering.kt` (1 error)** — `Modifier.background()` needs
   `import androidx.compose.foundation.background` specifically; the
   existing `androidx.compose.foundation.layout.*` wildcard doesn't
   cover it since `background` lives one package up.

All mechanical, all real, all fixed with the same rigor as the
earlier CI fix passes.

---

# Runtime Fix 3 — Second unthrottled loop (Wave Engine) + Waveform diagnosis

Real device report: crash persisted after RuntimeFix1, consistently
around ~20s. Found a second instance of the exact same bug class.

## The second crash contributor

`PlayerViewModel` is created once at the top of `RikoNavGraph` and
shared across every screen (passed into Search, Now Playing, the
MiniPlayer, the EQ sheet) — never cleared just by navigating away from
Now Playing. Its `init{}` starts `waveEngine.observe(...)`, which
collects real-time audio frames (every PCM buffer callback — likely
50-100+ times/second) with zero throttling, for the entire app session,
including fully backgrounded. Same class of bug as VisualOsCoordinator
(RuntimeFix1), just in an even higher-frequency subsystem — which fits
the ~20s-consistent timing better than the first fix alone did.

`WaveEngine.pause()` looked like the fix but isn't — it only gates the
solver's *output*, the underlying flow collection keeps running
regardless. Real fix uses `stop()`/`observe()` (which does cancel the
subscriptions), wired to real ON_STOP/ON_START lifecycle events in
`RikoNavGraph.kt`, mirroring the exact pattern from RuntimeFix1.

(Caught a real structural mistake while editing `PlayerViewModel.kt` —
an early version of this edit closed `init{}` too early, orphaning the
rest of its original content outside any function. Brace-balance
checking caught it before packaging, not after.)

## Waveform diagnosis (the "kokra"/not-smooth report)

Real, precise finding: the wave visible in your screenshot — the
seekbar — is rendered by `WaveformTimeline.kt`, which draws individual
straight-line segments (`drawLine()`), NOT the sophisticated organic
bezier ribbon (`RibbonRenderer.kt`) verified earlier this session. That
sophisticated system is real and wired, but it currently drives the
*ambient background* wave, not the seekbar — which is where your
reference image shows the coiled/organic look. Two separate rendering
paths for two different real subsystems; the one you're looking at
happens to be the simpler one. Next step: make the seekbar itself use
the same organic rendering the ambient wave already has.

---

# Wave Rebuild — real per-band energy now drives the seekbar

Direct response to the "kokra, not smooth, doesn't feel like live FFT"
report. Real finding, real fix — not a guess.

## What was actually happening

`WaveformTimeline.kt` (the "Glowing Fire Plasma" seekbar — an existing,
already fairly sophisticated organic/glowing 3-layer flame renderer,
not a flat bar chart) was driven by a single aggregate `audioEnergy:
Float`, sourced from `colorState.energyIntensity` — a color-
intelligence value, not the wave engine. The real, verified
bass/mid/treble Energy Solver output (`WaveState.energy.bassPressure/
midTexture/trebleSpark`) was never connected here at all — this file
was never touched this session until now.

## The fix

- `WaveformTimeline` now takes `bassEnergy`/`midEnergy`/`trebleEnergy`
  instead of one `audioEnergy` float.
- Each of the file's existing 3 flame layers is now driven by its OWN
  band: Layer 2 (colorA) ← bass, Layer 3 (colorB) ← mid/vocal, Layer 4
  (white core) ← treble. Three layers, three bands — a direct mapping
  that didn't exist before.
- `NowPlayingScreen.kt` now collects `viewModel.waveState` (the real
  Energy Solver output, already verified real and connected earlier
  this session) and passes its actual per-band values in.

## Honest note

This is a real, concrete step — the seekbar now genuinely reacts
per-band instead of uniformly. It is NOT the full "coil/braid"
`RibbonRenderer` bezier system (that still only drives the ambient
background wave). Whether this reads as "smooth" enough, and whether
the per-band visual difference feels strong enough, needs your eyes on
a real build — the next honest checkpoint, same as everything else
this session.

---

# Background glow — real 3rd color, not a repeat

Checked your "audio thumbnail colour" background note. Found: `ColorState`
extracts a genuinely rich palette (primary/secondary/accent/waveStart/
waveEnd...), but `GlassBackground`'s 3rd blob just reused the primary
color at lower alpha — never a real distinct hue, capped at 2 real
colors no matter how rich the extraction was. That's very likely why
the screenshot read as flat/one-tone rather than a multi-hue nebula.

Fix: added a real `accentColor` parameter (animated the same
spring-morph way as the other two), wired to `colorState.accent` in
NowPlayingScreen. Other screens using GlassBackground are unaffected
(defaults to the old behavior since they don't pass accentColor).

---

# Design Brief Pass 1 — Priorities 1 & 2 (real annotated-screenshot feedback)

Following the 23-section design brief's own stated priority order.

## Priority 1 — Fake/random shine removed at the root

Found and fixed the actual source of every "glossy hotspot" circled in
the screenshots — three specular-highlight ovals, all far too large and
bright:

- `BubbleIcon.kt` → `SmallBubbleIcon` (chevron, info, heart, 3-dot,
  Lyrics/Equalizer/Timer/More — every small circular button, every
  screen): was 0.6 alpha covering ~40% of the button. Now 0.14 alpha,
  ~10% — a subtle top-edge catch-light, not a spotlight.
- `BubbleIcon.kt` → outer shell of the big Play button: was 0.80 alpha
  covering ~50%. Now 0.22 alpha, ~9%.
- `BubbleIcon.kt` → inner glyph of the big Play button: was 0.60 alpha
  covering ~65%×45%. Now 0.18 alpha, ~10%.
- `GlassSurface.kt` specular highlight (the 30-file-shared glass card
  system — Settings sections, Library rows, etc.): was 0.5/0.18 alpha.
  Now 0.16/0.05.

All four are root-level, shared components — this fixes every screen
at once, not a per-screen patch.

## Priority 2 — Bottom nav / system navigation overlap

`RikoBottomNav.kt` had zero `WindowInsets` handling at all — confirmed
real bug matching the "buttons hide in [gesture nav] down" annotation.
Added `.navigationBarsPadding()` so the dock always clears the gesture
bar / 3-button nav area, on any device.

## Bonus, directly evidenced by the screenshots

`SettingsRendering.kt`'s root `Column` was missing `.statusBarsPadding()`
— confirmed cause of "Settings" / "Reset Defaults" / "Clear All Data"
clipping under the status bar in the annotated screenshot. Every other
main screen (Now Playing, Library, Favorites) already had this; Settings
was the one exception. Fixed.

## Honest — what's NOT done yet from the 23-section brief

This pass covered priorities 1-2 plus one directly-evidenced bug. Still
ahead, in the brief's own order: a real spacing/typography/corner-radius
design-token audit across every screen (not just the components already
touched this session), redesigning the bottom nav from "5 bubbles" into
one cohesive container, Playing Now screen's remaining spacing/hierarchy
pass, and the empty-state/album-art-placeholder system (Sections 19-20).
Not attempting all of this in one pass — same reasoning as every other
large piece this session: real, verified progress over a rushed sweep.

---

# Design Brief Pass 2 — Bottom nav rebuilt as one cohesive container (Section 4)

Rewrote `RikoBottomNav.kt` per the brief's explicit Section 4 direction.

## Before
Each of the 5 tabs was wrapped in its own `SmallBubbleIcon` — 5 separate
glass shells, each with its own shadow/border/specular highlight. This
is literally "five glossy glass bubbles," the exact phrase the brief
uses.

## After
One shared `GlassPill` container (unchanged, already correct) with icons
placed directly inside it — no per-icon glass shell at all. The only
active-state cue is a single soft rounded-rect background behind the
active icon (`accentColor` at 0.16 alpha, spring-animated width/alpha),
plus a tint color change. Inactive tabs are just an icon at 0.5 alpha,
no surface underneath.

This directly matches the brief's own checklist for this section: "একটি
cohesive navigation container... clear active-state indicator... subtle
active background... restrained elevation... visually অতিরিক্ত loud হবে
না" — one container, one restrained indicator, not five loud orbs.

---

# Runtime Fix 3 — ExperienceDataHub bridges never stopped

Third confirmed instance of the same root pattern as RuntimeFix1
(`VisualOsCoordinator`) and RuntimeFix2 (`WaveEngine`): a background
collector with no way to stop.

## What was wrong

`MainActivity.onCreate()` called `experienceDataHub.startBridges()`
once, with no matching `stopBridges()` call anywhere in the codebase —
confirmed by grep before touching anything. But the deeper bug was
inside `startBridges()` itself: it launched four separate coroutines
(Bridge D player-state, Bridge E PCM diagnostics, Bridge E PCM
amplitude, Bridge F visualizer waveform) via bare `scope.launch { }`
calls, discarding each returned `Job`. A `bridgeJob: Job?` field
existed and `stopBridges()` faithfully called `bridgeJob?.cancel()` —
but nothing ever assigned that field, so it was permanently `null`.
Even if something had called `stopBridges()`, it would have been a
no-op. Two bugs stacked: never called, and wouldn't have worked if it
had been.

Net effect: PCM amplitude and visualizer waveform collectors —
feeding off audio callbacks at a high, continuous rate — ran for the
entire process lifetime, foreground or backgrounded, identical in
shape to RuntimeFix1 and RuntimeFix2.

## The fix

- `ExperienceDataHub.kt`: replaced the single dead `bridgeJob` field
  with a real `bridgeJobs: MutableList<Job>`; each of the four
  `scope.launch { }` calls is now appended to it. `stopBridges()`
  cancels and clears the whole list — now genuinely stops all four
  collectors, not zero of them.
- `MainActivity.kt`: removed the one-shot `startBridges()` call from
  `onCreate()`. Added `onStart()`/`onStop()` overrides — using the
  Activity's own real lifecycle directly (no extra `LifecycleObserver`
  needed here, unlike RuntimeFix2's Compose-side wiring, since
  `MainActivity` already *is* the `LifecycleOwner`) — calling
  `startBridges()` on `onStart()` and `stopBridges()` on `onStop()`.

## Why pause-on-background is correct here (checked, not assumed)

Grepped every consumer of `ExperienceDataHub`/`audioState` before
deciding this was safe: `PlayerViewModel` (exposes it straight
through, comment confirms it's UI-facing — waveform/energy/glow, not
playback control), `NowPlayingScreen`, `ColorIntelligenceEngine`,
`ExperienceInspector(V2)`. All visual-experience consumers, none
service- or notification-level. Playback itself runs through
`PlayerManager`/Media3 independently of this hub, so pausing the hub
on `ON_STOP` does not touch playback continuity — only stops feeding
data to visuals nobody can see while backgrounded.

## What I couldn't verify from here

No `google()` Maven mirror is reachable from this sandbox, so I
couldn't run a real `./gradlew build` to prove this compiles — verified
by hand instead (brace/paren balance check, grep for every remaining
`bridgeJob`/`startBridges`/`stopBridges` reference, confirmed no
duplicate `onStart`/`onStop` override already existed). CI or a local
build is the next real checkpoint. Also didn't re-check whether
rotation/config-change churns `onStop()`→`onStart()` fast enough to
matter — should be harmless (both calls are idempotent/guarded by
`isBridgesStarted`) but wasn't specifically tested.

---

# Runtime Fix 4 — Shared Live Clock never paused ("floating object = lag")

Direct response to your report that floating objects correlate with
lag. Found the single shared driver behind every floating icon in the
app, and it had the same shape of issue as Fix1-3.

## What was wrong

`RikoLiveClock.kt`'s own header comment documents a real, legitimate
prior optimization: every floating/shine/zoom effect used to run its
OWN `rememberInfiniteTransition()` (8-16 independent Choreographer
loops per screen); that was consolidated into ONE shared transition
(`ProvideRikoLiveClock`), read by every effect via `LocalRikoClock`.
Grepped every consumer to confirm scope: `BubbleIcon.kt` is the ONLY
file reading `LocalRikoClock` — both `BubbleIconButton` (the big glass
play button) and `SmallBubbleIcon` (every tab bar / toolbar icon) —
so this one clock really is the driver behind every floating object in
the app, matching your report precisely.

But that one remaining transition is mounted once at `RikoNavGraph`'s
root (`ProvideRikoLiveClock { NavHost(...) }`) and stays composed for
the entire app process, background or foreground — with zero lifecycle
gating. Same root shape as RuntimeFix1-3: a continuous loop with no
handle to stop it while backgrounded — just via Compose's
`rememberInfiniteTransition` API instead of a raw coroutine.

## Honest nuance (not overclaiming)

Compose's window Recomposer is generally lifecycle-aware and *may*
already pause frame requests below `Lifecycle.State.STARTED` on some
Compose versions — I can't confirm that behavior for certain without a
real device, which this sandbox doesn't have. Rather than assume the
framework already handles it, applied the same explicit ON_STOP/ON_START
discipline used everywhere else this session, for certainty.

## The fix

- `ProvideRikoLiveClock` now tracks real Activity visibility via the
  same `LifecycleEventObserver` pattern already used in
  `RikoNavGraph.kt` for RuntimeFix2 (no new dependency).
- While backgrounded, the `if (isForeground) { rememberInfiniteTransition... }
  else { staticClock }` branch means the live transition is not just
  gated in output — it's fully UNMOUNTED, which is what actually stops
  its Choreographer callback loop. This directly follows the
  RuntimeFix2 lesson: gating a value's *output* isn't a real fix if the
  underlying loop keeps ticking; `WaveEngine.pause()` looked like a fix
  and wasn't, for exactly this reason.
- Caught and corrected a self-introduced regression while writing this:
  an earlier draft synced a "last known angle" by reading `angle.value`
  directly in the composable body (not inside a draw-scope lambda) —
  that would have subscribed `ProvideRikoLiveClock`, and everything in
  `content()` under it (the whole `NavHost`), to recompose on every
  single animation frame. Removed; backgrounded icons now simply hold
  their neutral resting position (`staticClock` = `0f`, which
  `oscillate()` maps to zero translation) instead, and resume cleanly
  from `0f` on `ON_START`.

## Blast radius

Single file (`RikoLiveClock.kt`), one composable function changed.
`BubbleIcon.kt` (the only consumer) needed zero changes — it just
reads `LocalRikoClock.current` and doesn't know or care how the value
is produced.

---

# Design Brief Pass 3 — Premium empty states (Playlists)

## What was actually there

Checked both screens directly rather than assuming from the pending
note:

- `PlaylistsScreen.kt` (top-level playlist list): when `playlists` is
  empty, there was no empty-state branch at all — the `LazyColumn`
  just rendered nothing. Not even placeholder text.
- `PlaylistDetailScreen.kt` (songs inside one playlist): had a real
  branch, but it was one bare `Text("No songs in this playlist", ...)`
  — no icon, no visual weight, no hierarchy.

`FavoritesScreen.kt` already has the actual target pattern, built
earlier this project with its own comment citing the requirement
directly: "Real empty state, not a bare 'no songs found' — per RIKO
Vision's requirement that empty states stay premium." Icon (48dp,
`TextSecondary`) + bold title + secondary hint line, centered.

## The fix

Both screens now use that exact same pattern — reused, not
reinvented, per the consistency rule:

- `PlaylistsScreen.kt`: `Icons.Filled.PlaylistAdd` / "No playlists
  yet" / "Tap + above to create your first playlist" — that hint is
  accurate, the `+` icon in the header really does call
  `viewModel.openCreateDialog()`.
- `PlaylistDetailScreen.kt`: `Icons.Filled.MusicNote` / "No songs
  yet" / "Songs you add will show up here".

## Honest gap surfaced, not fixed here

`PlaylistDetailScreen`'s hint deliberately does NOT say "tap here to
add songs" — checked `PlaylistDetailViewModel`/`PlaylistViewModels.kt`
first, and `addSongs(songIds: List<Long>)` → `playlistRepository
.addSongsToPlaylist(...)` is real and fully wired on the data side,
but no button anywhere on `PlaylistDetailScreen` calls it. Exactly
"existing but unwired" in this project's own terms. Writing a hint
that points at a tap target which doesn't exist would be the "mon ke
dhoka" this project explicitly rules out, so the copy stays honest
about what's actually on screen. Building the real add-songs picker
flow (library song list → multi-select → confirm) is a genuinely
separate, larger piece of UI than a two-screen empty-state polish
pass — flagging it as the natural next step rather than scope-creeping
into it here.

## Not touched this pass

SongListItem's placeholder-square system, Now Playing album art
container polish, and the broader spacing/padding audit — still ahead,
per the pending list's own order.

---

# Runtime Fix 5 — Fake specular "shine" deleted from the real hero Play button

Direct request: "দুই নাম্বারি shine বা hierarchy টা delete করো, যেটা
পুরো অ্যাপ কে unprofessional বানাচ্ছে" — delete the shady-looking
shine/hierarchy thing making the whole app look unprofessional.

## Finding the real target (checked before touching anything)

Design Brief Pass 1 already reduced the same class of "glossy hotspot"
specular oval in `BubbleIcon.kt` and `GlassSurface.kt` — but grepping
`BubbleIconButton(` (the component that fix targeted) turned up ZERO
call sites anywhere in the app. That component, including its
"reactive shine sweep," is dead code — never rendered.

Traced the actually-live Now Playing screen instead:
`RikoNavGraph.kt` → `NowPlayingScreen.kt` (confirmed — not
`NowPlayingLayout.kt`, which is the OTHER known duplicate already
flagged in `docs/FINAL_AUDIT_REPORT.md`'s release-blocker list, and
which turned out to be the one pulling in `TransportControlsV2.kt`'s
own separate, also-unfixed specular oval — also dead code, same root
confusion). The real hero Play button is
`core/designsystem/components/TransportControls.kt`'s
`HeroPlayButton` — used directly by the live `NowPlayingScreen.kt` at
line 608. It had a `drawOval` specular highlight: 0.4 alpha, sized
60%x50% of the entire 88dp button — on the single most prominent
element in the whole app.

`HeroPlayButton`'s own header comment uses the word "hierarchy"
directly: "The old row was five near-identical glass bubbles — no
hierarchy... This replaces it with one deliberate contrast: a single
heavy, dial-like hero for Play/Pause, and quiet, flat, unadorned icons
for everything else." The oversized shine blob was undermining that
exact goal — competing with the icon for attention instead of quietly
supporting it.

Also checked the other two live components on the same row/pill-bar —
`MinimalTransportButton` (Shuffle/Prev/Next/Repeat) has no shine at
all by design ("no bubble, no shadow"), and `SecondaryActionButton`
(Lyrics/Equalizer/Timer/More) uses `SmallBubbleIcon`, which — unlike
`BubbleIconButton` — genuinely is live, and genuinely did get the
Pass 1 alpha reduction already. Nothing else needed touching.

## The fix

Deleted the `drawOval(...)` block from `HeroPlayButton` outright — not
dimmed again, actually removed, per the explicit "delete" instruction.
Left the thin 0.35-alpha inner ring stroke in place (a genuine "dial
bevel" detail line, not a shine blob) since the request named "shine"
specifically.

## Why this one was missed twice

Both prior "fix" attempts (Pass 1's `BubbleIcon.kt` work, and this
session almost repeating the mistake by nearly fixing
`TransportControlsV2.kt` before checking its call sites) targeted
components that looked right by name but weren't reachable from the
live nav graph. The actual lesson: grep the call site before editing
a shared component, every time — component names and doc comments
describe intent, not what's on screen.

---

# Runtime Fix 6 — SongListItem's random colored square

## What was wrong

`SongListItem.kt` had an 8-entry hardcoded gradient palette
(`fallbackPalette`), picked per song via `song.id % 8`. Any song
without embedded album art got one of 8 unrelated color pairs — dark
purple, teal, maroon, green, blue, brown, violet, red — assigned
essentially arbitrarily. A scrolling list of songs without art looked
like a random assortment of unrelated colors with no relationship to
each other or to the rest of the app. Exactly the "cheap/template"
signature this project's own rules call out directly.

## The fix

Deleted `fallbackPalette` and the per-song `fallbackPair` lookup
entirely. Checked `theme/Color.kt` first for an existing token before
inventing a new color, per the consistency rule — found
`ColorObjectMat`, literally commented `// album art mat / backing
plate`, defined for exactly this purpose but never actually used
anywhere in the codebase. Now every song without art gets
`lerp(ColorObjectMat, accentColor, 0.18f)` — the same real
design-system neutral, lightly blended with whatever the *screen's*
real, current `accentColor` is (confirmed at each of the 4 call sites
— `AllSongsScreen`, `FavoritesScreen`, `SearchScreen`,
`PlaylistDetailScreen` — that `accentColor` is one screen-level value
passed the same way to every row, not per-song).

Result: every placeholder on a given screen looks the same (no
lottery), and still reflects the app's actual live color pipeline
instead of a static gray box — consistent with "everything alive,
everything has real logic," not consistent-but-dead.

## Not touched this pass

Now Playing album art container polish and the broader spacing/padding
audit — still ahead, per the pending list's own order.

---

# Runtime Fix 7 — Fake catch-light deleted from real album art

Continuing the album-art-container polish pass, found the same "fake
shine" pattern from RuntimeFix5 sitting on `TactileAlbumArt.kt` — the
real, live album art container used by `NowPlayingScreen.kt`.

## What was wrong

A static white `drawOval` catch-light, top-left, 0.30 alpha, radius
covering roughly half the image — drawn UNCONDITIONALLY on top of
whatever's underneath, including real embedded album art photos from
the user's own music files. Worse than RuntimeFix5's version: that one
sat on UI chrome (a plain colored circle); this one was baked directly
over actual photographic content the user didn't ask to have altered.

## The fix

Deleted the overlay outright. The component's other "physical object"
touches — a warm mat backing card, a real directional shadow, and
device-tilt-driven perspective rotation — are untouched and still do
the intended job on their own; the static decal wasn't adding to that
effect, it was undermining it.

---

# Spacing/Padding Audit — Pass 1 (screen-edge alignment)

Checked adoption of the project's own existing dimension system first:
`theme/Dimensions.kt`'s `RikoDimens` object has an explicit header
comment — "Never hardcode dp values inside UI. Always use RikoDimens."
— but only 8 of 289 files in `presentation/` actually reference it.
That's a real gap, but retrofitting all 289 files in one pass is a
different, much larger, higher-risk undertaking than a spacing check;
flagging it here rather than attempting it silently or ignoring it.

For this pass, focused on the single most visible spacing signal — the
horizontal edge padding used by every live top-level screen (confirmed
live via `RikoNavGraph.kt`, not assumed from file names, per this
session's own repeated lesson) — and measured the real values instead
of guessing:

- `AllSongsScreen`, `FavoritesScreen`, `PlaylistsScreen` headers: 20dp
  — matches `RikoDimens.screenHorizontalPadding` (also 20dp).
- `SearchScreen`: whole screen was 16dp — the one outlier among the 4
  top-level tabs. Switching Home → Search visibly shifted the left
  edge by 4dp.
- `PlaylistDetailScreen`: header row was 16dp, but its own Play
  All/Shuffle row directly below was 20dp — an internal mismatch on
  the same screen, not just a cross-screen one.

## The fix

Both outliers moved to 20dp, matching the majority, the already-named
token, and (for PlaylistDetailScreen) the same screen's own other row.

## Not done in this pass

Vertical padding/rhythm across screens, corner-radius consistency, and
the 289-file `RikoDimens` adoption gap — noted, not attempted here.
Vertical spacing naturally varies more legitimately by content shape
(a header with a 44dp back button vs. a plain title) than horizontal
edge alignment does, so it needs a closer look rather than a quick
grep-and-compare pass like this one.

---

# Runtime Fix 8 — Add-songs picker (the "existing but unwired" gap, closed)

Already flagged twice in earlier passes: `PlaylistDetailViewModel
.addSongs(songIds)` → `PlaylistRepository.addSongsToPlaylist()` was
real and fully wired on the data side, but nothing on
`PlaylistDetailScreen` could call it. Per this project's own naming for
that state — "existing but unwired = missing" — this was a real,
missing feature, not just polish, so it got its own pass rather than
staying a permanently-flagged gap.

## What was checked before building anything

- `MusicRepository.getAllSongs()` already existed and was already
  injected into `PlaylistDetailViewModel` — no new dependency needed.
- Looked for an existing multi-select pattern to reuse first, per the
  consistency rule. Found an entire, sophisticated one:
  `presentation/library/interaction/` (`SelectionRuntime`,
  `SelectionReducer`, `InteractionValidator`, plus a whole
  Contract→State→Event→Reducer→Coordinator→Mapper→UI engine under
  `presentation/library/`). Grepped every live screen and
  `RikoNavGraph.kt` for any reference to `presentation.library.*` —
  zero hits. That entire subsystem is dead code, unreachable from the
  app exactly like `BubbleIconButton`/`TransportControlsV2`/
  `NowPlayingLayout` turned out to be earlier this session. Flagging
  it here as a significant discovery, not fixing/reviving it — that's
  a much bigger decision than this pass, and not mine to make
  unprompted.
- Checked the existing `ModalBottomSheet` pattern in `RikoNavGraph.kt`
  (Queue/EQ/Lyrics/Info/Sleep sheets) and matched it exactly:
  `containerColor = Transparent`, `dragHandle = null`,
  `GlassBottomSheetContainer`, `Modifier.fillMaxHeight(0.85f)` on the
  sheet + `Modifier.weight(1f)` on the scrollable content — same as
  `QueueSheet`, since a song picker over a whole library is the same
  shape of problem (long scrollable list in a sheet).

## The fix

- `PlaylistDetailViewModel`: added `pickableSongs` (full library minus
  songs already in this playlist), sheet open/close state, and
  selection tracking (`Set<Long>`). Caught a real Kotlin
  forward-reference bug while writing this — `pickableSongs`'
  initializer read `uiState`, but I'd placed it above `uiState`'s own
  declaration in the class body; Kotlin initializes properties
  top-to-bottom, so that's a genuine compile error, not just a style
  issue. Reordered.
- New file `AddSongsSheet.kt`: search-filterable list with tap-to-select
  circles, confirm button that only appears once something's selected
  (no disabled-button clutter). Deliberately NOT built from
  `SongListItem`/`GlassSurface` per row — a picker over a potentially
  multi-thousand-song device library needs cheap rows, and
  `GlassSurface`'s own code comments already document blur/specular
  per-row cost as "murder on a scrolling list." Flat rows with a
  tinted background for the selected state instead.
- Reused `cleanSongTitle`/`cleanArtistName` from `SongListItem.kt`
  rather than duplicating the filename-cleanup regex logic — widened
  their visibility from `private` to `internal` (zero behavior change
  for the existing caller, `internal` is module-wide so the new
  package can reach them).
- `PlaylistDetailScreen`: added a header icon (gated the same way as
  the existing Edit button — hidden for the auto-managed Favorites
  playlist, which adds songs via the heart toggle elsewhere, not this
  picker) and a real CTA button in the empty state. Updated the empty
  state's hint text now that it's actually true, and made it
  conditional — the Favorites-playlist case gets its own accurate
  copy instead of one hint trying to cover both cases.

## Self-caught bug while building this

The unselected-state selection indicator's first draft used nested
`Box`es with `Color.Transparent` backgrounds, intending to draw an
outlined ring — that renders nothing visible at all. Caught before
shipping, replaced with an actual `Modifier.border(...)` ring.

## Stopping point

Per direct instruction, not proceeding into Now Playing screen work
from here — that has its own separate task queued.

---

# Runtime Fix 9 — Waveform timeline: full rewrite from the reference app

The project owner built and uploaded a complete standalone reference
app (`riko-fluid-wave-timeline`) specifically to show what this
component should look like — direct instruction: match it exactly.
This is the largest single change this session.

## What the reference actually is

Not a bar-graph spectrum analyzer. `RikoWaveTimeline.tsx` +
`audioEngine.ts` render a continuous 3-layer fluid plasma wave whose
HEIGHT and FLOW SPEED react to live audio energy, with a hollow-ring
playhead trailed by a colored glow halo. Read both files fully before
writing anything — the exact math (wave equations, envelope fades,
damping rates, gradient stops, ring geometry) is documented inline in
the new file's header comment as a reference-to-RIKO mapping table.

## What RIKO's old version actually was

Checked before touching anything: the previous `WaveformTimeline.kt`
was a genuinely different algorithm — a "flame/taper" shape growing
from x=0, thin at both ends via a sin-taper, additively blended
(`BlendMode.Plus`), shaped by a per-track `realEnvelope` (decoded
audio amplitude buckets varying along the whole song). The reference
uses none of that — uniform wave shape along the played region except
for a short entry/exit fade at the edges; only the CURRENT live energy
changes amplitude, not "how loud this part of the song was." This is
why the earlier attempt didn't match what was asked for — it was
solving a different problem, not an inferior version of the same one.
Matched the reference exactly rather than blending the old per-track
behavior back in.

## Porting decisions (each one deliberate, not a guess)

- **Frequency bands**: reference used raw Web Audio FFT bins
  (bass/vocal/rms from `AnalyserNode`). RIKO already has a more
  considered real signal for this exact purpose —
  `core/experience/wave/WaveEnergy.kt`'s Energy Solver output
  (`bassPressure`, `midTexture` as the closest real analog to "vocal,"
  `overallEnergy` as an already-composed bass+mid+treble+attack+decay
  signal, better-considered than a raw spectrum average). Used the
  real pipeline instead of trying to replicate Web Audio's FFT bins on
  Android.
- **Animation loop**: `requestAnimationFrame` → `LaunchedEffect` +
  `withFrameNanos`, gated to real Activity visibility via the same
  `LifecycleEventObserver` pattern from RuntimeFix4 — applied from the
  start this time, not retrofitted after the fact. A continuous
  per-frame loop needs a real stop, every time, no exceptions now.
- **Stale-closure risk**: the loop needs to read live `isPlaying`,
  `progress`, and energy values every frame, but
  `LaunchedEffect(isForeground)` only restarts when `isForeground`
  changes — so without `rememberUpdatedState` on each of those, the
  loop would freeze at whatever values existed the instant it first
  launched. Wrapped each one.
- **Smoothing math**: the reference's seek-glide and thumb-scale
  damping are flat per-frame factors (`0.22`, `0.45`, `dt*14`) that
  assume a ~60Hz `requestAnimationFrame`. Android's variable refresh
  rate (60/90/120Hz+) would make those factors converge at different
  real-world speeds on different devices. Converted to dt-scaled
  continuous rates that match the reference's feel at 60Hz but stay
  correct at any refresh rate — a disclosed adaptation, not a
  deviation from the design.
- **Pixel vs. ratio-space smoothing**: reference smooths the seek-head
  position in raw canvas pixels. Ported to ratio space (0..1),
  resolved to pixels at draw time — mathematically identical since
  ratio→pixel is a fixed linear map for a given width, but survives
  width/rotation changes without extra handling the reference didn't
  need (fixed-size browser canvas).
- **Ring drop-shadow**: reference uses Canvas2D's native `shadowBlur`
  behind the playhead ring. Compose `DrawScope` has no direct
  equivalent; a real `Modifier.blur` layer for a detail this small
  (a few px) wasn't worth a whole extra Canvas pass, so it's
  approximated with a soft dark circle instead. Disclosed
  simplification — the actual wave-glow blur (the big, visually
  important one) DOES use real blur, same as the previous version.

## Self-caught bug while porting

`Brush.linearGradient`/`radialGradient` take `vararg colorStops:
Pair<Float, Color>`. First draft wrote `colorStops = arrayOf(...)` as
a named argument — that's a type mismatch in Kotlin; assigning an
array to a vararg parameter needs the spread operator
(`colorStops = *arrayOf(...)`). Caught in review before packaging,
fixed in all three occurrences (both fill gradients + the glow
gradient).

## Also caught: an unused parameter I almost shipped

Kept a `trebleEnergy` parameter in the new signature at first, for
symmetry with the old API — but the reference's `freqEnergy` formula
genuinely only uses bass/vocal/rms, so `trebleEnergy` would have sat
there completely unused. That's exactly the "existing but unwired"
pattern flagged and fixed everywhere else this session; leaving it
here would be a double standard. Removed it from both the component
signature and the one call site.

## What I couldn't verify from here

Same sandbox limitation as every fix this session — no `google()`
Maven access, so no real `./gradlew build`. Verified by hand: brace/
paren/bracket balance, every Compose API used (`Path.quadraticTo`,
`Stroke` named params, `drawCircle` overloads, `Brush.linearGradient`/
`radialGradient` vararg signatures, `Modifier.blur` API-gating)
checked against known Compose signatures, and grepped for stale
references after removing `trebleEnergy` and the old `realEnvelope`
plumbing. This is by far the most complex single file changed this
session — CI or a real device is the next real checkpoint, more so
than usual.

---

# Runtime Fix 10 — ConcurrentModificationException crash on track transition (confirmed via real device logcat)

The project owner reported the app fully stopping responding — audio
kept playing but the UI froze, sometimes with an ANR-style popup,
sometimes an instant kill — specifically at track auto-advance, and
provided a full device logcat. This is the first fix this session
backed by an actual crash log rather than static-analysis reasoning
alone, and it's a real, confirmed bug — not something introduced by
the WaveformTimeline rewrite in RuntimeFix9.

## The exact crash

```
FATAL EXCEPTION: DefaultDispatcher-worker-2
java.util.ConcurrentModificationException
  at java.util.AbstractList$Itr.checkForComodification
  at java.util.AbstractList$Itr.next
  at EnergyHistory.getAverageEnergy(AudioBands.kt)
  at ExperienceDataHub.detectBeat(ExperienceDataHub.kt)
  at ExperienceDataHub.publishFrame(ExperienceDataHub.kt)
  at ExperienceDataHub.tryPublish(ExperienceDataHub.kt)
  at ExperienceDataHub.feedPcmFrame(ExperienceDataHub.kt)
  at ExperienceDataHub$startBridges$3$1.invokeSuspend(ExperienceDataHub.kt)
```

The log's own line numbers didn't line up 1:1 with this zip's current
`AudioBands.kt` (123 lines here vs. line 1628 in the trace) — the
device build was from a different snapshot — but every class/method
name in the trace maps directly onto real, current code, so the root
cause investigation is solid regardless of the exact line offset.

## Root cause

`EnergyHistory` (in `AudioBands.kt`) wraps an `ArrayDeque<AudioBands>`
with zero synchronization. Its own doc comment claimed `push()` is
"called ONLY by the Experience Data Hub (single writer)" — true for
`push()` alone, but `clear()` is a second writer, called from
`ExperienceDataHub.updatePlaybackState()` on pause/track-change
detection. Both `push()`/`clear()` (writers) and `averageEnergy`/
`energyTrend`/`peakEnergy` (readers, via `.map{}`/`.takeLast{}`
iteration) were completely unsynchronized.

The reason this specifically fires on track transitions: reading
`ExperienceDataHub`'s bridge wiring (RuntimeFix3), Bridge D
(player state → `updatePlaybackState`), Bridge E (PCM →
`feedPcmFrame`), and Bridge F (Visualizer → `feedVisualizerFrame`) are
three separate coroutines all launched on the SAME
`CoroutineScope(Dispatchers.Default)`. Unlike `Dispatchers.Main`,
`Dispatchers.Default` is a real thread POOL — these three coroutines
can and do run on genuinely different physical threads at the same
instant, not just interleaved on one thread. A track transition is
exactly the moment `updatePlaybackState`'s pause-detection branch
(`energyHistory.clear()`) is most likely to fire (briefly, during the
buffering/re-attach step visible in the logcat right before the
crash — `VisualizerManager: Initializing Visualizer with session ID:
22145`, then an `AudioTrack: pause(...)`) at the same moment Bridge
E/F's `publishFrame() → detectBeat() → energyHistory.averageEnergy`
is actively iterating the same buffer on a different thread. Classic
`ConcurrentModificationException`: `ArrayDeque` isn't thread-safe, and
nothing made it act like it was.

This also explains the reported "sometimes a popup, sometimes an
instant kill" — that's not two different bugs, it's the same race
landing at a different point in the interleaving each time, which is
the signature of a genuine race condition rather than a deterministic
one-line bug.

## Investigation before this — what's already ruled out

Before the logcat arrived, checked and ruled out by direct code
reading (not guessing): `progress` division-by-zero (already guarded,
`if (duration > 0) ... else 0f`), palette extraction threading
(correctly on `Dispatchers.Default`), waveform decode threading
(correctly on `Dispatchers.IO`), AudioFx/Visualizer re-init exception
handling (already try/caught), and the position ticker's job
lifecycle (properly cancelled/reassigned, no accumulation). None of
these were the cause — good to have ruled them out concretely rather
than leaving them as open questions.

## The fix

Two layers, both necessary:

1. **`EnergyHistory` (`AudioBands.kt`)** — made genuinely
   self-synchronized: every method that touches the internal buffer
   (`push`, `clear`, `latest`, `size`, `isFull`, `averageEnergy`,
   `energyTrend`, `peakEnergy`) now runs inside `synchronized(lock)`
   on a dedicated private lock object. This directly fixes the exact
   crash, and — because this `EnergyHistory` instance is also exposed
   externally via `AudioState.energyHistory` to UI/other engines — it
   protects every caller, not just `ExperienceDataHub`'s own internal
   bridges, which a lock living only inside `ExperienceDataHub` could
   not have done.
2. **`ExperienceDataHub` itself** — added its own private lock and
   wrapped `feedPcmFrame`, `feedVisualizerFrame`, `updatePlaybackState`,
   `publishFrame`, and `reset()` in `synchronized(lock)`. Reading the
   whole file end-to-end while diagnosing the crash surfaced a wider
   issue than the one exact exception: Bridge E and Bridge F can both
   enter `publishFrame()` at the same instant with nothing serializing
   them, so `previousEnergy`, `beatDecay`, `lastBeatTime`,
   `lastFrameTime`, `activePcmFrame`, and `activeVisualizerFrame` were
   all exposed to the same class of race, just without an iterator to
   throw a named exception — more likely silent data corruption
   (lost updates) than a crash, but the same root flaw. This directly
   enforces the class's own already-stated design ("⭐ GOLDEN RULE ⭐
   ... Only Experience Hub writes") rather than adding new behavior —
   the intent was already correct, the enforcement was missing.
   `EnergyHistory`'s lock and `ExperienceDataHub`'s lock are two
   separate objects with simple one-way nesting (Hub's lock held,
   then briefly acquires EnergyHistory's own lock for `push()`) — no
   circular wait, no deadlock risk. None of the synchronized bodies
   contain suspension points (`delay`, awaiting another coroutine),
   so plain JVM `synchronized` is safe here — no risk of blocking a
   dispatcher thread indefinitely.

## Not changed

`updateMetadata`/`updateChannelInfo` still read-modify-write
`_audioState.value` outside the lock. Lower risk than what's fixed
above — a race here would just mean a metadata/channel-info update
gets transiently overwritten and picked up again on the next frame,
not a crash — so left alone rather than expanding scope further on a
lower-severity path while the confirmed crash was the priority.

## Confidence level

Higher than any other fix this session — this one is backed by an
actual stack trace with exact class/method names matching real code,
not architectural reasoning about what could theoretically go wrong.
Still can't run a real device test from this sandbox, so: please
reinstall and specifically try to reproduce several track transitions
in a row (the race won't fire every single time — it's timing-
dependent) and let me know if it's actually gone.

---

# Runtime Fix 11 — StackOverflowError from a systemic event/action cycle in VisualOsCoordinator (confirmed via a second real device logcat)

RuntimeFix10 was real and effective — the second logcat confirms it:
no more `ConcurrentModificationException`. But it wasn't the only
crash. Same reproduction (track transition), a different device
crash log, a completely different exception:
`java.lang.StackOverflowError: stack size 8188KB`, `FATAL EXCEPTION:
main`, thousands of alternating `VisualOsCoordinator.dispatch` /
`executeActions` stack frames.

## Root cause

`VisualOsCoordinator.executeActions()` runs the actions a reducer
produces. Several actions' handlers were implemented as
`dispatch(VisualOsEvent.X(...))` — re-entering the whole event →
reduce → action pipeline instead of applying an effect directly.
That's a legitimate pattern in general ("actions may result in events
being dispatched back, closing the loop" — genuinely true for things
like `CompleteTransition → TransitionCompleted`, which terminates
correctly). The bug: four specific pairs where the REDUCER handler for
event X *also* emitted the action whose *only* handler re-dispatches
X — an unconditional, information-free 2-step cycle with no exit
condition:

- `AlbumArtColorsExtracted` (event) → `UpdateBackgroundColors` (action) → dispatches `AlbumArtColorsExtracted` again
- `OverlayShown` (event) → `ShowOverlay` (action) → dispatches `OverlayShown` again
- `OverlayHidden` (event) → `HideOverlay` (action) → dispatches `OverlayHidden` again
- `DockAppeared` (event) → `ShowDock` (action) → dispatches `DockAppeared` again

In every one of the four cases, the reducer handler had ALREADY
applied the real state change directly (`state.copy(backgroundState =
...)`, `state.copy(overlayState = ...)`, `state.copy(dockState =
...)`) in the exact same function — the mirrored action carried no
additional information and served no purpose the direct state update
hadn't already served. It just closed a loop back on itself, forever.

The specific crash in the logcat was the color-extraction cycle
(fires on every song's album art). The dock cycle
(`DockAppeared`/`ShowDock`) is arguably higher-risk going forward —
checked its guard condition and it only skips the cycle when
`NowPlaying` is the current focus scene; navigating away from Now
Playing during playback (a very ordinary action) would trigger the
identical crash on the main thread. Found this by reading the whole
file systematically after finding the first instance, not by waiting
for a third crash report.

## The fix — two layers, applied to all four

1. **Reducer (`VisualOsReducer.kt`)**: `handleColorsExtracted`,
   `handleOverlayShown`, `handleOverlayHidden`, `handleDockAppeared`
   no longer emit the redundant mirrored action — each now returns
   `emptyList()` for actions, since the state update directly above
   already does the real work.
2. **Coordinator (`VisualOsCoordinator.kt`)**: `UpdateBackgroundColors`,
   `ShowOverlay`, `HideOverlay`, `ShowDock` handlers now apply state
   directly (mirroring the reducer's own `state.copy(...)` shape)
   instead of dispatching an event. This matters beyond just closing
   the four found cycles: grepping every emission site turned up
   `ShowDock` and `HideOverlay` ALSO being emitted from other,
   legitimately different reducer functions (`handleSongChanged`,
   `handleBackPressed`, a dock-visibility-change handler) that were
   NOT part of the tight 2-step cycle but would still have hit the
   same "re-dispatch instead of apply" bug if triggered — those sites
   were intentionally left alone (their emissions aren't redundant,
   each carries real information), and are now safe automatically
   because the Coordinator-level fix protects every caller, not just
   the four traced cycles.

## Verification method

After finding and fixing the first cycle (colors), didn't stop there —
went through every remaining `dispatch(VisualOsEvent...)` call still
in `executeActions` and traced each one's corresponding reducer
handler by hand, checking whether it emits the mirroring action back.
Confirmed clean (genuinely one-way, no cycle): `PopScene`/`PopToScene`
→ `LayerPopped`, `ReplaceScene` → `SceneNavigated`,
`CompleteTransition` → `TransitionCompleted`, `CancelTransition` →
`TransitionInterrupted`, `HideDock` → `DockDisappeared`,
`ExtractAlbumArtColors`'s own dispatch (the genuine, correct origin
point for `AlbumArtColorsExtracted`, not a mirror-back),
`handlePanelOpened` → `BeginTransition` (calls
`startTransitionAnimation` directly, no dispatch at all),
`handlePanelDismissed` → `CompleteTransition` (already confirmed
one-way). Four cycles found, four fixed, rest confirmed clean by
direct inspection rather than assumed clean by omission.

## Confidence level

Same tier as RuntimeFix10 — backed by a real stack trace, not
speculation. The systemic sweep beyond the one traced cycle is
reasoned from the same evidence (the exact bug shape, confirmed once,
checked everywhere else it could recur) rather than guessed.
Reinstalling and testing several track transitions plus general
navigation (opening/closing overlays, panels, switching away from Now
Playing during playback) is the real next checkpoint.
