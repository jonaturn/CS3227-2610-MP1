# Staniz Development Interaction Summary

## UI plan check moved out of the desktop suite - 23 September 2026

- The user observed that using a Markdown file to scope a GUI test felt wrong. That
  surfaced a design fault the review had missed: a documentation consistency check
  was living inside a behaviour test, so a stale Markdown table failed `guiTest`
  and looked like an application regression.
- The check turned out to be two things. Comparing the plan's cases against the
  mapping is pure data and needs no desktop. Confirming each mapped scenario
  actually ran can only come from a completed journey. Only the first could move.
- A new `UiTestPlan` class holds the mapping and the plan reader.
  `UiTestPlanCoverageTest` is untagged, so it runs with the backend suite under
  `check` and reports under its own name. `WorkoutGuiTest` keeps only the
  transcript check. The plan's system property and declared input moved from
  `guiTest` to `test`.
- A separate Gradle task was considered and rejected. It would start a JVM for one
  assertion and need manual wiring into `check`, while an untagged test gets both
  for free and still runs alone with `gradlew test --tests '*UiTestPlan*'`.
- The first version compared the two sets directly and printed both sides
  scrambled, because the mapping is an unordered map. It now reports two directed
  differences, naming which side is at fault.
- Both directions verified by mutation and reverted. A stale plan now fails in
  about twelve seconds under `check` with no JavaFX. Backend count 48 to 49.

## Deliverables pass: guides, logs and reflection - 23 September 2026

- The user asked to get the submission deliverables in order. `README.md` was
  checked and needed nothing.
- `logs/PromptSummary.md` had a stale period header and named only Codex as the AI
  tool, although the later work ran in Claude Code after the Codex monthly limit
  was reached. Both were corrected. At the user's request the earlier Codex pricing
  and account-setup task was removed, taking the log from 138 to 136 entries with
  all later entries renumbered and two cross-references corrected. The prompts that
  requested that consolidation were reworded rather than deleted, so the record
  still shows what was asked and now says why its content is absent.
- `logs/DevelopmentInteractionSummary.md` had no entry for 23 September although
  that day carried most of the review. Dated sections were added for each approved
  piece of work.
- Both guides were verified against current behaviour and five factual gaps closed.
  The user guide described only set fields as highlighted on error, and claimed a
  failed save applies nothing, which is now only half true for an active session
  where the typed entry is retained. The developer guide did not document the plan
  coverage requirement, still described CI as not packaging the JAR, and did not
  mention that Java 25 is now enforced by a toolchain.
- `docs/Reflections.md` gained a closing section on the workout conversion and this
  review. The user supplied the judgements and the agent structured them; the
  first-person narrative was deliberately not drafted for them, since a reflection
  on their own use of AI is an account only they can give. Their four points were
  declining unknown-key strictness because users are not meant to hand-edit the
  save file, accepting the history filter reset as cheaper than remembering state,
  the temp-file-then-move design surviving every injected failure, and questioning
  an assertion style they had themselves approved earlier in the same session.

## Build and CI hardening - 23 September 2026

- The user approved four supporting-file changes after a review of `styles.css`
  and `build.gradle`. The stylesheet itself needed nothing: every custom class it
  defines is applied in Java, and the only unmatched selectors are JavaFX's own
  `thumb`, `track` and `viewport` substructures.
- `build.gradle` now declares a Java 25 toolchain rather than only source and
  target compatibility, so building without JDK 25 reports an explicit toolchain
  error instead of `invalid source release: 25`. This matters because the project
  is built by a marker on their own machine.
- CI now runs `check shadowJar` instead of `check`. Nothing previously proved the
  released JAR still packages, so a packaging failure would have left CI green on
  all three platforms while `release/staniz.jar` was broken.
- Removed the vestigial `run { standardInput = System.in }` left from the console
  version, and the `add-set` style class, which was referenced by neither the
  stylesheet nor any test.
- Java 25 `check guiTest shadowJar` passed 48 backend tests, the GUI journey and
  both Checkstyle tasks; JAR rebuilt.

## Storage test strengthening - 23 September 2026

- The user approved pinning load-failure messages in `WorkoutStoreTest`. Its
  nine-case loop previously asserted only the exception type, although the nine
  documents fail for different reasons across three layers, and storage concern 2
  of this review exists specifically to produce useful load-failure messages.
