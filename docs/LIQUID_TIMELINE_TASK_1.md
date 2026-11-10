# Current execution status — 2026-10-15

Target branch: `riko-perf-fixes`; reviewed base: `be3db7729f2ea9d84984d65270c6e6ea9169fb7e`.
Current owner correction: independent random births for both accents AND white;
broader horizontal coverage with the approved height and fast leftward motion.
Source/regression changes complete; CI native rendering and APK verification next.
Historical implementation notes below are superseded by the latest entry.

- [x] Inspect the new device screenshots 1000271152/1153 and target 1000271125.
- [x] Replace synchronized/parented white folds with three independent emitters.
- [x] Widen the expanding membranes without raising the 16dp amplitude budget.
- [x] Extend geometry and native render regressions for independence and width.
- [ ] GitHub Actions unit tests/native renders/debug APK for this head.
- [ ] Owner-device visual equivalence and frame-time measurement.

CI renders establish simulated Android drawing, not phone GPU/FPS or 100% parity.

# Task 1 — translucent album-colour liquid timeline

Reference: owner supplied 1000184906.jpg and screen-20261015-002330.mp4.
The photo specifies the translucent, broad ribbon and small hollow playhead.
The video shows a restrained timeline beneath the artwork, rather than dense,
fast oscillations. A photographed/compressed reference cannot establish exact
shader parameters, motion frequency or pixel parity on a different device.

Implemented:
- three overlapping translucent broad liquid bodies confined to played span;
- small hollow playhead, thin unplayed baseline, related artwork colours;
- bass pressure, mid texture and overall energy influence shape;
- exponential time-based audio smoothing and palette transitions;
- reusable paths and primitive buffers, one Canvas, no RenderEffect blur pass;
- stop frame loop in background, when player sheet is hidden, when paused,
  or when motion is disabled;
- inset-correct taps/absolute horizontal drag, latest callbacks, paused seeks,
  and canceled scrub transaction cleanup.

This is the requested LIVE fluid visual, not a historical file-amplitude plot.
Existing file waveform analyzer and cache have not been removed.

Validation: integration/source review performed; geometry JUnit tests added.
Gradle execution unavailable: wrapper cannot download Gradle 8.7 because network
access to services.gradle.org is blocked. Android compile, device rendering,
frame timing and 100% reference parity remain unverified. No GitHub push at that initial ZIP stage.

Device gate: run :app:testDebugUnitTest and :app:assembleDebug; then compare
slow/quiet/loud music, artwork changes, pause/seek/resume, horizontal scrubbing,
vertical sheet collapse, endpoints, and 60/90/120 Hz frame traces against reference.

## Frame-by-frame correction pass (ZIP-stage record)

All 469 decoded frames were extracted without temporal subsampling and reviewed
in numbered contact sheets; active frames 180, 200, 300, 350 inspected enlarged.
334 frames admitted a simple unplayed-rail detection. A local green-channel
colour-deficit heuristic on those four active frames estimates the dark cap at
13–15 source pixels. This is an approximate compressed-video measurement, not
absolute device dp, alpha, or shader reconstruction.

Frame/time map (source PTS):
- 1–153 / 0–3.296s: mostly flat timeline; controls show playback, so do not infer
  silence or pause solely from the flat appearance.
- 154–175: presenter/cut; timeline not visible.
- 176 / 3.634s onward: visible asymmetric coloured caps and pale inner folds.
- 211–277: finger partly obscures the rail during interaction.
- 278–354: another clear wave segment, including editorial replay/cut.
- 355–391: partly obscured by touch interaction.
- 392 / 10.581s: play triangle appears; subsequent timeline becomes flat.
- 431–469: presenter; not evidence about waveform shape.

Corrections from first implementation:
- one-sided dark translucent outer cap, lighter thin internal lenses;
- no three equal-height stacked mounds, no bright heavy outline;
- maximum body height tuned relative to the small ring;
- neutral unplayed rail separated from the hollow ring;
- live energy drives amplitude with no permanently visible idle mound;
- pause collapse uses elapsed seconds, independent of display refresh rate;
- animation enabled only after the timeline's sheet reveal threshold.

The replays/camera motion/compression prevent an exact motion or opacity estimate.
Flow speed and alpha remain visual tuning parameters. This pass does not claim
pixel parity, original shader recovery, or an Android render verification.

