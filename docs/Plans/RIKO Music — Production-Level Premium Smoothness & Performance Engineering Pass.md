# RIKO Music — Production-Level Premium Smoothness & Performance Engineering Pass

## ROLE

Act as a **senior Android performance engineer + Jetpack Compose performance specialist + UI motion engineer + Media3/music-player architect**.

You are working on a production-level Android music player called **RIKO Music**.

RIKO is no longer a prototype. The application is already close to production quality and its visual identity is intentionally premium, modern, highly visual, glass/translucent, artwork-driven, animated, and polished.

Your responsibility is to perform a **deep performance and smoothness engineering pass** on the existing RIKO codebase.

The objective is NOT merely to remove obvious lag.

The real objective is:

> Make RIKO feel **instant, fluid, stable, premium, responsive, and flagship-level** while preserving its existing visual identity and functionality.

---

# 1. CORE PROBLEM

RIKO does not necessarily exhibit catastrophic lag.

The problem is much more subtle:

- Buttons work.
- Screens eventually appear.
- Navigation works.
- Scrolling generally works.
- Animations exist.
- The app is technically usable.

However, the UI does NOT consistently feel:

- instant
- crisp
- effortless
- fluid
- high-refresh-rate
- flagship-quality
- “expensive”
- physically smooth

When the user taps a button, the action and animation happen together, but both can feel slightly delayed/heavy.

The user describes the feeling approximately as:

> “Action + animation start together, but it feels slightly late/heavy, like frames are being skipped.”

This is important.

Do NOT immediately assume that the application simply has “low FPS”.

The real problem may involve a combination of:

- frame pacing
- missed frame deadlines
- main-thread contention
- excessive recomposition
- unstable Compose models
- excessive rendering work
- continuous state emissions
- over-invalidated composition
- expensive draw operations
- image decoding
- blur rendering
- animation overlap
- navigation rendering
- unnecessary background work
- poor state granularity
- unnecessary work during scroll
- excessive work occurring outside the visible UI region

The app must therefore be optimized at the **system level**, not by blindly disabling visual features.

---

# 2. ABSOLUTE DESIGN PRINCIPLE

## DO NOT DESTROY RIKO'S VISUAL IDENTITY TO GAIN PERFORMANCE

The following are NOT acceptable as lazy “optimizations”:

- Remove all blur.
- Remove the glass UI.
- Remove artwork ambient backgrounds.
- Disable the visualizer.
- Remove animations globally.
- Replace premium UI with plain Material UI.
- Remove gradients.
- Remove translucency.
- Remove shadows everywhere.
- Remove artwork.
- Make the interface visually primitive.
- Reduce everything to static layouts simply to increase benchmark numbers.

Performance improvements must come from **engineering**, not visual surrender.

RIKO should still look like RIKO after optimization.

---

# 3. TARGET USER EXPERIENCE

After the optimization pass, the following interactions should feel substantially more immediate and fluid.

## Interaction targets

The following areas are ALL currently affected:

- Button press
- Bottom tab change
- Screen-to-screen navigation
- Horizontal swipe
- Vertical scroll
- Playlist/library scroll
- Mini-player expand/collapse
- Search interaction
- Keyboard opening
- Artwork change
- Now Playing screen
- Visualizer
- Bottom sheet/dialog
- App startup

Do not optimize only one screen.

The goal is to identify **global bottlenecks** that affect many screens simultaneously.

---

# 4. CURRENT ARCHITECTURE

Current high-level architecture:

```text
MainActivity
    ↓
RikoMusicTheme
    ↓
ProvideVisualOs
    ↓
RikoNavGraph
    ↓
RikoAppShell
        ↓
Bottom navigation / dock
        ↓
Mini player
        ↓
Destinations
```

The shell is mostly presentational.

Navigation uses:

```text
rememberNavController()
RikoNavGraph
RikoAppShell
```

Tab destinations are preserved through navigation state restoration.

Current navigation logic:

```kotlin
navController.navigate(destination.route) {
    launchSingleTop = true
    restoreState = true
    popUpTo(RikoDestination.AllSongs.route) {
        saveState = true
    }
}
```

This is already structurally reasonable and should NOT be replaced unnecessarily.

---

# 5. IMPORTANT EXISTING EVIDENCE

The following observations have already been made from the codebase.

Treat these as **real investigation evidence**, not speculative guesses.

You must verify them in code before changing anything, but they are high-priority investigation targets.

---

# 6. CRITICAL BOTTLENECK #1 — MONOLITHIC PLAYER STATE

## Current implementation

`PlayerManager.kt:174-190`

There is a:

```kotlin
while (true) {
    update()
    delay(250)
}
```

style position ticker.

Playback position is stored inside a large `PlayerState`.

The important problem is:

> Playback position is not isolated into its own lightweight observable state.

Instead, every ~250 ms the larger `PlayerState` object gets copied/re-emitted.

This PlayerState contains much more than playback position, including queue-related information.

---

# 7. GLOBAL INVALIDATION PROBLEM

Current observations:

### MainActivity

`MainActivity.kt:136`

The Activity root collects `PlayerState`.

This is problematic because MainActivity only needs a very small subset of the information, such as the current song.

However, the entire PlayerState is being observed.

That means a playback position tick can invalidate a much larger composition scope than necessary.

---

# 8. NAV GRAPH INVALIDATION PROBLEM

`RikoNavGraph.kt:99`

The NavGraph/root layer also collects the player state.

This means the root navigation/shell composition can be invalidated approximately 4 times per second simply because playback position changes.

This is especially problematic because the root contains:

- navigation
- shell wiring
- bottom navigation
- mini-player
- sheets
- other global UI infrastructure

A 250 ms position ticker should NOT invalidate the entire application shell.

---

# 9. MULTIPLE SCREEN INVALIDATIONS

Several screens also observe the full PlayerState.

Known examples include approximately:

- Home
- AllSongs
- MyMusic
- Favorites
- CollectionDetail
- PlaylistDetail
- Search
- NowPlaying
- QueueSheet
- SongInfoSheet
- additional player-related components

Many of these screens do not require live playback position.

Yet they can currently be invalidated by the same broad state.

---

# 10. REQUIRED FIX — GRANULAR PLAYER STATE

Do NOT simply reduce the 250 ms ticker frequency.

That would treat the symptom rather than the architecture.

Instead, architect playback state into smaller independently observable state streams.

Example conceptual separation:

