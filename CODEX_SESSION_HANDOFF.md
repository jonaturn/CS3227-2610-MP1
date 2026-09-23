# Staniz workout-app handoff

Last updated: 2026-09-22 (Asia/Singapore).

Read `AGENTS.md` first, then this document. This document supersedes the old
Level-8 task-manager checkpoint. The workout conversion and six improvements
below are implemented. We are now reviewing the code file by file, discussing
each concern and obtaining approval for its fix. Do not interpret earlier feature
approval as blanket approval to implement newly identified review fixes.

- Current review: `src/test/java/staniz/storage/WorkoutJsonValidationTest.java`,
  the last test file. Most thorough in the project. assertRejectedThenRecover
  checks the message category, the exact derived JSON path, byte-identical file
  contents after rejection, absence of a spurious .v1.bak, and recovery to the same
  state, across 74 missing-field cases, 42 numeric cases and 15 coercion cases.
  First concern was raised, discussed at length and then dropped by decision; do
  not re-raise it. The observation was that unknown JSON fields are ignored with no
  test pinning it. Discussion established three things worth keeping. The loader
  tolerates unknown keys only by omission, since the validator never enumerates
  keys and Gson drops unmapped members. Unknown keys are also dropped on the next
  save, because save serializes a State that never held them, so the property is
  load tolerance and not round-trip preservation. And the dangerous version of that,
  a newer file silently stripped by an older build, is already prevented by the
  version gate, which rejects any version it cannot migrate rather than loading it;
  the exposure is limited to additive changes that skip a version bump, which the
  project's own migration convention already rules out.
  Strict unknown-key rejection was then considered, since it would match the
  "reject rather than guess" philosophy of storage concern 1, and would catch
  additive typos such as adding "kgs" beside a valid "kg", which currently do
  nothing silently. Note that a typo in a required field is already caught
  precisely, with the JSON path named. The user decided against it on the grounds
  that users are not expected to hand-edit the file. With that, the canary test was
  also dropped: pinning an incidental behavior nobody chose would impose a
  constraint on future validation work for no benefit. The rest of the validation
  remains justified regardless, since it guards truncated writes, pre-migration
  files, partially synced copies and writer bugs, none of which unknown-key
  checking would catch.
  Second, smaller: the missing-field list is representative rather than exhaustive.
  Absent are history.0.exercises.0.name, history.0.exercises.0.sets and its
  minReps/maxReps, and session.exercises.0.sets.0.planned.minReps/maxReps. The
  shared helpers mean the same validation code runs, so the real risk is only that
  a wiring mistake would surface with a vaguer model-level message instead of a
  path-naming one. Cheap to close by extending the list.

- Supporting-file review, styles.css and build.gradle, complete with four approved
  changes. styles.css is clean: every custom class defined there is applied in Java,
  and the only unmatched selectors are JavaFX's own thumb, track and viewport. The
  reverse check found four Java style classes with no CSS rule; exercise-selector,
  set-reps and set-weight are GUI-test lookup handles and must stay, while add-set
  was dead in both directions and was removed.
  build.gradle now declares a Java 25 toolchain instead of only source/target
  compatibility, so a machine without JDK 25 fails with an explicit toolchain
  message rather than "invalid source release: 25". This matters because the
  project is built by a grader on their own machine. Verified that the portable JDK
  under _temp still satisfies it offline. The vestigial run { standardInput } from
  the console version was removed.
  CI now runs `check shadowJar` rather than `check`, because nothing was proving the
  deliverable JAR still packages; a shadow conflict would have left CI green on all
  three platforms while release/staniz.jar was broken. CI still runs guiTest on
  Linux only, under xvfb, which is deliberate.
  Java 25 check guiTest shadowJar passed 48 backend tests, the GUI journey and both
  Checkstyle tasks on 2026-09-23, zero failures and errors across all seven suites;
  release/staniz.jar rebuilt at about 14.6 MB, which is expected since the JAR
  bundles JavaFX natives for all three platforms.

- Deliverables pass, partially complete. README was checked and needed nothing;
  it is accurate for the workout app. logs/PromptSummary.md period header was
  stale at "to 21 September" and now reads 23 September.
  logs/DevelopmentInteractionSummary.md had no entry for 23 September at all,
  although that day carried most of the review; seven dated sections were added
  covering the GUI review fixes, the two method decompositions, GUI test assertion
  strength and plan coverage, model invariant coverage, backend message
  assertions, storage test strengthening, and build/CI hardening.
  Guides verified against current behavior and five factual gaps corrected.
  UserGuide: the editor-error paragraph described only set fields, and the
  saving paragraph said a failed save applies nothing, which is now only half true
  for an active session, where the typed entry is retained and only a ticked set
  is unticked. DeveloperGuide: the plan-coverage requirement was undocumented,
  which mattered most, because editing test/ui-test-plan.md without updating
  PLAN_COVERAGE now fails guiTest and the message would otherwise look like a GUI
  regression; CI now also packages the JAR; Java 25 is enforced by a toolchain;
  and the test-coverage inventory gained model invariants and save-failure
  cleanup. The guide deliberately describes test categories rather than counts,
  so no count went stale and none was introduced.
  Reflections.md now has a closing section covering the workout conversion and
  this review, written from judgements the user supplied rather than drafted for
  them. Its four parts are deciding what not to validate (unknown-key strictness
  declined because users are not meant to hand-edit, with the reasoning traced to
  CS2103's emphasis on stating who touches what), deciding what not to build (the
  history filter reset accepted as cheaper than remembering state), a design
  property that earned its keep (temp-file-then-move surviving every injected
  failure), and being reminded of their own decisions (questioning an assertion
  style they had themselves approved earlier in the session). Style constraints
  the user set for this section: no colons, no semicolons, no italics, short
  sentences. Keep those if it is extended.
  Tool attribution corrected across the deliverables: the reflection's header note
  and PromptSummary now record Codex and then Claude Code from 22 September 2026,
  after the Codex monthly limit was reached.
  PromptSummary housekeeping at the user's request: the earlier Codex pricing and
  account-setup task was removed, taking the log from 138 to 136 entries with all
  later entries renumbered. Two handoff cross-references were corrected to match,
  entry 120 to 118 and entries 104-108 to 102-106. Entry 92 and the matching line
  in DevelopmentInteractionSummary were reworded rather than deleted, so the
  request is still recorded and now says why its content is absent. If entries are
  removed again, re-check those cross-references.
  STILL OUTSTANDING, the user may iterate further: docs/Reflections.md
  covers only the task-manager era and carries a note saying so. The workout
  conversion and this entire review have no reflection. Important boundary agreed
  in discussion: the reflection is the user's own account of their judgement, so
  do not draft the first-person narrative for them. What was offered instead is
  the factual material, including decisions the user made against recommendation
  (keeping the per-keystroke session save, rejecting unknown-key strictness
  because users are not meant to hand-edit, skipping the boundary-test cleanup,
  deferring the Form refactor), the moments where their questions changed the
  outcome (the disk-rigour suggestion withdrawn after they asked what it would
  catch, the docs-check-inside-a-behaviour-test smell they spotted), and the cases
  where verification contradicted reasoning (Gradle skipping the plan check, the
  test bug where an identical value fired no save).

- Carried-forward item closed: the plan-drift check no longer lives inside guiTest.
  Design note, because the split is not symmetric. The check had two halves. Whether
  the plan and the mapping list the same cases is pure data comparison needing no
  desktop, and that moved out. Whether each mapped scenario actually ran can only
  be shown by a completed journey, so it stays in WorkoutGuiTest. Do not try to
  move the second half.
  New test-support class `UiTestPlan` holds COVERAGE and the plan reader.
  `UiTestPlanCoverageTest` is untagged, so it runs under `check` with the backend
  suite, and reports under its own name instead of looking like a GUI regression.
  A literal separate Gradle Test task was considered and rejected: it would spawn
  a JVM for one assertion and need manual wiring into check, while an untagged
  test gets both for free and still runs alone via
  `gradlew test --tests '*UiTestPlan*'`.
  The plan file's systemProperty and inputs.file declaration moved from guiTest to
  test, since guiTest no longer reads the file.
  Failure reporting was deliberately written as two directed differences rather
  than one set comparison, because COVERAGE is a Map.ofEntries and comparing the
  sets whole printed both sides scrambled and unreadable.
  Both directions verified by mutation: adding a case 99 row failed with
  "Cases listed in test/ui-test-plan.md with no entry in UiTestPlan.COVERAGE"
  showing [99], and deleting the real case 28 row failed with the mirror message
  showing [28]. Plan restored to 35 table rows and 33 numbered cases; confirmed no
  99 row remains and case 28 is back. Backend count 48 to 49, eight suites now
  report zero failures and errors. DeveloperGuide updated to describe the split.
  Java 25 check guiTest shadowJar passed on 2026-09-23; JAR rebuilt.

## Resume here

