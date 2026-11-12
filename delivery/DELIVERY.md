# RIKO MDNA Genesis P8 source delivery

Base P4: f75a7e4854e435da95f05cc649b68fa6e3b69f8f
Final P8: e114429e9639325435efb21723f6ec375a2211ad

P5: e6bb2e460146f50cc4815417a9f3e55d1c5b5cad
P6: 8fbd8df070dd666df7f4ab1a21b58632aae2382e
P7: d2d3bc491ebda2afbc666bf71bd28a36a21f8ce4
P8: e114429e9639325435efb21723f6ec375a2211ad

This ZIP contains the complete source tree plus four ordered git-format patches.
Apply patches only to the matching fixed P4 baseline, or use the full source tree.
Do not apply patches on top of this already-updated full source tree.

No Android build/CI was run for P5–P8, per user request. P4 green is user-reported.
Host Java/Python checks passed; see docs/mdna/genesis/P8_REPORT.md and FINAL_HANDOFF.md.
Android compilation, migration execution, codecs, lifecycle and device performance
remain the final acceptance gate. No APK is bundled and no remote push was performed.
The archive excludes .git and local caches. Existing app identity is preserved.
