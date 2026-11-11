# Genesis P1 — deterministic DNA contract

## Scope and architecture

P1 supplies an immutable, versioned song identity core. It does not analyze songs,
persist Room entities, render new archetypes, or add a screen. Those remain P2–P8.
The user's explicit P0 approval/next-phase request is the entry gate for this phase.

The existing Kotlin/Compose application consumes the core through ordinary Java
interop (`DnaGenomeMapper.generate(...)`, getter properties in Kotlin). These seven
small Java classes intentionally have no Android, UI, coroutine, serialization
plugin, or random-library dependencies. This permits the **actual production
sources** to be compiled and tested on a plain JVM, including the current workspace
without an Android SDK or downloadable Gradle distribution. App Java/Kotlin target
17 is retained; the APIs used by the core are available on Android minSdk 26.

| Type | Responsibility |
| --- | --- |
| DnaFeatures | Immutable canonical musical summary, strict input validation |
| DnaAlgorithmVersion | Explicit persistent algorithm IDs; V1 = 1 |
| DnaSeedGenerator | SHA-256 of content identity, canonical features and version |
| SplitMix64 | Local reproducible pseudo-random stream |
| DnaGenomeMapper | Frozen integer mapping from features and seed to visual parameters |
| DnaGenome | Immutable archetype, geometry budget, musical coefficients and palette weights |
| DnaCodec | Bounded binary feature/genome persistence, wire version and CRC32 |

No global PRNG, wall clock, locale, Android ID, path, title, album metadata, playback
position, display quality, camera state, or listening generation enters this core.
Construction/serialization allocate once per profile operation; this API is not
intended to run every animation frame. Existing playback/rendering paths are unchanged.

## Canonical feature contract (V1)

P2 must provide a frozen song summary computed using a documented, versioned analyzer.
Do not pass P0 demo values or live spectrum frames as analyzed song DNA.

| Field | Unit and accepted domain | Meaning |
| --- | --- | --- |
| bassEnergyPpm | integer 0–1,000,000 | normalized bass energy descriptor |
| midEnergyPpm | integer 0–1,000,000 | normalized mid energy descriptor |
| trebleEnergyPpm | integer 0–1,000,000 | normalized treble energy descriptor |
| centroidMilliHz | integer 0–11,025,000 | centroid at the future 22,050 Hz analysis rate |
| spectralFluxPpm | integer 0–1,000,000 | normalized positive spectral change |
| tempoMilliBpm | integer 0–300,000 | estimated tempo; zero means unknown |
| tempoConfidencePpm | integer 0–1,000,000 | tempo confidence; must be zero for unknown tempo |
| dynamicRangeMilliDb | integer 0–120,000 | nonnegative dynamic range summary |
| chromaPpm[12] | each 0–1,000,000 | C, C#, D, D#, E, F, F#, G, G#, A, A#, B |

Chroma sums to exactly 1,000,000, or all zeros for unknown/silent chroma. Bands are
independent normalized descriptors, not forced to sum to one. P1 deliberately does
not claim an implemented extraction/calibration rule; P2 must document it and freeze
its version before storing real-song profiles. Changing the analyzed values changes
the seed. Analyzer/version changes cannot silently overwrite a stored base identity.

`fromCanonical` is the preferred persistence/analyzer boundary. `fromNormalized`
accepts finite doubles in the corresponding physical units, rounds half-up once,
and rejects out-of-range input instead of clamping. Chroma must already be L1
normalized within 1e-6 of one (or all zero); largest-remainder allocation produces
exact integer mass with ties resolved toward the lower pitch class. Mutable arrays
are copied on both input and output. Mapper calculations after this boundary use
only fixed-width integer arithmetic. Numerical differences crossing a quantization
boundary are different features, not a promise of cross-decoder acoustic equivalence.

## Identity and SplitMix64

The caller supplies exactly 64 ASCII hexadecimal characters representing a stable
audio-content SHA-256 fingerprint. Hex casing is equivalent; whitespace and path or
metadata stand-ins are rejected. P2 owns creation/caching of that fingerprint. P1
does not claim to match different encodings of the same recording or to have already
implemented a content-fingerprint extraction method. The original fingerprint is
not included in the genome or blobs; no device identifiers or local paths are stored.

Seed preimage, concatenated without platform encoding or implicit separators:

1. ASCII `RIKO:MDNA:GENESIS:SEED` followed by byte 0.
2. Algorithm ID as unsigned 16-bit big-endian (V1: `00 01`).
3. The 32 decoded fingerprint bytes.
4. 80 feature payload bytes: eight scalar fields in table order, then 12 chroma
   bins, all signed 32-bit big-endian, validated to the nonnegative domains above.

Take the first eight SHA-256 digest bytes as the bit pattern of a big-endian signed
64-bit seed. `map(features, storedSeed, version)` reconstructs an existing profile;
`generate(fingerprint, features, version)` derives the seed then maps it. A caller
must keep the persisted features with that seed. A 64-bit seed is not a collision-free
song key; a future database must retain its independent stable `songKey`.

