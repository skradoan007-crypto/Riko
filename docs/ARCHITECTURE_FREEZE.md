# RIKO EXPERIENCE OS — ARCHITECTURE FREEZE

> এই document টি B5.3 (Library Rendering UI) শুরু হওয়ার ঠিক আগে
> একটি permanent freeze declaration. এই freeze এর পরে locked
> layers এ আর কোনো feature add হবে না — শুধু bug fix যাবে।

---

## 1. LOCKED LAYERS

নিচের সব layer আনুষ্ঠানিকভাবে LOCKED 🔒। এগুলোতে আর নতুন
feature যোগ করা যাবে না — শুধু bug fix এবং documentation যাবে।

| # | Layer                                  | Path                                                                  | Files | Lines  | Build | Score      | Status        |
|---|----------------------------------------|-----------------------------------------------------------------------|-------|--------|-------|------------|---------------|
| 1 | Collection Constitution                | `presentation/library/contract/`                                      | 15    | ~6000  | B1 v2 | 10/10      | 🔒 LOCKED     |
| 2 | Collection Runtime                     | `presentation/library/runtime/`                                       | 5     | ~2500  | B2    | 10/10      | 🔒 LOCKED     |
| 3 | Collection Engine                      | `presentation/library/engine/`                                        | 13    | ~3500  | B3    | 9.998/10   | 🔒 LOCKED     |
| 4 | Rendering Runtime                      | `presentation/library/rendering/`                                     | 14    | ~3900  | B4    | 10/10      | 🔒 LOCKED     |
| 5 | Library Screen Constitution            | `presentation/library/screen/contract/`                               | 15    | 4914   | B5.1  | 10/10      | 🔒 LOCKED     |
| 6 | Library Screen Runtime                 | `presentation/library/screen/runtime/`                                | 8     | 3720   | B5.2  | 9.99/10    | 🔒 LOCKED     |
| 7 | Library Background Language            | `presentation/library/screen/background/`                             | 5     | 2125   | B5.3-A | 10/10     | 🔒 LOCKED     |
| 8 | Library Identity Layer                 | `presentation/library/screen/identity/`                               | 9     | ~4823  | B5.3-B | 9.995/10  | 🔒 LOCKED     |
| 9 | Library Living Motion                  | `presentation/library/screen/motion/`                                 | 6     | 3636   | B5.3-C | 10/10     | 🔒 LOCKED     |
| 10 | Library Composition (Invisible Layer) | `presentation/library/screen/composition/`                            | 4     | 3869   | B5.3-D | 10/10     | 🔒 LOCKED     |

**Total locked codebase:** ~94 files, ~38,800 lines of pure Kotlin + Compose
(Phase A/B/C/D — pure rendering, zero business logic, zero state mutation).
Library Rendering UI milestone OFFICIALLY COMPLETE. 🔒

---

## 2. RULES #58 – #61 (NEW — REGISTERED WITH THIS FREEZE)

এই চারটি Rule B5.2 review এর সময় চালু হয় এবং এখন থেকে B5.3+ সব
layer এ enforce হবে।

### ⭐ Rule #58 — Runtime Never Knows Rendering

**মানে:**
Runtime (B2 Collection Runtime + B5.2 Screen Runtime) জানবে না —

- Glass
- Wave
- Blur
- Canvas
- Card
- Padding
- Color (hex / argb)
- Animation (duration / easing / spring)

**Runtime শুধু জানবে:**
- Hero
- Section
- Dock
- Selection
- Search
- Scroll
- Profile
- Phase
- Action
- Event

Rendering vocabulary Runtime-এ ঢুকবে না। যদি কোনো runtime file
এ `Color`, `Dp`, `Brush`, `Path`, `Canvas` শব্দ দেখা যায় — সেটা
violation এবং immediate fix আবশ্যক।

**Enforcement:** `screen/runtime/` এবং `runtime/` তে zero Compose /
zero Android framework import।

---

### ⭐ Rule #59 — Mapper Is The Last Translator

**মানে:**
Screen Mapper (B5.2 `LibraryScreenMapper`) এর পরে আর কোনো mapping
হবে না।

```
Engine → Mapper → ScreenState → UI
```

**UI-তে নিষিদ্ধ:**
- `if (...)` (যদি data-shape decision হয় — layout decision মানে)
- `when (...)` (data shape এর উপর)
- `map(...)`
- `convert(...)`
- `filter(...)` (UI filtering)
- `sortedBy(...)` (UI sorting)
- `take(...)` (UI truncation)
- `groupBy(...)` (UI grouping)
- `chunked(...)` (UI chunking)

**UI শুধু করবে:**
- Render করবে
- Subcompose করবে
- Layout করবে (measure / place)
- Animation চালাবে

State Mapper থেকে বের হওয়ার সাথে সাথে already renderable।

---

### ⭐ Rule #60 — Screen State Is Already Renderable

**মানে:**
Component (Composable) যেন কখনো না লিখতে পারে —

```kotlin
// ❌ FORBIDDEN in Composable
title.uppercase()
subtitle.take(18)
items.sortedBy { it.timestamp }
items.filter { it.isVisible }
items.chunked(3)
items.groupBy { it.category }
```

সব এই transformation Mapper-এ হবে। State যখন Composable এ পৌঁছায়,
সে already final — uppercase করা, truncated, sorted, filtered,
chunked, grouped।

**Enforcement:** B5.3 component review এ প্রতিটি Composable এর ভেতরে
data transformation call খোঁজা হবে।

---

### ⭐ Rule #61 — Coordinator Never Mutates State

**মানে:**
Coordinator (B5.2 `LibraryScreenCoordinator`) শুধু request করবে।

```
❌ FORBIDDEN
Coordinator → State Change

✅ REQUIRED
Coordinator → Request
           → Host
           → Event
           → Reducer
           → New State
```

Coordinator কখনো সরাসরি State পরিবর্তন করবে না। সে শুধু
`CoordinatorRequest` তৈরি করবে, এবং Host (B5.5 ViewModel) সেই
request execute করে Event emit করবে, যেটা Reducer consume করে
new State তৈরি করবে।

**Enforcement:** `LibraryScreenCoordinator` এর signature অবশ্যই
হবে — `(State, Actions) → List<CoordinatorRequest>`। কোনো mutable
state field, কোনো `var`, কোনো direct state assignment নয়।

---

### ⭐ Rule #62 — Never Rebuild Existing Systems

**মানে:**
যদি কোনো system ইতিমধ্যেই mature হয় — Library সেটা consume করবে।

Already-mature systems in RIKO:
- **Glass Engine V2** — `core/designsystem/glass/GlassSurface.kt` +
  `GlassStyle` + `GlassTokens` (intent-based, audit-derived)
- **Wave Engine** — `core/experience/wave/WaveEngine.kt` +
  `WaveCanvas` + `WaveRibbonHost` + 4-layer render pipeline
