# Build from GitHub

The workflow is manual only; pushing source does not start a build.

Open **Actions → Android APK and AAB → Run workflow**. Choose `release` for a publisher-signed APK/AAB or `preview` for a minified development-signed APK. The preview package ends in `.preview`; it is not a Play release.

Before a release build, create/select your upload keystore in Android Studio and add these repository **Actions secrets** under Settings → Secrets and variables → Actions:

| Secret | Value |
|---|---|
| ANDROID_KEYSTORE_BASE64 | Base64 encoding of your upload-keystore bytes |
| ANDROID_KEYSTORE_PASSWORD | Keystore password |
| ANDROID_KEY_ALIAS | Alias inside the keystore |
| ANDROID_KEY_PASSWORD | Password for that alias |

Keep the original key backed up privately. Do not put any key/password/PAT in a commit or chat. The workflow stops when release signing secrets are missing; it never substitutes a development key for release.

After success, download the workflow's artifact from the run page. Artifacts expire after **1 day** to limit storage usage. The signed release includes APK, AAB and the R8 mapping file; save the mapping alongside that release. Use the signed AAB with Play App Signing for Play distribution.

GitHub's displayed artifact-storage usage may take 6–12 hours to update after cleanup. Deleting artifacts does not undo already accrued billing usage or reset a separate minutes allowance. Reference: https://docs.github.com/en/billing/concepts/product-billing/github-actions

No workflow run was triggered during the source push. Final compile and device/Play validation remain pending.
