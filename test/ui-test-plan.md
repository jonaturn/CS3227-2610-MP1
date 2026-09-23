# Staniz JavaFX UI test plan

The app has no CLI. These scenarios replace the former task-command cases.
Run `.\gradlew.bat guiTest` (Windows) or `./gradlew guiTest` (macOS/Linux) using
Java 25 and a desktop. On Linux without a display: `xvfb-run -a ./gradlew guiTest`.
The PowerShell convenience wrapper is `.\test\run-tests.ps1 -JavaHome <JDK25>`.

`WorkoutGuiTest` runs the sequence below against isolated temporary JSON, stops
on its first assertion failure, and prints each action and checked result.
Artifacts are `build/gui-test/transcript.txt`, PNG screenshots in that directory,
and `build/reports/tests/guiTest/index.html`. No production user data is touched.

| Case | Action | Expected result and state check |
| --- | --- | --- |
| 1 | Open a fresh app; select Build my split | Starter library, empty split and history; setup opens My Split directly |
| 2 | Add a blank exercise | Error; library still contains ten entries |
| 3 | Add Cable fly, then duplicate cable FLY; search CABLE, an unknown name, then clear | Duplicate rejected; filtering finds Cable fly or shows no matches; clearing restores eleven entries without mutating data |
| 4 | Add Push, reject blank day, then add Pull and Legs | Three days in order; Push remains current |
| 4a | Edit Push; add an exercise row and save without choosing an exercise, then choose Bench press | Error names the row ("Exercise 1") and marks the selector; saved day unchanged; choosing an exercise clears both the mark and the message |
| 5 | Edit Push; enter -1 kg, then range 12-8 | Both saves rejected; editor remains open, saved day unchanged |
| 6 | Correct invalid input; duplicate last set, then change the copy to 70 kg × 8 | Error clears; duplicate initially retains 60 kg × 8-12; two individual sets saved with distinct weights |
| 7 | Plan Pull with 10-15 reps and Legs with 5 reps; move Legs up and down | Both rep formats saved correctly; order restored; current workout retains its identity |
| 8 | Resize to minimum content size, then complete Push as planned | Completion/skip actions remain visible; range preserved in history; Pull becomes current |
| 9 | Skip Pull | Legs becomes current; no added history |
| 10 | Complete Legs without recording | Wraps to Push; still one history record |
| 11 | Change a recording draft, then Cancel | Plan, position, and history unchanged |
| 12 | Save actual workout without filling a ranged set's reps | Exact reps required; no state change |
| 13 | Record changed weights/reps and an extra set | Actual snapshot saved; plan unchanged; cycle advances |
| 13a | Open Progress after a single exact recording of Bench press | Count reads "1 exact recorded session" in the singular; charts stay hidden until a second session exists |
| 14 | Record another session: remove planned exercise, add Cable fly and remove a set; open History | Actual replacement saved, original plan unchanged; three records available |
| 14a | Correct an as-planned history record: reject 12-8, then save 61 kg x 8-12 | Invalid draft leaves state unchanged; valid range saved with record ID/type/date, other history, plan, library and cycle position preserved; reload matches |
| 15 | Recreate the app/service from the same save | Library, plan, history, and current day restored |
| 16 | Add days up to seven, attempt eighth | Guidance shows 7 and badge shows 7 / 7 days; Add and Duplicate disabled; still seven days |
| 17 | Cancel day removal, then remove current day | Cancellation preserves split; removal selects successor; history intact |
| 18 | Simulate failed completion save, then retry successfully | First attempt changes neither memory nor disk; retry records/advances once |
| 19 | Start active workout; attempt to tick a ranged actual rep entry | Exact reps required; completion unchecked and history unchanged |
| 20 | Enter 62.5 kg × 10, tick Done, then restart | Actual values and completion restored; two sets remain |
| 20a | While saving fails, type a weight into another set, then restore storage and change that set's reps | Typed weight stays on screen with a failure message and nothing is saved; after recovery the retained weight persists without retyping it |
| 21 | Cancel partial finish, then confirm it | Cancellation preserves active session; confirmation records only checked work and shows previous performance |
| 22a | In Copy sets, confirm with no source exercise selected, then reselect one and copy | Copy rejected with a visible message and an unchanged split; the message clears as soon as a selection changes; the corrected copy then saves |
| 22 | Duplicate day with blank name, correct name, reorder copied exercises, copy sets across days | Blank name rejected; new day gets independent identity; target prescription copied, original order unchanged |
| 23 | Verify fixed-name guidance and no Rename action; archive Bench press, show archived and restore | Library names/IDs, plans, and history remain unchanged after restore |
| 24 | Filter history by unknown day, then Push and Bench press | Empty result does not change data; original session found by stable exercise ID |
| 25 | Correct actual record with zero reps, then a range, then valid 65 kg × 11 | Both invalid edits rejected; exact-count correction preserves timestamp and position and updates previous performance |
| 26 | Record another session, then open Progress | Two exact recordings produce weight and rep charts, including corrected first record |
| 27 | Replace historical Bench press with Squat; reject zero reps, then save 80 kg x 8 | Rejection preserves state; replacement retains record ID/date, updates performance attribution, and preserves library, plans and cycle position |
| 28 | Replace a planned exercise with Squat and reload | Selected workout changes; library names/IDs, history and cycle position remain unchanged |

Backend tests additionally cover unknown exercises, invalid indexes, zero/negative
reps, nonfinite weights, empty-day completion, a one-day cycle, deleting every
day, omitted exercise recording, corrupt JSON, unreadable paths, and preservation
of the legacy task data file. Run `gradlew check` for these cases.
Additional backend checks cover migration without rewriting on load, original-file
backup, stable identity after archive/restore, seven-day duplication limits,
correction validation, discarded sessions, invalid drafts, and failed active-session saves.
Backend identity checks also reject a planned name paired with another library
exercise's ID without changing memory or disk, reject such saved JSON without
overwriting it, reject mismatched ID/name pairs in actual recordings and corrections,
and preserve older saved active/history snapshots with names from previous releases.

## Manual release checks

- Launch `java -jar release/staniz.jar` from a clean directory; verify startup
  and first-use guidance, then create a split and restart from the same directory.
- Inspect all five tabs and editor dialogs at the minimum window size and normal
  display scaling. Check that long names wrap and many exercises/sets scroll.
- Navigate fields and buttons using Tab/Shift+Tab and cancel an editor with Escape.
- In actual recording, remove a skipped set/exercise and add an extra exercise.
- Restore a copied JSON backup; confirm the saved current day and history return.
- Start with malformed JSON in an isolated directory; expect a clear startup
  error and an unchanged file, never a silent reset.
- In a copied save, remove a set's `kg`, then separately give `minReps` a fractional
  value. Each launch should report the offending JSON field and leave the file
  unchanged. Restore the valid copy and confirm normal loading. Automated backend
  tests cover these cases along with wrong types, integer overflow, history and
  session fields, optional sessions, and version-one migration.
- In a copied save, set `minReps` to -1. Expect the startup alert to explain that
  reps must be positive, retaining the file path and recovery guidance, without
  a constructor-invocation message. Confirm the file is unchanged, then restore
  the valid copy and relaunch. Backend tests also verify invalid weights,
  mismatched exercise names/IDs, and completed-session input errors, preserving
  the full exception chain for debugging.

Record manual results separately from automated results; screenshots alone do
not prove keyboard or packaged-launch behavior.