- Every case now asserts the message carries the recovery guidance, and the three
  cases this project words are pinned to their explanations. Parser wording is
  deliberately not asserted, because it belongs to Gson and would break on a
  library upgrade.
- A new test proves a failed save deletes its temporary file and leaves the target
  untouched. The cleanup code already ran, but nothing asserted it worked, so a
  broken cleanup would have littered the user's data folder unnoticed.
- The related ordering guarantee, that a failed save leaves the migration flag set
  so the retry still writes the backup, was deliberately left unpinned: forcing
  that failure needs platform-specific mechanisms that would skip on either
  Windows or POSIX, and adding a seam to production code for testability alone was
  judged not worth it.

## Backend test message assertions - 23 September 2026

- The user approved exact message assertions across `WorkoutServiceTest`,
  `WorkoutImprovementsTest` and `WorkoutServiceBoundaryTest`, preferring exact
  matching over substring checks. The service's messages are shown to the user
  verbatim by the GUI, so they are user-facing contract rather than internal
  detail, and several tests checked five or six different rules with identical
  assertions that could not tell them apart.
- Service messages asserted nowhere fell from eleven to zero. The last two guards,
  for an unknown history record and an unknown library exercise, turned out to have
  no test at all and gained one.
- Two behaviours surfaced that the previous assertions hid: moving up from the
  first day is rejected by the position rule rather than the direction rule, and
  all three completion routes on an empty day share a single guard.
- Verified by mutation. Swapping the identity rule's message for the archive rule's
  failed two tests; narrowing the direction rule failed with a wrong-reason
  mismatch; making an unknown exercise ID resolve to the first library entry, which
  would silently archive the wrong exercise, failed the new guard test.

## Model state invariant coverage - 23 September 2026

- The user approved covering the `State` constructor's referential invariants,
  which no test exercised. `new State(` appeared in the whole test tree exactly
  twice, and only value-level rules were verified.
- `WorkoutDataTest` went from one test to five, covering duplicate identities,
  exercises missing from the library wherever they appear, session-to-current-day
  consistency and split bounds, each asserting its specific message. The bounds
  test also asserts the accepted edges, so the rules are shown to accept what they
  should rather than only to reject.
- Backend suite moved from 42 to 46 tests. Verified by mutation: weakening the
  session-day rule failed only the new test, and no pre-existing test noticed the
  rule disappearing.

## GUI test assertion strength and plan coverage - 23 September 2026

- The user approved tightening three assertions in `WorkoutGuiTest` that searched
  the whole flattened scene for strings present regardless of behaviour. Two day
  checks matched names that the cycle chips render for every day, and a session
  check matched a word supplied by an unrelated placeholder label.
- Demonstrated rather than assumed: with the session error suppressed entirely, the
  original assertion still passed, which proves it was vacuous rather than merely
  loose. The hero's day-name label gained an id so the replacement can name the day
  exactly, since the `workout-title` class is shared with two other views.
- Failures now report how many scenarios ran and the last one completed. Splitting
  the journey into independent tests was deliberately rejected: the scenarios share
  accumulated state and several test restart and cycle position.
- A coverage map ties all 33 numbered cases in the UI test plan to the scenario
  exercising each, failing if the two drift in either direction. Proving it works
  exposed a real hole: Gradle had not been told the plan file is an input, so
  editing it left the task up to date and the new check never ran. `build.gradle`
  now declares it. The check was initially written inside the desktop suite and was
  moved out later the same day, as recorded below.

## GUI method decomposition - 23 September 2026

- After discussing whether long methods are normal in programmatic UI code, the
  user approved refactoring the two recommended on evidence of interleaved
  concerns rather than length. Measurement came first; no line limit was applied.
- `MainWindow.currentView` went from about 87 lines to 24, delegating to
  `activeSession`, `emptySplit`, `workoutHero`, `cycleChips` and
  `completionActions`. The rule that recording actions need at least one exercise
  is now applied once instead of at two separate points, and Skip's deliberate
  exemption is stated rather than implied by omission.
- `RoutineDialogs.copySets` went from about 61 lines to 7, using the same helper
  names already approved for `WorkoutDialog` so the two dialogs read alike.
