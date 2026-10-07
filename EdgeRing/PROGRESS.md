# PROGRESS

## Status
- Zip number / milestone: 3 - M02 (data layer), CODE COMPLETE, NOT YET BUILT BY CI
- Date/time of this checkpoint: 2026-10-08
- Build status: NOT RUN. Gradle/Maven are unreachable from the sandbox. M02 adds KSP, Room, DataStore, kotlinx.serialization and Robolectric, so a CI build is mandatory before M03. Last known good build: M01 (CI + device)
- Tests: 64/64 pass here (20 M01 + 44 new pure tests, kotlinc 2.0.21 + JUnit shim, not Gradle). 44 more tests are written for CI and have NEVER been run (list below). 1 instrumented test (MigrationTest). Lint: not run
- Known issues: none known. Highest-risk unverified items are listed under "Unverified assumptions"
- CI status: M00 PASS, M01 PASS, M02 NOT RUN. The repo's WebPaper.yml only runs `assembleDebug`; the zip's `.github/workflows/build.yml` also runs `lint test` and uploads `app/schemas`. Use build.yml (or copy its steps) so the new tests actually run
- Verified here: all library-free code (model, entities, backup DTOs/validator/mapper) compiles with 0 warnings; Reorder, TriggerRules, AppSettings, BackupValidator, BackupMapper tests pass; DAOs, repositories, BackupRepository and the database class type-check against hand-written API stubs (weaker than a real build)
- Verified by CI / device: M00 and M01 only (nothing from M02)
- Not verified: Gradle build with the new plugins/versions; KSP/Room annotation processing (query validity, schema export); DataStore wiring; kotlinx.serialization codec; ALL 44 CI-only tests; lint; MigrationTest; M01 latency number still unrecorded
- Unverified assumptions: (1) `com.google.devtools.ksp` 2.0.21-1.0.28 exists (release tag confirmed) and works with AGP 8.7.3; (2) Room 2.6.1 enables foreign keys by default (EdgeRingDatabaseTest.foreignKeysAreEnforced will show it); (3) Room accepts `parentItemId IS :parentItemId` with a null bind; (4) Robolectric 4.14.1 + `@Config(sdk=[34])` runs Room and DataStore under `./gradlew test`; (5) every `Json.decodeFromString` failure on bad input is a `SerializationException`/`IllegalArgumentException` (BackupCodecTest checks six shapes); (6) `sourceSets.getByName("androidTest").assets.srcDir(...)` and `MigrationTestHelper(instrumentation, Class)` find `app/schemas`

## Done (checked off, with file paths)
- [x] M00 foundation, flavours, CI, empty shell - see CHANGELOG
- [x] M01 overlay + trigger spike (device-verified) - `overlay/`, `service/`, `ui/settings/`
- [x] Pure model: enums, AppSettings + keys, TriggerRules, Reorder, Limits - `data/model/` (tested here)
- [x] Backup DTOs, validator, mapper (tested here) - `data/backup/BackupFile.kt`, `BackupValidator.kt`, `BackupMapper.kt`
- [x] Written, type-checked against stubs only (see Status): Room entities/DAOs/database - `data/db/`; repositories - `data/repo/Repositories.kt`; `SettingsRepository.kt`, `AppData.kt`; `BackupCodec.kt`, `BackupRepository.kt`
- [x] Docs: `docs/DATA_MODEL.md` (tables, settings keys, backup format)

## In progress / partially done
- [ ] M02 exit criteria need CI: (a) green `assembleDebug lint test`; (b) commit generated `app/schemas/com.example.edgering.data.db.EdgeRingDatabase/1.json` (from the `reports` artifact), then run `connectedAndroidTest` once for MigrationTest
- [ ] CI-only tests, never run: `EdgeRingDatabaseTest` (10), `RepositoriesTest` (13), `SettingsRepositoryTest` (5), `BackupCodecTest` (7), `BackupRepositoryTest` (8), `MigrationChainTest` (1)
- [ ] Data layer is not consumed yet: nothing calls `AppData` or reads `AppSettings`. M01 still uses in-memory `EdgeState` and `TriggerSide` (replaced in M06). Wire `serviceEnabled`/side to the stores in M06/M08
- [ ] `overlay/SpikeAppSource.kt` is a STUB (first 12 launcher apps A-Z); replaced by the cached index in M03
- [ ] M01 loose end: record the latency value shown in the app

