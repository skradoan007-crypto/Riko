# RIKO MUSIC — WORLD-CLASS EXPERIENCE MASTER PLAN v1.0
### "Good music player" থেকে "WAIT… এটা MUSIC PLAYER?!" — সম্পূর্ণ Product Experience Transformation

| | |
|---|---|
| Document ID | RIKO-EXPERIENCE-MASTER-001 |
| Status | DRAFT — PO review pending |
| Companion docs | SM-UI-SPEC-001 v2 (visual truth) · RIKO Music — Production-Level Premium Smoothness & Performance Engineering Pass (perf pass) · PROJECT_STATUS.md |
| মূল নীতি | FEATURE বেশি নয় — DESIGN INTELLIGENCE বেশি |
| Target | "ভাই, RIKO use করাটা অন্যরকম লাগে" |

---

## ১. EXECUTIVE VISION

RIKO এখন একটা **ভালো** music player। এই প্ল্যানের কাজ এটাকে **অনন্য** বানানো।

মূল থিসিস: সব music player একই জায়গায় generic — list rows, thin seek bar, boring sleep-timer menu, একই transport row, একই queue sheet। এই "সবাই একই করে" জায়গাগুলোতেই RIKO-র differentiation লুকিয়ে আছে। আমরা নতুন feature যোগ করব না; আমরা **একই feature-এর চারপাশে experience তৈরি করব**।

প্ল্যানের পাঁচটা pillar:

1. **THE LIVING TIMELINE** — গানের আসল amplitude-shape দেখানো একটা waveform timeline। User গান শোনার *আগেই* গানটা দেখে বোঝে কোথায় quiet intro, কোথায় drop। (SoundCloud/Poweramp waveform-এর চেয়ে এক ধাপ এগিয়ে: RIKO-র wave art-reactive + organic।)
2. **THE WIND-DOWN INSTRUMENT** — Sleep Timer যা menu না, একটা visual instrument। বাকি সময় যত কমবে, RIKO-র "ঘর" তত আসলেই অন্ধকার হবে — aurora glow আর UI brightness আসল fade-out curve-এর সাথে sync করে।
3. **ONE AURORA WORLD** — চলমান গানের color DNA (HCT-corrected) পুরো app-কে subtle-ভাবে রাঙিয়ে দেবে — canvas, glass, dock, waveform, EQ glow — গান বদলালে পুরো environment এক নিঃশ্বাসে নতুন রঙে বদলায়।
4. **MOTION WITH MEANING** — প্রতিটা animation-এর কাজ state বা relationship বোঝানো; প্রতিটা animation interruptible; artwork একটা continuous object যা row → mini player → Now Playing পর্যন্ত morph করে।
5. **SMART OBJECTS** — component-রা context বোঝে: mini player জানে গান paused কিনা, queue জানে কোনটা "next", dock জানে user scroll করছে।

**সফল হলো চেনার উপায়:** ৫ মিনিট ব্যবহারের পর user মনে রাখবে waveform-টা। ১ সপ্তাহ পর মনে রাখবে timer-টা। বন্ধুকে দেখালে বলবে — "এই app-টা দেখলেই বোঝা যায় এটা RIKO।"

---

## ২. CURRENT-STATE DIAGNOSIS (evidence-based)

### ২.১ যা ইতিমধ্যে world-class পথে আছে (preserve করতে হবে)

| সম্পদ | ফাইল | কেন দামি |
|---|---|---|
| Wave Engine (organic plasma wave, 5-solver pipeline) | `core/experience/wave/WaveEngine.kt` | RIKO-র signature; কোনো competitor-এ নেই |
| ColorIntelligenceEngine (HCT-corrected art colors) | `core/experience/ColorIntelligenceEngine.kt` | raw Palette নয় — contrast-corrected; ভিত্তি মজবুত |
| Aurora design tokens (8A2BF0→C92CEE) + glass engine | `core/designsystem/riko/theme/`, `RikoGlassSurface.kt` | সম্পূর্ণ spec-সঠিক, সুন্দর |
| EQ glow-curve (art-synced, per-band drag) | `presentation/nowplaying/EqualizerSheet.kt` | Poweramp-এর চেয়ে বেশি expressive |
| Sleep ring countdown (নতুন) | `SleepTimerSheet.kt` | ইতিমধ্যে "menu" paradigm ভাঙা শুরু |
| LiveClock + ON_STOP gating | `RikoLiveClock.kt` | continuous animation ঠিকমতো বন্ধ হয় |
| Device-tier performance gate | `MiniPlayerPerformanceGate.kt`, `FrameTimingMonitor.kt` | perf pass-এর ভিত্তি তৈরি |

### ২.২ যেখানে RIKO এখনো "generic" (এই প্ল্যানের কাজের জায়গা)

1. **Timeline** — এখন সাধারণ organic wave (সাজসজ্জা), গানের আসল amplitude-এর সাথে সম্পর্ক নেই। SoundCloud/Poweramp waveform-seek করে; RIKO করে না।
2. **Sleep Timer** — ring আছে, কিন্তু "wind-down" অনুভূতি নেই — fade শুধু লেখায় ("Playback fades out…"), আসল behavior-এ নয়।
3. **Queue** — সাধারণ sheet; "next" ট্র্যাক বোঝার কোনো visual language নেই; reorder-এ কোনো উৎসব নেই।
4. **Mini Player** — সব player-এর মতোই একটা bar; context-aware কিছু নেই; D13-এর progress hairline এখনো সবসময় 0 (state wire হয়নি)।
5. **Empty/error states** — বেশিরভাগ স্ক্রিনে minimal text; "designed" লাগে না।
6. **Motion** — দুইটা RikoMotion ফাইল (duplicated), tween+spring মিশ্রিত, কোনো interruption rule নেই, haptic ব্যবহার সম্ভবত কার্যত নেই।
7. **Environment continuity** — color brain আছে, কিন্তু গান বদলালে পুরো app-এর রঙ একসাথে "নিঃশ্বাস নেয়" — এমন অনুভূতি নেই।
8. **Artwork ভ্রমণ** — row art → mini player → hero: তিনটা আলাদা object, মাঝে কোনো সম্পর্ক নেই।

### ২.৩ Known tech debt (এই প্ল্যানের scope-এর বাইরে, তবে সীমাবদ্ধতা হিসেবে উল্লেখ)

