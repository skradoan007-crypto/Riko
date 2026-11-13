# RIKO MDNA P8 — live dot replacement

Commit: d158498c54317cbf941c4ca803bbea226fff5767
Branch: new (local milestone amended; no remote push/history rewrite performed)
Milestone: mdna(genesis-p8): production integration

This is a complete source snapshot, not an APK. Current changes are described in
`docs/mdna/genesis/LIVE_DOT_REPLACEMENT.md`; full P8 file list is P8_FILES.txt.
The existing workflow is `.github/workflows/build-debug-apk-release-sdkfix.yml`.
No Android build was run for this snapshot. Prior green builds do not cover it.

Main MDNA now opens independent live PCM dots immediately. There is no automatic
offline decode. DNA tools contains optional analysis, its truthful failure details,
sharing and background scheduling. The observed 5,042 us later-buffer mismatch is
not claimed fixed. Hardware EQ/preamp capture and isolated vocals remain unsupported.

## Patch choice
- If your source is exactly 7830b41 (InitialTimestampFix delivery), apply only
  `delivery/live-dot-replacement.patch` with `git apply --check` then `git apply`.
- For exact P7 d2d3bc4, use genesis-p8.patch.
- P5/P6/P7 patches are provided for the prior exact phase baselines. Do not apply
  the full P8 patch and the incremental replacement patch to the same tree.
- Otherwise inspect/diff this full source against your checkout before transferring
  changes. Preserve unrelated work and your repository metadata. Do not reset or
  force-push a remote to use this archive.

Each supplied patch was applied to a temporary git index and matched its target tree.

## Evidence
`delivery/evidence/reference-audio-preview.mp4` renders the actual ESSL with audio
from the supplied reference video using a host reference DSP driver. It is NOT an
Android capture or proof of device latency/performance. No reference image texture
was used. `synthetic-audio-preview.mp4` has silence, bass pulses, mid sweep, treble
transients, then silence, all computed from the clip's own PCM samples.

Passed: 19 analyzer JVM contract groups; existing final JVM camera/code/timeline/
absolute-spectrum contracts; actual GLES 2.0 Mesa shader tests, all densities and
viewports, stable silence/constant input, local response, amplitude ordering and
reference pause decay; diff/credential checks. New Kotlin response and Android GLES
context tests are included but were not run. See report for exact limitations.

Next gate: final Android build, real-device playback/seek/volume/pause/route/lifecycle
and visual review. This archive is source-ready, not an asserted green APK build.