- **Riko Color System** — `core/designsystem/theme/Color.kt`
  (Abyss, Void, Depth, Surface, AccentCyan, AccentViolet, etc.)
- **Riko Dimension System** — `core/designsystem/theme/Dimensions.kt`
  (`RikoDimens` object)
- **Riko Motion System** — `core/designsystem/theme/RikoMotion.kt`
  + `Motion.kt` + `SpringMotion.kt`
- **Riko Live Clock** — `core/designsystem/animation/RikoLiveClock.kt`

**Duplicate code = reject.** If a Phase A/B/C/D file finds itself
reimplementing blur logic, glass tinting, wave physics, or color
extraction — STOP. The existing system must be wrapped, not rebuilt.

**Enforcement:** B5.3 PR review checks every new file for:
- No new `Paint()` / `Brush.verticalGradient` / `drawBehind` blocks
  that re-implement what `GlassSurface` already does
- No new sine/cosine/path-generation code that re-implements what
  `WaveEngine` already does
- No new color constants that duplicate `Color.kt` entries

---

### ⭐ Rule #63 — Background Never Demands Attention

**মানে:**
Background-এর কাজ — **Support. Never compete.**

```
❌ FAIL: User Background-ই বেশি notice করে
✅ PASS: User Hero notice করে — Background silent
```

Background কখনো নিজে নিজে attention demand করবে না। তার
vocabulary হবে —

- Low alpha (0.05–0.20 range for ambient layers)
- Muted hues (no saturated reds / oranges / magentas)
- Slow motion (≥ 8s cycle for breathing, never pulsing)
- No glow / bloom on background (those belong to Hero / Wave)
- No contrast against content (background must recede)

**Enforcement:** B5.3 Phase A review:
- Background base color L value ≥ 5 (very dark) or follows profile
- Ambient alpha ≤ 0.20 (DEFAULT) / ≤ 0.15 (AMOLED)
- No animated hue shifts > 5° on background
- No background element uses `glowAlpha > 0.10`

---

### ⭐ Rule #64 — Motion Must Be Discoverable, Not Obvious

**মানে:**
Motion এমন হবে না — "ওই দেখ animation."
Motion এমন হবে — "একটু পরে খেয়াল করলাম screen-টা জীবন্ত।"

This subtlety is what separates premium apps from amateur ones.

**Concrete thresholds for "discoverable motion":**
- Duration ≥ 8 seconds per cycle (breathing / drifting)
- Alpha delta ≤ 0.10 per cycle (subtle fade, not flash)
- Position delta ≤ 6% of screen dimension (drift, not pan)
- No motion > 0.5 second of explicit animation on entrance
- If reduce-motion is on → motion becomes crossfade or instant

**Forbidden patterns:**
- Pulsing (alpha oscillating 0.0 → 1.0 → 0.0)
- Flashing (any rapid color change)
- Bouncing (any spring with dampingRatio < 0.6 on background)
- Spinning (any rotation on background)
- Visible parallax shift on background (parallax belongs to Hero)

**Enforcement:** B5.3 Phase A review:
- `InfiniteTransition` cycle ≥ 8s
- `animateFloat*` range width ≤ 0.10
- No `repeatMode = RESTART` (use `REVERSE` for soft oscillation)
- Reduce-motion contract field honored

---

### ⭐ Rule #65 — Hero Never Competes

**মানে:**
Hero কখনো Collection-এর সাথে competition করবে না।
সে শুধু direction দিবে।

Hero-র কাজ — ব্যবহারকারীকে বলা "এই screen-এ নিচে কী আছে",
সে নিজে spotlight চাইবে না।

```
❌ FAIL: User শুধু Hero-তেই তাকিয়ে থাকে, Collections খেয়াল করে না
✅ PASS: User Hero দেখে → scroll করে → Collections এ চলে যায়
```

**Enforcement:** B5.3 Phase B review:
- Hero card-এর glow alpha ≤ 0.50 (Hero পাবে, কিন্তু blinds করবে না)
- Hero card-এর saturation accent color-এর 70% এ ক্যাপ করা
- Hero-তে animation > 720ms হবে না (entrance), > 14s হবে না (idle)
- Hero এবং প্রথম Collection-এর মধ্যে spacing ≥ 24dp
- Hero text যেন title ধরে রাখে, body text overflow না করে

---

### ⭐ Rule #66 — Collections Feel Discoverable

**মানে:**
Collection দেখে user-এর মনে হবে —
"এখানে কিছু interesting আছে."

না যে — "আরও একটা list."

Collections হলো *invitations*। প্রতিটি card একটা tiny story।
Title একাই পুরো অধ্যায় বলবে না — subtitle, artwork, accent এবং
layout মিলে একটা micro-narrative তৈরি করবে।

```
❌ FAIL: Card দেখে user-এর মনে হয় "আরেকটা album thumbnail"
✅ PASS: Card দেখে user-এর মনে হয় "এটা আমার জন্য বাছাই করা"
```

**Enforcement:** B5.3 Phase B review:
- প্রতিটি CollectionCard-এ subtitle / metadata field থাকবে
- Card corner radius ≥ 12dp (sharp cards মনে হয় cold)
- Card-এ subtle accent glow থাকবে (glowAlpha ≤ 0.10)
- Card-এ hover/press state একটা micro-feedback দিবে
- Row-তে cards-এর মধ্যে spacing ≥ 12dp (crowded = list)

---

### ⭐ Rule #67 — Dock Never Interrupts

**মানে:**
Mini Player Dock থাকবে।
কিন্তু attention চাইবে না।

Need হলে notice হবে (track change, play state)।
Need না হলে environment-এর অংশ থাকবে।

Dock হলো screen-এর *floor*। ব্যবহারকারী যখন scroll করে,
Dock সেখানেই আছে — quiet, stable, ready। ব্যবহারকারী যখন
Dock দেখে চায়, সে সাথে সাথেই interaction পাবে।

```
❌ FAIL: Dock প্রতি 2 সেকেন্ডে pulse করে, user-এর চোখ সেখানে যায়
✅ PASS: Dock stable থাকে, শুধু track-change এ subtle flash করে
```

**Enforcement:** B5.3 Phase B review:
- Dock idle animation alpha delta ≤ 0.06 (breathing barely visible)
- Dock track-change flash ≤ 200ms (one-shot, not pulsing)
- Dock scroll-down hide duration ≥ 320ms (smooth, not jarring)
- Dock height never changes without animation (no jump)
- Dock glass tint alpha ≤ 0.12 (background-aware)

---

### ⭐ Rule #68 — Hero Explains Library

**মানে:**
Hero decorative না।
Hero user-কে বুঝাবে —
তোমার music world আজ কেমন।

Hero হলো screen-এর *headline*। ব্যবহারকারী যখন app খোলে,
Hero তাকে এক পলকে বলে দেয় — "এই মুহূর্তে তোমার Library-তে কী
ঘটছে।" Continue Listening হতে পারে, নতুন গান এসেছে, mood shift
হয়েছে, কিছু একটা।