```text
CurrentSong
PlaybackState
IsPlaying
PlaybackPosition
PlaybackDuration
BufferedPosition
Queue
ShuffleMode
RepeatMode
PlaybackSpeed
AudioEffectsState
VisualizerState
```

Only components that need a particular piece of state should observe that piece.

For example:

```text
MiniPlayer
    → currentSong
    → isPlaying
    → lightweight progress if actually necessary

BottomNavigation
    → should observe almost nothing related to playback position

AllSongs
    → songs only
    → favorite state only if needed

NowPlaying
    → currentSong
    → playback position
    → duration
    → playback state
    → visualizer state

Search
    → search state
    → library data required by search
```

Do NOT push high-frequency playback state into the entire tree.

---

# 11. IMPORTANT RULE: FIELD EXTRACTION

Do not do this:

```kotlin
val playerState by viewModel.playerState.collectAsState()
```

everywhere.

Then:

```kotlin
playerState.currentSong
```

or:

```kotlin
playerState.isPlaying
```

if the entire object is being invalidated by unrelated fields.

Prefer narrowly scoped flows/state.

Conceptually:

```kotlin
val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
```

and:

```kotlin
val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
```

and only collect progress where needed.

Where appropriate, use:

- `distinctUntilChanged()`
- derived flows
- `map`
- `stateIn`
- lifecycle-aware collection
- state hoisting
- smaller immutable UI state objects

Do not blindly add `distinctUntilChanged()` everywhere. The architecture should become granular first.

---

# 12. CRITICAL BOTTLENECK #2 — UNSTABLE COMPOSE MODELS

Current observations:

`Song` and `PlayerState` contain types such as:

- Uri
- List
- other potentially unstable references

There is no reliable `@Immutable` / `@Stable` strategy currently applied.

As a result, Compose may treat parts of the UI tree as unstable, reducing skipping opportunities.

This is especially important for list rendering.

---

# 13. REQUIRED COMPOSE STABILITY AUDIT

Perform an actual Compose stability investigation.

Do NOT blindly annotate everything with:

```kotlin
@Stable
```

or:

```kotlin
@Immutable
```

without proving that the contract is true.

Incorrect stability annotations can create subtle correctness bugs.

Instead:

1. Identify UI models crossing composable boundaries.
2. Identify immutable vs mutable fields.
3. Use immutable collection types where appropriate.
4. Create dedicated UI models if domain models are unsuitable.
5. Prefer persistent/immutable collections where beneficial.
6. Verify Compose compiler stability where tooling is available.
7. Confirm that recomposition skipping actually improves after the changes.

Pay particular attention to:

- `Song`
- `PlayerState`
- list item models
- mini-player models
- navigation state
- artwork models
- player-related UI models

---

# 14. CRITICAL BOTTLENECK #3 — SONG LIST RENDERING

Main list:

`AllSongsScreen.kt:134-147`

Uses:

```kotlin
LazyColumn
```

and already has:

```kotlin
key = song.id
```

This is good and should remain.

However:

```text
contentType
```

is apparently not defined anywhere in the relevant lists.

Evaluate whether adding meaningful `contentType` improves reuse/skipping.

Do not add it mechanically if all items truly have identical layout types.

---

# 15. SONG LIST ITEM CURRENT COST

`SongListItem.kt`

A row contains approximately:

- GlassCard wrapper
- press-scale `graphicsLayer`
- 52dp artwork
- 2 IconButton
- 2 Text elements
- glass-related drawing

The current GlassSurface itself performs several draw operations.

This makes list scrolling significantly more expensive than a normal Material list.

---

# 16. CRITICAL IMAGE BUG — DUPLICATE ARTWORK REQUEST

Inside each SongListItem:

There is an existing pattern where:

```text
rememberAsyncImagePainter
```

is used,

AND another:

```text
AsyncImage
```

uses the same URI.

Even if Coil memory caching prevents duplicate actual decoding in many cases, this is still unnecessary request/loading/painter machinery.

Do not rely on cache to justify duplicated composition work.

Refactor each row to use a single efficient image path.

---

# 17. CRITICAL IMAGE BUG — FULL-RES DECODE FOR SMALL THUMBNAILS

Current Coil configuration:

`Coil 2.7`

App-wide setup currently enables:

- crossfade
- memory cache
- disk cache

But there is no robust size configuration.

MediaStore artwork URI:

```text
content://media/external/audio/albumart/{albumId}
```

may resolve to large artwork, often around:

```text
500–1000+ px
```

Yet a list row may display only:

```text
52dp
```

Therefore a tiny UI element can cause decoding of a disproportionately large image.

This can create:

- memory pressure
- decoding overhead
- upload cost
- cache pressure
- scroll hitching
- GC pressure

---

# 18. REQUIRED IMAGE PIPELINE

Introduce proper image size tiers.

For example:

```text
Thumbnail
Small artwork
Medium artwork
Hero artwork
Background artwork
```

The exact dimensions must be determined from actual UI requirements and device density.

Use Coil `ImageRequest` sizing intentionally.

Conceptually:

```kotlin
ImageRequest.Builder(context)
    .data(uri)
    .size(targetWidthPx, targetHeightPx)
    .memoryCacheKey(...)
    .diskCacheKey(...)
    ...
```

Do not blindly hardcode one universal size.

Ensure:

- list thumbnails decode near display size
- recently-added cards decode near display size
- Now Playing hero artwork gets an appropriate resolution
- background artwork is not decoded at unnecessarily extreme resolution
- background artwork has a separate strategy from thumbnail artwork

---

# 19. CROSSFADE AUDIT

Coil crossfade is currently enabled globally.

Do not assume global crossfade is always desirable.

For scrolling lists, repeated crossfades can create visual and rendering overhead and can make cached images feel less immediate.

Evaluate whether crossfade should be:

- disabled for cached list thumbnails
- limited to selected hero/content transitions
- used only where it contributes positively to UX

The final decision must be based on measured behavior.

---

# 20. CRITICAL BOTTLENECK #4 — GLASS ENGINE

`GlassSurface.kt`

Each GlassSurface can perform roughly:

- multi-stop gradient
- animated tint
- specular oval
- edge-refraction stroke
- grounding shadow oval
- optional shadow
- colorGlow

This means a visually beautiful glass component can become a very expensive primitive.

The problem becomes severe when it is repeated for every item in a scrolling list.

---

# 21. IMPORTANT EXISTING EVIDENCE

