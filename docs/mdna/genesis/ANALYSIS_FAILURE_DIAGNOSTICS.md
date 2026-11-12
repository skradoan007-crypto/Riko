# P8 device follow-up: offline analysis unavailable

The supplied device screenshot for MONTAGEM SETHRON (Ultra Slowed) shows
“Analysis unavailable · live organism only · Generation 0”. It proves the UI
received no completed genome; it does not reveal the decoder exception, verify
the installed revision, or establish live audio synchronization.

## Confirmed defects and changes

- OfflineDnaAnalyzer previously retained only an exception class name, and
  DnaRepository discarded even that by returning null. New DnaAnalysisFailure
  codes preserve specific known timestamp, incomplete-decode, format, access,
  empty-audio, timeout and limit failures. Only predefined text is displayed;
  raw exception messages, song paths and provider URIs stay private.
- The screen requested genome and then timeline separately. After a failed
  genome attempt, timeline() initiated the same expensive analysis again.
  The screen now calls load() once and receives genome + timeline or a typed
  failure. Cancellation still propagates and incomplete results are not saved.
- Existing timeline bytes are validated before reuse. Missing or corrupt
  timeline caches are rebuilt while the existing DNA profile and listening
  history remain intact. AtomicFile also handles interrupted cache writes.
- The screen displays a diagnostic code, an explanatory dialog and an explicit
  Retry analysis button. Controls have 48dp minimum targets and share available
  width; status changes have polite accessibility announcements.
- Existing background/export nullable APIs remain available. Live playback and
  renderer behavior are unchanged by this follow-up. No schema migration.

## Validation actually performed

- tools/test_genesis_analyzer.sh: 14 JVM contract groups PASS, including original
  V1 feature/fingerprint/timeline goldens and new safe diagnostic classification.
- tools/test_genesis_final.sh: camera, DNA code/timeline, MDNA spectrum contracts PASS.
- Workflow YAML parsing and bash syntax PASS; git diff whitespace check PASS.
- Added DnaRepositoryTest (Robolectric/Room): one decode per failed attempt,
  explicit retry recovery, cache reuse/repair without profile replacement, and
  cancellation without persistence. Added to existing release-unit-test CI command.
  These Kotlin/Android tests were NOT executed locally; no Android build or CI
  was run for this revision. No new device performance measurements or screenshots.

## Unresolved device issue

The exact cause for the user's song is still unknown. Decoder timestamp
validation, completion validation, resampling and feature generation were not
relaxed speculatively. This patch fixes diagnostics and duplicate work; it does
not claim that the failing song now analyzes successfully. Reproduction requires
the original failing audio file or a diagnostic code from a build of this source.
If the cause is device-codec-specific, device evidence is still needed.