## Additional matching / wiring review

- Spatial phase now uses physical played length / 125dp, not a fixed number of
  folds stretched across the current seek position. Changing the playhead
  crops/reveals the flowing field while retaining a tapered endpoint envelope.
- Short played segments attenuate height; the ring remains the same radius on
  touch rather than inflating beyond the photographed reference.
- Palette interpolation has a separate finite frame job. Album changes no longer
  restart the wave phase clock or the audio attack/release integrator.
- Monochrome artwork uses neutral timeline colours instead of the global cyan
  accent fallback. Other screens' existing colour policy is unchanged.
- A software geometry render exposed an internal-gradient mismatch: using outer
  height for all gradients hid the luminous fold in the thinner layers. Each
  layer now uses its own height range for the gradient.
- 95,445 mathematical samples across physical spans were checked for finite,
  bounded caps. This Python check is NOT Kotlin execution or Android rendering.

The source remains a visually tuned reconstruction. Matching the original's
unknown audio normalization, easing, palette selection and GPU rendering still
requires side-by-side device output; no 100% identity claim is made.

## Final hollow-playhead correction

Initial pushed commit `1139905` passed the GitHub Actions unit-test step.
Final draw review found that ending the played rail at the ring centre could
show a bright line inside its hollow centre. Both body clipping and played rail
now stop at the ring's outer left edge plus a 2dp gap; the right rail uses the
same gap. An endpoint regression test covers the full progress range.
The correction requires a fresh CI run for the new head.

## Device feedback correction — 2026-10-15, 01:32 Bangladesh time

Owner confirmed the first APK is close (92/100), with two remaining issues:
body height too small and flow should be slower. Two 1080x2400 screenshots
were compared with the perspective-corrected crop of the third target photo.
The screenshots show only about 6–9px of body above the rail versus a roughly
17px ring radius; the target crop's body is closer to its own ring radius.
These are approximate image measurements, not recordings of audio magnitude.

Changes:
- Maximum amplitude: 9dp -> 15dp (also bounded by 30% of canvas height).
- Linear energy scaling -> continuous square-root perceptual compression.
  At 0.25 weighted energy, strength becomes 0.5; zero stays zero. Combined
  with the new cap this gives about 3.3x old height for that passage, with
  smaller gain for loud peaks to avoid excessive expansion.
- Flow angular speed: 1.35–1.80 -> 0.60–0.78 rad/s, about 2.3x slower.
  Audio attack/release, vsync cadence and direct finger tracking are unchanged.
- No new blur layer, increased sampling density, or additional frame loop.
- Four focused regression tests added for quiet/silent signal mapping, slow
  flow range and invalid signal bounds (eleven tests in the geometry suite).

The screenshots do not establish actual motion speed. Slow-flow adjustment
follows the owner's feedback; visual/device confirmation of this tune remains
pending after the new build.

Perceptual compression also changes apparent decay, so a finite smooth pause
envelope was added: compressed height fades continuously to zero in 450ms,
rather than retaining a visible body and snapping off at the settling deadline.
The pause-envelope test checks monotonic fade, its endpoint and near-zero tail.

## Owner correction — 2026-10-15, 01:42 Bangladesh time

The previous feedback was misinterpreted: the owner wants FAST, ultra-smooth
fluid flow, not slower motion. The already-triggered slow-tune CI run is allowed
to finish, but its APK is superseded by this commit.

Current flow is 2.80–3.50 rad/s, about 1.8–2.25 seconds per cycle. The larger
15dp body, perceptual quiet-signal visibility and smooth 450ms pause fade stay.
Phase still advances with actual vsync delta time; no frame skipping, timers,
extra blur or new frame loops were added. Existing audio attack/release smoothing
continues to suppress jitter without slowing the independent travel clock.
The speed regression test now enforces the fast, bounded cycle range and clamps
invalid/out-of-range inputs. CI for this new head is the delivery gate.

## Owner clarification — decorative flame without PCM

The latest device screenshot still shows a very thin body. The owner explicitly
requires time-driven motion without PCM, FFT, RMS or file-envelope dependence.
This supersedes the earlier energy-dependent design and tuning above. A pending
RMS experiment was discarded before commit/push.

