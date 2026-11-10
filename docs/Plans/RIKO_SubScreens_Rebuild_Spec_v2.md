# RIKO Music — Sub-Screen Rebuild Specification v2.0
### Equalizer · Now Playing / Sleep Timer · Song Info
**Status:** Supersedes the uploaded "Pro Max Doc v1.0" — corrected and re-aligned to the RIKO manifesto
**Scope:** The 3 screens shown in your screenshots only (not a full-app spec)

---

## 0. Read This First — Why v1.0 Needed Correction, Not Just Polish

I read your uploaded doc, your three screenshots, and cross-checked both against your own RIKO Vision manifesto (the one where RIKO is defined as a "Music-driven Experience OS," not an offline player). Here's the honest finding:

**The uploaded doc is a well-written *generic* Material 3 spec. It is not a RIKO spec.** If you handed it to a random Android agency, they'd build a nice, clean, forgettable Material You music app — the kind that looks like every other Compose sample on GitHub. Several of its own recommendations actively contradict decisions you already made:

| # | v1.0 said | Conflicts with | Why it matters |
|---|---|---|---|
| 1 | Base colors from **wallpaper** (Material You dynamic_color) | RIKO's "Chemical Reaction" rule — colors must cascade from **album art**, not the phone's wallpaper | A wallpaper-driven palette means two different songs can look identical, and the wallpaper (not the music) drives the mood. That's the opposite of "album art hero." |
| 2 | Use **androidx.palette** for extraction | RIKO's explicit rule: *"colors must be extracted with contrast/saturation/lightness adjustment, never raw bitmap colors — muddy otherwise"* | Palette's `Vibrant`/`Muted` swatches are raw bitmap colors. They are NOT contrast-corrected and will produce exactly the "muddy" result your manifesto warns against. |
| 3 | Icons from **Phosphor Icons / Lucide** | RIKO's rule: *"Icons should be a consistent glass family... never mixing outline/filled/cartoon styles"* — and you already built an **88-icon neon SVG pack** for this exact purpose | Pulling in a generic third-party family (even a good one) breaks the one-family rule you already solved. Phosphor should be a *last-resort supplement*, not the primary source. |
| 4 | **Downloadable Fonts API** for Manrope/Sora/Inter | RIKO's "Offline-first identity is core... no cloud/internet dependency" | Compose's `ui-text-google-fonts` fetches fonts at runtime through Google Play Services over the network. On a plane, in airplane mode, or on a Play-Services-less device (common in your market), the type system silently falls back to the system font. For an offline app this is a real bug, not a style choice. |
| 5 | Flat **M3 achromatic surfaces**, standard `ModalBottomSheet`, generic `Switch`/`Slider` | RIKO's 8-layer **glass formula** (transparency + blur + tint + border + reflection + highlight + depth + grounding shadow) and the rule that "glass must receive background color, never sit as an empty transparent box" | A stock M3 surface is exactly the "empty transparent box" your manifesto forbids. |
| 6 | Circular album art with a "subtle continuous rotation" | Your established **Tactile Object** direction (accelerometer-tilt parallax, physical album art) already shipped in `riko-music` | v1.0 regresses you back to a generic vinyl-spin idea you've already moved past. |

None of this means throw the doc away — the type scale, the token table, the accessibility checklist, and the screen-by-screen structure are genuinely useful scaffolding. What follows is that scaffolding **rebuilt on your actual foundations**, using the two design languages your memory already documents: the **glass/layer system** from RIKO Vision, and the **Tactile Object / plasma-waveform** work already shipped in `riko-music`.

---

## 1. What's Actually Wrong in the Three Screenshots

Diagnosing the screens as-built, not as-described:

**Equalizer (Image 1)**
- The curve is a thin flat-blue polyline with plain circle nodes — no glow, no gradient, no thickness. This is the "flat graph" the doc itself flags as a *current issue*, meaning the rebuild hasn't reached this screen yet.
- The card background is a single flat dark tone with no glass depth — no top specular highlight, no ambient color bleed from the song, no inner glow. It reads as "dark grey box," not glass.
- Toggle switches are stock Material — no dynamic-color fill, no icon-morph on state change.
- Preset pills (Flat/Normal/Classical/Dance) are plain outlined capsules with no elevation or selection glow.

**Song Info (Image 2)**
- Exactly matches the doc's own "current issues": every field is its own heavy rounded card with a big icon-in-a-circle, producing excessive vertical scroll for 7 data points. This is a form, not a panel.
- No visual hierarchy between primary metadata (Title/Album) and technical metadata (Bitrate/Format) — they're all styled identically.
- No album art thumbnail, no bitrate/quality badge, no share action.