- Both are behaviour-preserving: no control id, style class, message, ordering or
  disable rule changed. Confirmed against `build/gui-test/05-current.png` as well
  as the suite, since a green build does not prove a view still looks right.
- `splitView`, `libraryView` and `historyView`, now the longest at about 57 to 61
  lines, were deliberately left alone rather than split on line count.

## GUI review fixes: editor, session, progress and dialogs - 23 September 2026

- Completed the file-by-file review of the remaining GUI sources with the user
  approving each fix in turn.
- `WorkoutDialog` and `RoutineDialogs` now run their post-save callback outside the
  validation catch. A failure after a committed save would otherwise have been
  shown as a retryable form error; in the copy dialog it would also have refused to
  close on every further confirmation.
- `WorkoutEditor` reports an unchosen exercise the way it reports a bad set,
  naming, marking and focusing the row, and checks the name before the sets so the
  marked control always matches the message. The `invalid` style is now paired with
  the selector control, without which the mark would have rendered nothing.
- `SessionView` set rows announce themselves correctly to assistive technology,
  replacing a raw zero-based index and two controls with no accessible text at all.
  A failed write no longer erases text the user is still typing, which also removed
  a mutable field and a re-entrancy guard.
- `ProgressView` uses the shared pluralisation helper, so a single recording no
  longer reads "1 exact recorded sessions", and its selector holds library entries
  instead of their names, removing a name-to-identity lookup and its unguarded
  `orElseThrow`.
- `RoutineDialogs` error label now clears on any selection change, reserves no
  layout space when empty, and renders as an error, which required attaching the
  stylesheet the dialog never loaded.
- Four new UI plan cases, 4a, 13a, 20a and 22a, cover the behaviours these fixes
  introduced; each fails against the code as it was before.

## WorkoutDialog review: focused helpers and review guidance - 22 September 2026

- The user approved decomposing the shared form setup by responsibility and adding
  that practice to the existing code-review skill, without arbitrary method limits.
- WorkoutDialog.show now coordinates dialog configuration, validation feedback,
  form layout, button setup and save-action helpers. A private DialogText record
  groups mode-specific text, selected with clear branches rather than repeated
  nested ternaries. A private SaveOperation interface keeps service routing out of
  the common save handler. UI behavior, IDs, styling and callback/catch semantics
  remain unchanged; other review concerns are still pending discussion.
- Updated the existing user-level review-agent SKILL.md outside the repository
  with focused method-structure guidance; retained its frontmatter and all existing
  instructions. Verified the installed content against the reviewed update. Its
  bundled Python validator could not run because Python is unavailable locally.
- Java 25 `check guiTest shadowJar` passed all 42 backend tests, both Checkstyle
  tasks and the full GUI journey; JAR rebuilt. Existing tests/UI plan remain
  applicable without changes. Updated developer guide and handoff. No commit/push.

## WorkoutDialog review: named entry methods - 22 September 2026

- The user approved editPlan, recordWorkout and correctHistory entry methods.
  Recording resolves the current day internally; history derives exact-rep
  requirements from its recording type. One private show method retains form
  construction and save handling. MainWindow uses matching named handlers and
  one shared refresh/status callback; callers no longer supply mode combinations.
- Extended the existing GUI journey to reject a reversed range then save a valid
  range in as-planned history, preserving record identity/date/type and surrounding
  state through reload. Actual history also rejects ranges before an exact correction.
  Updated developer guide, UI plan, prompt log and handoff. UI behavior is unchanged.
- Java 25 `check guiTest shadowJar` passed all 42 backend tests, both Checkstyle
  tasks and the expanded GUI journey; release JAR rebuilt. No separate packaged
  smoke run, commit or push. Other dialog review observations remain unimplemented.

## MainWindow review: workout dialog extraction - 22 September 2026

- The user accepted history filters and selection resetting on refresh, then
  approved extracting the modal workout form and correcting outdated Javadoc.
- Added package-private WorkoutDialog for form construction, editor validation
  feedback and plan/completion/history save routing. MainWindow opens it and
  refreshes with a success message through a callback after saving. WorkoutEditor
  still handles exercise/set controls. Retained IDs, styling, messages, dimensions,
  Cancel behavior and draft retention on failures. No filter persistence added.
