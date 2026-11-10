# RIKO — DESIGN TOKEN MAPPING TABLE

> **Master-plan requirement:** §97 Phase 2 — *Create mapping table from existing
> values to canonical tokens.* Covers all eight consolidated areas:
> typography · spacing · shape · color · glass · artwork · motion · state.
>
> Scope of Phase 2: **tokens + this table.** Migrating call sites is *not* part
> of Phase 2 — component call sites move in Phase 3, screen call sites in Phase 5,
> where each change can be looked at visually. Nothing here changes a value.
>
> Counts exclude the locked `presentation/library/**` tree (freeze document).
> Tags: `[R]` ready to migrate as-is · `[N]` needs normalisation (visual review) ·
> `[V]` already canonical.

---

## 1. Typography

Canonical owner: `core/designsystem/riko/theme/RikoTypography.kt` (13 styles).

Material slots are **already routed** — `RikoTheme.kt:27` maps every
`MaterialTheme.typography.*` slot onto a `RikoTypography.*` style, so a slot use
is not by itself a violation. It becomes one when code reads a *scale* slot
instead of the RIKO role that says what the text is for.

| Raw / current | Canonical token | Notes |
|---|---|---|
| `MaterialTheme.typography.displayLarge/Medium` | `RikoTypography.Display` | `[R]` |
| `MaterialTheme.typography.displaySmall` / `headlineLarge/Medium` | `RikoTypography.LargeTitle` | `[R]` |
| `MaterialTheme.typography.headlineSmall` / `titleLarge` | `RikoTypography.Title` | `[R]` |
| `MaterialTheme.typography.titleMedium` | `RikoTypography.Section` | `[R]` |
| `MaterialTheme.typography.titleSmall` / `bodyLarge` | `RikoTypography.Body` | `[R]` |
| `MaterialTheme.typography.bodyMedium` | `RikoTypography.Secondary` | `[R]` |
| `MaterialTheme.typography.bodySmall` / `labelSmall` | `RikoTypography.Caption` | `[R]` |
| `MaterialTheme.typography.labelLarge/Medium` | `RikoTypography.Label` | `[R]` |
| (no role) | `RikoTypography.Button` | `[V]` all button text |
| (no role) | `RikoTypography.Overline` | `[V]` meta / counts |
| (no role) | `RikoTypography.Legal` | `[V]` fine print |
| (no role) | `RikoTypography.BodyRegular` | `[V]` running body copy |
| **(missing)** time/progress read-outs | `RikoTypography.TimeTabular` | **added in Phase 2** — §11 "tabular numerals for time/progress" |

**Open:** 140 `MaterialTheme.typography.*` references across 12 files
(`SearchRendering` 24, `AudioRendering` 23, `PersonalizationRendering` 19,
`QueueRendering` 13, `SleepTimerSheet` 9, `LyricsRendering` 9 …) → migrate to
named roles in Phase 5.

§11 rules that accompany the migration: list rows are title + subtitle, one-line
ellipsis on dense surfaces, marquee allowed on the Now Playing title.

---

## 2. Spacing

Canonical owner: `RikoSpacing` in `RikoDimensions.kt` —
`XS 4 · SM 8 · MD 12 · LG 16 · XL 20 · XXL 24 · XXXL 32 · Huge 40 · Massive 48`.

| Raw `.padding(N.dp)` | Count | Canonical | Status |
|---|---:|---|---|
| `12.dp` | 14 | `RikoSpacing.MD` | `[R]` |
| `8.dp` | 7 | `RikoSpacing.SM` | `[R]` |
| `16.dp` | 6 | `RikoSpacing.LG` | `[R]` |
| `20.dp` | 3 | `RikoSpacing.XL` | `[R]` |
| `32.dp` | 3 | `RikoSpacing.XXXL` | `[R]` |
| `4.dp` | 2 | `RikoSpacing.XS` | `[R]` |
| `10.dp` | 4 | → `SM` (8) or `MD` (12) | `[N]` off-scale |
| `3.dp` | 1 | → `XS` (4) | `[N]` off-scale |
| `14.dp` | 1 | → `MD` (12) or `LG` (16) | `[N]` off-scale |
| `17.dp` | 1 | → `LG` (16) | `[N]` off-scale |
| `2.dp` | 1 | → `XS` (4) | `[N]` off-scale |

§11 base: 16dp screen horizontal rhythm → `RikoSpacing.LG`.
Also defined: `RikoComponentSize` (row heights, artwork sizes, touch targets)
and `RikoIconSize`.

---

## 3. Shape

Canonical owners: `RikoCornerRadius` (radius scale) and, added in Phase 2,
`RikoShape` (§11's intent list: pills · circles · cards · sheets · artwork ·
mini artwork · controls). `RikoShape` introduces **no new numbers** — every
value is an existing `RikoCornerRadius` entry named by intent.

