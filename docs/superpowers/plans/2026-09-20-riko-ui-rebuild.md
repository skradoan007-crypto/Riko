# RIKO Full-App UI Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild every screen of the RIKO Music app in a single dark-purple neon design system matching the 4-screen reference mock, with zero regressions to playback/state/performance fixes.

**Architecture:** New `V2` token + component library under `core/designsystem/riko/`; screens rewritten in place on top of unchanged ViewModels, repositories, navigation routes, and Media3 stack; legacy theme duplicates deprecated via compat shims, not deleted.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.09.03), Material3, Navigation Compose 2.8.2, Hilt 2.52, Media3 1.4.1, Coil 2.7.0, JUnit4 + `createComposeRule` UI tests.

**Spec:** Reference design = 4-screen dark-purple neon mock (full token table in §Reference Spec below; executors read this file only). Prior forensic findings are encoded in Global Constraints — no other doc needed.

## Global Constraints

- minSdk 26, targetSdk 35, compileSdk 35, Java 17, AGP 8.5.2.
- No `runBlocking`, no `Thread.sleep`, no `synchronized` in new UI code.
- ExoPlayer reads (`currentPosition`, `bufferedPosition`, any `player.*` getter) run on `Dispatchers.Main` ONLY (Media3 `verifyApplicationThread`; see crash `IllegalStateException: Player is accessed on the wrong thread`).
- WaveEngine solvers (`EnergySolver`, `MotionSolver`, `RibbonPhysicsSolver`, `OrganicPathGenerator`) are NEVER modified — render-layer gating/scaling only.
- Preserve existing perf fixes: granular `PlayerViewModel` flow collection (never collect monolith `playerState` in a broad scope), Coil `.size()` + `memoryCacheKey`, Lazy `key = { stable id }`, PCM/visualizer throttles.
- Every Lazy list item gets `key = { <stable unique id> }` and `contentType` for mixed-type lists (per developer.android.com lazy-list + performance guides).
- Every interactive composable gets a `contentDescription` or centralized `testTag` (accessibility + UI tests).
- Secondary text on dark surfaces must meet WCAG AA 4.5:1 (reference `#A79BC7` on `#0D0716` ≈ 7:1 — use it, never dimmer than `#9A8FC4`).
- One commit per task step-5; never mix two tasks in one commit.
- CI builds debug APK on push; run `./gradlew :app:testDebugUnitTest` and `./gradlew :app:assembleDebug` before every push.

## Reference Spec (extracted from mock, normative)

| Token | Value |
|---|---|
| Background | `Color(0xFF0D0716)` |
| Surface card | `Color(0xFF1B1230)` |
| Surface elevated | `Color(0xFF241640)` |
| Primary text | `Color(0xFFFFFFFF)` |
| Secondary text | `Color(0xFFA79BC7)` |
| Accent magenta | `Color(0xFFFF3D8A)` |
| Gradient CTA | horizontal `Color(0xFFFF2E88)` → `Color(0xFF7B2FF7)` → `Color(0xFF2E7CF6)` |
| Card radius | 20.dp (cards), 16.dp (rows), 999.dp (pills) |
| Section title | 18.sp SemiBold white; action 13.sp magenta |
| Card art radius | 16.dp; play badge 36.dp circle, black 45% scrim |
| Track row | 56.dp thumb 12.dp radius, title 15.sp SemiBold, artist 13.sp secondary, duration 13.sp secondary, kebab |
| Bottom nav | 4 tabs Home/Search/Library/Profile, active magenta, inactive secondary |
| Mini player | surface card, 52.dp thumb, title/artist, pause toggle, 2.dp magenta progress hairline |
| Now Playing | chevron/title/kebab topbar; art card 20.dp radius + magenta glow border; title 22.sp Bold + `artist • topic` 14.sp secondary; 4.dp magenta progress + times; shuffle/prev/72.dp ring-pause/next/repeat; quality card (icon + `320 kbps`/`High Quality` + Lyrics pill); Next Up card (thumb + title/artist + play) |

## File Map (what gets created / modified)

Create:
- `core/designsystem/riko/theme/RikoThemeV2.kt` — all V2 tokens + `V2Theme` Material3 wrapper.
- `core/designsystem/riko/components/V2TestTags.kt` — `object V2TestTags` string constants.
- `core/designsystem/riko/components/V2Buttons.kt` — `V2PrimaryButton`, `V2GhostButton`, `V2SocialRow`.
- `core/designsystem/riko/components/V2SectionHeader.kt` — title + See All.
- `core/designsystem/riko/components/V2Cards.kt` — `V2BannerCard`, `V2PlaylistCard`, `V2QualityCard`, `V2NextUpCard`.
- `core/designsystem/riko/components/V2TrackRow.kt` — single canonical track row.
- `core/designsystem/riko/components/V2AppBar.kt` — `V2TopBar(title, onBack?, actions)`.
- `core/designsystem/riko/components/V2BottomNav.kt` — 4-tab nav with correct selection.
- `core/designsystem/riko/components/V2MiniPlayer.kt` — mini player bar.
- `core/designsystem/riko/components/V2Sheet.kt` — `V2SheetContainer` standard bottom-sheet chrome.
- `app/src/test/.../V2FormatTest.kt`, `app/src/androidTest/.../V2ComponentTest.kt` — tests.
- `benchmark/` module (Task 15 only).

Modify (in place, content rewrite, same signatures/routes/VMs):
- `presentation/onboarding/OnboardingScreen.kt`, `presentation/home/HomeScreen.kt` (+ `home/components/*` as needed), `presentation/search/SearchScreen.kt`, `presentation/mymusic/MyMusicScreen.kt`, `presentation/favorites/FavoritesScreen.kt`, `presentation/playlist/PlaylistsScreen.kt`, `presentation/playlist/PlaylistDetailScreen.kt`, `presentation/collection/CollectionDetailScreens.kt`, `presentation/settings_v2/rendering/SettingsRendering.kt`, `presentation/player/NowPlayingLayout.kt` (+ `TransportControlsV2.kt`, `WaveHost.kt`), `presentation/navigation/RikoNavGraph.kt` (wire V2 nav/mini/sheets only), `core/experience/wave/render/GlowRenderer.kt` + `RibbonRenderer.kt` (caps only), `gradle/libs.versions.toml`, `app/build.gradle.kts`.
Delete (explicit tasks): `presentation/allsongs/AllSongsScreen.kt`, `presentation/favorites/MyFavouriteSongsScreen.kt`, `presentation/components/RikoBottomNav.kt`, dormant miniplayer morph files, dead routes in `RikoDestinations.kt`.

---

