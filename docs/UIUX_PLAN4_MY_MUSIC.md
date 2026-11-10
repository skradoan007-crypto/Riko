# RIKO PLAN 4 — MY MUSIC / LIBRARY SYSTEM 📚

> **Status:** 🟢 CODE COMPLETE — ⏳ build verification pending
> **Scope:** My Music/Library screen family ONLY — presentation/query composition
> **Locked (spec):** Media3/Room/scanner/repo/playback/queue/Global shell/Plan 3 Home — সব untouched

---

## 1. AUDIT (spec §1)

| প্রश্ন | উত্তর |
|---|---|
| Current route | `AllSongs` tab → এখন Plan 3 HomeScreen |
| Old My Music screen | `AllSongsScreen` (sort + list + favorite) — Plan 3-এর পর unused |
| Big Library runtime (B1–B5) | `LibraryScreenWiring` ইত্যাদि frozen কিন্তু **NavGraph-এ wired না** → Plan 4 rewire করেনি (ঝুঁকি); lightweight approach |
| SortBy (REAL) | TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION, SIZE ✅ |
| Downloads data | ❌ কোনো বাস্তব উৎস নেই (শুধু frozen badge enum) → **mode বাদ** (spec §1) |
| Folders | ❌ query নেই → `song.path` parent থেকে client-side derive ✅ |
| Selection/context | পুরনো screen-এ ছিল না → invent করা হয়নি (spec §12/§38) |

## 2. NEW FILES (`presentation/mymusic/`, ৮টা)

`LibraryMode.kt` (Songs/Albums/Artists/Folders) · `MyMusicViewModel.kt` (+`FolderUi`,
`MyMusicUiState`, mode-local sort state — spec §34) · `MyMusicScreen.kt` (+dark
skeleton) · `components/`: `MyMusicHeader` (My Music + Search, max 2 actions §3),
`LibraryModeSelector` (scrollable chips, selected=purple gradient §4), `LibraryToolbar`
(count + "Sort ▾" pill), `LibrarySortSheet` (GlassBottomSheet + radio rows §10),
`RikoLibraryCards` (Album square-art card §16 / Artist circle avatar §18 / Folder
utility row §20), `LibraryContents` (4 modes, lazy, hoisted saveable states §30).

## 3. MODIFIED (minimal)

- `RikoDestinations.kt` — `data object MyMusic("my_music")` (NOT a bottom-nav tab, §4)
- `RikoNavGraph.kt` — MyMusic composable (back → Home, §45–46); HomeScreen-এ
  `onOpenLibrary` wiring
- `HomeScreen.kt` — "All Songs" chip এখন My Music খোলে (scroll-anchor বাদ — এখন
  real destination, spec §57–58); Home-এর All Songs list section অপরিবর্তিত

## 4. KEY DECISIONS

- **Mode state (§5/§30):** `rememberSaveable` enum + per-mode hoisted
  `LazyListState/LazyGridState` (Saver) — mode round-trips predictable, states কখনো
  শare হয় না
- **Grid adaptation (§17):** `GridCells.Adaptive(150dp)` albums / `(96dp)` artists —
  width-responsive, hardcoded tablet column না
- **Sort (§9–10):** শুধু repository-র বাস্তব ফিল্ড; sheet = existing GlassBottomSheetContainer reuse
- **Density (§8):** RikoSongRow 48dp art/72dp row default — perfect for browse
- **Glass (§43):** শুধু mode chips + sort pill; content rows calm
- **Album/Artist/Folder click (§35):** play-as-queue (বাস্তব existing action — detail route নেই)
- **Library runtime (B1–B5):** untouched, unwired থাকল — ভবিষ্যতের decision

## 5. VALIDATION (§53 checklist)
Modes switch + back-stack correct · sort sheet works · favorite works · playback
unchanged · empty/loading/error · search entry · long titles · missing artwork ·
large library smooth · bottom shell কখনো cover করে না · build passes.