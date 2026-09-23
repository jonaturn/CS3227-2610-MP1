# Staniz Workout Planner

A JavaFX desktop application for one person's workout split. Build an exercise
library, plan up to seven workout days, and cycle through them at your own pace,
independently of weekdays or the calendar.

- Start with ten exercises; add, search, archive, and restore library entries with fixed names.
- Plan each individual set with kg and an exact rep count or rep range.
- Duplicate days, reorder exercises, and copy sets within or between workout days.
- Start a workout, enter actual kg/reps, and tick individual sets with automatic session recovery.
- See previous performance beside each exercise.
- Complete a workout as planned, record changed performance, or complete without recording.
- Skip a workout and pick up at the next day; the last day wraps to the first.
- Filter and correct history without changing your routine or current position.
- Review recorded sets over time, with weight and rep charts after two exact recordings.
- Resume your split after restarting; changes save automatically to local JSON.

See the [User Guide](docs/UserGuide.md), [Developer Guide](docs/DeveloperGuide.md),
and [GUI test plan](test/ui-test-plan.md).

## Build and run

Use **JDK 25**. Set `JAVA_HOME` to its installation directory and add its `bin`
directory to `PATH`. Confirm `java -version` reports version 25.

On Windows, `.\run-workouts.ps1` launches the packaged app from this repository.
It uses `JAVA_HOME`, or the portable JDK in `_temp/tools/java25/` if available.
You can also pass `-JavaHome <JDK25 path>` without changing global settings.

| Task | Windows PowerShell | macOS / Linux |
| --- | --- | --- |
| Run from source | `.\gradlew.bat run` | `./gradlew run` |
| Backend tests and style checks | `.\gradlew.bat check` | `./gradlew check` |
| Desktop interaction tests | `.\gradlew.bat guiTest` | `./gradlew guiTest` |
| Package executable JAR | `.\gradlew.bat shadowJar` | `./gradlew shadowJar` |
| Run packaged app | `java -jar release/staniz.jar` | `java -jar release/staniz.jar` |

GUI tests require a desktop; on Linux CI use `xvfb-run -a ./gradlew guiTest`.
They generate a console transcript and screenshots in `build/gui-test/`.

## Saved data

`data/workouts.json` is relative to the directory where the app is launched.
It contains the exercise library, split, current day, active session, and history together, so
recording a workout and advancing the cycle use a single save. Close the app
before copying this file as a backup or moving it to another computer. Run only
one instance against a given save file.

Existing version-one saves are migrated automatically, with their original bytes
backed up to `data/workouts.json.v1.bak` before the first save in version two.

Legacy task-manager data in `data/staniz.txt` is left untouched. This version
does not expose the former task commands or import tasks as workouts.

The project retains its original Git history and the historical
[AI reflection](docs/Reflections.md) and [prompt log](logs/PromptSummary.md).
Those older sections describe the previous task-manager implementation.
