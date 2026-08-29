# Codex Session Handoff

This file summarizes the project context and decisions needed to continue work in a new Codex session or on
another computer. Read `AGENTS.md` first because it contains the active repository instructions, then read this
file before changing code.

## Quick start for a new Codex session

Use this prompt after opening the cloned repository in Codex:

```text
Read AGENTS.md and CODEX_SESSION_HANDOFF.md completely. Verify the current Git status, branch, tags, Java
version, and automated tests before making changes. Continue the project one increment at a time. For each
increment, first inspect the official brief and current code, propose an implementation plan, and wait for my
approval before implementing. Do not commit, tag, or push unless I explicitly ask.
```

## Authoritative project sources

- Project brief: <https://nus-cs2103-ay2627-s1.github.io/website/projectDuke/cs3227.html>
- Code quality: <https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/codeQuality.html>
- Refactoring: <https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/refactoring.html>
- Documentation: <https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/documentation.html>
- Error handling: <https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/errorHandling.html>

Always verify the exact wording of the next increment against the current project brief before planning it.

## Current checkpoint

Last verified on 2026-08-30:

```text
Branch: codex/mp1
HEAD: 0f25770 Level-8
Remote: origin/codex/mp1 at the same commit
Working tree: clean before this handoff file was created
Latest tag: Level-8
```

Existing lightweight tags:

```text
A-Classes
Level-3
Level-4
Level-5
Level-6
Level-7
Level-8
```

The student normally reviews the changes and then commits, tags, and pushes the increment manually. Do not
perform any of those Git mutations without an explicit request.

## Implemented functionality

- Level 3: Mark and unmark tasks; `Task` class hierarchy added through `A-Classes`.
- Level 4: `Todo`, `Deadline`, and `Event` task types and their commands.
- Level 5: User-facing validation and `StanizException` error handling.
- Level 6: Command enumeration and `delete` support.
- Level 7: Automatic persistence to `data/staniz.txt`, including restart loading, directory creation, escaped
  backslashes and pipes, and line-specific corrupted-data errors.
- Level 8: Deadline and event dates represented by `java.time.LocalDate`, ISO input/storage, friendly display
  formatting, calendar-date validation, and event-range validation.

Supported commands:

```text
todo DESCRIPTION
deadline DESCRIPTION /by yyyy-MM-dd
event DESCRIPTION /from yyyy-MM-dd /to yyyy-MM-dd
list
mark NUMBER
unmark NUMBER
delete NUMBER
bye
```

Level 8 examples:

```text
deadline return book /by 2019-12-02
event conference /from 2019-12-02 /to 2019-12-03
```

User-facing dates are formatted as `MMM dd yyyy`, using an explicit English locale:

```text
[D][ ] return book (by: Dec 02 2019)
[E][ ] conference (from: Dec 02 2019 to: Dec 03 2019)
```

The application rejects malformed or impossible calendar dates and rejects an event whose start date is after
its end date. Same-day events are valid.

## Current design

- `Staniz`: console entry point, command recognition, validation, dispatch, and responses.
- `CommandType`: enum containing command keywords, argument rules, and whether a command changes tasks.
- `Task`: abstract base class containing description and completion status.
- `Todo`, `Deadline`, `Event`: concrete task types with display and persistence serialization.
- `DateParser`: shared ISO `LocalDate` parsing and English user-facing date formatting.
- `Storage`: UTF-8 file loading/saving, escaped-field parsing, subtype reconstruction, and persisted-data
  validation.
- `StanizException`: invalid user-command errors.
- `StorageException`: file access and corrupted-data errors.

Prefer the simplest design sufficient for the next increment. Keep parsing and storage concerns centralized
rather than duplicating them across task subclasses.

## Persistence format

The OS-independent relative path is:

```text
data/staniz.txt
```

Example data:

```text
T | 0 | borrow book
D | 1 | return book | 2019-12-02
E | 0 | conference | 2019-12-02 | 2019-12-03
```

