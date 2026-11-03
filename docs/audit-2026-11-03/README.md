# IBD3D Panel accuracy, images and motion audit

Reviewed **2026-11-03**, starting from repository commit `6a8bface0e919e1f45cc321a540c8f404bdd07a6`. This is a source/data update. It is not a signed release or a claim that every cheat works in-game.

## Coverage and evidence

All **212 original records** have one individually traceable row in [ALL_ENTRIES.md](ALL_ENTRIES.md), [all-212-entries.csv](all-212-entries.csv) and [entries.json](entries.json). Each row includes the exact original ID/code, original and updated name/category, sources, evidence limitations, image observation and action. [original-212.json](original-212.json) is the immutable baseline. Import CSV code columns as **text**; JSON/Markdown are canonical for leading zeros.

| Evidence verdict | Entries | Meaning |
|---|---:|---|
| Confirmed in publisher text | 92 | Code/name present in official target-package listing; secondary cross-check performed. Not device-tested. |
| Official but ambiguous | 4 | Publisher itself lists two outcomes for each of 6666 and 12345. |
| Community reported | 7 | Community corroboration without publisher confirmation. |
| Conflicting | 13 | Community/historical claims disagree with current publisher wording or exact digits. |
| Unverified | 14 | Eight video-description claims and six word rumors. |
| Wrong game | 5 | Pairs belong to Indian Bikes Riding 3D, package com.OneTouch.GTAINDIA. |
| Experimental | 29 | Constructed from the publisher's general monster-truck prefix rule; individual results not tested. |
| RGS community commands | 48 | Cross-checked command tables; not phone-dialer codes, not publisher-confirmed. |
| **Total** | **212** | **No original ID or code string deleted/changed.** |

The app browses **207** records. Five wrong-game records remain addressable in existing favorites/recent copies with a Different game badge and explanation, but are excluded from search/categories/Home code count. No new confirmed entries were found; see [NEW_CODE_RESEARCH.md](NEW_CODE_RESEARCH.md). No unverified claim is silently upgraded to official.

## Corrections applied

- `875` Tata Safari, `309` Vitara Brezza, `247` Banana Car, `736` Crocodile Car and `914` Dog Car: quarantined as different-game claims. Their exact pairs are in the other game's publisher listing; several community guides mixed the games together.
- `6666` G-Wagon/Range Rover and `12345` Heicarrier/More NPCs: both pairs retained, visibly marked official ambiguity. No unsupported choice of a winning result.
- `300` renamed Tarzen / Tarzan Car and moved from Animal to Car/Vehicles. The supplied purple-car image and community car tables support this correction. Publisher spelling remains recognizable.
- `3380` RobotAi moved to Other and a robot/tool category.
- `8333` generalized to BMW (model disputed), because sources disagree M3/M4.
- Alternate Dragon `7222`, Elephant `6999`, truncated scooter `0` and R15 `15`: explicitly conflicting; publisher alternatives stated in details.
- Six word rumors now have **unknown** input method, instead of misleading numeric-dialer instructions.
- 22 generic RGS props now have documented slot labels and exact command in the title; NPC-dance scope generalized to Dance (npdance), because sources do not establish all-NPC behavior.
- Every entry includes offline evidence date, sources, verification and clear notes. Existing keys, history limit, copy behavior and DataStore settings remain intact.

## Complete image audit

All 103 originally mapped thumbnails were visually inspected in contact sheets, and all 19 unassigned files were inspected separately. All 122 original image files decoded successfully. Visual observations for every asset are in [asset-inventory.csv](asset-inventory.csv).

