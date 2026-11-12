# P5 — organism interaction
GL-thread-owned camera adds bounded pitch/zoom, exponential smoothing, controlled
fling inertia, cancellation and decaying tap pulses without changing genome identity.
GestureDetector handles touch slop; multi-touch suppresses tap/fling until next down.
RikoHaptic confirmation is triggered on the UI thread. Accessible click/zoom actions
and visible camera buttons provide alternatives to gestures. Reduced-motion system
setting disables fling. Existing audio playback pipeline is unchanged.

Files: OrganismCamera.java, MdnaGlSurfaceView.kt, MdnaGlRenderer.kt,
MdnaVisualizerHost.kt, tools/CameraCheck.java and this report.
Executed production Java camera harness: bounds, smoothing, cancel, pulse decay,
invalid input and reset PASS. No Gradle/APK build run, as requested. Android gesture,
TalkBack, lifecycle and playback stability validation remains pending. P6 follows.
