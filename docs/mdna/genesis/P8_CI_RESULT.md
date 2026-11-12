# P8 final CI run — publication collision correction

Evidence: user-supplied full logs for run 43192766063, checked-out remote commit
f0fbc77a5814f5036abf7f503ce127ea66e16b4e, branch new, 2026-11-12.
This is the user's CI run, not a newly triggered run by the assistant.

## Verified from logs
- Host DNA/analyzer/camera/code/timeline checks passed.
- Android Gradle release unit test task succeeded (2m 16s).
- Debug Android instrumentation test Kotlin compilation succeeded (2m 9s).
  Compilation is not instrumentation execution on a device.
- Release APK assembly including R8 and signing validation succeeded (3m 38s).
- Riko-Music-Release-APK uploaded successfully, artifact ID 13931643471.
- Final publication failed: `a release with the same tag name already exists: riko-new-build-5-1`.

Artifact recorded in the log:
https://github.com/skradoan007-crypto/Riko/actions/runs/43192766063/artifacts/13931643471
This is a GitHub artifact ZIP containing the APK, not a direct APK URL.
The existing APK can be used without rebuilding for this workflow-only correction.

## Correction
In .github/workflows/build-debug-apk-release-sdkfix.yml, both release creation and
release-link lookup now use `riko-new-build-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}`.
Workflow-scoped run numbers can collide between workflows; run IDs distinguish runs
and attempt numbers distinguish retries. Existing releases/tags are left untouched.
No continue-on-error, forced replacement, deletion, extra token or secret-scan bypass.
No Android source changes.

Executed locally: YAML parsing, bash syntax for every embedded run step, tag evaluation
for two distinct run IDs sharing run number 5 plus a retry; all passed. No additional
Android build or remote publication was performed. New publication success requires a
future CI run; the corrected workflow has not yet run on GitHub.

This evidence supersedes P8_REPORT/FINAL_HANDOFF's pre-build compilation status for the
user's tested source. Real-device migration execution, exports, lifecycle, AV sync and
performance acceptance remain pending. See delivery/DELIVERY.md for the corrected local
P8 milestone hash and the standalone workflow patch against the previous P8 package.
