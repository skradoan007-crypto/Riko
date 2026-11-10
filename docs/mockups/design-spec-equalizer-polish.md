# Equalizer + NowPlaying Subscreen Polish — Design Spec

**Date:** 2026-09-14
**Scope:** EqualizerSheet.kt rewrite, QueueHost.kt polish
**Reference:** Samsung/Poweramp-style equalizer (user-provided screenshot 3)
**Visual Mockup:** `docs/mockups/equalizer-redesign.html`

---

## 1. EqualizerSheet.kt — Full Rewrite

### 1.1 EQ Band Visualization
**Current:** 5-band curve with Canvas path drawing, gradient fill, glow nodes.
**Target:** 10-band vertical sliders with circular thumbs.

**Component: `EqBandsSection`**
- Column of 10 vertical slider tracks
- Each track: 3dp wide, 110dp tall, `rgba(255,255,255,0.06)` background
- Fill: linear gradient from `#00c8c8` (bottom) to `#008888` (top)
- Thumb: 14dp circle, `#00c8c8`, box-shadow glow
- Above thumb: gain value label (e.g. "+1.5") in `#00c8c8`, 10sp
- Below track: frequency label (e.g. "62", "1K", "16K") in `#5a5a70`, 9sp
- Gesture: per-band vertical drag (reuse existing `detectVerticalDragGestures` logic)
- Bands adapt to device band count (5-band MIUI, 10-band AOSP)

### 1.2 Preset Chips
**Current:** `LazyRow` of chips, selected = white fill, unselected = glass border.
**Target:** Keep `LazyRow`, refine styling.
- Selected: `rgba(0,200,200,0.15)` fill, `#00c8c8` text, `rgba(0,200,200,0.3)` border
- Unselected: `rgba(255,255,255,0.04)` fill, `#a0a0b8` text, `rgba(255,255,255,0.08)` border
- Keep delete icon for user presets

### 1.3 FX Cards — Circular Gauge (Bass Boost, Loudness, Virtualizer)
**Current:** `FxCard` with toggle + `GradientSlider` + value text.
**Target:** 3-column grid of circular gauge cards.

**Component: `FxGaugeCard`**
- Card: `rgba(255,255,255,0.03)` fill, `rgba(255,255,255,0.06)` border, 16dp radius
- Gauge: 56dp circle, `rgba(0,200,200,0.08)` fill, 2dp border
- Circular progress arc (border-top/right colored `#00c8c8`)
- Center value: percentage or dB (e.g. "62%", "12dB") in `#e8e8f0`, 14sp bold
- Label below gauge: effect name in `#a0a0b8`, 11sp
- Mini toggle at bottom: 32x18dp pill, `rgba(0,200,200,0.4)` when on

**Layout:**
```
┌──────────┬──────────┬──────────┐
│  (62%)   │  (12dB)  │  (45%)   │
│  Bass    │ Loudness │ Virtual  │
│  [ON]    │  [ON]    │  [OFF]   │
└──────────┴──────────┴──────────┘
```

### 1.4 Reverb & Channel Balance — Full-width cards
**Current:** `FxCard` with toggle + chips/slider.
**Target:** Keep full-width, refine styling to match new color scheme.
- Reverb: chip selector inside card (same as current, refined colors)
- Channel Balance: horizontal slider with L/Center/R labels

### 1.5 Color Scheme Change
**Current:** `GradientPinkVioletStart`/`GradientPinkVioletEnd` accents.
**Target:** Cyan/teal accents (`#00c8c8` primary, `#008888` secondary).
- Master toggle: cyan gradient
- FX gauge arcs: cyan
- Save button: cyan gradient
- Selected chips: cyan tint
- Glow effects: cyan

### 1.6 Bottom Bar
**Current:** "Reset to default" text + gradient "Save preset" pill.
**Target:** Same layout, cyan gradient on save button instead of pink-violet.

---

## 2. QueueHost.kt — Polish

### 2.1 Queue Item Row
- Artwork size: 40dp → 44dp
- Text hierarchy: title (15sp semibold), artist (13sp regular)
- Duration: 12sp, better positioning
- Playing indicator: smoother animation, slightly larger (14dp → 16dp)
- Drag handle: refine dot spacing/opacity

### 2.2 Mini Header
- Artwork: 48dp → 52dp
- Better padding/spacing
- Playing indicator slightly larger

### 2.3 Overall
- Sheet background: keep glass translucency
- Divider: subtle refinement
- Empty state: better typography

---

## 3. Files to Modify

| File | Change |
|------|--------|
| `EqualizerSheet.kt` | Full rewrite — bands, FX cards, colors |
| `QueueHost.kt` | Polish — sizing, spacing, animation |
| `Color.kt` | Possibly add new tokens (minor) |

## 4. Implementation Order

1. Rewrite `EqualizerSheet.kt` — bands section first, then FX cards, then bottom bar
2. Polish `QueueHost.kt` — sizing and spacing tweaks
3. Verify CI build
4. Commit
