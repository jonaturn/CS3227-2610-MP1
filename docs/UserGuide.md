# Staniz Workout Planner — User Guide

Staniz manages one person's active workout split. A split contains one to seven
ordered workout days, such as Push, Pull, and Legs. These are training sessions,
not calendar days. Before setting up a split, the application starts empty with
a library of ten exercise names.

## Starting the app

Install JDK 25, then run `java -jar release/staniz.jar` from the repository root.
To build the JAR first, use `.\gradlew.bat shadowJar` on Windows or
`./gradlew shadowJar` on macOS/Linux. From source, use the equivalent `run` task.

Keep the launch directory consistent: your save is `data/workouts.json` relative
to that directory. No account, database server, or network connection is needed
to use the packaged application.

## Exercises

Open **Exercises** to see the library. Enter a name and select **Add exercise**.
Use **Search your exercises** to filter by name, ignoring letter case. Clear
the search to show the full library again. Searching never changes saved data.
Names must not be blank and duplicates are rejected regardless of letter case.
Leading and trailing spaces are removed. The library stores names only; weights
and reps belong to each workout's sets.

The starter library includes Bench press, Squat, Deadlift, Overhead press,
Barbell row, Pull-up, Lat pulldown, Leg press, Biceps curl, and Triceps pushdown.
Exercise names are fixed after creation. To use a different exercise, select it
in the workout or history editor; this replaces that entry without renaming a
library exercise. To correct a library-name typo, add a new exercise and archive
the old one; they have separate performance histories.
Select a library row to **Archive / restore** it. Archived exercises are hidden
from new selections; existing plans and history remain.
Enable **Show archived** to restore them. Names remain unique across active and
archived entries. Exercises are not permanently deleted.

## My Split

1. Enter a workout day name, such as **Push**, and select **Add day**.
2. Select the day and choose **Edit workout**.
3. Select **+ Add exercise**, then choose an exercise from the library.
4. Enter a weight in kg and reps for each set. Any positive whole-number count
   (such as `5`, `8`, or `15`) or inclusive range (such as `3-5`, `8-12`, or
   `10-15`) works. The field hint **Count or range** describes these two formats.
   Select **+ Add set** for more sets.
5. Add more exercises in workout order and select **Save workout**.

Use **Duplicate last set** to copy its weight and reps into another row, then
adjust them if needed. The last remaining set cannot be removed on its own;
use **Remove exercise** to omit the entire exercise instead.

Use **Move up** or **Move down** inside an exercise card to change exercise order.
**Duplicate day** creates an independent copy at the end of the split, subject
to the seven-day limit. **Copy sets** lets you choose a source day/exercise and a
destination day/exercise. It replaces the destination's sets while preserving
its exercise name. Cancel leaves the plan unchanged.

Example: Bench press, set 1 at 60 kg for 8–12 reps, set 2 at 70 kg for 8 reps.
Each row represents one set. Weights can be fractional or zero (for example,
zero added weight on a bodyweight exercise). Negative or nonfinite weights,
zero/negative reps, and reversed ranges are rejected.

You can rename a day in its editor, remove exercises or sets, move a day up or
down, and remove a day after confirmation. A maximum of seven days is allowed.
Empty days may be saved while planning, but cannot be completed until they have
exercises. Every exercise entry must contain at least one set; remove the whole
exercise if it is no longer needed.

Editing and reordering preserve the current workout's identity. Removing the
current day selects the following day, wrapping to the first if necessary.
Removing every day returns the app to its initial planning state while keeping
the exercise library and history.

**Cancel** discards draft edits. Invalid input keeps the editor open with a
message so it can be corrected without changing the saved plan. The offending
field is highlighted and focused, whether it is a set value or an exercise that
was never chosen, and the message names the row, such as **Set 2** or
**Exercise 1**. Editing that field, or choosing an exercise, clears the message
and the highlight.

## Current Workout

This view shows the current day, its exercises and sets, and the next day.
The summary includes exercise and set counts, and the split chips show where
you are in the cycle. Completion and skip controls remain below the scrolling
workout list so they stay available when the window is small or the workout is long.
If you have no split yet, **Build my split** opens setup directly. An empty
current day offers **Plan this workout** to open its editor.
**Start workout** opens active workout mode. The previous exact performance is
shown beside each exercise. Older saved snapshots remain linked by exercise identity.
You can also use these quick actions:

