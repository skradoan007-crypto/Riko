# Genesis P1 — deterministic genome milestone

## Result

P1 implements the deterministic core only: immutable musical features and base
genome, stable seed derivation, SplitMix64, explicit algorithm dispatch, musical
parameter mapping and bounded versioned binary serialization. It is ready to be
consumed by P2's shared offline analyzer. No P2 decoding, Room migration, evolution,
export, new preset renderer or navigation integration has been started.

P0 was explicitly approved by the user before P1 began. The current remote `new`
branch was verified at `f8143484b92550431d5b3171e3de0b3a088d9aff` (`MDNA ch P0`).
Its complete source tree was matched locally, including the user's renamed release
workflow, gradlew mode and delivery files. P1 is based on **that exact commit**;
the earlier local P0 milestone is preserved separately. No remote history was rewritten.

The user's P0 release build was independently confirmed successful:
<https://github.com/skradoan007-crypto/Riko/actions/runs/42852362967>.
That run is P0 evidence, not P1 build evidence. Its workflow builds the release
variant; it did not run the newly added P1 tests or prove the debug Genesis Lab UI.

## Files and architecture

See `P1_FILES.txt` for the complete changed-file list and `P1_CORE.md` for the frozen
input, PRNG, mapping and binary wire contracts.

- Seven production classes under `dna/genome`, independent of Android/UI/Media3.
- Shared JVM contract suite plus 17 JUnit wrappers and three literal golden fixtures.
- Dependency-free JVM runner and independent Python oracle.
- Existing release workflow extended with the plain-JVM gate, targeted Gradle
  `testReleaseUnitTest` gate and downloadable reports; secret scanning, debug-key
  signing, APK artifact naming and direct prerelease asset mechanism retained.
- Corrected P0 audit: `SpectrumStyle.MDNA` does not exist; `MdnaVisualizerHost`
  is not wired into the old visualizer. Genesis remains a debug-only separate lab.
  P1 does not pretend to fix P8 navigation integration.

The core uses interoperable Java within the existing Kotlin project so actual
production code can be tested on a plain JVM without downloads. No dependency,
application identity, Room schema, user data, frozen library UI, audio processing,
render loop or package configuration is changed.

## Executed validation

Environment: Linux x86_64, OpenJDK 21.0.13, Java source/target 17, Python 3.

| Check | Result |
| --- | --- |
| Compile all seven production core classes and shared test bodies with warnings as errors | PASS |
| JVM contract groups | 17 PASS; full log in `evidence/genesis-p1-jvm.txt` |
| Independent Python oracle | PASS; all three full feature/seed/genome golden vectors match |
| Round-trip corpus | PASS; 1,000 feature/genome cases |
| Song diversity sample | PASS; 1,024 distinct visible parameter tuples, all three archetypes |
| Concurrent deterministic generation | PASS; four workers, 256 regenerations each |
| Malformed/checksummed input, byte corruption, every truncation, oversize and trailing data | PASS within contract suite |
| Workflow YAML parsing and retained credential/release gates | PASS |
| Git whitespace check and credential scan | PASS at milestone preparation |
| Android Gradle/JUnit execution | BLOCKED before Gradle launch: wrapper distribution download reports `Network is unreachable` |
| Android APK assembly / release verification for P1 | UNVERIFIED; no SDK/distribution available locally; no P1 remote CI run |

The JVM suite runs the actual source and assertions, **not a mock, transcription,
or claim that Android Gradle ran**. The independent Python implementation is an
additional oracle. The JUnit wrapper is present for CI but has not executed locally.
Initial local compiler probes encountered missing `javac` launcher / stripped
`--release` metadata; the checked-in runner uses the available JDK compiler module
and the repository's source/target 17. All final core checks above passed afterward.

## Known limits and next gate

- Musical extraction and content-fingerprint generation are inputs, not implemented
  capabilities in P1. P2 must freeze normalization and analyzer identity before
  creating persisted real-song profiles. Different decoded/quantized features can
  legitimately yield different seeds; cross-codec acoustic matching is not claimed.
- There is only one supported algorithm today. Old V1 fixtures are frozen; unknown
  versions are rejected rather than upgraded. Future algorithms require separate
  implementations and retained compatibility tests.
- CRC32 protects against accidental corruption, not hostile forgery. Internal
  blobs are not the P7 portable DNA Code format.
- No new visual screenshot is appropriate for this data-only phase. Existing P0
  shader evidence remains in `evidence/`; it is not a P1 device test.
- No device GPU/audio latency, frame pacing, battery, thermal or memory benchmark
  was run for P1. No device performance numbers are asserted.
- No remote push was performed. GitHub write access was previously denied (HTTP
  403); this phase supplies a local milestone and a reviewable source/commit patch
  without repeating a permission-denied mutation.

P2 is technically ready to begin from this core after the user's next-phase
instruction. P1 APK/Gradle verification remains a pending CI gate. Stop at P1;
do not begin the offline analyzer as part of this milestone.

Final commit message: `mdna(genesis-p1): deterministic genome`.
The exact commit hash is in the delivery record and Git history (not embedded into
this file, which is itself part of the commit).