- Monolithic `PlayerState` (4Hz tick, root-level collection) — **perf pass doc §6–§11** এ বিস্তারিত; UI phase-এ নতুন কোড এই প্যাটার্নে নতুন ঋণ তৈরি করবে না (rule §২১)।
- Duplicate RikoMotion/theme systems, Coil-এ size constraint নেই — perf pass-এর agenda।

---

## ৩. CURRENT DESIGN PROBLEMS (ranked)

| # | সমস্যা | প্রভাব | সীরিয়াসনেস |
|---|---|---|---|
| P1 | Timeline গানের সাথে সম্পর্কহীন | Hero surface একটা decoration | উচ্চ |
| P2 | Timer/Queue/MiniPlayer-এ state অনুভূতি নেই | "utility" লাগে, "instrument" লাগে না | উচ্চ |
| P3 | গান বদলালে environment বদলায় না (এক টোনে থেকে যায়) | "Alive" লাগে না | মাঝারি |
| P4 | Motion system split-brain (2টা RikoMotion) | নতুন কোড ভুল token ব্যবহার করে | মাঝারি |
| P5 | Edge states designed না | খারাপ দিনে app "সস্তা" লাগে | মাঝারি |
| P6 | Haptic প্রায় অনুপস্থিত | Interaction-এ "শারীরিক" অনুভূতি নেই | মাঝারি |
| P7 | Accent inconsistency (EQ-তে blue, বাকি অ্যাপে aurora) — সাম্প্রতিক parallel কাজের ফল | Identity ভাঙে | মাঝারি (এক লাইনে ঠিক হয়) |

---

## ৪. COMPETITIVE ANALYSIS

### ৪.১ সবাই যা একইভাবে করে (convention map)

| Convention | Spotify | Apple Music | YTM | Tidal | Poweramp | Musicolet | **সবাই মিলে যেখানে অন্ধ** |
|---|---|---|---|---|---|---|---|
| Bottom tab bar | ✓ | ✓ | ✓ | ✓ | ✗ (drawer) | ✓ | ৪-৫ tab, সবাই একই মডেল |
| List row (art+title+menu) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | প্রায় অক্ষরে এক |
| Mini player bar | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | সবারটা কার্যত identical |
| Seek = plain bar | ✓ | ✓ | ✓ | ✓ | waveform | বার | ব্যতিক্রম মাত্র ২ |
| Sleep timer = text menu | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | **কেউই instrument বানায়নি** |
| Queue = plain reorder list | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | "next" বোঝার ভাষা নেই |
| EQ = vertical sliders+presets | n/a | n/a | n/a | ✓ | ✓ | ✓ | glow/context কারও নেই |
| Album color ambient | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | সবাই screen-level, continuity নেই |
| Empty state = icon+text | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | কেউ design করে না |

### ৪.২ Insight: সবাই যেখানে এক — সেখানেই প্রশ্ন

- **"Sleep timer কেন সবাই text menu করে?"** — কারণ কেউ ভাবেনি। Timer একটা *সময়ের* জিনিস; সময় দেখানোর মানে clock/instrument। ← RIKO Moment #2।
- **"Waveform seek কেন সবাই করে না?"** — কারণ decode cost ভয় পায়। RIKO-র কাছে ইতিমধ্যে analysis infrastructure (AudioFileAnalyzer) আছে। ← RIKO Moment #1।
- **"গান বদলালে app বদলায় কেন না?"** — Spotify রঙ বদলায়, কিন্তু "environment" বদলায় না। RIKO-র glass+aurora architecture এটার জন্ম দিয়েছে ইতিমধ্যে। ← RIKO Moment #3।

### ৪.৩ RIKO-র অবস্থান বিবৃতি

> Poweramp = শক্তিশালী engine, দুর্দান্ত customization, কিন্তু utilitarian চেহারা।
> Spotify = content-এর রাজা, player অংশটা generic।
> Tidal = পরিষ্কার, কিন্তু নিরীহ।
> **RIKO = player-টাই যদি experience হয়।** Engine-এর গভীরতা Poweramp-এর দিকে, অনুভূতি Spotify-এর চেয়ে বেশি personal — আর প্রতিটা ছোট জিনিসে (timer, queue, timeline) এমন মাথা যা কারও অ্যাপে দেখা যায় না।

---

## ৫. RIKO DESIGN PHILOSOPHY — SMARTER, NOT BUSIER

অয়ৌক্তিকভাবে সংক্ষেপে: **প্রতিটা pixel-এর একটা কাজ আছে।**

৮-প্রশ্নের ফিল্টার (প্রতিটা নতুন interaction-এর আগে জিজ্ঞেস করব):
1. Usability বাড়ায়? 2. Discoverability বাড়ায়? 3. Cognitive load কমায়? 4. System state পরিষ্কার করে? 5. Intelligent feel দেয়? 6. Emotional connection বাড়ায়? 7. Memorable করে? 8. RIKO identity গড়ে?

**Score < 2 → বাদ।** Score ≥ 2 কিন্তু perf cost বেশি → device-tier gate-এ ঢুকবে (flagship-এ সম্পূর্ণ, low-end-এ gracefully কমে)।

তিনটা স্থায়ী নীতি:
- **Alive ≠ Noisy** — প্রতিটা স্ক্রিনে সর্বোচ্চ একটা "জীবন্ত" উপাদান (wave, ring, glow)। বাকি সব শান্ত।
- **Feedback = Information** — প্রতিটা animation user-কে নতুন কিছু জানাবে (কোথায় আছি, কী হলো, এরপর কী); pure decoration নিষিদ্ধ।
- **Respect the pause** — paused অবস্থায় app শান্ত হবে: wave থেমে যায়, glow কমে, breathing থামে। Music বাজলে তবেই app বাজবে।

---

## ৬. DIFFERENTIATION STRATEGY

**আমরা যেখানে লড়ব না:** feature count (Poweramp-এর ৫০টা setting-এর সাথে লড়াই আত্মহত্যা), catalog/cloud (Spotify-এর মাঠ), price।

**আমরা যেখানে লড়ব:** প্রতিটা মুহূর্তের *অনুভূতি* — track বদলানো, seek করা, রাতে timer সেট করা, queue সাজানো। এই মুহূর্তগুলোই একজন user দিনে ৫০ বার করে; এদের অভিজ্ঞতা ১% উন্নত করা = product-এর ১০০% আলাদা লাগা।