The list intentionally does not use blur because of an earlier issue documented around approximately:

```text
~15fps scroll
```

This is valuable evidence.

Do NOT reintroduce per-row blur.

Keep the principle:

> Scrolling list items should use a cheaper glass rendering path than large hero surfaces.

---

# 22. REQUIRED GLASS OPTIMIZATION STRATEGY

Do NOT remove glass.

Instead build rendering tiers.

Conceptually:

### Tier A — Hero glass

Used for:

- Now Playing
- prominent large surfaces
- static/slow-moving containers

Can afford more layers.

### Tier B — Interactive glass

Used for:

- buttons
- dock
- controls
- medium surfaces

Moderate draw complexity.

### Tier C — List glass

Used for:

- SongListItem
- playlist rows
- collection rows

Must be highly optimized.

Potential optimizations may include:

- fewer draw passes
- avoiding unnecessary shadow
- static rather than animated tint
- shared/background rendering
- reduced glow complexity
- avoiding expensive clipping where not necessary
- batching where possible
- precomputed colors
- avoiding per-item animation except during interaction

But preserve visual appearance as closely as possible.

---

# 23. CRITICAL BOTTLENECK #5 — INFINITE ANIMATED BACKGROUNDS

`GlassBackground.kt:105-114`

Uses:

```text
rememberInfiniteTransition
```

with:

- ~14s drift
- pulse
- 3 radial blobs

This creates continuous animation even when the user is not interacting.

Similar effects exist in:

`NowPlayingBackground.kt`

and:

`MiniPlayerBackground.kt`

The mini-player background animation can continue across screens.

---

# 24. CONTINUOUS ANIMATION RULE

Infinite animations are not automatically bad.

However:

> Every continuously animated pixel has a performance cost.

You must audit:

- whether the animation is visible
- whether it needs to run continuously
- whether it can be paused
- whether it can use cheaper properties
- whether the animation actually changes the rendered output enough to justify itself

Potential strategy:

```text
Foreground + visible:
    normal animation

Background / app not visible:
    pause

Screen not visible:
    pause

Low-power conditions:
    reduce/disable optional animation

User actively interacting:
    allow premium motion
```

Do not disable animation globally.

---

# 25. CRITICAL BOTTLENECK #6 — NOW PLAYING

Current Now Playing is the heaviest visual area.

`NowPlayingBackground.kt`

contains:

### Full-screen artwork

Current artwork is loaded without an explicit size constraint.

### 65dp blur

Approximate:

```text
65dp blur
```

on large artwork.

### Artwork scale

Approximately:

```text
1.4x
```

with:

```text
alpha = 0.5
```

### Multiple Canvas layers

Approximately 5 layers.

### Multiple animated color transitions

Approximately 5 `animateColorAsState` operations.

### Infinite background transitions

At least 2 continuous animation systems.

---

# 26. NOW PLAYING OPTIMIZATION RULE

Now Playing is allowed to remain visually rich.

But it should not continuously recompute every expensive component at maximum frequency.

Separate:

```text
static
slow-changing
interaction-driven
frame-driven
```

rendering.

For example:

### Static-ish

- structural gradients
- decorative shapes
- fixed overlays

Should not redraw due to playback position.

### Slow-changing

- ambient colors
- background atmosphere
- artwork-derived colors

Can update only when artwork changes or on controlled intervals.

### Interaction-driven

- gestures
- seek
- button animations

Only animate during interaction.

### Frame-driven

- waveform
- visualizer
- playback progress

Only these should operate at high frequency.

---

# 27. CRITICAL BOTTLENECK #7 — WAVEFORM TIMELINE

`WaveformTimeline.kt:152-192`

Current implementation has a:

```text
withFrameNanos
```

loop.

The loop updates approximately 8 mutable states every frame.

It performs approximately:

```text
two full canvas redraw paths
```

with one blurred.

The loop may continue while paused, as long as the foreground gate allows it.

This is wasteful.

---

# 28. REQUIRED WAVEFORM STRATEGY

The waveform should be treated as a rendering system.

Do NOT allow unrelated player updates to trigger waveform recomposition.

Separate:

```text
waveform geometry
waveform progress
visualizer data
animation state
```

as independently as possible.

Precompute static waveform geometry.

Do NOT reconstruct static waveform geometry every frame.

Only the tiny dynamic portion should update continuously.

When paused:

- no unnecessary frame loop
- no unnecessary animation chasing
- no unnecessary redraw

When screen leaves foreground:

- stop expensive rendering

When visualizer is not actually visible:

- stop or substantially reduce visualizer work.

---

# 29. CRITICAL BOTTLENECK #8 — WAVE ENGINE

Current evidence indicates:

```text
VisualizerManager capture rate ≈ max / 2
≈ 41–50 Hz
```

Data goes into:

```text
FloatArray StateFlow
```

then:

```text
ExperienceDataHub.audioState
```

then:

```text
WaveEngine.state
```

then:

```text
PlayerViewModel
```

which re-exposes the state.

The WaveEngine itself runs an approximate:

```text
5-solver audio pipeline
```

with a target near:

```text
60 fps
```

This creates multiple layers of high-frequency data propagation.

---

# 30. REQUIRED VISUALIZER ARCHITECTURE

Do not blindly render every incoming audio sample.

Instead separate:

```text
Audio capture frequency
Processing frequency
UI state frequency
Display frame frequency
```

These do NOT need to be identical.

For example:

```text
Audio capture:
    high frequency

Audio processing:
    controlled

UI-facing visualizer state:
    coalesced / latest-value-wins

Rendering:
    display-driven
```

A renderer typically does not need every intermediate audio callback.

Prefer:

```text
latest value
```

over processing/rendering stale intermediate values.

Avoid queueing dozens of visualizer states that the UI cannot possibly display.

---

# 31. IMPORTANT: KEEP VISUALIZER QUALITY

Do not disable the visualizer merely to improve benchmark numbers.

Maintain:

- responsiveness
- musical movement
- smoothness
- visual character

while avoiding unnecessary work.

The best result is:

> Fewer unnecessary computations, but the visible visualizer remains visually smooth.

---

# 32. CRITICAL BOTTLENECK #9 — ROOT-SCOPED WAVE OBSERVATION

`PlayerViewModel.init`

currently starts wave observation that can remain active for the lifetime of the root-scoped ViewModel.

There is a manual pause mechanism around:

