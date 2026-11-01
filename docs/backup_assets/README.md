# Backup: preview_contact_sheet.png
- Original: `app/src/main/assets/sanctuary_pack/preview_contact_sheet.png` (3 662 117 bytes, 1088x1088, valid PNG, SHA256 backed up)
- Reason: Not referenced by `sanctuary_assets_manifest.json` or any runtime code (`grep -r preview_contact` = 0 hits); caused APK bloat (+3.5 MB) and mimicked prior "tệp bị hỏng" bloat.
- Action: Kept in repo for reference, excluded from APK via `android.packaging.resources.excludes` in `app/build.gradle.kts` (minimal, non-destructive; no visual quality loss, Sanctuary layout preserved).
- Recovery: Remove the `packaging.resources.excludes` entry to re-include, or restore from this backup.
