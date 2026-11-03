# Build and publish

Use README.md for local build/signing commands. The final source has not been compiled after the user's no-build instruction.

## Phone acceptance checks

1. Cold launch in airplane mode. Browse all categories and missing-image entries.
2. Search/copy 0000, 0015, 0701, 01212 and 1#. Paste into a text field and compare exact characters.
3. Favorite each 6666 item separately. Copy them, check recent order, force-stop/reopen and verify persistence.
4. Toggle copy feedback and each accent, rotate, relaunch and verify retained values. Android may independently show clipboard confirmation.
5. Test search keyboard, empty search results, Favorites empty state, clear-history cancel/confirm, detail-sheet close and system Back.
6. Check a small phone, landscape, large font, TalkBack and Android APIs 26/33/36. Confirm list position returns when switching tabs.
7. Evaluate the minified preview/release variant on a modest real phone. No smoothness or memory benchmark is inferred from source review.

## Google Play

- Generate your own signed release AAB and enable Play App Signing. Retain the upload key; do not share it in a project ZIP.
- Add store screenshots, developer/support contact, public privacy-policy URL, content rating, audience and Data safety declarations in Console.
- Keep the unofficial-companion wording. Review rights to supplied game imagery before publishing.
- Test updates using an eligible Play-installed release, matching package/signing and a higher versionCode. Exercise cancellation, download failure, background/resume, completed-download restart and no-update states.
- Sideloaded .preview/.debug packages do not prove that live Play updates work. Do not add a fake auto-install setting: consent and installation are governed by Play/Android.
- Follow any testing/account verification requirements shown in your own Play Console before production release.

Official references checked during implementation:
https://developer.android.com/guide/playcore/in-app-updates/kotlin-java
https://developer.android.com/guide/playcore/in-app-updates/test
https://developer.android.com/reference/com/google/android/play/core/appupdate/testing/FakeAppUpdateManager
https://developer.android.com/studio/publish/app-signing
https://developer.android.com/google/play/requirements/target-sdk
