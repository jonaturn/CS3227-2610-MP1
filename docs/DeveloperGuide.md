# Staniz Workout Planner — Developer Guide

## Scope and architecture

The app manages one local user's single active split, up to seven ordered days,
an exercise library, individual planned sets, a resumable active session, cycle
position, editable performance history, and progress charts. Calendar scheduling,
accounts, multiple saved splits, and exercise metadata remain outside the scope.

```text
Launcher → Main → MainWindow / WorkoutDialog / WorkoutEditor / SessionView / ProgressView / RoutineDialogs
                         ↓
                  WorkoutService
                         ↓
                   WorkoutStore → data/workouts.json
                         ↕
                  WorkoutData records
```

The previous task model, text parser, console entry point, chat-bubble controls,
and FXML views have been removed. JavaFX views are constructed directly in Java;
`styles.css` controls their appearance. Gradle, the plain Java launcher, testing
tools, and the safe temporary-file save pattern are retained.

`MainWindow` coordinates the five tabs and refreshes them after successful changes.
`WorkoutDialog` owns the modal form for planning, recording and correcting a workout,
including save routing and validation feedback. It contains `WorkoutEditor`, which
edits exercise/set controls. A failed save retains the dialog draft; Cancel discards
it. The window receives a callback after a successful save to refresh and show status.
Its named entry points are `editPlan` (a split-day index), `recordWorkout` (the
current day) and `correctHistory` (a saved record). History rep requirements come
from the record's recording type. All three use one private form implementation.
The shared form coordinates focused helpers for dialog configuration, error feedback,
layout and save handling. Mode-specific text is selected together, and the service
save operation is selected once before binding the Save action.

## Model and validation

Production classes under `src/main/java/staniz/` are organized by responsibility:

| Package | Classes |
| --- | --- |
| `model` | `WorkoutData` and its nested records |
| `service` | `WorkoutService` |
| `storage` | `WorkoutStore`, package-private `WorkoutJsonValidation`, `StorageException` |
| `gui` | JavaFX launcher, application, views, editors and dialogs |

Tests mirror these packages under `src/test/java/staniz/`. Service workflow tests
also exercise persistence and model invariants. JSON parsing belongs to storage;
the model has no Gson or JavaFX dependency.

`WorkoutData` groups small immutable Java records:

| Type | Responsibility |
| --- | --- |
| `WorkoutSet` | Finite nonnegative kg, positive minimum and maximum reps |
| `Exercise` | Fixed library ID and name, changeable archive flag |
| `WorkoutExercise` | Library exercise ID, snapshot name, and individual sets |
| `WorkoutDay` | Stable ID, display name, ordered exercises; empty drafts allowed |
| `WorkoutRecord` | Record/day IDs, timestamp, day-name snapshot, recording type, set snapshots |
| `Session`, `SessionExercise`, `SessionSet` | Starting plan snapshot, text drafts, individually completed sets |
| `State` | Schema version, library, days, current index, newest-first history, optional active session |

Constructors validate values and defensively copy lists. The state enforces
case-insensitive library uniqueness, unique day IDs, the seven-day limit,
valid current position, and planned exercises' library membership and matching
names. History and active-session names from older releases may predate a library rename. Day names
need not be unique: identity is determined by ID. Exact reps use equal bounds.
`WITH_CHANGES` history requires exact rep counts; `AS_PLANNED` preserves ranges.

`WorkoutData.CURRENT_SCHEMA_VERSION` identifies the current state format, and
`MAX_WORKOUT_DAYS` supplies the shared split limit for validation and GUI controls
and labels. Storage migration has separate, fixed source/target version constants;
changing the current schema does not change what an existing migration produces.

`WorkoutService` supplies named operations rather than accepting command text.
All mutations create a prospective state, save it, and only then publish it in
memory. Invalid input or failed saves leave the previous state visible. Its
injectable `Clock` makes recording timestamps deterministic in tests.

Reordering uses IDs to preserve the current workout. Removing the current day
selects its successor, wrapping at the end. Completion and skip use modulo
advancement. Empty splits are valid during setup but cannot advance.