| Raw `RoundedCornerShape(N.dp)` | Count | Canonical | Status |
|---|---:|---|---|
| `8.dp` | 12 | `RikoShape.Control` / `RikoCornerRadius.XS` | `[R]` |
| `16.dp` | 7 | `RikoShape.Card` / `RikoCornerRadius.MD` | `[R]` |
| `12.dp` | 6 | `RikoShape.Artwork` / `RikoCornerRadius.SM` | `[R]` |
| `20.dp` | 5 | `RikoCornerRadius.LG` | `[R]` |
| `24.dp` | 4 | `RikoShape.CardLarge` / `RikoCornerRadius.XL` | `[R]` |
| `4.dp` | 4 | no scale step below `XS` (8) | `[N]` |
| `14.dp` | 3 | → `MD` (16) or `SM` (12) | `[N]` |
| `13.dp` | 2 | → `SM` (12) | `[N]` |
| `10.dp` | 2 | → `XS` (8) or `SM` (12) | `[N]` |
| `26.dp` | 2 | → `XL` (24) | `[N]` |
| `18.dp` | 1 | → `LG` (20) | `[N]` |
| `2.dp` | 1 | → `XS` (8) | `[N]` |
| `CircleShape` / `999.dp` | — | `RikoShape.Pill`, `RikoShape.Circle` | `[R]` |

`RikoDimensions.kt` already declares `.padding(17.dp)` / `RoundedCornerShape(19.dp)`
as FORBIDDEN — the table above is the evidence for how far the tree is from it.

---

## 4. Color

Canonical owner: `RikoColor.kt` — 45 tokens covering canvas gradient, aurora
accents (`AccentViolet`, `AccentMagenta`, `RikoAuroraColors`), text ramp,
glass, surfaces, semantic (`RikoSuccess` / `RikoError` / `RikoWarning`),
favourite, toggle and gradient tokens.

**Measured:** 373 raw `Color(0x…)` references, **176 distinct** hex values,
of which only 11 distinct / 16 references happen to equal an existing token.

### Already equal to a token (pure import swap) `[R]`

| Hex | Token | Refs |
|---|---|---:|
| `0xFFFF6B6B` | `RikoError` | 3 |
| `0xFFFBBF24` | `RikoWarning` | 3 |
| `0xFF34D399` | `RikoSuccess` | 2 |
| `0xFF0A0A52` / `0xFF060750` / `0xFF04041D` / `0xFF020210` | `CanvasTop` / `CanvasMid` / `CanvasLowMid` / `CanvasBottom` | 4 |
| `0xFF8A2BF0` / `0xFFC92CEE` | `AccentViolet` / `AccentMagenta` | 2 |
| `0xFF6C3CE9` | `RikoGlowBlob` | 1 |

### Largest un-tokenised values `[N]`

| Hex | Refs | Assessment |
|---|---:|---|
| `0xFF19B7FF` | 29 | bright blue — not in the aurora palette; decide token vs. scoped colour in Phase 5 |
| `0xFF050505` | 19 | near-black — candidate for a canvas/surface token |
| `0xFF9A6CFF` | 15 | violet — overlaps `AccentViolet` intent |
| `0xFF1A1A2E` | 13 | deep indigo — overlaps canvas ramp |
| `0xFF444444` | 12 | mid grey — likely placeholder/disabled |
| `0xFFFF6FD8` | 11 | pink — aurora-adjacent |
| `0xFF666666` / `0xFF888888` | 11 / 9 | greys — likely placeholders |
| `0xFF00FF88` | 10 | green — overlaps `RikoSuccess` intent |
| `0x3319B7FF` | 8 | 20% tint of `0xFF19B7FF` |

176 distinct values cannot be judged from a number alone — each needs the
screen it appears on looked at (Phase 5). §11 forbids random per-track hash
colours and screen-specific hardcoding outside the token system.

---

## 5. Glass

Canonical owner: `core/designsystem/theme/GlassTokens.kt` — `GlassStyle(blur,
tintAlpha, borderAlpha, glowAlpha, shadowAlpha)` plus intent-based tokens
(`GlassTokens.BlurLarge`, `GlassTokens.MiniPlayer`, …).

| Requirement (§12) | Where it lives |
|---|---|
| tint | `GlassStyle.tintAlpha` `[V]` |
| translucency | `GlassStyle.blur` `[V]` |
| border | `GlassStyle.borderAlpha` `[V]` |
| reflection / glow | `GlassStyle.glowAlpha` `[V]` |
| depth | `GlassStyle.shadowAlpha` `[V]` |
| contrast management | `RikoGlassBorderActive`, text ramp `[V]` |

**§12 constraint (not a token — an audit rule):** no expensive blur on track
rows, large lists or repetitive list items. Glass belongs to focal surfaces.
Verified as an audit item, not a value.

---

## 6. Artwork

Canonical owner: `RikoArtwork` (`core/designsystem/riko/components/RikoArtwork.kt:49`).
Sizing lives in `RikoComponentSize` (thumbnail 56 · shelf 84 · medium 96 ·
large 240 · hero 280 · favourites hero 200 · mini ≈44 per §77).