- Updated MainWindow documentation to describe five views including Progress,
  and documented the dialog responsibility in the developer guide.
- Java 25 `check guiTest shadowJar` passed all 42 backend tests, both Checkstyle
  tasks and the complete GUI journey. Existing tests and UI test plan were retained;
  no user-visible behavior changes. Inspected the editor screenshot. Rebuilt
  release/staniz.jar; no separate packaged-launch smoke run. Handoff updated.
  No commit or push was performed.

## JSON validator review: helper names - 22 September 2026

- The user approved renaming private WorkoutJsonValidation helpers after review.
  Validation actions now use validate-prefixed names; value-returning helpers
  use readInteger and requireArray/Object/Number/Field. Renamed the exception
  factory to invalidFieldError and updated all calls and the set method reference.
- Validation behavior, JSON format and error messages remain unchanged. Existing
  tests were retained without additions for this naming-only refactor.
- Java 25 `check shadowJar` passed all 42 backend tests and both Checkstyle tasks;
  rebuilt release/staniz.jar. GUI checks were not rerun. Updated prompt log and
  handoff. No commit or push was performed.

## Package organization - 22 September 2026

- The user approved organizing by responsibility: WorkoutData in model,
  WorkoutService in service, WorkoutStore/WorkoutJsonValidation/StorageException
  in storage, and the existing JavaFX classes in gui. Updated declarations/imports.
- Moved backend tests into corresponding packages. The existing direct model
  invariants test moved from WorkoutStoreTest into WorkoutDataTest; no test cases
  were removed and the count remains 42. WorkoutJsonValidation stays package-private.
- Updated PIT targetTests to discover model/service/storage tests, the developer
  guide's package layout, prompt log and handoff. JSON format, launcher entry point,
  user-facing behavior, and UI test scenarios are unchanged.
- Java 25 `clean check guiTest shadowJar` passed all 42 backend tests, both
  Checkstyle tasks and the full GUI journey. JAR inspection confirmed required
  classes under the new packages and no stale workout/exception package entries.
  The packaged JAR opened and closed normally (exit 0) in the isolated smoke folder.
- Mutation analysis was not run; its discovery configuration was updated.
  No commit or push was performed. Continue remaining dedicated file reviews
  from the new package paths when requested.

## Service review: boundary tests - 22 September 2026

- The user chose to retain performance sorting and approved the remaining test
  recommendations. Added three tests in WorkoutServiceBoundaryTest; production
  code and sorting behavior remain unchanged.
- Session tests cover absent sessions and negative/past-last exercise/set indexes,
  with different set counts. Rejections preserve memory and file bytes, then valid
  first/last selections save and survive restart without changing plans or history.
- Fixed-clock queries cover empty/single/mixed history, timestamps deliberately
  out of insertion order, as-planned/unrelated/empty records, and equal timestamps.
  They verify chronological results and latest selection without reordering saved
  history; ties preserve saved order across restart.
- Java 25 `check` passed 42 backend tests and both Checkstyle tasks. No GUI rerun
  or JAR rebuild was needed for tests-only changes. Updated the developer guide,
  prompt log and handoff. All discussed WorkoutService concerns are resolved.
  No commit or push was performed.

## Service review: reuse completion logic - 22 September 2026

- The user approved abstracting duplicated recording and cycle advancement.
  Quick completion and finishSession now share record(), which accepts the
  relevant day/session snapshot, creates history, and calls shared advance().
- advance() owns next-position calculation, clearing the finished session and
  committing State. Quick-mode guards remain in requireWorkout(); skip explicitly
  checks for no active session. Active completion still requires checked sets.
- Strengthened two existing tests: all quick modes/skip reject a checked active
  session without changing memory/disk; failed active completion can retry once,
  and a second finish is rejected without duplicating history or advancement.
- Java 25 `check shadowJar` passed 39 backend tests and both Checkstyle tasks.
  JAR rebuilt. No UI behavior changed, so GUI tests and UI plan were not rerun
  or changed. Updated developer guide, prompt log and handoff. Other service
  recommendations remain pending approval. No commit or push was performed.