`RikoNavGraph.kt:107-120`

and foreground gating.

This architecture should be reviewed.

The preferred design is:

```text
Visualizer work exists when needed
Visualizer work pauses when not needed
Visualizer work resumes cleanly
```

Do not let the application continuously process audio visualization data when:

- Now Playing is not visible
- the visualizer is not visible
- app is backgrounded
- screen is off
- user is not seeing the relevant effect

---

# 33. CRITICAL BOTTLENECK #10 — NAVIGATION ANIMATION

Current navigation:

`RikoNavGraph.kt:262-265`

approximately:

```text
enter:
    fadeIn(200ms)
    scaleIn(0.96f)

exit:
    fadeOut(100ms)
    scaleOut(...)
```

The problem is not necessarily the exact duration.

The key issue is:

> During transition, two screens can be rendered together.

And the root shell/background continues to animate.

Therefore navigation can temporarily combine:

- outgoing destination rendering
- incoming destination rendering
- shell rendering
- animated background
- glass rendering
- artwork/image work
- other screen-specific animation

This can create transition-time frame pressure.

---

# 34. NAVIGATION MOTION GOAL

Navigation should feel:

- immediate
- short
- crisp
- controlled
- premium

Not:

- slow
- floaty
- delayed
- heavy
- over-produced

Do not simply reduce all durations to 0.

Instead experiment with a smaller, coherent motion system.

Potential target range may be roughly:

```text
~150–220ms
```

for ordinary navigation, but determine the final value through perception and profiling.

---

# 35. DUPLICATE MOTION SYSTEM

There are apparently duplicate motion definitions:

```text
riko/theme/RikoMotion.kt
theme/RikoMotion.kt
SpringMotion.kt
```

with overlapping responsibilities.

Audit and consolidate the motion system.

Do not create arbitrary third/fourth animation constants.

There should be a single coherent motion language.

---

# 36. PROGRESS-DRIVEN ANIMATION BUG

`OrganicTimeline.kt:103-107`

Playback progress appears to be animated through a spring that continuously chases a ticking progress target.

This can create:

```text
new target
    ↓
spring starts chasing
    ↓
new target arrives
    ↓
spring target changes
    ↓
spring never fully settles
```

This becomes perpetual animation driven by another continuous animation.

That is usually unnecessary.

For actual playback progress, prefer a rendering model where:

```text
true playback position
```

is interpolated visually without launching an independent spring chase every 250 ms.

The displayed progress may use frame-time interpolation between known player positions.

The final model must remain accurate for seeking.

---

# 37. IDEAL PLAYBACK PROGRESS MODEL

Instead of:

```text
250ms state update
+
spring animation
```

consider:

```text
low-frequency authoritative playback state
+
frame-time interpolation while playing
```

Conceptually:

```text
authoritative player position:
    periodically synchronized

UI:
    interpolates locally between synchronization points
```

This can produce a smooth progress bar without forcing the entire Compose tree to update every 250 ms.

Be careful with:

- pause
- seek
- buffering
- speed changes
- playback completion
- track changes
- scrubbing
- background/foreground transitions

---

# 38. ROOM FLOW ARCHITECTURE

Current repository pattern:

```text
cold Room Flow
→ ViewModel
→ stateIn(WhileSubscribed(5000))
```

This pattern is generally reasonable.

Do not replace it purely for theoretical optimization.

However, evaluate cases where a tiny database change causes an entire:

```text
List<Song>
```

to be re-emitted.

For example:

```text
favorite-table modification
```

may trigger broader list updates than necessary.

---

# 39. REQUIRED ROOM/UI OPTIMIZATION

Investigate:

- SQL query scope
- Flow granularity
- list identity
- row-level updates
- favorite status propagation
- playlist changes
- album/artist changes

Prefer:

```text
only the affected data changes
```

rather than:

```text
entire huge list invalidates
```

every time a small property changes.

Do not over-engineer this if actual usage does not justify it.

Measure first.

---

# 40. SCROLL PERFORMANCE

Main lists use:

```text
LazyColumn
LazyRow
```

This is good.

But scrolling still lacks premium fluidity.

The main reason is likely not the LazyColumn itself.

The list contains expensive children.

Investigate:

- item composition cost
- item draw cost
- image decode cost
- glass rendering
- animated modifiers
- shadow
- glow
- state observation
- unstable models
- per-row image request machinery
- per-row animation state

---

# 41. SCROLL-SPECIFIC RULE

During active scrolling:

Avoid:

- unnecessary recomposition
- heavy blur
- expensive image decoding
- continuous per-row animated gradients
- animated color changes that do not contribute to the interaction
- duplicate image painters
- per-frame allocations
- repeated expensive calculations

Premium scrolling should look visually alive without every row behaving like a mini graphics engine.

---

# 42. PRESS SCALE

Current rows use press-scale:

```text
graphicsLayer
```

This is acceptable.

But verify that press-state changes only the pressed item rather than broad parent state.

Avoid:

```text
list-wide recomposition
```

for:

```text
one row pressed
```

---

# 43. APP STARTUP

Startup is also perceived as part of “premium smoothness”.

Audit:

- MainActivity startup work
- theme initialization
- Hilt graph creation
- Media3 initialization
- Room initialization
- Coil initialization
- artwork work
- player state collection
- visualizer initialization
- background animation startup
- navigation setup
- initial screen data loading

Do not move everything to async blindly.

Some startup work is essential.

The goal is:

```text
first meaningful frame quickly
```

followed by:

```text
progressive initialization of non-critical systems
```

where architecturally safe.

---

# 44. FIRST FRAME VS FULL INITIALIZATION

Separate startup operations into:

### Critical for first render

- theme
- navigation shell
- first screen skeleton/content
- minimum player state required to display UI

### Can initialize after first render

Potentially:

- visualizer engine
- nonessential ambient effects
- some caches
- secondary database work
- non-visible artwork
- low-priority services

Do NOT delay something if that creates a visible pop-in or incorrect UI.

---

# 45. SEARCH AND KEYBOARD

Search/open keyboard is part of the user's “instant response” perception.

Audit:

- search field recomposition
- keyboard-triggered layout changes
- focus handling
- search query processing
- filtering
- database queries
- animated container changes
- background/blur work during keyboard resize

Important:

Typing must remain responsive even while filtering a large music library.

Avoid performing heavy filtering directly on the main thread.

---

# 46. BOTTOM SHEETS AND DIALOGS