Differentiation stack (উপরেরটা সবচেয়ে দৃশ্যমান):
1. Living Timeline (প্রতি গানে, প্রতি স্ক্রাবে দেখা যায়)
2. Wind-Down Timer (যারা রাতে শোনে — সবচেয়ে আনুগত্যপূর্ণ মুহূর্ত)
3. Aurora Environment (চেনা অনুভূতি, ব্যাখ্যা করা কঠিন — এটাই moat)
4. Motion/Haptic grammar (ব্যাখ্যাহীন "মহঙ্গ" অনুভূতি)
5. Designed edge states (খারাপ দিনেও ভালো লাগা)

---

## ৭. SMART UI OBJECT STRATEGY

সংজ্ঞা: **Smart UI Object** = context/state/content বুঝে নিজের presentation বা behavior বদলায় এমন component। Animation ≠ Intelligence — বদলানো-টা হতে হবে *অর্থপূর্ণ*।

| Smart Object | কী বোঝে | কী বদলায় | User value | Feasibility | Rank |
|---|---|---|---|---|---|
| **Living Timeline** | গানের আসল amplitude, playhead, buffered, gesture | আকৃতি (প্রতি গানে), রঙ (played/unplayed), প্লেহেড-জোন bounce, হ্যাপটিক টিক | গান *দেখা* যায়; স্ক্রাব নিখুঁত | উচ্চ (analyzer আছে; Canvas) | S |
| **Wind-Down Sheet** | বাকি সময়, fade curve | ambient brightness 100→65%, ring sweep, শেষ ৩০ সেকেন্ডে breathing | ঘুমের আগের মুহূর্ত নরম | উচ্চ | S |
| **Context Mini Player** | paused/playing/loading/error | paused → play-icon বড় + "Resume" মাইক্রোকপি; loading → shimmer thumb; error → লাল hairline + retry | অবস্থা এক চোখে বোঝা | উচ্চ | A |
| **Aurora Environment** | চলমান গানের color DNA | canvas glow, glass tint, dock circle, EQ/wave রঙ — সব এক brain থেকে | app গানের সাথে বাস করে | মাঝারি (brain আছে, ফিড করতে হবে) | A |
| **Scroll-Aware Dock** | scroll দিক | নিচে scroll → dock+mini slide-out; উপরে/ট্যাপ → ফেরে | ২০% বেশি content দেখা যায়; immersive | মাঝারি | A |
| **Speaking Queue Row** | current/next/rest, drag state | current = pinned + live 3-bar; next = aurora edge; grab = scale+lift | queue এক চোখে পড়ে | মাঝারি | A |
| **Deterministic Placeholder Art** | song-id hash | artwork-নেই গানে hash→aurora-tone gradient (একই গান = একই রঙ, সব স্ক্রিনে) | ফাঁকা ঘর নয় — designed ঘর | উচ্চ | B |
| **Adaptive FX Sliders** | toggle অবস্থা | off → slider বন্ধ + মান লুকায় (ইতিমধ্যে আছে); on → gradient reveal | ইতিমধ্যে built — preserve | ✓ built | ✓ |

**প্রতিটি object-এর বিস্তারিত spec §৯–§১২-এ।** সব object-এর common rule: state বদলানো সবসময় spring-animated (interruptible), আর state যদি বদলায় ৩ বারের কম হয় সারা জীবনে — তাহলে animate করবে না।

---

## ৮. SIGNATURE COMPONENT STRATEGY

"এটা RIKO-র জিনিস" — ভবিষ্যতে এই চিনির স্তূপ:

1. **RikoWaveTimeline** (§৯) — প্রতিটা screenshot-এ থাকবে
2. **RikoWindDown** (§১০) — "timer-এর জন্য এত মাথা?!"
3. **RikoAurora** (environment protocol, §১৭) — অদৃশ্য কিন্তু সর্বব্যাপী
4. **RikoDock** (sliding-circle capsule + scroll-aware)
5. **RikoGlassSheet** (সব bottom sheet এক chrome: aurora substrate + handle + glass) — ইতিমধ্যে আছে, preserve
6. **RikoMorph** (artwork shared-element, §১২)
7. **RikoHaptic** grammar (light/medium/success টোকেন)
8. **RikoEmpty** (empty-state language: aurora wave + এক CTA)

নিয়ম: signature = একই design intelligence বারবার, ভিন্ন প্রেক্ষাপটে। সব স্ক্রিনে সব effect নয়।

---

## ৯. TIMELINE — HERO EXPERIENCE

### Concept
Timeline = TIME + MUSIC + MOTION + EMOTION-এর সম্পর্ক। RIKO-র timeline গানের **আসল আকৃতি** দেখাবে: ৩ মিনিটের গানের amplitude envelope আগেই বিশ্লেষণ করে (প্রথম প্লেতে ~৫০-১৫০ms, cache) ৯৬-১২০টা বারে সাজানো। Quiet intro = নিচু বার, drop = লম্বা বার। **User গান দেখে বোঝে — শোনার আগেই।**

### Visual behavior
- **আকৃতি:** ৯৬-১২০ vertical bars, 2dp wide / 3dp gap, মিররড amplitude (spec 2.20 ভাষা), rounded caps। আকৃতি = আসল ডেটা (fallback: title-hash pseudo-wave, edge-case §২০)।
- **রঙ:** played = Aurora gradient fill; unplayed = white 38%; buffered = played-এর নিচে white 12% স্তর।
- **Playhead:** 2dp aurora উল্লম্ব রেখা + 12dp গ্লো; বাজার সময় তার দুপাশে ২-৩টা বার ±৮% জীবন্ত bounce (~9Hz) — খুব সামান্য, "app গানের সাথে বাস করছে" বোঝাতে। Paused → bounce থামে (respect the pause)।
- **গান বদলালে:** পুরনো আকৃতি fade+scale-down (150ms), নতুনটা fade-in (300ms) — কোনো jump নেই।

