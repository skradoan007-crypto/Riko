# E2 — Audio FX Engine — Real Wiring Changelog

Status: same caveat as E1 — written, NOT compiled/run yet. Push to CI for
real signal.

## What was broken

`AudioHostDependenciesStub` wired every audio action (EQ, bass boost,
virtualizer, balance, crossfade, gapless, replay gain, sleep timer,
presets) to an empty no-op.

## The good surprise

Unlike Queue, most of the real engine already existed: `AudioFxManager`
wraps real `android.media.audiofx.Equalizer/BassBoost/Virtualizer`,
persists to Room, and is already correctly initialized by `PlayerManager`
with a real audio session id on `STATE_READY`. This subsystem needed a
bridge more than new engineering — for three of the seven effect types.

## What changed

**New file:**
- `presentation/audio/host/AudioHostDependenciesReal.kt`

**Modified:**
- `di/CoordinatorModule.kt` — Audio binding now points to the real class.

## Fully real
EQ bands, Bass Boost, Virtualizer (bridged to AudioFxManager) · Sleep
timer with real fade-then-pause (self-built here, PlayerManager's own
timer has a different shape) · Gapless/Crossfade/ReplayGain *persistence*
(SettingsRepository already had the real DataStore keys, just unused)
· Crossfade *runtime effect* — real volume-ramp fade near track end ·
ReplayGain *runtime effect* — real attenuation-only volume scaling ·
haptic/snackbar/log (same real pattern as Queue).

## Honest gaps (logged, not faked — see file header for full detail)
1. **Balance** — not implemented. Needs a custom Media3 AudioProcessor at
   ExoPlayer construction time; that construction site wasn't part of
   this pass, and DSP-level renderer surgery isn't something to insert
   blind without compiling.
2. **ReplayGain amplification** (positive preampDb) — volume alone can't
   go above unity. Real fix is `LoudnessEnhancer`, which `AudioFxManager`
   already imports but never uses — a pre-existing gap this pass found.
   Also: no real per-track gain tag reader exists anywhere yet — only the
   manual preamp slider is real.
3. **User-created EQ presets** (save/delete) — `EqSettingsDao` only has a
   single active-settings row, no multi-preset table. Needs a new Room
   entity + migration — real, bounded, deliberately not improvised here.

## Next
Waveform — live media/bass/vocal reactivity, explicitly the user's
top-priority item. Getting its own focused pass rather than being
compressed into this one, since it's arguably the most complex, most
emotionally-central piece of the whole app.
