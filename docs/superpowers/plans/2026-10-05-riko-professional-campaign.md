# RIKO Professional Campaign — Master Implementation Plan (v3)

> For agentic workers: REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans`.
>
> Session contract from `AGENTS.md`: one wave-slice per session → inspect → edit → recheck → commit → push → STOP. CI result comes from the user.
>
> Core principle: **RIKO must not merely compile. RIKO must feel professional in the user's hand.**
>
> Vision: **“Now Playing হলো product; বাকি সব তার orbit.”**

---

# 1. CAMPAIGN OBJECTIVE

RIKO must become a premium, production-grade offline music player that:

* launches into a genuinely ready experience instead of a decorative splash followed by loading;
* feels instant under touch;
* has smooth list scrolling and sheet motion;
* has consistent gestures, haptics and touch feedback;
* makes Now Playing feel alive through music-reactive album-art-driven motion;
* provides a real Visualizer Studio/workbench;
* provides a complete professional Equalizer;
* maintains architectural consistency;
* survives real-world lifecycle, interruption and playback edge cases;
* is measurable, reproducible and regression-resistant.

### Important honesty rule

“100% smooth” remains the PRODUCT GOAL.

Engineering must not claim “100% smooth” merely because the app builds or CI is green.

Every performance claim must be backed by:
`[M] measured`, `[R] researched`, `[V] device-verified`, `[N] not yet verified`.

---

# 2. NON-NEGOTIABLE OPERATING RULES

1. Never blindly trust old line numbers. Re-locate the symbol/function before editing.
2. Inspect the existing implementation before proposing replacement.
3. For every bug, identify:
   `Symptom → Root cause → Evidence → Fix → Verification`.
4. Do not patch around a systemic problem when the underlying mechanism can be fixed.
5. Do not rewrite working canonical systems without evidence.
6. Do not introduce new libraries unless first-party/approved and justified.
7. Never mass-replace code purely for visual consistency. Perform semantic migration.
8. Never delete code/assets until reachability and runtime usage are proven.
9. Never add a second canonical system for something RIKO already has.
10. No feature is “done” until its user-visible path is reachable and verified.
11. If a gate fails, the next wave does not start.
12. If a new technology is proposed, first prototype it in a performance-lab slice and promote it only if measurement proves it better.
13. Do not turn SplashScreen into a blocking IO container. Splash is a visual/transition surface; readiness is a separate system.
14. High-frequency audio/animation state must never cause unnecessary composition-wide recomposition.
15. Prefer draw-phase/layout-phase updates for continuously changing visual parameters.
16. No arbitrary allocation in per-frame rendering paths.
17. Every external sample/template/source gets a provenance/license check before code is copied into production.
18. Preserve the user's branch rules: never push `main`; campaign work stays on the approved feature branch.

---

# 3. ARCHITECTURE FREEZE / CANONICAL SYSTEMS

Preserve these unless evidence proves a defect:

* `RikoArtwork`
* `RikoTrackRow`
* `RikoTopBar`
* `RikoShape`
* `RikoCornerRadius`
* `RikoTypography`
* `RikoMaterialTypography`
* `RikoMotion`
* `RikoStateTokens`
* `RikoHaptic`
* `PlayerSheetHost`
* single `PlayerManager`
* `PcmFrames → SpectrumEngine → SpectrumBands`

Frozen tree:

`presentation/library/**`

Do not directly modify frozen-library implementation except through its existing public APIs.

The frozen `LibraryGestureMotion` pattern is the reference for gesture design, not something to duplicate blindly.

---

# 4. NEW CAMPAIGN ARCHITECTURE

The campaign introduces only these shared systems:

### Performance layer

* shared `LibraryRepository` cache
* stable artwork request pipeline
* artwork cache/prefetch strategy
* Baseline Profile
* Macrobenchmark scenarios
* performance diagnostics

### Interaction layer

* `RikoTouch`
* `RikoGesture`
* `RikoHaptic`

### Visual layer

* `AuroraFlow`
* Canvas fallback
* shared musical-reactivity shaping

No competing parallel systems.

---

# 5. WAVE 0 — EVIDENCE BEFORE OPTIMIZATION

## Purpose

Do not optimize blind.

Create a reproducible baseline before changing performance-critical code.

### Measure

Capture:

* cold startup TTID
* cold startup TTFD
* warm startup
* hot startup
* Home first usable state
* list fling frame timing
* Queue scroll frame timing
* Now Playing sheet expand/collapse
* EQ drag
* visualizer animation
* memory footprint during long scroll
* CPU usage during Now Playing animation
* 30-minute playback soak

### Scenarios

1. My Music list fling
2. Favorites fling
3. Search result fling
4. Queue scroll
5. Queue reorder
6. Now Playing open/close
7. Now Playing scrub
8. EQ interaction
9. Visualizer
10. Home startup

### Instrumentation

Use:

* Macrobenchmark
* `StartupTimingMetric`
* `FrameTimingMetric`
* Perfetto traces
* recomposition diagnostics
* Layout Inspector where useful
* release-like build with R8/shrinking where possible

Record P50/P90/P95/P99 instead of looking only at averages.

### Output

Create:

`docs/performance/PERF_BASELINE.md`

This becomes the campaign's numerical truth.

---

# 6. WAVE 1 — LAG KILL

## 1.1 Kill monolithic PlayerState recomposition

Replace high-frequency `playerState` collection with granular flows wherever list screens only require:

* song
* isPlaying
* queue
* current index

Do not pass position ticks through list rows.

### Acceptance

While playback position changes:

* visible rows must not recompose merely because position changed;
* no list-wide invalidation;
* behavior remains identical.

---

## 1.2 Artwork pipeline stabilization

Create:

`core/designsystem/riko/components/ArtworkRequestFactory.kt`

Requirements:

* stable cache identity
* stable request creation
* explicit requested size
* no unnecessary crossfade in dense lists
* hero artwork may use controlled short crossfade
* album identity must remain part of cache identity

Before creating a custom thumbnail cache, run a micro-test against the current Coil version.

Do NOT assume that custom `WebP + cacheDir/thumbs` is automatically better.

Only introduce custom thumbnail storage if measurement proves the current Coil/content-URI path is the bottleneck.

Cache-key contract should account for:

`source identity + requested size + transformation version`

---

## 1.3 Row composition hygiene

Fix:

* unstable keys
* expensive work in composition
* repeated sorting
* repeated `indexOfFirst`
* repeated duration formatting
* string/URI construction inside rows
* animated values that needlessly update every item
* missing `contentType`
* per-row shape construction
* unnecessary interaction sources

Use stable IDs as keys.

Use `itemsIndexed` where index is already available.

Use `remember`/precomputed UI models only where they actually remove repeated work.

---

## 1.4 Lazy-list performance

Audit every LazyColumn/LazyRow:

* stable key
* content type
* item identity
* scroll-state reads
* derived state
* expensive layout
* nested scrolling
* nested glass effects
* image loading
* per-item animations
* semantics overhead

Do not optimize only the visible 23-song test case. Test large libraries.

---

## 1.5 Root-scope invalidation kill

Audit:

* `MainActivity`
* navigation root
* VisualOS coordinator
* PlayerSheetHost
* Now Playing background
* color state
* 60Hz/30Hz animation loops

Continuously changing state must not invalidate unrelated screens.

High-frequency motion should remain as close as possible to the drawing/render phase.

---

## 1.6 Baseline Profile + release-like benchmark

Wire:

* Baseline Profile
* Startup Profile where appropriate
* Macrobenchmark scenarios
* release-like benchmark build
* profileable configuration
* profile installer

Generate profiles from real user journeys, not arbitrary startup code.

Include:

* startup
* library first view
* list fling
* queue
* Now Playing
* player-sheet morph

---

## 1.7 Performance Lab escape hatch

Only if Wave 1 cannot reach the gate:

create a temporary isolated experiment area/branch.

Candidates may include:

* `Modifier.Node` based interaction
* draw-phase state delivery
* alternate list rendering strategy
* lower-level rendering path

No experimental technology enters production merely because it looks faster.

It must beat the current implementation under measurement.

---

# 7. WAVE 1 GATE

The user experience target is:

**No visible stutter during normal fast fling.**

Measured gate:

* no regression from Wave 0;
* P95 frame timing must meet the established device budget;
* P99 must remain within the agreed campaign tolerance;
* position ticks must not cause visible list-row recomposition;
* no new per-frame allocations in audited paths.

Debug-mode visual impressions do NOT count as final performance evidence.

---

# 8. WAVE 2 — CANONICAL TOUCH + GESTURE SYSTEM

## 2.1 EQ band drag — real root-cause fix

Rewrite the EQ gesture.

Requirements:

* fresh state through `rememberUpdatedState`
* true band hit-test
* axis-locked gesture
* slop arbitration against vertical sheet scrolling
* long-press grab if that is the intended interaction
* consume only after the gesture has captured
* value follows finger position continuously
* release cleanly

Important:

The final implementation must actually implement the intended long-press UX.

A normal slop detector alone is not equivalent to long-press grab.

Visual response:

* grab scale
* subtle glow
* selected-band emphasis
* throttled haptic ticks

Do not fire haptic events so frequently that the gesture itself becomes noisy.

---

## 2.2 Queue reorder

Fix:

* unstable keys
* index-based identity
* stale captured indices
* density conversion
* reorder state after swaps

Drag identity must follow:

`Song ID`

not queue position.

Test at least five consecutive swaps in one continuous interaction.

---

## 2.3 Canonical RikoTouch

Create:

`core/designsystem/riko/gesture/RikoTouch.kt`

Include:

* 48dp minimum target
* PressScale
* GrabScale
* GrabRadius
* semantic motion constants

Create:

`RikoGesture.kt`

Provide reusable primitives:

* `rikoPress`
* `rikoTap`
* `rikoAxisDrag`
* `rikoGrabDrag`
* `rikoSeek`

All must use one interaction vocabulary.

---

## 2.4 App-wide gesture sweep

Audit:

* sheet dragging
* seek bar
* sliders
* queue reorder
* EQ
* timer
* tabs
* icon buttons
* draggable visualizer controls
* bottom navigation
* expandable cards
* any `pointerInput`

Rules:

* no stale closure
* axis locking where needed
* consume only after capture
* 48dp targets
* stable IDs
* correct dp→px conversion
* remembered interaction sources
* one consistent press response
* one consistent haptic vocabulary

---

# 9. WAVE 2 GATE

Manually verify:

* EQ drag tracks finger 1:1
* sheet does not steal the drag
* queue reorder survives five swaps
* seek does not accidentally collapse the sheet
* touch feedback feels consistent across screens
* no new gesture conflicts

---

# 10. WAVE 3 — REAL STARTUP / REAL READINESS

## 3.1 Shared LibraryRepository

Replace repeated MediaStore scans with one shared repository/cache.

Requirements:

* one canonical source
* app-lifetime cache
* derived albums/artists/folders computed once where possible
* ContentObserver invalidation
* coalesced/debounced refreshes
* no refresh storm when multiple media events arrive

Important:

Do not use `WhileSubscribed` in a way that accidentally destroys the intended app-level cache.

The repository must actually retain useful state across screen transitions.

---

## 3.2 Readiness model

Split startup into two classes:

### Critical readiness

Required before Home is considered interactive:

* library snapshot
* essential settings
* navigation state
* essential player state
* favorites/playlist essentials

### Background warmup

After the app is already useful:

* secondary artwork
* waveform preparation
* noncritical aggregate calculations
* secondary cache work
* optional diagnostics

This prevents the “real preload” system from becoming another startup bottleneck.

---

## 3.3 SplashScreen

Use the platform/compat splash correctly.

Splash behavior:

`System splash → RIKO branded transition → already-useful Home`

The splash may remain while critical readiness is incomplete, but the keep-on-screen check itself must remain extremely cheap.

Never perform IO directly inside the splash keep-condition callback.

The animation can run while readiness tasks execute.

Progress should reflect real readiness stages where meaningful.

---

## 3.4 Fully-drawn reporting

Integrate Compose/app startup measurement with real “ready for interaction” state.

The app must have a trustworthy TTFD signal.

The campaign must not artificially report fully drawn before the important Home content is actually usable.

---

## 3.5 Startup initialization audit

Audit and defer:

* ExoPlayer creation
* Equalizer engine initialization
* expensive dependency graphs
* SharedPreferences reads
* DataStore reads
* Room work
* artwork decoding
* waveform work

Do not move everything off the main thread blindly.

Instead:

`critical → early`

`noncritical → deferred`

`cold-cache work → background`

---

# 11. WAVE 3 GATE

### Fresh install

One intentional loading phase.

Then:

Home appears usable.

### Cached reopen

Home appears quickly with real cached content.

No:

`splash → Compose splash → skeleton → second loading state`

chain.

No fake “instant launch” achieved by simply hiding work behind another spinner.

---

# 12. WAVE 4 — NOW PLAYING AURORA EXPERIENCE

## 4.1 Musical reactivity shaping

Separate:

### Slow state

* album palette
* theme colors
* long-term atmosphere

### Fast state

* bass
* mid
* treble
* transient
* beat confidence
* energy momentum

Never use quantized color state as the primary motion signal.

Motion needs smoothing without destroying musical attack.

Use:

`attack + sustain + release`

rather than a clock-driven generic pulse.

Idle mode may use a very subtle breathing fallback only when audio activity is absent.

---

## 4.2 AuroraFlow

Create:

`presentation/player/aurora/AuroraFlow.kt`

Primary path:

`API 33+ → RuntimeShader / AGSL`

Fallback:

`API < 33 → Canvas`

Same parameter contract.

Uniforms:

* bass
* mid
* treble
* beat
* transient
* phase
* artwork-derived colors

Performance rules:

* shader remembered
* brush remembered
* no shader construction per frame
* no object allocations per frame
* no composition-driven 30/60Hz invalidation
* draw-phase animation
* adaptive quality tier
* reduced-motion support

Do not target an arbitrary GPU milliseconds number without device evidence.

Use frame-budget evidence instead.

---

## 4.3 Visual direction

Do not clone another platform literally.

Target the same emotional category:

**fluid + colourful + premium + restrained + musical**

The visual language should remain recognizably RIKO.

Album art controls:

* hue family
* secondary accent
* atmosphere

Music controls:

* movement
* intensity
* attack
* breathing

This creates:

`Artwork = identity`

`Music = motion`

---

## 4.4 Plasma timeline

Wire the existing plasma asset into the Now Playing seekbar.

But first:

* remove draw-loop allocations
* fix API fallback
* fix seek-on-down behavior
* use canonical `rikoSeek`
* consume drag only after correct capture

Do not reuse a dead renderer simply because it already exists.

It must pass the performance gate.

---

# 13. WAVE 4 GATE

Play multiple musical genres.

Verify:

* bass creates visible expansion
* high-frequency material adds detail
* transients produce controlled flashes
* colors remain artwork-driven
* animation never becomes noisy
* scrub remains smooth
* 60Hz and 120Hz remain responsive
* reduced-motion mode remains comfortable

---

# 14. WAVE 5 — VISUALIZER STUDIO

## 5.1 Source/template investigation FIRST

Before importing any code from:

`rare files to use`

perform a source inventory.

Create:

`docs/visualizer/VISUALIZER_SOURCE_INVENTORY.md`

For each source record:

* filename/project
* purpose
* license
* attribution requirement
* reusable algorithm
* reusable code
* incompatible code
* final RIKO usage

Never copy external code into production until its license/provenance is acceptable.

Conceptual math can be reimplemented independently when needed.

---

## 5.2 Studio styles

Target a professional set, not an endless pile of presets.

Core styles:

1. Classic Spectrum
2. Aurora / Plasma Ridge
3. Spectrogram / Waterfall
4. Shape / Orbital

All styles consume the canonical:

`SpectrumEngine → SpectrumBands`

No alternate FFT pipeline.

---

## 5.3 Spectrogram

Use playback spectrum history.

Do NOT request microphone access.

RIKO remains playback-centric and does not gain `RECORD_AUDIO` merely to make the studio look more professional.

Implement rolling history efficiently.

Avoid recreating large images/path structures every frame.

---

## 5.4 Rotary controls

Introduce:

`RikoRotaryKnob`

for:

* sensitivity
* glow
* density
* motion speed

Requirements:

* smooth drag
* touch target
* haptic tick
* no gesture conflict
* predictable values
* no excessive recomposition

---

## 5.5 Preset system

Schema:

```text
version
style
theme
sensitivity
glow
density
shape
motionSpeed
reactivityCurve
```

Requirements:

* save
* restore
* rename
* delete
* reset
* schema migration
* invalid-preset recovery

Renderer must remain blind to persistence details.

---

# 15. WAVE 5 GATE

Visualizer Studio should feel like a real creative workbench:

* styles visibly different
* controls feel intentional
* presets persist
* reset works
* no accidental mic permission
* no interaction lag
* no per-frame allocation storm

---

# 16. WAVE 6 — PROFESSIONAL EQUALIZER

## 6.1 DSP correctness

Audit:

* preset-to-device frequency mapping
* 5/6/10-band layouts
* persistence
* Room schema
* migrations
* round-trip correctness

Frequency mapping must be mathematically appropriate to actual device band frequencies.

Do not silently assume the UI reference frequencies equal engine frequencies.

---

## 6.2 Premium EQ

Build back only verified, useful richness:

* all bands
* user presets
* save/delete
* reverb
* channel balance
* preamp
* live spectrum overlay
* artwork-tinted curve
* proper labels
* reset
* accessibility semantics

Where a platform audio effect is unavailable:

**show honest unsupported state**

Never present fake functionality.

---

## 6.3 Preamp/headroom

Audit clipping/headroom behavior.

A professional EQ should not merely expose louder gain and accidentally encourage clipping.

Use proper terminology and honest UI feedback.

---

# 17. WAVE 7 — VISUAL SYSTEM UNIFICATION

## 7.1 Background language

Do not simply paint every screen the same color.

Use:

`neutral dark foundation + controlled aurora atmosphere + screen-specific depth`

Now Playing may be the richest.

Settings should be calmer.

Library should prioritize readability.

This preserves the brand without turning every screen into the same canvas.

---

## 7.2 Typography

Do NOT mass-apply Baloo 2 to every text element.

Use:

* Baloo 2 / brand display treatment for RIKO identity and selected expressive headings
* professional readable typography for dense UI/body/content

This will preserve personality without making the entire application look playful.

---

## 7.3 Iconography

Do not blindly convert all `Icons.Filled` → `Icons.Rounded`.

Perform semantic migration:

* navigation
* playback
* favorite
* overflow
* edit
* delete
* settings

Only migrate where the new icon better matches meaning and brand.

---

## 7.4 Token drift

Upgrade the token gate.

Do not rely only on violation count.

Use:

### Primary gate

new violation signatures fail CI.

### Secondary ratchet

total violation count may not increase.

Track at minimum:

* raw colors
* raw dimensions
* raw typography sizes
* raw shapes
* ad-hoc motion constants

Create/maintain:

`docs/TOKEN_DRIFT_ALLOWLIST.md`

---

# 18. WAVE 8 — PLAYBACK RELIABILITY / PROFESSIONAL BEHAVIOR

This wave is added because visual polish alone does not make a professional music player.

Audit:

* process death recovery
* queue restoration
* current track restoration
* playback position restoration
* audio focus
* audio becoming noisy
* headset/media-button interactions
* notification/media session behavior
* background service lifecycle
* seek failures
* missing/deleted media files
* corrupted media recovery
* screen off/on
* app background/foreground
* configuration/process recreation
* sleep timer
* repeated next/previous operations
* queue exhaustion

Verify that one playback engine remains canonical.

No second player.

No parallel player state authority.

---

# 19. WAVE 9 — SCREEN COMPLETION + ACCESSIBILITY

## 9.1 Onboarding

Professional brand moment.

No fake login language.

No fake streaming language.

No misleading feature promises.

---

## 9.2 Text system

Extract all user-visible strings.

Use:

* string resources
* plurals
* consistent terminology
* consistent spelling

---

## 9.3 Accessibility

Audit:

* TalkBack
* semantic labels
* switch labels
* slider values
* custom actions
* 48dp targets
* state announcements
* reduced motion
* contrast

---

# 20. WAVE 10 — MEASUREMENT + FINAL HARDENING

Repeat the exact Wave-0 scenarios.

Create:

`docs/performance/PERF_BEFORE_AFTER.md`

Include:

* TTID
* TTFD
* fling metrics
* queue metrics
* EQ metrics
* NP transition metrics
* memory observations
* battery/thermal observations
* device/API matrix

Compare:

`Before → Wave 1 → Wave 4 → Final`

Never replace a poor baseline with a new test that is easier to pass.

---

# 21. FINAL DEVICE MATRIX

Minimum target:

### Device A

API 35+ / high-refresh display

### Device B

API 33+

### Device C

API 26–29 fallback path where practical

The goal is not three identical experiences.

The goal is:

`highest available quality per device`

with the same interaction contract.

---

# 22. FINAL VISUAL/FEEL GATE

Do a human-only pass.

No code inspection.

Use RIKO exactly like a normal user:

1. launch
2. browse
3. fling
4. search
5. play
6. expand Now Playing
7. scrub
8. switch queue
9. reorder
10. open EQ
11. drag bands
12. open Visualizer Studio
13. switch styles
14. save preset
15. return to library
16. background app
17. resume playback
18. repeat for 30 minutes

The question is:

**“Does anything feel cheap, delayed, artificial, inconsistent, or unfinished?”**

Every “yes” becomes a final fix candidate.

---

# 23. CLEANUP RULE — NEVER REPEAT THE OLD MISTAKE

Before deleting anything:

1. search references
2. check navigation
3. check DI
4. check reflection
5. check resources
6. check build variants
7. check tests
8. check runtime-only paths
9. check hidden/public APIs

Create:

`docs/cleanup/DISPOSITION_REGISTER.md`

Every deleted item gets:

`dead → reason → evidence → commit`

Every retained item gets:

`live → owner → canonical role`

No aggressive cleanup that can silently remove working premium behavior.

---

# 24. EXECUTION ORDER

| #  | Slice   | Main goal                                |
| -- | ------- | ---------------------------------------- |
| 0  | Wave 0  | Baseline evidence                        |
| 1  | 1.1     | PlayerState recomposition                |
| 2  | 1.2     | Artwork pipeline                         |
| 3  | 1.3–1.4 | Row + Lazy performance                   |
| 4  | 1.5–1.6 | Baseline profile + root invalidation     |
| 5  | 2.1     | EQ gesture root fix                      |
| 6  | 2.2     | Queue reorder                            |
| 7  | 2.3     | Canonical gesture system                 |
| 8  | 3.1     | LibraryRepository                        |
| 9  | 3.2     | Critical startup readiness               |
| 10 | 3.3     | Splash + TTFD correctness                |
| 11 | 4.1     | Musical reactivity                       |
| 12 | 4.2     | AuroraFlow                               |
| 13 | 4.3     | Plasma timeline                          |
| 14 | 5.1     | Template/source audit + styles           |
| 15 | 5.2     | Studio controls + presets                |
| 16 | 6.1     | EQ correctness                           |
| 17 | 6.2     | Premium EQ                               |
| 18 | 7       | Visual unification                       |
| 19 | 8       | Playback reliability                     |
| 20 | 9       | Accessibility + text + screen completion |
| 21 | 10      | Final measurement                        |
| 22 | Final   | Human feel gate + release hardening      |

---

# 25. DEPENDENCY RULES

* Wave 0 before all performance claims.
* 1.2 before artwork-related cleanup.
* 2.3 before plasma seek integration.
* 4.1 before AuroraFlow.
* AuroraFlow before shared advanced visualization vocabulary.
* 5.1 source/license inventory before external template code integration.
* 6.1 before premium EQ persistence UI.
* Wave 1 performance gate before claiming smoothness.
* Wave 8 reliability before final release sign-off.
* Wave 10 measurement before “professional/production-ready” claim.

---

# 26. COMPOSE / DEPENDENCY POLICY

Do not change framework versions in the middle of performance refactoring unless required to fix a verified blocker.

Complete the first campaign on the known stack.

After final measurements, create a separate dependency-modernization checkpoint.

Current Android guidance recommends Compose 1.8+ for newer performance/stability improvements, so the campaign should include a **post-campaign dependency upgrade experiment**, not silently freeze the app forever on the current BOM. The upgrade must be benchmarked before adoption.

---

# 27. DEFINITION OF DONE

A task is NOT done because:

* code compiles
* tests pass
* CI is green
* a screen looks nicer

A task is done when:

`Implemented`
+
`Root cause addressed`
+
`User path reachable`
+
`Regression checked`
+
`Performance/behavior measured where relevant`
+
`Documentation updated`
+
`Correct commit made`

---

# 28. FINAL PRODUCT STANDARD

RIKO should feel like:

**fast before beautiful**

**beautiful without being heavy**

**animated without being noisy**

**premium without fake complexity**

**smart without pretending**

**powerful without friction**

The app should feel alive.

But the life must come from:

`music + motion + artwork + interaction`

—not from random effects.

---

# 29. AGENT SELF-THINKING RULE

Before every wave, independently answer:

1. What is actually wrong?
2. Why is it happening?
3. What existing system already owns this responsibility?
4. What is the smallest durable architectural fix?
5. What could this change break?
6. How will I prove the fix worked?
7. Is there a better approach than the user's original proposal?

If the answer to #7 is yes, tell the product director before implementing a worse solution.

Do not optimize for “more code”.

Optimize for:

**less friction + more quality + stronger architecture + measurable improvement.**

---

# 30. SESSION HANDOFF

At the end of every session:

* update `PROJECT_STATUS.md`
* update `AGENTS.md` where the new rule/state belongs
* record:

  * completed work
  * current state
  * known failures
  * remaining work
  * exact next step
  * verification status
* commit
* push approved branch
* STOP

When the product director sends:

**`CONTINUE-W`**

resume from the exact recorded stopping point using the existing context, rules, current branch state and unresolved gate.

Do not restart the campaign.
Do not redo completed work unnecessarily.
Do not assume missing facts.
Re-read the handoff/status first, then continue from the first unfinished verified step.

---

# FINAL DIRECTIVE

**Do not merely make RIKO bigger.**

Make it:

**faster, smoother, smarter, cleaner, more musical, more coherent, more reliable, and more premium.**

The goal is not a large codebase.

The goal is a music player that, in the user's hand, feels like a finished product.

**KHELA HOBE.**