## Service review: fixed exercise names - 22 September 2026

- After discussing name-only validation and history corrections, the user chose
  to make library exercise names immutable and asked about corresponding tests.
- Removed WorkoutService.renameExercise and the library Rename control. Library
  guidance now states that names are fixed; adding, archiving and restoring remain.
  Workout/history editors can still replace an exercise and edit its sets.
- Service inputs now resolve name-only choices and validate supplied ID/name pairs.
  An existing snapshot may retain its exact older pair for compatibility with
  earlier releases. Archive eligibility is checked by identity. Recorded and
  corrected mismatches are rejected before saving or publishing state.
- Replaced rename-focused tests with archive/restore invariance and an older-save
  compatibility fixture. Retained migration/backup verification by using archive
  as the first post-migration change. Added tests for mismatched actual/history
  inputs, archived selections, and plan/history replacement with unchanged library,
  other records, cycle position and active session. Performance attribution updates.
- Updated the GUI journey to check fixed-name guidance and absence of Rename,
  archive/restore, rejected history replacement then valid Squat replacement, and
  planned exercise replacement followed by reload. Updated guides and UI test plan.
- Java 25 `check guiTest shadowJar` passed all 39 backend tests, both Checkstyle
  tasks, and the full desktop journey. One overlong helper line was wrapped after
  the first Checkstyle run. The JAR was rebuilt. Reviewed new screenshots
  12-immutable-library.png and 13-history-replacement.png in build/gui-test;
  full interaction evidence is build/gui-test/transcript.txt.
- Updated prompt log and handoff. No commit or push was performed.

## Storage review: separate migration steps - 22 September 2026

- The user approved extracting private helpers within WorkoutStore. migrate()
  now coordinates library creation, planned-day linking, history migration and
  archival of historical-only exercises before updating the document schema.
- Preserved the existing loops, deterministic ID formula, processing order and
  backup behavior. Javadoc explains why history IDs must precede record mutations.
- Added a regression with duplicate historical records referencing an exercise
  absent from the active library. It checks one archived entry, active statuses,
  ordering, values, position, fixed record IDs, repeated loads and original backups.
  The test passed against the original implementation before extraction and after.
- Java 25 `check shadowJar` passed all 37 backend tests and both Checkstyle tasks;
  the JAR was rebuilt. No UI changes, GUI run or UI-plan changes were needed.
- Updated the developer guide, prompt log and handoff. All three discussed
  WorkoutStore concerns are addressed. No commit or push was performed.

## Storage review: meaningful load errors - 22 September 2026

- The user approved showing model-validation explanations hidden by Gson's
  constructor exceptions. WorkoutStore now selects the first nonblank argument-
  validation message in the cause chain, falling back to the original diagnostic.
- File-path/recovery guidance and the original exception chain remain intact.
  Selecting the first validation message preserves the friendly completed-session
  explanation even when a lower-level number-parsing exception is its cause.
- Added negative reps/weights and completed-session regression tests; strengthened
  mismatched-identity, malformed-JSON and file-read error assertions. Tests check
  unchanged saved bytes, debugging causes, and successful loading after repair.
- Java 25 `check shadowJar` passed 36 backend tests and both Checkstyle checks.
  An initial Checkstyle failure (one 121-character test line) was fixed before the
  successful rerun. The JAR was rebuilt; GUI tests/manual launch were not rerun.
- Updated the developer guide, manual startup-error plan, prompt log and handoff.
  Migration refactoring remains pending approval. No commit or push was performed.

## Storage review: validate JSON before record conversion - 22 September 2026

- The user approved the first WorkoutStore concern: prevent missing primitive
  fields and fractional integers from silently becoming valid-looking values.
- Added a package-private WorkoutJsonValidation helper, called before migration
  and record conversion. It checks required fields, exact JSON types, and whole
  numbers within Java's integer range across plans, history and session drafts.
- The store validates the version before selecting migration. Valid legacy saves
  still migrate and retain their original-byte backup; optional sessions and
  unfinished string drafts remain supported. Model constructors retain workout rules.
- Five new regression tests cover missing/null fields, incorrect types, fractions,
  overflow, valid boundary values, optional sessions, draft recovery and migration.
  Invalid loads are interleaved with valid ones and leave saved files unchanged.