### Interaction behavior
- পুরো timeline = একটা scrub surface: আঙুল যেখানে, সেখানেই জাম্প (thumb-নির্ভর নয়)।
- ড্র্যাগে: floating time-chip আঙুলের উপরে ভাসে (১:১ follow, কোনো easing নয়), playhead আঙুল ছাড়ে না।
- **Haptic tick** প্রতি ৩০ সেকেন্ডের সীমানায় (clockwise টিকের মতো) — চোখ বন্ধ করেও seek করা যায়।
- ছাড়লে: 100ms settle + seek commit। ড্র্যাগ চলাকালীন wave-এর live-bounce বন্ধ (অঙ্গভঙ্গিকে দর্শক-আসন দেওয়া)।

### Motion behavior
- Progress advance = 1:1 position mapping (কোনো easing নেই — সরাসরি অডিও ঘড়ি, spec 5.8)।
- Live bounce = spring, প্রতি বার স্বাধীন দশা (রোবটিক লাগে না)।
- **Interruption:** গান মাঝপথে বদলালে bounce spring গুলো নতুন song-এ retarget — কখনো আটকে থাকে না (Compose spring স্বাভাবিকভাবেই interruptible; tween ব্যবহার নিষিদ্ধ এখানে)।

### পার্থক্যের মূল্য
SoundCloud waveform = স্ট্যাটিক ধূসর গ্রাফ। Poweramp = টেকনিক্যাল। **RIKO = গানের আকৃতি গায়ে অরোরা-র রঙ, অঙ্গভঙ্গির সাথে ন্যায্য, হ্যাপটিক-ভাষী।** এটা এমন জিনিস যেটা user অন্য player-এ খুঁজবে — আর পাবে না।

### প্রযুক্তিগত বাস্তবতা (Compose)
- Analysis: `AudioFileAnalyzer` আছে — MediaCodec downsample (প্রতি ৪০৯৬ স্যাম্পলে ১ peak), 3-min track ≈ 100-200ms ব্যাকগ্রাউন্ডে। Cache: `LruCache<songId, FloatArray>` + optional Room টেবিল (perf pass পরে সিদ্ধান্ত)।
- Render: এক `Canvas`; bars static — শুধু playhead-জোন graphicsLayer translate-এ invalidate; মোট প্রতি-ফ্রেম ব্যয় < 1ms (মাঝারি ফোন)। Analysis ব্যস্ত সময়ে chunked coroutine (মেইন থ্রেড মুক্ত)।
- Perf cost: **নিম্ন** (তুলনা: এখনকার ৩-স্তরের blur ওয়েভ আরও ব্যয়বহুল)। Device-tier: low-end-এ bounce বন্ধ, বাকি সব থাকে।

---

## ১০. SLEEP TIMER REDESIGN — "WIND-DOWN INSTRUMENT"

### Concept
ঘুমের আগের মুহূর্ত দিনের সবচেয়ে কোমল মুহূর্ত — আর সব player সেখানে একটা text menu দেখায়। RIKO দেখাবে একটা **instrument**: dial ঘুরিয়ে সময় সেট, বাকি সময়ের সাথে ঘরটা আসলেই অন্ধকার হয়।

### Visual behavior (বর্তমান ring-এর বিবর্তন)
1. **Dial mode (timer বন্ধ থাকলে):** বৃত্তটাই ডায়াল — আঙুল ঘুরিয়ে ৫-১৮০ মিনিট সেট; aurora sweep আঙুলকে 1:1 follow করে; center-এ বড় সংখ্যা; preset chips (৫/১০/১৫/৩০/৪৫/৬০/৯০) দ্রুত-পথ হিসেবে থাকবে।
2. **Burn-down mode (চলছে):** sweep = বাকি সময়ের ভগ্নাংশ (আছে)। নতুন: **ambient fade** — sheet-এর aurora glow ও content brightness বাকি সময়ের সাথে 100% → 65% পর্যন্ত নামবে (linear নয়, ease-out)। শেষ ৫ মিনিটে প্লে/পজ আইকনের breathing ধীর হবে।
3. **End state:** হঠাৎ বন্ধ নয় — শেষ ৩০ সেকেন্ডে PlayerManager volume fade (config toggle: "Fade last 30s", default on) → ০; এক soft haptic; artwork 20% পর্যন্ত ডিম; sheet নিজেই ভাঁজ হয়ে যায়। পরদিন সকালে: স্বাভাবিক paused state, কোনো "ভাঙা" অনুভূতি নেই।

### Interaction behavior
- Dial drag = 1:1; প্রতি ৫ মিনিট ধরাকাটা (snap) light haptic; ছাড়ামাত্রই টাইমার চালু।
- চলমান অবস্থায় ring ট্যাপ = +৫ মিনিট extend; "Cancel timer" text button (আছে)।
- Custom = dial-ই (আলাদা dialog-এর দরকার পড়বে না — বর্তমান CustomTimerDialog সরে যাবে)।

### Anti-gimmick পরীক্ষা
Ambient fade = **তথ্য** (কতটা বাকি) — novelty বাদ দিলেও useful। Volume fade = ইতিমধ্যে ঘোষিত behavior-কে বাস্তবে দেখানো। Pass।

### প্রযুক্তি
Brightness/glow = একটা animated float (0.65..1) → ColorMatrix/alpha; PlayerManager-এ `fadeOut(seconds)` (volume setVolume ramp — Media3 supports); কোনো per-frame ব্লার নেই। Cost: **নিম্ন**। Priority: S-এর ঠিক নিচে (A+), কারণ timeline-এর পরেই সবচেয়ে বড় "কীভাবে ভাবলো!" মুহূর্ত।

---

## ১১. NOW PLAYING REDESIGN (চলমান ভিত্তির উপর)

আজকের অবস্থা (SM-UI-SPEC মিল): AppBar-B, 280dp circle art, শিরোনাম+শিল্পী, waveform, transport (D5 order), ৫টা glass action। সমস্যা: **গান বদলালে স্ক্রিন প্রায় স্থির**।