- Timeline now accepts playback state, progress, artwork colours and seek
  callbacks only. Removed its WaveState/AudioState/file-waveform subscriptions.
  The separate Visualizer Studio and canonical audio pipeline are unchanged.
- Constant 3.5 rad/s travel (about 1.8 seconds per cycle), driven by vsync elapsed
  time. Height cap is 20dp; breathing stays at 86–100% while playing, and the
  broad contour has a visible base body regardless of the track's audio level.
- All contour terms are periodic in phase. The previous fractional phase term
  could jump at the 2π wrap; every layer now closes continuously across it.
- Existing hollow-ring clipping, artwork palette interpolation, direct seeking,
  background/hidden gating and finite 450ms pause fade remain.
- Geometry tests cover refresh-rate independence, wrap continuity, persistent
  body height, endpoint taper, seek mapping and pause settling.

CI compilation/unit tests and APK delivery will verify this head. Device motion
and exact reference parity still require viewing the new APK on the owner's
phone; no measured frame-rate or pixel-identical match is claimed.

## Device height correction — 2026-10-15, 02:18 Bangladesh time

The new 1080x2400 device screenshot still shows a nearly flat visible body.
The previous geometry-only checks did not prove on-screen height or contrast.
The supplied image cannot establish the installed commit or runtime state, so
no claim is made that its exact runtime cause has been reproduced locally.

Changes now make the render height explicit:
- Normalize each sampled contour to its own peak before mapping to screen dp.
  The upper body is now 13.76–16dp in steady playback for played spans >=40dp,
  instead of multiplying the height budget by an arbitrarily smaller peak.
- Increase the luminous lens from 48% to 82% of that body and the internal fold
  from 24% to 50%. Align each gradient to the actual layer height; preserve
  translucent fill but increase its visible contrast against the aurora.
- Fast time-only phase, album colours, circle clipping and pause fade remain.
  No audio/PCM/FFT inputs or new render loop were introduced.
- Add a Robolectric native-graphics test of the production composable, starting
  paused and transitioning to playback. It captures seven frames over a full
  flow cycle, asserts the real 56dp layout, measures >=9dp of visible body above
  the baseline, then verifies the body settles after pause. These renders are
  uploaded as CI artifacts, never committed screenshots (D-10).
- Keep the pure geometry tests for normalized peak, taper and cycle continuity.

The render test runs in the existing JVM CI job with Android resources enabled;
Robolectric and Compose UI test dependencies are test-only. This provides actual
Android Canvas/Compose render evidence, not phone GPU/FPS or full-screen parity.
Reference setup: https://developer.android.com/training/testing/local-tests/robolectric

Validation status: source/diff review complete; CI render/tests/APK pending.

Render harness follow-up: first run compiled and ran 75 tests; 74 passed, but
Compose PixelCopy capture timed out before producing images. The test now draws
the real Compose content view into a native Android Canvas/Bitmap and crops the
semantic bounds. No height assertion was weakened. Include test-result XML with
the render artifact so capture/assertion failures remain inspectable.

## Reference shape/colour refinement — 2026-10-15, 02:50 Bangladesh time

Owner confirmed the latest device height is correct. The current 02:47 screenshot
was compared with the original green photo, the two blue/pink media-card examples
and the warm orange example. Remaining differences: broad nested/milky contours,
a dark backing dome, weak separation between hues and too little local peak
variation. The target family has separate overlapping coloured lobes and a thin
white/cream core. The three references have different heights and shapes, so
none is treated as proof of one exact proprietary animation algorithm.

- Preserve 16dp normalized height budget, 3.5 rad/s time clock, playback gating,
  pause fade and circle-limited played region. No PCM/FFT or audio subscription.
- Replace the global dome with independently moving compact smooth lobes, each
  with a smaller shoulder. Peaks can separate, overlap and cross positions.
- Three chromatic membranes + one thin bright core; remove black backing and
  reduce the underside. Keep primary/secondary artwork hues separate, project
  their brightness/saturation for legibility without introducing other hues.
  Near-monochrome artwork stays neutral.
- Reuse four paths and four mutable contour profiles. Per-layer phase parameters
  are computed once per frame, not again at every contour sample. No blur pass.
- Extend native render coverage with warm/green artwork transitions and colour
  visibility assertions, while retaining the existing visible-height/pause tests.
  Add focused palette regressions for monochrome and washed-out colours.