Audit:

- initial composition
- blurred backgrounds
- scrims
- sheet animation
- nested content
- drag handling
- expensive artwork
- list rendering inside sheets

Bottom sheet open should feel immediately responsive.

Avoid doing expensive database/image work exactly at the moment the sheet animation begins.

Preload or compute where appropriate.

---

# 47. MINI PLAYER

The mini-player is part of the global shell.

Current background uses an infinite animation.

Because it exists globally, its cost is multiplied by:

```text
every screen
```

Therefore:

> Global animation must be exceptionally cheap.

Audit:

- background animation
- artwork
- progress
- player controls
- state collection
- animation
- navigation events

The mini-player should not be receiving high-frequency state that it does not visually need.

---

# 48. IMPORTANT STATE-OWNERSHIP PRINCIPLE

Every piece of state must have a reason for being observed at its current level.

Ask for every state:

> “What is the lowest composable/component level that actually needs this?”

Move the state there.

Examples:

```text
playbackPosition
→ NowPlaying timeline only

visualizerState
→ Visualizer only

currentSong
→ MiniPlayer + NowPlaying

queue
→ Queue screen only

search query
→ Search screen only

favorite status
→ affected list rows / relevant VM state
```

Avoid globalizing convenience state.

---

# 49. COMPOSITION LOCALITY

A central goal is:

> Keep invalidation local.

Bad:

```text
position change
→ NavGraph recomposes
→ Shell recomposes
→ all destinations potentially re-evaluate
```

Better:

```text
position change
→ timeline progress state updates
→ only progress rendering work changes
```

This principle should guide the entire performance pass.

---

# 50. DRAWING LOCALITY

Similarly:

> Only the pixels that need to change should be expensive.

For example:

If playback progress moves:

Do NOT redraw:

- full artwork background
- glass blobs
- navigation
- song metadata
- unrelated controls

Only the required progress/waveform layers should update.

---

# 51. ANIMATION LOCALITY

Animation state must also be localized.

A button animation should not invalidate its entire screen.

A waveform animation should not invalidate the mini-player.

A mini-player background animation should not invalidate navigation.

A navigation animation should not force playback rendering to restart.

---

# 52. ALLOCATION AUDIT

Look for allocations inside:

- composable bodies
- draw lambdas
- frame loops
- animation callbacks
- visualizer callbacks
- scroll callbacks
- waveform calculations
- color transformations

Examples to investigate:

```kotlin
FloatArray(...)
List(...)
map { ... }
filter { ... }
toList()
toMutableList()
Color(...)
Brush...
Path(...)
Paint...
```

inside high-frequency code.

Do not optimize every allocation indiscriminately.

Focus on allocations occurring:

```text
per frame
per audio callback
per list row
per recomposition
per scroll update
```

---

# 53. DRAWING OBJECT REUSE

For Canvas-based visuals, investigate reuse of expensive objects when appropriate.

Examples:

- Path
- Paint
- Brush
- calculated geometry
- arrays
- intermediate buffers

Do not introduce unsafe mutation that breaks Compose rendering.

Use `remember` where the object is logically stable and safe to reuse.

---

# 54. REMEMBER STRATEGY

Audit current use of:

```kotlin
remember
rememberSaveable
derivedStateOf
LaunchedEffect
DisposableEffect
snapshotFlow
```

Do not add `remember` blindly.

Every `remember` should have a reason.

Particularly investigate:

- expensive calculations
- waveform geometry
- color extraction
- artwork metadata
- derived UI state
- animation objects

---

# 55. DERIVED STATE

Where UI derives a value from rapidly changing state, investigate:

```kotlin
derivedStateOf
```

when appropriate.

But do not use it as a universal “performance annotation”.

It is useful when it reduces recomposition due to unchanged derived values.

---

# 56. SIDE-EFFECT AUDIT

Audit all:

```kotlin
LaunchedEffect
DisposableEffect
produceState
snapshotFlow
Flow.collect
CoroutineScope.launch
```

for:

- repeated restarts
- excessive collectors
- root-scoped forever collectors
- collectors tied to the wrong lifecycle
- duplicate work
- unnecessary high-frequency updates

Pay special attention to effects that use:

```kotlin
while(true)
delay(...)
withFrameNanos(...)
```

---

# 57. FOREGROUND/BACKGROUND LIFECYCLE

When the app is not visible:

Stop or reduce expensive visual systems.

Examples:

- visualizer
- waveform render loop
- infinite ambient animation
- unnecessary artwork processing
- visual-only computations

The music player itself must continue functioning.

This distinction is critical:

```text
audio playback
```

and:

```text
visual playback UI
```

are different responsibilities.

Audio must continue.

Visual rendering should be lifecycle-aware.

---

# 58. BACKGROUND AUDIO SAFETY

Do NOT accidentally break:

- Media3
- MediaSession
- foreground service
- notification controls
- playback persistence
- queue
- Bluetooth/headset controls
- lock-screen playback behavior

Performance optimization must not regress audio behavior.

---

# 59. MAIN THREAD AUDIT

Search for main-thread work involving:

- database calls
- MediaStore queries
- artwork decoding
- bitmap processing
- color extraction
- waveform generation
- audio analysis
- expensive filtering
- sorting
- JSON/serialization
- file I/O

Make sure heavy work is appropriately dispatched.

But do not blindly dispatch UI-critical work to background threads if synchronization causes extra latency.

---

# 60. MEDIA3 / PLAYER INTERACTION

Audit whether player callbacks cause excessive Compose updates.

Examples:

- playback state
- media metadata
- position
- duration
- buffering
- repeat
- shuffle
- queue

Do not let every player callback trigger broad UI state regeneration.

Coalesce information where possible.

---

# 61. VISUAL STATE VS DOMAIN STATE

Prefer separating:

```text
Domain player model
```

from:

```text
UI-facing state
```

A domain object containing:

- queue
- current song
- position
- metadata
- playback state
- visualizer data

should not automatically become one monolithic Compose state object.

Create smaller UI-facing projections.

---

# 62. PERFORMANCE TIERS

Consider introducing device-aware performance tiers only where necessary.

For example:

```text
High
Balanced
Power Efficient
```

But do NOT use tiers as a substitute for proper optimization.

The app must be smooth on normal hardware before using adaptive degradation.

Any performance tier must preserve RIKO's design language.

---

# 63. FRAME BUDGET