### পরিবর্তনগুলো
1. **Song-change ritual (A-rank):** গান বদলালে 4-step নৃত্য — ①পুরনো waveform আকৃতি fade-out (150ms) ②শিরোনাম+শিল্পী slide-up ক্রসফেড (200ms, staggered 40ms) ③artwork crossfade+scale 0.96→1.0 (300ms) ④পুরো পরিবেশের রঙ এক নিঃশ্বাসে নতুন DNA-তে (400ms)। একসাথে নয় — এই ক্রমটাই ছন্দ।
2. **Living Timeline বসানো (S-rank):** বর্তমান organic wave-এর জায়গায় §৯-এর RikoWaveTimeline। (পুরনো plasma wave Wave Laboratory-তে বাঁচবে — debug surface।)
3. **Artwork জীবন্ত (B-rank):** বাজলে খুব ধীর "breathing" scale 1.0→1.012 (7s cycle, LiveClock ভাগ করা); paused → স্থির। কোনো rotation নয় (spec-বিরোধী)।
4. **Lyrics মুহূর্ত (C-rank, পরে):** LRC engine আছে — পরে phase; এই প্ল্যানে শুধু জায়গা রাখা।

### যা বদলাবে না
D5 transport order, ৫-glass-action row, পরিবেশ-নীতি — ইতিমধ্যে spec-ঠিক।

---

## ১২. MINI PLAYER + MORPHING ARTWORK

### Context Mini Player (A-rank)
- **D13 hairline wire করা:** progress আসলে ফিড করা (এখন সবসময় 0)। Aurora gradient hairline, 2dp।
- **Context states:** playing = play/pause + next; paused = বড় play-icon + শিরোনামের পাশে "Resume" মাইক্রোকপি (60% white); loading = thumb-এ shimmer; error = লাল hairline + tap→retry। প্রতিটা অবস্থা spring-এ বদলাবে।
- **Gestures:** tap = খোলা (আছে); **swipe-right = next track** (নতুন, এক হাতের দৈনন্দিন কাজ দ্রুত); swipe-left = dismiss নয় — queue-এর পরবর্তী প্রিভিউ নয়... **বাদ**। শুধু swipe-right=next (আন্দোলন = সামনে অর্থ)।
- 64dp, glass, floating — অপরিবর্তিত (spec D13)।

### Morphing Artwork (A-rank, phase-3)
এক জিনিস, তিন রূপ: list row 12dp square → mini player thumb → hero 280dp circle। Compose **SharedTransitionLayout** (BOM 2024.09-এ experimental কিন্তু stable-enough) দিয়ে mini→Now Playing morph; row→mini সংস্করণ C-rank (perf)। Fallback (পুরনো API): scale+fade illusion। এটা spec 5.2-এর "single highest-impact animation"।

---

## ১৩. NAVIGATION STRATEGY

### গঠন (অপরিবর্তিত)
Home · Favourites · Library(My Music) · Settings — floating dock, sliding aurora circle (নতুন করে ঠিক করা)। Search = header icon থেকে। Playlist = Library-র ভেতরে (D12)।

### স্মার্ট সংযোজন
1. **Scroll-Aware Dock (A-rank):** নিচের দিকে scroll → dock+mini player নিচে slide-out (200ms, easing-emphasized); উপরে scroll বা bottom-overscroll → ফেরে। Immersive list, ২০% বেশি content। নিয়ম: tab switch-এ সবসময় দৃশ্যমান।
2. **Tab transitions:** বর্তমান fade+scale (200/100ms) ঠিক আছে — peer destination, slide নয় (spec 5.2)।
3. **Push transitions:** এখন fade — পরিবর্তন: detail/push-এ slide-from-right + পিছনের স্ক্রিন 96% scale+dim (400ms, spec 5.2-এর iOS-ধাঁচ)। ব্যয়: NavHost transition config — সস্তা।
4. **Back prediction (C-rank):** predictive-back জেসচারে পিছনের স্ক্রিনের ঝলক — পরে।

---

## ১৪. MICRO-INTERACTION SYSTEM

### আজকের সত্য (অডিট)
Haptics শুধু engine-লেয়ারে (audio host/library) — **UI ব্যাকরণে প্রায় শূন্য**। Press-scale কোথাও আছে (0.96-0.975), কোথাও নেই। Queue-তে drag-reorder আছে (draggedIndex+offset) — কিন্তু grab/drop-এ কোনো haptic/celebration নেই।

### একক ব্যাকরণ (একবার define, সবাই ব্যবহার করবে)
| ট্রিগার | Feedback | কেন |
|---|---|---|
| যেকোনো tap-down | scale 0.96 (spring 100ms) + **light haptic** | "নেওয়া হয়েছে" |
| Tab switch | dock circle slide (spring) + light haptic | স্থানান্তর |
| Play/Pause | medium haptic + আইকন morph | প্রাণের স্পন্দন |
| Favorite on | scale 1→1.15→1 + **success haptic** (double-tick) | উদযাপন, কণা-বিস্ফোরণ নয় |
| Scrub tick (timeline) | light haptic / 30s | চোখ ছাড়া নেভিগেশন |
| Queue grab/drop | lift shadow+scale / settle thud haptic | ভৌত বস্তু |
| Toggle | spring slide (আছে) + light | — |
| ভুল/অপ্রাপ্য (disabled tap) | কোনো scale নয়, শুধু 60%-এ থেমে থাকা | নীরব প্রত্যাখ্যান |

নিয়ম: **haptic সবসময় visual-এর ঠিক সাথে** (আগে নয় পরে নয়); একই action-এ ২-এর বেশি haptic নেই।

### Interruptible-by-default
সব state-animation spring; কোনো gesture-ভিত্তিক মানে tween নিষিদ্ধ। মাঝপথে interrupt → spring নতুন লক্ষ্যে মসৃণ retarget (Compose নিজেই করে — শুধু tween বাদ)।

---

## ১৫. MOTION SYSTEM (এক RikoMotion)

**সমস্যা:** দুইটা RikoMotion (riko/theme + theme) — split-brain। **সমাধান:** এক ফাইল, টোকেন নিচে:

```
Durations : fast 100ms · base 200ms · medium 300ms · slow 400ms
Easings   : standard (0.2,0,0,1) · emphasized-enter (0.3,0,0.8,0.15) · emphasized-exit (0.05,0.7,0.1,1)
Springs   : snappy (stiffness 300, damping ~26 — LowBouncy) · soft (stiffness 150, high damping)
Rules     : state→spring · gesture→1:1 (no spec) · screen→tween+fade · interruptible always
Stagger   : list-entrance 30-40ms/item, সর্বোচ্চ 6 আইটেম (spec 5.9)
ReduceMotion: ANIMATOR_DURATION_SCALE==0 বা accessibility setting → সব duration 0, infinite সব static, wave amplitude frozen
```

