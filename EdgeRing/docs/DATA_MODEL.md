# Data model (M02)

Package `com.example.edgering.data`: `model/` (pure Kotlin), `db/` (Room), `repo/` (repositories,
DataStore), `backup/` (JSON export/import). Get everything through `AppData.get(context)`.

## Room database `edgering.db`, version 1 (schema exported to `app/schemas/`)

| Table | Columns | Rules |
|---|---|---|
| `zones` | id, name, colorArgb, position | Delete cascades to items; triggers targeting it get `targetZoneId = NULL`. |
| `triggers` | id, name, position (LEFT/RIGHT/TOP/BOTTOM), lengthFraction, thicknessDp, offsetFraction, targetZoneId?, enabled, isDefault | Exactly one `isDefault` row; `deleteIfNotDefault` guards it in SQL. Geometry limits in `TriggerRules`. |
| `items` | id, zoneId, parentItemId?, position, type, label, target?, customIcon?, options? | `parentItemId` null = top level, else a FOLDER item (same zone). Folder delete cascades to children. |
| `app_cache` | packageName + className (PK), label | Filled by the app index (M03). Never backed up. |

`items.target` by type: APP `package/className`; INTENT_SHORTCUT intent URI; ACTION action id;
URL the link; FOLDER null; FS_FOLDER folder URI/path. `options` is opaque JSON for per-type
settings (file-folder sort/hidden/depth, section 2.16). Enums are stored by name: never rename.

Migrations: add to `EdgeRingDatabase.MIGRATIONS` and bump `VERSION`. `MigrationChainTest` fails if a
version step has no migration. There is no destructive fallback.

## Settings (Preferences DataStore file `settings`)

All values are strings; bad or missing values fall back to the default per field (`AppSettings.fromMap`).

| Key | Type / values | Default |
|---|---|---|
| `grid_columns` | int 1..8 | 4 |
| `grid_direction` | TOP_TO_BOTTOM, BOTTOM_TO_TOP | TOP_TO_BOTTOM |
| `grid_position` | TOP, CENTER, BOTTOM | CENTER |
| `icon_size` | SMALL, MEDIUM, LARGE | MEDIUM |
| `label_position` | ABOVE, BELOW, HIDDEN, AUTO_HIDE_ABOVE, AUTO_HIDE_BELOW | BELOW |
| `hide_empty_cells` | bool | false |
| `launch_mode` | HYBRID, SINGLE_TOUCH, TOUCH_THEN_TAP | HYBRID |
| `service_enabled` | bool (master switch; **excluded from backup**) | false |
| `disable_in_landscape` | bool | false |
| `hide_service_notification` | bool | false |
| `hide_az_index` | bool | false |
| `left_handed_layout` | bool | false |

Defaults marked UNKNOWN in the reference analysis (grid direction/position) are my choices; revisit
after device testing. Haptics, theming and trigger-indicator keys are added by M08/M10/M06.

## Backup file, format `edgering-backup`, version 1

JSON: `format`, `schemaVersion`, `zones[]`, `triggers[]`, `items[]` (flat, `parentItemId` links),
`settings{}`. Import order: decode -> `BackupValidator` (everything, no writes) -> ONE Room
transaction (replace zones/triggers/items) -> settings. Any rejection or storage error leaves
existing data unchanged. Not in v1: custom icons, app cache, `service_enabled`.
Limits are in `model/Limits.kt` (200 zones, 50 triggers, 20 000 items, 8 M characters).
