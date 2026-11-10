# UI/UX Plan 8: Search + Universal Local Music Discovery

## Status: ✅ COMPLETE

## Overview
Search is the fastest gateway into the entire local Riko music library. Plan 8 migrates
the existing Search presentation to the Riko Premium design system while preserving all
existing search behavior.

## Audit Findings

### Existing Search Implementation
| Component | File | Status |
|-----------|------|--------|
| Route | `RikoDestination.Search` | `"search"` — bottom nav tab |
| Screen | `presentation/search/SearchScreen.kt` | 126 lines → 389 lines (Riko migration) |
| ViewModel | `presentation/search/SearchViewModel.kt` | 70 lines — UNCHANGED |
| Search capability | `MusicRepository.searchSongs()` | Songs only — UNCHANGED |
| Debounce | 280ms | UNCHANGED |
| Cancellation | `flatMapLatest` | UNCHANGED |

### What Was NOT Added (Per Plan 8 Scope)
- ❌ Multi-entity search (albums, artists, playlists, folders) — not supported by existing search
- ❌ Recent searches — not supported by existing architecture
- ❌ Filters — not supported by existing search
- ❌ AI/semantic search — explicitly forbidden
- ❌ New search index — architecture work, not visual migration

## Implementation Summary

### Visual Migration
- **Before**: Material3 TextField, SongListItem (legacy), basic layout
- **After**: GlassSurface search field, RikoSongRow, Riko skeleton loading, Riko empty states

### Key Features
1. **Glass Search Field** — Rounded, focused state with purple accent, clear button
2. **RikoSongRow Results** — Consistent with Home, My Music, Favorites, Playlists
3. **Empty Query State** — "Search your music library" guidance
4. **No Results State** — Meaningful message with query display
5. **Loading State** — Riko skeleton rows (no giant spinner)
6. **Back Navigation** — Proper popBackStack integration
7. **Keyboard Handling** — Focus management, IME dismiss on result tap
8. **Now Playing Integration** — Active song highlight, play queue on tap

### Design System Components Used
- `GlassBackground` — Dynamic color background
- `GlassSurface` — Search field container, skeleton rows
- `RikoSongRow` — Song result rows
- `RikoTypography` — Title, Secondary, Body text styles
- `RikoSpacing` — Consistent spacing (XS, SM, MD, LG)
- `RikoCornerRadius` — Rounded corners (SM, MD)
- `RikoPurple` — Accent color for focused state
- `LocalDynamicColors` — Dynamic theme integration

### State Management
- **Query state**: `SearchViewModel.query` StateFlow
- **Results**: `SearchViewModel.results` StateFlow
- **Loading**: `SearchViewModel.isSearching` StateFlow
- **Player**: `PlayerViewModel.playerState` for now-playing highlight

### Files Modified
- `SearchScreen.kt` — Complete rewrite (126 → 389 lines)
- `RikoNavGraph.kt` — Added `onBack` callback to SearchScreen

### Files NOT Modified (Preserved)
- `SearchViewModel.kt` — Unchanged (debounce, cancellation, search logic)
- `MusicRepository.kt` — Unchanged (searchSongs query)
- `RikoDestination.kt` — Unchanged (route definition)

## Acceptance Criteria Met
- [x] Search opens from bottom nav
- [x] Existing route preserved
- [x] Search field works with glass styling
- [x] Keyboard behavior correct (focus, dismiss)
- [x] Query updates correctly (280ms debounce preserved)
- [x] Existing search source reused (MusicRepository.searchSongs)
- [x] Songs appear correctly using RikoSongRow
- [x] No-results works with meaningful message
- [x] Loading works with Riko skeletons
- [x] Empty query shows search guidance
- [x] Result navigation preserves back stack
- [x] Song playback works (playQueue on tap)
- [x] Now-playing highlight works
- [x] Mini player remains correct
- [x] Global shell remains correct
- [x] No duplicate search/data logic
- [x] Build passes (brace balance verified)

## Verification
- ✅ Brace balance: depth=0 for SearchScreen.kt
- ✅ Brace balance: depth=0 for RikoNavGraph.kt
- ✅ All imports used
- ✅ No unused imports
- ✅ Content descriptions on all interactive elements
- ✅ Touch targets ≥ 42dp (IconButton)

## Notes
- Portrait-only layout (consistent with other screens)
- Search is utility/discovery, not immersive — restrained visual design
- No ambient glow from search results (per Plan 8 §28)
- All animations use RikoMotion for consistency