# RIKO PLAN 2 — GLOBAL APP SHELL MIGRATION 🌍

> **Status:** 🟢 CODE COMPLETE — ⏳ build verification pending (user environment)
> **Scope:** Global shell ONLY — কোনো individual production screen redesign না
> **Audit basis:** Phase 2A full audit (root=MainActivity → RikoMusicTheme →
> ProvideVisualOs → RikoNavGraph; NO Scaffold — custom Box layering; old dock =
> `RikoBottomNav` + VisualOsHost dock; 5 destinations preserved)
> **Git:** checkpoint commit before work ✅

---

## 1. ARCHITECTURE DECISION (spec 2W → Option C: Hybrid)

```
MainActivity (DESIGN_PREVIEW_MODE = false)
└── RikoMusicTheme (UNCHANGED — legacy screens safe)
    └── ProvideVisualOs { RikoNavGraph }
        └── RikoAppShell (NEW — Plan 2 core)
            ├── RikoAmbientBackground (artwork-driven tints via dynamicColors)
            │   └── NavHost (OLD screens — structurally untouched)
            ├── RikoShellMiniPlayer (persistent, bottom)
            └── RikoBottomNavigation (floating, bottom)
        ├── VisualOsHost (renderDock = false — overlays untouched)
        └── Glass sheets (untouched)
```

## 2. NEW FILES

| File | Role |
|---|---|
| `riko/shell/RikoAppShell.kt` | Shell container + `RikoAmbientColors` + `RikoShellMetrics` + `LocalRikoShellMetrics` |
| `riko/shell/RikoShellMiniPlayer.kt` | Adapter: existing `PlayerState` → `RikoMiniPlayerData` (NO logic) |
| `riko/components/RikoTopBar.kt` | 5 variants: Standard/Back/Centered/Search/Hero — transparent default, `scrolled` surface fade |

## 3. MODIFIED FILES (minimal, reversible)

| File | Change |
|---|---|
| `RikoNavGraph.kt` | Box → RikoAppShell; old dock Column removed; nav swapped; shell nav items (same 5 destinations); VisualOsHost renderDock=false |
| `VisualOsHost.kt` | `renderDock: Boolean = true` param — dock layer gated only (all other layers untouched) |
| `RikoAmbientBackground.kt` | `secondaryColor` param added (was hardcoded blue) |
| `MainActivity.kt` | `DESIGN_PREVIEW_MODE = false` (production shell live; true = Plan 1 preview lab) |

## 4. KEY DECISIONS

- **Destinations preserved (2L):** My Music / Playlists / Visualizer(=NowPlaying) / Favorites / Settings — NOT a blind 4-tab copy
- **Visibility (2M):** Now Playing → nav hidden (immersive); mini player auto-hidden (existing `showMiniPlayer` rule kept)
- **Source of truth (2N):** `navController.currentBackStackEntryAsState()` — single source
- **Layering (2P/Z):** content scrolls UNDER floating layers; mini(64) → gap(8) → nav(68) → margin(12) → navBarsPadding
- **Content padding contract (2U/V):** `LocalRikoShellMetrics.contentBottomPadding` — screens adopt in Plan 3+; old screens unchanged (2AE)
- **Ambient (2D/E):** `RikoAmbientColors(primary=dynamicColors.vibrant, secondary=lightVibrant)` — artwork-driven tints already flow; intensity 0.14 clamped 0.10–0.22
- **Old `RikoBottomNav.kt`/`components/MiniPlayer.kt`:** now unused, kept (Plan 3+ cleanup)

## 5. NOT TOUCHED (2AU)
Media3/ExoPlayer/audio focus/service/notification · Room/DAO/repo/scanner ·
lyrics/EQ/queue/favorites/playlist persistence · `presentation/miniplayer/`
logic package (25 files, M1.6 frozen) · Visual OS coordinator/state/mapper ·
individual screen content.

## 6. VALIDATION MATRIX (2AW — run after build)
[ ] App launches · [ ] Home(AllSongs) opens · [ ] Bottom nav works ·
[ ] Tab switch works · [ ] Back works · [ ] Playback continues across nav ·
[ ] Mini player appears · [ ] Play/pause works · [ ] Tap opens Now Playing ·
[ ] Nav doesn't cover content · [ ] Status/gesture bars correct ·
[ ] Long title safe (ellipsis) · [ ] Missing artwork safe ·
[ ] No flicker / navigation reset / playback interruption · [ ] Build passes

## 7. NEXT
Visual QA on device → Plan 3: Home screen full visual migration.