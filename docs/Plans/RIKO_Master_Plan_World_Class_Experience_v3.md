# RIKO EXPERIENCE SYSTEM — v3.3 (OBJECT INTELLIGENCE)

> **DO NOT TREAT THIS AS A VISUAL/UI BLUEPRINT.** This is an experience architecture — it governs how RIKO thinks, behaves, and evolves. It is RIKO's design operating system.

> **v3.3 Core Thesis:** Every important RIKO object should feel like it has a reason to exist, a physical identity, a state, a memory, and a way of responding. UI element → interaction object. Animation → behavior. Haptic → material feedback. Background → environment. State → physical condition. Screen transition → spatial continuity. Notification → compressed object. Error → recovery object.

**Documents this merges:**
1. `to plan about.md` — Objectives, philosophy, competitor analysis, screen-by-screen, priority matrix
2. `Current RIKO Master Plan v2.0` — Existing vision, philosophy, differentiation, systems
3. `Premium Smoothness & Performance Engineering Pass v1.0` — Performance constraints
4. **v3.3 Object Intelligence research** — 8 batches of deep research across audio control, player surface, dynamic environment, artwork, navigation, library, queue, playback state, time, notification, gesture, haptic, sheets, search, edge states

**v3.3 scope:** 54+ sections across 9 domains — Foundation · Intelligence · Object Intelligence Layer · Signatures · Exploration · Core Experiences · Systems · Validation · Execution.

**v3.2 North Star, four-signature limit, Living Timeline/Aurora/Wind-Down/Physicality philosophy, performance/accessibility principles — all preserved.** v3.3 adds an Object Intelligence Layer on top.

**This document is complete.** No further feature ideation. Future work = implementation, not expansion.

---

# PART I — FOUNDATION

---

## 01. PRODUCT NORTH STAR

**The Core Experience:**
> "RIKO should not make users notice the design.
> RIKO should make users notice the experience."

RIKO is not a launcher. Not a catalog. Not a visual demo.
RIKO is a *music experience* — where every interaction makes the user feel:

- **Engaged** — not passive, but involved
- **Respected** — never tricked, never overwhelmed
- **Delighted** — not by excess, but by precision

### The Three Emotional Territories

| Territory | Feeling | Signature Element |
|---|---|---|
| **MUSIC → দেখা যায়** | Music is visible | Living Timeline |
| **MUSIC → পরিবেশ বদলায়** | Environment responds to music | Aurora |
| **TIME → অনুভব করা যায়** | Time passes through the room | Wind-Down Timer |

These three are RIKO's identity. Everything else supports them.

### v3.3 North Star Extension

> **Every important RIKO object should feel like it has a reason to exist, a physical identity, a state, a memory, and a way of responding.**

---

## 02. EXPERIENCE PRINCIPLES

### Core Principles

1. **Music is the hero.** Glass, aurora, animation — all frame the music. Never compete with it.

2. **Intentional, not decorative.** Every animation communicates state, guides focus, or builds emotional connection. No animation exists because "it looks cool."

3. **Progressive complexity.** First use → simple. Repeated use → rich. Expert → powerful. Never overwhelming.

4. **The boring parts matter most.** Empty states, error recovery, loading, permissions — these are where trust is built or broken.

5. **Silence is a response.** Not every action needs haptic feedback. Not every state needs visual change. Not every moment needs motion.

6. **Audio-Reactive vs Audio-Representative — know the difference.**

   | Type | Behavior | Use |
   |---|---|---|
   | **Representative** | Communicates music structure/concept | Timeline waveform (amplitude structure), Aurora content identity |
   | **Reactive** | Responds to playback state in real-time | Timeline expression layer (playing bounce), Aurora intensity |

   RIKO uses both — but never confuses them. Representation = truth. Reaction = expression.

   **Critical:** Living Timeline has BOTH layers:
   - Layer A (Representation): actual amplitude structure from audio data — Audio-Representative
   - Layer B (Expression): playback-state visual treatment (bounce, glow) — Audio-Reactive

7. **Novelty must decay; utility must remain.** The first time: WOW. The tenth time: natural. The hundredth time: effortless. High-end products master this progression.

8. **RIKO redesigns the mental model of familiar objects, not the interface.** A slider should feel like control. A timeline should feel like a place. A timer should feel like disappearing time. A mini-player should feel like a persistent object.

### Principle Hierarchy

When principles conflict:
1. Music is hero (highest)
2. Intentional
3. Progressive complexity
4. The boring parts matter
5. Silence is a response
6. Novelty decay
7. Mental model redesign

---

## 03. COMPETITIVE WHITE SPACE

What competitors do well (RIKO does NOT compete here):

| Competitor | Their Strength | RIKO's Stance |
|---|---|---|
| Poweramp | Audio engine depth | Respect it. Don't replicate. |
| Spotify | Catalog + discovery | Offline-first product. No catalog war. |
| Apple Music | Ecosystem integration | Independent. No ecosystem. |
| YouTube Music | Video + live | Audio-first. Always. |
| Musicolet | 20 queues + file management | File management. Not RIKO's identity. |

