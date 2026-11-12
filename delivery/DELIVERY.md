# RIKO MDNA P8 analysis diagnostics follow-up
Local P8 milestone: 681cc92ce873fcb0f825e15acb2e21e6a9a25bc2

Full source snapshot; not an APK. No remote push or new CI run.
For the previous AudioResponse source (f46ceaa), apply only
analysis-diagnostics-fix.patch with git apply --check, then git apply.
For older sources, use the matching phase patches or inspect the full source;
do not blindly apply patches over unrelated local changes.

Read docs/mdna/genesis/ANALYSIS_FAILURE_DIAGNOSTICS.md for changes, tests and
remaining limitations. Original device decode failure is NOT yet reproduced.
The failing song itself or a failure code is still required to diagnose it.
14 host analyzer groups and final camera/code/spectrum contracts passed.
New Room/Robolectric tests are included in CI but unrun locally.
Existing musical-response changes are preserved.