Reduce-motion এবং device-low-tier **একই সুইচ** ভাগ করবে (এক ব্যবস্থা, দুই ট্রিগার) — কোড দ্বিগুণ নয়।

---

## ১৬. DYNAMIC VISUAL LANGUAGE (AURORA ENVIRONMENT)

**Input:** বর্তমান গানের artwork → ColorIntelligenceEngine → ৩ সুর: `primary / glow / deep`।

**Feed (এক brain → সব অঙ্গ):**
- Now Playing canvas glow (আছে)
- Shell ambient (আছে — সব ট্যাবে)
- Dock-এর active circle-এর গ্লো হ্যালো (নতুন, খুব সূক্ষ্ম)
- Waveform played-রঙ টিন্ট (নতুন — aurora-র সাথে 15% art-blend)
- Mini player hairline (নতুন)
- Sheet substrate-এর নিচের টিন্ট (আছে)

**গান বদলানোর নিঃশ্বাস:** সব রঙ একই মুহূর্তে নয় — 400ms-এ একটা তরঙ্গ: প্রথমে Now Playing, তারপর shell, শেষে dock (staggered 60ms)। App যেন একটা জিনিস।

**সীমা:** low-tier/reduced-motion → instant swap, কোনো stagger নয়। Text contrast কখনো ভোগাবে না — রঙ শুধু glow/tint স্তরে, text সবসময় white-স্কেল (C5 মান অটুট)।

---

## ১৭. DESIGN SYSTEM CONSOLIDATION

**এক উৎস নীতি — দ্বিগুণ ব্যবস্থা বিদায়:**
1. **Typography:** সব Baloo 2। Legacy `Type.kt` (FontFamily.Default!) → riko scale-এ পুনর্নির্দেশ। ফল: sheet/dialog-গুলোও Baloo পাবে (আজ মিশ্রিত)।
2. **Color:** `core/designsystem/theme/Color.kt`-এর পুরনো টোকেন (@Deprecated) → riko টোকেনে মাইগ্রেশন ম্যাপ। AccentBlue-যুক্ত নতুন EQ → Aurora-তে ফেরানো (P7) — এক-লাইন ঠিক।

## ১৮. EDGE-CASE SYSTEM — "খারাপ দিনেও designed"

Enterprise বোঝা যায় ideal case-এ নয়। প্রতিটা state-এর মালিক ঠিক:

| State | মালিক | আজকের অবস্থা | Designed আচরণ |
|---|---|---|---|
| লাইব্রেরি খালি | HomeEmpty | text+icon (নিরীহ) | ম্লান aurora wave ধীরে শ্বাস নিচ্ছে + "RIKO waits for your music" + একটাই CTA (permission) |
| Permission denied | HomeEmpty/AllSongs | বর্তমান সংলাপ | এক স্ক্রিনে কেন-দরকার (২ লাইন) + বাটন; অস্বীকৃতি = নিরীহ নয়, মর্যাদাপূর্ণ ব্যাখ্যা |
| ১টা গান | সব list | কাজ করে | বিশেষ চিকিৎসা নয় (সঠিক) — শুধু header-count একবচন ঠিক (আছে) |
| ৫০,০০০ গান | LazyColumn | key আছে, contentType নেই | contentType + স্থিতিশীল মডেল + fast-scroll স্ক্রাবার (C-rank) — perf pass-এর সাথে হাতে হাতে |
| Artwork নেই | সারি/hero | ফাঁকা box+icon | **Deterministic placeholder**: songId-hash → aurora টোন gradient; একই গান সব স্ক্রিনে এক রঙ |
| বিশাল শিরোনাম/শিল্পী | সব Text | ellipsis বেশিরভাগে | Now Playing শিরোনামে marquee (শুধু সেখানে), বাকিতে 1-line ellipsis |
| ভাঙা metadata | সারি | "Unknown Album" আছে | টাইটেল = ফাইলনাম পরিষ্কার (cleanSongTitle আছে) — ডকুমেন্টেড রাখা |
| মোছা ফাইল / playback error | PlayerState.error | প্রায় অদৃশ্য | mini player-এ লাল hairline + auto-skip পরবর্তী + snackbar "Skipping unavailable track" |
| খালি playlist | CollectionEmptyState | আছে (ভালো) | preserve |
| বিশাল queue | QueueSheet | LazyColumn | pinned current + index টিকে থাকে — preserve |
| Landscape | NavHost | অচ্ছেদ্য | R1-এ scope-বাইরে ঘোষণা; portrait-first পণ্য সিদ্ধান্ত |
| Font scale 130% | সারি 76dp | ঝুঁকি | সারি min-height নমনীয়; a11y pass-এ যাচাই |
| Offline | সব | N/A (সম্পূর্ণ অফলাইন) | এটা RIKO-র শক্তি — onboarding-এ গর্বের এক লাইন: "No account. No internet. Just music." |

নিয়ম: **নতুন কোনো স্ক্রিন তখনই "সম্পূর্ণ" যখন তার empty+error state-ও ডিজাইন করা।**

---

## ১৯. ACCESSIBILITY SYSTEM

- **কনট্রাস্ট:** white 100/60/38 স্কেল C5-এ গণিত — 60% pass (6.7:1), 38% শুধু বড়/অলঙ্কৃত টেক্সটে। নিয়ম হিসেবে এনফোর্স।
- **টাচ টার্গেট:** ন্যূনতম 44dp — transport (আছে), kebab 32→44 প্যাড (আছে), dock item 56 (আছে)। নতুন সব কম্পোনেন্টে বাধ্যতামূলক।
- **TalkBack:** প্রতিটা icon-only নিয়ন্ত্রণে অবস্থা-সচেতন লেবেল ("Play Midnight Vibes" / "Pause"); সারিতে merged semantics (এক স্বরে পড়া); timeline-এ ProgressBarRangeInfo + state announce।
- **Reduce motion:** §১৫-এর সুইচ — infinite সব (breathing, glow, live-clock float) static; springs → duration 0; কার্যকারিতা অটুট, শুধু নড়াচড়া বন্ধ।
- **রঙ-নিরপেক্ষ অবস্থা:** playing সারি শুধু রঙ নয় — ছোট equalizer-glyph; error শুধু লাল নয় — icon+text।