Source shape study reviewed; native render and APK verification pending for this
head. The source preview is not a phone capture or a claim of exact parity.

## Circle emission + explicit artwork combinations — 2026-10-15, 19:03 Bangladesh

Owner approved the preceding shape and asks for two changes:
1. Waves originate at the circle, travel rapidly LEFT, expand and disappear at
   the left track endpoint; oscillating around fixed centres is no longer valid.
2. Select white + two distinct colours from the supplied combinations using the
   MAIN artwork colour, instead of whitening/blending artwork swatches together.

Implementation:
- Two overlapping emission packets per reused contour. Each has a monotonic
  centre from u=1 to u=0, a growing width, smooth birth and a finite death fade.
  A full journey takes about 1.21s (5.2 rad/s); motion uses elapsed vsync time.
- Startup warmup rejects packets which have not been emitted yet. Playback starts
  at the circle rather than revealing pre-existing packets halfway along the rail.
  A fully settled pause clears the clock; interrupted pause/play resumes smoothly.
- Remove per-layer peak normalization because it would cancel a dying packet's
  height fade. Overlapping lifetimes preserve the existing 16dp height budget.
  Horizontal alpha falloff also dissolves the older packets near the left edge.
- Main artwork colour is now the sole colour input to the timeline. Core is white;
  outer/middle accents are explicit theme tokens. Coloured membranes retain their
  tint rather than being repeatedly interpolated toward white. Palette transitions
  still run independently of the emission clock. PCM/FFT remains disconnected.

Deterministic main-hue mapping (degrees, upper bound excluded):
| Main artwork hue | Combination |
| --- | --- |
| 0–12, 356–360 | White / light red / dark red |
| 12–24 | Solar Flare: #FFFFFF / #FF5400 / #FFE600 |
| 24–40 | Sunset Glow: #FFFFFF / #FF7A00 / #D500F9 |
| 40–78 | White / yellow / magenta |
| 78–155 | Acid Mint: #FFFFFF / #39FF14 / #00B0FF |
| 155–204 | Ice Neon: #FFFFFF / #00F5D4 / #0061FF |
| 204–252 | White / purple / blue |
| 252–294 | Vaporwave Pop: #FFFFFF / #8000FF / #E6004C |
| 294–338 | Cyber Spark: #FFFFFF / #FF007F / #7B2CBF |
| 338–356 | White / pink / red |

Achromatic artwork uses the neutral fallback. Same artwork hue selects the same
family at different brightness levels; there is no random palette cycling.

Validation includes left-only travel, monotonic expansion, zero birth/death gain,
startup origin, phase-wrap continuity, refresh-rate independence, exact named hex
values, main-hue routing, and native render/pause/colour checks. Native evidence
captures 16 motion frames plus the ten combination families. These captures are
simulated Android renders, not measurements of phone GPU frame rate.

Source/diff review complete; CI render/tests/APK verification pending for this head.

Native review follow-up: first CI tests passed, including startup origin. Across
15 adjacent 80ms native frame pairs, the outer-colour profile moved left by
27–30px per frame (median -28px). This is simulated-clock image analysis, not
phone frame-rate measurement. Visual review found detached white mounds between
coloured emissions; the white inner fold now follows the middle membrane's
packet timing, and the fine white core follows the outer membrane. An invariant
checks that the inner contour stays in its coloured parent. A new CI head is
required for this correction.

## Independent colour births and broad membranes — owner correction, 2026-10-15

Compared the two 07:39 device captures with the second target photo. The old
fixed two-packet cycle created narrow islands, and tying white to a coloured
parent contradicted the owner's intended independent waves. This entry supersedes
that parent invariant and the old fixed-cycle/warmup implementation.

- Exactly three independent streams: outer accent, middle accent and WHITE.
  Each has its own seeded pseudo-random schedule, lifetime, width, height and
  asymmetry. A colour can be absent while another is visible, including white
  appearing alone. No colour is a permanently nested copy of another.
- Initial packets still originate at the playhead; initial delays stagger the
  colours. Centres travel only left; width expands from 0.20 to 0.63 of the
  played span (half-width), compared with the previous 0.055 to 0.285 before
  its additional length scaling. Mature membranes overlap across most of the
  played rail instead of looking like a chain of small separate hills.
