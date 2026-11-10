# RIKO UI/UX REBUILD — DESIGN LANGUAGE 2.0 (V3 PLAN)

> ⛔ **OBSOLETE — SUPERSEDED BY PLAN 1 (Premium Dark Design System)**
> এই light-first plan আর valid না। Direction pivot হয়েছে — নতুন approved
> direction: **dark-first, cinematic, purple/blue ambient**। বর্তমান active doc:
> **`docs/UIUX_PLAN1_DESIGN_SYSTEM.md`**। নিচের content শুধু history-র জন্য।

> **Status (old):** 🟢 ACTIVE PLAN (Product Director approved — "UI/UX fully rebuild")
> **Scope:** Presentation/Rendering layer full rebuild — light-first identity
> **Frozen layers:** Engine / Reducer / Runtime / Mapper / audiofx / service / data — UNTOUCHED 🔒
> **This doc:** নতুন design language-এর single source of plan। Rebuild শেষে PROJECT_STATUS.md sync হবে।

---

## 0. EXECUTIVE SUMMARY

RIKO Experience OS-এর codebase এখন ~120K lines, architecturally strong, কিন্তু
visual identity **dark-only**। Product Director-এর vision হলো **bright, light,
refined-glass, flagship-grade** experience। তাই UI/UX **fully rebuild** হবে।

Core principle:

```
হাড় থাকবে, চামড়া পুরো নতুন।
Skeleton (logic) 100% preserve — Skin (visual) 100% new.
```

এটা Rule #70 (Architecture Freeze)-এর সাথে compatible — কারণ Rendering/Compose
layer নতুন contract দিয়ে rebuild করা allowed। Engine/Reducer/Runtime/Mapper
একটা line-ও change হবে না।

---

## 1. কী থাকবে, কী যাবে

### ✅ PRESERVE (এগুলো ছুঁয়েও দেখা হবে না)

