# Genesis P5–P8 handoff

**CI update (2026-11-12):** See [P8_CI_RESULT.md](P8_CI_RESULT.md). The supplied CI logs
confirm Android unit tests, device-test compilation and APK assembly/upload passed;
only release-tag publication failed. The pre-build notes below record the original handoff.

P5 interaction, P6 evolution/background analysis, P7 sharing/export and P8 production
wiring are implemented in four milestone commits. See each phase report and file list.
P8_REPORT supersedes earlier limitations about service-owned history and hybrid playback.

Entry points: Now Playing > overflow > Music DNA; Song Info > Open Music DNA;
Music Visualizer Lab > Open MDNA Genesis. Quality, motion, camera and AV offset are in
Controls. Share / Import DNA exposes PNG, silent 10-second MP4, portable code, comparison
and breeding. Library analysis > Start schedules charging-constrained analysis; Stop
cancels it. Offline analysis runs locally; no account/network service is added.

The user explicitly deferred all Android builds until this handoff. Do not label this
source package a green Android build. The existing Android workflow performs host tests,
Gradle unit tests, Android test compilation and development-signed release APK packaging.
It has not run for these commits. Use its named APK artifact after the final build;
GitHub may wrap artifact downloads in ZIP. Release signing remains the existing debug
key development/testing workflow, not production Play signing.

After a successful final build, verify on real devices:
1. Install/update without clearing data; verify playlists, favorites, EQ, history and
   playback/notification controls survive the Room migration and app lifecycle.
2. Play analyzed/unanalyzed songs; seek, pause/resume, change track/speed/EQ, change
   Bluetooth route, calibrate AV offset. Confirm no stale prior-song visual response.
3. Rotate/pinch/pulse during playback and pause; cancel gestures, use TalkBack buttons,
   increase text size, rotate device, background/restore and reopen each entry point.
4. Check meaningful listening increments once, no increment for brief skips, no
   duplicate after resume, background playback contribution, bounded generation.
5. Schedule analysis, disconnect charger, stop/resume, reboot, simulate low memory and
   thermal pressure; validate a single bounded decoder and playback stability.
6. Copy/import invalid/valid DNA, compare/breed, share PNG and MP4. Inspect dimensions,
   MP4 300 frames at 30 Hz, silence, timestamp progression, cancellation and retry.
7. Measure frame pacing and audio underruns, 60/120 Hz behaviour, memory over repeated
   opening/export, battery and thermal behaviour. Capture Android screenshots/video.

Do not infer device performance or Android compile success from the passing Java/Mesa
checks. All omitted Android acceptance checks are explicitly pending, not waived.