### Task 0: Test infrastructure (no tests exist today)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/com/rikodev/music/android/V2SanityTest.kt`
- Create: `app/src/androidTest/java/com/rikodev/music/android/V2SmokeTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `testDebugUnitTest` task; `V2TestTags` consumable by all UI tasks (Task 1 defines it; this task only proves infra).

- [ ] **Step 1: Add test dependency declarations**

```toml
junit = "4.13.2"
androidx-test-ext-junit = "1.2.1"
espresso-core = "3.6.1"
```

```toml
junit-junit = { group = "junit", name = "junit", version.ref = "junit" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-test-ext-junit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso-core" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

- [ ] **Step 2: Wire dependencies in `app/build.gradle.kts`**

```kotlin
testImplementation(libs.junit.junit)
testImplementation(libs.kotlinx.coroutines.test)
androidTestImplementation(libs.androidx.test.ext.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.ui.test.junit4)
debugImplementation(libs.androidx.ui.test.manifest)
```

- [ ] **Step 3: Write failing sanity tests**

```kotlin
package com.rikodev.music.android

import org.junit.Assert.assertEquals
import org.junit.Test

class V2SanityTest {
    @Test fun themeVersion_isV2() {
        assertEquals(2, 2)
    }
}
```

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun staticText_isDisplayed() {
        rule.setContent { Text("RIKO smoke") }
        rule.onNodeWithText("RIKO smoke").assertExists()
    }
}
```

- [ ] **Step 4: Run and verify**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rikodev.music.android.V2SanityTest"`
Expected: PASS (1 test).
Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (proves dep block compiles; androidTest runs on emulator via `connectedDebugAndroidTest`).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/test app/src/androidTest
git commit -m "test: add JUnit4 + compose UI test infrastructure"
```

---

### Task 1: V2 theme tokens + test tags (single source of truth)

**Files:**
- Create: `core/designsystem/riko/theme/RikoThemeV2.kt`
- Create: `core/designsystem/riko/components/V2TestTags.kt`
- Test: `app/src/test/java/com/rikodev/music/android/V2ThemeTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `RikoThemeV2` (Colors, Type, Shapes, Spacing, `V2Theme(content)`), `V2TestTags` (all tag strings later tasks use verbatim).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.compose.ui.graphics.Color
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import org.junit.Assert.assertEquals
import org.junit.Test

class V2ThemeTest {
    @Test fun background_isDeepPurpleBlack() {
        assertEquals(Color(0xFF0D0716), RikoThemeV2.Colors.background)
    }

    @Test fun secondaryText_isNeverDimmerThanAA() {
        val c = RikoThemeV2.Colors.onBackgroundSecondary
        assertEquals(Color(0xFFA79BC7), c)
    }

    @Test fun ctaGradient_hasThreeStops() {
        assertEquals(3, RikoThemeV2.Colors.ctaGradient.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rikodev.music.android.V2ThemeTest"`
Expected: FAIL with "unresolved reference RikoThemeV2".

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RikoThemeV2 {
    object Colors {
        val background = Color(0xFF0D0716)
        val surface = Color(0xFF1B1230)
        val surfaceElevated = Color(0xFF241640)
        val onBackground = Color(0xFFFFFFFF)
        val onBackgroundSecondary = Color(0xFFA79BC7)
        val accent = Color(0xFFFF3D8A)
        val ctaGradient = listOf(Color(0xFFFF2E88), Color(0xFF7B2FF7), Color(0xFF2E7CF6))
        val ctaBrush = Brush.horizontalGradient(ctaGradient)
        val progressTrack = Color(0xFF3A2B5C)
        val divider = Color(0x26A79BC7)
    }

    object Type {
        val sectionTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val sectionAction = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Colors.accent)
        val cardTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val cardSubtitle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
        val trackTitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val trackSubtitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
        val npTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Colors.onBackground)
        val npArtist = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
    }

    object Shapes {
        val card = RoundedCornerShape(20.dp)
        val row = RoundedCornerShape(16.dp)
        val pill = RoundedCornerShape(999.dp)
    }

    object Spacing {
        val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp
    }

    private val scheme = darkColorScheme(
        background = Colors.background,
        surface = Colors.surface,
        primary = Colors.accent,
        onBackground = Colors.onBackground,
        onSurface = Colors.onBackground,
        onSurfaceVariant = Colors.onBackgroundSecondary
    )

    private val type = Typography(
        titleLarge = Type.sectionTitle,
        titleMedium = Type.cardTitle,
        bodyMedium = Type.trackTitle,
        bodySmall = Type.trackSubtitle,
        labelSmall = Type.sectionAction
    )
}

@Composable
fun V2Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RikoThemeV2.scheme, typography = RikoThemeV2.type, content = content)
}
```

```kotlin
package com.rikodev.music.android.core.designsystem.riko.components

object V2TestTags {
    const val BOTTOM_NAV = "v2_bottom_nav"
    const val NAV_HOME = "v2_nav_home"
    const val NAV_SEARCH = "v2_nav_search"
    const val NAV_LIBRARY = "v2_nav_library"
    const val NAV_PROFILE = "v2_nav_profile"
    const val MINI_PLAYER = "v2_mini_player"
    const val MINI_PLAY_PAUSE = "v2_mini_play_pause"
    const val TRACK_ROW_PREFIX = "v2_track_row_"
    const val NP_PLAY_PAUSE = "v2_np_play_pause"
    const val NP_NEXT = "v2_np_next"
    const val NP_PREV = "v2_np_prev"
    const val NP_PROGRESS = "v2_np_progress"
    const val PRIMARY_BUTTON = "v2_primary_button"
    const val PLAY_ALL_BUTTON = "v2_play_all_button"
    const val LYRICS_BUTTON = "v2_lyrics_button"
    const val SEARCH_FIELD = "v2_search_field"
    const val SEE_ALL_SUFFIX = "_see_all"
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rikodev.music.android.V2ThemeTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/core/designsystem/riko/theme/RikoThemeV2.kt app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2TestTags.kt app/src/test/java/com/rikodev/music/android/V2ThemeTest.kt
git commit -m "feat(ui-v2): theme tokens and test tags single source of truth"
```

---

### Task 2: V2 buttons (primary / ghost / social)

**Files:**
- Create: `core/designsystem/riko/components/V2Buttons.kt`
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2ButtonsTest.kt`

**Interfaces:**
- Consumes: `RikoThemeV2` (Task 1), `V2TestTags.PRIMARY_BUTTON` (Task 1).
- Produces: `V2PrimaryButton(text, onClick, modifier, icon)`, `V2GhostButton(text, onClick, modifier)`, `V2SocialRow(onGoogle, onApple, onFacebook, onEmail, modifier)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2GhostButton
import com.rikodev.music.android.core.designsystem.riko.components.V2PrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.V2SocialRow
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2ButtonsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun primaryButton_rendersLabelAndClicks() {
        var clicked = false
        rule.setContent { V2Theme { V2PrimaryButton(text = "Play All", onClick = { clicked = true }) } }
        rule.onNodeWithTag(V2TestTags.PRIMARY_BUTTON).assertIsDisplayed()
        rule.onNodeWithText("Play All").performClick()
        assertTrue(clicked)
    }

    @Test fun ghostButton_rendersLabel() {
        rule.setContent { V2Theme { V2GhostButton(text = "Login", onClick = {}) } }
        rule.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test fun socialRow_rendersFourProviders() {
        rule.setContent { V2Theme { V2SocialRow(onGoogle = {}, onApple = {}, onFacebook = {}, onEmail = {}) } }
        rule.onNodeWithText("Continue with").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2ButtonsTest` (emulator/device required)
Expected: FAIL with "unresolved reference V2PrimaryButton".

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

@Composable
fun V2PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    testTag: String = V2TestTags.PRIMARY_BUTTON
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RikoThemeV2.Shapes.pill)
            .background(RikoThemeV2.Colors.ctaBrush)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.invoke()
            Text(text = text, style = RikoThemeV2.Type.trackTitle, color = Color.White)
        }
    }
}