**Now Playing + Sleep Timer (Image 3)**
- Album art is circular with heavy vignette but no ambient glow halo, no parallax, no "breathing" — it's static.
- Sleep Timer is already a flat inline grid at the bottom (5m–90m pills in a 4×2 grid) — this is the *disconnected block* the doc's own "current issues" describes, sitting there permanently instead of being summoned only when needed.
- Background is a plain dark vignette, not the blurred/scaled album-art background your manifesto calls for.
- No glass card boundary — the Sleep Timer block just floats on the same flat background as everything else, no depth separation.

The common thread across all three: **flat, single-layer surfaces with no glass depth, no content-driven color, and no motion identity.** That is precisely the RIKO Vision failure list ("weak hierarchy, generic cards, too-uniform glass, no hero, no emotional identity"). Good — it means the fix is consistent across all three screens, not three separate problems.

---

## 2. Corrected Design System (what replaces §3 of v1.0)

### 2.1 Color — Content-Driven HCT Extraction, Not Palette API

Replace `androidx.palette` with **[MaterialKolor](https://github.com/jordond/materialkolor)** (Kotlin Multiplatform, MIT-licensed, actively maintained as of 2026, built specifically for this problem). It:
- Extracts a seed color directly from an `ImageBitmap` (your album art)
- Runs it through the same **HCT (Hue-Chroma-Tone)** algorithm Google's Material Color Utilities uses for real Material You — meaning it auto-corrects for contrast, chroma, and tone the way your manifesto requires ("never raw bitmap colors")
- Ships `DynamicMaterialTheme` / `DynamicMaterialExpressiveTheme` composables that generate a full accessible tonal palette (not just 1–2 swatches like Palette API)
- Works fully offline — pure on-device math, no network call

```
implementation("com.materialkolor:material-color-utilities:<latest>")
```
Use it as: `albumArt bitmap → seedColor → dynamicColorScheme(seedColor, isDark) → CompositionLocal for this Now-Playing session`. This one swap fixes conflict #1 and #2 from the table above in a single dependency, and gives you the "chemical reaction" cascade almost for free — every color role in the scheme updates when the seed changes, so glass tint, glow, button color, and EQ curve gradient can all read from the *same* derived scheme instead of you hand-wiring five separate color pulls.

### 2.2 Glass — Apply Your Own Formula, Not a Flat `surfaceContainer`

Every card in these 3 screens (EQ panel, Song Info card, Sleep Timer sheet) should be built from your documented glass formula, not a plain M3 surface:

1. **Base:** `Modifier.blur()` (native, first-party — matches your decision to drop `haze`) sampling the *scaled, blurred album art* behind the panel, not a flat color.
2. **Tint:** overlay the current HCT-derived `surfaceContainer` tone at ~40–55% alpha on top of the blur.
3. **Border:** 1dp stroke using the scheme's `outlineVariant` at ~30% alpha.
4. **Top specular highlight:** a 1–2dp gradient line (white → transparent) along the top edge only — this alone is what makes a panel read as "glass" instead of "grey box."
5. **Grounding shadow:** soft diffused shadow, `rgba(0,0,0,0.35) 0px 12px 32px`, larger and softer than v1.0's spec (which was tuned for flat dark mode, not glass-over-content).
6. **Ambient halo:** a very soft radial glow in the scheme's `primary` tone bleeding outward from the panel's edge, intensity tied to the album art's saturation.

This directly fixes the "empty transparent box" problem in all three screenshots. Not all three panels should glow equally — per your own rule, treat the **Now Playing background** as hero glass, the **EQ panel** as supporting glass, and the **Song Info card** as utility glass (quieter, less glow, more legibility-first).

### 2.3 Typography — Bundle Fonts as Static Assets, Not Downloadable

Keep v1.0's type scale (Manrope for display/headlines, Inter for body/UI) — it's a good pairing and matches your "editorial typography" note from `riko-music`. But source it correctly for an offline app:

- **[Manrope — Google Fonts](https://fonts.google.com/specimen/Manrope)** — download the static `.ttf` weight files you need (600/700) and place them in `res/font/`.
- **[Inter — Google Fonts](https://fonts.google.com/specimen/Inter)** — same, static bundle, 400/600/700.
- Build `FontFamily(Font(R.font.manrope_bold, FontWeight.Bold), ...)` directly — no `GoogleFont.Provider`, no `ui-text-google-fonts` dependency, no runtime fetch, no Play-Services requirement. This is a two-minute fix that removes a real offline-mode bug from the original spec.
- Reference: [Compose "Work with fonts" — bundled vs. downloadable](https://developer.android.com/develop/ui/compose/text/fonts) (read the *bundled* section, skip the *downloadable* section for this project).

### 2.4 Iconography — Audit Your Own Pack First

Before pulling any external icon set, inventory your existing **88-icon neon SVG pack** against what these 3 screens actually need:

*Equalizer:* bass-boost, virtualizer, loudness/speaker, reset, save, chevron/dropdown
*Song Info:* music-note (title), disc/album, clock (duration), storage (size), gauge (bitrate), calendar (year), file (format), share
*Now Playing/Sleep Timer:* heart/favorite, back-arrow, timer/clock, close

Most of these are exactly the kind of icon a media-player-focused pack already covers. For any genuine gap:

- **Primary supplement — [Material Symbols](https://fonts.google.com/icons)** (Google, Apache-2.0, free). The reason to prefer this over a third-party pack: it's a **variable font** with `fill` as an actual axis (0 → 1), so an icon's filled/outlined state — exactly the "active vs inactive" requirement in your glass-icon rule — is one animated font-variation value instead of swapping between two different icon assets. Import only the specific icons you need as static Vector Drawables via Android Studio's *Import Vector Asset* tool (paste the SVG from [fonts.google.com/icons](https://fonts.google.com/icons)) — do **not** pull in `material-icons-extended` as a Gradle dependency; it adds ~8 MB to your APK for icons you'll use maybe a dozen of ([source](https://developer.android.com/develop/ui/compose/resources)).
- **Secondary fallback — [Phosphor Icons](https://phosphoricons.com)**, raw SVG source at **[github.com/phosphor-icons/core](https://github.com/phosphor-icons/core)** (MIT license, 1,500+ icons, 6 weights including a Duotone that could actually suit your "glass, layered, frosted" look better than a flat outline). Same rule: hand-pick individual SVGs, convert to `ImageVector` via Vector Asset Studio, restyle the stroke width to match your pack's 2dp convention. Don't add a webfont or an icon-library Gradle dependency for a handful of icons.

Either way, **restyle whatever you import** (stroke width, corner rounding, color source) so it visually merges into your existing 88-icon family. An unedited Phosphor icon sitting next to your neon glass icons will look like a seam.

### 2.5 Shape & Motion — `graphics-shapes` for Real Morphing

v1.0 suggests `AnimatedVectorDrawable` for the play/pause icon morph. Since Compose 1.2+/AndroidX has a native, first-party alternative that fits your "no fake reaction, every motion traceable to a real cause" rule much better:

**[`androidx.graphics:graphics-shapes`](https://developer.android.com/develop/ui/compose/graphics/draw/shapes)** — lets you define rounded polygons and interpolate/morph between them at runtime with real progress values (not a canned AVD timeline). Use it for:
- The play↔pause button shape/halo morph
- The Sleep Timer icon's press-state pulse
- EQ node shape change on drag (circle → slightly elongated blob while dragging, settling back to circle — a nice physical touch consistent with your "tactile object" language)

```
implementation("androidx.graphics:graphics-shapes:1.0.1")
```

For M3's own expressive shape vocabulary (which pairs with this library), Compose Material3 now ships `MaterialShapes` as of the stable **Material3 1.4.0** release — worth reviewing before hand-building custom blob shapes: [Material 3 Expressive in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3).

### 2.6 M3 Expressive — Now Stable, Actually Relevant to Your EQ Screen

One thing worth knowing since your doc predates it: **Material 3 Expressive graduated to stable in `androidx.compose.material3:material3:1.4.0`**, with `1.5.0` in alpha as of this writing. It ships exactly the primitives v1.0 was gesturing at with phrases like "expressive M3 Toggles" — a real `MaterialExpressiveTheme`, a physics-based `MotionScheme.expressive()`, and updated Slider/Switch/Chip components with springier default motion. Reference: [Compose Material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) · [Material 3 Expressive overview](https://developer.android.com/develop/ui/compose/designsystems/material3).

Practical implication for the Equalizer screen: don't hand-roll spring physics for the EQ node drag and toggle fill — opt into `MaterialExpressiveTheme` + `MotionScheme.expressive()` at the theme level first, and only hand-build custom motion for the parts that are truly RIKO-specific (the glowing curve itself, the halo).

---

## 3. Screen 1 — Equalizer (Rebuilt)

**Identity:** per RIKO Vision, this is a "floating instrument with real audio-session connection," not a settings panel. Treat it as a **Panel-layer** element (per your 5-layer system) — it floats over a blurred, dimmed Now Playing context; the background never fully disappears.

- **Container:** supporting-glass card (§2.2), radius 28dp, floating over a blurred/dimmed Now Playing screen rather than a full-screen destination — respects your "small tools stay as floating panels, not full-screen" rule.
- **Curve:** `Path` with `StrokeCap.Round`, 4–6dp stroke, `Brush.horizontalGradient` sourced from the MaterialKolor-derived scheme (not a fixed blue). Add a soft outer glow pass beneath the stroke using a blurred duplicate path at lower alpha — the same additive-glow technique you already built for the plasma waveform in `riko-music`; reuse that renderer rather than building a second one.
- **Nodes:** 24dp circles, morph to a slight ellipse on drag via `graphics-shapes` (§2.5), halo glow intensifies on drag, haptic tick on drag (light impact).
- **Presets:** `FilterChip` row, selected chip filled with the scheme's `secondaryContainer` + an animated check — this part of v1.0 is fine as-is, just re-themed to the dynamic scheme instead of a static M3 palette.
- **Toggles:** Bass Boost / Loudness / Virtualizer — fill color from the dynamic `primary`, and if you adopt Material Symbols per §2.4, the icon's `fill` axis animates 0→1 on toggle instead of swapping drawables.
- **Sliders:** reveal only when the toggle is on (v1.0 had this right) — thin track, large thumb, value in `Label Large`.
- **Bottom bar:** "Reset" text button + "Save Preset" pill, sticky.

---

## 4. Screen 2 — Now Playing + Sleep Timer (Rebuilt)

**Identity:** this is your **Focus layer** — the emotional core. The Sleep Timer must stop being a permanent block and become a **summoned Panel**, per your layer system's own rule that small tools stay as floating panels rather than living inline.

- **Background:** blurred, scaled album art (`RenderEffect.createBlurEffect`, radius ~80) behind a scrim tuned to the *extracted* scheme's tone (not a flat 60% black — a flat black scrim is exactly the kind of "no fake reaction" violation your manifesto calls out; the scrim's darkness/tint should derive from the art itself).
- **Album art:** keep your already-shipped Tactile Object treatment (accelerometer parallax) — do **not** regress to v1.0's "basic circle with subtle rotation." Add the ambient outer glow halo in the extracted `primary` tone, and a gentle "breathe" (gentle scale pulse while playing, near-still when paused) per your manifesto's hierarchy description.
- **Title/artist:** `Headline Medium` in Manrope, `Modifier.basicMarquee()` for overflow — [reference](https://developer.android.com/reference/kotlin/androidx/compose/foundation/basicMarquee.modifier). This part of v1.0 is genuinely good and needs no change, just confirm you're on Compose Foundation ≥1.9 (stable, non-experimental as of recent releases) so you don't need the `@ExperimentalFoundationApi` opt-in.
- **Sleep Timer — the actual fix:**
  - Remove the permanent grid entirely from the Now Playing layout.
  - Replace with a compact glass icon-button (timer glyph) near the transport controls.
  - Tapping it summons a **Panel-layer bottom sheet** (`ModalBottomSheet`, but built with your glass formula, not stock M3 surface) — this satisfies both v1.0's UX goal *and* your manifesto's "background never dies, blur must gradually earn its value" motion rule: the Now Playing screen should visibly blur/dim further as the sheet rises, not cut away.
  - Inside: pill row of presets (5/10/15/20/30/45/60/90m — keep the 8 options you already have, v1.0's 7-option set was actually a regression from your current build) + a custom radial dial for arbitrary durations, echoing the "analog clock face, draggable hands" instrument described for your Timer in RIKO Vision — reuse that same interaction pattern here rather than inventing a second timer UI language.
  - Active state: icon fills with the dynamic `primary`, thin circular progress ring around it, small "23m left" label — this part of v1.0 was already correct, keep it.

---

## 5. Screen 3 — Song Info (Rebuilt)

**Identity:** a **Panel-layer** utility sheet — quiet, legible, information-dense. This is the one screen where you want the *least* glow and the *most* clarity; per your own "not all glass should be equally loud" rule, this is utility glass.

- **Container:** single card, radius 28dp, utility-glass treatment (subtle blur/tint, minimal glow, high-contrast text) — replaces the 7 separate heavy cards in the current build.
- **Header:** 48dp rounded-square album art thumbnail + "Song Info" (`Title Large`) + Share icon button, all in one row.
- **Row layout:** `Column` with a hairline `HorizontalDivider` (0.5dp, `outlineVariant`) between rows — not full card separation. Each row: small icon (from your pack, `onSurfaceVariant` tint) + `Label Medium` field name + `Body Large` value directly beneath it. This single change (7 cards → 1 card + dividers) is the highest-leverage fix on this screen and costs almost nothing to implement.
- **Technical metadata grouping:** Size + Bitrate + Format + Year as a compact 2-column grid at the bottom, separated visually from Title/Album/Duration (primary vs. technical hierarchy, which the current build has none of).
- **Hi-Res badge:** small `tertiaryContainer` chip next to Bitrate when ≥320kbps — v1.0 had this right, keep it, but derive the chip's tertiary tone from the dynamic scheme rather than a static M3 token.

---

## 6. Resource Library (Verified Links)

| Category | Resource | Link | Note |
|---|---|---|---|
| Color extraction | MaterialKolor | https://github.com/jordond/materialkolor | Kotlin Multiplatform, MIT, HCT-based, offline |
| Icons — primary | Your existing 88-icon neon SVG pack | *(local — audit before sourcing externally)* | Matches your one-family rule already |
| Icons — supplement | Material Symbols | https://fonts.google.com/icons | Variable `fill` axis = animatable active/inactive state |
| Icons — fallback | Phosphor Icons (browse) | https://phosphoricons.com | MIT, 6 weights incl. Duotone |
| Icons — raw SVGs | Phosphor Icons Core repo | https://github.com/phosphor-icons/core | `/assets/<weight>/<name>-<weight>.svg` — hand-pick, don't bulk-import |
| Font | Manrope (Google Fonts) | https://fonts.google.com/specimen/Manrope | Download static `.ttf`, bundle in `res/font/` |
| Font | Inter (Google Fonts) | https://fonts.google.com/specimen/Inter | Same — static bundle only |
| Font (Compose) | Bundled vs. downloadable fonts guide | https://developer.android.com/develop/ui/compose/text/fonts | Use the bundled-font section only |
| Shape/motion | `androidx.graphics:graphics-shapes` | https://developer.android.com/develop/ui/compose/graphics/draw/shapes | Real polygon morphing, not AVD |
| Motion system | Material 3 Expressive in Compose | https://developer.android.com/develop/ui/compose/designsystems/material3 | Stable since Compose Material3 1.4.0 |
| Release notes | Compose Material3 releases | https://developer.android.com/jetpack/androidx/releases/compose-material3 | Track 1.5.0 alpha for new Slider/TimePicker |
| Marquee | `Modifier.basicMarquee` reference | https://developer.android.com/reference/kotlin/androidx/compose/foundation/basicMarquee.modifier | Stable API |
| Icon size warning | `material-icons-extended` size cost | https://developer.android.com/develop/ui/compose/resources | ~8MB — avoid as a direct dependency |

---

## 7. Build Order (Respecting Your Own Priority Stack)

Per RIKO Vision's stated priority order (Premium UI/UX → Motion → Now Playing → Glass → Function → Performance), the sequencing that respects it:

1. **MaterialKolor integration first** — every other visual decision (glass tint, glow color, EQ gradient, chip colors) depends on having a real dynamic scheme available. Building glass panels against a static color first, then retrofitting dynamic color later, means redoing the glass layer twice.
2. **Song Info rebuild** — lowest motion/glass complexity, highest immediate visual-quality return, good place to validate the new glass-panel component before using it on harder screens.
3. **Sleep Timer sheet extraction** — pulls the timer out of Now Playing into its own Panel, unblocking the Now Playing background rework.
4. **Now Playing background + album art glow**
5. **Equalizer** — most motion-complex (drag, morph, glow-on-drag), tackle last so it can reuse the glass-panel and glow-renderer patterns already proven on the earlier screens.

## 8. Pre-Ship Checklist (Your Own Test, Applied to These 3 Screens)

Before calling any of these three screens done, run your own per-scene check from RIKO Vision:
- Is it premium? Smooth? Alive?
- Is the hierarchy clear?
- Does it feel like RIKO — or would it be indistinguishable from a generic Compose Material You sample with the logo hidden?

Any "no" on any of the three screens means it's not shipped yet, regardless of how close it looks.
