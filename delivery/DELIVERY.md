# RIKO MDNA P8 initial timestamp fix (2026-11-13)
Local P8 milestone: 7830b41ac765bece06b1a7b51b176b6ae42424fa

Full source snapshot; not an APK. No remote push or new CI run.
For the previous TimestampFix source (0fc1977), apply only
initial-timestamp-fix.patch with git apply --check, then git apply.
For older sources, use the corresponding cumulative patch or inspect the full
source. Do not apply every patch together or overwrite unrelated local changes.

Read docs/mdna/genesis/INITIAL_TIMESTAMP_ORIGIN_FIX.md for findings and limits.
The exact device diagnostic (48 kHz, frames read 0, first PTS 36979 us) was
reproduced with the old production analyzer on JVM. The corrected analyzer retains
bounded leading offsets in the stored timeline and shared live/export sampler.
Zero-origin MDT1 remains byte-compatible; new nonzero timelines use MDT2.
19 host analyzer groups and final camera/code/spectrum checks passed.
Android/Room tests, Android builds and device playback remain unverified here.
No claim that the remaining song decode, visual quality or 45 FPS issue is resolved.