- Java 25 `check shadowJar` passed all 34 backend tests and both Checkstyle tasks;
  rebuilt release/staniz.jar. No desktop suite was rerun for this storage change.
  Updated the developer guide, startup-error manual checks, prompt log and handoff.
- Other review concerns (wrapped model errors and migration structure) remain
  pending discussion/approval. No commit or push was performed.

## Model review: shared constants — 22 September 2026

- The user approved named constants for shared rules. WorkoutData now defines
  CURRENT_SCHEMA_VERSION and MAX_WORKOUT_DAYS; the model, service, and GUI use them.
- Split guidance, the day-count badge, and the limit error derive their number
  from the same maximum. The guidance/error use the numeral 7 rather than seven.
- WorkoutStore names its migration source/target versions separately, preserving
  its explicit version-one-to-version-two conversion for future schema changes.
- The existing seven-day desktop scenario now checks the guidance and count as
  well as both Add and Duplicate being disabled. Updated the test plan and guide.
- Java 25 `check guiTest shadowJar` passed all 29 backend tests, both Checkstyle
  tasks, and the complete desktop journey. The executable JAR was rebuilt; GUI
  input/output evidence is in `build/gui-test/transcript.txt`.

## Model review: direct first-use initialization — 22 September 2026

- After clarifying when starter exercises are created and saved, the user approved
  separating new-data creation from old-save migration.
- State.initial now constructs ten active Exercise records with new UUIDs and a
  version-two state directly. Removed the version-one compatibility constructor
  and legacyVersion helper; updated the two validation-test callers to version two.
- WorkoutStore's version-one migration, deterministic migration IDs, and original
  file backup remain unchanged. First-use tests also check the current schema,
  ten active starter entries, and that loading alone does not create a save file.
- Java 25 `check shadowJar` passed all 29 backend tests, including migration and
  backup verification, plus both Checkstyle tasks. The packaged JAR was rebuilt.

## Model review: separate state validation — 22 September 2026

- The user approved extracting the State constructor's responsibilities into
  private helpers. The constructor now coordinates reference resolution, split
  position checks, library indexing, planned-day validation, history validation,
  and active-session validation.
- Helpers remain inside State with explanatory Javadoc. Validation order, error
  messages, immutable collections, and snapshot behaviour are preserved.
- Java 25 `check shadowJar` passed all 29 existing backend tests and both
  Checkstyle tasks; the packaged JAR was rebuilt. This was a behaviour-preserving
  refactor with no UI changes.

## Model review: planned exercise identity — 22 September 2026

- File-by-file review found that a planned name could reference a different
  existing exercise ID. A focused Java 25 reproduction confirmed the mismatch
  was accepted. The user asked how to implement the proposed validation.
- State validation now looks up library entries by ID and rejects planned names
  that do not match. History and active-session snapshots retain older names after
  renames. This avoids guessing which half of inconsistent saved data is correct.
- Regression tests cover rejection without changing memory/disk, malformed JSON
  remaining untouched, and renaming during a session followed by restart/completion.
- Java 25 `check shadowJar` passed: 29 backend tests, production/test Checkstyle,
  and JAR packaging. The storage test was corrected to inspect the nested
  validation cause because Gson wraps constructor exceptions.

## Workout tracking improvements — 21 September 2026

- The user authorized six connected features: active workout mode, previous
  performance, routine-copying tools, editable/filterable history, library
  rename/archive, and exercise progress views.
- Schema version two adds stable exercise and record IDs and saved active-session
  drafts. Existing version-one data migrates in memory; its original bytes are
  backed up before the first upgraded save. Local JSON remains sufficient.
- Active sessions save text and completion ticks immediately, recover on restart,
  record only checked sets, and advance once. Invalid or failed saves preserve
  the prior state. Split edits are blocked until the session finishes or is discarded.
- Renaming updates plans but preserves historical names. Archive/restore retains
  references. History corrections preserve dates and position. Previous-performance
  comparisons and the new Progress tab follow exercise IDs through these changes.
- Routine editing supports independent day copies, exercise ordering, and copying
  a prescription onto another exercise in the same or another workout day.
- Charts require two exact recordings and show maximum set kg and total recorded
  reps per session. As-planned prescriptions are excluded from measurements.