## Next steps (ordered, each small enough for one session)
1. M02-fix: run CI with build.yml, paste the log; fix whatever the new plugins/tests surface; commit schema `1.json`. Do this BEFORE any new code.
2. M03 app index (LauncherApps/PackageManager into `app_cache` via `AppCacheRepository`, diffing, locale-aware `Collator` sort, A-Z strip, `LauncherApps.Callback` refresh); replaces `SpikeAppSource`. Store the index change marker in DataStore (new key, document it).
3. M04 zones and grid.

## Decisions and conventions
- App name EdgeRing; package `com.example.edgering` (placeholder; sideload flavour adds `.sideload`)
- minSdk 26, target/compile SDK 35, JDK 17, Kotlin 2.0.21, AGP 8.7.3, Gradle 8.10.2, Compose BOM 2024.10.01, coroutines 1.9.0
- M02 libs: KSP 2.0.21-1.0.28, Room 2.6.1 (KSP, no destructive fallback), DataStore Preferences 1.1.1, kotlinx-serialization-json 1.7.3, Robolectric 4.14.1, androidx.test core 1.6.1 / ext-junit 1.2.1 / runner 1.6.2
- UI: Compose + Material3; overlay uses plain custom Views. Concurrency: coroutines only
- Clean-room: never copy code/strings/layouts/icons/DB format from the reference app; no paywall bypass
- Data (M02, v1, nothing released so reversible only until the first public build):
  - Folders are `items` rows of type FOLDER with `parentItemId` children (no separate folders table; nesting-ready, cascade delete). Deviates from the prompt's "folders" entity list on purpose
  - Reference `blocks_count` dropped (derived). Trigger corners/centres are expressed by length+offset, enum has only LEFT/RIGHT/TOP/BOTTOM
  - Settings are string-valued Preferences with per-field fallback; key names are my own (not the reference's); `service_enabled` is device state and is excluded from backup (default false)
  - Backup v1 is validated fully before a single transaction; excludes custom icons and app cache (M05/M13 decide icon portability)
  - Enums persisted by name; never rename. Schema changes need a Migration + test + CHANGELOG
- No signing config or keystores in repo; `allWarningsAsErrors` still off

## Feature checklist (mirrors section 2 of the prompt)
- [ ] 2.1 Triggers (partial: one fixed trigger device-verified; DB table + rules exist, unused)  - [ ] 2.2 Overlay/gesture (partial: spike, device-verified)
- [ ] 2.3 Zones (partial: table + repository, no UI)  - [ ] 2.4 Shortcuts grid (spike 3x4 only; items table + repository, no UI)  - [ ] 2.5 Folders (partial: data model only)
- [ ] 2.6 Action shortcuts  - [ ] 2.7 Other launch types  - [ ] 2.8 Apps index A-Z (partial: `app_cache` table only)  - [ ] 2.9 Small-hand mode
- [ ] 2.10 Appearance/theming  - [ ] 2.11 Behaviour/background (partial: 3 setting keys)  - [ ] 2.12 Onboarding/permissions (partial: permission screen)
- [ ] 2.13 Backup/restore (partial: export/import logic + tests written, no UI, not CI-verified)  - [ ] 2.14 Monetisation  - [ ] 2.15 i18n/misc  - [ ] 2.16 File-system folders (partial: `FS_FOLDER` type + `options` column only)

## Open questions / UNKNOWN items still to verify
- Prompt section 8 items (folder hover/nesting, hotspot/lock-orientation, icon packs, free-tier limits, haptics...) - untouched
- Defaults I chose without reference data: grid direction TOP_TO_BOTTOM, grid position CENTER, label BELOW
- Do file-folder shortcuts count against the free limit (suggested yes)? Decide before M14
- Final package id and branding (placeholder); monetisation model and free-tier limits (decide before M14)
- Custom icons in backups (portability) - decide in M05/M13

## How to build and test
- `./gradlew assembleDebug lint test` (JDK 17, Android SDK 35). Repo workflow: `.github/workflows/build.yml` (uploads APKs, reports, `app/schemas`)
- Instrumented: `./gradlew connectedAndroidTest` (device/emulator; needs committed schema 1.json)
- Install: `./gradlew installPlayDebug` or `installSideloadDebug`
- Local pure-Kotlin check used here: kotlinc 2.0.21 + shim + stubs (not a substitute for Gradle)
