# IBD3D Panel — RikoDev

Native Android source project for an offline Indian Bikes Driving 3D cheat-code companion.
Release identity: `com.rikodev.ibd3d.android` · version `1.2.0` / version code `3`.

## GitHub build

Use Actions → Android APK and AAB → Run workflow. The release option uses your private signing secrets; preview produces a development-signed test APK. See [GitHub build instructions](docs/GITHUB_BUILD.md). Builds are manual only; artifacts expire after 1 day.

## Open and build on your PC

1. Extract this ZIP. Open the **IBD3DPanel** folder in Android Studio (the folder containing settings.gradle.kts).
2. Install Android SDK Platform **36**, Build Tools **35.0.0**, and select **JDK 17** as the Gradle JDK. Let Android Studio create local.properties with your SDK path.
3. Sync Gradle. The complete Gradle 8.13 wrapper is included; the first sync needs internet to download dependencies. The installed app browses its catalog fully offline.
4. Choose **preview** in Build Variants for release-optimized phone testing, or **release** for your publisher-signed distribution.

No build was run after the user's latest instruction. See **BUILD_STATUS.md** for the precise validation limits.

### Windows commands (run these yourself)

```bat
gradlew.bat :app:assemblePreview
gradlew.bat :core:model:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
gradlew.bat :app:assembleRelease :app:bundleRelease
```

Linux/macOS: replace `gradlew.bat` with `./gradlew`.

| Variant | Purpose | Package and signing |
|---|---|---|
| debug | Development/debugger | com.rikodev.ibd3d.android.debug, development key |
| preview | Minified phone evaluation | com.rikodev.ibd3d.android.preview, development key |
| release | Publisher distribution | com.rikodev.ibd3d.android, your upload key; unsigned if not configured |

Outputs: `app/build/outputs/apk/preview/app-preview.apk`, `app/build/outputs/apk/release/`, and `app/build/outputs/bundle/release/app-release.aab`.

## Sign your release APK / AAB

In Android Studio choose **Build → Generate Signed App Bundle / APK**, create/select your own upload keystore, and select release. Choose **Android App Bundle** for Google Play. Enable **Play App Signing** in your Play Console.

For command-line signing, copy keystore.properties.example to keystore.properties and fill your own local values. Use forward slashes in Windows keystore paths. Keep your key and passwords private and backed up. No signing key or password is included in this project.

An unsigned bundle is not upload-ready. Increment versionCode for every Play update. Preview builds have a separate package and are not suitable for testing actual Play update eligibility.

## Implemented screens and functions

- Native Android splash with the bike/man identity.
- Home hero, 2×2 category shortcuts, Plugins card, recent/favorite previews and bottom navigation.
- Search by name or exact code, All/Bike/Car/Animal/Other filters and a Vehicles shortcut.
- Local thumbnail cards, stable list IDs, exact clipboard copy and accessible copy/favorite controls.
- Detail sheet with source status, notes, input method and unconfirmed-image information.
- Persistent favorites, bounded 50-item recent-copy history, clear-history confirmation.
- RGS Tools, Plugins URL catalog, 23 offline RGS Load Files, and Custom RGS File Loader.
- Seven-second first-use resource notice; About/privacy/credits. Optional copy reminder on background.
- SAF import/export and read-only document provider; conditional file handoff, no guaranteed automatic game import.
- Settings: Check for Update, copy-feedback toggle, Purple/Blue/Rose accents, developer information, About/version/package/privacy.
- Google Play Flexible Update flow with failure, cancellation, download, resume and explicit restart handling.

All **212 original IDs and code strings** survive in **346 records**: 155 visible cheats, 65 RGS commands, 86 plugin links, 23 models and 17 archived records. Twelve superseded IDs migrate saved favorites/history. Gym Props is excluded. The models are UnityFS AssetBundles despite .glb names. Plugin previews/URLs are offline; game-side plugin fetching requires internet.

See [current Bengali audit](docs/content-intake-2026-11-04/README_BN.md), [every original entry](docs/content-intake-2026-11-04/ALL_ORIGINAL_ENTRIES.md), [image requests](docs/content-intake-2026-11-04/IMAGE_ISSUES.md) and [policy publication steps](docs/legal/PUBLISHING_BN.md). Source attribution is not a redistribution license. This source revision was not built; see BUILD_STATUS.md.

## Architecture

Four Gradle modules: app, core:model, core:data, core:designsystem. Hilt supplies a single local repository; a ViewModel exposes lifecycle-aware Flow state; Navigation Compose owns destinations. Atomic DataStore edits store favorites/history/preferences. See docs/ARCHITECTURE.md for the choices and data flow.

## Toolchain

Kotlin/Compose plugin 2.2.21 · AGP 8.11.1 · Gradle 8.13 · JDK 17 · Compose BOM 2025.08.01 · Hilt 2.57.2 · Coil 3.3.0 · Play Update 2.1.0. Min SDK 26; compile/target SDK 36. SDK 36 supersedes the original recommended 35 because new Play submissions require API 36 as of November 2026: https://developer.android.com/google/play/requirements/target-sdk .

See docs/RELEASE.md for phone/Play checks and docs/CONTENT_UPDATES.md for editing future catalog releases.

## Accuracy and motion update (1.1.0 / versionCode 2)

Home now includes Get the Game; source badges, category corrections, lightweight motion and indexed lookups are included. Zero unverified new codes were promoted to confirmed.

Run the source/data checks without an Android build:

```bash
python3 -m unittest discover -s scripts -p 'test_*.py' -v
```

To compile and execute the supplied Android/model regression tests when ready:

```bash
./gradlew :core:model:test :app:testDebugUnitTest :app:lintDebug
```

These Gradle tasks were not executed for this revision. See [BUILD_STATUS.md](BUILD_STATUS.md).

Model payload is about 174 MiB; actual APK/AAB size is unmeasured. Large model segments are lazily reassembled into versioned cache. Header checks do not establish game compatibility.
