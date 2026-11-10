# RIKO PLAN 1 — PREMIUM DESIGN SYSTEM FOUNDATION 🧪

> **Status:** 🟢 CODE COMPLETE — ⏳ build verification pending (এই Windows মেশিনে
> JDK/SDK/Studio নাই — user-এর build environment-এ `assembleDebug` দরকার)
> **Approved direction:** Dark-first, cinematic, premium, purple/blue ambient
> **Migration:** Preview-only — production screens UNTOUCHED until Plan 2
> **Entry:** `MainActivity.DESIGN_PREVIEW_MODE = true` (temporary, revert in Plan 2)

---

## 1. PRINCIPLE

একটা component-এর token বদলালে পুরো system-এর visual language একসাথে বদলাবে।
কোনো স্ক্রিনে কখনো direct color, spacing বা radius hardcode করা যাবে না।

## 2. COLOR TOKENS (`riko/theme/RikoColor.kt`)

| Token | Value | Role |
|---|---|---|
| `RikoBackground` | `#05050A` | Base — সব স্ক্রিনের মূল |
| `RikoBackgroundElevated` | `#0A0912` | Slightly raised base |
| `RikoSurface` | `#111019` | Level-1 surface (cards) |
| `RikoSurfaceElevated` | `#171522` | Level-2 (mini player, sheets) |
| `RikoSurfaceStrong` | `#1D1A29` | Level-3 (modals, hero) |
| `RikoPurple` | `#9C4DFF` | **Primary accent** |
| `RikoPurpleBright` | `#B86CFF` | Accent highlight |
| `RikoPurpleDeep` | `#7429D9` | Deep accent / container |
| `RikoBlue` | `#4F6BFF` | **Ambient only — button না** |
| `RikoTextPrimary/Secondary/Tertiary/Disabled` | `#F7F5FF / #B9B5C7 / #777285 / #4E4A59` | Text hierarchy (no pure white) |
| `RikoGlass / Strong / Border` | White 5.5% / 8.5% / 10% | Glass system |
| `RikoPrimaryGradient` | `#7A2CFF → #B84CFF` | Primary CTA / selected |
| `RikoAmbientGradient` | `#30206F → #120C31 → #05050A` | Full-screen ambient |
| `RikoPurpleBlueGradient` | `#8A35FF → #5367FF` | Hero accents |

**Accent rule (~70/20/8/2):** Purple শুধু — Play, Active nav, Favorite,
Progress, Selected filter, Primary CTA, Focused control.
**Ambient limits:** opacity 0.10–0.22, blur 40–80dp (enforcement constants included).

## 3. SCALES

- **Spacing** (`RikoSpacing`): 4/8/12/16/20/24/32/40/48dp
- **Radius** (`RikoCornerRadius`): 8/12/16/22/28/36 + Pill
- **Icons** (`RikoIconSize`): 20/24/28/32dp
- **Motion** (`RikoMotion`): 120/180/220/300/400ms, FastOutSlowIn default, NO bounce
- **Typography** (`RikoTypography`): Display 34 / LargeTitle 28 / Title 22 /
  Section 18 / Body 15 / Secondary 13 / Caption 11 / Label 12 / Button 14
- **Touch targets:** ≥48dp everywhere

## 4. COMPONENT INVENTORY (`riko/components/`, 11 files)

1. `RikoAmbientBackground` — near-black + 2 soft glows (purple TL, blue BL)
2. `RikoGlassSurface` — subtle/strong glass + border, active border variant
3. `RikoPrimaryButton` — gradient pill 48dp, press scale 0.96
4. `RikoSecondaryButton` — glass pill 48dp
5. `RikoIconButton` — 48dp circle, optional glass container, active=purple
6. `RikoSegmentedControl` — glass pill + animated purple selected segment (220ms)
7. `RikoArtwork` — 48/96/240/280dp scale, consistent shapes, music-note placeholder
8. `RikoSongRow` — 48dp art + title/artist + trailing slot; playing=purple title
9. `RikoSectionHeader` — section title + purple action
10. `RikoMiniPlayer` — 64dp strong-glass shell (state only, NO Media3 wiring)
11. `RikoBottomNavigation` — 68dp floating glass shell (no routing wired)

## 5. PREVIEW LAB (`riko/preview/RikoDesignSystemPreviewScreen.kt`)

Sections: Colors → Typography → Buttons → Segmented → Artwork Scale →
Song Rows → Glass Surfaces → Mini Player → Bottom Nav.

## 6. VISUAL QA CHECKLIST (validate before palette LOCK 🔒)

- [ ] A. Purple overdose? (accent ~2% of screen)
- [ ] B. Glass too visible? (subtle, never first thing noticed)
- [ ] C. Text hierarchy readable in 5 seconds?
- [ ] D. Surface separation clear? (3 levels)
- [ ] E. Everything token-driven and consistent?

## 7. MIGRATION ROADMAP

- **Plan 1 (এই doc):** Foundation + Preview Lab → validate → **LOCK PALETTE**
- **Plan 2:** Global App Shell — root RikoTheme, background, nav, mini player (`DESIGN_PREVIEW_MODE` revert)
- **Plan 3+:** Screen-by-screen (Home, Library, Favorites, Playlist, Search, Collection, Now Playing, Settings)

## 8. HARD BANS (enforced by tokens + component docs)

No excessive glow/glass, no fake shine, no metallic highlights, no hard radial
hotspots, no random gradients, no arbitrary dp/color values outside tokens.

## 9. BUILD NOTE

`gradlew.bat` এই repo-তে ভাঙা stub (`gradle %*`) — real wrapper jar আছে
(`gradle/wrapper/`)। Windows মেশিনে JDK/SDK নাই; build করতে হবে Android
Studio (JBR) বা JDK 17 + SDK সহ মেশিনে। `local.properties`-এ Linux path
(`/home/z/Android/Sdk`) — অন্য machine-এ override দরকার।