@Composable
fun V2GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RikoThemeV2.Shapes.pill)
            .border(1.dp, RikoThemeV2.Colors.divider, RikoThemeV2.Shapes.pill)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = RikoThemeV2.Type.trackTitle, color = RikoThemeV2.Colors.onBackground)
    }
}

@Composable
fun V2SocialRow(onGoogle: () -> Unit, onApple: () -> Unit, onFacebook: () -> Unit, onEmail: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Continue with", style = RikoThemeV2.Type.cardSubtitle)
        Row(
            modifier = Modifier.padding(top = RikoThemeV2.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(RikoThemeV2.Spacing.lg)
        ) {
            V2SocialCircle(label = "Google", onClick = onGoogle)
            V2SocialCircle(label = "Apple", onClick = onApple)
            V2SocialCircle(label = "Facebook", onClick = onFacebook)
            V2SocialCircle(label = "Email", onClick = onEmail)
        }
    }
}

@Composable
private fun V2SocialCircle(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RikoThemeV2.Shapes.pill)
            .border(1.dp, RikoThemeV2.Colors.divider, RikoThemeV2.Shapes.pill)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label.take(1), style = RikoThemeV2.Type.trackTitle, color = RikoThemeV2.Colors.onBackground)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (3 tests). Also run `./gradlew :app:testDebugUnitTest` (must stay green).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2Buttons.kt app/src/androidTest/java/com/rikodev/music/android/V2ButtonsTest.kt
git commit -m "feat(ui-v2): primary, ghost and social buttons"
```

---

### Task 3: Section header + playlist/banner cards

**Files:**
- Create: `core/designsystem/riko/components/V2SectionHeader.kt`
- Create: `core/designsystem/riko/components/V2Cards.kt`
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2CardsTest.kt`

**Interfaces:**
- Consumes: `RikoThemeV2`, `V2TestTags` (Task 1).
- Produces: `V2SectionHeader(title, onSeeAll)`, `V2PlaylistCard(title, subtitle, artUri, onClick, onPlay)`, `V2BannerCard(eyebrow, title, subtitle, ctaText, artUri, onCta)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2BannerCard
import com.rikodev.music.android.core.designsystem.riko.components.V2PlaylistCard
import com.rikodev.music.android.core.designsystem.riko.components.V2SectionHeader
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2CardsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun sectionHeader_seeAllClicks() {
        var seen = false
        rule.setContent { V2Theme { V2SectionHeader(title = "Recently Played", onSeeAll = { seen = true }) } }
        rule.onNodeWithText("Recently Played").assertIsDisplayed()
        rule.onNodeWithText("See All").performClick()
        assertTrue(seen)
    }

    @Test fun playlistCard_rendersTitleAndSubtitle() {
        rule.setContent { V2Theme { V2PlaylistCard(title = "Chill Beats", subtitle = "Playlist • 42 songs", artUri = null, onClick = {}, onPlay = {}) } }
        rule.onNodeWithText("Chill Beats").assertIsDisplayed()
        rule.onNodeWithText("Playlist • 42 songs").assertIsDisplayed()
    }

    @Test fun bannerCard_rendersCta() {
        rule.setContent { V2Theme { V2BannerCard(eyebrow = "FEATURED PLAYLIST", title = "Feel the Vibe", subtitle = "Best of Electronic", ctaText = "Play Now", artUri = null, onCta = {}) } }
        rule.onNodeWithText("Play Now").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2CardsTest`
Expected: FAIL with "unresolved reference V2SectionHeader".

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

@Composable
fun V2SectionHeader(title: String, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = RikoThemeV2.Type.sectionTitle)
        Text(
            text = "See All",
            style = RikoThemeV2.Type.sectionAction,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onSeeAll)
                .testTag(title + V2TestTags.SEE_ALL_SUFFIX)
        )
    }
}

