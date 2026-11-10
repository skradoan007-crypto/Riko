# RIKO — Plan 6: Favorites + Playlist Experience Migration

> **Status:** 🟢 Code complete — build verify pending (Plans 1–6 combined)
> **Scope:** Migrate the Favorites + Playlist user experiences into the
> Riko Premium UI while preserving all existing data, navigation, playback,
> persistence, and business behavior.
> **Principle:** "হাড় থাকবে, চামড়া পুরো নতুন" — Engine/Reducer/Runtime/Media3/
> Room schema/playback layers untouched. Reuse, never redefine, inherited systems.

---

## 1. Audit summary (authoritative logic preserved)

| Concern        | Source                                        | Plan 6 action          |
|----------------|-----------------------------------------------|------------------------|
| Favorites route| `RikoDestination.Favorites` → `favorites`     | Reused (bottom-nav tab)|
| Favorite source| `FavoriteDao` + `Song.isFavorite` (real filter)| Reused — no dup storage|
| Favorites VM   | `FavoritesViewModel.uiState` + `toggleFavorite`| Reused               |
| Playlists route| `RikoDestination.Playlists` → `playlists`     | Reused (bottom-nav tab)|
| Playlist repo  | `PlaylistRepository` (Room-backed)            | Reused               |
| create/edit/del| `createPlaylist`/`renamePlaylist`/`deletePlaylist` | Reused            |
| add/remove song| `addSongsToPlaylist`/`removeSongFromPlaylist` | Reused               |
| reorder        | `reorderPlaylistSongs` (persisted)            | Reused (new up/down UI)|
| coverUri       | `Playlist.coverUri`                           | Reused exactly        |
| selection      | `AddSongsSheet` multi-select                  | Reused — not duplicated|
| sort/search    | existing routes/repos only                    | No new search/sort arch|

**No new** Favorites DB, Playlist DB, duplicate ViewModel, or duplicate repository.

---

## 2. What changed

### Favorites (`FavoritesScreen.kt`, `CollectionTrackRow.kt`)
- **Metadata** (Plan 6 §3): real count **and** duration computed from the live
  song list (`N songs • Xm` / `Nh Ym`) — never fabricated.
- **Empty state** (§8): "No favorite songs yet" + **Browse Music** → existing
  My Music route (`onBrowse`).
- **Favorite toggle pop** (§7): subtle `0.96 → 1.0` scale on the shared heart
  (`RikoCollectionTrackRow`) — restrained, no explosion.
- Favorites now sits fully on Plan 5's `RikoCollectionDetail` framework (§37).

### Playlists index (`PlaylistsScreen.kt`) — **migrated to Riko**
- Header + prominent-but-compact **Add New Playlist CTA** (§12) — glass card,
  not a 200dp banner.
- Single-column card list (§14) — recognition + access priority, no forced grid.
- Cards show **real `coverUri`** when set; deterministic fallback otherwise
  (Favorites → heart icon; else music-note placeholder) — no random art (§13, §25).
- Empty state (§28): "No playlists yet" + primary **Create Playlist**.
- Shell-aware bottom padding (`LocalRikoShellMetrics`), stable `playlist.id` keys.

### Create flow (§16–17)
- Compact **Riko bottom sheet** (`ModalBottomSheet` + `GlassBottomSheetContainer`)
  with a single name field + `[Cancel] [Create]` (Riko buttons). Keyboard/IME
  handled (`ImeAction.Done`). Creates via `PlaylistsViewModel.createPlaylist`
  (Room), then navigates into the new playlist (`onCreated`).

### Edit / Delete / Reorder (§18–23)
- **PlaylistDetail** (already on Plan 5 framework) gains a **More** action →
  bottom-sheet with **Rename** (existing `renamePlaylist`) and **Delete**
  confirmation (§19: "This cannot be undone." `[Cancel] [Delete]`).
- **Delete never touches source files** — only `PlaylistRepository.deletePlaylist`.
- **Reorder** (§22–23): edit-mode **up/down** controls per row →
  new `PlaylistDetailViewModel.moveSongUp/moveSongDown` → existing persisted
  `reorderPlaylistSongs`. Playlist order stays user-defined, never auto-
  alphabetized (§33).

---

## 3. Semantic distinction (Favorites ≠ Playlist, §24)

- **Favorites** = automatic preference collection (heart-driven).
- **Playlist** = explicitly ordered, user-curated collection.
Shared visual components; shared meaning is intentionally not merged.

---

## 4. Design-contract compliance

- ✅ No fake data — every number/art derived from real repositories.
- ✅ Reuses `RikoSongRow`, `RikoArtwork`/artwork pattern — no duplicate rows/cards.
- ✅ Shell metric bottom padding; single `LazyColumn`; stable keys (`playlist.id`, `song.id`).
- ✅ Playback = composition of existing `PlayerViewModel` actions only.
- ✅ No Media3 / playback / queue / Room schema changes.
- ✅ No global-shell changes — consumes Plan 2 shell (ambient, top bar, mini player, bottom nav).

---

## 5. Verification status

- Brace-balance depth check: **pass** across all 6 Plan 6 files + NavGraph.
- Confirmed icons (`Add`, `Edit`, `DeleteOutline`, `MoreVert`, `KeyboardArrowDown`,
  `PlaylistAdd`, `Close`, `Favorite*`) and tokens (`RikoError`, `RikoFavoriteActive`,
  `RikoPurple`, `RikoSpacing.*`, `RikoCornerRadius.*`, `RikoTypography.*`).
- `buildList`/`sumOf` used exactly as existing files do (no import needed).
- ❌ **Build not yet run** — no JDK/Android SDK on the dev machine.
  **Action:** run `./gradlew assembleDebug` (Plans 1–6 combined) and report
  errors / screenshots.

---

## 6. Final checklist

- [x] Favorites visually migrated + real data + toggle + metadata
- [x] Favorites empty/loading/error (Browse Music CTA)
- [x] Playlist index migrated (header, New Playlist CTA, real coverUri cards, empty)
- [x] Create playlist (compact Riko sheet → Room, navigate in)
- [x] Rename (edit sheet → existing `renamePlaylist`)
- [x] Delete (explicit confirm → existing `deletePlaylist`, never files)
- [x] Add songs (existing `AddSongsSheet`, not duplicated)
- [x] Remove songs (existing `removeSongFromPlaylist`)
- [x] Reorder (edit-mode up/down → existing persisted reorder)
- [x] Playlist detail + Favorites detail on Plan 5 framework
- [x] Back-stack: create → detail; delete → pop to index; favorites → My Music
- [ ] Build verification (`assembleDebug`, Plans 1–6) — **user**
- [ ] On-device QA (sheets, reorder, cards, long titles, large playlists)