| Raw | Count | Canonical | Status |
|---|---:|---|---|
| `AsyncImage(...)` bypassing the wrapper | **20 → 2** | `RikoArtwork` | **Phase 3 [M]**: 18 migrated (wrapper gained fill-mode; perf `ImageRequest`s kept); 2 remain in `V2Cards.kt` (file dies Phase 5, D-9) |

Re-measured in Phase 3 (G-17): the earlier 27/26 counted references; direct call sites were 20 (incl. the wrapper's own 1). One `rememberAsyncImagePainter` site (`VisualOsDockRenderer`) sits outside §76's `AsyncImage` wording — documented, not migrated.
call sites to migrate in Phase 3.

---

## 7. Motion

Canonical owner: `RikoMotion` (`core/designsystem/riko/theme/RikoMotion.kt`).

§24's six canonical classes and where each now lives:

| §24 class | Token | Status |
|---|---|---|
| fast | `RikoMotion.fast()` · `Fast = 100` | `[V]` |
| snappy | `RikoMotion.snappy()` | `[V]` |
| travel | `RikoMotion.travel()` | **added in Phase 2** → `bottomSheet()` (no new value) |
| normal | `RikoMotion.normal()` · `Normal = 200` | `[V]` |
| screen | `RikoMotion.screen()` · `Screen = 400` | `[V]` |
| settle | `RikoMotion.settle()` | **added in Phase 2** → `seekBarSettle()` (no new value) |

Scale: `Fast 100 · Normal 200 · Emphasized 300 · Screen/Hero 400 · ColorChange 700`.

| Raw `tween(N ms)` | Count | Canonical | Status |
|---|---:|---|---|
| `400` | 4 | `RikoMotion.screen()` | `[R]` |
| `700` | 4 | `RikoMotion.ColorChange` | `[R]` |
| `200` | 3 | `RikoMotion.normal()` | `[R]` |
| `300` | 1 | `RikoMotion.emphasized()` | `[R]` |
| `220` | 4 | → `Normal` (200) | `[N]` |
| `420` | 2 | → `Screen` (400) | `[N]` |
| `450` | 1 | → `Screen` (400) | `[N]` |
| `900` / `1100` | 1 / 1 | no scale step — long reveals | `[N]` |
| `6000` / `14000` | 1 / 2 | timed holds, not transitions | `[N]` — keep, but name them |

**Duplicate removed:** `object Duration { INSTANT · FAST 150 · MEDIUM 300 · SLOW 500 ·
SCREEN_TRANSITION 350 }` was a second ladder with values that *conflicted* with
the §5.1 scale (`FAST 150` vs `Fast 100`, `SLOW 500` vs `Screen 400`) while the
file's own comment claimed no parallel ladder existed. Five of its six members
had zero call sites; `COLOR_CHANGE 700` was used once, in the same file, and is
now `RikoMotion.ColorChange`. §8 (no duplicate source of truth) + §95 (dead code).

---

## 8. State

Canonical owner: `RikoStateTokens` (**added in Phase 2**).

Before it, press state lived in `RikoMotion` and semantic / disabled /
favourite / toggle state lived in `RikoColor`, so a call site had to know which
file owned its state. `RikoStateTokens` delegates to those owners — it adds
only what did not exist:

| State | Token | Origin |
|---|---|---|
| press scale / pressed overlay | `PressScale`, `PressedOverlayAlpha` | delegates to `RikoMotion` (§115 Press) |
| disabled | `RikoStateTokens.Disabled` | delegates to `RikoTextDisabled` (§86 enabled) |
| error / success / warning | `RikoStateTokens.Error/Success/Warning` | delegates to `RikoColor` (§26, §85) |
| skeleton / placeholder body | `RikoStateTokens.Skeleton` | delegates to `RikoSurface` (§84) |
| favourite, toggle | `FavoriteActive/Inactive`, `ToggleTrackOn/Off/Thumb` | delegates to `RikoColor` (§115) |
| **focus ring** | `FocusRingColor`, `FocusRingWidth` | **new** — §86 accessibility contract, colour reuses `RikoTextPrimary` so no new hue enters the palette |

Not a token, but the content half of "state": §85's typed empty states
(first launch · no songs · filtered empty · error · permission denied ·
no collection · no search results), each with message, explanation and a CTA
where meaningful. That contract is fulfilled per screen in Phase 5.

---

## 9. Migration plan

| Phase | Moves | Why there |
|---|---|---|
| 3 — Component System | 26 `AsyncImage` → `RikoArtwork`; track row / mini player / nav / top bar onto canonical tokens | one component changes once, everywhere follows |
| 5 — Screen by screen | 140 typography slots, 96 shapes, 373 colours, ~13 raw tweens | each screen is reviewed visually as it moves, so `[N]` normalisations can be confirmed by eye |
| 6 — Architecture | the two dependency cycles and the dead FFT tier config | structural, not token |

No value is changed by this document. Normalising `13.dp → 12.dp` moves pixels,
so it happens while its screen is being looked at, never as a drive-by.
