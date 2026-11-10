# RIKO — Plan 5: Collection Detail System

> **Status:** 🟢 Code complete — build verify pending
> **Scope:** One shared, premium collection-detail framework for Album / Artist /
> Playlist / Favorites / Folder, plus route + navigation wiring.
> **Principle:** "হাড় থাকবে, চামড়া পুরো নতুন" — Engine/Reducer/Runtime/Media3/
> Room/playback layers untouched. Presentation-only rebuild.

---

## 1. Goal

Every collection screen used to be its own bespoke `GlassBackground` + custom
header + hand-rolled row. Plan 5 collapses them onto **one** framework so the
whole library feels like one product:

- **One hero** (correct identity treatment per collection type),
- **One action bar** (Play / Shuffle),
- **One track row**, **one empty/loading/error state set**,
- **One vertical scroll** with shell-metric bottom padding.

Read the same "হাড়/চামড়া" line as Plans 1–4: identity + business logic stay
in existing ViewModels / repositories; only the presentation moves.

---

## 2. New shared framework — `presentation/collection/`

| File | Responsibility |
|------|-----------------|
| `CollectionIdentity.kt` | Sealed identity model (title + metadata + type-specific art). Pure presentation model, zero business logic. |
| `RikoCollectionDetail.kt` | The shared container: compact header (← back · title), hero, action bar, single `LazyColumn`, shell-metric bottom padding. |
| `components/RikoCollectionHero.kt` | Five identity treatments: Album (square art) / Artist (circle) / Playlist (cover or fallback) / Favorites (heart) / Folder (icon). |
| `components/RikoCollectionActions.kt` | `[ Shuffle ] [ Play ]` — Play is the primary (accent) action. |
| `components/CollectionTrackRow.kt` | Wraps the one-and-only `RikoSongRow` with semantic track numbering + optional favorite toggle. No duplicate row system. |
| `components/CollectionStates.kt` | `CollectionSkeleton` + `CollectionEmptyState` for loading / empty / error. |
| `CollectionDetailViewModels.kt` | Album / Artist / Folder ViewModels (client-derived identities). |
| `CollectionDetailScreens.kt` | Thin Album / Artist / Folder detail screens over the framework. |

**Container contract:**
```
RikoCollectionDetail(identity, onPlay, modifier,
    onBack = null,        // null → no back button (tab pages like Favorites)
    onShuffle = null,     // null → Play-only bar
    content = LazyListScope receiver)
```

---

## 3. Identities (spec §5–§12)

| Type      | Hero treatment      | Notes |
|-----------|---------------------|-------|
| Album     | Large **square** art | Never circularized (spec §5) |
| Artist    | **Circle** avatar   | Artist ≠ album (spec §6) |
| Playlist  | Custom cover / Riko fallback (spec §9) |
| Favorites | Heart icon (spec §10) |
| Folder    | Light icon — **no** huge artwork hero (spec §11) |

Title + metadata lines render centered under the hero (spec §2).

---

## 4. Screens adapted onto the framework

### PlaylistDetailScreen (`presentation/playlist/PlaylistDetailScreen.kt`)
- Rebuilt on `RikoCollectionDetail` with a `PlaylistIdentity`.
- **Preserved business logic** (spec §33 — adapt/extract, not rewrite):
  - Add-songs **sheet** (pickableSongs filter, multi-select, confirm) — unchanged VM.
  - **Edit mode** — toggle bar + per-row remove (via `RikoSongRow` trailing).
  - Play / Shuffle — composition of existing `PlayerViewModel` actions only.
- Shared components reused: `RikoSongRow`, `RikoIconButton`, `RikoPrimaryButton`,
  `CollectionSkeleton`, `CollectionEmptyState`.

### FavoritesScreen (`presentation/favorites/FavoritesScreen.kt`)
- Tab-embedded → `onBack = null` (no back button; it is a top-level tab).
- `FavoritesIdentity` heart hero, favorite rows carry the toggle affordance.
- Play / Shuffle composition only.

---

## 5. Routes & navigation

### `RikoDestinations.kt` — new routes
```
album_detail/{albumId}      (ARG_ALBUM_ID)
artist_detail/{artistId}    (ARG_ARTIST_ID)
folder_detail/{folderPath}  (ARG_FOLDER_PATH, URI-encoded)
```

### `RikoNavGraph.kt`
- Three new `composable` fragments → `AlbumDetailScreen` / `ArtistDetailScreen`
  / `FolderDetailScreen` (with `onBack = popBackStack`, song tap → NowPlaying,
  artist album tap → AlbumDetail).
- **Home** album/artist taps now **navigate** to detail (previously `playQueue`).
- **My Music** album/artist/folder taps navigate to detail (previously `playQueue`).

> Intentional: tapping a collection **card** opens its detail; the row's Play /
> Shuffle inside detail runs playback. Direct play-from-list is unchanged on
> song rows.

---

## 6. Design-contract compliance

- ✅ No fake/invented data — every identity is derived from existing repos.
- ✅ Single `RikoSongRow`/`RikoArtwork` — no duplicate row/card system.
- ✅ Shell-metric bottom padding (never hardcoded) via `LocalRikoShellMetrics`.
- ✅ Stable list keys (`song.id`), single `LazyColumn` per screen, no nested scroll.
- ✅ Playback = composition of existing `PlayerViewModel`/repository actions only.

---

## 7. Verification

- Brace-balance depth check: **pass** across all Plan 5 files,
  `RikoNavGraph.kt` restored to depth 0.
- Import paths verified (`TextPrimary`, `LocalDynamicColors` → `.theme`;
  `GlassBottomSheetContainer` → `.presentation.components`).
- ❌ **Build not yet run** — no JDK/Android SDK on the dev machine.
  **Action:** run `./gradlew assembleDebug` (Plans 2–5 combined) and report
  errors / screenshots.

---

## 8. Final checklist

- [x] Shared framework (identity + container + hero/actions/track/states)
- [x] Album / Artist / Folder detail screens + ViewModels
- [x] PlaylistDetailScreen & FavoritesScreen adapted (logic preserved)
- [x] Routes + NavGraph wiring + Home/My Music navigation hookups
- [x] Brace-balance + import verification
- [x] PROJECT_STATUS.md changelog updated
- [ ] `assembleDebug` build (Plans 2–5 combined) — **user**
- [ ] On-device QA matrix (hero treatments, edit/add-songs, back stacks)