Performance should be judged by frame deadlines, not only average FPS.

For:

```text
60 Hz
```

one frame is approximately:

```text
16.67 ms
```

For:

```text
90 Hz
```

approximately:

```text
11.11 ms
```

For:

```text
120 Hz
```

approximately:

```text
8.33 ms
```

Therefore a screen can average “60fps” while still feeling bad if frame pacing has spikes.

The goal is:

> Consistent frame delivery.

---

# 64. FRAME PACE > RAW FPS

Look for:

- missed frame deadlines
- long UI thread blocks
- long RenderThread work
- expensive GPU passes
- image decoding spikes
- GC spikes
- animation synchronization stalls

Premium smoothness means:

```text
stable frame timing
```

rather than simply:

```text
high average FPS
```

---

# 65. PROFILE BEFORE MAJOR CHANGES

Before making major architectural changes:

1. Build the project.
2. Run the app on representative hardware.
3. Profile the most problematic interactions.
4. Capture evidence.
5. Identify actual hotspots.
6. Apply targeted fixes.
7. Profile again.

Do not use intuition as the final validation.

---

# 66. REQUIRED PERFORMANCE TEST MATRIX

Test at minimum:

### Test A — Button

Tap repeatedly.

Measure:

- input-to-visual feedback
- frame timing
- recomposition
- main-thread work

### Test B — Bottom Tab

Switch:

```text
Home
→ My Music
→ Favorites
→ Search
→ Collections
```

Observe transition smoothness and frame spikes.

### Test C — Large Library Scroll

Use realistic library size.

Scroll rapidly.

Observe:

- frame time
- image decoding
- recomposition
- GC
- rendering

### Test D — Now Playing

Test:

- artwork
- animation
- waveform
- visualizer
- playback progress

simultaneously.

### Test E — Mini Player

Switch screens while mini-player is visible.

### Test F — Bottom Sheet

Open/close:

- Queue
- Song Info
- Equalizer

### Test G — Search

Open keyboard and type quickly.

### Test H — Startup

Cold start.

Then:

- first frame
- first meaningful content
- player readiness
- background initialization

---

# 67. TEST BOTH DEBUG AND RELEASE

Debug behavior is not enough.

Current debug build is minified, but still verify production-like behavior.

Evaluate:

- release build
- minified release
- baseline profile
- R8 behavior
- startup
- rendering

Do not declare success based only on Android Studio Preview or emulator behavior.

---

# 68. BASELINE PROFILE

Investigate whether RIKO currently has a proper:

```text
Baseline Profile
```

The current evidence suggests one may be missing.

Determine whether implementing one would materially improve:

- startup
- navigation
- first render
- scrolling
- common code paths

If useful, implement it correctly.

Do not add baseline profile machinery merely because it sounds advanced.

---

# 69. FRAME TIMING MONITOR

RIKO already has:

```text
FrameTimingMonitor.kt
```

and:

```text
MiniPlayerPerformanceGate.kt
```

These must be used as part of the optimization process.

Do not ignore existing telemetry.

Review:

- how frame timing is measured
- sampling accuracy
- overhead introduced by the monitor itself
- device-tier logic
- false positives
- whether it can identify actual spikes

---

# 70. IMPORTANT: PERFORMANCE TELEMETRY MUST NOT BECOME THE PROBLEM

Any performance monitor must be lightweight.

Avoid:

- logging every frame in production
- allocating large arrays every frame
- string formatting every frame
- excessive tracing
- noisy Logcat output

Instrumentation should be:

```text
low overhead
```

and ideally debug/profile configurable.

---

# 71. TARGETED OPTIMIZATION ORDER

Work roughly in this order unless profiling proves another order is more important:

## Phase 1 — State invalidation

Fix:

- monolithic PlayerState
- root-level collection
- unnecessary screen collection
- high-frequency state propagation

This is likely one of the biggest global improvements.

## Phase 2 — Compose stability

Fix:

- unstable UI models
- unnecessary list recomposition
- incorrect state ownership

## Phase 3 — Image pipeline

Fix:

- duplicate image request path
- full-resolution thumbnails
- size constraints
- crossfade strategy
- cache usage

## Phase 4 — List rendering

Fix:

- expensive per-row glass
- unnecessary drawing
- row-level state propagation
- image loading
- content types where useful

## Phase 5 — Now Playing rendering

Fix:

- blur
- artwork decoding
- ambient background
- color animation
- waveform
- visualizer

without destroying visual identity.

## Phase 6 — High-frequency systems

Fix:

- waveform loop
- visualizer
- playback progress
- infinite transitions

## Phase 7 — Navigation/motion

Fix:

- duplicate motion definitions
- unnecessary transition workload
- perpetual animation chasing
- overly heavy transitions

## Phase 8 — Startup

Optimize:

- first frame
- initialization sequencing
- baseline profile

## Phase 9 — Verification

Re-profile everything.

---

# 72. WHAT NOT TO DO

Do NOT:

- rewrite the entire architecture unnecessarily
- replace Compose with Views
- remove Media3
- remove Room
- remove Hilt
- remove visual effects globally
- disable animation globally
- remove artwork
- remove visualizer
- remove glass design
- replace LazyColumn with RecyclerView simply because it is familiar
- add random delays to “smooth” animations
- use arbitrary coroutines everywhere
- add `LaunchedEffect` to compensate for poor state architecture
- add `distinctUntilChanged()` everywhere without understanding upstream identity
- annotate everything `@Stable`
- allocate fake placeholder data solely for profiling
- use arbitrary FPS caps without evidence
- make UI less attractive just to pass a benchmark
- optimize based only on one cheap device
- optimize only the Now Playing screen
- stop playback just to improve UI performance
- break notification/media-session behavior

---

# 73. IMPORTANT: PRESERVE FUNCTIONALITY

The following must continue working:

- Music playback
- queue
- playlist
- favorites
- search
- library scanning
- artwork
- MediaSession
- notification
- foreground playback
- seek
- shuffle
- repeat
- Bluetooth/headset controls
- audio effects
- visualizer
- waveform
- mini-player
- navigation
- state restoration

No performance optimization is considered successful if it introduces behavioral regressions.

---

# 74. CHANGE DISCIPLINE

For every significant optimization:

### Before

Explain:

```text
What is expensive?
Why is it expensive?
Which scope is invalidated?
How frequently?
What does the user experience as a result?
```

### Change

Explain:

```text
What architectural/rendering change is being introduced?
```