- 82 mapped images are visually consistent with their subjects, without proof of execution/current model version.
- 13 uncertain mappings now show clean placeholders: original rows 34, 48, 49, 59, 87, 90, 97, 101, 104, 105, 106, 109, 110. This includes ambiguous official outcomes, a questionable Porsche/McLaren-like car, a mast labelled Gas Tank, and seven no-code community image associations.
- 109 originals already lacked a thumbnail. With the 13 quarantines, **122** records have placeholders; **117** are browseable and 5 are retired.
- 8 retained effect images are explicitly illustrative. A still cannot prove infinite health, slow motion, jump strength or traffic changes.
- One unused byte-identical health image was removed: **1,678,082 bytes** of bundled source assets saved. A tank near-duplicate is reported but not mapped to another item. No current image path is reused across entries.

[IMAGE_REQUESTS.md](IMAGE_REQUESTS.md) and [image-requests.csv](image-requests.csv) enumerate all **130** missing/uncertain/illustrative rows with exact names, string codes and requested evidence. Five rows explicitly need no replacement because they belong to another game. No generated or unrelated images were introduced.

## UI and performance implementation

The existing light palette, card shapes, typography, hero art, icon identity and four bottom tabs are retained. Home gets a secondary **Get the Game** button beside Cheat Codes at normal phone widths; it wraps on narrow/large-text layouts. It opens the official package's store details, never a game launch intent. Missing/restricted Play handler falls back to the HTTPS listing; if neither handler exists, the app shows feedback.

Navigation fades use 180 ms enter / 120 ms exit. Lazy-list insertion, removal and placement use short 100–180 ms transitions with stable IDs. Copy icon crossfades, favorite color transitions and accent changes remain interruptible. Existing Material chips, focus, switches, ripples and sheet motion remain native. Compose motion uses the platform duration scale; no infinite decorative animations, blur, timer-driven recomposition or blocking input.

Catalog parsing and ID-index creation stay on IO and are cached. Favorite/recent lists are memoized; details and history use a shared ID index instead of repeated linear scans. Home hero now decodes asynchronously through Coil. Thumbnail URIs are remembered and AsyncImage continues constraint-sized decoding/cache use. Lazy content types permit reuse; filters/search reuse the list with stable IDs. Search is local over 207 browseable records, with no network or debounce delay. No new services or SDKs were added; Compose animation is an explicit dependency.

These are code-level improvements, **not measured FPS/startup claims**. The only measured size saving is the duplicate source file; compressed APK/AAB savings have not been measured.

## Actual verification

- **PASS:** 10 offline Python dataset regression tests; see [data-test-results.txt](data-test-results.txt).
- **PASS:** catalog integrity script, exact strings/IDs, duplicate-code preservation, all mappings and current image paths; [../catalog-audit.json](../catalog-audit.json).
- **PASS:** all 122 original images decoded; hashes, dimensions and per-asset observations recorded. 121 remain packaged.
- **PASS:** 25 Kotlin/Kotlin-DSL files parsed/formatted by ktfmt; five XML files parsed. Formatter success is not compilation/type checking.
- **NOT RUN:** current 26 supplied JUnit/model/Compose/repository/Play tests (includes four new store-flow tests and three model regressions). The existing Home test also exercises Get the Game.
- **NOT RUN:** Gradle compile, lint, APK/AAB, emulator/device, screenshot comparison, TalkBack/large-font/landscape, animator-scale-zero, frame timing and real Play update eligibility. The user's no-build instruction remains respected; no CI build was triggered.

## Assistance still needed

1. Run the manual build/test commands in the root README or GitHub workflow, then device-check the matrix in [DEVICE_CHECKS.md](DEVICE_CHECKS.md). Release signing must use the existing app's upload key.
2. Supply version-stamped gameplay evidence for unresolved code claims, especially duplicate official outcomes and candidates in NEW_CODE_RESEARCH.md. Website agreement alone is insufficient.
3. Use IMAGE_REQUESTS.md to gather genuine screenshots. Precise object identity for generic RGS props cannot be inferred from their slot number.

No payment, subscription, Firebase, ads, custom server or live content download was added. Plugins remain Coming Soon. New content still ships through ordinary app updates.