---

## ২০. PERFORMANCE CONSTRAINTS (UI পর্যায়ের নিয়ম)

এই প্ল্যান UI-first। কিন্তু UI কাজের সময় নতুন ঋণ নিষিদ্ধ — নিয়মগুলো perf pass-এর সাথে সাংঘর্ষিক নয়, পথ প্রশস্ত করে:

1. **প্রতি-ফ্রেম বরাদ্দ শূন্য:** প্রতিটা নতুন Canvas/animation — অবজেক্ট আগে তৈরি, draw-এ শুধু অঙ্কন।
2. **Blur বাজেট:** প্রতি স্ক্রিনে সর্বোচ্চ ১টা ভারী blur — শুধু Now Playing artwork (65dp)। নতুন blur = tier-gate বাধ্যতামূলক।
3. **List rows:** GlassTier.List (আছে) — সারিতে নতুন specular/shadow/spring নয়।
4. **Continuous animation:** প্রতিটা নতুন infinite transition ON_STOP-gate + reduced-motion-gate — দুটোই বাধ্যতামূলক। এক স্ক্রিনে একটার বেশি নয়।
5. **Image:** নতুন সব Coil request-এ স্পষ্ট `.size()`; crossfade শুধু hero-স্তরে।
6. **Per-idea cost line:** এই ডকের প্রতিটা S/A আইটেমে ব্যয়-রেখা আছে; গেট: FrameTimingMonitor p95 < 8ms মাঝারি ফোনে।
7. **Perf pass পরে হবে** ("Premium Smoothness" doc) — আগে UI 100% → build → user sign-off। এই ক্রম অপরিবর্তিত।

---

## ২১. ANTI-GIMMICK RULES (স্থায়ী আইন)

**প্রতিটা "wow"-এর আগে:** novelty বাদ দিলেও কি interaction useful? না → REJECT।

**স্থায়ীভাবে বাদ (REJECT তালিকা):**
- প্রিয়তে কণা-বিস্ফোরণ (particles) — শিশুসুলভ; scale+হ্যাপটিকই যথেষ্ট
- স্ক্রলে 3D tilt-parallax সারি — জিমিক + পারফ খরচ
- প্রতিটা স্ক্রিনে visualizer — "Alive ≠ Noisy"; শুধু Now Playing + mini hairline
- সর্বত্র নিয়ন-গ্লো / সর্বত্র glass — স্তরের শ্রেণিবিন্যাস নষ্ট
- List-এ Haze/বাস্তব blur — 15fps ইতিহাস মনে আছে
- লুকানো জেসচার-নির্ভর একমাত্র পথ (কোনো feature-এর একমাত্র প্রবেশপথ কখনো লুকানো নয়)
- Fake physics / সর্বত্র bouncy — উদযাপনে bouncy, কাঠামোয় কখনো নয়

**রাখার পরীক্ষা:** "এটা কি ব্যাখ্যা ছাড়াই বোঝায়?" — Living Timeline (গানের আকৃতি), Wind-Down (সময় ফুরাচ্ছে), Aurora (গান বদলেছে) — তিনটাই ব্যাখ্যা ছাড়াই বোঝায়। এগুলোই থাকবে।

---

## ২২. PRIORITY MATRIX

| Rank | আইটেম | প্রভাব | পার্থক্য | Usability | Feasibility | পারফ ব্যয় | MUST? |
|---|---|---|---|---|---|---|---|
| **S** | Living Timeline (§৯) | বিস্ময়কর | অতুলনীয় | স্ক্রাব উন্নত | মাঝারি | নিম্ন | **হ্যাঁ** |
| **S** | Wind-Down Timer (§১০) | আবেগঘন | অতুলনীয় | সেট-সহজ | মাঝারি | নিম্ন | **হ্যাঁ** |
| **A** | Aurora Environment নিঃশ্বাস (§১৬) | বড় | উচ্চ | নিরপেক্ষ | মাঝারি | নিম্ন | হ্যাঁ |
| **A** | Song-change ritual (§১১) | বড় | মাঝারি | নিরপেক্ষ | উচ্চ | নিম্ন | হ্যাঁ |
| **A** | Context Mini Player + hairline (§১২) | বড় | মাঝারি | অবস্থা-সচেতন | উচ্চ | নিম্ন | **হ্যাঁ** |
| **A** | Scroll-Aware Dock (§১৩) | মাঝারি | মাঝারি | জায়গা | মাঝারি | নিম্ন | না (strong polish) |
| **A** | Morphing Artwork (§১২) | বিস্ময়কর | উচ্চ | স্থান-বোধ | মাঝারি-কঠিন | মাঝারি | না (phase-3) |
| **A** | Haptic ব্যাকরণ (§১৪) | অদৃশ্য-বড় | মাঝারি | স্পর্শ | উচ্চ | শূন্য | **হ্যাঁ** |
| **A** | Motion একীকরণ (§১৫) | ভিত্তি | — | নিরপেক্ষ | উচ্চ | শূন্য | **হ্যাঁ** |
| **B** | Speaking Queue (§৭) | মাঝারি | মাঝারি | স্পষ্টতা | মাঝারি | নিম্ন | না |
| **B** | Edge-state ভাষা (§১৮) | খারাপ দিনে বড় | মাঝারি | বড় | উচ্চ | নিম্ন | হ্যাঁ (B-তে প্রথম) |
| **B** | Deterministic placeholder art | মাঝারি | মাঝারি | মাঝারি | উচ্চ | শূন্য | না |
| **B** | Breathing artwork | সূক্ষ্ম | মাঝারি | নিরপেক্ষ | উচ্চ | নিম্ন | না |
| **C** | Alphabet fast-scroll | বড় লাইব্রেরিতে | নেই | বড় | মাঝারি | নিম্ন | পরে |
| **C** | Lyrics মুহূর্ত | বড় (পরে) | মাঝারি | মাঝারি | কঠিন | মাঝারি | পরে |
| **C** | Predictive back | সূক্ষ্ম | নেই | সূক্ষ্ম | মাঝারি | নিম্ন | পরে |