### After

Verify:

```text
What became cheaper?
What recomposition disappeared?
What frame work disappeared?
What functionality remains intact?
```

Do not make dozens of unrelated changes simultaneously without a way to understand their effects.

---

# 75. CODE QUALITY REQUIREMENT

All changes must remain:

- idiomatic Kotlin
- idiomatic Jetpack Compose
- lifecycle-safe
- coroutine-safe
- thread-safe
- maintainable
- testable
- production-ready

Avoid hacks whose only purpose is to make one profiler trace look better.

---

# 76. CORRECTNESS OVER MICRO-OPTIMIZATION

Do not optimize:

```text
0.1 ms
```

while leaving a major architecture issue untouched.

Prioritize:

```text
high-frequency × large-scope work
```

For example:

```text
4 Hz × entire application tree
```

may matter more than:

```text
one 0.2 ms draw operation
```

Similarly:

```text
50 Hz × expensive visualizer pipeline
```

can matter far more than a one-time calculation.

---

# 77. PERFORMANCE HEURISTIC

A useful mental model:

```text
Cost ≈
frequency
× affected scope
× per-operation cost
```

Therefore prioritize code that is:

- frequent
- broad
- expensive

This is why root-level PlayerState observation is a high-priority target.

---

# 78. PREMIUM FEEL ENGINEERING

Do not interpret “smoothness” as simply:

```text
more animation
```

Premium feel usually comes from:

```text
low input latency
+
stable frames
+
short transitions
+
correct motion
+
predictable response
+
minimal visual hitching
```

A 180 ms crisp transition can feel more premium than a 400 ms elaborate transition.

---

# 79. INPUT RESPONSIVENESS

When a button is touched:

The UI should acknowledge interaction immediately.

The expensive underlying operation should not block that feedback.

Where appropriate:

```text
visual press state
→ immediately visible
→ operation continues asynchronously
```

But do not fake state in a way that creates correctness problems.

---

# 80. MOTION SYSTEM

Create one coherent motion system.

Classify animations as:

```text
Micro interaction
Normal transition
Emphasized transition
Hero transition
Physics/spring interaction
```

Each should have a clear purpose.

Avoid multiple competing animation systems for the same property.

---

# 81. SPRING USE

Springs are good for:

- touch feedback
- drag
- expandable controls
- physically responsive UI

Springs are not automatically ideal for:

- continuously ticking playback progress

Do not use springs to chase rapidly changing targets.

---

# 82. ARTWORK CHANGES

Artwork changes should feel smooth without causing:

- decode spikes
- full-screen layout invalidation
- multiple simultaneous heavyweight image transitions

Potential strategy:

```text
new artwork prepared
→ lightweight transition
→ background processing occurs independently
```

The final implementation must preserve the premium visual transition.

---

# 83. AMBIENT COLOR PROCESSING

Current artwork-derived ambient colors use animated color state.

Audit whether five simultaneous springs/transitions are actually necessary.

Consider:

```text
artwork changes
→ compute target palette
→ one coordinated ambient transition
```

instead of multiple independent animation systems if visually equivalent.

Do not sacrifice the aesthetic.

---

# 84. BLUR STRATEGY

Blur is one of the most expensive visual tools in RIKO.

Use it strategically.

Large static blur:

Potentially acceptable.

Blur inside a:

```text
per-row
per-frame
scrolling
```

context:

Very expensive.

Blurred waveform:

Audit whether:

- it needs blur every frame
- blur can be approximated
- static glow can replace some work
- the blur region can be smaller
- rendering can use cached/precomputed geometry

Do not simply delete the effect.

---

# 85. CANVAS OPTIMIZATION

For custom drawing:

Separate:

```text
static geometry
```

from:

```text
dynamic parameters
```

Static geometry should be computed once where possible.

Dynamic rendering should update only what changes.

Avoid:

```text
recreating full geometry every frame
```

when only progress changes.

---

# 86. SCROLL IMAGE LOADING STRATEGY

During fast scroll:

Images should:

- decode near display size
- use memory cache efficiently
- avoid duplicate request machinery
- avoid giant decode operations
- avoid unnecessary crossfade

The scroll should remain stable even with many songs.

---

# 87. MEMORY / GC

Investigate memory usage during:

- library scroll
- Now Playing artwork changes
- fast navigation
- visualizer operation

Look for:

- large bitmap allocations
- temporary arrays
- duplicate image objects
- excessive collections
- frequent GC

A smooth 60/90/120 Hz UI can still hitch due to memory pressure.

---

# 88. GPU VS CPU

Determine whether the dominant problem in each scenario is:

```text
CPU/composition
```

or:

```text
RenderThread/GPU
```

Examples:

### CPU-heavy

- broad recomposition
- Flow emissions
- filtering
- list calculations
- image decoding
- audio processing

### GPU/render-heavy

- blur
- multiple translucent layers
- shadows
- large Canvas redraw
- repeated gradients
- overdraw

Optimize the right layer.

---

# 89. OVERDRAW AUDIT

Check for layered:

```text
background
+
blurred artwork
+
gradient
+
glass
+
shadow
+
glow
+
overlay
```

especially where large areas overlap.

The goal is not necessarily to eliminate overdraw.

It is to ensure overdraw is intentional and affordable.

---

# 90. GLOBAL BACKGROUND OPTIMIZATION

RIKO has animated backgrounds shared across the UI.

A global background must be cheaper than a screen-local background.

Consider:

- updating slower
- shared state
- reduced animation complexity
- pausing when unnecessary
- limiting redraw regions
- using precomputed gradients

The visual effect should remain visually coherent.

---

# 91. RELEASE BUILD / R8

Verify:

- release performance
- R8 optimizations
- resource shrinking if enabled
- baseline profile
- startup
- method/class initialization cost

Avoid assuming:

```text
debug = release
```

or:

```text
release automatically fixes performance
```

---

# 92. DEVICE TESTING

Test at least on representative devices covering:

```text
60 Hz
90 Hz
120 Hz
```

and at least one lower/mid-range Android device.

Do not tune only for a flagship.

RIKO should remain smooth on realistic hardware.

---

# 93. SUCCESS CRITERIA

The optimization should produce noticeable improvements in:

### Buttons

Tap feels immediate.

### Tabs

Tab switching feels instant.

### Navigation

No visible hitch during transitions.

### Vertical scroll

No micro-stutter.

### Horizontal scroll

