# P7 — DNA sharing and export
Added bounded versioned portable DNA code, strict codec validation, clipboard copy,
imported preview, normalized visual-trait comparison and order-independent deterministic
breeding. Imports/offspring do not overwrite a song profile. Added persisted versioned
10Hz timeline using atomic file writes and strict size/value validation.

DnaExporter renders actual organism GLES geometry offscreen: 1440px PNG branded card,
or silent 720px AVC/MP4 with 300 fixed-step frames at 30Hz. Video reads stored feature
timeline only; no live audio capture. Progress, coroutine cancellation, serialized
exports, codec timeouts, partial-file deletion and Android content-URI share chooser
are wired into MDNA Lab. Fixed seed/settings/timeline reproduce inputs; encoded bytes
and GPU pixels are not guaranteed identical across hardware/codecs. Imported preview
video uses the selected song's timeline, not an inferred imported audio recording.

Executed: existing P1/P2 Java regression suites plus DnaShareCheck (DNA roundtrip,
version rejection, symmetric offspring, distance, timeline roundtrip/trailing bytes).
No Gradle/APK build, Android encoder/PNG visual validation or sharing-device tests run,
per requested build deferral. Codec/GPU compatibility and device cancellation cleanup
remain unverified. Output files remain in app cache for Android sharing/cache eviction.
