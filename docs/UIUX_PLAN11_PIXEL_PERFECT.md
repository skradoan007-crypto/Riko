# UI/UX Plan 11: Pixel-Perfect UI Reflection

## Status: 🔵 IN PROGRESS (Design Tokens Updated)

### Completed Updates

#### 1. Color Tokens (RikoColor.kt) ✅
Updated all colors to match reference images exactly:
- Background: #090717 → #0E0920 (gradient)
- Surface: #18122B (alpha 0.65f)
- Inactive glass: #1F1838 (border #2E2550)
- Primary accent: #8E24EC / #A100FF
- Ambient: #7A1EFA, #9D26EB
- Text primary: #FFFFFF
- Text secondary: #8E8B9E
- Nav capsule: #1B1333 (alpha 0.85f)
- Nav active: #8A22EB
- Active toggle: #8C22EC

#### 2. Dimension Tokens (RikoDimensions.kt) ✅
- Bottom nav capsule: 72dp height
- Play container hero: 64dp (large white circle)
- Artwork hero: 280dp (CircleShape)
- Artwork favorites hero: 200dp (CircleShape)
- Corner radius: MD=16dp, XL=24dp

#### 3. Floating Capsule Bottom Navigation ✅
- Updated to CircleShape capsule
- Background: RikoNavCapsule (#1B1333 alpha 0.85)
- Active tab: RikoNavCapsuleActive (#8A22EB) pill behind icon
- Horizontal padding: 24dp
- Label only shown for active tab

#### 4. Artwork Component ✅
- Hero size now uses CircleShape (was RikoShapes.Hero)
- Added FavoritesHero size (200dp, CircleShape)
- Thumbnail/Shelf/Medium/Large remain rounded

### Remaining Work

1. **Home Screen**: Category chips, featured banner, recently played
2. **Favourites Screen**: Circular hero, dual pill CTAs (Shuffle/Play)
3. **My Music Screen**: Sub-nav chips, artist shelf
4. **Playlist Screen**: "Add New Playlist" capsule card
5. **Now Playing Screen**: Circular hero with purple aura, waveform, white play button
6. **Settings Screen**: Premium banner, glass capsule items
7. **Context Bottom Sheet**: Dark glass sheet with pill menu options
8. **Search Screen**: Verify glass search field matches reference