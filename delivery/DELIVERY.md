# Audio-response correction delivery

Current local P8: f46ceaaa7999ffbff9d781cacf0423e7cd7f9cde
Fixed P4 baseline: f75a7e4854e435da95f05cc649b68fa6e3b69f8f
Supersedes P8 7b70a2d and e114429; no remote history rewritten.

Use this complete source for a NEW Android build. The earlier APK is unchanged and
cannot demonstrate this correction. Android compilation and device musical/visual
acceptance of this correction are still pending. Earlier CI success covers old code.

See docs/mdna/genesis/AUDIO_RESPONSE_CORRECTION.md for exact root causes, changes,
executed Java/Mesa tests, unexecuted Kotlin/device checks, and remaining limitations.
Proof clip: docs/mdna/genesis/evidence/response-correction/pcm-response-proof.mp4
It uses synthetic diagnostic PCM and actual ESSL on Mesa, not Android playback.

The full source is already updated. Do not apply patches on top of this archive.
For prior fixed P8 source (7b70a2d), delivery/audio-response-fix.patch contains the delta.
For matching fixed P4, the four genesis-p5 through genesis-p8 patches reconstruct HEAD.
Neither path authorizes force pushing or discarding unrelated repository changes.
