# CI artifact quota recovery

The supplied log for commit 221feab shows Kotlin compilation and :app:testDebugUnitTest passing (BUILD SUCCESSFUL). Timeline render upload then fails with Artifact storage quota has been hit, preventing APK compilation from starting. The post-cleanup missing-submodule URL warning is separate.

Both artifact uploads are now nonblocking delivery steps. Tests and compilation remain mandatory. Upload failure creates an explicit warning and job summary stating that there is no downloadable APK artifact. This allows compilation to finish even while upload storage is exhausted; green build status is not proof of APK delivery.

Future render artifacts retain for one day and APK artifacts for three days. This does not reclaim existing quota. No artifact deletion, billing changes, workflow write permissions, automatic releases or token storage are introduced. The automatic release alternative was rejected by approval review and removed before pushing.

Validation: YAML structure, generated Bash syntax and mandatory build/test guards checked locally. Next CI run remains pending the user's result. To restore artifact downloads, free Actions artifact storage and rerun after GitHub recalculates quota (the supplied error says 6-12 hours).
