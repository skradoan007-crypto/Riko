# Shipping a content update

Edit core/data/src/main/assets/catalog.json and presentation_mapping.json together. Preserve existing IDs so saved favorites/history remain associated with the same item. Assign a new stable ID to a new entry even if its code matches another item.

Always quote codes as JSON strings. Never parse them as numbers or normalize zeros/special characters. Retain source category, status, inputMethod, notes and imageStatus. Update count and catalogVersion. schemaVersion remains 1 unless you intentionally change the parser.

Images live under core/data/src/main/assets/images. Each image path must match exact filename/case. An absent image is valid and displays a placeholder; do not manufacture a misleading image association. The UI mapping gives each ID a chip (Bike/Car/Animal/Other) and vehiclesShortcut Boolean.

Run scripts/audit_catalog.py and the included model/app tests locally, then increase app versionCode/versionName, sign a new release AAB and distribute through Play. No content is remotely downloaded.
