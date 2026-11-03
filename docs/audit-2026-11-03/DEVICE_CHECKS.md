# Device verification still to run

These are pending checks, not claimed passes. Use the same signing identity as the installed app to preserve data across update.

| Area | Scenario | Expected result |
|---|---|---|
| Existing data | Upgrade with favorites, recents, Blue/Rose and feedback off | IDs/history/preferences preserved; retired entries clearly labelled in saved collections |
| Offline | Airplane mode; cold start; all categories and detail sheets | Bundled data and placeholders load; no network needed |
| Exact strings | Copy 0000, 0015, 01212, 0701, 1#, b1 | Clipboard matches byte-for-byte; recent record created only on successful copy |
| Duplicates | Search 6666 or 12345; favorite both labels | Both IDs remain independent; ambiguity text visible |
| Quarantine | Search 875, 309, 247, 736, 914 | Wrong-game records excluded from normal results; old saved rows still readable |
| Search/filter | Rapid typing; no-result query; clear; switch chips; return from tabs | Correct rows, no stale-position jump, taps remain responsive |
| Copy/favorite | Rapid repeated copy; toggle favorite during filtering | Correct final state; newest copy feedback timer restarts; no identity mismatch |
| Store | Game installed and uninstalled, Play present | Official store details opens both times; game never launches |
| Fallback | Disable Play; browser present; then no handler | HTTPS listing opens, or clear snackbar if no handler |
| Motion | Animator scale 0 and 1; rapid tab/chip changes | Correct final state without relying on animation completion |
| Layout | 320/360dp portrait, landscape, tablet, font scale 1.0/2.0 | Buttons wrap as needed; no cropped text/actions; IME and bars don't cover controls |
| Accessibility | TalkBack; keyboard navigation | Named controls, copy state, logical focus, dismissible details |
| Performance | Release/preview on modest physical device, cold launch and scroll | Measure frames/startup; no claim of 60fps until measured |
| Play update | Install through internal Play track with eligible newer version | Flexible download, restart prompt, cancellation/resume work |