- Java 25 verification passed 26 backend tests, Checkstyle, the extended desktop
  interaction journey, and JAR packaging. The desktop journey includes invalid
  entries, restart recovery, partial completion, rename/archive, history correction,
  and charts. Screenshots and action/output records are in `build/gui-test/`.
- The first test runs hit Windows temporary-directory cleanup failures from a
  reused restricted Gradle daemon. A fresh process (`--no-daemon`) resolved those.
  Existing assertions were updated for stable IDs and schema version two.
- Visual review prompted more readable checked-set fields, matching chart colours,
  shorter axis labels, numbered session details, and accurate completion status.
  The packaged JAR opened its workout window and closed normally in an isolated folder.
- Guides, README, test plan, and handoff were updated. No Git commit or push was requested.

## UI refinement — 21 September 2026

- After accepting the functionality, the user requested a more polished UI/UX.
  The implementation retains the workout model and persistence behavior.
- A compact light interface with green accents replaces the large banner.
  The current workout has a session summary, cycle position chips, aligned set
  tables, and completion controls outside the scrolling content. Compact window
  sizes use reduced spacing and typography.
- Split planning places the day list beside workout details. Exercise search
  filters names without changing saved data, and history uses descriptive rows.
- The editor adds last-set duplication, explicit column labels, highlighted
  invalid fields, focus on errors, and automatic clearing of stale messages.
  Removing an exercise is the explicit way to omit its last remaining set.
- GUI scenarios were extended for setup navigation, searching and clearing,
  copying a set, correcting validation errors, and action visibility at minimum
  window size. Screenshots are generated in `build/gui-test/` for visual review.

## Workout application conversion — 20–21 September 2026

- The user changed the product scope after identifying overlap with a previous
  semester's task-list project. Requirements were clarified before implementation.
- The agreed app uses JavaFX, one ordered split of up to seven workout days,
  a starter/custom exercise-name library, individual kg/reps sets, optional
  performance history, completion modes, skip, and cyclic advancement.
- The user approved a repository assessment and implementation roadmap. The
  task hierarchy, parser, console UI, and chat-bubble interface were replaced by
  immutable workout records, a service, JSON storage, and four desktop views.
- A single JSON document was selected so completing a workout saves its history
  and next-day position together. State changes are published only after a
  successful save. Old task data is left untouched.
- As-planned history preserves rep ranges; actual recording requires exact reps.
  Cancelled drafts and rejected operations do not mutate saved state. History
  snapshots survive later plan edits and removal.
- The configured local Java installation was missing. A portable Java 25 was
  downloaded into the ignored temporary directory for building and testing.
- Twenty backend tests passed, followed by the JavaFX interaction sequence.
  Screenshots exposed a clipped dialog button label, which was corrected during
  visual review. Current verification artifacts are in `build/reports/` and
  `build/gui-test/`; older metrics below refer to the former task-manager app.
- The User Guide, Developer Guide, test plan, build tasks, and session handoff
  were updated. Earlier reflection/log entries remain as historical records.
- Final verification passed on Java 25.0.4.1: 20 backend tests, both Checkstyle
  tasks, the complete JavaFX scenario sequence, and JAR packaging. Screenshots
  were inspected at normal and minimum content sizes. The packaged JAR was
  launched from an isolated directory, its workout window was detected, and it
  closed normally with exit code 0. Two smoke-checker issues (hidden-window
  detection and PowerShell exit-code tracking) were corrected during verification.
- No commit, tag, push, or external release was requested or performed.

## Purpose

This log complements [PromptSummary.md](PromptSummary.md). The prompt summary
records the chronological user requests, while this file records the principal
decisions, implementation outcomes, corrections, and verification performed
during AI-assisted development.

## Initial setup and Levels 0-3

- The user established the trimmed CS3227 Project Duke brief as the
  authoritative requirements source and required proposed diffs before code
  changes.
- The placeholder Duke application was renamed to Staniz and given its banner,
  greeting, and farewell.
- Java 25 was configured and the user learned how source compilation, classpath
  selection, JAR creation, Git commits, and lightweight milestone tags work.
- Level 1 added the input loop and `bye`; Level 2 echoed commands; Level 3 added
  tasks and the initial task list.