MUST-HAVE তালিকা: Living Timeline · Wind-Down · Mini Player context+hairline · Haptics · Motion একীকরণ · Edge-states · Aurora feed।
NICE-TO-HAVE: Scroll-Dock · Speaking Queue · Placeholder art · Breathing · Morph।
PURE EXPERIMENT: Lyrics মুহূর্ত · Predictive back · fast-scroll।
দেখতে দারুণ কিন্তু বাদ: particles · tilt-parallax · সর্বস্ক্রিন visualizer।

---

## ২৩. IMPLEMENTATION ROADMAP

> পূর্বশর্ত: চলমান SM-UI-SPEC v2 fidelity build **সবুজ + ডিভাইসে user sign-off**। তার আগে কিছুই শুরু নয়।

| Phase | নাম | কাজ | আনুমানিক | Exit গেট |
|---|---|---|---|---|
| **R0** | Fidelity Freeze | চলমান spec-rebuild শেষ → CI সবুজ → ডিভাইস QA → PO sign-off | চলছে | user "UI 100%" বললে |
| **R1** | Foundations | এক RikoMotion + reduce-motion সুইচ + RikoHaptic ব্যাকরণ + amplitude analyzer pipeline (analysis+cache, UI ছাড়া) | ৩-৪ দিন-সমতুল্য | haptic সর্বত্র; analyzer unit-test |
| **R2** | Timeline Hero | RikoWaveTimeline কম্পোজ + scrub/haptics/bounce + Now Playing-এ swap | ৪-৫ | FrameTiming p95<8ms |
| **R3** | Wind-Down | dial + burn-down + ambient fade + volume-fade + end state | ৩-৪ | ৩০s fade যাচাই; রাতের ব্যবহার |
| **R4** | Environment | aurora feed সব অঙ্গে + song-change ritual + context mini player + hairline wire | ৩-৪ | গান-বদল অভিজ্ঞতা |
| **R5** | Dock & Queue | scroll-aware dock + speaking queue স্তর | ২-৩ | স্ক্রলে dock অখণ্ড |
| **R6** | Edge & A11y | §১৮/§১৯ পূর্ণ প্রয়োগ + TalkBack যাত্রা | ৩ | প্রতিটি স্ক্রিনের empty/error designed |
| **R7** | Morph (ঐচ্ছিক) | SharedTransition artwork | ৩-৪ | পুরনো-ডিভাইস fallback |
| **R8** | **PERF PASS** | "Premium Smoothness" doc পূর্ণ প্রয়োগ (granular state, stability, Coil, baseline profile) | doc অনুযায়ী | p95, jank% লক্ষ্য |
| **R9** | Final QA | pixel-diff ≤1% (C4) + সামগ্রিক যাত্রা-পরীক্ষা + sign-off সনদ | ২ | PO+DL স্বাক্ষর |

**মোট:** R1-R7 ≈ ৩-৪ সপ্তাহ কাজ-সমতুল্য; R8 আলাদা (তোমার perf doc প্রস্তুত)।

---

## ২৪. "DO THIS FIRST" — আগামীকাল থেকে ৫টা কাজ

1. **R0 শেষ করো:** চলমান fidelity build সবুজ → ফোনে APK → "UI ঠিক আছে?" — এই সিল ছাড়া কিছু নয়।
2. **AccentBlue EQ → Aurora** (P7): এক-লাইন ঠিক — identity ভাঙা বন্ধ।
3. **এক RikoMotion ফাইল:** দুইটা মুছে একটা; সব import পুনর্নির্দেশ; reduce-motion সুইচ ভেতরেই।
4. **RikoHaptic.kt:** ৪ ফাংশন (light/medium/success/tick) — আর কোথাও raw HapticFeedback নয়।
5. **Analyzer spike:** এক test-composable-এ যেকোনো গানের amplitude বের করে লগ — R2-র জমিন প্রমাণ।

---

## ২৫. FINAL RIKO PRODUCT VISION

**ব্যক্তিত্ব:** একজন চিন্তাশীল বন্ধু যে রাতে তোমার গান বাজায় — জোরে চিৎকার করে না, খেয়াল রাখে, ঘরটা তোমার সাথে অন্ধকার হয়।

- **দৃশ্য:** গাঢ় নীল-কালো মহাকাশ, আরোরা-র আভা, কাচের স্তর — শান্ত, দামি, এক টানা।
- **স্পর্শ:** প্রতিটা ছোঁয়ায় একটা উত্তর — scale, রঙ, কাঁপুনি বা haptic; কখনো নীরবতা নয়।
- **ছন্দ:** app গানের ছন্দে বাস করে — বাজলে জীবন্ত, থামলে শান্ত।
- **চেনা মুহূর্ত:** waveform-এ গানের আকৃতি · ঘুমের আগে ঘর অন্ধকার · গান বদলে ঘরের রঙ বদল — এই তিনটাই মানুষ বর্ণনা করবে।

**৫ মিনিট পরে user মনে রাখবে:** "সেই waveform-টা — গানটা দেখা যাচ্ছিল!"
**১ সপ্তাহ পরে:** "টাইমার সেট করলে app-টা নিজেই অন্ধকার হয়ে যায়, ভাই।"
**বন্ধুকে:** "শুধু player না — এরা পুরো অভিজ্ঞতাটা নতুন করে ভেবেছে।"

---

## ২৬. WHY RIKO?

Feature-এর জন্য না — Poweramp বেশি জানে। Catalog-এর জন্য না — Spotify বিশাল। শুধু সুন্দর UI-এর জন্যও না — সৌন্দর্য কপি হয়।

**RIKO ভালোবাসার কারণ:** RIKO ব্যবহার করলে মনে হয় — *এই product-টা আমার music শোনার experience-টাকে গুরুত্ব দিয়ে design করা হয়েছে।* গানটা শুধু বাজে না — **দেখা যায়** (timeline), ঘর রাঙায় (aurora), রাতের শেষে ঘরটা নরম হয়ে ফুরিয়ে যায় (wind-down)। প্রতিটা ছোট মুহূর্তে এমন মাথা খাটানো — যা user ব্যাখ্যা করতে পারে না, শুধু অনুভব করে।

সেটাই RIKO-র পরের প্রজন্ম।

---
*END OF DOCUMENT — RIKO-EXPERIENCE-MASTER-001 v1.0*
*পাশাপাশি পড়ো: SM-UI-SPEC-001 v2 (চেহারার সত্য) · Premium Smoothness & Performance Pass (গতির সত্য)*