- Current review: `src/test/java/staniz/storage/WorkoutStoreTest.java`. Strongest
  file in the suite: every rejection asserts the file is byte-identical afterwards,
  cause chains are walked to their root and pinned, migration record UUIDs are
  fixed literals, and failures are followed by a recovery load that proves the
  store still works. First concern raised, awaiting approval: the nine-case loop in
  malformedOrUnsupportedJson asserts only StorageException.class, with a cause
  check for "{broken" alone. Those nine inputs fail for genuinely different reasons
  (empty, JSON null, array not object, syntax error, missing version, a version-two
  document using version-one field names, an out-of-range currentDay, duplicate
  library names, trailing content), yet all nine are checked identically.
  That matters more here than elsewhere, because storage concern 2 of this review
  was specifically about useful load-failure messages, and loadFailureMessage walks
  the cause chain to surface the first meaningful explanation. A regression that
  made every load failure generic would pass this loop untouched.
  Proposed: assert for all nine that the message carries the recovery guidance
  ("Your saved file has been left unchanged"), which is the user-facing contract
  and is not brittle, and additionally assert the specific explanation for the
  three semantically meaningful cases. Do not assert raw Gson parser text for the
  syntax cases; that would be brittle and is already covered by the cause check.
  User approved and it is implemented. The loop is now a LinkedHashMap from
  document to expected explanation, null where the wording belongs to Gson. Every
  case asserts the message carries "Your saved file has been left unchanged", and
  the three semantic cases additionally assert their explanation: "Missing required
  field: $.library.", "The current workout position is invalid." and "Exercise
  names must be unique." All three predictions from reading the source were right,
  including the duplicate-name case, where legacyId lowercases so both entries
  share a UUID and "Exercise IDs must be unique." was plausible; the name check
  runs first in validateAndIndexLibrary, so the name message wins.
  Verified by mutation: reducing loadFailureMessage to exception.getMessage(),
  dropping the cause walk, failed four tests. The strengthened loop case reported
  the currentDay document producing "Failed to invoke constructor
  'staniz.model.WorkoutData$State(int, List, List, int, List, Session)' with args
  [2, [], [], 1, [], null]", which is exactly the unhelpful wrapper that storage
  concern 2 existed to remove. Be accurate about the credit: three of the four
  failures came from pre-existing tests, so loadFailureMessage was not uncovered.
  What the change adds is coverage for the loop's own cases, which had none.
  Mutation reverted and confirmed. Final Java 25 check guiTest shadowJar passed
  47 backend tests, the GUI journey and both Checkstyle tasks on 2026-09-23, zero
  failures and errors across all seven suites, JAR rebuilt.
  Known gap recorded earlier: backup-creation failure and final-replacement failure
  during save had no targeted coverage. Partially closed now.
  Closed: failedSave_removesItsTemporaryFileAndLeavesTheTargetAlone points the store
  at a non-empty directory, which no platform lets a file replace, so the move fails
  after the temporary file exists. It asserts the failure message, that no .tmp file
  survives in the parent, and that the target's contents are untouched. Before this
  the cleanup block did execute, via directoryInsteadOfFile, but nothing asserted it
  worked, so a broken cleanup would have littered the user's data folder unnoticed.
  Verified by mutation: disabling the cleanup failed with "A failed save left a
  temporary file behind ==> expected: <[]> but was: <[.workouts-...tmp]>". Reverted.
  Backend count 47 to 48.
  NOT closed, and do not record it as closed: the ordering guarantee that a failed
  save leaves `migrated` true so the retry still writes the .v1.bak. Pinning it
  needs a save that fails while a real migrated v1 file sits at the target, and the
  only ways to force that are POSIX directory permissions, which throw
  UnsupportedOperationException on Windows, or a Windows file lock, which does
  nothing on POSIX. CI runs ubuntu, macos and windows, so either choice skips
  somewhere. The alternative, adding a failure seam to WorkoutStore purely for
  testability, was judged not worth it. The ordering is currently protected only by
  the shape of save(): backup, then temp, then move, then migrated = false.
  Also still uncovered and accepted: the non-atomic move fallback, which is near
  unreachable because the temp file is created in the same directory, and the
  absence of fsync before the move, which is a durability property rather than a
  test gap.

- Current review: `src/test/java/staniz/service/WorkoutServiceBoundaryTest.java` as
  a whole, having already added the identity-guard test to it. The file is sound:
  it picks real boundaries (-1, exactly size, both the exercise and set dimensions),
  interleaves each rejection with a valid selection, checks memory and bytes after
  every rejection, and reloads from disk to prove persistence. The performance and
  tie-order tests are the strongest in the suite, asserting that stored history
  keeps insertion order while performance() returns chronological order, which is a
  distinction the service review specifically wanted preserved.
  One modest concern raised, awaiting approval, in the data-driven loop of
  sessionIndexes: the expected message is computed as
  indexes[0] < 0 || indexes[0] >= 2 ? "Select an exercise." : "Select a set.",
  where the literal 2 duplicates the fixture's exercise count, so changing setUp to
  three exercises would silently invert the expectation for a case rather than fail.
  The loop also has no per-case identification, so a failure in one of the six rows
  reports a bare message mismatch without saying which indexes produced it.
  Proposed deriving the count from the session instead of hardcoding it, and adding
  a label to the loop's assertions. Smaller than the earlier findings; the file is
  otherwise in better shape than the rest of the suite.
  User chose to skip it and move to the storage tests. Nothing here is wrong today,
  so this is deferred rather than rejected: it is maintainability only, and it
  would matter if setUp ever gains a third exercise. This file's review is
  otherwise complete, with the identity-guard test added earlier.

- Current review: `src/test/java/staniz/service/WorkoutImprovementsTest.java`.
  Strong file overall: real files, restart-by-reconstruction, byte comparisons on
  the migration and identity tests, and rejection checks that verify memory and
  disk are unchanged. First concern raised, awaiting approval: 19 bare
  assertThrows(IllegalArgumentException...), and this file is where the remaining
  unasserted service messages live. After the WorkoutServiceTest work, six are
  still asserted nowhere: "Complete at least one set, or discard the session.",
  "Finish or discard your active session first.", "Select a history record.",
  "Select a library exercise.", "Start a workout session first." and "The exercise
  name does not match its library ID." Be accurate about the last one: it is
  partially checked by a contains() on the fragment "does not match its library ID"
  in plannedIdentity, so it is not wholly unverified.
  Sharper than the general point: session_restoresDrafts checks nine rejections
  with nine identical assertions. Six of them should all report the active-session
  guard and three should report different rules, so as written the test cannot show
  that the session is what blocked removeDay or skip rather than some unrelated
  failure. Proposed exact message assertions as in WorkoutServiceTest, and
  upgrading the plannedIdentity contains() to an exact match.
  User approved and it is implemented. All 16 IllegalArgumentException checks now
  assert exact messages through a rejected() helper, with ACTIVE_SESSION,
  EXACT_REPS and MISMATCHED_NAME as shared constants. The six session-guard checks
  share one constant, which makes it visible that one rule blocks all of them.
  The plannedIdentity contains() is now an exact match. The three StorageException
  checks were left as type-only deliberately: their message comes from the test's
  own stub, so asserting it there would be circular, unlike WorkoutServiceTest
  where it proves the service does not replace the store's explanation.
  Verified by a realistic mutation rather than a contrived one: changing the
  identity rule's message to the archive rule's, the kind of copy-paste slip that
  shows the user the wrong reason, failed plannedIdentity and actualIdentity with
  expected "The exercise name does not match its library ID." but was "Choose an
  active exercise or restore it first." That bug was invisible to the previous
  type-only assertions. Mutation reverted. Final Java 25 check guiTest shadowJar
  passed 46 backend tests, the GUI journey and both Checkstyle tasks on
  2026-09-23, zero failures and errors across all seven suites, JAR rebuilt.
  Progress on the message gap: service messages asserted nowhere fell from 11 to
  2. The remaining two, "Select a history record." and "Select a library
  exercise.", are not merely unasserted but appear to have no test at all. They
  guard correctRecord with an unknown record ID and archiveExercise with an
  unknown exercise ID, so WorkoutServiceBoundaryTest is their natural home.
- Both are now closed, in `WorkoutServiceBoundaryTest`. Confirmed first that they
  were genuinely untested: every correctRecord and archiveExercise call in the
  suite passed a valid ID. New test unknownIdentities_areRejectedWithoutChanging
  HistoryOrLibrary rejects an unknown record ID, an unknown exercise ID, and an
  unknown non-empty identity inside updateDay, which reaches the same library
  guard because a non-empty ID is looked up directly rather than resolved by name.
  Each asserts its exact message, then memory and file bytes are checked unchanged,
  then the same two operations succeed with real identities so the guards are shown
  to reject only what they should. The file's one bare assertThrows was tightened
  to "Start a workout session first." at the same time. Backend count 46 to 47.
  Verified by mutation: making the library lookup fall back to the first entry
  instead of throwing, so an unknown ID silently archives the wrong exercise, failed
  the new test with "Expected java.lang.IllegalArgumentException to be thrown, but
  nothing was thrown." Mutation reverted. Every distinctive message in
  WorkoutService is now asserted somewhere, down from 11 unasserted at the start of
  the backend review. Final Java 25 check guiTest shadowJar passed 47 backend tests,
  the GUI journey and both Checkstyle tasks on 2026-09-23, zero failures and errors
  across all seven suites, JAR rebuilt.