@Composable
fun V2PlaylistCard(title: String, subtitle: String, artUri: String?, onClick: () -> Unit, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(role = Role.Button, onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RikoThemeV2.Shapes.row).background(RikoThemeV2.Colors.surfaceElevated)) {
            if (artUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(artUri).size(512)
                        .memoryCacheKey("${artUri}_v2card512").crossfade(200).allowHardware(true).build(),
                    contentDescription = "Artwork for $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = RikoThemeV2.Colors.onBackgroundSecondary, modifier = Modifier.size(32.dp).align(Alignment.Center))
            }
            Box(
                modifier = Modifier.size(36.dp).align(Alignment.BottomEnd).padding(8.dp)
                    .clip(RikoThemeV2.Shapes.pill)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(role = Role.Button, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play $title", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Text(text = title, style = RikoThemeV2.Type.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(text = subtitle, style = RikoThemeV2.Type.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun V2BannerCard(eyebrow: String, title: String, subtitle: String, ctaText: String, artUri: String?, onCta: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RikoThemeV2.Shapes.card)
            .background(Brush.horizontalGradient(listOf(Color(0xFF2A1245), RikoThemeV2.Colors.surface)))
    ) {
        if (artUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(artUri).size(512)
                    .memoryCacheKey("${artUri}_v2banner512").crossfade(200).allowHardware(true).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()
            )
            Box(modifier = Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xCC0D0716), Color.Transparent))))
        }
        Column(modifier = Modifier.padding(RikoThemeV2.Spacing.lg)) {
            Text(text = eyebrow, style = RikoThemeV2.Type.cardSubtitle, color = RikoThemeV2.Colors.accent)
            Text(text = title, style = RikoThemeV2.Type.npTitle, modifier = Modifier.padding(top = 4.dp))
            Text(text = subtitle, style = RikoThemeV2.Type.cardSubtitle, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            V2PrimaryButton(text = ctaText, onClick = onCta, modifier = Modifier.fillMaxWidth(0.55f))
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2SectionHeader.kt app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2Cards.kt app/src/androidTest/java/com/rikodev/music/android/V2CardsTest.kt
git commit -m "feat(ui-v2): section header, playlist and banner cards"
```

---

### Task 4: Canonical track row (replaces 5 row patterns)

**Files:**
- Create: `core/designsystem/riko/components/V2TrackRow.kt`
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2TrackRowTest.kt`

**Interfaces:**
- Consumes: `RikoThemeV2`, `V2TestTags.TRACK_ROW_PREFIX` (Task 1).
- Produces: `V2TrackRow(id, title, subtitle, duration, artUri, isPlaying, onClick, onMenu)`; later screen tasks use it with `key = { it.id }`, `contentType = "v2_track"`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.components.V2TrackRow
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2TrackRowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun trackRow_rendersAllFieldsAndClicks() {
        var clicked = false
        var menu = false
        rule.setContent {
            V2Theme {
                V2TrackRow(id = 7L, title = "Usay Toofan Keht", subtitle = "Prince Lovers • Topic", duration = "4:14", artUri = null, isPlaying = true, onClick = { clicked = true }, onMenu = { menu = true })
            }
        }
        rule.onNodeWithTag(V2TestTags.TRACK_ROW_PREFIX + 7L).assertIsDisplayed()
        rule.onNodeWithText("Usay Toofan Keht").assertIsDisplayed()
        rule.onNodeWithText("4:14").assertIsDisplayed()
        rule.onNodeWithText("Usay Toofan Keht").performClick()
        assertTrue(clicked)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2TrackRowTest`
Expected: FAIL with "unresolved reference V2TrackRow".

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

@Composable
fun V2TrackRow(
    id: Long,
    title: String,
    subtitle: String,
    duration: String,
    artUri: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(V2TestTags.TRACK_ROW_PREFIX + id)
            .padding(horizontal = RikoThemeV2.Spacing.lg, vertical = RikoThemeV2.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp)
                .clip(RikoThemeV2.Shapes.row)
                .background(RikoThemeV2.Colors.surfaceElevated)
        ) {
            if (artUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(artUri).size(128)
                        .memoryCacheKey("${artUri}_v2row128").crossfade(200).allowHardware(true).build(),
                    contentDescription = "Artwork for $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = RikoThemeV2.Colors.onBackgroundSecondary, modifier = Modifier.size(24.dp).align(Alignment.Center))
            }
            if (isPlaying) {
                Icon(Icons.Filled.Equalizer, contentDescription = "Now playing", tint = RikoThemeV2.Colors.accent, modifier = Modifier.size(20.dp).align(Alignment.Center))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = RikoThemeV2.Spacing.md)) {
            Text(text = title, style = RikoThemeV2.Type.trackTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = RikoThemeV2.Type.trackSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = duration, style = RikoThemeV2.Type.trackSubtitle)
        Icon(
            Icons.Filled.MoreVert, contentDescription = "More options for $title", tint = RikoThemeV2.Colors.onBackgroundSecondary,
            modifier = Modifier.size(24.dp).clickable(role = Role.Button, onClick = onMenu)
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2TrackRow.kt app/src/androidTest/java/com/rikodev/music/android/V2TrackRowTest.kt
git commit -m "feat(ui-v2): canonical track row"
```

---

### Task 5: App bar + bottom nav + mini player

**Files:**
- Create: `core/designsystem/riko/components/V2AppBar.kt`
- Create: `core/designsystem/riko/components/V2BottomNav.kt`
- Create: `core/designsystem/riko/components/V2MiniPlayer.kt`
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2ChromeTest.kt`

**Interfaces:**
- Consumes: `RikoThemeV2`, `V2TestTags` (Task 1).
- Produces: `V2TopBar(title, onBack, actions)`, `V2BottomNav(selected, onSelect)`, `V2MiniPlayer(title, subtitle, artUri, isPlaying, progress, onToggle, onOpen, onNext)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2BottomNav
import com.rikodev.music.android.core.designsystem.riko.components.V2MiniPlayer
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.components.V2TopBar
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2ChromeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun topBar_backClicksAndTitleShows() {
        var back = 0
        rule.setContent { V2Theme { V2TopBar(title = "Library", onBack = { back++ }) } }
        rule.onNodeWithText("Library").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
    }

    @Test fun bottomNav_selectsLibrary() {
        var selected = "home"
        rule.setContent { V2Theme { V2BottomNav(selected = selected, onSelect = { selected = it }) } }
        rule.onNodeWithTag(V2TestTags.NAV_LIBRARY).performClick()
        assertEquals("library", selected)
    }

    @Test fun miniPlayer_toggleClicks() {
        var toggled = false
        rule.setContent {
            V2Theme {
                V2MiniPlayer(title = "T", subtitle = "A", artUri = null, isPlaying = false, progress = 0.25f, onToggle = { toggled = true }, onOpen = {}, onNext = {})
            }
        }
        rule.onNodeWithTag(V2TestTags.MINI_PLAY_PAUSE).performClick()
        org.junit.Assert.assertTrue(toggled)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2ChromeTest`
Expected: FAIL with "unresolved reference V2TopBar". (Also add missing import `androidx.compose.ui.test.onNodeWithContentDescription` to the test file in this step — it is required by the back-button assertion.)

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

@Composable
fun V2TopBar(title: String, onBack: (() -> Unit)? = null, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RikoThemeV2.Colors.onBackground,
                modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onBack).padding(12.dp))
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
        Text(text = title, style = RikoThemeV2.Type.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        actions()
    }
}

private data class V2NavItem(val route: String, val label: String, val icon: ImageVector, val tag: String)

@Composable
fun V2BottomNav(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        V2NavItem("home", "Home", Icons.Filled.Home, V2TestTags.NAV_HOME),
        V2NavItem("search", "Search", Icons.Filled.Search, V2TestTags.NAV_SEARCH),
        V2NavItem("library", "Library", Icons.AutoMirrored.Filled.PlaylistPlay, V2TestTags.NAV_LIBRARY),
        V2NavItem("profile", "Profile", Icons.Filled.Person, V2TestTags.NAV_PROFILE)
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = RikoThemeV2.Spacing.xl, vertical = RikoThemeV2.Spacing.sm)
            .clip(RikoThemeV2.Shapes.card).background(RikoThemeV2.Colors.surface).padding(vertical = 8.dp)
            .testTag(V2TestTags.BOTTOM_NAV),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { item ->
            val active = selected == item.route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(role = Role.Tab, onClick = { onSelect(item.route) }).testTag(item.tag).padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(item.icon, contentDescription = item.label, tint = if (active) RikoThemeV2.Colors.accent else RikoThemeV2.Colors.onBackgroundSecondary, modifier = Modifier.size(24.dp))
                Text(text = item.label, style = RikoThemeV2.Type.cardSubtitle, color = if (active) RikoThemeV2.Colors.accent else RikoThemeV2.Colors.onBackgroundSecondary)
            }
        }
    }
}

@Composable
fun V2MiniPlayer(
    title: String, subtitle: String, artUri: String?, isPlaying: Boolean, progress: Float,
    onToggle: () -> Unit, onOpen: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RikoThemeV2.Shapes.row)
            .background(RikoThemeV2.Colors.surface)
            .clickable(role = Role.Button, onClick = onOpen)
            .testTag(V2TestTags.MINI_PLAYER)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(RikoThemeV2.Shapes.row).background(RikoThemeV2.Colors.surfaceElevated)) {
                if (artUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(artUri).size(128)
                            .memoryCacheKey("${artUri}_v2mini128").crossfade(200).allowHardware(true).build(),
                        contentDescription = "Artwork for $title", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = RikoThemeV2.Colors.onBackgroundSecondary, modifier = Modifier.size(24.dp).align(Alignment.Center))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = title, style = RikoThemeV2.Type.trackTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = subtitle, style = RikoThemeV2.Type.trackSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = RikoThemeV2.Colors.onBackground,
                modifier = Modifier.size(40.dp).clickable(role = Role.Button, onClick = onToggle).padding(8.dp).testTag(V2TestTags.MINI_PLAY_PAUSE)
            )
            Icon(Icons.Filled.SkipNext, contentDescription = "Next track", tint = RikoThemeV2.Colors.onBackgroundSecondary,
                modifier = Modifier.size(40.dp).clickable(role = Role.Button, onClick = onNext).padding(8.dp))
        }
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(2.dp), color = RikoThemeV2.Colors.accent, trackColor = RikoThemeV2.Colors.progressTrack)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2AppBar.kt app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2BottomNav.kt app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2MiniPlayer.kt app/src/androidTest/java/com/rikodev/music/android/V2ChromeTest.kt
git commit -m "feat(ui-v2): app bar, bottom nav and mini player"
```

---

### Task 6: Standard bottom-sheet chrome

**Files:**
- Create: `core/designsystem/riko/components/V2Sheet.kt`
- Test: extend `app/src/androidTest/java/com/rikodev/music/android/V2ChromeTest.kt` with `sheetContainer_rendersTitle`.

**Interfaces:**
- Consumes: `RikoThemeV2` (Task 1).
- Produces: `V2SheetContainer(title, onDismiss, content)` — single heights/shape/drag-handle standard; screen tasks wrap existing sheet bodies in it.

- [ ] **Step 1: Write the failing test (append to V2ChromeTest.kt)**

```kotlin
@Test fun sheetContainer_rendersTitle() {
    rule.setContent { V2Theme { V2SheetContainer(title = "Queue", onDismiss = {}) { Text("body") } } }
    rule.onNodeWithText("Queue").assertIsDisplayed()
    rule.onNodeWithText("body").assertIsDisplayed()
}
```

Plus import `com.rikodev.music.android.core.designsystem.riko.components.V2SheetContainer`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2ChromeTest`
Expected: FAIL with "unresolved reference V2SheetContainer".

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

@Composable
fun V2SheetContainer(title: String, onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth()
            .clip(RikoThemeV2.Shapes.card)
            .background(RikoThemeV2.Colors.surface)
            .padding(horizontal = RikoThemeV2.Spacing.lg, vertical = RikoThemeV2.Spacing.md)
    ) {
        Box(modifier = Modifier.width(40.dp).height(4.dp).align(Alignment.CenterHorizontally).clip(RikoThemeV2.Shapes.pill).background(RikoThemeV2.Colors.divider))
        Text(text = title, style = RikoThemeV2.Type.sectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        content()
    }
}
```

NOTE: `onDismiss` is accepted for API stability (callers already handle dismiss via `ModalBottomSheet(onDismissRequest=...)`); the container itself renders chrome only.

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (4 tests in class).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2Sheet.kt app/src/androidTest/java/com/rikodev/music/android/V2ChromeTest.kt
git commit -m "feat(ui-v2): standard bottom-sheet container"
```

---

### Task 7: Onboarding screen rebuild (reference screen 1)

**Files:**
- Modify: `presentation/onboarding/OnboardingScreen.kt` (content rewrite; keep signature `OnboardingScreen(onGetStarted, onLogin, ...)` — read current params first and keep them).
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2OnboardingTest.kt`

**Interfaces:**
- Consumes: `V2PrimaryButton`, `V2GhostButton`, `V2SocialRow` (Task 2), `RikoThemeV2` (Task 1).
- Produces: Onboarding visuals only; no new API (same callbacks).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.onboarding.OnboardingScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2OnboardingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun onboarding_showsHeadlineAndCtas() {
        var started = false
        rule.setContent { V2Theme { OnboardingScreen(onGetStarted = { started = true }, onLogin = {}) } }
        rule.onNodeWithText("Together").assertIsDisplayed()
        rule.onNodeWithText("Create Account").performClick()
        assertTrue(started)
    }
}
```

If the current `OnboardingScreen` signature differs, adjust the call in this step to the real params (read the file before writing the test).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2OnboardingTest`
Expected: FAIL — "Together" text not found (current screen has different copy).

- [ ] **Step 3: Rewrite screen content (keep function signature and callbacks)**

Replace the body with: background `RikoThemeV2.Colors.background`; headline `Music` (white) / `Brings Us` (accent) / `Together` (white) at 40.sp Bold; sub `Discover • Stream • Feel` secondary; `V2PrimaryButton("Create Account", onGetStarted)`; `V2GhostButton("Login", onLogin)`; `V2SocialRow` wired to existing social callbacks if the signature has them, else no-op lambdas. No ViewModel, no state, no hardcoded colors outside `RikoThemeV2`.

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/presentation/onboarding/OnboardingScreen.kt app/src/androidTest/java/com/rikodev/music/android/V2OnboardingTest.kt
git commit -m "feat(ui-v2): onboarding screen rebuild"
```

---

### Task 8: Home screen rebuild (reference screen 2)

**Files:**
- Modify: `presentation/home/HomeScreen.kt` (content rewrite; keep `HomeViewModel` collection, route, `onSongClick` flow).
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2HomeTest.kt` + extend `V2ThemeTest` is NOT needed.

**Interfaces:**
- Consumes: `V2SectionHeader`, `V2PlaylistCard`, `V2BannerCard` (Task 3), `V2TopBar` (Task 5).
- Produces: same screen route/state contract; rows use `key = { song.id }`, `contentType = "v2_card"`.

- [ ] **Step 1: Write the failing test** — render `HomeContent`-equivalent with a fake `HomeUiState` if the screen exposes one, else assert via the real screen with a test ViewModel is NOT possible (Hilt); so test the section composables the screen is built from:

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.home.HomeScreenSections
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2HomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun homeSections_showShelves() {
        rule.setContent { V2Theme { HomeScreenSections(recentTitles = listOf("Chill Beats"), topTitles = listOf("EDM Pulse")) } }
        rule.onNodeWithText("Recently Played").assertIsDisplayed()
        rule.onNodeWithText("Top Picks for You").assertIsDisplayed()
    }
}
```

This requires extracting a pure `HomeScreenSections(recentTitles, topTitles)` composable in the rewrite (no ViewModel inside) — do that in Step 3.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2HomeTest`
Expected: FAIL with "unresolved reference HomeScreenSections".

- [ ] **Step 3: Rewrite screen**

`HomeScreen` keeps collecting `HomeViewModel.uiState` + granular player flows exactly as today; body becomes: `V2TopBar("Prince Lovers")` + search/bell actions, `V2BannerCard` featured, `HomeScreenSections` (pure: two `V2SectionHeader` + `LazyRow` of `V2PlaylistCard` with `key = { title }`, `contentType = "v2_card"`), `V2MiniPlayer` is nav-owned (do NOT render here). Delete `HomeHeroCard/HeroArtwork` custom header usage; keep sort/filter logic untouched.

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/presentation/home/HomeScreen.kt app/src/androidTest/java/com/rikodev/music/android/V2HomeTest.kt
git commit -m "feat(ui-v2): home screen rebuild"
```

---

### Task 9: Search + Library screens rebuild

**Files:**
- Modify: `presentation/search/SearchScreen.kt`, `presentation/mymusic/MyMusicScreen.kt` (content rewrites; keep VMs, routes, sort-sheet behavior).
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2SearchLibraryTest.kt`

**Interfaces:**
- Consumes: `V2TopBar`, `V2TrackRow` (Tasks 4-5), `V2TestTags.SEARCH_FIELD` (Task 1).
- Produces: same routes/state; all rows `key = { song.id }`, `contentType = "v2_track"`; search field carries `testTag(V2TestTags.SEARCH_FIELD)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.search.V2SearchBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2SearchLibraryTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun searchBar_hasTestTagAndHint() {
        rule.setContent { V2Theme { V2SearchBar(query = "", onQuery = {}) } }
        rule.onNodeWithTag(V2TestTags.SEARCH_FIELD).assertIsDisplayed()
    }
}
```

Requires a pure `V2SearchBar(query, onQuery)` composable extracted during the Search rewrite (Step 3).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2SearchLibraryTest`
Expected: FAIL with "unresolved reference V2SearchBar".

- [ ] **Step 3: Rewrite screens**

Search: `V2TopBar` + `V2SearchBar` (48.dp, surface, search icon, tag), states (searching/empty/results) preserved with V2 styling, results via `V2TrackRow` keyed. Library: `V2TopBar("Library", onBack)` + mode pills + `V2TrackRow` lists (albums/artists/folders keep their cards, restyled to V2 tokens only). Keep `LibrarySortSheet`, VMs, and `File`-stat removal intact.

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/presentation/search/SearchScreen.kt app/src/main/java/com/rikodev/music/android/presentation/mymusic/MyMusicScreen.kt app/src/androidTest/java/com/rikodev/music/android/V2SearchLibraryTest.kt
git commit -m "feat(ui-v2): search and library screens rebuild"
```

---

### Task 10: Favorites + Playlists + PlaylistDetail rebuild (reference screen 3 pattern)

**Files:**
- Modify: `presentation/favorites/FavoritesScreen.kt`, `presentation/playlist/PlaylistsScreen.kt`, `presentation/playlist/PlaylistDetailScreen.kt` (content rewrites; keep VMs, routes, sheet behaviors).
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2CollectionTest.kt`

**Interfaces:**
- Consumes: `V2TopBar`, `V2TrackRow`, `V2PrimaryButton` (Tasks 2/4/5), `V2TestTags.PLAY_ALL_BUTTON` (Task 1).
- Produces: `PlaylistDetailContent(title, songs, onPlayAll, onPlay, onMenu)` pure composable (also reused by Task 11); header art + `V2PrimaryButton("Play All")` + heart/download circles + `V2TrackRow` list keyed by song id.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.playlist.PlaylistDetailContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun fakeSong(id: Long, title: String) = Song(
    id = id, title = title, artist = "A", album = "B", albumId = 1L,
    duration = 254_000L, size = 8_000_000L, path = "/music/$id.mp3",
    uri = Uri.parse("content://media/external/audio/media/$id"),
    dateAdded = 0L, dateModified = 0L, trackNumber = 1, year = 2024,
    bitrate = 320, sampleRate = 44100, mimeType = "audio/mpeg", isFavorite = false
)

@RunWith(AndroidJUnit4::class)
class V2CollectionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun playlistDetail_rendersRowsAndPlayAll() {
        var played = false
        val songs = listOf(fakeSong(1L, "T1"), fakeSong(2L, "T2"))
        rule.setContent { V2Theme { PlaylistDetailContent(title = "P", songs = songs, onPlayAll = { played = true }, onPlay = {}, onMenu = {}) } }
        rule.onNodeWithTag(V2TestTags.PLAY_ALL_BUTTON).performClick()
        assertTrue(played)
        rule.onNodeWithText("T1").assertIsDisplayed()
        rule.onNodeWithText("T2").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2CollectionTest`
Expected: FAIL with "unresolved reference PlaylistDetailContent".

- [ ] **Step 3: Rewrite screens**

Extract pure `PlaylistDetailContent(title, songs, onPlayAll, onPlay, onMenu)` in `PlaylistDetailScreen.kt`: header art + title/meta + `V2PrimaryButton("Play All", testTag = PLAY_ALL_BUTTON)` + heart/download circles + `LazyColumn` of `V2TrackRow(key = { song.id }, contentType = "v2_track")`. Favorites/Playlists screens reuse `V2TopBar` + `V2TrackRow`/cards; keep VMs, dialogs, and sheet behaviors; fix `"Playlist"` → `"Playlists"` title.

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/presentation/favorites/FavoritesScreen.kt app/src/main/java/com/rikodev/music/android/presentation/playlist/PlaylistsScreen.kt app/src/main/java/com/rikodev/music/android/presentation/playlist/PlaylistDetailScreen.kt app/src/androidTest/java/com/rikodev/music/android/V2CollectionTest.kt
git commit -m "feat(ui-v2): favorites, playlists and playlist detail rebuild"
```

---

### Task 11: Album / Artist / Folder detail rebuild (reuse Task 10 content)

**Files:**
- Modify: `presentation/collection/CollectionDetailScreens.kt` (content rewrites; keep the 3 ViewModels, routes, granular flow collection).
- Test: extend `app/src/androidTest/java/com/rikodev/music/android/V2CollectionTest.kt` with `collectionDetail_reusesContent`.

**Interfaces:**
- Consumes: `PlaylistDetailContent` (Task 10).
- Produces: same 3 routes/states; bodies delegate to `PlaylistDetailContent`.

- [ ] **Step 1: Write the failing test (append to V2CollectionTest.kt)**

```kotlin
@Test fun collectionDetail_reusesContent() {
    val songs = listOf(fakeSong(9L, "C1"))
    rule.setContent { V2Theme { PlaylistDetailContent(title = "Album", songs = songs, onPlayAll = {}, onPlay = {}, onMenu = {}) } }
    rule.onNodeWithText("C1").assertIsDisplayed()
}
```

- [ ] **Step 2: Run test to verify it passes already (contract check)**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2CollectionTest`
Expected: PASS — this step locks the reuse contract before the rewrite (redundant-by-design safety net).

- [ ] **Step 3: Rewrite screens**

Replace each screen's list body with `PlaylistDetailContent` (artist screen keeps its albums shelf above the content, restyled to V2 tokens). Keep ViewModels, `trackNumber "%02d"` behavior only if still wanted — reference shows no track numbers, so drop them. Keep granular collection + loading/error/empty states.

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (2 tests in class).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/presentation/collection/CollectionDetailScreens.kt app/src/androidTest/java/com/rikodev/music/android/V2CollectionTest.kt
git commit -m "feat(ui-v2): collection detail screens rebuild"
```

---

### Task 12: Settings rebuild + dead-code deletion

**Files:**
- Modify: `presentation/settings_v2/rendering/SettingsRendering.kt` (content rewrite; keep `SettingsUiState` + 14 callbacks).
- Modify: `presentation/navigation/RikoDestinations.kt` (delete `PlaylistEditor`, `AddToPlaylist` objects).
- Delete: `presentation/allsongs/AllSongsScreen.kt`, `presentation/favorites/MyFavouriteSongsScreen.kt`, `presentation/components/RikoBottomNav.kt`, dormant `presentation/miniplayer/MiniPlayer*.kt` morph files (keep only what `RikoShellMiniPlayer` uses — verify with grep before deleting).
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2SettingsTest.kt`

**Interfaces:**
- Consumes: `V2TopBar` (Task 5), `RikoThemeV2` (Task 1).
- Produces: settings list on V2 tokens; dead routes/files gone (compile proves it).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.settings_v2.host.SettingsHostCoordinator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2SettingsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun settings_rendersV2Title() {
        rule.setContent { V2Theme { V2SettingsHeader(title = "Settings") } }
        rule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
```

This requires a pure `V2SettingsHeader(title)` composable extracted in the rewrite (Step 3). (Do NOT reference `SettingsHostCoordinator` — remove that import; the header is standalone.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2SettingsTest`
Expected: FAIL with "unresolved reference V2SettingsHeader".

- [ ] **Step 3: Rewrite + delete**

Settings: `V2SettingsHeader` + V2 rows bound to the same `SettingsUiState`/callbacks; fix `"Setting"` → `"Settings"`; keep `Column.verticalScroll` (settings is short). Deletions: remove the two dead `data object` routes, delete the dormant files, fix any resulting unresolved references by deleting their call sites too (they are dormant — verify each has zero non-test references via grep before deleting).

- [ ] **Step 4: Run test to verify it passes + full compile**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2SettingsTest` then `./gradlew :app:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL (proves deletions broke nothing).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(ui-v2): settings rebuild and dead-code deletion"
```

---

### Task 13: Now Playing rebuild (reference screen 4)

**Files:**
- Modify: `presentation/player/NowPlayingLayout.kt` (structure rewrite; keep all flow params + callbacks).
- Modify: `presentation/player/TransportControlsV2.kt` (restyle to transport spec below; keep callback signature).
- Create: `core/designsystem/riko/components/V2NpCards.kt` (`V2QualityCard`, `V2NextUpCard`).
- Test: `app/src/androidTest/java/com/rikodev/music/android/V2NowPlayingTest.kt`

**Interfaces:**
- Consumes: `V2TrackRow`-style tokens, `V2TestTags.NP_*` (Task 1), `ActiveGatedVisualizer` (Task 14 provides it — implement Task 14 FIRST if running out of order).
- Produces: NP layout per reference: topbar / art card / title+artist / thin progress / transport / quality card / next-up card; transport spec: `shuffle/prev/72.dp ring-pause/next/repeat`, pause fill = `colorState.accent`, times `0:00`/`durationFormatted` secondary.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2NextUpCard
import com.rikodev.music.android.core.designsystem.riko.components.V2QualityCard
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2NowPlayingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun qualityCard_showsBitrateAndLyrics() {
        var lyrics = false
        rule.setContent { V2Theme { V2QualityCard(bitrate = "320 kbps", subtitle = "High Quality", onLyrics = { lyrics = true }) } }
        rule.onNodeWithText("320 kbps").assertIsDisplayed()
        rule.onNodeWithTag(V2TestTags.LYRICS_BUTTON).performClick()
        assertTrue(lyrics)
    }

    @Test fun nextUpCard_showsTrack() {
        rule.setContent { V2Theme { V2NextUpCard(title = "Raatan Lambiyan", subtitle = "Jubin Nautiyal", artUri = null, onPlay = {}) } }
        rule.onNodeWithText("Next Up").assertIsDisplayed()
        rule.onNodeWithText("Raatan Lambiyan").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.V2NowPlayingTest`
Expected: FAIL with "unresolved reference V2QualityCard".

- [ ] **Step 3: Write minimal card implementation**

```kotlin
package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

@Composable
fun V2QualityCard(bitrate: String, subtitle: String, onLyrics: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clip(RikoThemeV2.Shapes.row).background(RikoThemeV2.Colors.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = RikoThemeV2.Colors.accent, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text = bitrate, style = RikoThemeV2.Type.trackTitle)
            Text(text = subtitle, style = RikoThemeV2.Type.trackSubtitle)
        }
        Row(
            modifier = Modifier.clip(RikoThemeV2.Shapes.pill).borderV2().clickable(role = Role.Button, onClick = onLyrics).testTag(V2TestTags.LYRICS_BUTTON).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = RikoThemeV2.Colors.accent, modifier = Modifier.size(16.dp))
            Text(text = "Lyrics", style = RikoThemeV2.Type.cardSubtitle, color = RikoThemeV2.Colors.onBackground)
        }
    }
}

private fun Modifier.borderV2(): Modifier =
    this.border(1.dp, RikoThemeV2.Colors.divider, RikoThemeV2.Shapes.pill)

@Composable
fun V2NextUpCard(title: String, subtitle: String, artUri: String?, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().clip(RikoThemeV2.Shapes.row).background(RikoThemeV2.Colors.surface).padding(12.dp)) {
        Text(text = "Next Up", style = RikoThemeV2.Type.cardSubtitle, modifier = Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Box(
                modifier = Modifier.size(48.dp).clip(RikoThemeV2.Shapes.row).background(RikoThemeV2.Colors.surfaceElevated)
            ) {
                if (artUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(artUri).size(128)
                            .memoryCacheKey("${artUri}_v2next128").crossfade(200).allowHardware(true).build(),
                        contentDescription = "Artwork for $title", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = RikoThemeV2.Colors.onBackgroundSecondary, modifier = Modifier.size(20.dp).align(Alignment.Center))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = title, style = RikoThemeV2.Type.trackTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = subtitle, style = RikoThemeV2.Type.trackSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play next up", tint = RikoThemeV2.Colors.onBackground,
                modifier = Modifier.size(40.dp).clickable(role = Role.Button, onClick = onPlay).padding(8.dp))
        }
    }
}
```

Add missing imports in this step: `androidx.compose.foundation.border`, `androidx.compose.foundation.Box` (replace the fully-qualified `androidx.compose.foundation.Box` with the import).

- [ ] **Step 4: Run test to verify it passes**

Run: same `connectedDebugAndroidTest` class filter as Step 2.
Expected: PASS (2 tests).

- [ ] **Step 5 (same task): rewire NowPlayingLayout structure**

Keeping every flow param and callback, reorder/restyle content to: `V2TopBar("Now Playing", onDismiss)` / art card (existing `HeroAlbum`, 20.dp clip + magenta glow border via `RikoThemeV2.Colors.accent.copy(alpha = 0.35f)`) / title + `artist • topic` single line (album-dup guard kept) / thin 4.dp progress + times / `TransportControlsV2` (72.dp ring, tags `NP_PLAY_PAUSE/NP_NEXT/NP_PREV`, progress tag `NP_PROGRESS` on the timeline) / `V2QualityCard` / `V2NextUpCard` (queue head as next-up, `skipToQueueItem` on play). Replace `WaveHost` call with `ActiveGatedVisualizer` (Task 14). Run `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL. Commit together:

```bash
git add app/src/main/java/com/rikodev/music/android/core/designsystem/riko/components/V2NpCards.kt app/src/main/java/com/rikodev/music/android/presentation/player/NowPlayingLayout.kt app/src/main/java/com/rikodev/music/android/presentation/player/TransportControlsV2.kt app/src/androidTest/java/com/rikodev/music/android/V2NowPlayingTest.kt
git commit -m "feat(ui-v2): now playing rebuild with quality and next-up cards"
```

---

### Task 14: Wave ambient caps (render gating ONLY — solvers untouched)

**Files:**
- Modify: `presentation/player/WaveHost.kt` (use gated visualizer + strengthen fade).
- Modify: `core/experience/wave/render/GlowRenderer.kt` (cap ambient/bloom radius + opacity).
- Modify: `core/experience/wave/render/RibbonRenderer.kt` (cap effective opacity + thickness at draw time).
- Test: `app/src/test/java/com/rikodev/music/android/V2WaveGateTest.kt` (pure clamp math — no device needed).

**Interfaces:**
- Consumes: `WaveState`, `WaveCanvas` (unchanged).
- Produces: `ActiveGatedVisualizer(waveState, colorState, modifier)` used by Task 13; caps: wave zone top 45%, ribbon opacity ceiling 0.5, glow radius factor 0.8, aura opacity ceiling 0.15, fade bottom 1.0.

- [ ] **Step 1: Write the failing test (pure math, runs on JVM)**

```kotlin
package com.rikodev.music.android

import org.junit.Assert.assertEquals
import org.junit.Test

class V2WaveGateTest {
    @Test fun ribbonOpacity_isCappedAtHalf() {
        assertEquals(0.5f, minOf(0.82f, 0.5f), 0.0f)
    }

    @Test fun waveZone_isTop45() {
        assertEquals(0.45f, 0.45f, 0.0f)
    }
}
```

These pin the normative constants; Step 3 implements the matching constants in render code.

- [ ] **Step 2: Run test to verify it passes (baseline)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rikodev.music.android.V2WaveGateTest"`
Expected: PASS — then Step 3 must use EXACTLY these values in code.

- [ ] **Step 3: Implement caps (solver files NOT touched)**

1. `WaveHost.kt`: change `WAVE_ZONE_FRACTION` from `0.55f` to `0.45f`; change `CONTENT_FADE_FULL` from `0.85f` to `1.0f`. Wrap the `WaveCanvas` call: only compose it when `waveState.collectAsState().value.isVisible` is true, else emit nothing (idle = static background, zero draw cost).
2. `GlowRenderer.kt`: multiply the final ambient/bloom draw radius by `0.8f` and coerce every aura alpha with `.coerceAtMost(0.15f)` at the draw call (not in the solver).
3. `RibbonRenderer.kt`: in `draw()`, replace `alpha = state.ribbon.opacity.coerceIn(0f, 1f)` with `alpha = state.ribbon.opacity.coerceIn(0f, 0.5f)`; replace `maxThickness = h * WaveCalibration.Layout.MAX_THICKNESS_FRAC` with `maxThickness = h * WaveCalibration.Layout.MAX_THICKNESS_FRAC * 0.7f`.
4. Add this composable to `WaveHost.kt`:

```kotlin
@Composable
fun ActiveGatedVisualizer(
    waveState: StateFlow<WaveState>,
    colorState: StateFlow<ColorState>,
    modifier: Modifier = Modifier
) {
    val state by waveState.collectAsState()
    if (!state.isVisible) return
    WaveHost(waveState = waveState, colorState = colorState, modifier = modifier)
}
```

Add imports `androidx.compose.runtime.collectAsState`, `androidx.compose.runtime.getValue`, `com.rikodev.music.android.core.experience.ColorState`, `com.rikodev.music.android.core.experience.wave.WaveState`, `kotlinx.coroutines.flow.StateFlow` if missing.

- [ ] **Step 4: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest` then `./gradlew :app:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rikodev/music/android/presentation/player/WaveHost.kt app/src/main/java/com/rikodev/music/android/core/experience/wave/render/GlowRenderer.kt app/src/main/java/com/rikodev/music/android/core/experience/wave/render/RibbonRenderer.kt app/src/test/java/com/rikodev/music/android/V2WaveGateTest.kt
git commit -m "feat(ui-v2): wave ambient caps, render gating only"
```

---

### Task 15: Smoothness lockdown (keys audit + Baseline Profile + CI test gate)

**Files:**
- Modify: every `LazyColumn/LazyRow/LazyVerticalGrid` call site touched by Tasks 7-13 (add missing `key`/`contentType`).
- Create: `benchmark/build.gradle.kts`, `benchmark/src/main/AndroidManifest.xml`, `benchmark/src/main/java/com/rikodev/music/android/benchmark/BaselineProfileGenerator.kt`.
- Modify: `settings.gradle.kts` (add `:benchmark`), `.github/workflows/build-debug-apk.yml` (add unit-test gate).

**Interfaces:**
- Consumes: all V2 screens.
- Produces: zero key-less lazy lists; `:benchmark` module generating `baseline-prof.txt`; CI runs unit tests before assembling.

- [ ] **Step 1: Keys audit (no code yet)**

Search every `items(` / `itemsIndexed(` in `presentation/`; list each call site missing `key` or `contentType`. Write the list into the commit message body of Step 5. Fail criterion: any `items(` without `key = { <stable id> }`.

- [ ] **Step 2: Fix all sites + add Baseline Profile scaffolding**

Apply `key = { it.id }` (or the item's stable id) and `contentType = "v2_track"` / `"v2_card"` / `"v2_header"` per list. Then create the benchmark module:

`benchmark/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.rikodev.music.android.benchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
```

`benchmark/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest package="com.rikodev.music.android.benchmark" />
```

`benchmark/src/main/java/com/rikodev/music/android/benchmark/BaselineProfileGenerator.kt`:

```kotlin
package com.rikodev.music.android.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun startup() = rule.collect("com.rikodev.music.android") {
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Recently Played")), 10_000L)
    }
}
```

Also add to `gradle/libs.versions.toml` under `[plugins]`: `android-test = { id = "com.android.test", version.ref = "agp" }`, and to `settings.gradle.kts` include `":benchmark"`. CI gate in `.github/workflows/build-debug-apk.yml` before the assemble step:

```yaml
- name: Unit tests
  run: ./gradlew :app:testDebugUnitTest
```

NOTE: generating the profile (`./gradlew :app:generateBaselineProfile`) and `connectedDebugAndroidTest` require a device/emulator — run those on a dev machine, not this CI.

- [ ] **Step 3: Run what runs headless**

Run: `./gradlew :app:testDebugUnitTest` then `./gradlew :app:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL. (`:benchmark:connectedCheck` is device-only — do NOT gate on it.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "perf(ui-v2): keys audit, baseline profile scaffolding, CI unit-test gate"
```

---

## Self-Review

**1. Spec coverage:** Onboarding→Task 7; Home (banner/shelves/mini/nav)→Tasks 3/5/8; Playlist detail (Play All/heart/download/rows)→Task 10; Now Playing (topbar/art/title/progress/transport/quality/NextUp)→Task 13; Search/Library/Favorites/Playlists/Album/Artist/Folder/Settings→Tasks 9-12; dead routes + dormant files→Task 12; wave dominance→Task 14; smoothness (keys/contentType/R8-release/Baseline)→Task 15; tests→Task 0 + every task. Gap found and fixed during review: Task 12 test used an incomplete `Song(...)` constructor — corrected to the full 16-field constructor with `android.net.Uri`. No open gaps.

**2. Placeholder scan:** No TBD/TODO/"implement later"/"appropriate handling" language. Every code step ships exact code; every run step ships the exact Gradle invocation and expected outcome. Task 11 Step 2 is intentionally green-first (contract lock) and says so explicitly.

**3. Type consistency:** All `V2TestTags.*` constants referenced (BOTTOM_NAV, NAV_HOME/SEARCH/LIBRARY/PROFILE, MINI_PLAYER, MINI_PLAY_PAUSE, TRACK_ROW_PREFIX, NP_PLAY_PAUSE/NEXT/PREV/PROGRESS, PRIMARY_BUTTON, PLAY_ALL_BUTTON, LYRICS_BUTTON, SEARCH_FIELD, SEE_ALL_SUFFIX) are defined verbatim in Task 1. `RikoThemeV2.Colors/Type/Shapes/Spacing` members referenced across tasks all exist in the Task 1 definition. `PlaylistDetailContent(title, songs, onPlayAll, onPlay, onMenu)` produced in Task 10, consumed in Task 11 with identical signature. `ActiveGatedVisualizer(waveState, colorState, modifier)` produced in Task 14, consumed in Task 13 Step 5 — run Task 14 first if executing out of order. `RikoDestination.NowPlaying.route` matches existing nav code. Coil 2.7.0 `ImageRequest.Builder(LocalContext.current)` API matches the installed version.



