# Actual validation — 2026-11-05

Version 1.2.0 / versionCode 3. No Gradle build, dependency sync, emulator, signing, APK/AAB generation or workflow dispatch was run, respecting the user's no-build instruction. **Not compile-certified.**

- 26 Python regression tests passed; see docs/content-intake-2026-11-04/data-test-results.txt.
- 32 Kotlin/Kotlin-DSL files parsed/formatted by ktfmt, exit 0. This is syntax formatting, not imports/types/Compose compilation; see kotlin-format-results.txt in the same folder.
- 7 XML files parsed; git diff --check passed.
- 38 Kotlin/JUnit/UI tests supplied, **not executed** for this revision. New tests cover model headers, reminder scheduling/dismissal, saved-ID migration and notice countdown.

No gameplay, device performance, accessibility, actual Play update eligibility or legal clearance result is claimed. All 23 supplied model files are UnityFS AssetBundles despite .glb names; SHA/header validity does not prove game compatibility. No documented automatic-import API was established. Load uses a compatible file-opening receiver if present, otherwise the game's RGS picker/export fallback.

Model payload is 182,747,780 bytes (~174 MiB) before Android packaging. APK/AAB download/installed size still needs measurement. Earlier build results do not certify this source revision. Use README manual build commands when ready, complete docs/content-intake-2026-11-04/DEVICE_CHECKS.md, and resolve the contact/privacy/license items in docs/legal/PUBLISHING_BN.md.