| Action | History | Cycle |
| --- | --- | --- |
| Complete as planned | Saves the prescribed sets, including rep ranges | Advances |
| Record with changes | Saves the exact performed sets you enter | Advances after saving |
| Complete without recording | Adds no performance record | Advances |
| Skip workout | Adds no completion record | Advances |

The last day wraps to the first; a one-day split returns to the same workout.
Opening the app or changing views never advances the cycle automatically.

**Complete as planned** does not invent exact reps for a range. The history
entry explicitly identifies these as prescribed sets. To record actual reps,
choose **Record with changes**. Exact prescribed counts are prefilled; planned
ranges leave the actual rep field empty for you to fill in. You can change kg,
remove skipped sets or exercises, and add extra sets or library exercises.
Removing every exercise records an empty performed session. To simply move on
without a performance record, use Skip instead. Cancelling recording leaves the
current day unchanged.

### Active workout mode

Enter actual kg and exact reps as you train, then tick **Done** for each set.
Ranged prescriptions leave actual reps blank until entered. Uncheck a completed
set to correct it. The progress bar and remaining count show what is left.
Set entries and completion ticks save immediately, including unfinished text;
closing and reopening the app resumes the session.

**Finish session** records checked sets and advances once. If some sets remain,
confirm that only checked sets should be recorded. At least one completed set is
required. **Discard session** removes the active draft without recording or
advancing; you can then skip the workout if needed. Finish or discard the active
session before changing the split or using other completion actions.

Active mode tracks the sets from the starting plan. To add extra exercises or
sets to a performance record, use **Record with changes**, or correct the saved
session afterward in History.

## History

Open **History** and select a session to see its sets. Newest recordings appear
first, with local date/time, workout name, and recording type. Timestamps describe
when a session was recorded; they do not determine the split order.

Filter by workout-day name (case-insensitive partial match) and/or a library
exercise. Archived exercises remain available in these filters.
Select **Correct record** to fix a workout name, weights, reps, or exercise/set
list. Invalid entries keep the editor open. Save preserves the original date,
record identity and recording type, and does not advance the split or change
the planned routine. Cancel leaves the record unchanged.

Replacing an exercise in a past session moves that entry's measured performance
to the newly selected exercise's previous-performance and progress views. Existing
weights and reps stay in the editor, so correct them if necessary. The library,
other sessions, planned workouts, and any active session remain unchanged.

Renaming, editing, or removing a planned workout never rewrites history. Only
an explicit history correction changes its snapshot. History deletion is not
provided.

## Progress

Choose an exercise in **Progress** to review its exact recorded sets over time.
Archived exercises remain available. With at least two recorded sessions, charts
show the heaviest recorded set in kg and total recorded reps per session. Session
numbers run chronologically; dated details below the charts list each set.
Corrections appear in these views and previous-performance comparisons.

As-planned records are prescriptions, so they are excluded even when their rep
targets are exact. Use active mode or Record with changes to capture performance.

## Saving and recovery

Successful changes save automatically to `data/workouts.json`. Routine and
history dialogs save when you press Save; active-session typing saves as you go.
If saving fails, an error appears and the change is not applied. Correct the
storage problem and retry. Your draft is kept either way: a dialog stays open
with what you entered, and in active workout mode the entry you were typing stays
on screen, so once storage recovers the next thing you type saves it too. Only a
set you had ticked is unticked again, to match what is actually saved.

On first use, a missing file loads the starter library. The file is created on
your first saved change. A malformed existing file produces a startup error;
Staniz does not overwrite it or silently reset your workouts. Close the app and
restore a backup or correct the file before restarting. Keep a separate backup
of `data/workouts.json`, and open only one app instance for that file.

Older version-one workout files are upgraded automatically. Before the first
save in version two, the original is copied to `data/workouts.json.v1.bak` if
that backup does not already exist. Keep this backup if you may need the old app;
the old app cannot read version-two saves.

Old task data in `data/staniz.txt` is preserved but is not imported. There is no
command-line interface in this version; all workout interaction uses JavaFX.