- Keep the 16dp height budget and approximately 1.21s travel, with independent
  0.90–1.10 lifetime variation. Births are irregular, with occasional gaps;
  neither the breathing phase wrap nor artwork changes restart their schedules.
- Reuse three packet slots per colour and three paths. Smooth polynomial lobes
  replace per-sample cosine/power evaluation. The frame job updates reused
  profiles; the Canvas reads changing frame state in the draw phase. No extra
  PCM/FFT/audio input, blur pass, frame job or UI layout animation is added.
- Pause freezes the current shape for its existing 450ms fade, clears packets
  after settling, and resumes births at the circle. Hidden/background gating,
  scrubbing, hollow-ring clipping and explicit artwork combinations remain.

Regression coverage: independent absence/solo presence for all three colours,
finite bounds/taper, broad mature coverage, continuous births/retirement,
60/90/120Hz equivalence, origin/reset, and packet travel/expansion/fade. Native
render evidence samples 32 frames and measures occupied width as well as height;
colour checks observe a time window because temporary colour absence is valid.
The rejected white-inside-parent assertion has been removed.

Implementation guidance checked against Android's official Compose draw-phase
performance documentation:
https://developer.android.com/develop/ui/compose/performance/phases
The supplied photos remain the visual reference; no proprietary Samsung algorithm
or pixel-identical reconstruction is claimed.

CI follow-up: the first independent-emitter head compiled and passed the native
render test (all 32 measured frames: 10.67–15.33dp visible height; 94.6–100% of the
sampled played region occupied 2dp above the baseline). Its geometry continuity
test caught a steep early birth. Increase the initial half-width and smooth the
birth envelope over 20% rather than 14% of lifetime. Travel speed and final width
are unchanged; the strict 120Hz shape-change gate remains. Startup checks allow
the gentler initial height while still requiring an empty left half and a visible
new wave by the circle. Fresh CI verification is required for this correction.


## Luminous endpoints and playhead — owner reference 37342, 2026-10-15

Reviewed the marked defects in 37340/37339 and all four widgets in 37342.
The previous hollow ring, 2dp dry gap and near-invisible halo do not match this
latest reference. This entry supersedes those earlier playhead constraints.

- The played rail now meets a 6.2dp accent-filled playhead with a bright white
  rim and soft radial bloom. Its hue follows the main artwork, normalized to a
  bright light source, and transitions with the existing 420ms palette change.
- A long smooth left envelope replaces the abrupt 4.5% edge ramp. Broad old
  packets flatten gradually before disappearing; the birth end still meets the
  circle. The independent emitters, approximately 1.2s leftward travel, 16dp
  height budget, exact combination tokens, and PCM-free animation remain.
- Screen-composited translucent membranes emit light over both dark and bright
  player backgrounds. This fixes the dark-red silhouette visible on the owner's
  saturated red aurora. Accent crests blend into a lighter thin base; the white
  membrane retains its own independent births/absence. A narrow white rail and
  small feathering strokes provide light without a blur/offscreen layer.
- Add native regression coverage for bright red and dark red/blue backgrounds:
  no darkening caps, continuous rail-to-rim contact, filled playhead, halo
  falloff, no body on the unplayed rail, and visible body across a time window.
  Existing width sampling moves above the thin rail bloom so it cannot falsely
  pass on a flat glowing line. Capture progress endpoints for clipping review.
- Geometry coverage checks the shallow dissolving left tail and exact rim
  contact. Palette coverage checks hue-preserving playhead light and grayscale.
  No audio pipeline, playback, unrelated UI or frozen library files changed.

Reference guidance: https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/BlendMode
and https://developer.android.com/develop/ui/compose/graphics/draw/brush
Native render/test/build verification is pending for this commit. These are
simulated Android renders, not phone GPU/FPS measurements or a claim of exact
Samsung source reconstruction.


Native follow-up (be7c337): 85/86 tests passed. The new bright-red check correctly
caught insufficient playhead bloom against an already red backdrop. The halo
now includes 25% white light while the filled centre retains the artwork hue.
Review of the native images also exposed a faint clipped stroke at the left
endpoint: feather the membrane edge-light horizontally to zero at both ends.
Keep the existing halo threshold and add a native endpoint-light check. The
geometry, independent-motion, palette, height/width and pause gates all passed;
a fresh CI run must validate this material correction before APK delivery.
