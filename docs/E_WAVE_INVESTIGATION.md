# E-Wave — Live Waveform Investigation

Status: investigation + one small real fix. NOT compiled/run yet.

## Headline finding — different from Queue/Audio FX

Queue and Audio FX were "existing but unwired" (stub dependencies, no
real backend calls). The waveform system is NOT that. It is genuinely,
fully wired end to end already:

```
PlayerModule.kt (ExoPlayer construction)
  → PcmAudioProcessor registered as a real Media3 AudioProcessor
    (direct decoder-pipeline tap, real RMS/peak, passthrough-only —
    confirmed it never alters playback)
  → ExperienceDataHub.startBridges() (called from MainActivity.onCreate)
    collects BOTH PcmAudioProcessor.amplitude AND
    VisualizerManager.waveformData, merges with real PCM > Visualizer >
    IDLE priority, publishes one AudioState
  → WaveEngine.observe(experienceDataHub.audioState, colorState)
    (called from PlayerViewModel.init)
  → PlayerViewModel.waveState = waveEngine.state (direct passthrough,
    not faked)
  → presentation/nowplaying/NowPlayingScreen.kt (the screen actually
    registered in RikoNavGraph) renders
    WaveCanvas(waveState = viewModel.waveState, ...)
```

Static (timeline/seekbar) half is separately real too: WaveformRepository
checks a Room cache, else decodes the real file via AudioFileAnalyzer,
caches the result, and explicitly returns `Unavailable(reason=...)` on
failure — never substitutes a fake pattern. `loadWaveformFor(song)` is
correctly called on every song change.

## What this means

If the wave still feels "dead" on your device, it is very unlikely to be
missing wiring. The two real remaining possibilities are:

1. **Device/OEM capture restriction.** VisualizerManager's own status
   states (`ActiveButSilent`, `NoCallbacksReceived`) exist specifically
   because some OEMs block or restrict live audio capture at the OS
   level — something no code change can fix, only work around (which is
   exactly why the PCM tap exists as the preferred path — it doesn't
   depend on that OS capture policy at all).
2. **Visual tuning**, not data — the reactive values may be arriving but
   not reading as "alive" enough. That's a design/parameter task
   (Phase E6), not a wiring task.

**I cannot tell which of these it is from here** — there's no Android
device or emulator in this environment to actually run the app and watch
real callbacks arrive. That's a hard limitation, not a corner being cut.

## The one real change made this pass

`presentation/nowplaying/NowPlayingScreen.kt` — the wave diagnostics HUD
(`InspectorOverlay`) already existed in code but was hardcoded off
(`isEnabled = false`). Flipped to `true` so your next build shows the
exact live status on screen — source (PCM/VISUALIZER/IDLE), callback
count, last RMS — instead of needing logcat. **Turn this back to `false`
once you've confirmed the status you wanted to see** — it's a debug
overlay, not a RIKO Vision-approved permanent UI element.

## Secondary finding (not fixed this pass — flagged for E5)

Confirmed the audit's RB-3 duplicate-file issue is more than cosmetic:
`presentation/player/NowPlayingScreen.kt` + `NowPlayingLayout.kt` (which
renders through `WaveHost`) are dead code — `RikoNavGraph.kt` only ever
routes to `presentation/nowplaying/NowPlayingScreen.kt`. Both files
reference the real wave/audio pipeline correctly, so this isn't a
functional bug today, but it's a real source of exactly the confusion
this investigation had to work through — worth deleting the dead one in
the E5 cleanup pass rather than leaving two screens that look
authoritative.

## Next

Given the wiring is already largely real, the highest-leverage next step
for "the waveform feels alive" is likely E6 (visual tuning) rather than
more engine work — once you've confirmed via the HUD what your device's
capture status actually is.
