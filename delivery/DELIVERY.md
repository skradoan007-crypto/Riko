# RIKO MDNA P8 timestamp follow-up (2026-11-13)
Local P8 milestone: 0fc197768d5aed9c226cdd0e280767441ebe3197

Full source snapshot; not an APK. No remote push or new CI run.
For the previous AnalysisDiagnostics source (681cc92), apply only
 timestamp-fix.patch with git apply --check, then git apply.
For older sources, use the matching patches or inspect the full source;
do not blindly apply patches over unrelated local changes.

Read docs/mdna/genesis/MP3_TIMESTAMP_CORRECTION.md for findings and limitations.
A known OMX MP3 529-frame timestamp mismatch is reproduced and corrected in the
shared PCM source. The screenshot confirms TIMESTAMP, but the user's exact
codec/offset has not been reproduced on Android. New numeric diagnostics help
identify other timing cases if needed.
17 host analyzer groups and final camera/code/spectrum contracts passed.
Two generated MP3 fixtures decode with host FFmpeg; Android instrumentation
is expanded but not executed here. Android builds remain unverified.
The 45 FPS device snapshot remains an open performance observation.