```
❌ FAIL: Hero static title "Library" দেখায়, প্রতিদিন একই
✅ PASS: Hero দেখায় "Continue Listening: Bohemian Rhapsody" বা
        "12 new songs added today"
```

**Enforcement:** B5.3 Phase B review:
- Hero subtitle যেন dynamic — title আর subtitle উভয়ই আসবে state থেকে
- Hero artwork যেন current context বুঝায় (active song / featured collection)
- Hero একদম নিরেট হবে না — subtle overlay gradient সবসময় থাকবে
- Hero card-এ metadata badge থাকতে পারে (count, time, mood) কিন্তু ≤ 3 টা
- Hero empty state যেন graceful — কখনো ফাঁকা থাকবে না

---

### ⭐ Rule #69 — Sections Breathe Independently

**মানে:**
সব section একই animation করবে না।
Tiny phase offset থাকবে।
এতে পুরো screen mechanical লাগবে না।

যখন Library তৈরি হয়, sections একে একে entry করে — একটু একটু
stagger করে। Hero প্রথমে, তারপর Smart Collections, তারপর Albums —
প্রতিটির জন্য 40-80ms দেরি। এটা কৃত্রিম না — এটা RIKO-র
"organic" সত্ত্বা প্রকাশ করে।

```
❌ FAIL: সব sections একই সময়ে fade-in করে (mechanical feel)
✅ PASS: Sections staggered, tiny phase offsets, organic feel
```

**Enforcement:** B5.3 Phase B review:
- Section entrance stagger ≥ 40ms (per Section)
- Section breathing phase offset ≥ 1.2s (when ambient drifts, sections drift differently)
- Section collapse/expand animation যেন independent (no shared state)
- Section header এবং section body motion যেন decoupled
- Reduce-motion এ stagger disabled (all simultaneous, but with crossfade)

---

### ⭐ Rule #70 — Architecture Freeze

**মানে:**
নতুন milestone-এ নতুন architecture invent করা যাবে না।
শুধু existing pattern reuse হবে।

আগামী milestones:
- Search
- Queue
- Settings
- Profile
- Playlist
- Lyrics

এগুলোর কোনোটাতেই আনা যাবে না —
- নতুন Runtime Pattern
- নতুন Reducer Pattern
- নতুন State Pattern
- নতুন Contract Pattern

RIKO-র signature pipeline এখন frozen:
```
Contract → Runtime → Reducer → Mapper → Rendering Runtime → Compose → Integration
```

এটাই RIKO-র architecture। এই pattern ভাঙলেই product-এর পরিচয় নষ্ট হয়।

```
❌ FAIL: Search milestone-এ আলাদা reducer pattern চালু হয়
✅ PASS: Search একই Contract→Runtime→Reducer→Mapper pipeline follow করে
```