`0` means incomplete and `1` means complete. Backslashes and pipe characters inside text fields are escaped.
Dates are stored in ISO `yyyy-MM-dd` form so loading is locale-independent.

Saved deadline/event values from before Level 8, such as `Sunday` or `Mon 2pm`, cannot be converted reliably
to complete calendar dates and are treated as invalid saved data. The current local save file contained only a
todo when Level 8 was verified, so it was unaffected.

The `/data/` directory is ignored by Git. Copy it separately if runtime tasks must move to another computer.

## Automated testing

The project-local `test-ui` skill is the required console-testing workflow:

```text
.codex/skills/test-ui/SKILL.md
```

The executable test specification is:

```text
test/ui-test-plan.md
```

There are currently 11 end-to-end cases. Cases 1-8 cover the Level 4-6 behavior, Cases 9-10 verify persistence
across application restarts, and Case 11 verifies Level 8 date handling. The Level 8 coverage includes:

1. Valid deadline and event dates.
2. User-facing date formatting.
3. Persistence and restart with `LocalDate` values.
4. Invalid date formats.
5. Impossible dates.
6. A valid leap date (`2024-02-29`).
7. Event start after its end.
8. Same-day events.
9. Confirmation that rejected commands do not add tasks.

The last complete Java 25 run passed:

```text
PASS [UI test suite: 11/11 cases]
```

Run the suite on Windows from the repository root:

```powershell
.\test\run-tests.ps1 -JavaHome "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
```

The runner compiles all Java sources, creates a separate temporary working directory for every case, and uses
`<restart>` markers to start new JVM processes while retaining that case's isolated save directory. Keep
`test/ui-test-plan.md` synchronized with every user-visible command or output change.

## Manual compilation and execution

On Windows PowerShell:

```powershell
$java25Home = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$sources = Get-ChildItem "src\main\java\*.java" | ForEach-Object FullName

New-Item -ItemType Directory -Force "_temp\manual-classes" | Out-Null
& "$java25Home\bin\javac.exe" -d "_temp\manual-classes" $sources
& "$java25Home\bin\java.exe" -cp "_temp\manual-classes" Staniz
```

Java 25 is mandatory. If VS Code continues showing an older Java version after selecting the JDK, restart VS
Code so its integrated terminal inherits the updated environment.

## Project-local skills

- `test-ui`: compiles with Java 25 and runs the documented console cases while printing full transcripts.
- `present-changes-visually`: generates `_temp/visual-diff.html`, a self-contained split-view comparison of the
  worktree against `HEAD`.

Read a selected skill's complete `SKILL.md` before using it. After command-line UI changes, run the complete UI
suite and show the input/output transcript. Generate the visual diff when reviewing an increment.

## Working preferences and next-step workflow

For each new project increment:

1. Confirm the previous increment is committed, tagged, pushed, and the working tree is clean.
2. Read the exact increment requirements from the official brief.
3. Inspect the existing implementation and identify affected files.
4. Present a concrete plan, command syntax, validation rules, storage implications, and tests.
5. Wait for approval before changing files.
6. Implement only the approved scope with explanatory Javadoc where useful.
7. Update and run all applicable UI tests with Java 25; show the console transcript.
8. Run quality checks and generate the visual diff.
9. Leave commit, lightweight tag, and push actions to the student unless explicitly requested.

Before continuing beyond Level 8, verify the current course sequence in the brief. Likely upcoming increments
include more OOP separation, packages, Gradle, JUnit, and JAR creation, but the current brief is authoritative.

## Environment notes

On this Windows checkout, some Git commands previously needed:

```powershell
git -c safe.directory=C:/Users/jonat/Desktop/CS3227-2610-MP1 status
```

This workaround is checkout-specific and may not be needed on another computer. `_temp/` is ignored and is
used for generated classes and visual diffs.