What NO competitor does (RIKO's space):

| Territory | Gap | RIKO's Answer |
|---|---|---|
| **Music visualization** | Functional but ugly (Poweramp) or basic (Spotify) | Living Timeline — beautiful + functional |
| **Sleep experience** | Timer only (all competitors) | Wind-Down Instrument — environmental, not just time |
| **Emotional atmosphere** | Static dark mode (everyone) | Aurora Environment — music-responsive, state-aware |
| **Physical interaction** | Abstract gestures (everyone) | RIKO Weight Model — objects feel like what they are |
| **Temporal awareness** | Clock only (everyone) | Music visualizes time's passage |
| **Object intelligence** | Static UI elements (everyone) | Every object has state, physicality, memory |

---

## 04. RIKO EXPERIENCE STACK

Six layers. Bottom to top. Each layer depends on the one below.

```
┌─────────────────────────────────┐
│  6. EMOTIONAL                   │  ← how user FEELS
│     Moments · Signatures ·      │
│     Journey Memory              │
├─────────────────────────────────┤
│  5. SIGNATURES                  │  ← what makes RIKO, RIKO
│     Timeline · Wind-Down ·      │
│     Aurora · Song Change        │
├─────────────────────────────────┤
│  4. ATMOSPHERE                  │  ← environment + motion + sound
│     Aurora · Motion System ·    │
│     Haptic Grammar              │
├─────────────────────────────────┤
│  3. INTELLIGENCE                │  ← context + state + physicality
│     Object Intelligence Layer · │
│     State Matrix · Physicality  │
├─────────────────────────────────┤
│  2. COMPONENTS                  │  ← what things look like
│     Glass · GlassCard · Ripple  │
│     SongListItem · SectionHeader│
├─────────────────────────────────┤
│  1. FOUNDATION                  │  ← material + tokens + physics
│     GlassTokens · Color · Light │
│     Typography · RikoMotion     │
└─────────────────────────────────┘
```

Design at layer 1 is NOT done until layers 2-6 are considered.
Design at layer 6 is NOT possible without layers 1-5.
**v3.3 adds:** Layer 3 now includes Object Intelligence Layer — the contract that governs how every object behaves.

---

## 05. EMOTIONAL DESIGN MAP

Per-experience emotional targets:

| Experience | Before | During | After |
|---|---|---|---|
| **Open App** | "I want music" | Visual trust, calm | "This feels right" |
| **Find Song** | "What do I want?" | Focused browsing | "That was easy" |
| **Play Song** | "Let's go" | Music visible (timeline) | "I see the music" |
| **Seek** | "I want a specific part" | Scrub pleasure | "I controlled that" |
| **Song Change** | "Next!" | Temporal ritual | "Smooth transition" |
| **Timer** | "I'm going to sleep" | Wind-down begins | "The room faded" |
| **Queue** | "What's next?" | Journey clarity | "I know where I'm going" |
| **Error** | "Oh no" | Explained + auto-recovered | "App handled it" |

---

# PART II — INTELLIGENCE

---

## 06. EXPERIENCE GOVERNANCE

### Why This Exists

Future contributors (designers, developers, AI) must evaluate every new UI/interaction against RIKO's DNA — not just "does it look good?" but "does it belong?"

### The 15-Question Governance Gate

**Before any new UI/interaction ships, answer all 15:**

| # | Question | Criterion |
|---|---|---|
| 1 | **Purpose** | What problem does this solve? |
| 2 | **User Value** | Does the user *need* this, or is it decoration? |
| 3 | **Context** | Where in the journey does this live? |
| 4 | **State** | How does it behave in all states (empty, loading, error, normal)? |
| 5 | **Emotion** | What should the user feel? |
| 6 | **Motion** | What motion communicates its state? |
| 7 | **Atmosphere** | How does Aurora/environment respond? |
| 8 | **Performance** | What's the frame budget impact? |
| 9 | **Accessibility** | Does it work for everyone? |
| 10 | **Distinction** | Is this uniquely RIKO, or a commodity pattern? |
| 11 | **Object Identity** | What kind of object is this? |
| 12 | **Physical Metaphor** | Why does it behave this way? |
| 13 | **Cross-Surface Continuity** | How does identity survive on other surfaces? |
| 14 | **State Continuity** | Is state change visually continuous? |
| 15 | **Failure/Recovery** | Is the broken state also designed? |

### Verdict

**KEEP** — Ships as-is.
**MODIFY** — Adjust per governance feedback.
**EXPLORE** — Needs design exploration before commitment.
**REJECT** — Doesn't belong in RIKO. Document why.

---

## 07. RIKO EXPERIENCE ARCHITECTURE

### The 4 Core RIKO Signatures (MAXIMUM)

| # | Signature | What | Why Tier-S | Emotional Territory |
|---|---|---|---|---|
| 1 | **Living Timeline** | Waveform visualization | Unmatched globally | MUSIC → visible |
| 2 | **Wind-Down Timer** | Sleep instrument | No competitor has this | TIME → felt |
| 3 | **Aurora Environment** | Music-responsive atmosphere | Unique visual identity | MUSIC → environment |
| 4 | **Song-Change Ritual** | Temporal transition choreography | Replaces "cold cut" | MUSIC → environment |

### System Signature (not user-facing feature)

| System | What | Role |
|---|---|---|
| **RIKO Physicality Grammar** | Weight-based feedback | Infrastructure — felt everywhere, never perceived as a "feature" |

### Tier System

| Tier | Count | Description |
|---|---|---|
| **S — Signature** | 4 | Defines RIKO's identity. Maximum design investment. User-facing. |
| **System** | 1+ | Infrastructure signatures. Felt, not seen. |
| **A — Supporting** | 5–8 | Important. Well-designed. But not signature. |
| **B — Commodity** | Unlimited | Standard patterns. Function-first. Minimal animation. |

---

## 08. RIKO MOMENT vs RIKO SIGNATURE

### Distinction

| | RIKO Moment | RIKO Signature |
|---|---|---|
| **What** | A specific surprising instant | A repeatable system-level design pattern |
| **Duration** | 1–5 seconds | Ongoing |
| **Discoverable** | First use or rare occasion | Every use |
| **Example** | Wind-down ending → environment fades | Wind-down's temporal transition language |
| **Risk** | Novelty dies if not backed by utility | Must remain functional at zero animation |

---

## 09. ATTENTION BUDGET

### Rule: One Primary Attention Anchor Per Screen

Each screen has exactly ONE element that commands primary attention. Everything else is supporting or ambient.

**But: Attention Budget ≠ Animation Count.** Supporting motion can accompany the primary anchor.

### Budgets Per Screen

| Screen | Primary Anchor | Supporting | Ambient | Static |
|---|---|---|---|---|
| **Now Playing** | Timeline (Aurora-bounce) | Title slide, play button | Glow | Controls |
| **Mini Player** | — (context only) | Timeline hairline | — | Everything else |
| **Home Feed** | Section headers | Aurora stagger | — | Cards, text |
| **Queue** | Current track | Drag feedback | Aurora edge on next | Rows |
| **Search** | Results | — | — | Keyboard |
| **Sleep Timer** | Dial | Ambient fade | Background | Numbers |

---

## 10. STATE MATRIX SYSTEM

Formal state machine for every dynamic component.

### Mini Player State Matrix

| State | Artwork | Hairline | Shadow | Haptic |
|---|---|---|---|---|
| Playing | Breathing | Moving | Dynamic glow | — |
| Paused | Still | Still | Static | — |
| Error | Tinted | Red | Red | Error buzz |
| Loading | Dim | Indeterminate | None | — |

### Timeline State Matrix

| State | Bars | Glow | Scrub |
|---|---|---|---|
| Playing | Animated | Active | Seekable |
| Paused | Static | Dim | Seekable |
| Transitioning | Crossfade | Fade out/in | Disabled |
| Error | Monotone | None | Disabled |

### Wind-Down State Matrix

| State | Dial | Ambient | Volume | End |
|---|---|---|---|---|
| Idle | Shows presets | Normal | Normal | — |
| Active | Burning down | Fading | Fading | RIKO Moment |
| Extended | Reset burn | Restore | Restore | — |
| Cancelled | Snap back | Instant restore | Instant restore | — |

---

## 11. PHYSICALITY SYSTEM

### RIKO Weight Model

| Weight Class | Examples | Spring | Settle | Haptic |
|---|---|---|---|---|
| **Light** | Toggle, icon, chip, pill | Fast (200ms) | Immediate | Click |
| **Medium** | Sheet, button cluster, mini player | Standard (350ms) | Gentle wobble | Thud |
| **Heavy** | Hero artwork, full-screen panel, timeline | Slow (500ms) | Settle bounce | Deep thud |

### Interaction States

| Input | Response |
|---|---|
| **Rest** | Default state, minimal animation |
| **Press** | Scale down (0.97), background darken |
| **Drag** | Scale up (1.02), shadow lift, axis-lock |
| **Stretch** | Elastic resistance (overscroll) |
| **Release** | Return to rest with mass-based spring |
| **Settle** | Final position, haptic confirmation |
| **Interrupt** | Capture velocity, apply to new target |

---

## 12. FAMILIAR vs INNOVATIVE

| Quadrant | Approach | Example |
|---|---|---|
| **Familiar** | Standard pattern, works | Bottom sheet, toggle, text input |
| **Familiar + Improved** | Standard + subtle RIKO polish | Glass bottom sheet (specular edge) |
| **Innovative + Approachable** | New concept, intuitive | Living Timeline, Wind-Down dial |
| **Innovative + Demanding** | New concept, needs learning | Full aurora system |

---

## 13. DISCOVERABILITY vs SURPRISE

### Progressive Delight

| Level | Trigger | Response |
|---|---|---|
| **Simple** | First use | Works, makes sense |
| **Responsive** | Repeated use | Adapts to state, feels alive |
| **Smart** | Extended use | Context-aware responses |
| **Wow** | Rare occasion | Creates a memorable moment |

> **Smart = context-aware, not personalized.** Mini Player knows playback paused. Timer knows remaining time. Queue knows current/next. These are state-aware responses — not AI personalization.

---

# PART III — OBJECT INTELLIGENCE LAYER

> **v3.3's defining contribution.** This layer sits between Foundation/Components and Signatures — it governs how every RIKO object thinks, behaves, and communicates.

---

## 14. OBJECT INTELLIGENCE CONTRACT

### The 12-Dimension Specification

Every major RIKO object is specified across these 13 dimensions:

| Dimension | Question |
|---|---|
| **Identity** | What IS this object? |
| **Representation** | What truth/information does it represent? |
| **Mental Model** | How does the user think this object works? |
| **States** | What states does it have? |
| **Physicality** | How does it behave when touched? |
| **Motion** | Why, when, and how much does it move? |
| **Audio Relationship** | How does it relate to music/audio? |
| **Environment Relationship** | How does it connect to Aurora/artwork/environment? |
| **Haptic Relationship** | What does it feel like in the hand? |
| **Compression** | How does it survive on mini/notification/system surfaces? |
| **Accessibility** | How is the same state communicated without eyes? |
| **Performance** | How does it degrade across device tiers? |
| **Continuity / Memory** | What does this object remember across interaction, surfaces, and transitions? |

> **Continuity/Memory is NOT AI memory.** It's: previous position, previous state, object origin, interaction history during current gesture, transition source, previous surface. Mini → Now Playing uses this: same object's previous spatial state → expanded.

### The Rule

> **No "cool UI idea" enters production without this contract.**

---

## 15. OBJECT HIERARCHY

### Tier S — RIKO Signatures

- Living Timeline
- Wind-Down Temporal Object
- Aurora Environment
- Song-Change Ritual

### Tier A — Signature-Supporting Intelligent Objects

- RikoMusicObject (Mini Player)
- Playback State Object
- RikoWorld Artwork
- Physicality Grammar
- Contextual Sheets
- Adaptive Queue
- Intent Search
- Recovery States

### Tier B — Polished Commodity Objects

- Tabs
- Checkboxes
- Generic toggles
- Basic dialogs
- Standard buttons
- Ordinary lists

> **Goal:** Don't make Tier B ugly. Don't try to make Tier B into Tier S.
>
> **Tier B rule:** Use platform conventions exceptionally well. A checkbox can stay a checkbox. No "RIKO Checkbox™" needed.

### CRITICAL: Object Intelligence Is a Reasoning Framework, Not a Mandatory Class Hierarchy

> **Not every object needs its own abstraction, state machine, renderer, or contract implementation in code.** Use the minimum architecture required to preserve the behavioral model.
>
> If a simple play button needs 14 interfaces, that's not RIKO — that's an architecture museum.

---

## 16. OBJECT ARCHITECTURE PATTERN

Every RIKO object follows this architecture:

```
ObjectState + ObjectIntent + Context/Capability
                    ↓
            ObjectBehavior
                    ↓
      ┌─────────────┼─────────────┐
      │             │             │
Visual Expression  Haptic Expr  Semantic Expr
```

**State + Intent + Context = Behavior.** Not sequential. Parallel inputs.

**Never:** UI animation → somehow derive state.

### Example: Timeline Architecture

```
PlaybackPosition + AudioStructure + SeekIntent + InteractionState + DeviceTier + UserMotionPreference
        ↓
LivingTimelineState
        ↓
Visual Renderer
        ↓
Haptic Mapper
        ↓
Semantics Mapper
```

### Example: Timer Architecture

```
TimerIntent + Duration/EndOfAlbum/EndOfQueue + CurrentPlaybackState + RemainingTime + UserMotionPreference + DeviceTier
        ↓
TemporalObjectState
        ↓
Arc Geometry + Atmospheric State + Haptic Events + Accessibility State
```
Accessibility State
```

### Example: RikoMusicObject

```
PlaybackSnapshot + CurrentTrack + PlaybackPosition + QueueContext + SurfaceDensity + InteractionState
        ↓
RikoMusicObjectState
        ↓
Mini / Expanded / Full / Remote
```

---

## 17. COMPRESSION GRAMMAR

> **One object, multiple densities.**

```
Now Playing
    ↓
Mini Player
    ↓
Notification
    ↓
Lock Screen
```

Data hierarchy changes, conceptual identity doesn't.

| Surface | What Survives | What Compresses |
|---|---|---|
| **Now Playing** | Full expression | — |
| **Mini Player** | Artwork + title + state + timeline hairline | Controls → essential |
| **Notification** | Artwork + title + state + 3 actions | Timeline → none |
| **Lock Screen** | Artwork + title + basic controls | Everything else |

---

## 18. SEMANTIC TWIN

Every expressive object has TWO representations:

### Visual Twin
What the eyes see.

### Semantic Twin
What TalkBack/assistive technology understands.

| Visual | Semantic |
|---|---|
| `◯ 03:21` | "Sleep timer, 3 minutes 21 seconds remaining" |
| `BUFFERING` | "Buffering. Playback is waiting to resume" |
| Waveform bars moving | "Song playing, 47% complete" |
| Aurora shifting color | "Track changed. Now playing: Midnight Vibes" |

> **Mandatory rule:** Every Tier S and Tier A object must have both twins defined.

---

## 19. BORING OBJECT HUNT

Research only on these commodity objects:

| Object | Question |
|---|---|
| `slider` | What assumption is everybody making? |
| `checkbox` | What assumption is everybody making? |
| `toggle` | What assumption is everybody making? |
| `list row` | What assumption is everybody making? |
| `progress bar` | What assumption is everybody making? |
| `search field` | What assumption is everybody making? |
| `loading indicator` | What assumption is everybody making? |
| `empty state` | What assumption is everybody making? |
| `error` | What assumption is everybody making? |
| `dialog` | What assumption is everybody making? |
| `notification` | What assumption is everybody making? |

### Classification

**KEEP** — Necessary familiarity. Preserve.
**ADAPT** — Lazy convention. Rethink with RIKO principles.
**REINVENT** — Invisible complexity. Transform.
**REJECT** — Doesn't need RIKO treatment.

---

# PART IV — SIGNATURES (Reinterpreted Through Object Intelligence)

---

## 20. LIVING TIMELINE 2.0

### Three Layers

| Layer | What | Purpose | Type |
|---|---|---|---|
| **Layer A — Truth** | Actual audio structure/amplitude/time | Technical accuracy | Audio-Representative |
| **Layer B — Expression** | RIKO visual treatment (color, glow, playback bounce) | Artistic identity | Audio-Reactive |
| **Layer C — Interaction** | User drag/seek physical response | Control feel | Physicality |

> **Waveform is not made to look beautiful. It is a truthful representation of audio structure.**

> **Expression may decorate truth, never rewrite truth.**

### Architecture

```
PlaybackPosition
AudioStructure
SeekIntent
InteractionState
DeviceTier
UserMotionPreference
        ↓
LivingTimelineState
        ↓
Visual Renderer (bars + glow + energy)
        ↓
Haptic Mapper (5-second ticks, boundary resistance)
        ↓
Semantics Mapper ("Song playing, 47% complete")
```

### Seek Interaction

| Gesture | Response |
|---|---|
| Slow drag | Precision mode |
| Fast drag | Travel mode |
| 5-second crossing | Subtle tick haptic |
| Important boundary | Slightly stronger feedback |
| Release | Magnetic settle |

### Performance Pipeline

```
audio decode / preprocessing
    ↓
normalized waveform cache
    ↓
immutable timeline model
    ↓
GPU/Canvas draw
```

**UI frame must never do raw audio parsing.** Preprocess → cache → render.

---

## 21. WIND-DOWN TEMPORAL OBJECT

### Time IS the Object

| Remaining | Representation |
|---|---|
| `45m` | Full temporal mass |
| `30m` | Reduced |
| `15m` | More compact |
| `5m` | Quiet |
| `1m` | Concentrated |
| `0` | Dissolve/settle |

**Structure changes as time disappears.** Not flashy seconds animation.

### Timer Intent Types

| Intent | Behavior |
|---|---|
| `30 minutes` | Fixed duration countdown |
| `End of Album` | Tracks current album, ends after last track |
| `End of Queue` | Tracks queue, ends after last track |

These are different intent types. Smart — but no artificial AI needed.

### Two Paths

| Path | Interaction | User |
|---|---|---|
| **Fast path** | 15m / 30m / 45m / 60m / End of Album / End of Queue — tap → done | "I just want sleep timer" |
| **Deep path** | RikoArc — drag arc → duration → preview → extend | "I want control" |

### Architecture

```
TimerIntent
Duration / EndOfAlbum / EndOfQueue
CurrentPlaybackState
RemainingTime
UserMotionPreference
DeviceTier
        ↓
TemporalObjectState
        ↓
Arc Geometry
        ↓
Atmospheric State
        ↓
Haptic Events
        ↓
Accessibility State
```

---

## 22. AURORA ENVIRONMENT 2.0

### NOT "animated background"

### RIKO Model: Aurora is the environment's response to playback state

| Input | Aurora Role |
|---|---|
| Artwork | Identity/color source |
| Playback | Vitality |
| Pause | Suspension |
| Seeking | Displacement |
| Track change | Transition |
| Buffering | Restrained motion |
| Idle | Fade toward quiet |

### Formal Mapping

```
Artwork Identity × Playback State × Transition State
        ↓
AuroraState
        ↓
Renderer (independently optimized per tier)
```

### Performance Rule

Only one full-screen expensive atmospheric layer. Multi-layer blur + particle + shader + artwork scaling — never together.

| Tier | Aurora Expression |
|---|---|
| Core | Gradient + low-frequency motion |
| Enhanced | Richer field |
| Full Expression | Advanced shader/reactivity |

---

## 23. SONG-CHANGE RITUAL

### Choreography (ONE cohesive motion)

1. Aurora color shift (old → new)
2. Artwork identity morph
3. Title crossfade
4. Timeline reset

**These are ONE motion, not four.**

### Motion Meaning

| Phase | Motion Type |
|---|---|
| Aurora shift | Environmental transition |
| Artwork morph | Identity transfer |
| Title crossfade | Information update |
| Timeline reset | Temporal boundary |

---

## 24. PHYSICALITY GRAMMAR (System Signature)

### Gesture Object Model

| Input | Response |
|---|---|
| `Tap` | Activate |
| `Hold` | Engage |
| `Drag` | Manipulate |
| `Fling` | Transfer momentum |
| `Release` | Settle |
| `Boundary` | Resist |
| `Cancel` | Return |

### Consequence Preview

| Risk Level | Behavior |
|---|---|
| Destructive/high-consequence | `preview → commit` |
| Low-risk | `direct manipulation` |

Example: Seek → immediate continuous response. Track change → stronger intent threshold. Delete → never accidental.

---

# PART V — CORE EXPERIENCES (Reinterpreted)

---

## 25. RIKO MUSIC OBJECT

### Not "small Now Playing"

A persistent music object that exists across surfaces.

### States

| State | Surface | Expression |
|---|---|---|
| `Dock` | Home screen | Mini player — compressed identity |
| `Expanded` | Tap-to-expand | Artwork grows, controls reveal |
| `Focused` | Now Playing full screen | Full expression |
| `Compressed` | Notification/Lock screen | Remote object |

### Mini → Now Playing

**Screen doesn't change. Object unfolds.**

- Artwork grows from mini position (shared element)
- Title moves from spatial origin
- Timeline hairline → Living Timeline
- Controls expand
- Aurora gains complexity

```
PlaybackSnapshot
CurrentTrack
PlaybackPosition
QueueContext
SurfaceDensity
InteractionState
        ↓
RikoMusicObjectState
        ↓
Mini / Expanded / Full / Remote
```

---

## 26. RIKOWORLD ARTWORK

### 4-Layer Object

| Layer | What |
|---|---|
| **Identity** | Actual album cover |
| **Atmosphere** | Artwork-derived Aurora |
| **Physicality** | Subtle depth/press/breathing |
| **Transition** | Song change identity morph |

### Gyroscope

**REJECT as default.** Utility low, battery/performance/accessibility cost high.
Optional Full Expression experiment only.

---

## 27. QUEUE = INTENT OBJECT

### Not a list. A listening plan.

| Layer | Visual Weight | Intent |
|---|---|---|
| **Current** | Strongest | Pinned + equalizer glyph |
| **Next** | Strong | Aurora edge highlight |
| **Upcoming** | Medium | Standard glass row |
| **Later** | Light | Slightly dimmed |

### Drag Interaction

```
grab → lift → reorder → magnetic settle
```

Current track visually "pinned". Next track subtle Aurora edge. Later items visually calm.

> **Queue = planning, not entertainment.** No over-animation.

---

## 28. NAVIGATION — SPATIAL ANCHOR

### Current mental model: `selected icon`

### RIKO model: `moving spatial anchor`

Aurora circle physically travels between tabs, then settles. Animation purpose = **location communication**, not beauty.

### Navigation Transitions

Library artwork: `small tile` → shared element → `large object`

> "I followed that thing to this destination."

### Back

Predictive back: `current object → reverse spatial path`

---

## 29. SEARCH = INTENT SURFACE

### Query → Intent → Result

| Intent | Detection | Result Structure |
|---|---|---|
| **Song** | Matches track title | Songs first |
| **Artist** | Matches artist name | Artist → songs |
| **Album** | Matches album title | Album → tracks |
| **Folder** | Matches folder path | Folder contents |
| **Playlist** | Matches playlist name | Playlist → tracks |

### Result Architecture

Not a flat dump.

```
Exact → strongest
Related → secondary
Context → supporting
```

### Search Landscape

Typing `a` → broad. `ar` → narrower. `around` → narrower. `around the world` → exact.

**Not animation-driven. Ranking/grouping-driven.**

### Empty State

Not "No results." Instead: **"No exact match"** + available useful context. Never fake results.

---

## 30. CONTEXT-ANCHORED BOTTOM SHEET

### Not "bottom popup"

**Source-aware surface.** User taps `⋮` on song row. Sheet's identity is visually connected to the selected artwork/title. Dismiss → selected object returns to original state.

### Semantic Heights

| Height | Purpose |
|---|---|
| **Peek** | Context only |
| **Work** | Common actions |
| **Focus** | Detailed interaction |

Height itself becomes communication.

### Sheet vs Dialog

| Surface | Metaphor | Use |
|---|---|---|
| **Sheet** | Spatial extension — continue context | Queue, Track actions, Timer, Sort/filter |
| **Dialog** | Decision boundary — stop briefly | Delete, Replace, Permission, Critical confirmation |

---

## 31. PLAYBACK STATE OBJECT

### Media3 State Model Foundation

`STATE_IDLE`, `STATE_BUFFERING`, `STATE_READY`, `STATE_ENDED` + `playWhenReady` + `isPlaying`

### RIKO States

| State | Environment | UI |
|---|---|---|
| **Playing** | Alive | Full expression |
| **Paused** | Settles | Static |
| **Buffering** | Held | Held motion (not spinner) |
| **Ended** | Temporal boundary | Quiet |
| **Error** | Interrupted journey | Recovery object |

### Buffering: No Spinner

**Held Motion** — audio temporarily unavailable.

- Play intention remains visible
- Timeline holds
- Aurora slows/restrains
- Artwork doesn't look dead
- Controls remain responsive

> "System is waiting." Not "App froze."

---

## 32. ERROR = RECOVERY OBJECT

### Not error message. Recovery state.

```
Detect → Explain → Recover → Continue
```

### Recovery Policy

> **Recovery must be deterministic, bounded, and context-appropriate.**

| Rule | Meaning |
|---|---|
| Known failure → known recovery | Don't invent recovery paths |
| Bounded attempts | Max 1-2 retries, then safe fallback |
| Safe fallback | Skip track, show message, continue queue |
| No infinite retry | Never retry forever |
| No "smart recovery" | Don't over-engineer because we can |

Example: Deleted track → file unavailable → mark stale → remove or auto-skip → continue queue. No rescan. No background recovery monster.

**Error screen is never the endpoint.** Every error has a designed exit.

---

## 33. EDGE-STATE EXPERIENCE SYSTEM

| State | Designed Behavior |
|---|---|
| **0 songs** | "RIKO waits for your music." + Scan Music CTA |
| **1 song** | Valid universe. Layout doesn't break. |
| **50K songs** | Adaptive grouping + fast navigation. |
| **Missing artwork** | Deterministic placeholder from metadata. |
| **Broken metadata** | Fallback: embedded → filename → folder → safe fallback |
| **Deleted file** | Recovery state, not error text. |
| **Huge title** | Layout compression / controlled marquee |
| **Huge queue** | Semantic hierarchy survives |
| **Offline** | Native state. No useless warnings everywhere. |
| **Buffering** | Held motion (§31) |

---

# PART VI — SYSTEMS

---

## 34. MOTION SYSTEM

### Motion Has Meaning

| Motion Type | What It Communicates |
|---|---|
| **Transform** | Object changes form |
| **Travel** | Object moves between places |
| **Compression** | Object becomes denser |
| **Expansion** | Object reveals itself |
| **Resistance** | Object hits a boundary |
| **Settle** | Object returns to stable state |
| **Transfer** | Object carries identity from one surface to another |

### New Motion Rule

> **Never animate only the pixels that changed. Animate the relationship that changed.**

Song change: NOT "old text fade out → new text fade in." BUT "environment shifts + artwork identity transforms + title crossfades + timeline resets" — relationship-level choreography.

### M3 Expressive Alignment

| Property | Spring |
|---|---|
| `DefaultSpatialTransition` | `spring(dampingRatio = 0.72, stiffness = 260)` |
| `DefaultSpatialStrict` | `tween(200, easing = FastOutSlowIn)` |
| `FastSpatial` | `spring(dampingRatio = 0.8, stiffness = 400)` |
| `SlowSpatial` | `spring(dampingRatio = 0.65, stiffness = 180)` |

### Motion Hierarchy Degradation (Accessibility)

| Level | Description |
|---|---|
| **Full motion** | All animations active |
| **Reduced motion** | Replace spatial transforms with fades (150-200ms) |
| **Essential only** | Only loading indicators and state-change feedback |
| **Static** | No animation (rare, extreme) |

---

## 35. HAPTIC SYSTEM

### New Haptic Rule

> **Haptic must confirm physical meaning.** Not "all buttons vibrate." But: button press → crisp contact. Slider snap → tick. Boundary → resistance. Queue reorder → lift/settle.

### Riko Material Grammar

| Physical Event | Haptic Meaning |
|---|---|
| Contact | light |
| Press | crisp |
| Snap | tick |
| Boundary | resistance |
| Confirmation | clear |
| Settle | soft |
| Error | restrained |
| Long continuous drag | very sparse |

### Haptic Semantic Priority

| Priority | Type | Strength |
|---|---|---|
| 1 | Confirmation | Strongest |
| 2 | Boundary | Medium |
| 3 | Navigation hint | Light |
| 4 | Ambient | None |

### Device Tiers

| Tier | Haptic |
|---|---|
| Core | Platform standard feedback |
| Enhanced | Better supported haptic effects |
| Full Expression | Rich/custom tactile compositions where capability exists |

---

## 36. AURORA 2.0

See §22 for full specification.

---

## 37. DESIGN SYSTEM

### Glass Surface

- 26dp corner radius
- Specular edge: linear gradient 0°→90° (0.04→0.0)
- Shadow: 0dp/4dp/12dp for List/Dialog/Hero
- Blur: Now Playing artwork only (blur enable only)

### Blur Policy (v3.3 strict)

> **Blur is a material operation, not a default decoration.**

| Surface | Blur Allowed |
|---|---|
| Now Playing artwork | Yes |
| Mini Player | Mostly no |
| Library | No expensive blur |
| Notification | No |
| Queue | No |
| Sheet | Glass material but avoid expensive full-screen blur |
| Low-end tier | Simplify/remove blur before removing functionality |

---

## 38. ADAPTIVE LAYOUT / DEVICE TIERS

### Three-Tier System

| Tier | Name | Expression |
|---|---|---|
| **RIKO Core** | Essential | All features, reduced expression |
| **RIKO Enhanced** | Balanced | Mid-range+, more expression |
| **RIKO Full Expression** | Premium | Maximum visual richness |

### Smart Density

| Library Size | Strategy |
|---|---|
| 1 song | Hero-like universe |
| 500 songs | Regular library |
| 50,000 songs | Adaptive grouping + fast navigation |

Features don't disappear. Expression complexity reduces.

---

## 39. ACCESSIBILITY SYSTEM

### Primary Acceptance Criteria (Android-Native)

| Criterion | Target |
|---|---|
| Touch target | 48×48 dp |
| TalkBack labels | State-aware |
| Merged semantics | `Modifier.semantics(mergeDescendants = true)` |
| Dynamic announcements | "Now playing: [Track] by [Artist]" |
| Roles | `Role.Button`, `Role.Switch` |
| Font scale | Works at 130%+ |
| System reduced motion | Respected |

### Semantic Calm

| Visual Event | TalkBack Behavior |
|---|---|
| Aurora color changes | No announcement |
| Timeline playback progress | No constant announcement |
| Song change | Meaningful announcement |
| Timer set | Confirmation |
| Playback error | Announcement with recovery info |

### Three Expression Modes

| Mode | Description |
|---|---|
| **Full Expression** | Full motion + rich haptic + atmosphere |
| **Calm** | Reduced motion, simplified physics |
| **Assistive** | Information-first visual + semantic clarity |

> **Feature disappears NEVERT. Expression complexity reduces.**

---

## 40. PERFORMANCE ARCHITECTURE

### Four Performance Dimensions

| Dimension | Metric |
|---|---|
| Frame time | p95 ms per refresh class |
| Frame pacing | Jank rate |
| Input latency | Touch → visual response |
| Startup latency | See Startup Metrics below |

### Startup Metrics (not just one number)

| Metric | What | Target |
|---|---|---|
| **TTFF** | Time To First Frame | < 200ms |
| **TTMUI** | Time To Meaningful UI | < 400ms |
| **TTFP** | Time To First Playback-ready | < 800ms |

> p50 and p95 measured separately. "Startup < 400ms" is NOT an excuse to remove necessary initialization.

### Frame-Budget Compliance

| Device | Refresh Rate | Frame Budget |
|---|---|---|
| Budget | 60 Hz | ~16.7ms |
| Mid-range | 90 Hz | ~11.1ms |
| Flagship | 120 Hz | ~8.3ms |

### Jank Classification

| Type | Duration | Priority |
|---|---|---|
| Micro-jank | 1–2 frames over budget | Low |
| Jank | 3–5 frames over budget | High |
| Freeze | > 5 frames over budget | Critical |

### Three-Gate Device Tier Detection

| Gate | Input | Purpose |
|---|---|---|
| Static Capability | GPU, API, RAM, refresh rate | Hardware ceiling |
| Runtime Performance | Observed frame/jank data | Actual performance |
| User/System Preference | Reduced motion, battery saver | Override |

**Final expression tier = min(Static, Runtime, Preference)**

**With hysteresis:**
- Detect degradation → observe over several samples → downgrade only after sustained failure
- Don't immediately upgrade when performance recovers → reevaluate later
- Tier changes within a session should be rare
- Avoids oscillation: Full → GC → Enhanced → stable → Full → GC → ...

### Performance Architecture

```
UI thread:     state + composition + drawing ONLY
Background:    metadata, waveform, artwork, audio features, database
Cache:         artwork colors, waveform, song structure, Aurora swatch
Renderer:      no allocation per frame, no bitmap recreation, no heavy blur stack,
               no database query during animation, no audio analysis inside Composable
```

### Critical Rule

> **No signature is allowed to make interaction slower.**
> `visual wow < input responsiveness` — always.

---

# PART VII — VALIDATION

---

## 41. CONCEPT VALIDATION

| Test | Pass Criteria |
|---|---|
| **5-second test** | User understands primary purpose |
| **30-second test** | Completes primary action without help |
| **5-minute test** | Remembers the experience next day |
| **1-week test** | Returns to feature organically |

### Explainability Test

Show feature without explanation. Ask: "এটা কী?" If user misunderstands → redesign.

---

## 42. JOURNEY VALIDATION

| Journey | First-Use | Repeat-Use | Long-Term |
|---|---|---|---|
| A: Open→Find→Play | _/10 | _/10 | _/10 |
| B: Browse→Queue→Continue | _/10 | _/10 | _/10 |
| C: Play→Seek→Change | _/10 | _/10 | _/10 |
| D: Play→Timer→Sleep | _/10 | _/10 | _/10 |
| E: Pause→Return→Resume | _/10 | _/10 | _/10 |
| F: Error→Recovery | _/10 | _/10 | _/10 |

---

## 43. EXPERIENCE SCORE

### Experience Score (feature quality)

| Dimension | Weight |
|---|---|
| Usefulness | 15% |
| Intuitiveness | 15% |
| Differentiation | 10% |
| Emotional impact | 10% |
| Memorability | 10% |
| Visual quality | 10% |
| Accessibility | 10% |
| Performance | 10% |
| RIKO identity | 5% |
| **Cohesion** | **5%** |

**Threshold:** ≥ 7/10 to ship.

### Signature Potential Score (brand identity)

| Dimension |
|---|
| Distinctiveness |
| Memorability |
| Recognizability |
| Repeatability |
| **Ownability** |

> **Ownability test:** "If RIKO didn't exist, could another app claim this?" If seeing it makes people think "that's RIKO" → ownable.

---

## 44. DESIGN RED TEAM

| # | Question |
|---|---|
| 1 | "এই জিনিসটা কি সত্যিই useful?" |
| 2 | "এটা কি Apple/Spotify/Poweramp-এর effect clone?" |
| 3 | "User কি এটা বুঝবে?" |
| 4 | "৩ মাস পরে এটা annoying হবে?" |
| 5 | "Low-end phone-এ এটা কী করবে?" |
| 6 | "Reduce motion-এ কী থাকবে?" |
| 7 | "এই animation বন্ধ করলে product কি এখনও ভালো?" |
| 8 | "এটার জন্য code complexity worth it?" |
| 9 | "এই feature না থাকলে RIKO কি সত্যিই worse হবে?" |
| 10 | "একজন power user 100 বার ব্যবহার করার পর কি এখনও useful?" |

### Verdict: PASS / REWORK / KILL

---

## 45. v3.3 ACCEPTANCE CRITERIA

| # | Criterion | Test |
|---|---|---|
| 1 | **1 second test** | User glance understands state |
| 2 | **5 second test** | Interaction pattern understood |
| 3 | **30 second test** | Object pattern feels natural |
| 4 | **5 minute test** | Animation not irritating |
| 5 | **1 week test** | Novelty decay → utility remains |
| 6 | **Accessibility test** | Same meaning via TalkBack |
| 7 | **Low-end test** | Functionality intact |
| 8 | **Performance test** | Interaction never waits for effect |
| 9 | **"Why?" test** | User: "ও, সুন্দর" → "আরে, smart" → "কেন এমন?" → "হ্যাঁ, এটাই তো logically হওয়া উচিত" |

> **This last reaction is RIKO territory.**

---

# PART VIII — EXECUTION

---

## 46. PRIORITY MATRIX

> **Signature Tier (S/A/B) = architectural importance. Implementation Priority (P0-P3) = build order. These are different.**

| Item | Signature Tier | Priority | Impact | MUST? |
|---|---|---|---|---|
| Living Timeline 2.0 | **S** | **P0** | Breathtaking | **YES** |
| Wind-Down Temporal Object | **S** | **P0** | Emotional | **YES** |
| Aurora Environment 2.0 | **S** | **P1** | Large | YES |
| Song-Change Ritual | **S** | **P1** | Large | YES |
| RikoMusicObject (Mini→Full) | A | **P0** | Large | **YES** |
| Physicality Grammar | System | **P0** | Foundation | **YES** |
| Object Intelligence Layer | Architecture | **P0** | Foundation | **YES** |
| Adaptive Queue | A | P2 | Medium | No |
| Intent Search | A | P2 | Medium | No |
| Context-Anchored Sheets | A | P2 | Medium | No |
| Recovery States | A | P1 | Bad-day-Large | YES |
| RikoMaterialGrammar | System | P1 | Foundation | YES |

### Signature Tier Rules (AGGRESSIVE LOCK)

- **4 signatures MAXIMUM.** No exceptions.
- Object Intelligence does NOT create new signatures. It improves existing ones.
- Queue is NOT signature. Search is NOT signature. Artwork is NOT signature.
- If someone says "Queue-ও এত সুন্দর, এটাও signature!" → NO.4

---

## 47. IMPLEMENTATION ROADMAP

### Phase 1 — Object Foundation

Create shared foundation types:
- `ObjectState`
- `ObjectIntent`
- `InteractionState`
- `MotionState`
- `HapticEvent`
- `SemanticState`
- `DeviceExpressionTier`

**No UI yet.** Types only.

### Phase 2 — Physicality Grammar

Implement once, reuse everywhere:
- Press, Drag, Resistance, Snap, Settle, Cancel, Velocity
- Test in isolation
- Then apply to: Timeline, Queue, Sheet, Mini Player, Timer

### Phase 3 — RikoMusicObject

First prototype:
- Mini → Expanded → Full
- Artwork shared identity
- Title continuity
- Timeline continuity
- Control continuity
- No expensive effects first

Then polish.

### Phase 4 — Living Timeline 2.0

Audio preprocessing pipeline:
```
Media3 / file → waveform extraction → normalized cached model
→ timeline renderer → interaction state → haptic landmarks → accessibility
```

First: truth correct. Then: expression.

### Phase 5 — Riko Temporal Object

Prototype RikoArc only.
Then: duration mode, end-of-album, end-of-queue, remaining-time geometry, zero-state dissolution.
Then: accessibility + reduced motion.

### Phase 6 — Aurora 2.0

Rewrite visual background as architecture:
```
Artwork Identity × Playback State × Transition State → AuroraState
```

Renderer independently optimized. Now Playing ছাড়া heavy Aurora কোথাও default করা হবে না.

### Phase 7 — Gesture + Haptic

Same gesture physics across: Timeline, Queue, Sheet, Mini Player.
Then device capability mapping.

### Phase 8 — Edge State System

Test matrix:
`0 song · 1 song · 100 · 10K · 50K · missing artwork · broken metadata · deleted file · huge title · huge queue · offline · denied permission · buffering · error · landscape · large font`

**Behavior correctness > visual beauty** at this stage.

### Phase 9 — System Surfaces

MediaSession, Notification, Lock Screen, Widget — same conceptual object model.

### Phase 10 — Performance Pass

Measure: Input latency, Jank, Frame time, GPU/CPU load, Memory, Cold startup, Scroll stability, Animation stability.

Baseline Profile early.

### Phase 11 — Reduced Motion / Accessibility

Every expressive object: Full Expression / Calm / Assistive modes.

---

## 48. FINAL v3.3 OBJECT MAP

```
                         RIKO EXPERIENCE
                                │
                  ┌─────────────┴─────────────┐
                  │                           │
         SIGNATURE EXPERIENCES         OBJECT INTELLIGENCE
                  │                           │
     ┌────────────┼────────────┐       ┌──────┼──────┐
     │            │            │       │      │      │
 Living       Aurora       Wind-Down  State  Physical Context
 Timeline   Environment     Time      Model  Model   Model
     │            │            │       │      │      │
     └────────────┴────────────┴───────┴──────┴──────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                 Visual      Haptic     Semantic
                 Response    Response   Response
                    │           │           │
                    └───────────┼───────────┘
                                │
                       Cross-Surface
                         Continuity
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
      Now Playing          Mini Player          Notification
          │                     │                     │
          └─────────────── same object ───────────────┘
```

---

# PART IX — DESIGN DNA

---

## 49. RIKO DESIGN DNA

Seven sentences. Permanent north-star.

1. RIKO prefers meaningful motion over decorative motion.

2. RIKO makes music perceptible without turning the app into a visualizer.

3. RIKO uses atmosphere to support content, never replace it.

4. RIKO hides complexity until it becomes useful.

5. RIKO makes familiar actions feel considered, not unfamiliar for novelty.

6. RIKO treats time, playback, and space as visual materials.

7. RIKO never sacrifices responsiveness for spectacle.

---

## 50. RIKO NEVER

| # | Rule |
|---|---|
| 1 | RIKO never copies competitor visuals directly |
| 2 | RIKO never hides core controls behind gestures |
| 3 | RIKO never sacrifices responsiveness for animation |
| 4 | RIKO never uses motion without semantic purpose |
| 5 | RIKO never makes every screen equally expressive |
| 6 | RIKO never changes important information merely for novelty |
| 7 | RIKO never turns atmosphere into distraction |
| 8 | RIKO never requires learning for basic playback |
| 9 | RIKO never uses "AI" as decoration |
| 10 | RIKO never treats low-end devices as second-class users |
| 11 | RIKO never adds visual novelty without Object Intelligence Contract |
| 12 | RIKO never makes interaction slower for visual wow |

---

## 51. THE RIKO EXPERIENCE LOOP

```
Perceive    → Understand user context
    ↓
Interpret   → Read state + content + intent
    ↓
Respond     → Give immediate feedback
    ↓
Express     → Show meaningful response via motion + atmosphere
    ↓
Delight     → Where opportunity exists, create a RIKO Moment
    ↓
Learn       → Identify UX improvement from design validation (NOT user profiling)
    ↓
Simplify    → Cut what is unnecessary
    ↓
Repeat
```

> **Learn = product/experience learning, not user profiling.** Which interactions are confusing? Which states are unclear? Where do users repeatedly go back? Which controls are not discoverable? App runtime intelligence is local, state-aware, non-invasive. No AI-by-default.

---

## 52. THE LOCK PRINCIPLES

> **RIKO does not redesign interfaces by adding visual novelty.
> RIKO redesigns the mental model of familiar objects.**

> A slider should feel like control.
> A timeline should feel like a place.
> A timer should feel like disappearing time.
> A mini-player should feel like a persistent object.
> Artwork should feel like the seed of a world.
> Aurora should feel like the room responding.
> A gesture should feel like manipulating something.
> Haptics should feel like material feedback.
> A sheet should feel like a contextual surface.
> An error should feel like an interrupted journey, not a dead end.
> Search should feel like entering the music you are looking for.
> Edge states should feel intentional, not exceptional.

---

## 53. THE PERSON / FEEL / MEMORY / WHY / NORTH STAR

### The Person
RIKO-র ব্যক্তিত্ব: একজন চিন্তাশীল বন্ধু যে রাতে তোমার গান বাজায় — জোরে চিৎকার করে না, খেয়াল রাখে, ঘরটা তোমার সাথে অন্ধকার হয়।

### The Feel
- **দৃশ্য:** গাঢ় নীল-কালো মহাকাশ, আরোরা-র আভা, কাচের স্তর।
- **স্পর্শ:** প্রতিটা ছোঁয়ায় একটা উত্তর — scale, রঙ, কাঁপুনি বা haptic।
- **ছন্দ:** app গানের ছন্দে বাস করে।

### The Memory
- ৫ মিনিট: "সেই waveform-টা — গানটা দেখা যাচ্ছিল!"
- ১ সপ্তাহ: "টাইমার সেট করলে app-টা নিজেই অন্ধকার হয়ে যায়।"
- বন্ধুকে: "শুধু player না — এরা পুরো অভিজ্ঞতাটা নতুন করে ভেবেছে।"

### The Why
> RIKO ব্যবহার করলে মনে হয় — *এই product-টা আমার music শোনার experience-টাকে গুরুত্ব দিয়ে design করা হয়েছে।*

### The North Star
> RIKO should not make users notice the design.
> RIKO should make users notice the experience.

---

# PART X — RESEARCH APPENDIX

---

## 54. REFERENCE LIBRARY

### Audio / EQ Concepts

- [Oleh Symonik — Music iOS Player Equalizer](https://dribbble.com/shots/6813608) — Motion + equalizer + quick playback control
- [Bediter — Bass Booster App UI](https://dribbble.com/shots/27245879) — Real-time EQ, volume boost, bass-focused control
- [Md Tamim Hasan — Neumorphic Audio Controller](https://dribbble.com/shots/25923428) — Tactile/depth-based controls (physicality principle, not visual style)
- [DucManh — Music Equalizer App Design](https://dribbble.com/shots/22815578)
- [BOOZ Music Player](https://www.behance.net/gallery/67551631) — Dedicated icon/interaction module

### Mini Player / Player Surface

- [Alex Tatu — MusicPlayer: Pocket Nostalgia](https://dribbble.com/shots/26767318) — Mini-player/widget as compact tactile object
- [Leo Leung — Music mini player](https://dribbble.com/shots/2969122)
- [Vahid — Dynamic Island Music Player](https://dribbble.com/shots/22265719)
- [The Glyph — Minimalistic Player Widget](https://dribbble.com/shots/6862618)
- [Tk Vishal — Music Player Dock Microinteraction](https://dribbble.com/shots/16118910)

### Artwork / Environment

- [Ariel Christie — Apple Music UX Case Study](https://www.behance.net/gallery/127598211)
- [Andre Navarre — Gyroscope Music Cover](https://dribbble.com/shots/5789153)
- [Immersive Music Streaming — Dark UI Concept](https://dribbble.com/shots/27083239)
- [Hozier "All Things End" Audio-reactive Visualizer](https://www.behance.net/gallery/169953409)
- [Rotational Music App](https://www.behance.net/gallery/62169521)

### Track Actions / Sheet

- [Anastasy Gubanova — Music Player Track Actions](https://dribbble.com/shots/27044940)
- [Subash Chandra — Music Player Card Interaction](https://dribbble.com/shots/27582813)

### Platform References (Technical Authority)

- [Android Media3 Player Interface](https://developer.android.com/media/media3/session/player)
- [Android Media Controls](https://developer.android.com/media/implement/surfaces/mobile)
- [Jetpack Compose Shared Element Transitions](https://developer.android.com/develop/ui/compose/animation/shared-elements)
- [Jetpack Compose Predictive Back](https://developer.android.com/develop/ui/compose/system/predictive-back)
- [Android Haptics Design Principles](https://developer.android.com/develop/ui/views/haptics/haptics-principles)
- [Android Haptics API Reference](https://developer.android.com/develop/ui/views/haptics/haptics-apis)
- [Compose Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Compose Bottom Sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [Partial Bottom Sheet](https://developer.android.com/develop/ui/compose/components/bottom-sheets-partial)
- [Build for Billions — UI](https://developer.android.com/docs/quality-guidelines/build-for-billions/ui)
- [Baseline Profiles / Startup](https://developer.android.com/guide/topics/androidgo/optimize-startup)
- [Apple HIG — Searching](https://developer.apple.com/design/human-interface-guidelines/searching)
- [Apple HIG — Tab Bars](https://developer.apple.com/design/human-interface-guidelines/tab-bars)
- [Apple HIG — Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets)
- [Apple HIG — Playing Haptics](https://developer.apple.com/design/human-interface-guidelines/playing-haptics)
- [Apple HIG — Live Activities](https://developer.apple.com/design/human-interface-guidelines/live-activities)

> **Note:** Dribbble/Behance references are concept/design studies — visual inspiration only. Android/Apple documentation are platform constraints and authoritative technical references. RIKO architecture/performance decisions follow official platform behavior.

---

*END OF DOCUMENT — RIKO-EXPERIENCE-SYSTEM-003 v3.3 (OBJECT INTELLIGENCE)*

*v3.2 preserved the experience architecture. v3.3 defines how every object physically behaves.*

*পাশাপাশি পড়ো: SM-UI-SPEC-001 v2 (চেহারার সত্য) · Premium Smoothness & Performance Pass (গতির সত্য) · to plan about.md (উদ্দেশ্যের সত্য)*

*This document is complete. Future work = implementation, not expansion.*
