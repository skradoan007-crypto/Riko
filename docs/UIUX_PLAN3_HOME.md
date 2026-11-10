# RIKO PLAN 3 — HOME / MY MUSIC FULL VISUAL MIGRATION 🎧

> **Status:** 🟢 CODE COMPLETE — ⏳ build verification pending (user environment)
> **Scope:** Home screen ONLY — প্রথম individual production-screen migration
> **Product role (spec §1):** WELCOME → RESUME → DISCOVER → ACCESS → PLAY
> **Route:** `AllSongs` অপরিবর্তিত (spec §86) — শুধু composable swap
> **Git:** checkpoint `43e461f` থেকে কাজ শুরু

---

## 1. AUDIT FINDINGS (STEP 1)

| Data source | Status |
|---|---|
| Songs (A–Z) | `MusicRepository.getAllSongs()` ✅ |
| Recently Added | `getAllSongs(DATE_ADDED)` pattern ✅ |
| **Recently Played** | `PlayHistoryDao.getRecentlyPlayed(50)` — **existing but unused**; এখন READ-only ব্যবহৃত (no new storage, spec §16) |
| Favorites | `PlaylistRepository.getFavoriteSongIds()` ✅ |
| Playlists | `PlaylistRepository.getAllPlaylists()` — `coverUri`, `songIds`, `isFavorites` ✅ |
| Albums/Artists | ❌ কোনো query নেই → **client-side derive** from songs (groupBy) — Room/repo untouched (spec §44–45) |
| Old screen problems | `GlassBackground(0.65f)` glow, `padding(bottom = 180.dp)` hardcoded ✅ fixed |

## 2. NEW FILES (`presentation/home/`)

| File | Role |
|---|---|
| `HomeUiState.kt` | `HomeUiState` + `HomeSection(Type)` + `AlbumUi`/`ArtistUi` — data-driven sections (§42) |
| `HomeViewModel.kt` | Combines 5 existing flows + refreshKey retry; NO duplicate VM (§43) |
| `HomeScreen.kt` | Single LazyColumn; conditional items; adaptive All-Songs anchor; shell-metrics padding |
| `components/HomeHeader.kt` | Time greeting 28sp + subtitle + Search/Settings (max 2 actions, §4–6) |
| `components/HomeHeroCard.kt` | `RikoHomeHero` + `HomeHeroData` — 140dp rounded art, 56dp purple play (0.96 press), 3dp progress, fallback "Your Music" |
| `components/HomeQuickActions.kt` | Glass chips 40dp: All Songs(anchor) / Favorites / Playlists / Search — all REAL (§57) |
| `components/HomeSections.kt` | `RikoHomeSection` generic + `SongShelfSection` + `FavoriteShelfSection` |
| `components/HomeCollectionSections.kt` | Playlists (real coverUri/icon) / Albums (square art) / Artists (circle avatar, §21) |
| `components/HomeStateViews.kt` | Dark skeleton shimmer / Empty+Scan / Error+Retry (§37–41) |

## 3. MODIFIED (minimal)

- `RikoNavGraph.kt` — AllSongs composable → `HomeScreen` (route ID preserved)
- `RikoArtwork.kt` + `RikoDimensions.kt` — additive `Shelf` size (84dp, spec §15)

## 4. KEY DECISIONS

- **Hero priority (§7):** currentSong → recentlyPlayed[0] → "Your Music" fallback; onPlay: toggle vs playQueue
- **All Songs সমাধান:** Home-এর শেষে full A–Z list (RikoSongRow + favorite toggle) — সব functionality preserved
- **Spacing rhythm (§75):** header→hero 24 · hero→quick 20 · section gaps 32 · header→items 12
- **Glass (§50):** শুধু hero + quick chips — প্রতিটা section/row না
- **Ambient (§49):** Home-এ দ্বিতীয় gradient system নেই — global shell-ই দেয়; পুরনো GlassBackground সরানো
- **Insets (§28–29):** top = statusBar inset একবার contentPadding-এ; bottom = `LocalRikoShellMetrics.contentBottomPadding` + navBars — কোনো arbitrary 180dp না
- **Performance (§65–66):** সব Lazy list-এ stable keys (`song.id`, `album.id`, `artist.id`, `playlist.id`); কোনো nested vertical scroll না
- **Permission (§40):** empty state থেকে SAME contract re-trigger — MainActivity-র flow অপরিবর্তিত
- **Playback (§58/§91):** `playQueue` + `togglePlayPause` + `toggleFavorite` — existing PlayerViewModel actions

## 5. NOT TOUCHED (§96–lock)
Media3/playback/queue · Room/DAO/repo/scanner · miniplayer/ package · Visual OS ·
global shell (Plan 2 locked) · other screens (Favorites/Playlists/Now Playing/Search/Settings pages) ·
কোনো recommendation algorithm, fake data, বা নতুন dependency না।

## 6. VALIDATION (§87–97)
Screenshot top/mid/bottom · hero balance · sections breathe · content above mini player ·
fast scroll smooth · ambient subtle across artworks · long text ellipsis · missing artwork placeholder ·
small screen safe · build passes.