- Current review: `src/test/java/staniz/service/WorkoutServiceTest.java`.
  The file is structurally sound: isolated temp files, an injected fixed clock, a
  shared addRoutine fixture, and a consistent habit of asserting that a rejection
  changed neither memory nor the saved file. First concern raised, awaiting
  approval: its 20 rejection checks assert only IllegalArgumentException.class and
  never the message. That matters more here than in most services, because every
  GUI catch block displays exception.getMessage() verbatim, so these strings are
  user-facing UI text rather than internal detail. Evidence: of the service's 13
  distinctive messages, 11 appear nowhere in src/test, and the two that do are
  reached from the boundary and GUI suites. A few others are matched only by short
  substrings in the GUI journey, such as "already" for the duplicate-name case, so
  do not claim they are entirely unverified.
  The sharper risk is misattribution rather than absence: invalidDayEditsAndMoves
  checks five different rules with five identical assertions, so the wrong rule
  firing still passes. Proposed asserting the message on each rejection, matching
  what was just done in WorkoutDataTest, which both pins the UI contract at the
  backend level and makes each assertion name the rule it is testing.
  User approved, preferring exact matching, and it is implemented. Every
  IllegalArgumentException check now asserts the exact message through a rejected()
  helper, with NO_DAY and NOT_IN_LIBRARY as shared constants for the repeated ones.
  The three StorageException checks assert "Simulated disk failure" too, which
  verifies the service propagates the store's explanation rather than replacing it.
  The only bare assertThrows left are the one inside the helper and the
  UnsupportedOperationException check, where the type is the whole contract.
  Two findings worth keeping. First, moveDay(0, -1) is rejected by the position
  rule, not the direction rule: -1 is a valid direction onto an invalid index, so
  the message is "Select a workout day first." A comment in the test records this,
  since the obvious guess is wrong. Second, all three completion routes share one
  guard, which the repeated message now makes visible.
  Verified by mutation: narrowing the direction rule to direction == 1 failed with
  expected "Select a workout day first." but was "Move a day up or down one
  position.", which is a rule firing for the wrong reason. Under the previous
  type-only assertions that mutation was invisible, because both paths still throw
  IllegalArgumentException and the valid moveDay(1, 1) still worked. Mutation
  reverted and confirmed. Final Java 25 check guiTest shadowJar passed 46 backend
  tests, the GUI journey and both Checkstyle tasks on 2026-09-23, zero failures and
  errors across all seven suites, JAR rebuilt.
  Second concern was raised, examined and deliberately dropped; do not re-raise it.
  The observation was that library_rejectsBlankAndDuplicateNames compares raw bytes
  with Files.readString while the other rejection tests compare only a reloaded
  State. On examination the extra strictness buys nothing here. commit(State next)
  takes an already-constructed State and only then calls store.save, so every
  invariant runs before any write and a rejected operation cannot reach the disk.
  Serialization is deterministic, so a redundant save of an equal State would be
  byte-identical anyway. The one case byte comparison uniquely catches, losing
  unknown JSON fields on a rewrite, belongs to the storage layer and WorkoutStoreTest
  already asserts original bytes there. The division of labour is correct as it is.
  Caveat if commit is ever refactored: the argument depends entirely on validation
  preceding store.save, and that ordering is protected by the shape of commit
  rather than by any test.

- Current review: `src/test/java/staniz/model/WorkoutDataTest.java`, first of the
  backend test reviews. 33 lines, one test method. First concern raised, awaiting
  approval: the file covers WorkoutSet value rules and two State rules, but the
  State constructor's referential invariants are unverified. Evidence, not
  impression: `new State(` appears in the whole test tree exactly twice, both in
  this file, for duplicate day IDs and a negative current position. Of the model's
  distinctive invariant messages, only six are asserted anywhere in src/test, and
  the only referential one among them is "A planned exercise name does not match
  its library ID.", reached through WorkoutStoreTest rather than directly.
  Unasserted anywhere: exercise IDs and names unique, planned/history/session
  exercises present in the library, the active session belonging to the current
  day, history record IDs unique, the day-count limit and an unsupported version.
  Why it matters: WorkoutJsonValidation checks JSON shape while the State
  constructor checks meaning, so a file that is shape-valid but refers to a
  deleted exercise is caught only here, and nothing proves it is.
  Be careful not to overstate the gap when discussing it: a message going
  unasserted does not prove the branch never executes, only that no test pins it.
  Proposed grouped tests constructing State directly for identity uniqueness,
  referential integrity, session-to-current-day consistency and size/position
  bounds, each asserting its specific message the way the two existing State
  assertions already do. Smaller related point: the WorkoutSet assertions checked
  only the exception type, so a wrong IllegalArgumentException would satisfy them,
  unlike the State ones.
  User approved both and they are implemented. The file now has five tests instead
  of one, and the backend count moved from 42 to 46: the original value/state test
  with message assertions added to its WorkoutSet cases, plus duplicate identities
  across library and history, exercises missing from the library wherever they
  appear, session-to-current-day consistency, and split bounds. Every case asserts
  its specific message. The bounds test also asserts the accepted edges, an empty
  split at position 0 and a full seven-day split on its last day, so the rules are
  shown to accept what they should rather than only to reject.
  One subtlety that shaped the fixtures, worth keeping if these are extended:
  WorkoutData.link only resolves an exercise whose ID is blank, by name. So an
  unknown non-empty ID reaches validatePlannedDays and yields "A planned exercise
  is missing from the library.", while a blank ID with an unknown name fails
  earlier in link with "Exercise is missing from the library." Both are covered.
  Verified by mutation rather than assumed: weakening the session-day rule to
  require(true, ...) failed only activeSession_mustBelongToTheCurrentDay, with
  46 tests completed and 1 failed. No pre-existing test noticed the rule
  disappearing, which confirms the gap was real rather than theoretical.
  Mutation reverted and confirmed absent. Final Java 25 check guiTest shadowJar
  passed 46 backend tests, the GUI journey and both Checkstyle tasks on
  2026-09-23, zero failures and errors across all seven suites, JAR rebuilt.
  No concern is outstanding for this file.

- Current review: `src/test/java/staniz/gui/WorkoutGuiTest.java`, the first of the
  dedicated test-file reviews. 756 lines, one @Test driving 47 recorded scenarios
  through runJourney, testSaveFailure, runEnhancedJourney and two extracted
  failure helpers. First concern raised, awaiting approval: three assertions are
  vacuous because they search allText(root), which flattens every Label in the
  scene, for a string that is present regardless of the behavior under test.
  Line 212 checks "Pull" and line 288 checks "Legs" to confirm the current day
  advanced, but cycleChips renders every day's name, so both pass whatever day is
  current. Line 367 checks "exact" to confirm the ranged-rep rejection, but the
  session runs against a fresh improvements.json with empty history, so every
  exercise card shows ProgressView.previous's placeholder "No exact performance
  recorded yet.", which supplies the lowercase word on its own.
  None hides a live bug, because the state assertions beside them are strong:
  212 follows currentDay()==1, 288 follows assertEquals(saved, getState()), and
  367 follows a completed() check. What is missing is any real verification that
  the view reflects that state. Proposed scoping each to a node: `.workout-title`
  for the current day name, and `#session-error` for the session message.
  Checked and found adequate, so do not re-raise: the other short substring
  assertions, including contains("exact") at 232 and 463, read getText() on a
  specific error Label rather than the whole scene.
  User approved and it is implemented. The two day checks are now
  assertEquals against `#current-day-name`, and the session check reads
  `#session-error`. One production line was added for this: the hero's day-name
  Label now carries the id current-day-name, because the workout-title style
  class is shared with the onboarding card and history details, so a class lookup
  could match the wrong node. That follows the codebase's existing convention of
  giving controls ids for lookup.
  The new assertions were verified by deliberate mutation rather than assumed:
  making the hero render days().getFirst() failed with "expected: <Pull> but was:
  <Push>", and suppressing SessionView's error text failed at line 367. Crucially,
  with that same suppression the old allText assertion still passed and the run
  got as far as line 373, which is direct proof the original check was vacuous
  rather than merely loose. Both deliberate breaks were reverted and the final
  Java 25 check guiTest shadowJar passed 42 backend tests, the GUI journey and
  both Checkstyle tasks on 2026-09-23, zero failures and errors across all seven
  suites, JAR rebuilt.
  Both remaining observations were then raised and fixed on approval.
  Scope note on the first, so it is not misread as solved: the scenarios share
  accumulated state and several deliberately test restart and cycle position, so
  splitting them into independent tests would destroy the interleaving AGENTS.md
  requires. Fail-fast is kept as correct for a stateful sequence. What changed is
  locality: runJourney is wrapped so a failure reports how many scenarios ran, the
  last one completed and where the transcript is. Later scenarios still do not run
  after a failure, and that is intended, not an outstanding gap.
  The second is now enforced rather than remembered. A PLAN_COVERAGE map ties each
  of the 33 numbered cases in test/ui-test-plan.md to text from the scenario that
  exercises it, and assertPlanCoverage fails if the two drift in either direction:
  a plan case with no mapping, a mapping for a case the plan no longer lists, or a
  mapped scenario that never ran. Cases 24 and 25 deliberately share one scenario
  and are distinguished by different substrings of its text.
  Verification, both proven by deliberate mutation rather than assumed: adding a
  case 99 row to the plan failed with "UI test plan and the scenario mapping have
  drifted", and the same run showed the locality wrapper reporting "Failed after
  52 scenarios. Last completed: Replace a planned exercise with Squat."
  That experiment also exposed a real hole worth keeping in mind: the first
  attempt reported BUILD SUCCESSFUL in 7s because guiTest did not declare the plan
  as an input, so Gradle treated the task as up to date and the new check never
  ran. build.gradle now declares both the staniz.ui.plan system property and
  inputs.file('test/ui-test-plan.md'). Do not remove that inputs line; without it
  a plan-only edit silently skips the drift check.
  Temporary row removed and plan back to 33 cases. Final Java 25
  check guiTest shadowJar passed 42 backend tests, the GUI journey and both
  Checkstyle tasks on 2026-09-23, zero failures and errors across all seven
  suites, JAR rebuilt. No concern is outstanding for this file.

