# CHANGELOG

## M02 - data layer
- Room database v1 (zones, triggers, items with nesting, app cache), DAOs, repositories with business rules
  (default trigger undeletable, geometry clamping, atomic reorder, parent/zone consistency).
- DataStore settings (12 documented keys, per-field safe defaults); versioned JSON backup with full
  validation and transactional import; migration guard test + instrumented migration harness.
- KSP, Room, DataStore, kotlinx.serialization, Robolectric added to the version catalog; CI uploads `app/schemas`.

## M01 - overlay + trigger spike
- Edge trigger strip, non-touchable full-screen overlay, 3x4 ring/grid, release-to-launch (spike app list).
- Foreground service with Stop action, in-app switch, idle watchdog; permission screen; latency readout.
- Pure CellGrid/GestureTracker logic with 20 unit tests.

## M00 - project foundation and CI
- Kotlin/Compose project, Gradle 8.10.2 wrapper, version catalog, `play`/`sideload` flavours.
- Empty settings screen, own adaptive icon, light/dark theme, R8 rules, GitHub Actions workflow.