Consistent frame pacing.

### Library

Large list remains smooth.

### Mini player

Does not add background UI lag.

### Now Playing

Heavy visuals remain fluid.

### Visualizer

Movement remains smooth but does not waste work.

### Waveform

Playback progress is smooth and accurate.

### Search

Typing remains responsive.

### Bottom sheets

Open/close is immediate.

### Startup

First meaningful frame appears quickly.

---

# 94. BEFORE/AFTER VALIDATION

For each major category capture:

```text
Before:
- frame behavior
- composition behavior
- main-thread behavior
- visual symptoms

After:
- same test
- compare frame behavior
- compare invalidations
- compare memory/CPU/GPU
```

A numerical benchmark is useful, but user-perceived smoothness matters too.

---

# 95. PERFORMANCE REGRESSION PROTECTION

After optimization, establish lightweight rules/documentation such as:

```text
Do not collect high-frequency player position at root scope.
Do not decode full-size artwork for thumbnail rows.
Do not add blur to scrolling rows.
Do not run visualizer rendering when it is not visible.
Do not launch infinite animations without lifecycle consideration.
Do not propagate visualizer state into unrelated shell UI.
Do not duplicate artwork loaders in list rows.
```

These become architectural guardrails for future development.

---

# 96. IMPORTANT: DO NOT STOP AT THE FIRST FIX

The likely root cause hierarchy currently looks approximately like:

### Priority 1
Monolithic PlayerState and broad root-level observation.

### Priority 2
Compose instability / failed skipping.

### Priority 3
Expensive list rows + duplicated image machinery.

### Priority 4
Full-resolution artwork decoding.

### Priority 5
Now Playing's simultaneous:
- blur
- ambient rendering
- animation
- waveform
- visualizer

### Priority 6
Visualizer/waveform high-frequency propagation.

### Priority 7
Perpetual progress spring animation.

### Priority 8
Navigation and duplicate motion systems.

### Priority 9
Startup / baseline profile.

However:

> These priorities are hypotheses backed by existing evidence, not permission to skip profiling.

Verify them.

---

# 97. REQUIRED FINAL REPORT

After implementation, produce a detailed report containing:

## A. Root causes discovered

For each one:

```text
File
Location
Problem
Frequency
Affected scope
Why it caused perceived lag
```

## B. Changes made

For each:

```text
File
What changed
Why
Performance effect
Visual impact
```

## C. State architecture changes

Show:

```text
Before
→
After
```

especially PlayerState.

## D. Rendering changes

Show which effects were:

- preserved
- optimized
- consolidated
- paused
- localized

## E. Image pipeline changes

Show:

```text
thumbnail size
hero size
background strategy
cache strategy
crossfade strategy
```

## F. Visualizer changes

Explain:

```text
capture
processing
state
rendering
lifecycle
```

## G. Motion changes

Show final motion durations / spring strategy.

## H. Verification

Describe:

```text
profiling method
test devices
before/after behavior
remaining bottlenecks
```

---

# 98. FINAL IMPLEMENTATION RULE

Before modifying a file:

1. Read the relevant implementation.
2. Understand its data flow.
3. Identify what triggers it.
4. Identify its rendering scope.
5. Make the smallest architecture-correct change.
6. Build.
7. Test.
8. Profile when relevant.
9. Only then move to the next bottleneck.

Do not blindly perform broad search-and-replace edits across the project.

---

# 99. IF YOU FIND A BETTER ROOT CAUSE

The evidence supplied here is a starting point.

If profiling reveals that another issue is actually more important, follow the evidence.

For example:

```text
If GPU blur dominates → optimize rendering first.

If main-thread recomposition dominates → fix state architecture first.

If image decode dominates → fix image pipeline first.

If GC dominates → investigate allocations/bitmaps.

If audio processing dominates → optimize visualizer processing.

If startup dominates → optimize initialization/baseline profile.
```

Do not force the implementation to match this document if measured evidence contradicts it.

---

# 100. THE FINAL GOAL

The final RIKO experience should feel like this:

```text
User taps
    ↓
Immediate visual acknowledgement
    ↓
Immediate state transition
    ↓
Consistent frame delivery
    ↓
Crisp animation
    ↓
No tiny hitch
    ↓
No unnecessary work
```

For scrolling:

```text
User drags
    ↓
Immediate content movement
    ↓
Stable frame pacing
    ↓
Artwork remains responsive
    ↓
Glass remains visually premium
    ↓
No periodic micro-stutter
```

For Now Playing:

```text
Artwork
+ ambient background
+ glass
+ waveform
+ visualizer
+ progress
+ controls

ALL remain visually rich,

but expensive work is isolated,
coalesced,
cached,
localized,
and lifecycle-aware.
```

The final result should **not** look like a simplified performance mode.

It should look like:

> **The same beautiful RIKO, but finally running with the responsiveness and smoothness its design deserves.**

---

# 101. NON-NEGOTIABLE END CONDITION

Do not report:

> “Optimization complete”

merely because:

- the project compiles
- there are no crashes
- FPS appears acceptable
- one screen became faster

The task is complete only when:

1. Global unnecessary invalidation is reduced.
2. High-frequency state is localized.
3. Compose stability is improved where justified.
4. List rendering is significantly cheaper.
5. Image decoding is appropriately sized.
6. Heavy visual effects are optimized without destroying the design.
7. Waveform/visualizer work is lifecycle-aware and display-efficient.
8. Navigation/motion feels crisp.
9. Startup is examined.
10. Functional behavior remains intact.
11. Before/after profiling or equivalent evidence supports the major optimizations.
12. No obvious new performance regression is introduced.

---

# 102. EXECUTION INSTRUCTION

Start by auditing the existing implementation.

Do NOT immediately rewrite code.

First establish:

```text
Current rendering tree
Current state flow
Current high-frequency data paths
Current expensive drawing paths
Current image pipeline
Current animation graph
Current lifecycle behavior
```

Then produce a ranked performance diagnosis.

Then implement the fixes in priority order.

After each major phase, build and validate.

At the end, provide:

```text
Root causes
+
Files changed
+
Architectural changes
+
Rendering changes
+
Image changes
+
Visualizer changes
+
Motion changes
+
Verification
+
Remaining risks
```

The objective is not merely optimization.

The objective is to make **RIKO Music feel genuinely premium, immediate, fluid, and production-grade without sacrificing the visual identity that makes RIKO RIKO.**