Library exercise IDs and names are fixed after creation; the service exposes
add and archive/restore operations. Workout and history editors can replace an
exercise occurrence without changing the library. The service resolves name-only
selections and validates supplied ID/name pairs before saving. Only an existing
snapshot's exact pair may retain an older name from a previous app release.
Archived entries can be retained when already present in the edited workout.
Editors preserve IDs for unchanged snapshot names; new name-only choices prefer
the library before falling back to older snapshot names. History corrections
update performance attribution while retaining the record ID, date and type.
Day duplication generates a new day ID; copying sets preserves the
destination exercise ID. Lists remain immutable so copies can be edited safely.

Active sessions save incomplete text without treating it as measured performance.
Checking a set validates finite nonnegative kg and an exact positive rep count.
Finishing records only completed sets, clears the session, and advances in one
save. Quick completion and active-session completion share private recording and
cycle-advancement helpers. Public operations retain their own guards: quick
completion and skip reject an active session; finishing requires completed sets.
Discarding does not advance. Split edits and alternative completion actions
are rejected while a session is active. History corrections preserve record IDs,
dates, day IDs and recording types, without changing current position.

## Persistence

`WorkoutStore` uses [Gson](https://github.com/google/gson), configured for strict
JSON parsing. Before migration or conversion to records, `WorkoutJsonValidation`
checks required fields and their JSON types, including nested history and session
data. Integer fields must contain whole numbers within Java's integer range;
numeric strings and missing primitive values are rejected instead of silently
coerced or defaulted. Errors identify the offending JSON field. Optional `session`
may be absent or null, and unfinished session inputs remain strings. Record
constructors then validate workout rules and relationships. If Gson wraps an
argument-validation exception, loading reports the first nonblank validation
message in its cause chain. Other failures retain their original message; the
complete exception chain remains attached for debugging. All data lives
in one versioned document so recording history and advancing the cycle do not
require coordination across multiple files. Version 2 has the top-level keys
`version`, `library`, `days`, `currentDay`, `history`, and optional `session`.
Gson omits `session` when it is null. Version-one files are migrated in memory
using deterministic exercise and record IDs. Historical orphan names become
archived library entries. Private helpers separate library creation, planned-day
linking, history migration and archival. History IDs use each record's original
JSON and position before its migration; preserving that order keeps IDs stable.
The original bytes are backed up to `workouts.json.v1.bak`
before the first upgraded save; an existing backup is never overwritten.

Writes create a temporary sibling file and then replace the destination with
an atomic move where supported, falling back to a normal replacement. Incomplete
temporary files are cleaned up on failure. A missing file yields the starter
library; an unreadable, malformed, or unsupported file fails with an actionable
`StorageException` and is left untouched. The first successful change creates
the file. `data/staniz.txt` is not read or overwritten.

The app assumes one running instance per save file. It does not implement
cross-process locking or automatically retained backup generations; users can
copy the JSON while the app is closed. An external database is not required.

## JavaFX interactions

`MainWindow` presents five tabs: Current Workout, My Split, Exercises, History,
and Progress.
The current workout uses a scrollable summary and set tables with completion
actions outside the scroll area. Split and history lists use descriptive cells;
library search filters the displayed names without mutating the saved library.
The service remains the only source of persisted state. Successful actions
refresh the views. Errors are shown in the status area or inside the editor.

`WorkoutEditor` holds detached text fields and selectors until Save. It supports
adding/removing exercise occurrences and individual sets. Actual-performance
editing requires exact counts and leaves fields blank where the plan contains
a range. A dialog event filter consumes failed saves, retaining the draft for
correction. Cancel performs no service operation.
The editor can duplicate the last draft set without committing it. Field errors
highlight and focus the invalid input; editing clears stale messages. Removing
the last set is disabled so users remove the exercise explicitly instead.

`SessionView` saves per-set inputs immediately, restores the last saved values
on write failure, and displays completed/remaining counts. `ProgressView` uses
only exact `WITH_CHANGES` records. Two or more records enable JavaFX line charts
of maximum set weight and total reps per session; dated set details remain visible.
`RoutineDialogs` handles copying prescriptions across days/exercises. History
corrections reuse `WorkoutEditor` with the saved recording type's validation.

## Build and verification

Use **Java 25** for every build and test. The build declares a Java 25 toolchain,
so Gradle selects a detected JDK 25 and otherwise stops with a toolchain error
naming the missing version, rather than failing later with `invalid source
release: 25`. On Windows replace `./gradlew` below with `.\gradlew.bat`.

```text
./gradlew check
./gradlew guiTest
./gradlew shadowJar
```

- `check`: backend JUnit tests and production/test Checkstyle checks.
- `guiTest`: a separate desktop test task tagged `gui`, with a fail-fast ordered
  scenario. It fires JavaFX controls and dialogs on the JavaFX thread, checks
  rendered content and persisted state, and captures screenshots. It requires
  a desktop; Linux CI uses `xvfb-run -a ./gradlew guiTest`. A failure reports how
  many scenarios ran and the last one completed, since the run stops at the first
  failure by design: the scenarios share accumulated state and several of them
  test restart and cycle position.
- `jacocoTestReport`: generated after backend tests. JavaFX classes are excluded
  from backend coverage; the desktop suite provides separate interaction evidence.
- `pitest`: optional backend mutation analysis, excluding JavaFX tests/classes.
- `shadowJar`: packages `release/staniz.jar` with runtime dependencies.

The backend tests cover split limits, completion modes, skipped workouts,
wraparound, reordered/deleted current days, immutable history, storage failures,
Unicode JSON round trips, legacy-file preservation, and corrupt-data rejection.
They also cover version-one migration and backups, resumable drafts, partial
completion, fixed library identities, archive/restore, older snapshots, routine copies, correction validation, and
performance filtering. Boundary tests interleave rejected session indexes with
valid first/last selections and reloads, checking memory and saved-file preservation.
Fixed-clock history tests cover empty/single/mixed results, chronological ordering,
and equal timestamps. Performance queries leave stored history order unchanged;
ties retain that order, and previous performance chooses its first matching entry.
Model tests cover the `State` constructor's referential rules directly, including
identity uniqueness, exercises missing from the library, session-to-current-day
consistency, and split bounds with their accepted edges. Storage tests check that
every load failure carries recovery guidance and that a failed save deletes its
temporary file without touching the target.
Desktop scenarios exercise all six improvement flows.
The GUI plan interleaves rejected actions and successful state checks. See
[`test/ui-test-plan.md`](../test/ui-test-plan.md) for scenarios and artifact paths.
The desktop suite uses temporary files, never the user's real `data/` directory.

**Adding or removing a UI plan case:** `UiTestPlan.COVERAGE` maps every numbered
case in `test/ui-test-plan.md` to the scenario that exercises it, and two separate
checks use it. `UiTestPlanCoverageTest` runs with the backend suite and compares
the plan against the map in both directions, naming the cases that are listed but
unmapped or mapped but no longer listed. It needs no desktop, so a stale plan
fails in seconds under `check` rather than appearing as a GUI regression. The
desktop suite then checks that each mapped scenario actually ran, which only a
completed journey can show. The plan file is a declared input of the `test` task,
so editing it alone is enough to rerun that check.

CI runs backend checks and packages `release/staniz.jar` on Windows, macOS, and
Linux, and runs desktop tests under Xvfb on Linux. Native desktop appearance on each platform still merits manual
review, particularly keyboard navigation, scrolling, and display scaling.

## Release and attribution

Run `clean check`, the desktop suite, and `shadowJar`, then launch the resulting
JAR from an isolated directory to verify first-use behavior. Source, guides, and
JAR should be reviewed together before submission. No commit, tag, push, or public
release is automatic.

This repository began with the SE-EDU Duke starter project. The previous
task-manager implementation remains in Git history and historical development
logs. Runtime/build dependencies include OpenJFX, Gson, Gradle, Shadow, JUnit,
Checkstyle, JaCoCo, and PIT. Codex assisted with implementation, tests, and
documentation; the project author selected the workout requirements and retains
responsibility for review and submission. See [CONTRIBUTORS.md](../CONTRIBUTORS.md).