**Enforcement:** প্রতিটি নতুন milestone review-এ এই checklist থাকবে:
- নতুন `*Reducer.kt` file signature `(State, Event) → Pair<State, List<Action>>`?
- নতুন `*Coordinator.kt` pure Kotlin returning `CoordinatorRequest`?
- নতুন `*Mapper.kt` is last translator (Rule #59)?
- নতুন `*Contract.kt` zero Compose/Android import?
- Zero "novel pattern" — সব familiar

---

### ⭐ Rule #71 — New Screen Must Feel Familiar

**মানে:**
User যেন বুঝতেই না পারে "এটা নতুন screen"।
বরং মনে হয় — "এটা RIKO-র আরেকটা অংশ।"

প্রতিটি নতুন screen একই visual DNA share করবে:
- Same glass material (GlassSurface + GlassTokens)
- Same color palette (Abyss, Void, AccentCyan, AccentViolet)
- Same depth stack (LibraryDepth strata)
- Same motion vocabulary (RikoMotion + Phase A ambient)
- Same hero pattern (Hero → Sections → Dock)
- Same spacing rhythm (RikoDimens)

```
❌ FAIL: Settings screen-এ আলাদা card style, আলাদা spacing
✅ PASS: Settings screen-এ একই CollectionCard ব্যবহার, শুধু content আলাদা
```

**Enforcement:** নতুন screen review-এ:
- Glass token reuse ≥ 80% (vs new tokens)
- Color palette reuse = 100% (no new accent colors)
- Motion spec reuse ≥ 70% (Phase A/B/C motion reused)
- Component reuse ≥ 60% (existing Composables composed, not rebuilt)
- "ভিন্ন" feel allowed — "ভিন্ন pattern" forbidden

---

### ⭐ Rule #72 — Identity Before Feature

**মানে:**
যদি একটা feature আর একটা feel-এর মধ্যে choose করতে হয় —
Always choose feel.

কারণ feature copy করা যায়।
Feeling copy করা কঠিন।

Feature = "কী করতে পারে"
Feel = "কেমন লাগে"

Premium app-এর difference feature-এ না — feel-এ।
Spotify-এর "Wrapped" feature না — feel. Apple Music-এর
spatial audio feel না — feature। কিন্তু যেটা মানুষ মনে রাখে,
সেটা feel।

```
❌ FAIL: "Shuffle করি" feature আছে কিন্তু cold button — feature ok, feel না
✅ PASS: "Shuffle" বললে একটা subtle ripple — feature ছোট, feel বড়
```

**Enforcement:** প্রতিটি feature ship-এর আগে এই প্রশ্ন:
- এই feature-টার সাথে কোনো feel যুক্ত আছে কি?
- যদি শুধু function থাকে আর feel না থাকে — feel যোগ করা হবে
- যদি feature আর feel conflict করে — feel জিতবে, feature defer হবে
- "Feature checklist" না — "Experience checklist"

---

### ⭐ Rule #73 — Motion Must Have Meaning

**মানে:**
Animation শুধু সুন্দর হওয়ার জন্য না।
Animation একটা information।

প্রতিটা motion-এর একটা semantic message থাকতে হবে:

| Motion               | Message                                  |
|----------------------|------------------------------------------|
| Dock hide            | User scroll করছে                         |
| Dock reveal          | User উপরে যেতে চায়                       |
| Hero breathing       | Library alive                            |
| Wave pulse           | Music alive                              |
| Card press dim       | Tappable, feedback given                 |
| Section stagger      | Organic, not mechanical                  |
| Loading shimmer      | Content আসছে                            |
| Empty state breath   | Empty না — resting                       |
| Track-change flash   | নতুন গান শুরু                            |

প্রতিটা motion-কে এই প্রশ্ন করা হবে: "তুমি কী বলছ?"
যদি উত্তর না থাকে — সেই motion বাদ।

```
❌ FAIL: Card-এ সবসময় pulsing glow — কিন্তু কেন?
✅ PASS: Card-এ glow শুধু তখনই যখন currently playing item এর সাথে related
```

**Enforcement:** প্রতিটা animation declaration review-এ:
- Motion-এর semantic message explicitly documented (KDoc)
- Decorative-only motion = banned
- Pulsing without context = banned
- "Looks nice" justification = rejected
- Each `InfiniteTransition` এর সাথে একটা `// Meaning:` comment

---

### ⭐ Rule #74 — Silence Is Also UX

**মানে:**
সব সময় কিছু না কিছু নড়তে হবে — এটা ভুল।

অনেক সময় static UI premium লাগে।
Movement তখনই হবে যখন তার meaning আছে।

Premium apps-এর hallmark: তারা জানে কখন থামতে হয়।
- Apple Music-এ Now Playing সবসময় নড়ে না — শুধু play state-এ
- Photos app-এ grid সবসময় animate করে না — শুধু scroll-এ
- Mail-এ mail list static — animation শুধু action-এ

```
❌ FAIL: Library-তে idle অবস্থায় ৫টা জিনিস নড়ছে (chaotic)
✅ PASS: Library idle অবস্থায় শুধু background ambient — বাকি static
```

**Enforcement:** প্রতিটা screen-ে motion audit:
- Idle state motion count ≤ 2 (background ambient + at most one more)
- Active playback adds ≤ 1 motion (wave pulse)
- Loading state adds ≤ 1 motion (shimmer)
- User interaction adds ≤ 1 motion per gesture
- মোট motion count কখনো > 5 হবে না একই screen-এ
- Reduce-motion চালু থাকলে সব motion বাদ, শুধু crossfade

---

### ⭐ Rule #75 — Composition Is Invisible

**মানে:**
Composition file (`LibraryComposition.kt`) conductor। Musician না।
সে নিজে কোনো UI invent করবে না, কোনো animation invent করবে না, কোনো token invent করবে না।
সে শুধু assemble করবে — পূর্বে build করা সব layers কে একটা tree করবে।

```
❌ FAIL: LibraryComposition-এ নতুন @Composable দিয়ে একটা "loading overlay" invent করা
✅ PASS: LibraryComposition শুধু LibraryLoadingView() call করবে (Phase C-তে আগে থেকে আছে)
```

Premium apps-এর composition signature: একটাই root composable, যা ৫-৬টা অন্য composable-কে একটা নির্দিষ্ট order-এ arrange করে।
- Apple Music-এ `NowPlayingView` শুধু assemble করে — artwork, lyrics, controls আলাদা।
- Spotify-এ `HomeScreen` শুধু sections arrange করে — প্রতিটা section নিজে draw করে।
- Photos-এ `PhotoGrid` শুধু grid layout করে — photo cell অন্য file-এ।

**Enforcement:** Composition file-এ audit:
- Zero `drawBehind` / `Canvas` / `Brush.*` থাকবে না (no rendering)
- Zero `InfiniteTransition` / `Animatable` / `animateFloat` থাকবে না (no animation)
- Zero `Color` / `Dp` / `TextUnit` literal থার্ড না (no token invention)
- Zero `data class` / `sealed class` / `enum class` (no contract invention)
- শুধু function calls: BackgroundHost, HeroContainer, SectionHeader, CollectionRow/Grid, Dock, LoadingView, EmptyState, WaveAmbient, etc.
- Single entry point: `LibraryComposition(constitution, state, modifier)`
- Composition-এর ভেতরে শুধু `when(state.phase)` branching — কোনো business logic না

---

### ⭐ Rule #76 — Performance Guards Never Optimize

**মানে:**
`LibraryPerformanceGuard` শুধু observe করবে, report করবে।
Optimize করবে না। Degrade করবে না। Trigger করবে না।

অনেকেই এখানে ভুল করে — guard কে বলে দেয় "budget exceed হলে frame rate কমিয়ে দাও" বা "glass blur বন্ধ করে দাও"। এটা ভুল।
Guard-এর কাজ শুধু — সত্য বলা। সে বলবে "frame time 18ms, budget 16ms, over budget by 2ms"।
তারপর developer সিদ্ধান্ত নেবে কী optimize করবে।

```
❌ FAIL: PerformanceGuard budget exceed হলে automatically reduceMotion=true set করে দিল
✅ PASS: PerformanceGuard budget exceed হলে log করে + report emit করে — UI unchanged
```

Premium apps-এর discipline: performance observability সবসময় measurement থেকে আলাদা।
- Android Studio Profiler — measure করে, code change করে না
- Xcode Instruments — report করে, fix করে না
- Chrome DevTools Performance tab — show করে, optimize করে না

**Enforcement:** PerformanceGuard-এ audit:
- Zero `set`/`update`/`copy` state mutation (no optimization trigger)
- Zero `when budget exceeded` → auto-degrade logic
- Zero Coroutine launch for "adaptive degradation"
- Reports are pure data classes (immutable snapshots)
- `auditPerformance(state, schedule, layers)` returns `LibraryPerformanceReport` — pure function
- Report contains `warnings: List<String>` — text human-readable
- Report contains `isWithinBudget: Boolean` — single boolean answer
- Report never has `recommendedAction` field — that's developer's job to decide

---

### ⭐ Rule #77 — Preview Is Documentation

**মানে:**
Preview শুধু screenshot না। Preview হল Living Documentation।
নতুন developer Preview tab খুলেই পুরো screen বুঝে যাবে — code না পড়লেও।

প্রতিটা Preview এর নাম self-explanatory হবে: কোন profile, কোন state, কোন scenario।
Preview-তে শুধু "PreviewDefault" না — "PreviewMorningReady50K" হবে।

```
❌ FAIL: @Preview name = "Default" — কিছুই বোঝা যায় না
✅ PASS: @Preview name = "Morning — Ready — 50K songs — DEFAULT profile"
```

Premium teams-এর hallmark: Preview-তে প্রতিটা meaningful scenario covered থাকে।
- Apple-এর SwiftUI previews প্রতিটা state + size class + locale দেখায়
- Material 3 Catalog প্রতিটা component এর dark/light/dynamic theme দেখায়
- Jetpack Compose samples প্রতিটা interactive state দেখায়

**Enforcement:** PreviewKit-এ audit:
- প্রতিটা `@Preview` annotation-এ থাকবে:
  - `name` — human-readable (profile + state + scenario)
  - `showBackground = true` + `backgroundColor = 0xFF050505`
  - `device` parameter (when applicable — phone/tablet/foldable)
  - `widthDp` / `heightDp` (when form-factor specific)
- প্রতিটা profile (DEFAULT, AMOLED, CINEMATIC, COMPACT, LOW_END, HIGH_END, REDUCED_MOTION, HIGH_CONTRAST) — অন্তত একটা preview
- প্রতিটা phase (COLD_START, LOADING, PARTIAL_READY, READY, EMPTY, ERROR, PERMISSION_REQUIRED, SELECTION_MODE) — অন্তত একটা preview
- প্রতিটা content scale (0 songs, 50 songs, 500 songs, 50K songs) — অন্তত একটা preview
- Form factors (phone-portrait, phone-landscape, tablet, foldable) — অন্তত একটা preview
- Accessibility (reduced motion, high contrast) — অন্তত একটা preview
- Time-of-day (morning, night) — অন্তত একটা preview
- প্রতিটা preview-তে `@Composable` একটা named function — নাম থেকেই scenario বোঝা যায়

---

## 2.5 RULES #78 – #82 (B5.4 — LIBRARY INTERACTION RUNTIME)

এই পাঁচটি Rule B5.4 শুরুর সাথে freeze হলো। Library Interaction Runtime
সব সময় এই নিয়ম মানবে।

### ⭐ Rule #78 — Interaction Creates Intent, Never Action

**মানে:**
Gesture কখনো সরাসরি Action emit করবে না। প্রতিটা interaction একটা
**Intent** তৈরি করবে। Intent Reducer এ যাবে। Reducer decide করবে কোন
Action দরকার।

```
Gesture → Intent → Reducer → Action
```

**কেন:** Action হল কর্ম (execute)। Intent হল ইচ্ছা (will)। যদি gesture
সরাসরি Action emit করে — gesture কে decide করতে হবে "এখন কী execute হবে"।
সেটা UI-এর কাজ না। আর যদি gesture শুধু intent emit করে, Reducer পুরো
state দেখে সিদ্ধান্ত নিতে পারে।

```
❌ FAIL: onTap → emit PlaySongAction(songId)
✅ PASS: onTap → emit SongTappedIntent(songId)
                            ↓
                  Reducer checks state:
                  - SelectionMode active? → SelectItem(songId)
                  - Drag pending? → Ignore (let drag win)
                  - Otherwise? → PlaySongAction(songId)
```

**Enforcement:**
- `LibraryGesture` sealed class শুধু `Intent` return করবে
- `GestureResolver` শুধু `Intent` produce করবে
- `LibraryAction` শুধু Reducer থেকে আসবে — gesture থেকে না
- Inspector-এ প্রতিটা Intent-এর সাথে source gesture থাকবে (Rule #81)

---

### ⭐ Rule #79 — Selection Is Independent

**মানে:**
Card নিজের selection state own করবে না। Library own করবে। Card শুধু
query করবে — "আমি selected কি না?"

এর তিনটা কারণ:
1. **Range selection কাজ করে** — first item থেকে last item পর্যন্ত সব
   select হয়। Card নিজে এটা করতে পারে না, কারণ সে শুধু নিজের state জানে।
2. **Recomposition কম হয়** — একটা card toggle হলে শুধু সেই card
   recompose হয়, পুরো list না।
3. **Undo/Redo possible** — selection state centralized থাকলে snapshot/restore
   সহজ।

```
❌ FAIL: CollectionCard.kt এ `var isSelected by remember { mutableStateOf(false) }`
✅ PASS: LibraryState.selection.selectedItemIds: Set<Long>
         CollectionCard শুধু reads: isSelected = id in selectedItemIds
```

**Enforcement:**
- `SelectionRuntime` centrally manage করবে সব selection
- Card / Row / Grid composables কখনো local selection state own করবে না
- SelectionReducer হবে pure: `(SelectionState, SelectionIntent) → SelectionState`
- Selection state persist হবে across configuration change (savedInstanceState)

---

### ⭐ Rule #80 — Gesture Never Knows UI

**মানে:**
Gesture জানবে না —
- Glass আছে কি না
- Wave চলছে কি না
- Card-এর rounded corners কত
- Hero বড় না ছোট
- Dock expanded না collapsed

Gesture শুধু জানবে —
- **Coordinates** (x, y, dx, dy)
- **Time** (down timestamp, duration)
- **Velocity** (px/sec)
- **Pointer count** (1, 2, 3, ...)
- **Bounds** (gesture started where)

```
❌ FAIL: if (glassStrength > 0.5) longPressThreshold = 400 else 600
✅ PASS: longPressThreshold = 500.ms  (constant — gesture doesn't know glass)
```

**Enforcement:**
- `LibraryGesture` sealed class এ কোনো UI reference থাকবে না
- `GestureResolver` এ কোনো Glass/Wave/Card/Hero/Dock import থাকবে না
- Gesture thresholds হবে constant — profile-driven হতে পারে কিন্তু
  UI-state-driven না
- Dock Pull gesture শুধু screen-edge coordinates জানবে, Dock-এর
  morph state জানবে না

---

### ⭐ Rule #81 — Every Interaction Is Replayable

**মানে:**
Inspector থেকে যেকোনো interaction replay করা যাবে। মানে একই gesture
আবার চালালে একই Intent আসবে, একই State change হবে।

এর দুটো কারণ:
1. **Debugging heaven** — bug reproduce করতে গিয়ে "আমি এইভাবে touch
   করেছিলাম" আর বলতে হবে না। Inspector-এ recording আছে।
2. **Future automation** — macro / script / test সব interaction replay
   করে চালাতে পারবে।

```
Gesture(touchDown(100,200), hold(82ms), movement(3dp), touchUp)
   ↓
Recorded: GestureRecord(id, timestamp, type, params, source)
   ↓
Replay: gestureRecord.replay() → same Intent → same State change
```

**Enforcement:**
- প্রতিটা `LibraryGesture` হবে `data class` (সব fields immutable, সব
  serializable)
- `InteractionDiagnostics` প্রতিটা gesture record করবে
- Replay API: `interactionDiagnostics.replay(gestureRecordId)`
- Replay কখনো live state-কে affect করবে না — শুধু dry-run report দেবে

---

### ⭐ Rule #82 — Every Gesture Has Explanation

**মানে:**
Inspector বলবে শুধু "Tap accepted" না — বলবে কেন accept হলো:

```
Tap accepted
Reason:
  - Movement: 3dp (within slop of 8dp)
  - Duration: 82ms (within tap window of 100ms)
  - Priority: CARD (resolved over CHILD)
  - Conflict: LongPress cancelled (movement exceeded slop)
  - Outcome: → SongTappedIntent(songId=42)
```

এর কারণ:
1. **Debugging** — "Tap কাজ করছে না কেন?" এর উত্তর Inspector-এ আছে
2. **Tuning** — thresholds ঠিক করতে গেলে দরকার জানা যে এখন কী হচ্ছে
3. **Confidence** — user যখন দেখবে প্রতিটা gesture এর explanation আছে,
   সে trust করবে যে system ঠিকঠাক কাজ করছে

**Enforcement:**
- `GestureResolution` data class এ থাকবে `explanation: GestureExplanation`
- `GestureExplanation` হবে structured:
  ```kotlin
  data class GestureExplanation(
      val accepted: Boolean,
      val winner: LibraryGesture?,
      val losers: List<LibraryGesture>,
      val metrics: GestureMetrics,         // movement, duration, velocity
      val reason: String,                  // human-readable summary
      val intentProduced: Intent?          // what intent was generated
  )
  ```
- `InteractionValidator` check করবে প্রতিটা resolution-এ explanation আছে
- Empty explanation = validation failure

---

## 2.6 RULE #83 (B5.5 — LIBRARY HOST INTEGRATION)

### ⭐ Rule #83 — Coordinator Executes. Reducer Never Executes.

**মানে:**
B5.2-তে `LibraryScreenCoordinator` pure Kotlin — সে শুধু `CoordinatorRequest`
plan করে, কিছু execute করে না। B5.5-তে আমরা একটা **executing coordinator**
introduce করছি — সেই `CoordinatorRequest`-গুলো সে actually run করে।

কিন্তু Reducer কখনো execute করবে না। Reducer pure থাকবে —
`(State, Event) → Pair<State, List<Action>>`।

```
Reducer (pure)              ←  State + Actions only
  │
  ▼
Screen Coordinator (pure)   ←  Plans CoordinatorRequests
  │
  ▼
Host Coordinator (B5.5)     ←  Executes: PlayerManager, Repository, NavController, Haptic
  │
  ▼
Result Events               ←  Fed back as new Events to Reducer
```

**কেন এই separation?**
1. Reducer unit-testable থাকে — zero side effects
2. Screen Coordinator unit-testable থাকে — zero Android
3. Host Coordinator-এ শুধু এক জায়গায় side effects — audit সহজ
4. Replay/debug সহজ — প্রতিটা request log করা যায়

**Enforcement:**
- `LibraryHostCoordinator` class — receives `List<CoordinatorRequest>`, executes via:
  - `PlayerManager` (playback)
  - `MusicRepository` / `PlaylistRepository` (data)
  - `NavController` (navigation)
  - `HapticFeedback` (haptic)
  - `SnackbarHostState` (snackbar)
- Reducer-এ কোনো `suspend` function নেই, কোনো Android import নেই
- Screen Coordinator-এ কোনো `suspend` function নেই, কোনো `ViewModel` import নেই
- Host Coordinator-ই একমাত্র জায়গা যেখানে side effects হয়

---

## 3. RULES REGISTRY (FULL — B1 থেকে B5.5 পর্যন্ত)

| #    | Rule                                       | Layer        | Status        |
|------|--------------------------------------------|--------------|---------------|
| #1   | Single Source of Identity                  | B1 Contract  | 🔒 Enforced   |
| #2   | Identity Never Recomputed                  | B1 Contract  | 🔒 Enforced   |
| #3   | Snapshot Is Truth At A Point In Time       | B1 Contract  | 🔒 Enforced   |
| ...  | ... (B1-B4 rules — abbreviated)            | ...          | 🔒 Enforced   |
| #44  | Stable Key Always                          | B1+          | 🔒 Enforced   |
| #45  | Events Are Verbs (Past Tense)              | B2/B5.2      | 🔒 Enforced   |
| #46  | No Random / No UUID In Domain              | B1+          | 🔒 Enforced   |
| #47  | Reducer Never Calls Engine                 | B5.2         | 🔒 Enforced   |
| #48  | Actions Are Nouns                          | B5.2         | 🔒 Enforced   |
| #49  | Strategy Never Scores                      | B3           | 🔒 Enforced   |
| #50  | Scorer Never Ranks                         | B3           | 🔒 Enforced   |
| #51  | Ranking Never Filters Identity             | B3           | 🔒 Enforced   |
| #52  | Cache Never Changes Data                   | B3           | 🔒 Enforced   |
| #53  | Diagnostics Never Affects Engine           | B3/B4/B5.2   | 🔒 Enforced   |
| #54  | Screen Tells a Story                       | B5.1         | 🔒 Enforced   |
| #55  | Screen Owns Composition                    | B5.2         | 🔒 Enforced   |
| #56  | (reserved — implicit)                      | —            | —             |
| #57  | Every Screen Has One Hero                  | B5.2         | 🔒 Enforced   |
| #58  | Runtime Never Knows Rendering              | B5.2+        | 🆕 REGISTERED |
| #59  | Mapper Is The Last Translator              | B5.2+        | 🆕 REGISTERED |
| #60  | Screen State Is Already Renderable         | B5.2+        | 🆕 REGISTERED |
| #61  | Coordinator Never Mutates State            | B5.2+        | 🆕 REGISTERED |
| #62  | Never Rebuild Existing Systems             | B5.3+        | 🆕 REGISTERED |
| #63  | Background Never Demands Attention         | B5.3+        | 🆕 REGISTERED |
| #64  | Motion Must Be Discoverable, Not Obvious   | B5.3+        | 🆕 REGISTERED |
| #65  | Hero Never Competes                        | B5.3+ Phase B| 🆕 REGISTERED |
| #66  | Collections Feel Discoverable              | B5.3+ Phase B| 🆕 REGISTERED |
| #67  | Dock Never Interrupts                      | B5.3+ Phase B| 🆕 REGISTERED |
| #68  | Hero Explains Library                      | B5.3+ Phase B| 🆕 REGISTERED |
| #69  | Sections Breathe Independently             | B5.3+ Phase B| 🆕 REGISTERED |
| #70  | Architecture Freeze                        | B5.3+ Phase C| 🆕 REGISTERED |
| #71  | New Screen Must Feel Familiar              | B5.3+ Phase C| 🆕 REGISTERED |
| #72  | Identity Before Feature                    | B5.3+ Phase C| 🆕 REGISTERED |
| #73  | Motion Must Have Meaning                   | B5.3+ Phase C| 🆕 REGISTERED |
| #74  | Silence Is Also UX                         | B5.3+ Phase C| 🆕 REGISTERED |
| #75  | Composition Is Invisible                   | B5.3+ Phase D| 🆕 REGISTERED |
| #76  | Performance Guards Never Optimize          | B5.3+ Phase D| 🆕 REGISTERED |
| #77  | Preview Is Documentation                   | B5.3+ Phase D| 🆕 REGISTERED |
| #78  | Interaction Creates Intent, Never Action   | B5.4+        | 🆕 REGISTERED |
| #79  | Selection Is Independent                   | B5.4+        | 🆕 REGISTERED |
| #80  | Gesture Never Knows UI                     | B5.4+        | 🆕 REGISTERED |
| #81  | Every Interaction Is Replayable            | B5.4+        | 🆕 REGISTERED |
| #82  | Every Gesture Has Explanation              | B5.4+        | 🆕 REGISTERED |
| #83  | Coordinator Executes. Reducer Never Does.  | B5.5+        | 🆕 REGISTERED |

---

## 4. ARCHITECTURE INVARIANTS (LOCKED)

এই 10 টি invariant এখন থেকে ভঙ্গ করা যাবে না —

1. Reducer is pure: `(State, Event) → Pair<State, List<Action>>`
2. Reducer NEVER calls Engine — Coordinator does (Rule #47)
3. Coordinator is pure Kotlin — returns `CoordinatorRequest` objects
4. Host (B5.5) executes `CoordinatorRequest` async (Coroutine/Flow)
5. Engine results arrive as new Events — Reducer handles in separate pass
6. State is immutable — Reducer returns new state, never mutates
7. Actions describe intent — never execution (Rule #48)
8. Diagnostics is observation-only — never affects behavior (Rule #53)
9. Mapper is the only place composition happens (Rule #55)
10. At most one Hero per screen (Rule #57)

---

## 5. B5.3 REFINED SCOPE — 4 PHASES

B5.3 কে 4 phase এ ভাগ করা হলো। প্রতিটি phase আগের phase এর
উপর দাঁড়ায়। কোনো phase skip করা যাবে না।

### Phase A — Background Language  (~800-1200L, 4-5 files)

> Hero render করার আগেই Background language establish হওয়া দরকার।

| File                                                | Responsibility                                                  |
|-----------------------------------------------------|-----------------------------------------------------------------|
| `LibraryBackgroundContract.kt`                      | Background shape — gradient, blur, ambient zones                |
| `LibraryGlassLayer.kt`                              | Glass material spec — blur radius, tint, noise, blur stratifier |
| `LibraryDepthStack.kt`                              | Z-axis depth layers — back, mid, front, overlay                 |
| `LibraryAmbientField.kt`                            | Ambient color field — time-of-day, mood-driven                  |
| `LibraryBackgroundPresets.kt`                       | 8 profile presets — DEFAULT, AMOLED, CINEMATIC, etc.            |

**Deliverable:** A background that breathes — no content yet, but you
already feel "এটা অন্যরকম".

---

### Phase B — Hero / Collections / Cards / Dock  (~2000-2800L, 7-9 files)

> এখন content বসবে background এর উপর।

| File                                                | Responsibility                                                  |
|-----------------------------------------------------|-----------------------------------------------------------------|
| `LibraryHeroCard.kt`                                | Hero composable — big, breathing, dominant                      |
| `LibraryHeroMotion.kt`                              | Hero motion spec — parallax, breathing, gesture response        |
| `LibraryCollectionCard.kt`                          | Per-personality card — 9 personality variants                   |
| `LibraryCollectionRow.kt`                           | Horizontal row — multi-card layout                              |
| `LibraryCollectionGrid.kt`                          | Grid layout — for ALL_ALBUMS / ALL_ARTISTS                      |
| `LibrarySectionHeader.kt`                           | Section header composable — story chapter title                 |
| `LibrarySectionContainer.kt`                        | Section wrapper — slots in RenderSection                         |
| `LibraryDockBar.kt`                                 | Mini player dock — bottom, persistent, breathing                |
| `LibraryDockMotion.kt`                              | Dock motion — expand/collapse, swipe-up reveal                  |

**Deliverable:** Full content visible — Hero at top, sections below,
dock at bottom. Static, but complete.

---

### Phase C — Motion / Wave / Loading / Empty  (~1200-1800L, 4-6 files)

> এখন সব কিছু নড়বে। Wave engine এর subtle language এখানে এসে যুক্ত হবে।

| File                                                | Responsibility                                                  |
|-----------------------------------------------------|-----------------------------------------------------------------|
| `LibraryWaveAmbient.kt`                             | Wave engine integration — subtle motion on background           |
| `LibraryLoadingShimmer.kt`                          | Loading state — shimmer cards, not spinners                     |
| `LibraryEmptyState.kt`                              | 11 empty kinds — each with personality                           |
| `LibraryMotionOrchestrator.kt`                      | Coordinates all motion — hero, dock, background, sections       |
| `LibraryGestureMotion.kt`                           | Gesture-driven motion — swipe, drag, long-press feedback        |
| `LibraryReducedMotionFallback.kt`                   | Accessibility — when reduced motion on, replace with crossfade  |

**Deliverable:** Screen is alive. Cards breathe, dock pulses subtly,
empty states feel intentional.

---

### Phase D — Composition / Performance / Validator / Preview  (~1500-2500L, 4 files)

> Composition file assemble করবে সব layer কে। Performance Guard observe করবে।
> Render Validator Rules enforce করবে। Preview Kit Living Documentation হবে।

| File                                                | Responsibility                                                  |
|-----------------------------------------------------|-----------------------------------------------------------------|
| `LibraryComposition.kt`                             | THE ROOT — assembles Background + Depth + Hero + Sections + Dock + Overlay + Effects |
| `LibraryPerformanceGuard.kt`                        | Observation-only — 16ms budget, overdraw, layer count, recomposition, allocation, glass count, wave count |
| `LibraryRenderValidator.kt`                         | Rule #58 / #60 / #70 enforcement — emits warnings, never blocks |
| `LibraryPreviewKit.kt`                              | Living Documentation — Morning/Night/AMOLED/50K/0/500/Accessibility/Reduced Motion/High Contrast/Landscape/Tablet/Foldable previews |

**Deliverable:** THE screen. Library milestone architecture officially complete.

---

### B5.3 TOTAL ESTIMATE

| Phase | Files | Lines (est.) | Actual Lines |
|-------|-------|--------------|--------------|
| A     | 4-5   | 800-1200     | 2125 ✅      |
| B     | 7-9   | 2000-2800    | ~4823 ✅     |
| C     | 4-6   | 1200-1800    | 3636 ✅      |
| D     | 4     | 1500-2500    | 3869 ✅      |
| **TOTAL** | **20-24** | **7000-10300** | **14453** ✅ |

**Library Rendering UI Milestone OFFICIALLY COMPLETE.** 🔒

---

## 5.4 B5.4 — LIBRARY INTERACTION RUNTIME ✅ LOCKED

> Library-এর Nervous System। Wave Engine ছিল Heart, Collection Engine ছিল Brain।
> Interaction Runtime হলো Nervous System। ✅ Complete — 15 files locked.

### Architecture Flow

```
Touch
  ↓
Gesture Interpreter
  ↓
Intent Generator
  ↓
Reducer
  ↓
Library State
  ↓
Composition
  ↓
Rendering
```

**UI never decides. UI শুধু intent পাঠায়।**

### 15 Deliverables (~4500–7000L Monster Build)

| # | File                                       | Responsibility                                                              |
|---|--------------------------------------------|-----------------------------------------------------------------------------|
| 1 | `LibraryGesture.kt`                        | 14 gestures define — Tap/DoubleTap/LongPress/Press/Hover/Drag/DragCancel/Fling/Swipe/Scroll/Pinch/MultiTouch/EdgeSwipe/Context/DockPull — no implementation |
| 2 | `GesturePriority.kt`                       | Hierarchy — System → Dock → Card → Child. Conflict resolution              |
| 3 | `GestureResolver.kt`                       | Conflict resolution — Long Press + Scroll → winner? Pure                   |
| 4 | `SelectionRuntime.kt`                      | Single/Multiple/Range/Invert/Clear/Pinned/Focused selection modes          |
| 5 | `SelectionReducer.kt`                      | Pure reducer for selection state                                            |
| 6 | `ContextMenuRuntime.kt`                    | Menu generation — never UI                                                  |
| 7 | `ContextAction.kt`                         | Pin/Rename/Delete/Hide/Queue/Favorite/Share/Info actions                    |
| 8 | `DockInteraction.kt`                       | Mini Player Dock interaction — morph awareness                              |
| 9 | `LibraryDragRuntime.kt`                    | Drag/Drop/Reorder/Hover/Threshold                                           |
| 10| `LongPressRuntime.kt`                      | Press lifecycle — down/hold/cancel/up                                       |
| 11| `HapticContract.kt`                        | Every vibration spec — amplitude/duration/curve                             |
| 12| `AccessibilityInteraction.kt`              | TalkBack/Keyboard/Controller/Mouse support                                  |
| 13| `InteractionDiagnostics.kt`                | Inspector — replay support (Rule #81)                                       |
| 14| `InteractionValidator.kt`                  | Rule violation warnings (Rule #82 explanation check)                        |
| 15| `InteractionPreview.kt`                    | Preview documentation                                                       |

### Path

`presentation/library/interaction/` — new sub-package, separate from
`screen/` (which holds rendering). Interaction is broader than screen —
it's the Library's nervous system, not the screen's gesture layer.

**Status:** ✅ All 15 files created, ~5200 lines, rules #78-#82 enforced.

---

## 5.5 B5.5 — LIBRARY HOST INTEGRATION (CURRENT — FINAL LIBRARY BUILD)

> এটাই Milestone 2-এর শেষ gate। এর পরে Library officially ship-ready।
> Production integration build — micro-step-এ ভাঙা নয়, একটা cohesive subsystem।

### Architecture

```
Repository ──┐
             │
Collection Engine ──┤
             │       combine()
Player State ─┤              │
             │              ▼
Color State ──┤       LibraryMapper
             │              │
Wave State ───┤              ▼
             │       LibraryReducer (pure)
MiniPlayer State ┘             │
                       │       ▼
                       │  List<Action>
                       │       │
                       │       ▼
                       │  Screen Coordinator (pure)
                       │       │
                       │       ▼
                       │  List<CoordinatorRequest>
                       │       │
                       │       ▼
                       │  HOST Coordinator (Rule #83 — executes)
                       │       │
                       │       ▼
                       │  Side Effects: PlayerManager / Repository /
                       │               NavController / Haptic / Snackbar
                       │       │
                       ▼       ▼
                    LibraryState ─► UI
```

### 7 Modules (~4000–6000L Production Build)

| # | File                                       | Responsibility                                                          |
|---|--------------------------------------------|-------------------------------------------------------------------------|
| 1 | `LibraryHostCoordinator.kt`                | THE EXECUTOR — receives CoordinatorRequest, executes via PlayerManager / Repository / Nav / Haptic / Snackbar (Rule #83) |
| 2 | `LibraryViewModelBridge.kt`                | Android ViewModel — combine() 6 upstream flows → Mapper → Reducer → StateFlow<LibraryState> |
| 3 | `LibraryScreenWiring.kt`                  | Compose screen entry — LibraryComposition + InteractionRuntime + Coordinator wiring |
| 4 | `MiniPlayerDockIntegration.kt`            | Library ↔ MiniPlayer morph continuity — no duplicate state, shared element handoff |
| 5 | `PlaybackIntegration.kt`                   | Card → Play → Queue → PlayerManager → Wave → MiniPlayer → NowPlaying — single call, zero duplication |
| 6 | `LibraryNavigationRuntime.kt`             | Album / Artist / Folder / Playlist / Collection / Search / NowPlaying / Settings routes — all via Coordinator |
| 7 | `LibraryValidationGate.kt`                 | Production checklist — Rotation / ProcessDeath / ConfigChange / Theme / Empty / 50k / SearchReturn / QueueReturn / MiniPlayerRestore / PlaybackContinue |

### Path

`presentation/library/host/` — new sub-package for B5.5 host integration.
This is the ONLY layer that knows about Android, ViewModel, Coroutine,
NavController. Everything above stays pure Kotlin.

---

## 6. POST-FREEZE PROTOCOL

এই freeze এর পরে —

✅ **Allowed:**
- Bug fix (কোনো incorrect behavior)
- Documentation update
- Test addition
- Performance optimization (behavior unchanged)

❌ **Forbidden:**
- নতুন field যোগ কোনো locked contract এ
- নতুন event / action / request যোগ কোনো locked runtime এ
- নতুন sealed variant যোগ কোনো locked sealed class এ
- API signature change কোনো locked file এ
- Package move / rename

যদি কোনো feature সত্যিই দরকার হয় — সেটা নতুন build এ যাবে
(B6+), নতুন contract তৈরি করে, locked contracts কে extend করে।

---

## 7. NEXT ACTION

**B5.4 — Library Interaction Runtime** শুরুর জন্য ready।

B5.4 এর 15 file লেখার আগে আমি verify করব —

1. `LibraryGestureContract.kt` (B5.1) এ কী gesture vocabulary আছে
2. `LibraryGestureMotion.kt` (Phase C) এ কী motion spec আছে
3. `LibraryAction.kt` (Phase A) এ কী existing actions আছে
4. `LibraryState.kt` (Phase A) এ selection state কোথায় live করে
5. `PlaybackGestures.kt` + `MiniPlayerGestureLayer.kt` এ existing pattern

তারপর B5.4 এর প্রতিটি file লিখব — pure Kotlin declaration,
zero business logic, zero state mutation, zero UI reference (Rule #80)।

Item order:
1. LibraryGesture.kt → 2. GesturePriority.kt → 3. GestureResolver.kt
4. SelectionRuntime.kt → 5. SelectionReducer.kt
6. ContextMenuRuntime.kt → 7. ContextAction.kt
8. DockInteraction.kt → 9. LibraryDragRuntime.kt → 10. LongPressRuntime.kt
11. HapticContract.kt → 12. AccessibilityInteraction.kt
13. InteractionDiagnostics.kt → 14. InteractionValidator.kt → 15. InteractionPreview.kt

---

**FREEZE TIMESTAMP:** B5.3 (Library Rendering UI) ALL 4 PHASES LOCKED ✅.
B5.4 Interaction Runtime ✅ LOCKED — 15 files, ~5200 lines.
B5.5 Host Integration 🔄 IN PROGRESS — 7 modules, ~4000-6000 lines.

**NEXT SIGNAL:** "Milestone 2 LOCK" → Library officially ship-ready।

---

© RIKO Experience OS — Architecture Freeze Tier

© RIKO Experience OS — Architecture Freeze Tier