SplitMix64 follows Sebastiano Vigna's 2015 public-domain reference:
<https://prng.di.unimi.it/splitmix64.c>. Modulo-2^64 additions/multiplications and
logical right shifts are explicit. `nextInt(bound)` uses the upper 63 random bits
with rejection to avoid modulo bias. Instances are private to each generation call;
they are not thread-safe shared services and are not cryptographic generators.

## Frozen V1 mapping

The mapper consumes randomness in a fixed documented source order: geometry seed,
weighted archetype selection, particle count, layer count, expansion, brightness,
sharpness, burst, deformation, flow, microdetail, palette rotation. Chroma symmetry
and rhythm are deterministic feature-only decisions. Do not insert new random draws
into V1 when adding a future parameter; add a new algorithm implementation instead.

| Musical input | Visual mapping |
| --- | --- |
| Bass | More points and larger expansion coefficient; increased orb weight |
| Mid | More layers and faster surface flow; increased terrain weight |
| Treble | More points and microdetail; increased nebula weight |
| Centroid | Increased brightness and surface sharpness |
| Flux | Stronger transient coefficient and increased nebula weight |
| BPM/confidence | Exact milli-BPM when confidence >= 350,000; otherwise no rhythmic grid |
| Chroma | 12 colour weights plus dominant-pitch/concentration-based symmetry |
| Dynamic range | Stronger deformation coefficient |
| Seed | Fine geometry, bounded structural variation, palette rotation |

Colour interpretation for P4: pitch class `i` has hue `(paletteRotationMilliDegrees
+ i * 30000) mod 360000` and weight `chromaAt(i)`. Unknown chroma is explicitly all
zero; a renderer may use its neutral brand palette without modifying the genome.
Symmetry is 2–11 under the current mapper (the model accepts up to 12). Point budgets
remain 4,096–32,768 and layers 2–5. A quality tier may display a subset of the
geometry without changing the base genome. These are parameter contracts, **not
evidence that P4's terrain/nebula renderers already exist**.

Changing features also changes the derived seed, so comparing independently generated
songs is not a strict one-parameter monotonic experiment. Tests hold the seed fixed
when validating the individual musical mappings. All 1,024 tested identities with
identical features produced distinct tested visible parameter tuples and covered
all three archetypes; that is a bounded test, not a mathematical uniqueness guarantee.

## Binary persistence, separate from P7 sharing

Big-endian, exact sizes, no strings or unbounded length fields:

| Blob | Layout | Bytes |
| --- | --- | --- |
| Features | `MDFE`, u16 wire version 1, 20 i32 feature values, u32 CRC32 | 90 |
| Genome | `MDGN`, u16 wire version 1, u16 algorithm ID, i64 seed, i64 geometry seed, u8 archetype, 24 i32 values, u32 CRC32 | 125 |

Genome's 24 ints: particle count, layers, symmetry, expansion, brightness, sharpness,
burst, rhythm, deformation, flow, microdetail, palette rotation, then 12 chroma bins.
Archetype IDs are explicit: orb 1, terrain 2, nebula 3. CRC covers every preceding
byte. Length is checked before copying; checksum, magic, versions and every field
are validated. Truncated, oversized, trailing, malformed and unknown-version data
fail with `IllegalArgumentException`; null programming arguments fail fast.

CRC detects accidental corruption, not forgery. This codec is internal persistence,
not authentication and not the compact portable DNA Code/import UI planned for P7.
Wire version and algorithm version are distinct. V1 feature payload ordering remains
part of V1 seed generation even if a future wire envelope changes. Preserve old
decoders/mappers and golden fixtures when introducing later versions. No fallback
to the newest algorithm is allowed for unknown IDs.

## Reproducible validation

Run `bash tools/test_genesis_dna.sh` with JDK 17+ and Python 3. It compiles the actual
production Java sources with warnings treated as errors, runs 17 assertion groups,
then compares all three checked-in vectors with an independent Python oracle.
No SDK, emulator, external packages, fixture rewriting or network access is required.

The same assertion bodies have 17 JUnit 4 wrappers in `DnaCoreTest`, run by:

```
bash gradlew :app:testReleaseUnitTest --tests 'com.rikodev.music.android.dna.genome.DnaCoreTest'
```

Fixtures in `app/src/test/resources/dna/genesis-v1.properties` include silence,
A-minor pulse and bright noise, with full feature bytes, seed and full genome bytes.
`tools/genesis_dna_reference.py --check` verifies them independently; `--write`
refuses to overwrite an existing fixture. The three literal reference SplitMix64
seed-zero values are checked separately. Golden equality, randomized valid-value
round trips, individual musical influences, corruption at every byte, versions,
malformed but checksummed payloads, array ownership, concurrent generation and
locale/timezone independence are all exercised.