- Latest task, complete: the user asked whether long GUI methods are normal, then
  approved refactoring the two recommended. Measured spans first rather than
  applying a line limit, since none is specified for this project. Note the
  measurement caveat: a method followed by a nested class reads as far too long,
  which is why WorkoutEditor.addExercise appeared as 217 lines.
  MainWindow.currentView was about 87 lines and, more importantly, interleaved
  concerns: three different screens in one method, the same "needs at least one
  exercise" rule applied at two separate points, and style classes applied in
  three passes. It is now a roughly 24-line router over activeSession,
  emptySplit, workoutHero, cycleChips and completionActions. The disable rule is
  applied once, in a loop over the four recording actions; Skip is deliberately
  excluded, because it records nothing, and that intent is now stated in Javadoc
  rather than implied by which lines were omitted.
  RoutineDialogs.copySets was about 61 lines and is now 7, delegating to
  createDialog, createSelectors, createErrorLabel, createForm and
  configureCopyAction, matching the decomposition already approved for
  WorkoutDialog. A private Selectors record carries the four combo boxes as a unit
  and owns the all() list used for stale-error clearing.
  Both are behavior-preserving: no control ID, style class, message, ordering or
  disable rule changed. Java 25 check guiTest shadowJar passed 42 backend tests,
  the GUI journey and both Checkstyle tasks on 2026-09-23, with zero failures and
  errors across all seven suites; JAR rebuilt. Also compared
  build/gui-test/05-current.png, which renders identically, since a green build
  alone does not prove a view still looks right.
  Deliberately not refactored: splitView, libraryView and historyView, now the
  longest at about 61, 61 and 57 lines, were not read closely enough to justify
  it, and ProgressView's constructor is near 60 only because of the converter.
  Do not treat those numbers alone as a reason to split them; check first whether
  they interleave concerns the way currentView did.

- Current review: `src/main/java/staniz/gui/RoutineDialogs.java`, the last
  application source. Read the whole class and WorkoutService.copySets. Two
  suspicions were checked and cleared, so do not re-raise them: a day with no
  exercises leaves the exercise selector empty at index -1, but copySets rejects
  that with the friendly "Select source and target exercises." message; and the
  day selectors resolve by selected index rather than by name, which is correct
  here and is not the ProgressView identity problem, since indices address the
  same days() list that filled them and duplicate day names are therefore safe.
  First concern raised, awaiting approval: refresh.run() sits inside the try that
  catches IllegalArgumentException and StorageException, which is exactly the flaw
  already fixed in WorkoutDialog.configureSaveAction this session. Here the
  consequence differs: a copy retry is idempotent, so no duplicate data results,
  but event.consume() means a refresh failure after a committed copy would show a
  misleading error and refuse to close the dialog on every further OK, leaving
  Cancel as the only exit. User approved and it is implemented with the same shape
  as WorkoutDialog: a local copied flag set inside the try, with refresh.run()
  after it, plus a comment recording why. Java 25 check guiTest shadowJar passed
  42 backend tests, the GUI journey including the existing copy-sets case, both
  Checkstyle tasks and rebuilt the JAR on 2026-09-23, with zero failures and
  errors across all seven suites. No visible behavior changed on any exercised
  path, so no UI plan change was needed. The post-commit callback failure itself
  stays untested here, as in WorkoutDialog: both would need a modal-aware harness
  with an injected throwing callback. Second concern is also resolved. It was: the error label is never cleared, so a
  stale message survives any later selection change, unlike WorkoutDialog, which
  clears on edit, and it has no visible/managed binding, so an empty label still
  occupies layout space, and it lacks the editor-error style class the other two
  views use. The three compounded: adding the class alone would have rendered
  nothing, because this DialogPane never loaded styles.css. User approved and all
  of it is implemented: the pane now adds styles.css the way WorkoutDialog does,
  the label carries editor-error with maxWidth and the visible/managed bindings,
  and a loop over the four selectors clears the message on any value change, which
  is this dialog's equivalent of editing a field. MainWindow's Alert and
  TextInputDialog instances are still unstyled, left for the CSS pass, since those
  are standard dialogs where default styling is defensible.
  The failure path had no coverage at all: the journey drove the four selectors
  only down the success path and never asserted #copy-error, which is how three
  defects accumulated in one label. GUI case now clears the source exercise
  selection, which reproduces the state a day with no exercises produces without
  adding an empty day and disturbing the day counts that neighbouring cases
  assert. It confirms the rejection message and visibility, that the split is
  unchanged, then that reselecting clears the message and the corrected copy
  saves. UI plan row 22a records it. Java 25 check guiTest shadowJar passed
  42 backend tests, the GUI journey with both new records, both Checkstyle tasks
  and rebuilt the JAR on 2026-09-23, with zero failures and errors across all
  seven suites. No concern is outstanding for this file.