| Layer | কেন রাখা হচ্ছে |
|---|---|
| `audiofx/` (Wave Engine input) | Playback-এর heartbeat — battle-tested |
| `presentation/*/engine, runtime, reducer, mapper` | Frozen pipeline (Rule #70) |
| `service/` + `data/` | Playback + storage backbone |
| `visualos/runtime/` (State/Reducer/Mapper) | Visual OS-এর brain |
| Color Intelligence core + Wave Engine core | Compute logic same, output styling নতুন |

### 🏗️ REBUILD (user যা সত্যিই দেখে)

| Layer | Rebuild কী হবে |
|---|---|
| `core/designsystem/theme/` | Dark-only → **Light-first dual theme engine** |
| `core/designsystem/glass/` | নতুন light-refined glass language |
| `core/designsystem/components/` | WaveformTimeline v3, TransportControls v3, cards |
| `presentation/*/screen/ + rendering/` | Library, Now Playing, Mini Player, Search, Settings |
| `visualos/host/ + rendering/` | Scene renderer নতুন skin |
| `splash/` | TD-10 resolve — RIKO identity splash (logo morph + wave reveal) |

---

## 2. নতুন DESIGN DIRECTION — "RIKO LIGHT GLASS"

চারটা pillar অপরিবর্তিত থাকবে, নতুন light অবতারে:

1. **Glass** — light পটভূমিতে refined glass: subtle white translucency +
   hairline dark border + soft blue-gray shadow। ❌ artificial shine, ❌ white
   radial highlight, ❌ glossy bubble।
2. **Wave** — Waveform hero visual element থাকবে, light surface-এ soft
   complementary accent দিয়ে।
3. **Color Intelligence** — album art → palette pipeline same, কিন্তু output
   এখন light-surface-safe হবে (contrast নিশ্চিত করা accent শেড)।
4. **Living Motion** — RikoMotion system থাকবে, semantic animation।

**Identity Test (Rule #72):** logo ঢাকলেও ৫ সেকেন্ডে "এটা RIKO" — এখন
**light glass + living waveform** দিয়ে।

---

## 3. DESIGN TOKEN SPEC V3

### 3.1 Color System — Light-First

```kotlin
// ── Light Surfaces ──
val ColorMist       = Color(0xFFF6F8FC)  // app background (soft cool white)
val ColorPaper      = Color(0xFFFFFFFF)  // card / sheet base
val ColorPaperUp    = Color(0xFFFBFCFE)  // elevated surface
val ColorShade      = Color(0xFFE9EEF5)  // recessed / divider substrate

// ── Ink (text) ──
val InkPrimary      = Color(0xFF161B26)
val InkSecondary    = Color(0x99161B26)  // ~60%
val InkTertiary     = Color(0x61161B26)  // ~38%
val InkDisabled     = Color(0x33161B26)

// ── Soft Complementary Accent Pairs (Product Director spec) ──
val AccentCoral     = Color(0xFFFF8A5C)  // pairs with AccentPairSky
val AccentPairSky   = Color(0xFFBFE3FF)
val AccentPinkSoft  = Color(0xFFFF7BA9)  // pairs with AccentPairMint
val AccentPairMint  = Color(0xFFCFF5E7)
val AccentLavender  = Color(0xFFB9A6FF)  // pairs with AccentPairAzure
val AccentPairAzure = Color(0xFFCDEBFF)
```

**নিয়ম:**
- Light mode-এ accent সবসময় ink-on-light safe variant ব্যবহার করবে
  (text contrast ≥ 4.5:1, icon ≥ 3:1)।
- পুরানো dark token নাম (`ColorVoidDark`, `AccentCyan`...) transition period-এ
  **রাখা হবে** — additive migration, big-bang rename না (যেন 120K line
  codebase compile হতে থাকে)।
- Dark theme মরবে না — Settings-এ option থাকবে। Default identity = Light।

### 3.2 Glass Engine V3 — Light Tokens

`GlassStyle` data class অপরিবর্তিত (intent-based design ভালো)। নতুন token set:

| Token | Character |
|---|---|
| `GlassTokensLight.MiniPlayer` | white tint ~0.55, dark hairline border, soft shadow |
| `GlassTokensLight.BottomBar` | সবচেয়ে grounded — white tint ~0.70 |
| `GlassTokensLight.Dialog` | সবচেয়ে prominent — white tint ~0.80 |
| `GlassTokensLight.FloatingCard` | light presence, subtle shadow |
| `GlassTokensLight.Hero` | album-art real blur 65dp (concept same) |
| `GlassTokensLight.Overlay` | ink-based scrim |
| `GlassTokensLight.Chip / Tooltip` | light frosted surfaces |

Light glass optics rules:
- Border = **dark hairline** (~`0x14101620`) — light-এ white border invisible।
- Shadow = soft, downward offset, **blue-gray hue** — কখনো pure black না।
- Blur = real backdrop blur — R1-এ spike: Haze vs pure-Compose benchmark।

### 3.3 Typography

Scale ঠিক থাকবে। পরিবর্তন: text colors token-driven হবে (`LocalRikoColors`
composition local) — hardcoded `TextPrimary` dependency সরবে।

### 3.4 Dimensions

`RikoDimens` অপরিবর্তিত — scale already premium।

### 3.5 Motion

`RikoMotion` অপরিবর্তিত। নতুন: **MorphContinuitySpec** — Mini Player ↔
Library ↔ Now Playing morph-এর canonical spring preset (Animation Continuity
gate-এর জন্য)।

---

## 4. MILESTONE ROADMAP — R-SERIES

> Cadence: Subsystem Build → Review → Freeze → Next (Build Cadence Policy)
> প্রতিটা milestone শেষে: `./gradlew assembleDebug` + Production Gate checklist।

| # | Milestone | Scope | Files |
|---|---|---|---|
| **R0** | **Plan Freeze** | এই doc | ✅ done |
| **R1** | Theme Foundation | Dual light/dark scheme, `LocalRikoColors`, GlassTokensLight, blur spike, TD-1 compile verify | ~8–10 |
| **R2** | Core Components | Glass v3 components, WaveformTimeline v3, TransportControls v3 | ~12–15 |
| **R3** | Mini Player + Library | দুই screen-এর light rebuild | ~20–25 |
| **R4** | Now Playing | Hero album, OrganicTimeline, gestures — flagship | ~12–15 |
| **R5** | Search + Settings + Splash | Splash identity (TD-10), বাকি screens | ~10–12 |
| **R6** | Visual OS Reskin | Scene renderers, dock, handoff, overlay | ~10–12 |
| **R7** | Validation + Polish | 6 gates + Identity Test + device test | audit |

**Order logic:** Foundation → Components → Screens (bottom-up)। R1+R2 freeze
না হওয়া পর্যন্ত কোনো screen rebuild শুরু হবে না।

---

## 5. PRODUCTION GATES (প্রতি R-milestone)

- [ ] Compile: `./gradlew assembleDebug` pass
- [ ] কোনো frozen layer touch হয়নি (diff audit)
- [ ] Contrast: text ≥ 4.5:1, icon ≥ 3:1 (light surface-এ)
- [ ] Frame budget: waveform/glass-heavy screen <16ms @60Hz
- [ ] Dark theme regression pass
- [ ] Zero hardcoded color/dp/animation — সব token থেকে
- [ ] Low-end tier-এ blur/glow degrade করে (Rule #76)

---

## 6. KNOWN RISKS (R-series)

| ID | Risk | Mitigation |
|---|---|---|
| UR-1 | Light surface-এ album-art palette clash | Color Intelligence-এ light-safe shade mapping (R1) |
| UR-2 | Blur performance low-end-এ | R1 spike → Haze vs pure-Compose benchmark; tier-driven degrade |
| UR-3 | Token rename 120K line ভাঙবে | Additive-only migration, পুরানো নাম alias |
| UR-4 | Morph continuity নতুন skin-এ ভাঙবে | MorphContinuitySpec (R1) + R7-এ 100-iteration test |
| UR-5 | TD-1 compile unverified ঝুলে আছে | R1-এর প্রথম কাজ `assembleDebug` verify |

---

## 7. NEXT ACTION

1. **R1 শুরু** — Theme Foundation rebuild
2. R1-এর প্রথম ধাপ: `assembleDebug` verify (TD-1 clear)
3. প্রতিটা R-milestone freeze-এ PROJECT_STATUS.md + এই doc sync

---
© RIKO Experience OS — UI/UX Rebuild Plan (Design Language 2.0)