- Manual output review found a plausible but incorrect incomplete-task marker.
  The user corrected it to the required `[X]` and `[ ]` convention.
- Empty input and command-recognition bugs were found during manual use and
  corrected before proceeding.

## Object-oriented restructuring

- UI, parsing, command metadata, task-domain behavior, and task-list ownership
  were separated into focused classes.
- The user requested explanations of `TaskList`, parser responsibilities, and
  the stateless-versus-stateful parser trade-off before approving the design.
- Classes were organized into packages such as `staniz.parser`,
  `staniz.command`, and `staniz.task`, later supplemented by storage, UI,
  exception, and GUI packages.
- Public visibility was limited to cross-package contracts; implementation
  details remained private or package-private.

## Build, packaging, and code standards

- Gradle became the repeatable build interface for compilation, tests, running,
  quality checks, coverage, mutation testing, and JAR packaging.
- JUnit tests were grouped by production class and package, following the
  course tutorial's structure.
- The Shadow plugin produced one executable JAR containing runtime and JavaFX
  dependencies.
- Coding-standard and Checkstyle changes were reviewed carefully because some
  proposed edits differed only in whitespace or access modifiers.
- A standing rule was established that approval requests for commands must
  explain their syntax, purpose, and effect, and that merge-conflict resolutions
  require explicit user approval.

## JavaFX GUI and personality

- The initial JavaFX interface followed the course tutorial structure, with
  FXML controllers and a separate launcher suitable for a fat JAR.
- The user selected a green color scheme, asymmetric bubbles, stable window
  sizing, and separate supplied avatars for the user and Staniz.
- GUI automation through operating-system input was explored but found fragile.
  The user chose manual GUI verification because the trimmed brief did not
  require automated GUI testing.
- A disciplined training-partner personality was applied to greetings,
  confirmations, errors, and the farewell while keeping command semantics
  unchanged.

## Reliability and testing

- Assertions were added only for internal invariants, not as replacements for
  user-input validation.
- Code-quality refactoring kept command dispatch at one abstraction level and
  centralized response formatting.
- GitHub Actions was configured to run Gradle checks on supported operating
  systems with Java 25.
- Tests expanded to cover individual classes, backend integration, console I/O,
  storage failures, and command recovery after rejected input.
- JaCoCo measured structural coverage and PIT mutation testing checked whether
  tests detected behavioral changes. GUI implementation classes were excluded
  from these automated metrics according to the agreed scope.
- The console test plan interleaved valid and invalid commands so rejected input
  could be checked for unintended state changes.

## Error handling and persistence

- Parser errors were made specific for missing arguments, malformed numbers,
  invalid dates, duplicate parameters, and misordered event parameters.
- Flexible whitespace was accepted without weakening command-keyword or
  parameter-token boundaries.
- Storage validation reports malformed saved data rather than silently dropping
  it.
- Saving uses a temporary sibling file and atomic replacement when available,
  reducing the risk of corrupting the previous data file.

## Documentation and submission preparation

- The User Guide was checked against the implemented commands, response text,
  date format, persistence behavior, and Java 25 build/run instructions.
- The Developer Guide documented architecture, parsing, the task model,
  persistence, GUI and console boundaries, error handling, testing, CI, and the
  release process. It includes an acknowledgements section for reused sources,
  dependencies, supplied assets, and AI assistance.
- The authored reflection was converted to the required Markdown path and
  expanded with four detailed prompt examples.
- Prompt summaries from an earlier Codex task and the current project task were
  combined into one chronological list. The earlier task covered Codex pricing and
  account setup, and its prompts were later removed as not relevant to the project.
- Gradle was configured to generate `release/staniz.jar`. The final compliance
  build used Java 25.0.4.1, passed all 69 JUnit tests and both Checkstyle tasks,
  and produced a JAR containing the Staniz launcher, JavaFX classes and native
  libraries, FXML, CSS, and avatar resources.

## Human oversight retained

The user remained responsible for selecting requirements, approving design and
visual choices, correcting specification mismatches, deciding the scope of GUI
testing, manually checking visible behavior, and authorizing Git operations.
Codex supplied implementation, explanations, automated verification, and
documentation support within those decisions.