- Previous reviewed file: `src/main/java/staniz/gui/ProgressView.java`.
  Read the whole class, the Exercise model, addExercise, State.exercises(),
  MainWindow's quantity() helper and the legacy migration's library creation.
  One suspicion was checked and cleared, so do not re-raise it: duplicate library
  names are not reachable, because addExercise compares case-insensitively against
  State.exercises(), which lists every entry including archived ones, and the
  version-two compatibility fixture keeps distinct library names while only the
  history and session snapshots hold the older name. First concern raised, awaiting
  approval: line 53 builds "N exact recorded sessions" by concatenation, so an
  exercise recorded once reads "1 exact recorded sessions". MainWindow already has
  a quantity(count, noun) helper used in six places, but it is private, so this
  file cannot reuse it. User approved and this is implemented: quantity is now
  package-private with Javadoc explaining that every counting view shares one rule,
  and ProgressView calls MainWindow.quantity(records.size(), "exact recorded
  session"). A shared helper class was considered and not taken, since it would add
  a file for one method; the codebase already crosses views this way, as SessionView
  calls ProgressView.previous. GUI case "Open Progress after a single exact
  recording" and UI plan row 13a cover the singular form, asserting both that the
  singular text appears and that the plural does not, since one is a prefix of the
  other, plus that no chart is built below two sessions. Placement matters: the
  case sits immediately after the first actual recording, where Bench press
  provably has exactly one exact record. An earlier attempt placed it after the
  two-session Progress case and failed, because by then no library entry has
  exactly one exact recording; do not move it back there.
  Second concern is also resolved. It was: the exercise selector
  holds display names and lines 50-51 resolve the chosen name back to an ID with
  findFirst().orElseThrow(), which round-trips identity through a display string
  and would throw an uncaught NoSuchElementException from inside a JavaFX listener
  rather than showing a message. It was safe only because of the uniqueness
  invariant enforced in addExercise, and three cracks were noted: uniqueness is
  enforced with equalsIgnoreCase while the lookup used equals; the items and the
  resolution came from two separate getState() calls; and a ComboBox<String>
  accepts any string, so a name not in the library is expressible at all.
  User approved the typed-selector fix, now implemented: the selector is a
  ComboBox<Exercise> holding library entries, with a StringConverter whose
  toString shows the name and whose fromString returns null, since the control is
  not editable. Items are set from the library directly and the listener reads
  after.id(), so the two-line name lookup and its orElseThrow are gone. The
  GUI case now selects the Exercise rather than its name. Java 25
  check guiTest shadowJar passed 42 backend tests, the GUI journey, both Checkstyle
  tasks and rebuilt the JAR on 2026-09-23, with zero failures and errors across all
  seven suites. Verified visually in build/gui-test/11-progress.png that the
  selector still displays "Bench press", which is what the converter governs.
  One related product question was raised and left open, deliberately not bundled:
  the selector lists archived library entries with nothing distinguishing them,
  while the Exercises tab hides archived entries behind a toggle, so the two views
  disagree about what the library means. Showing them here is probably intended,
  since progress for a dropped exercise is still useful. Decide before release.
  No other concern is outstanding for this file.
- Previous reviewed file: `src/main/java/staniz/gui/SessionView.java`.
  Read the whole class, updateSessionSet/startSession/requireWorkout in the
  service, and WorkoutEditor's accessible-text calls for comparison. Two suspected
  defects were checked and cleared, so do not re-raise them: updateCount() cannot
  divide by zero, because requireWorkout rejects starting a session on a day with
  no exercises and every WorkoutExercise requires at least one set, so sets.size()
  is always positive; and ordinary typing never triggers SetRow.save's restore
  branch, because updateSessionSet stores weight and reps as unvalidated strings,
  so only ticking Done can fail validation. First concern raised, awaiting
  approval: accessible text is inconsistent and partly wrong on the set rows.
  Line 141 builds "0 exercise, set 1 weight in kg" from the raw 0-based
  exerciseIndex, while the reps field and the Done checkbox get no accessible text
  at all, so every row announces just "Done". WorkoutEditor sets accessible text on
  both fields and names the exercise selector. User approved and this is now
  implemented: SetRow takes the exercise name, builds one `position` string of
  "<exercise>, set N", and applies it to all three controls as "... weight in
  kilograms", "... reps performed" and "Mark ... complete". The kilograms wording
  matches WorkoutEditor rather than inventing a second phrasing. Control ids were
  deliberately left unchanged, so every GUI test lookup still resolves. Java 25
  check guiTest shadowJar passed 42 backend tests, the GUI journey, both Checkstyle
  tasks and rebuilt the JAR on 2026-09-22; all seven suites reported zero failures
  and zero errors. Nothing visual changed, so no UI plan case was needed for this
  fix.
  Second concern was raised and deliberately declined by the user after discussion:
  every keystroke calls updateSessionSet, so each character performs a full JSON
  write through the temp-file replace. Recommendation was to keep it, because the
  per-character write is what makes a session survive a crash at any instant, which
  the "Restart during workout" case depends on, no slowdown has been measured, and
  debouncing would add flush points on focus loss, Done, Finish, Discard and window
  close, each a chance to silently lose an entry. Do not reopen without a
  measurement. Third concern is resolved: SetRow.save's catch restored the text
  fields as well as the checkbox, which was a no-op on the validation path, since
  parseActual runs only when completed is true so the text was already saved, and
  destructive on the storage-failure path, where it deleted characters the user had
  just typed. That also contradicted WorkoutDialog's stated convention that unsaved
  drafts stay editable after a failure. The catch now resynchronizes only the
  completion box; the `restoring` field and its guard were removed with it, since
  CheckBox.setSelected does not fire setOnAction and they existed only to protect
  the two setText calls. Class Javadoc reworded to match. Recovery is now automatic:
  the next keystroke after storage returns carries the retained text through.
  New GUI case testSessionDraftSurvivesSaveFailure covers it, and UI plan row 20a
  records it. Note for anyone extending that case: the Biceps curl set is
  prescribed 12 reps, so its field already holds "12" and setting "12" again fires
  no change event; the test uses 11 deliberately. Java 25 check guiTest shadowJar
  passed 42 backend tests, the GUI journey including both new records, both
  Checkstyle tasks and rebuilt the JAR on 2026-09-22, with zero failures and errors
  across all seven suites. This closes the session half of the recorded
  storage-failure coverage gap; the modal draft retention/retry gap in
  WorkoutDialog remains open.
- Previous reviewed file: `src/main/java/staniz/gui/WorkoutEditor.java`.
  Read the whole class, the WorkoutExercise/WorkoutSet model invariants, the
  `.invalid` CSS rule and the UI test plan's blank-name cases. Two suspected
  defects were checked and cleared, so do not re-raise them: sets.getLast() in
  the duplicate-set handler cannot throw, because WorkoutExercise requires at
  least one set, so any initial row starts with a set row and the last one
  cannot be removed; and an unchosen ComboBox yields null, which WorkoutData.text
  converts to a clean IllegalArgumentException rather than an NPE, so it reaches
  the dialog's catch normally. First concern, now resolved: an
  unchosen exercise name is rejected only by the model, so it gets no invalid
  highlight, no focus and no row number, unlike set errors, which SetFields.invalid
  marks, focuses and prefixes with "Set N". With several rows the user cannot tell
  which row is at fault. Proposed giving ExerciseFields its own read() that
  validates the name, marks and focuses the selector and names the row, which also
  stops readExercises reaching into three private inner-class fields. Related: the
  `.invalid` rule in styles.css is scoped to `.text-field`, so a ComboBox would
  need a rule, and the `.exercise-selector` class set in code has no CSS rule at
  all. No UI plan case covers saving a dialog row with no exercise chosen; cases
  2, 4 and 22 cover blank library and day names instead. Note that
  `.exercise-selector` has no CSS rule but is not dead: WorkoutGuiTest line 599
  looks it up, so it is a test handle and must stay.
  User approved, and both problems are implemented: ExerciseFields.read()
  validates the chosen name before reading its sets and throws through
  invalidName(), which marks the selector, focuses it and prefixes "Exercise N";
  readExercises() is now one stream over fields and no longer reaches into private
  inner-class state; the name value listener removes the invalid class so a
  corrected row does not stay red; styles.css line 90 now pairs
  `.text-field.invalid, .combo-box.invalid`, matching how lines 84 and 89 already
  pair the two controls. Deliberate behavior change: within one row the name is
  checked before its sets, so a row missing both reports the name first and the
  marked control always matches the message. Java 25 check guiTest shadowJar
  passed all 42 backend tests, both Checkstyle tasks and the full GUI journey on
  2026-09-22; JAR rebuilt. No existing case depended on the previous order.
  That coverage is now complete. The GUI journey adds two interleaved records
  inside the existing edit-day modal, placed before the -1 kg case: adding a row
  and saving with nothing chosen asserts the message contains "Exercise 1", that
  the selector carries the invalid class, and that the saved day is still empty;
  choosing Bench press then asserts both the class and the message clear. UI plan
  row 4a records it. This is a genuine regression test: under the previous code
  the row's empty set was read first, so the message was "Set 1: Enter a positive
  whole-number rep count or range", and the selector was never marked. Java 25
  check guiTest shadowJar passed 42 backend tests, the GUI journey with both new
  records, both Checkstyle tasks and rebuilt the JAR on 2026-09-22, with zero
  failures and errors across all seven suites.
- Previous reviewed file: `src/main/java/staniz/gui/WorkoutDialog.java`.
  Read the complete class, all MainWindow callers and existing GUI scenarios.
  First concern is API clarity: show(service, owner, actual, index, record,
  onSaved) encodes plan/completion/history modes through a boolean and nullable
  record; history passes -1 for an unused index and callers must derive actual
  from recording type. Existing callers are consistent; no current wrong-route
  defect claimed. User approved named entry points, now implemented: editPlan
  accepts a split index; recordWorkout reads the current day internally;
  correctHistory derives exact-rep requirements from the record's recording type.
  All delegate to one private show implementation; its internal mode arguments
  are supplied only by these entry points. MainWindow now has matching named
  handlers and a shared workoutSaved status/refresh callback, with no mode flags,
  null record or unused index at its dialog call sites. UI behavior unchanged.
  Expanded the existing GUI journey: as-planned correction rejects a reversed
  range then saves a valid range with identity/date/type and surrounding state
  preserved; actual history correction also rejects a range before accepting exact
  reps. Java 25 check guiTest shadowJar passed all 42 backend tests, both Checkstyle
  tasks and the full GUI journey. JAR rebuilt; guide/UI plan/logs updated.
  This first concern is resolved.
  Second concern is now resolved: onSaved.run() was inside the validation/save
  catch, so an IllegalArgumentException thrown by the callback (refresh rebuilds
  all five views) would have been shown as a retryable form error after the save
  had already committed, inviting a second save. For recordWorkout a retry would
  call completeWithChanges again, writing a second history entry and advancing
  the cycle twice for one workout. This followed from code inspection only; it was
  never reproduced, and no current refresh() path is known to throw. User approved
  continuing the review and refactoring this file. Implemented the minimal fix:
  configureSaveAction sets a local saved flag inside the try and runs onSaved
  after it, so the guarded region covers exactly reading and saving the draft.
  Validation and storage failures still keep the dialog open with the draft and
  message intact, so UI plan cases 14a and 25 are unaffected and unchanged.
  Java 25 check guiTest shadowJar passed all 42 backend tests, both Checkstyle
  tasks and the full GUI journey on 2026-09-22; JAR rebuilt. No test was added
  for a throwing callback; WorkoutDialog.show uses showAndWait, so covering it
  needs a modal-aware harness. Raise that if callback coverage is wanted.
  Third concern was raised and deferred, not rejected: private show() still
  threads actual/index/record with -1 and null sentinels, and four sites re-derive
  the mode from them (day selection, dialogText, name.setDisable, saveOperation).
  Proposed a private Form record built by each entry point, carrying day, text,
  exactReps, nameEditable and operation, leaving show as pure assembly. The user
  chose to move to the next file instead, so this is unimplemented. Two minor
  points also recorded without change: the styles.css getResource at line 100 is
  dereferenced without a null check, deliberately, since a missing stylesheet is a
  packaging bug; and the synthetic WorkoutDay("history", ...) ID is a literal that
  never reaches storage. Remaining untouched observation: the GUI storage-failure
  scenario exercises quick completion, not modal draft retention and retry.
  Discuss concerns one at a time before implementing.
  Note: logs/PromptSummary.md entry 118 records a request to add responsibility
  based refactoring guidance to the code-review skill. No such skill exists under
  .codex/skills (only present-changes-visually and test-ui), so treat that as
  an open item and confirm the intended target before creating anything.
- Previous reviewed file: `src/main/java/staniz/gui/MainWindow.java`.
  Read the full file, relevant service methods and GUI correction tests. First
  concern: saving a history correction calls refresh(), which recreates
  historyView with an empty day filter, All exercises, and first-record selection.
  Thus correcting an older filtered entry loses the user's browsing context.
  This follows from code inspection; no GUI reproduction was run. Existing GUI
  tests verify corrected data but do not assert retained filters or selection.
  Proposed retaining filter/selection state, but the user explicitly chose to keep
  refresh resetting that state: "i think its fine to have refresh and not save the
  state". This concern is resolved by accepting existing behavior; do not implement
  filter persistence or tests expecting it. Clarified that history lists workout
  sessions and the exercise filter finds sessions containing an exercise.
  User approved extracting the workout dialog and correcting Javadoc. Completed:
  package-private final WorkoutDialog owns modal construction, WorkoutEditor,
  validation feedback and the three service save routes. MainWindow opens it
  and supplies an onSaved callback to refresh and report success. Retained control
  IDs, styles, messages, dimensions, Cancel behavior and failed-save draft handling.
  Existing WorkoutEditor remains responsible for exercise/set controls. No tab
  extraction, framework or filter-state persistence was added. Class/constructor
  Javadoc now accounts for all five views, including Progress.
  Java 25 check guiTest shadowJar passed 42 backend tests, both Checkstyle tasks
  and the full GUI journey; JAR rebuilt. Existing tests retained unchanged.
  Inspected build/gui-test/03-editor.png. Developer guide and logs updated.
  No visible behavior changed, so existing UI test plan still applies unchanged.
  All discussed MainWindow concerns are resolved. Await direction for next file;
  the extracted WorkoutDialog has been inspected during this refactor, with its
  dedicated file review still outstanding. No further functional bug confirmed.
- Remaining dedicated application-source reviews: gui/WorkoutDialog.java,
  gui/WorkoutEditor.java, gui/SessionView.java, gui/ProgressView.java,
  gui/RoutineDialogs.java. Main startup-error checks remain pending separately.
  Dedicated test-file reviews still outstanding: model/WorkoutDataTest.java;
  service/WorkoutServiceTest.java, WorkoutServiceBoundaryTest.java,
  WorkoutImprovementsTest.java; storage/WorkoutStoreTest.java,
  WorkoutJsonValidationTest.java; gui/WorkoutGuiTest.java. These tests have been
  inspected during related reviews, but not completed as separate file reviews.
  Supporting review remains for styles.css, build.gradle, .github/workflows/gradle.yml,
  run-workouts.ps1, test/run-tests.ps1, config/checkstyle XML files, UI test plan,
  and final README/guides/reflections/log consistency. Repository inventory checked.
  Latest work is the approved dialog extraction and verification recorded above.
- Previous reviewed file: `src/main/java/staniz/gui/Launcher.java`.
  User requested moving on from Main without approving the proposed startup
  checks; those remain pending. Inspected Launcher, Main and Gradle entry-point
  configuration. No actionable concern: the final class has a private constructor,
  documented main method, and forwards arguments to Application.launch(Main.class,
  args); Gradle names the same launcher. Keep unchanged. No code edits or tests
  rerun; only handoff updated. Next suggested review: gui/MainWindow.java.
- Previous review: `src/main/java/staniz/gui/Main.java`.
  Inspected startup, Launcher/build/launch-script wiring, relevant MainWindow
  construction, GUI test setup and documented manual startup checks. No clear
  functional defect established. First concern to discuss is verification of
  Main.start's loading-failure branch: WorkoutGuiTest.showWindow constructs
  MainWindow directly and does not execute Main.start. Backend tests verify
  storage errors/file preservation, but not the startup alert or dismissal.
  Proposed focused startup checks in an isolated working directory: valid launch,
  malformed save shows recovery alert without opening the main window, alert
  dismissal exits normally, original bytes remain intact, then repaired-save
  relaunch succeeds. Manual cases already exist but are not recorded as executed
  in this review. No code/test/diagnostic edits approved or made; no tests rerun.
  Only this handoff changed. Discuss one concern at a time before implementing.
- Previous reviewed file: `src/main/java/staniz/storage/StorageException.java`.
  Inspected both constructors, every construction site, service propagation,
  representative GUI handling and existing failure/cause assertions. No actionable
  concern found; recommend retaining it unchanged. Both constructors are used:
  production wraps the underlying cause, while test doubles use message-only
  failures. Its checked type keeps storage failures explicit in service APIs.
  Only this handoff changed; no code edits or test reruns during this review.
  Review complete with no changes; subsequently moved to gui/Main.java.
- Previous review: `src/main/java/staniz/storage/WorkoutJsonValidation.java`.
  Resumed from this handoff and inspected the full validator, its tests, the
  WorkoutStore loading sequence and relevant model constructors. No clear
  functional defect identified. User approved the naming cleanup, now completed:
  validation helpers use validateExercises/validateSession/validateSet,
  validateObjectArray/validateStringFields/validateString/validateBooleanField;
  value-returning helpers use readInteger and requireArray/Object/Number/Field.
  The exception factory is invalidFieldError. All call sites and the method
  reference were updated; validation rules and messages are unchanged.
  Java 25 check shadowJar passed all 42 backend tests and both Checkstyle tasks;
  JAR rebuilt. Existing tests were retained unchanged. No GUI rerun for this
  private-helper rename. The discussed validator concern is resolved; await
  direction before moving to the next file. No other fix is pending approval.
- Previous task: user approved reorganizing classes into model, service, storage
  and gui packages. Completed with matching test packages, updated imports and
  PIT test-discovery patterns.
- All three discussed WorkoutStore concerns (JSON validation, wrapped validation
  messages, and migration structure) are approved, implemented and verified.
  Latest approval: "ok lets do this" for the migration helper extraction.
  The user asked for the next concern; review has moved to WorkoutService.java.
- First service concern is addressed: name-only validation could accept a
  mismatched known ID/name in actual/history inputs. Following discussion, the
  user chose fixed library names and approved implementation, asking about tests.
  Removed library renaming, added service identity checks, and verified editable
  workout/history replacements. The user then requested review of the rest of
  WorkoutService; that inspection is complete. The user approved abstracting
  duplicated completion logic, now implemented and verified. After discussing
  latest-query sorting, the user preferred keeping sorting for chronological
  browsing; retain the current approach. Boundary tests were subsequently approved,
  added and verified. All discussed WorkoutService items are now resolved. Await
  direction to review the next file (WorkoutJsonValidation.java is a candidate).
- Product decision: exercise ID and name are fixed after creation. Adding,
  archiving and restoring remain; replacing an exercise occurrence in a workout
  or history entry remains allowed. History replacement updates that entry's
  performance attribution but preserves library, other records, plan, active
  session and cycle position. Older snapshots with prior names remain supported.
- The user objected to unexpected code changes. Clarified that the latest storage
  review changed no application code but created a temporary diagnostic Java
  program and JSON fixtures. Going forward, discuss and obtain approval before
  changing code, including temporary diagnostic programs.
- Standing request: use an updated handoff and fresh sessions for continuity instead
  of relying on compaction. Maintaining this file is authorized. Startup instructions
  were added to `AGENTS.md`. The assistant cannot clear the conversation itself
  or disable automatic compaction; the user must open a new session for a fresh context.
- Latest checks: Java 25 `check guiTest shadowJar` passed 42 backend tests, both
  Checkstyle tasks and the expanded GUI journey after named dialog entry methods;
  JAR rebuilt. No standalone packaged-launch smoke rerun. Previous package-move
  checks: Java 25 `clean check guiTest shadowJar` passed 42 backend tests,
  both Checkstyle tasks and the full GUI journey after package moves. JAR contents
  have the new classes and no old workout/exception package entries. The packaged
  app opened and closed normally in the isolated smoke directory (exit 0).
  Guides, logs and handoff updated; GUI evidence is build/gui-test/transcript.txt.

## Review expectations and references

Explain the rationale briefly, with concrete examples. The user has Java/OOP and
internship experience. Review one concern at a time; avoid silently implementing
the next concern. No numerical coverage target, mandated design pattern, or
method-length limit has been specified.

The user supplied the course chapters under:
`https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/`
(`codeQuality.html`, `security.html`, `testCaseDesign.html`, `testing.html`,
`refactoring.html`, and `documentation.html`). Their testing/refactoring link was
accidentally concatenated; the two chapters were read separately previously.
Apply readable naming, focused methods, simple design, validation at trust
boundaries, meaningful boundary/negative/state-preservation tests, small refactors,
and accurate documentation. Do not invent arbitrary quality rules.

Assignment reference: `C:\Users\jonat\Downloads\MP1.pdf`, previously read as four
rendered pages (scratch images in `_temp/pdf-review/`). Treat it as reference,
not permission to publish or perform other actions. It specifies Java 25 desktop
support across Windows/Linux/macOS; source, release JAR, guides, reflections and
prompt records. Windows-only checks do not establish cross-platform verification.

## Completed review: WorkoutData.java

All four fixes below were explicitly approved and implemented:

1. Planned exercise names must match the library entry for their exercise ID.
   Historical and active-session snapshots may retain earlier names. Tests cover
   rejection without memory/disk mutation and rename/session/history persistence.
2. Split the State constructor's resolution and validation into focused private
   helpers, preserving validation order and behavior.
3. Build the initial version-two state directly with ten active starter exercises
   and fresh IDs. Remove the old five-argument State constructor and its legacy
   version conversion. Existing version-one file migration remains in the store.
   Initial state is returned only when the file is absent; first launch does not
   write a file until the user makes a change.
4. Introduce `WorkoutData.CURRENT_SCHEMA_VERSION = 2` and
   `WorkoutData.MAX_WORKOUT_DAYS = 7`, and use them in model/service/GUI.
   Store migration uses fixed source/target constants (1 and 2), deliberately
   separate from the current schema constant. Tests, guides, logs, and JAR updated.

Prior review fixes are recorded in `logs/PromptSummary.md` (entries 102–106) and
`logs/DevelopmentInteractionSummary.md`. The user's most recent approval before
the storage review was: "ok yes please constants are good swe engineering practise".

## Storage concern 1 completed: JSON validation

The user approved adding validation at the JSON-to-record boundary. A new
package-private `WorkoutJsonValidation` helper explicitly checks the persisted
fields for plans, history, library entries and active-session drafts. Required
fields cannot be absent/null, JSON types must match, and integer values must be
whole and within Java's integer range. Errors name the offending JSON path.
The store validates the schema version without coercion, rejects unsupported
versions, then validates the document before migration and deserialization.

Valid legacy saves still migrate; optional sessions may be absent/null. Incomplete
session weight/reps remain strings. Numerically whole values such as 8.0 and 8e0
are accepted. Model constructors continue to enforce workout rules. Unknown JSON
fields remain ignored. Save behavior and migration structure were not refactored
as part of that first fix; migration helper extraction is recorded below.

`WorkoutJsonValidationTest` adds five tests with grouped cases for missing/null
fields, incorrect types, fractional/overflowing integers, valid boundary values,
drafts, optional sessions, and legacy migration/backups. Negative cases interleave
valid loads through the same store and verify unchanged file contents. Earlier
reproductions (missing kg -> 0, missing currentDay -> 0, fractional reps/version
truncation) are now covered by regression tests.

## Storage concern 2 completed: useful validation errors

WorkoutStore.loadFailureMessage walks the cause chain and returns the first
nonblank IllegalArgumentException message, falling back to the original error
message if none exists. Choosing the first explanation preserves the model's
friendly message over a deeper NumberFormatException. The StorageException still
contains the original caught exception, file path and recovery guidance.

Tests cover negative reps/weights, mismatched exercise names/IDs, invalid completed
session weights, intact debugging causes, unchanged files and valid reloads after
rejection. Existing syntax/read-failure tests now assert original-message fallback.
This does not change save handling or unwrap all possible exception types.

## Storage concern 3 completed: migration helper extraction

The approved refactor keeps migrate() as the coordinator and extracts private
createLegacyLibrary(), migratePlannedDays(), migrateHistory(), and
archiveHistoricalExercises() methods within WorkoutStore, with explanatory Javadoc.
The original loops, ID-generation formula, processing order and backup behavior
are retained. Record IDs are generated before mutating each historical record.

A focused WorkoutStoreTest regression passed against the original implementation
before extraction and again afterward. Its fixture contains two identical history
records, each using a historical-only exercise and an active library exercise.
Checks cover single archived-entry creation, active entries remaining active,
library/day/exercise order, current position, recorded values, stable distinct
record IDs (fixed expected UUIDs), repeated loading, original-byte preservation,
backup creation on first save, round trip, and backup preservation on repeat save.

All three discussed concerns are addressed. This is not a claim that every
possible storage fault has been tested; the earlier coverage observation follows.

Existing strengths: load leaves saved files untouched, save writes a sibling
temporary file before replacement, version-one data is backed up before saving
the migrated state. Backup-creation and final-replacement failure cases warrant
targeted coverage if those paths are changed; do not claim they were verified.

Diagnostic artifacts created during the last review (not application changes):
`_temp/WorkoutStoreReview.java` and `_temp/store-review/*.json`. The reproduction
checked that all sample files stayed byte-for-byte unchanged after loading.
Do not delete these or create more diagnostic code without appropriate direction.

## Verification checkpoint and worktree

Latest result for moving the WorkoutDialog save callback out of the catch block
on 2026-09-22: Java 25 `check guiTest shadowJar` passed all 42 backend tests,
both Checkstyle tasks and the full GUI journey; release JAR rebuilt. No test or
UI plan changes were needed, since no visible behavior changed on any path that
the suite exercises. Known JavaFX startup warnings remain nonfatal.

Previous result for named dialog entry methods on 2026-09-22: Java 25
`check guiTest shadowJar` passed all 42 backend tests, both Checkstyle tasks and
the full GUI journey with additional as-planned/actual history rep-mode checks.
JAR rebuilt; current GUI transcript is build/gui-test/transcript.txt. UI plan
cases 14a and 25 document the added assertions. No standalone packaged smoke run.

Latest result for workout dialog extraction on 2026-09-22: Java 25
`check guiTest shadowJar` passed all 42 backend tests, both Checkstyle tasks and
the full GUI journey; release JAR rebuilt. No test modifications. GUI transcript
and screenshots are current in build/gui-test; inspected 03-editor.png. No
standalone packaged-launch smoke rerun. Known JavaFX warnings remain nonfatal.

Latest result for validator helper renaming on 2026-09-22: Java 25
`check shadowJar` passed all 42 backend tests and both Checkstyle tasks; release
JAR rebuilt. No test changes or GUI rerun for this behavior-preserving rename.

Latest result for package reorganization on 2026-09-22: Java 25
`clean check guiTest shadowJar` passed 42 backend tests, both Checkstyle checks
and the entire desktop journey. Clean build removed stale classes. JAR contents
were checked for new classes and absence of old workout/exception package entries;
packaged-launch smoke test opened its window and closed normally with exit 0.
PIT discovery patterns were updated; mutation analysis itself was not run.

Previous result for the approved boundary tests on 2026-09-22: Java 25 `check`
passed all 42 backend tests and both Checkstyle tasks. Added three tests in
WorkoutServiceBoundaryTest. No production edits, GUI run or JAR rebuild this turn;
the packaged code remains current from the preceding completion refactor.

Previous result for shared completion helpers on 2026-09-22: Java 25
`check shadowJar` passed 39 backend tests and both Checkstyle tasks; JAR rebuilt.
Two existing tests gained active-session guard, failure/retry, and duplicate-finish
checks. GUI tests were not rerun for this behavior-preserving backend refactor.

Previous result for fixed library names on 2026-09-22: Java 25
`check guiTest shadowJar` passed 39 backend tests, both Checkstyle tasks and the
complete GUI journey. JAR rebuilt. An initial Checkstyle failure on one 124-character
helper line was fixed; the rerun passed. Reviewed build/gui-test screenshots
12-immutable-library.png and 13-history-replacement.png. Earlier manual startup
checks for validation messages remain unrun this session.

Historical result from the approved constants change, reported on 2026-09-22:
29 backend tests, both Checkstyle tasks, the JavaFX GUI journey, and `shadowJar`
passed. `release/staniz.jar` was rebuilt then. Do not present these as freshly run
checks for a later change. The diagnostic artifacts listed above predate the
approved JSON validation implementation.

The worktree contains extensive uncommitted conversion/improvement/review work,
including removal of task-manager classes and new workout/GUI classes. Preserve
it; do not reset, revert, or attribute all existing changes to the current task.
No commit, push, or tag was requested or performed. No subagents were used or
authorized for this review.

Known working PowerShell build command from the repository root:

```powershell
$env:JAVA_HOME = (Resolve-Path '_temp/tools/java25/zulu25.36.205-ca-jdk25.0.4.1-win_x64').Path
$env:GRADLE_USER_HOME = Join-Path $PWD '_temp/gradle-home'
$env:DEBUG = $null
.\gradlew.bat check guiTest shadowJar --offline --console=plain --no-daemon
```

GUI execution requires the appropriate tool escalation. Earlier sandboxed JUnit
runs also failed during OS temporary-directory cleanup with AccessDeniedException;
successful builds used escalation for that reason. Request it via the tool when
needed, following the active environment's rules. Backend-only changes generally
need `check shadowJar`; add `guiTest` when relevant to visible behavior.

## Agreed product

- JavaFX GUI, no CLI.
- One active split of up to seven ordered workout days, independent of weekdays.
- Starter exercise-name library plus user-added names.
- Library exercise names and IDs are fixed after creation; archive/restore is
  available. Replacing exercises within plans or historical records is supported.
- Individual sets: kg only, exact reps or inclusive rep ranges.
- Current workout shows its exercises and sets.
- Completion: as planned, with changed performance, or without recording.
- Skip advances without marking completion; the cycle wraps after the final day.
- Local text storage is sufficient; JSON was accepted.
- Proposed four views (Current Workout, My Split, Exercises, History) were included
  in the implementation plan the user authorized.

## Implementation decisions

The user subsequently authorized six improvements: active sessions with per-set
actual recording, previous performance, day duplication/exercise reordering/set
copying, history corrections and filters, library management, and progress
charts. They are implemented in the five-tab JavaFX UI, still using local JSON.
Library renaming was originally included but explicitly removed in the service
review; names are now fixed while adding, archiving and restoring remain.
Schema version two adds stable exercise/record identities and active-session
snapshots. Version-one saves migrate in memory and get a `.v1.bak` before first
save. Exact performed sessions drive charts; as-planned prescriptions do not.
Split changes are blocked during an active session until it is finished or discarded.

All state is in `data/workouts.json`, so history and cycle advancement save
together. Changes are published in memory only after a successful write.
As-planned history preserves prescribed rep ranges; changed-performance recording
requires exact counts. History contains immutable snapshots. Editing/reordering
preserves the current day's ID; removing it selects its successor. Legacy
`data/staniz.txt` is untouched. Starter exercises are seeded only when no workout
file exists. One app instance per data file is supported.

## Service concern 1 completed: fixed library names and validated choices

Removed renameExercise() and the Rename GUI action. Exercise records were already
immutable Java values; the service now exposes no operation that replaces an
existing library name. Archive/restore preserves that entry's ID and name.

Replaced validateChoices() with resolveChoices() used by updateDay(),
completeWithChanges(), and correctRecord(). It resolves blank IDs from library
names, validates supplied ID/name pairs, permits an exact retained older snapshot
pair, and enforces archived availability by ID. resolveChoiceName() prefers a
library name match and otherwise uses an existing older snapshot's identity.
No schema bump or saved-file rewriting on load was needed.

Tests: rename/archive workflow became archive/restore invariance; runtime rename
during session became a saved version-two compatibility fixture with older active
and historical names, including reuse of the old name by another library entry.
The migration test now archives instead of renaming to trigger its first save.
Two new backend workflows cover mismatched actual/history pairs and archived
additions, plus replacements that update derived performance without changing
library, other records, plan/current day or active session. Desktop cases 23/24
were revised and 27/28 added for history and planned exercise replacements.

## Remaining WorkoutService review findings and resolutions

The rest of the file and relevant tests/callers were inspected. No additional
clear functional defect was identified. The review initially changed only this
handoff. Completion helper extraction was subsequently approved and implemented
as recorded below; the following two recommendations have also been resolved.

2. Low-priority simplification: previousPerformance() calls performance(), which
   filters/sorts/materializes every matching record, then scans again with max().
   Proposed shared match predicate/filter and direct max for latest lookup, leaving
   chronological sorting for charts. No measured slowdown is claimed. User later
   preferred retaining sorting, so leave this implementation unchanged. Clarified
   that performance() sorts a returned list, not stored state.history(); the history
   browser currently uses saved order (normally newest-first). Do not claim the
   current previousPerformance() sort controls history-screen ordering.
3. Test gaps: controlled different timestamps for multiple matching performances,
   including ties, and negative/past-last exercise/set indexes in updateSessionSet()
   were not explicitly covered. User approved adding tests; completed in
   WorkoutServiceBoundaryTest (details below). Existing tests were retained.

Latest query decision: retain sorting; no code change authorized or needed for that
suggestion. Approved boundary tests now pass. The discussed service review is complete;
await direction before moving to another file or implementing any further findings.

## Shared completion logic completed

User approved "1. please abstract this out and reuse". record() now accepts the
day/session snapshot's ID and name plus recording type and exercises, creates one
history entry, and calls shared advance(). finishSession() collects/validates
checked sets and calls record(); quick completions use the same helper.
advance() alone builds the next State, clears the completed session and commits.

Moved requireNoSession() from advance() into skip(). Quick completions already
call requireWorkout(), which checks no active session. finishSession() retains
requireSession() and at-least-one-checked-set validation. Empty-list quick actual
recording remains allowed, and saved historical/session names are preserved.

Strengthened existing tests to reject all quick completion modes and skip while
a checked session exists, preserving memory and disk. Failed-session-save test
now retries successfully, verifies one record and one advancement, then rejects
a second finish. Java 25 backend/Checkstyle/JAR checks passed as noted above.

## Boundary tests completed

User approved "last concern please add the boundary tests". Added
WorkoutServiceBoundaryTest with three service-level tests using isolated files:

- Session indexes: no active session, negative and just-past-last exercise/set
  indexes across two exercises with different set counts. Each rejection checks
  the useful validation message and unchanged memory/file bytes, followed by a
  valid first/last update and restart. Plan, library, history and position persist.
- Performance: empty history, a single exact record, then deliberately out-of-order
  fixed timestamps, newer as-planned/unrelated records and an empty actual record.
  Queries select only matching exact records, sort chronologically, choose the
  latest timestamp, return empty for an unrecorded exercise, and preserve stored order.
- Equal timestamps: preserve saved tie order and select its first matching entry
  for previous performance, including after restart and with an older record added later.

All 42 backend tests and both Checkstyle tasks passed with Java 25. No production,
sorting or GUI changes were made. Developer guide, prompt/development logs and
this handoff were updated; UI test plan did not need changes.

## Code and checks

- Model: `src/main/java/staniz/model/WorkoutData.java`.
- Service: `src/main/java/staniz/service/WorkoutService.java`.
- Storage: `src/main/java/staniz/storage/` contains WorkoutStore,
  package-private WorkoutJsonValidation, and StorageException.
- GUI: `src/main/java/staniz/gui/`; programmatic controls and CSS.
- UI refinements: compact light/green layout, workout summary and cycle chips,
  pinned completion actions, descriptive split/history rows, exercise search,
  last-set duplication, and focused field validation with stale-error clearing.
- Backend tests mirror model/service/storage under `src/test/java/staniz/`:
  WorkoutDataTest in model; WorkoutServiceTest, WorkoutServiceBoundaryTest and
  WorkoutImprovementsTest in service; WorkoutStoreTest and WorkoutJsonValidationTest
  in storage. The existing standalone model-invariants test moved from
  WorkoutStoreTest to WorkoutDataTest, keeping the total count at 42.
- Desktop tests: `src/test/java/staniz/gui/WorkoutGuiTest.java`.
- GUI scenario plan: `test/ui-test-plan.md`.
- Run `gradlew check`, `gradlew guiTest`, and `gradlew shadowJar` using Java 25.
- Desktop test artifacts: `build/gui-test/`; test HTML under `build/reports/tests/`.
- Packaged application: `release/staniz.jar`.

Package organization was explicitly approved after the service review. Imports,
package declarations and PIT targetTests now use model/service/storage/gui.
The JSON format and application behavior did not change. Older notes/logs and
ignored diagnostic scratch programs may still mention previous package paths;
do not run those scratch programs without updating them under appropriate scope.
Completed review status for WorkoutData/WorkoutStore/WorkoutService carries over
to their new paths. Dedicated reviews of remaining classes, tests, CSS, build/CI,
scripts and final documentation consistency are still outstanding.

The console test-ui skill does not apply to removed console commands. Its old
Python runner is historical; `test/run-tests.ps1` now invokes the JavaFX suite.
Continue interleaving positive/negative cases and checking state preservation.

## Local environment

At conversion time, the configured `JAVA_HOME` pointed at a missing installation.
A portable Java 25.0.4.1 was downloaded under the ignored path:
`_temp/tools/java25/zulu25.36.205-ca-jdk25.0.4.1-win_x64`.
Builds used `_temp/gradle-home` as `GRADLE_USER_HOME`. These local temporary paths
are not prerequisites on another machine; configure a valid JDK 25 there.

Do not commit, tag, or push unless explicitly requested. Current check results
must be verified from a fresh run or labeled as historical. Earlier reflection
and log sections describe the prior task-manager app, not current functionality.
